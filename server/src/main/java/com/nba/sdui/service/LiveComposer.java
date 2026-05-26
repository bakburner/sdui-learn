package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
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

/**
 * Composes the "Games" SDUI screen — live, upcoming & final games for a
 * selected date.
 *
 * Layout:
 *   0. CalendarStrip        – horizontal date picker; parameterized refresh
 *   1. GameScheduleList     – "Live Now" compact list (SSE per-row clock countdown)
 *   2. GameScheduleList     – "Upcoming" (static)
 *   3. GameScheduleList     – "Final" (static)
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
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final AtomicCompositeBuilder atomicBuilder;
    private final SectionRefreshService sectionRefreshService;
    private final ParameterizedRefreshService parameterizedRefreshService;
    private final SeasonCalendarService seasonCalendarService;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    @Autowired
    public LiveComposer(ObjectMapper objectMapper,
                        StatsApiClient statsApiClient,
                        SduiUtils utils,
                        SectionSurfaces surfaces,
                        SectionRefreshService sectionRefreshService,
                        ParameterizedRefreshService parameterizedRefreshService,
                        SeasonCalendarService seasonCalendarService) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.surfaces = surfaces;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
        this.sectionRefreshService = sectionRefreshService;
        this.parameterizedRefreshService = parameterizedRefreshService;
        this.seasonCalendarService = seasonCalendarService;
    }

    @PostConstruct
    private void registerResolvers() {
        String sectionId = SectionIdDeriver.derive("stats-api:live-games", "GameScheduleList");
        sectionRefreshService.registerResolver(sectionId, (id, ctx) -> {
            JsonNode scoreboard = safeGetScoreboard();
            List<JsonNode> liveGames = new ArrayList<>();
            if (scoreboard != null) {
                for (JsonNode game : scoreboard.path("scoreboard").path("games")) {
                    if (game.path("gameStatus").asInt(1) == 2) {
                        liveGames.add(game);
                    }
                }
            }
            return liveGames.isEmpty() ? buildEmptyLiveScheduleList() : buildLiveScheduleList(liveGames);
        });

        parameterizedRefreshService.registerResolver("games",
                (traceId, params, ctx) -> composeLive(
                        traceId,
                        ctx.getLocale() != null ? ctx.getLocale() : "en",
                        params.get("date")));
    }

    public ObjectNode composeLive(String traceId, String locale) {
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
    public ObjectNode composeLive(String traceId, String locale, String selectedDateOverride) {
        log.info("Composing Games screen, locale={}, dateOverride={}", locale, selectedDateOverride);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "games");
        response.put("analyticsId", "games");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        utils.applyTabDestinationNavigation(response, "games");

        // Always fetch the CDN today scoreboard first — it is the authoritative
        // source for the league's current game date. The CDN's gameDate field
        // already accounts for the league's roll-forward semantics (late-night
        // games still count as the prior day until the league rolls over), so
        // it produces a more meaningful default than wall-clock midnight ET.
        JsonNode todayScoreboard = safeGetScoreboard();
        LocalDate today = resolveTodayFromScoreboard(todayScoreboard);
        String defaultDate = today.toString();
        LocalDate fetchDate = resolveRequestedDate(selectedDateOverride, today);
        String selectedDate = fetchDate.toString();

        ObjectNode state = objectMapper.createObjectNode();
        state.put("games_selected_date", selectedDate);
        response.set("state", state);

        ArrayNode sections = objectMapper.createArrayNode();

        // 0. CalendarStrip — date picker driving parameterized refresh
        sections.add(buildCalendarStripSection(selectedDate, defaultDate));

        // Reuse the already-fetched CDN response when the selection is today;
        // otherwise the Core API gameCardFeed serves per-date scoreboards.
        JsonNode scoreboard = fetchDate.equals(today)
                ? todayScoreboard
                : safeGetScoreboardForDate(fetchDate);
        JsonNode games = (scoreboard != null)
                ? scoreboard.path("scoreboard").path("games")
                : objectMapper.createArrayNode();

        // Partition games by status: 2=live, 1=upcoming, 3+=final.
        List<JsonNode> liveGames = new ArrayList<>();
        List<JsonNode> upcomingGames = new ArrayList<>();
        List<JsonNode> finishedGames = new ArrayList<>();

        for (JsonNode game : games) {
            int status = game.path("gameStatus").asInt(1);
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

        response.set("sections", sections);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    /**
     * Read the authoritative "today" date from the CDN scoreboard payload's
     * {@code scoreboard.gameDate} field. Falls back to the wall-clock ET date
     * if the field is missing or unparseable (e.g. CDN call failed).
     */
    private LocalDate resolveTodayFromScoreboard(JsonNode todayScoreboard) {
        if (todayScoreboard != null) {
            String gameDate = todayScoreboard.path("scoreboard").path("gameDate").asText(null);
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
     * Fetch scoreboard for the given date via {@link StatsApiClient#getScoreboardForDate}.
     * Swallows exceptions so composition can fall through to the mock path.
     */
    private JsonNode safeGetScoreboardForDate(LocalDate date) {
        try {
            return statsApiClient.getScoreboardForDate(date);
        } catch (Exception e) {
            log.warn("Could not fetch scoreboard for date={}: {}", date, e.getMessage());
            return null;
        }
    }

    // ── CalendarStrip builder ────────────────────────────────────────────

    private ObjectNode buildCalendarStripSection(String selectedDate, String defaultDate) {
        String contentSourceId = "server:games-calendar";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "CalendarStrip");

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", sectionId);
        section.put("type", "CalendarStrip");
        section.put("contentSourceId", contentSourceId);
        section.put("analyticsId", "games_calendar_strip");

        ObjectNode accessibility = objectMapper.createObjectNode();
        accessibility.put("label", "Games date picker");
        section.set("accessibility", accessibility);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "games_selected_date");
        data.put("selectedDate", selectedDate);
        data.put("defaultDate", defaultDate);
        data.put("minDate", seasonCalendarService.seasonStart().toString());
        data.put("maxDate", seasonCalendarService.seasonEnd().toString());

        ObjectNode onDateSelected = objectMapper.createObjectNode();
        onDateSelected.put("trigger", "onActivate");
        onDateSelected.put("type", "refresh");
        onDateSelected.put("endpoint", "/v1/sdui/screen/games");
        ObjectNode paramBindings = objectMapper.createObjectNode();
        paramBindings.put("date", "{{games_selected_date}}");
        onDateSelected.set("paramBindings", paramBindings);
        data.set("onDateSelected", onDateSelected);

        section.set("data", data);
        return section;
    }

    // ── Section builders ───────────────────────────────────────────────

    /**
     * Build the live games schedule list with SSE per-row data binding.
     * Each live game row gets a LiveClock element that counts down via
     * its per-game SSE channel.
     */
    private ObjectNode buildLiveScheduleList(List<JsonNode> liveGames) {
        String contentSourceId = "stats-api:live-games";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "GameScheduleList");

        String[][] rows = new String[liveGames.size()][];
        Map<String, AtomicCompositeBuilder.GameClockSnapshot> clockSnapshots = new HashMap<>();

        for (int i = 0; i < liveGames.size(); i++) {
            JsonNode game = liveGames.get(i);
            String gameId = game.path("gameId").asText("0000000000");
            rows[i] = gameToRow(game);
            clockSnapshots.put(gameId, clockSnapshotFromGame(game));
        }

        // SSE refresh on the first live game's channel; all rows get clock
        // snapshots seeded from the initial composition. Subsequent Ably/SSE
        // frames update per-row content via the dataBinding map.
        ObjectNode refreshPolicy = ssePolicy(liveGames.get(0));
        ObjectNode dataBinding = buildScheduleListBindings(liveGames);

        ObjectNode section = atomicBuilder.buildGameScheduleList(
                sectionId, "live_games", "Live Now", rows,
                refreshPolicy, dataBinding, clockSnapshots);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.flushSurface());
        return section;
    }

    /**
     * Empty live schedule list returned by the section-refresh resolver when
     * all previously-live games have concluded. We can't emit SSE refresh on
     * "no games", so the section drops back to a static poll so the client
     * can keep checking. Composition will omit the section entirely on the
     * next full screen fetch.
     */
    private ObjectNode buildEmptyLiveScheduleList() {
        String contentSourceId = "stats-api:live-games";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "GameScheduleList");

        ObjectNode pollPolicy = objectMapper.createObjectNode();
        pollPolicy.put("type", "poll");
        pollPolicy.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
        pollPolicy.put("intervalMs", 30_000);
        pollPolicy.put("pauseWhenOffScreen", true);

        ObjectNode section = atomicBuilder.buildGameScheduleList(
                sectionId, "live_games", "Live Now", new String[0][],
                pollPolicy, null);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.flushSurface());
        return section;
    }

    /**
     * Build a static (or non-live) schedule list for upcoming / final games.
     * Uses the 3-arg SectionIdDeriver form because multiple schedule lists
     * share the same contentSourceId + sectionType (slug disambiguates).
     */
    private ObjectNode buildScheduleList(String slug, String analyticsId,
                                         String title, List<JsonNode> gamesList,
                                         boolean live) {
        String contentSourceId = "stats-api:scoreboard";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "GameScheduleList", slug);

        String[][] rows = new String[gamesList.size()][];
        for (int i = 0; i < gamesList.size(); i++) {
            rows[i] = gameToRow(gamesList.get(i));
        }
        ObjectNode section = atomicBuilder.buildGameScheduleList(
                sectionId, analyticsId, title, rows, staticPolicy(), null);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.flushSurface());
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
    private String[] gameToRow(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        JsonNode away = game.path("awayTeam");
        JsonNode home = game.path("homeTeam");
        int gameStatus = game.path("gameStatus").asInt(1);

        String awayScore = gameStatus >= 2
                ? String.valueOf(away.path("score").asInt(0)) : null;
        String homeScore = gameStatus >= 2
                ? String.valueOf(home.path("score").asInt(0)) : null;

        return new String[]{
                gameId,
                away.path("teamTricode").asText(""),
                away.path("teamName").asText(""),
                null, // awaySeed
                awayScore,
                SduiUtils.teamLogoUrl(away.path("teamId").asText("")),
                home.path("teamTricode").asText(""),
                home.path("teamName").asText(""),
                null, // homeSeed
                homeScore,
                SduiUtils.teamLogoUrl(home.path("teamId").asText("")),
                game.path("gameStatusText").asText(""),
                null, // seriesText
                null, // broadcastLogos
                "nba://game/" + gameId,
                null  // overflowUri
        };
    }

    // ── Data binding for live schedule list ─────────────────────────────

    /**
     * Build per-row data bindings for all live games in the schedule list.
     * Each game's SSE channel pushes linescore frames that update scores
     * and the clock snapshot for its row.
     */
    private ObjectNode buildScheduleListBindings(List<JsonNode> liveGames) {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();

        for (JsonNode game : liveGames) {
            String gameId = game.path("gameId").asText("0000000000");
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
        for (JsonNode game : liveGames) {
            String gameId = game.path("gameId").asText("0000000000");
            channels.add(gameId + ":linescore");
        }
        dataBinding.set("channels", channels);
        return dataBinding;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private JsonNode safeGetScoreboard() {
        try {
            return statsApiClient.getScoreboard();
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

    private ObjectNode ssePolicy(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
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
    private AtomicCompositeBuilder.GameClockSnapshot clockSnapshotFromGame(JsonNode game) {
        int seconds = parseGameClockSeconds(game.path("gameClock").asText(""));
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
}
