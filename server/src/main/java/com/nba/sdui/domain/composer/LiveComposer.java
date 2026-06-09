package com.nba.sdui.domain.composer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.models.generated.AccessibilityProperties;
import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.AdSlot;
import com.nba.sdui.models.generated.CalendarStrip;
import com.nba.sdui.models.generated.DataBinding;
import com.nba.sdui.models.generated.DataBindingPath;
import com.nba.sdui.models.generated.ParamBindings;
import com.nba.sdui.models.generated.Placeholder;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionSurface;
import com.nba.sdui.models.generated.State;
import com.nba.sdui.models.generated.Spacing;
import com.nba.sdui.models.generated.Targeting;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.integration.model.scoreboard.Broadcasters;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.PlayoffSeries;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;
import com.nba.sdui.integration.model.scoreboard.ScoreboardTeam;

/**
 * Composes the "Games" SDUI screen — live, upcoming & final games for a
 * selected date.
 *
 * Layout:
 *   0. CalendarStrip        – horizontal date picker; parameterized refresh
 *   1. PromoBanner          – League Pass commerce CTA (static, server-owned copy)
 *   2. GameScheduleList     – "Live Now" compact list (SSE per-row clock countdown)
 *   3. GameScheduleList     – "Upcoming" (static)
 *   4. GameScheduleList     – "Final" (static)
 *
 * <p>The default date is the CDN scoreboard's authoritative {@code gameDate}
 * field — the league's "current game day" already accounts for late-night
 * roll-forward, so this is more meaningful than wall-clock midnight ET. The
 * CalendarStrip drives a {@code refresh} action that re-runs composition with
 * the selected ISO date: today's date reuses the already-fetched CDN
 * scoreboard, other dates call the NBA Core API gameCardFeed endpoint. When
 * no games exist for the selected date, only the CalendarStrip is rendered —
 * no synthetic mock data is emitted.
 *
 * <p>Live rows use per-game SSE channels bound via {@code dataBinding} so the
 * LiveClock element counts down in real-time. When the Ably/SSE channel delivers
 * a linescore frame, the client writes {@code clock.isRunning=true} and the
 * clock starts local interpolation.
 */
@Component
public class LiveComposer {

    private static final Logger log = LoggerFactory.getLogger(LiveComposer.class);

    private final ScoreboardPort scoreboardPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;
    private final SectionRefreshService sectionRefreshService;
    private final ParameterizedRefreshService parameterizedRefreshService;
    private final SeasonCalendarService seasonCalendarService;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    @Autowired
    public LiveComposer(ScoreboardPort scoreboardPort,
                        SduiUtils utils,
                        SectionSurfaces surfaces,
                        Tokens tokens,
                        SectionRefreshService sectionRefreshService,
                        ParameterizedRefreshService parameterizedRefreshService,
                        SeasonCalendarService seasonCalendarService) {
        this.scoreboardPort = scoreboardPort;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(tokens);
        this.sectionRefreshService = sectionRefreshService;
        this.parameterizedRefreshService = parameterizedRefreshService;
        this.seasonCalendarService = seasonCalendarService;
    }

    @PostConstruct
    private void registerResolvers() {
        String gamesCardPrefix = SectionIdDeriver.prefixFor("games:card-");
        sectionRefreshService.registerResolver(gamesCardPrefix, (id, ctx) -> {
            SectionIdDeriver.Parsed parsed = SectionIdDeriver.parse(id);
            String source = parsed.source();
            if (source == null || !source.startsWith(gamesCardPrefix)) {
                log.warn("Games card refresh misroute for sectionId='{}' source='{}'", id, source);
                return null;
            }

            String gameId = source.substring(gamesCardPrefix.length());
            if (gameId.isBlank()) {
                log.warn("Games card refresh missing gameId in sectionId='{}'", id);
                return null;
            }

            ScoreboardResponse scoreboard = safeGetScoreboard();
            if (scoreboard == null || scoreboard.getGames() == null) {
                log.warn("Games card refresh scoreboard unavailable for gameId='{}' sectionId='{}'", gameId, id);
                return null;
            }

            for (Game game : scoreboard.getGames()) {
                if (gameId.equals(game.getGameId())) {
                    return buildGameCardSection(game);
                }
            }

            log.info("Games card refresh gameId='{}' not found in current slate for sectionId='{}'", gameId, id);
            return null;
        });

        parameterizedRefreshService.registerResolver("games",
                (traceId, params, ctx) -> composeLive(
                        traceId,
                        ctx.getLocale() != null ? ctx.getLocale() : "en",
                        params.get("date")));
    }

    public Screen composeLive(String traceId, String locale) {
        return composeLive(traceId, locale, null);
    }

    /**
     * Compose the Games screen. When {@code selectedDateOverride} is non-null it
     * becomes the strip's selected date and the seeded screen state — used by the
     * parameterized-refresh resolver on date tap.
     *
     * <p>The override is parsed as a {@link LocalDate}. If it falls outside the
     * season window or fails to parse, composition falls back to today's league
     * date for both fetching and state echo — this resets the client's strip to
     * today, which is safe because out-of-bounds dates can only arrive from
     * unexpected deep links or forged requests (the strip's own minDate/maxDate
     * prevents normal navigation outside the season).
     */
    public Screen composeLive(String traceId, String locale, String selectedDateOverride) {
        log.info("Composing Games screen, locale={}, dateOverride={}", locale, selectedDateOverride);

        Screen response = new Screen();
        response.setId("games");
        response.setAnalyticsId("games");
        response.setSchemaVersion(schemaVersion);
        utils.applyTabDestinationNavigation(response, "games");

        // Always fetch the CDN today scoreboard first — it is the authoritative
        // source for the league's current game date. The CDN's gameDate field
        // already accounts for the league's roll-forward semantics (late-night
        // games still count as the prior day until the league rolls over), so
        // it produces a more meaningful default than wall-clock midnight ET.
        ScoreboardResponse todayScoreboard = safeGetScoreboard();
        LocalDate today = resolveTodayFromScoreboard(todayScoreboard);
        String defaultDate = today.toString();
        LocalDate fetchDate = resolveRequestedDate(selectedDateOverride, today);
        String selectedDate = fetchDate.toString();

        State state = new State();
        state.setAdditionalProperty("games_selected_date", selectedDate);
        response.setState(state);

        List<Section> sections = new ArrayList<>();

        // 0. CalendarStrip — date picker driving parameterized refresh
        sections.add(buildCalendarStripSection(selectedDate, defaultDate));

        // 1. League Pass promo banner — always rendered directly under the
        //    CalendarStrip, mirroring the production NBA Games tab. Server
        //    owns copy, art, and target URI; CMS can later swap in a different
        //    contentSourceId without a client release.
        sections.add(buildLeaguePassPromoBanner());

        // Reuse the already-fetched CDN response when the selection is today;
        // otherwise the Core API gameCardFeed serves per-date scoreboards.
        ScoreboardResponse scoreboard = fetchDate.equals(today)
                ? todayScoreboard
                : safeGetScoreboardForDate(fetchDate);
        java.util.List<Game> games = (scoreboard != null)
                ? scoreboard.getGames()
                : java.util.List.of();

        // Partition games by status: 2=live, 1=upcoming, 3+=final.
        List<Game> liveGames = new ArrayList<>();
        List<Game> upcomingGames = new ArrayList<>();
        List<Game> finishedGames = new ArrayList<>();

        for (Game game : games) {
            int status = game.getGameStatus();
            switch (status) {
                case 2 -> liveGames.add(game);
                case 1 -> upcomingGames.add(game);
                default -> finishedGames.add(game);
            }
        }

        List<Game> orderedGames = new ArrayList<>(games.size());
        orderedGames.addAll(liveGames);
        orderedGames.addAll(upcomingGames);
        orderedGames.addAll(finishedGames);
        for (Game game : orderedGames) {
            sections.add(buildGameCardSection(game));
        }

        insertGamesScreenAdSlot(sections, orderedGames.size());

        // Stamp a uniform bottom margin on every section so cards on the games
        // screen render with visible separation rather than flush. Margin is a
        // SectionSurface concern (shared SectionContainer applies it); per-screen
        // composer policy decides the token.
        applyInterSectionMargin(sections, tokens.spacing("md"));

        response.setSections(sections);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    /**
     * Add {@code surface.margin.bottom = token} to every section in the list,
     * preserving any existing surface fields. Used by the games screen to
     * separate adjacent section cards uniformly. A composer that already set
     * a {@code surface.margin.bottom} on a specific section keeps its value —
     * this pass only fills in the missing edge so per-section choices win.
     */
    private void applyInterSectionMargin(List<Section> sections, String token) {
        for (Section section : sections) {
            SectionSurface surface = section.getSurface();
            if (surface == null) {
                surface = new SectionSurface();
                section.setSurface(surface);
            }
            Spacing margin = surface.getMargin();
            if (margin == null) {
                margin = new Spacing();
                surface.setMargin(margin);
            }
            if (margin.getBottom() == null) {
                margin.setBottom(token);
            }
        }
    }

    /**
     * Read the authoritative "today" date from the CDN scoreboard payload's
     * {@code scoreboard.gameDate} field. Falls back to the wall-clock ET date
     * if the field is missing or unparseable (e.g. CDN call failed).
     */
    private LocalDate resolveTodayFromScoreboard(ScoreboardResponse todayScoreboard) {
        if (todayScoreboard != null) {
            String gameDate = todayScoreboard.getGameDate();
            if (gameDate != null && !gameDate.isBlank()) {
                try {
                    return LocalDate.parse(gameDate);
                } catch (Exception e) {
                    log.warn("Could not parse scoreboard.gameDate='{}'; falling back to wall-clock today", gameDate);
                }
            }
        }
        return seasonCalendarService.currentLeagueDate();
    }

    // ── Date resolution ─────────────────────────────────────────────────

    /**
     * Parse the user-requested date override and clamp to the season window.
     *
     * <p>Out-of-bounds dates are clamped to today's league date and a warning
     * is logged. The strip's minDate/maxDate prevents normal navigation outside
     * the season, so out-of-bounds means an unexpected deep link or forged
     * request — resetting to today is the safe behavior (the echoed date in
     * screen state resets the client's strip selection as well).
     */
    private LocalDate resolveRequestedDate(String selectedDateOverride, LocalDate today) {
        if (selectedDateOverride == null || selectedDateOverride.isBlank()) {
            return today;
        }
        try {
            LocalDate parsed = LocalDate.parse(selectedDateOverride);
            if (!seasonCalendarService.isInSeason(parsed)) {
                log.warn("Requested date {} is outside season [{}, {}]; falling back to today ({})",
                        parsed, seasonCalendarService.seasonStart(), seasonCalendarService.seasonEnd(), today);
                return today;
            }
            return parsed;
        } catch (Exception e) {
            log.warn("Failed to parse selectedDateOverride='{}'; falling back to today ({}): {}",
                    selectedDateOverride, today, e.getMessage());
            return today;
        }
    }

    /**
     * Fetch scoreboard for the given date via {@link ScoreboardPort#getScoreboardForDate}.
     * Swallows exceptions so composition can fall through to the mock path.
     */
    private ScoreboardResponse safeGetScoreboardForDate(LocalDate date) {
        try {
            return scoreboardPort.getScoreboardForDate(date);
        } catch (Exception e) {
            log.warn("Could not fetch scoreboard for date={}: {}", date, e.getMessage());
            return null;
        }
    }

    // ── CalendarStrip builder ────────────────────────────────────────────

    private Section buildCalendarStripSection(String selectedDate, String defaultDate) {
        String contentSourceId = "server:games-calendar";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "CalendarStrip");

        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.CALENDAR_STRIP);
        section.setContentSourceId(contentSourceId);
        section.setAnalyticsId("games_calendar_strip");

        AccessibilityProperties accessibility = new AccessibilityProperties();
        accessibility.setLabel("Games date picker");
        section.setAccessibility(accessibility);

        CalendarStrip data = new CalendarStrip();
        data.setStateKey("games_selected_date");
        data.setSelectedDate(selectedDate);
        data.setDefaultDate(defaultDate);
        data.setMinDate(seasonCalendarService.seasonStart().toString());
        data.setMaxDate(seasonCalendarService.seasonEnd().toString());

        Action onDateSelected = new Action();
        onDateSelected.setTrigger(Action.ActionTrigger.ON_ACTIVATE);
        onDateSelected.setType(Action.ActionType.REFRESH);
        onDateSelected.setEndpoint("/v1/sdui/screen/games");
        ParamBindings paramBindings = new ParamBindings();
        paramBindings.setAdditionalProperty("date", "{{games_selected_date}}");
        onDateSelected.setParamBindings(paramBindings);
        data.setOnDateSelected(onDateSelected);

        Action expandedAction = new Action();
        expandedAction.setTrigger(Action.ActionTrigger.ON_ACTIVATE);
        expandedAction.setType(Action.ActionType.NAVIGATE);
        expandedAction.setTargetUri("nba://calendar");
        data.setExpandedAction(expandedAction);

        section.setData(data);
        return section;
    }

    // ── Section builders ───────────────────────────────────────────────

    private Section buildGameCardSection(Game game) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        String contentSourceId = "games:card-" + gameId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, Section.Type.ATOMIC_COMPOSITE.value());
        int status = game.getGameStatus();

        List<RefreshPolicy> refreshPolicies;
        DataBinding dataBinding = null;
        java.util.Map<String, AtomicCompositeBuilder.GameClockSnapshot> clockSnapshots = null;

        if (status == 2) {
            RefreshPolicy sectionPoll = new RefreshPolicy()
                    .withType(RefreshPolicy.RefreshType.POLL)
                    .withSectionEndpoint("/v1/sdui/section/" + sectionId)
                    .withIntervalMs(60_000)
                    .withPauseWhenOffScreen(true);
            refreshPolicies = List.of(ssePolicy(game), sectionPoll);
            dataBinding = buildScheduleListBindings(List.of(game));
            clockSnapshots = java.util.Map.of(gameId, clockSnapshotFromGame(game));
        } else if (status == 1) {
            RefreshPolicy sectionPoll = new RefreshPolicy()
                    .withType(RefreshPolicy.RefreshType.POLL)
                    .withSectionEndpoint("/v1/sdui/section/" + sectionId)
                    .withIntervalMs(300_000)
                    .withPauseWhenOffScreen(true);
            refreshPolicies = List.of(sectionPoll);
        } else {
            refreshPolicies = List.of(staticPolicy());
        }

        Section section = atomicBuilder.buildGameScheduleList(
                sectionId,
                "games_game_" + gameId,
                null,
                new String[][]{ gameToRow(game) },
                null,
                dataBinding,
                clockSnapshots);
        section.setRefreshPolicy(refreshPolicies);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.gameCardFlushSurface());
        return section;
    }

    // ── Row conversion ─────────────────────────────────────────────────

    /**
     * Convert a coreapi game JSON node into the row format expected by
     * {@code buildGameScheduleList}:
     * [id, awayTri, awayName, awaySeed, awayScore, awayLogoUrl,
     *  homeTri, homeName, homeSeed, homeScore, homeLogoUrl,
     *  statusText, seriesText, broadcastLogos, targetUri, overflowUri]
     */
    private String[] gameToRow(Game game) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        ScoreboardTeam away = game.getAwayTeam();
        ScoreboardTeam home = game.getHomeTeam();
        int gameStatus = game.getGameStatus();

        String awayScore = gameStatus >= 2 && away != null
                ? String.valueOf(away.getScore()) : null;
        String homeScore = gameStatus >= 2 && home != null
                ? String.valueOf(home.getScore()) : null;

        String awayTri = away != null && away.getTeamTricode() != null ? away.getTeamTricode() : "";
        String awayName = away != null && away.getTeamName() != null ? away.getTeamName() : "";
        String awayId = away != null && away.getTeamId() != null ? away.getTeamId() : "";
        String homeTri = home != null && home.getTeamTricode() != null ? home.getTeamTricode() : "";
        String homeName = home != null && home.getTeamName() != null ? home.getTeamName() : "";
        String homeId = home != null && home.getTeamId() != null ? home.getTeamId() : "";

        return new String[]{
                gameId,
                awayTri,
                awayName,
                resolveSeed(away, game, /* isAway */ true),
                awayScore,
                SduiUtils.teamLogoUrl(awayId),
                homeTri,
                homeName,
                resolveSeed(home, game, /* isAway */ false),
                homeScore,
                SduiUtils.teamLogoUrl(homeId),
                game.getGameStatusText() != null ? game.getGameStatusText() : "",
                resolveSeriesText(game),
                resolveBroadcastText(game),
                "nba://game/" + gameId,
                null  // overflowUri
        };
    }

    /**
     * Resolve a playoff seed/rank string for a team from upstream game JSON. Returns a
     * 1-2 character seed (e.g. "3", "4") when present on the team payload or on the
     * surrounding {@code playoffSeries} block, or null for regular-season games where
     * upstream omits the field. No client-side fallback — composers only render the seed
     * prefix when this is non-null.
     */
    private String resolveSeed(ScoreboardTeam team,
                               Game game,
                               boolean isAway) {
        if (team != null) {
            String[] candidates = {team.getPlayoffRank(), team.getSeed(), team.getPlayoffSeed()};
            for (String v : candidates) {
                if (v != null && !v.isBlank() && !"0".equals(v)) return v;
            }
        }
        PlayoffSeries series = game.getPlayoffSeries();
        if (series != null) {
            String v = isAway ? series.getAwayTeamSeed() : series.getHomeTeamSeed();
            if (v != null && !v.isBlank() && !"0".equals(v)) return v;
        }
        return null;
    }

    /**
     * Resolve a non-blank {@code seriesText} from upstream game JSON, or null when the
     * field is absent or empty. Clients render the row's series/context slot only when
     * this is non-null — no client-side fallback string.
     */
    private String resolveSeriesText(Game game) {
        String series = game.getSeriesText();
        if (series == null || series.isBlank()) return null;
        return series;
    }

    /**
     * Resolve a non-blank broadcaster string (e.g. "ESPN") from upstream game JSON, or
     * null when no broadcaster information is present. Prefers the national broadcaster
     * name when {@code broadcasters} is structured; otherwise falls back to a flat
     * {@code broadcasterText} string. Clients render the broadcast row only when this
     * returns a non-null value.
     */
    private String resolveBroadcastText(Game game) {
        Broadcasters broadcasters = game.getBroadcasters();
        if (broadcasters != null && broadcasters.getNationalTvBroadcasters() != null
                && !broadcasters.getNationalTvBroadcasters().isEmpty()) {
            com.nba.sdui.integration.model.scoreboard.Broadcaster first = broadcasters.getNationalTvBroadcasters().get(0);
            if (first != null) {
                String name = first.getBroadcasterDisplay();
                if (name == null || name.isBlank()) {
                    name = first.getBroadcasterAbbreviation();
                }
                if (name != null && !name.isBlank()) return name;
            }
        }
        String flat = game.getBroadcasterText();
        if (flat == null || flat.isBlank()) return null;
        return flat;
    }

    // ── Data binding for live schedule list ─────────────────────────────

    /**
     * Build per-row data bindings for all live games in the schedule list.
     * The section subscribes to a single SSE channel ({@code refreshPolicy.channel}
     * = first live game's linescore); only the row whose gameId matches the
     * channel's frames updates in real-time. Other rows refresh on the next
     * full screen composition.
     */
    private DataBinding buildScheduleListBindings(List<Game> liveGames) {
        DataBinding dataBinding = new DataBinding();
        java.util.List<DataBindingPath> bindings = new java.util.ArrayList<>();

        for (Game game : liveGames) {
            String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
            bindings.add(utils.bindingPath(
                    "$.homeTeam.score", "content." + gameId + ".homeScore"));
            bindings.add(utils.bindingPath(
                    "$.awayTeam.score", "content." + gameId + ".awayScore"));
            bindings.add(utils.bindingPath(
                    "$.gameStatusText", "content." + gameId + ".statusText"));
            bindings.add(utils.bindingPath(
                    "$.clock", "content." + gameId + ".clock", "liveClockSnapshot"));
        }

        dataBinding.setBindings(bindings);
        return dataBinding;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private ScoreboardResponse safeGetScoreboard() {
        try {
            return scoreboardPort.getScoreboard();
        } catch (Exception e) {
            log.warn("Could not fetch scoreboard for Games screen: {}", e.getMessage());
            return null;
        }
    }

    private RefreshPolicy staticPolicy() {
        return new RefreshPolicy().withType(RefreshPolicy.RefreshType.STATIC);
    }

    private RefreshPolicy ssePolicy(Game game) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        return new RefreshPolicy()
                .withType(RefreshPolicy.RefreshType.SSE)
                .withChannel(gameId + ":linescore")
                .withPauseWhenOffScreen(false);
    }

    /**
     * Build an initial LiveClock snapshot from upstream stats-api game JSON.
     * The gameClock string is ISO-8601 duration (e.g. PT04M32.00S).
     * Initial server payloads are rendered as paused snapshots; SSE/Ably
     * linescore frames set isRunning=true to start local interpolation.
     */
    private AtomicCompositeBuilder.GameClockSnapshot clockSnapshotFromGame(Game game) {
        int seconds = parseGameClockSeconds(game.getGameClock());
        return new AtomicCompositeBuilder.GameClockSnapshot(
                seconds,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                AtomicCompositeBuilder.INITIAL_CLOCK_RUNNING);
    }

    private static int parseGameClockSeconds(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            return (int) java.time.Duration.parse(iso).getSeconds();
        } catch (Exception e) {
            return 0;
        }
    }

    // ── Promo banner ─────────────────────────────────────────────────────

    /**
     * Build the League Pass promo banner that sits directly under the CalendarStrip
     * on the Games screen. Stable per-screen entry point that drives subscribers
     * into the commerce flow. Surface chrome (solid {@code bg.secondary} card
     * matching the game-card siblings below, {@code lg} inner padding) is owned
     * by {@link SectionSurfaces#promoCardSurface}, so the promo stacks with the
     * same visual rhythm as the live/upcoming/final game cards rather than
     * reading as a separate gradient hero.
     */
    private Section buildLeaguePassPromoBanner() {
        String contentSourceId = "cms:promo-games_screen-leaguepass";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildPromoBanner(
                sectionId,
                "games_screen_promo_banner",
                "NBA League Pass",
                "Every game. Every night.",
                "Stream out-of-market games live and on demand all season long.",
                // No leading thumbnail: the previous NBA-logo tile rendered
                // with a stray rounded border on iOS and pushed the row
                // alignment such that the Subscribe CTA was clipped below
                // the surface. Removing the image lets the row size to the
                // column's natural intrinsic height and keeps the chrome
                // identical across iOS / Android / Web.
                null,
                "Subscribe",
                "nba://commerce/leaguepass",
                tokens.color("nba.label.accent.live"),
                // Track the surface across light/dark themes — the promo
                // now sits on `bg.secondary` (same as the game cards), so
                // the headline uses the primary label token rather than
                // the theme-inverted one used by gradient hero variants.
                tokens.color("nba.label.primary"),
                tokens.color("nba.label.secondary"));
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.promoCardSurface(
                tokens.color("nba.bg.secondary"),
                tokens.spacing("lg")));
        return section;
    }

    // ── Ad-slot placement ──────────────────────────────────────────────

    /**
     * Insert a single Games-screen {@code AdSlot} section in {@code sections} per the
     * payload-owned placement rule:
     * <ul>
     *   <li>0 games: no ad emitted</li>
     *   <li>1 game: ad inserted after the section containing the first game</li>
     *   <li>2+ games: ad inserted after the section containing the second game</li>
     * </ul>
     *
     * <p>The roster order is live → upcoming → final, mirroring the on-screen card
     * order. All dimensions, sizes, and creative metadata are server-owned on the
     * emitted section.
     *
     * <p>The {@code sections} list passed in must already include the {@code CalendarStrip}
     * as the first entry, promo as the second entry, and game-card sections after that;
     * only game-card sections participate in ad placement.
     */
    private void insertGamesScreenAdSlot(List<Section> sections, int gameCardCount) {
        int totalGames = gameCardCount;
        if (totalGames == 0) return;

        int targetGameIndex = totalGames == 1 ? 1 : 2; // 1-based: after game N
        // CalendarStrip is index 0; League Pass promo banner is index 1;
        // game sections start at index 2.
        final int firstGameSectionIndex = 2;
        int insertAfter = firstGameSectionIndex + targetGameIndex - 1;

        Section adSection = buildGamesScreenAdSlot();
        sections.add(insertAfter + 1, adSection);
    }

    /**
     * Build the Games-screen {@code AdSlot} section. Payload-owned reservation dimensions
     * (320×50 mobile, 728×90 desktop) come from the server; the client never invents
     * fallback sizes. Surface chrome is owned by {@link SectionSurfaces#adSlotSurface()}.
     */
    private Section buildGamesScreenAdSlot() {
        String contentSourceId = "ads:gam-games_screen";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AdSlot");

        Section section = new Section();
        section.setId(sectionId);
        section.setType(Section.Type.AD_SLOT);
        section.setAnalyticsId("games_screen_ad");
        section.setContentSourceId(contentSourceId);

        RefreshPolicy refreshPolicy = new RefreshPolicy();
        refreshPolicy.setType(RefreshPolicy.RefreshType.STATIC);
        section.setRefreshPolicy(List.of(refreshPolicy));

        AdSlot data = new AdSlot();
        data.setProvider("gam");
        data.setAdUnitPath("/nba/games_screen");
        data.setSizes(List.of(List.of(320, 50), List.of(728, 90)));

        Targeting targeting = new Targeting();
        targeting.setAdditionalProperty("section", "games");
        targeting.setAdditionalProperty("position", "games_screen");
        data.setTargeting(targeting);

        data.setCollapseOnEmpty(true);
        data.setLabel("Advertisement");

        Placeholder placeholder = new Placeholder();
        placeholder.setBackgroundColor("token:nba.bg.tertiary");
        placeholder.setText("Advertisement");
        data.setPlaceholder(placeholder);

        section.setData(data);
        section.setSurface(surfaces.adSlotSurface());
        return section;
    }
}
