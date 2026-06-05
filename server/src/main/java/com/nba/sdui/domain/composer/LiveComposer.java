package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.models.generated.AccessibilityProperties;
import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.AdSlot;
import com.nba.sdui.models.generated.CalendarStrip;
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

    private final ObjectMapper objectMapper;
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
    public LiveComposer(ObjectMapper objectMapper,
                        ScoreboardPort scoreboardPort,
                        SduiUtils utils,
                        SectionSurfaces surfaces,
                        Tokens tokens,
                        SectionRefreshService sectionRefreshService,
                        ParameterizedRefreshService parameterizedRefreshService,
                        SeasonCalendarService seasonCalendarService) {
        this.objectMapper = objectMapper;
        this.scoreboardPort = scoreboardPort;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
        this.sectionRefreshService = sectionRefreshService;
        this.parameterizedRefreshService = parameterizedRefreshService;
        this.seasonCalendarService = seasonCalendarService;
    }

    @PostConstruct
    private void registerResolvers() {
        String sectionId = SectionIdDeriver.derive("stats-api:live-games", "GameScheduleList");
        sectionRefreshService.registerResolver(sectionId, (id, ctx) -> {
            ScoreboardResponse scoreboard = safeGetScoreboard();
            List<Game> liveGames = new ArrayList<>();
            if (scoreboard != null) {
                for (Game game : scoreboard.getGames()) {
                    if (game.getGameStatus() == 2) {
                        liveGames.add(game);
                    }
                }
            }
            Section section = liveGames.isEmpty()
                    ? buildEmptyLiveScheduleList()
                    : buildLiveScheduleList(liveGames);
            // Section channel is still ObjectNode-shaped at the SectionRefreshService
            // boundary (see plan-server-typed-pojo-migration.md, deferred to a later
            // step that flips the section channel to typed Section returns).
            return objectMapper.valueToTree(section);
        });

        parameterizedRefreshService.registerResolver("games",
                (traceId, params, ctx) -> {
                    Screen screen = composeLive(
                            traceId,
                            ctx.getLocale() != null ? ctx.getLocale() : "en",
                            params.get("date"));
                    // Parameterized refresh service is still ObjectNode-shaped at the
                    // boundary; flips to typed Screen in the later controller step.
                    return (ObjectNode) objectMapper.valueToTree(screen);
                });
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

        if (!liveGames.isEmpty()) {
            sections.add(buildLiveScheduleList(liveGames));
        }
        if (!upcomingGames.isEmpty()) {
            sections.add(buildScheduleList("upcoming-games", "upcoming_games",
                    "Upcoming", upcomingGames, false));
        }
        if (!finishedGames.isEmpty()) {
            sections.add(buildScheduleList("final-games", "final_games",
                    "Final", finishedGames, false));
        }

        insertGamesScreenAdSlot(sections, liveGames.size(), upcomingGames.size(), finishedGames.size());

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

    /**
     * Build the live games schedule list with SSE per-row data binding.
     * Each live game row gets a LiveClock element that counts down via
     * its per-game SSE channel.
     */
    private Section buildLiveScheduleList(List<Game> liveGames) {
        String contentSourceId = "stats-api:live-games";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "GameScheduleList");

        String[][] rows = new String[liveGames.size()][];
        Map<String, AtomicCompositeBuilder.GameClockSnapshot> clockSnapshots = new HashMap<>();

        for (int i = 0; i < liveGames.size(); i++) {
            Game game = liveGames.get(i);
            String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
            rows[i] = gameToRow(game);
            clockSnapshots.put(gameId, clockSnapshotFromGame(game));
        }

        // SSE refresh on the first live game's channel; all rows get clock
        // snapshots seeded from the initial composition. Subsequent Ably/SSE
        // frames update per-row content via the dataBinding map.
        ObjectNode refreshPolicy = ssePolicy(liveGames.get(0));
        ObjectNode dataBinding = buildScheduleListBindings(liveGames);

        Section section = atomicBuilder.buildGameScheduleList(
                sectionId, "live_games", "Live Now", rows,
                refreshPolicy, dataBinding, clockSnapshots);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.gameCardFlushSurface());
        return section;
    }

    /**
     * Empty live schedule list returned by the section-refresh resolver when
     * all previously-live games have concluded. We can't emit SSE refresh on
     * "no games", so the section drops back to a static poll so the client
     * can keep checking. Composition will omit the section entirely on the
     * next full screen fetch.
     */
    private Section buildEmptyLiveScheduleList() {
        String contentSourceId = "stats-api:live-games";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "GameScheduleList");

        ObjectNode pollPolicy = objectMapper.createObjectNode();
        pollPolicy.put("type", "poll");
        pollPolicy.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
        pollPolicy.put("intervalMs", 30_000);
        pollPolicy.put("pauseWhenOffScreen", true);

        Section section = atomicBuilder.buildGameScheduleList(
                sectionId, "live_games", "Live Now", new String[0][],
                pollPolicy, null);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.gameCardFlushSurface());
        return section;
    }

    /**
     * Build a static (or non-live) schedule list for upcoming / final games.
     * Uses the 3-arg SectionIdDeriver form because multiple schedule lists
     * share the same contentSourceId + sectionType (slug disambiguates).
     */
    private Section buildScheduleList(String slug, String analyticsId,
                                      String title, List<Game> gamesList,
                                      boolean live) {
        String contentSourceId = "stats-api:scoreboard";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "GameScheduleList", slug);

        String[][] rows = new String[gamesList.size()][];
        for (int i = 0; i < gamesList.size(); i++) {
            rows[i] = gameToRow(gamesList.get(i));
        }
        Section section = atomicBuilder.buildGameScheduleList(
                sectionId, analyticsId, title, rows, staticPolicy(), null);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.gameCardFlushSurface());
        return section;
    }

    private Section toSection(ObjectNode node) {
        try {
            return objectMapper.treeToValue(node, Section.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to bind composed section to Section.class", e);
        }
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
     * Each game's SSE channel pushes linescore frames that update scores
     * and the clock snapshot for its row.
     */
    private ObjectNode buildScheduleListBindings(List<Game> liveGames) {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();

        for (Game game : liveGames) {
            String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
            // Per-row score bindings from the SSE linescore frame
            bindings.add(utils.bindingPath(
                    "$.homeTeam.score", "content." + gameId + ".homeScore"));
            bindings.add(utils.bindingPath(
                    "$.awayTeam.score", "content." + gameId + ".awayScore"));
            bindings.add(utils.bindingPath(
                    "$.gameStatusText", "content." + gameId + ".statusText"));
            bindings.add(utils.bindingPath(
                    "$.clock", "content." + gameId + ".clock", "liveClockSnapshot"));
        }

        dataBinding.set("bindings", bindings);
        // Multi-channel: the section subscribes to all live game channels
        ArrayNode channels = objectMapper.createArrayNode();
        for (Game game : liveGames) {
            String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
            channels.add(gameId + ":linescore");
        }
        dataBinding.set("channels", channels);
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

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }

    private ObjectNode ssePolicy(Game game) {
        String gameId = game.getGameId() != null ? game.getGameId() : "0000000000";
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "sse");
        rp.put("channel", gameId + ":linescore");
        rp.put("pauseWhenOffScreen", false);
        return rp;
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
     * <p>The roster order is live → upcoming → final, mirroring the on-screen section
     * order. The ad is inserted at the section boundary that contains the Nth game so
     * that section integrity is preserved (we never split a status group). All
     * dimensions, sizes, and creative metadata are server-owned on the emitted section.
     *
     * <p>The {@code sections} list passed in must already include the {@code CalendarStrip}
     * as the first entry and the per-status game sections in roster order; only those
     * game sections participate in ad placement.
     */
    private void insertGamesScreenAdSlot(List<Section> sections, int liveCount, int upcomingCount, int finalCount) {
        int totalGames = liveCount + upcomingCount + finalCount;
        if (totalGames == 0) return;

        int targetGameIndex = totalGames == 1 ? 1 : 2; // 1-based: after game N
        int[] perStatusCounts = new int[]{ liveCount, upcomingCount, finalCount };

        int sectionIndex = 0; // section index in the input list, relative to the first game section
        // CalendarStrip is index 0; League Pass promo banner is index 1;
        // game sections start at index 2.
        final int firstGameSectionIndex = 2;
        int runningGameTotal = 0;
        int insertAfter = -1;
        for (int count : perStatusCounts) {
            if (count == 0) continue;
            runningGameTotal += count;
            if (runningGameTotal >= targetGameIndex) {
                insertAfter = firstGameSectionIndex + sectionIndex;
                break;
            }
            sectionIndex++;
        }
        if (insertAfter < 0) return; // defensive — totalGames > 0 guarantees a match

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
        section.setRefreshPolicy(refreshPolicy);

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
