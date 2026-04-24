package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Composes the "Games" SDUI screen — all live, upcoming & final games.
 *
 * Layout:
 *   1. GamePanel (featured) – hero card for the top live game (SSE refresh)
 *   2. SectionHeader    – "Live Now"
 *   3–N. GamePanel       – every live game (gameStatus == 2)
 *   N+1. SectionHeader  – "Upcoming Today"
 *   N+2–M. GamePanel     – every upcoming game (gameStatus == 1)
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
        ArrayNode liveGames = objectMapper.createArrayNode();
        ArrayNode upcomingGames = objectMapper.createArrayNode();
        ArrayNode finishedGames = objectMapper.createArrayNode();

        for (JsonNode game : games) {
            int status = game.path("gameStatus").asInt(1);
            switch (status) {
                case 2 -> liveGames.add(game);
                case 1 -> upcomingGames.add(game);
                default -> finishedGames.add(game);
            }
        }

        // 1. Featured game — first live game, else first upcoming
        JsonNode heroGame = liveGames.size() > 0 ? liveGames.get(0)
                : (upcomingGames.size() > 0 ? upcomingGames.get(0) : null);
        if (heroGame != null) {
            sections.add(buildFeaturedGamePanel(heroGame));
        } else {
            sections.add(buildMockFeaturedGame());
        }

        // 2. "Live Now" section
        if (liveGames.size() > 0) {
            sections.add(buildSectionHeader("live-now-header", "Live Now",
                    liveGames.size() + " Games", null));
            for (JsonNode g : liveGames) {
                sections.add(buildGamePanel(g, true));
            }
        }

        // 3. "Upcoming Today"
        if (upcomingGames.size() > 0) {
            sections.add(buildSectionHeader("upcoming-header", "Upcoming Today",
                    null, null));
            for (JsonNode g : upcomingGames) {
                sections.add(buildGamePanel(g, false));
            }
        }

        // 4. "Final" — completed games
        if (finishedGames.size() > 0) {
            sections.add(buildSectionHeader("final-header", "Final", null, null));
            for (JsonNode g : finishedGames) {
                sections.add(buildGamePanel(g, false));
            }
        }

        // If no real data, add mock sections
        if (liveGames.size() == 0 && upcomingGames.size() == 0 && finishedGames.size() == 0) {
            addMockSections(sections);
        }

        response.set("sections", sections);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildFeaturedGamePanel(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);
        boolean live = gameStatus == 2;

        ObjectNode refreshPolicy = live ? ssePolicy(gameId) : staticPolicy();
        ObjectNode bindings = live ? utils.buildCompositeLinescoreBindings() : null;
        AtomicCompositeBuilder.GameClockSnapshot clock = live ? clockSnapshotFromGame(game) : null;

        ObjectNode section = atomicBuilder.buildGamePanelComposite(
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
        return section;
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

    private ObjectNode buildSectionHeader(String id, String title,
                                           String subtitle, String actionUri) {
        ObjectNode section = atomicBuilder.buildSectionHeader(id, title, subtitle, null, actionUri);
        section.set("surface", utils.sectionHeaderSurface());
        return section;
    }

    private ObjectNode buildGamePanel(JsonNode game, boolean liveRefresh) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);
        boolean live = liveRefresh && gameStatus == 2;

        ObjectNode refreshPolicy = live ? ssePolicy(gameId) : staticPolicy();
        ObjectNode bindings = live ? utils.buildCompositeLinescoreBindings() : null;
        AtomicCompositeBuilder.GameClockSnapshot clock = live ? clockSnapshotFromGame(game) : null;

        return atomicBuilder.buildGamePanelComposite(
                "live-game-" + gameId,
                "live_game_" + gameId,
                "standard",
                gameId,
                gameStatus,
                game.path("gameStatusText").asText(""),
                null,
                atomicBuilder.gamePanelTeamFromJson(game.path("awayTeam")),
                atomicBuilder.gamePanelTeamFromJson(game.path("homeTeam")),
                clock,
                "nba://game/" + gameId,
                refreshPolicy,
                bindings,
                utils.gamePanelSurface());
    }

    private void addMockSections(ArrayNode sections) {
        sections.add(buildSectionHeader("live-now-header", "Live Now", "2 Games", null));

        // Mock live games
        sections.add(mockGamePanel("mock-live-1", "BOS", 1610612738, "LAL", 1610612747,
                "Q3 4:22", 2, true));
        sections.add(mockGamePanel("mock-live-2", "GSW", 1610612744, "PHX", 1610612756,
                "Q1 9:15", 2, true));

        sections.add(buildSectionHeader("upcoming-header", "Upcoming Today", null, null));

        sections.add(mockGamePanel("mock-up-1", "MIL", 1610612749, "DEN", 1610612743,
                "8:00 PM ET", 1, false));
        sections.add(mockGamePanel("mock-up-2", "DAL", 1610612742, "MIA", 1610612748,
                "10:00 PM ET", 1, false));
    }

    private ObjectNode mockGamePanel(String id, String awayTri, int awayId,
                                     String homeTri, int homeId,
                                     String statusText, int gameStatus,
                                     boolean liveRefresh) {
        ObjectNode refreshPolicy;
        if (liveRefresh) {
            refreshPolicy = objectMapper.createObjectNode();
            refreshPolicy.put("type", "sse");
            refreshPolicy.put("channel", id + ":linescore");
            refreshPolicy.put("pauseWhenOffScreen", false);
        } else {
            refreshPolicy = staticPolicy();
        }

        AtomicCompositeBuilder.GamePanelTeam away = new AtomicCompositeBuilder.GamePanelTeam(
                awayTri, gameStatus == 2 ? 72 : 0, SduiUtils.teamLogoUrl(awayId));
        AtomicCompositeBuilder.GamePanelTeam home = new AtomicCompositeBuilder.GamePanelTeam(
                homeTri, gameStatus == 2 ? 68 : 0, SduiUtils.teamLogoUrl(homeId));
        AtomicCompositeBuilder.GameClockSnapshot clock = liveRefresh && gameStatus == 2
                ? mockClockSnapshotFromStatus(statusText)
                : null;

        return atomicBuilder.buildGamePanelComposite(
                id,
                null,
                "standard",
                id,
                gameStatus,
                statusText,
                null,
                away,
                home,
                clock,
                "nba://game/" + id,
                refreshPolicy,
                null,
                utils.gamePanelSurface());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private JsonNode safeGetScoreboard() {
        try {
            return statsApiClient.getScoreboard();
        } catch (Exception e) {
            log.warn("Could not fetch scoreboard for Live screen: {}", e.getMessage());
            return null;
        }
    }

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }

    private ObjectNode ssePolicy(String gameId) {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "sse");
        rp.put("channel", gameId + ":linescore");
        rp.put("pauseWhenOffScreen", false);
        return rp;
    }

    /**
     * Build an initial {@code LiveClock} snapshot from upstream stats-api
     * game JSON. The `gameClock` string is ISO-8601 duration (e.g.
     * {@code PT04M32.00S}); for the first paint we treat the current
     * server time as the snapshot anchor so clients interpolate from
     * "now" until the first Ably frame lands.
     */
    private AtomicCompositeBuilder.GameClockSnapshot clockSnapshotFromGame(JsonNode game) {
        int seconds = parseGameClockSeconds(game.path("gameClock").asText(""));
        return new AtomicCompositeBuilder.GameClockSnapshot(
                seconds,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                true);
    }

    private AtomicCompositeBuilder.GameClockSnapshot mockClockSnapshotFromStatus(String statusText) {
        int seconds = parseMockClockSeconds(statusText);
        return new AtomicCompositeBuilder.GameClockSnapshot(
                seconds,
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                true);
    }

    private static int parseGameClockSeconds(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            return (int) java.time.Duration.parse(iso).getSeconds();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Parse the M:SS portion of mock status strings such as
     * {@code "Q3 4:22"} into seconds. Returns 0 when the format does
     * not match (upcoming / final / mock non-live).
     */
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
