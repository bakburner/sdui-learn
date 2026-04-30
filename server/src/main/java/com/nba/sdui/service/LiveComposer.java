package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Composes the "Games" SDUI screen — all live, upcoming & final games.
 *
 * Layout:
 *   1. GamePanel (featured) – hero card for the top live game (SSE refresh)
 *   2. GameScheduleList     – "Live Now" compact list (SSE per-row clock countdown)
 *   3. GameScheduleList     – "Upcoming Today" (static)
 *   4. GameScheduleList     – "Final" (static)
 *
 * Live rows use per-game SSE channels bound via dataBinding so the LiveClock
 * element counts down in real-time. When the Ably/SSE channel delivers a
 * linescore frame, the client writes clock.isRunning=true and the clock starts
 * local interpolation.
 */
@Component
public class LiveComposer {

    private static final Logger log = LoggerFactory.getLogger(LiveComposer.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public LiveComposer(ObjectMapper objectMapper,
                        StatsApiClient statsApiClient,
                        SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    public JsonNode composeLive(String traceId, String locale) {
        log.info("Composing Games screen, locale={}", locale);

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "games");
        response.put("title", "Games");
        response.put("analyticsId", "games");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("games"));

        ArrayNode sections = objectMapper.createArrayNode();

        JsonNode scoreboard = safeGetScoreboard();
        JsonNode games = (scoreboard != null)
                ? scoreboard.path("scoreboard").path("games")
                : objectMapper.createArrayNode();

        // Partition games by status
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

        // 1. Featured game — hero card for top live game (SSE refresh)
        JsonNode heroGame = !liveGames.isEmpty() ? liveGames.get(0)
                : (!upcomingGames.isEmpty() ? upcomingGames.get(0) : null);
        if (heroGame != null) {
            sections.add(buildFeaturedGamePanel(heroGame));
        } else {
            sections.add(buildMockFeaturedGame());
        }

        // 2. "Live Now" — compact schedule list with per-row SSE clock
        if (!liveGames.isEmpty()) {
            sections.add(buildLiveScheduleList(liveGames));
        }

        // 3. "Upcoming Today" — static schedule list
        if (!upcomingGames.isEmpty()) {
            sections.add(buildScheduleList("upcoming-games", "upcoming_games",
                    "Upcoming Today", upcomingGames, false));
        }

        // 4. "Final" — static schedule list
        if (!finishedGames.isEmpty()) {
            sections.add(buildScheduleList("final-games", "final_games",
                    "Final", finishedGames, false));
        }

        // If no real data, add mock sections
        if (liveGames.isEmpty() && upcomingGames.isEmpty() && finishedGames.isEmpty()) {
            addMockSections(sections);
        }

        response.set("sections", sections);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    /**
     * Build the live games schedule list with SSE per-row data binding.
     * Each live game row gets a LiveClock element that counts down via
     * its per-game SSE channel.
     */
    private ObjectNode buildLiveScheduleList(List<JsonNode> liveGames) {
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
                "live-games", "live_games", "Live Now", rows,
                refreshPolicy, dataBinding, clockSnapshots);
        section.set("surface", utils.cardSurface());
        return section;
    }

    /**
     * Build a static (or non-live) schedule list for upcoming / final games.
     */
    private ObjectNode buildScheduleList(String sectionId, String analyticsId,
                                         String title, List<JsonNode> gamesList,
                                         boolean live) {
        String[][] rows = new String[gamesList.size()][];
        for (int i = 0; i < gamesList.size(); i++) {
            rows[i] = gameToRow(gamesList.get(i));
        }
        ObjectNode section = atomicBuilder.buildGameScheduleList(
                sectionId, analyticsId, title, rows, staticPolicy(), null);
        section.set("surface", utils.cardSurface());
        return section;
    }

    private ObjectNode buildFeaturedGamePanel(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);
        boolean live = gameStatus == 2;

        ObjectNode refreshPolicy = live ? ssePolicy(game) : staticPolicy();
        ObjectNode bindings = live ? utils.buildCompositeLinescoreBindings() : null;
        AtomicCompositeBuilder.GameClockSnapshot clock = live ? clockSnapshotFromGame(game) : null;

        return atomicBuilder.buildGamePanelComposite(
                "live-featured-" + gameId,
                "live_featured_game",
                "featured",
                gameId,
                gameStatus,
                game.path("gameStatusText").asText(""),
                live ? "LIVE" : "UP NEXT",
                atomicBuilder.gamePanelTeamFromJson(game.path("awayTeam")),
                atomicBuilder.gamePanelTeamFromJson(game.path("homeTeam")),
                clock,
                "nba://game/" + gameId,
                refreshPolicy,
                bindings,
                utils.gamePanelSurface());
    }

    private ObjectNode buildMockFeaturedGame() {
        return atomicBuilder.buildGamePanelComposite(
                "live-featured-mock",
                "live_featured_game",
                "featured",
                "0022400050",
                1,
                "7:30 PM ET",
                "NEXT UP",
                new AtomicCompositeBuilder.GamePanelTeam(
                        "MIA", 0, SduiUtils.teamLogoUrl("1610612748")),
                new AtomicCompositeBuilder.GamePanelTeam(
                        "NYK", 0, SduiUtils.teamLogoUrl("1610612752")),
                null,
                "nba://game/0022400050",
                staticPolicy(),
                null,
                utils.gamePanelSurface());
    }

    /**
     * Mock sections for when the CDN scoreboard is unreachable.
     */
    private void addMockSections(ArrayNode sections) {
        // Mock live schedule list
        String[][] liveRows = {
                {"mock-live-1", "BOS", "Celtics", null,
                        "72", SduiUtils.teamLogoUrl("1610612738"),
                        "LAL", "Lakers", null,
                        "68", SduiUtils.teamLogoUrl("1610612747"),
                        "Q3 4:22", null, null, "nba://game/mock-live-1", null},
                {"mock-live-2", "GSW", "Warriors", null,
                        "31", SduiUtils.teamLogoUrl("1610612744"),
                        "PHX", "Suns", null,
                        "28", SduiUtils.teamLogoUrl("1610612756"),
                        "Q1 9:15", null, null, "nba://game/mock-live-2", null}
        };
        Map<String, AtomicCompositeBuilder.GameClockSnapshot> mockClocks = new HashMap<>();
        mockClocks.put("mock-live-1", mockClockSnapshotFromStatus("Q3 4:22"));
        mockClocks.put("mock-live-2", mockClockSnapshotFromStatus("Q1 9:15"));

        ObjectNode mockSsePolicy = objectMapper.createObjectNode();
        mockSsePolicy.put("type", "sse");
        mockSsePolicy.put("channel", "mock-live-1:linescore");
        mockSsePolicy.put("pauseWhenOffScreen", false);

        ObjectNode liveSection = atomicBuilder.buildGameScheduleList(
                "live-games", "live_games", "Live Now", liveRows,
                mockSsePolicy, null, mockClocks);
        liveSection.set("surface", utils.cardSurface());
        sections.add(liveSection);

        // Mock upcoming schedule list
        String[][] upcomingRows = {
                {"mock-up-1", "MIL", "Bucks", null,
                        null, SduiUtils.teamLogoUrl("1610612749"),
                        "DEN", "Nuggets", null,
                        null, SduiUtils.teamLogoUrl("1610612743"),
                        "8:00 PM ET", null, null, "nba://game/mock-up-1", null},
                {"mock-up-2", "DAL", "Mavericks", null,
                        null, SduiUtils.teamLogoUrl("1610612742"),
                        "MIA", "Heat", null,
                        null, SduiUtils.teamLogoUrl("1610612748"),
                        "10:00 PM ET", null, null, "nba://game/mock-up-2", null}
        };
        ObjectNode upcomingSection = atomicBuilder.buildGameScheduleList(
                "upcoming-games", "upcoming_games", "Upcoming Today", upcomingRows,
                staticPolicy(), null);
        upcomingSection.set("surface", utils.cardSurface());
        sections.add(upcomingSection);
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
                    "$.gameClock", "content." + gameId + ".clock", "liveClockSnapshot"));
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
                AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING);
    }

    private AtomicCompositeBuilder.GameClockSnapshot mockClockSnapshotFromStatus(String statusText) {
        int seconds = parseMockClockSeconds(statusText);
        return new AtomicCompositeBuilder.GameClockSnapshot(
                seconds,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING);
    }

    private static int parseGameClockSeconds(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            return (int) java.time.Duration.parse(iso).getSeconds();
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseMockClockSeconds(String statusText) {
        if (statusText == null) return 0;
        int colon = statusText.lastIndexOf(':');
        if (colon <= 0) return 0;
        try {
            int m = Integer.parseInt(statusText.substring(
                    Math.max(0, colon - 2), colon).trim()
                    .replaceAll("[^0-9]", ""));
            int s = Integer.parseInt(statusText.substring(
                    colon + 1, Math.min(statusText.length(), colon + 3)).trim()
                    .replaceAll("[^0-9]", ""));
            return m * 60 + s;
        } catch (Exception e) {
            return 0;
        }
    }
}
