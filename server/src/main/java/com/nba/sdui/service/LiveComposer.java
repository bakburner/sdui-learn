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

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "live-featured-" + gameId);
        section.put("type", "GamePanel");
        section.put("analyticsId", "live_featured_game");

        if (gameStatus == 2) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", gameId + ":linescore");
            rp.put("pauseWhenOffScreen", false);
            section.set("refreshPolicy", rp);
            section.set("dataBinding", utils.buildLinescoreBindings());
        } else {
            section.set("refreshPolicy", staticPolicy());
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
        data.put("gameTimeEt", game.path("gameTimeEt").asText(""));
        data.set("displayConfig", atomicBuilder.featuredConfig(null, new String[]{ColorTokens.PALETTE_BLUE_30, ColorTokens.BRAND_LIVE}));
        data.set("homeTeam", mapTeam(game.path("homeTeam")));
        data.set("awayTeam", mapTeam(game.path("awayTeam")));
        data.put("badgeText", gameStatus == 2 ? "LIVE" : "UP NEXT");
        data.put("visualLabel", "Recommended");

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + gameId);
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("surface", utils.gamePanelSurface());
        return section;
    }

    private ObjectNode buildMockFeaturedGame() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "live-featured-mock");
        section.put("type", "GamePanel");
        section.put("analyticsId", "live_featured_game");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400050");
        data.put("gameStatus", 1);
        data.put("gameStatusText", "7:30 PM ET");
        data.set("displayConfig", atomicBuilder.featuredConfig(null, new String[]{ColorTokens.PALETTE_BLUE_30, ColorTokens.BRAND_LIVE}));
        data.put("badgeText", "NEXT UP");
        data.put("visualLabel", "Recommended");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612752);
        home.put("teamTricode", "NYK");
        home.put("teamName", "Knicks");
        home.put("teamCity", "New York");
        home.put("score", 0);
        home.put("record", "4-2");
        home.put("logoUrl", SduiUtils.teamLogoUrl("1610612752"));
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612748);
        away.put("teamTricode", "MIA");
        away.put("teamName", "Heat");
        away.put("teamCity", "Miami");
        away.put("score", 0);
        away.put("record", "3-3");
        away.put("logoUrl", SduiUtils.teamLogoUrl("1610612748"));
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400050");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("surface", utils.gamePanelSurface());
        return section;
    }

    private ObjectNode buildSectionHeader(String id, String title,
                                           String subtitle, String actionUri) {
        ObjectNode section = atomicBuilder.buildSectionHeader(id, title, subtitle, null, actionUri);
        section.set("surface", utils.railSurface());
        return section;
    }

    private ObjectNode buildGamePanel(JsonNode game, boolean liveRefresh) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "live-game-" + gameId);
        section.put("type", "GamePanel");
        section.put("analyticsId", "live_game_" + gameId);

        if (liveRefresh && gameStatus == 2) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", gameId + ":linescore");
            rp.put("pauseWhenOffScreen", false);
            section.set("refreshPolicy", rp);
            section.set("dataBinding", utils.buildLinescoreBindings());
        } else {
            section.set("refreshPolicy", staticPolicy());
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
        data.put("gameTimeEt", game.path("gameTimeEt").asText(""));
        data.put("gameDateEt", game.path("gameDateEt").asText(""));
        // Broadcast info
        JsonNode broadcasters = game.path("broadcasters");
        if (broadcasters.isObject()) {
            JsonNode national = broadcasters.path("nationalTvBroadcasters");
            JsonNode home = broadcasters.path("homeTvBroadcasters");
            if (national.isArray() && national.size() > 0) {
                data.put("broadcaster", national.get(0).path("broadcasterDisplay").asText(""));
            } else if (home.isArray() && home.size() > 0) {
                data.put("broadcaster", home.get(0).path("broadcasterDisplay").asText(""));
            }
        }
        data.set("homeTeam", mapTeam(game.path("homeTeam")));
        data.set("awayTeam", mapTeam(game.path("awayTeam")));

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + gameId);
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("surface", utils.gamePanelSurface());
        return section;
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
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "GamePanel");

        if (liveRefresh) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", id + ":linescore");
            rp.put("pauseWhenOffScreen", false);
            section.set("refreshPolicy", rp);
        } else {
            section.set("refreshPolicy", staticPolicy());
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", id);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", statusText);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", awayId);
        away.put("teamTricode", awayTri);
        away.put("teamName", awayTri);
        away.put("teamCity", awayTri);
        away.put("score", gameStatus == 2 ? 72 : 0);
        away.put("record", "4-2");
        away.put("logoUrl", SduiUtils.teamLogoUrl(awayId));
        data.set("awayTeam", away);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", homeId);
        home.put("teamTricode", homeTri);
        home.put("teamName", homeTri);
        home.put("teamCity", homeTri);
        home.put("score", gameStatus == 2 ? 68 : 0);
        home.put("record", "4-2");
        home.put("logoUrl", SduiUtils.teamLogoUrl(homeId));
        data.set("homeTeam", home);

        data.put("broadcaster", "Fox Sports Southeast - Atlanta");

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/" + id);
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        section.set("surface", utils.gamePanelSurface());
        return section;
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

    private ObjectNode mapTeam(JsonNode team) {
        ObjectNode mapped = objectMapper.createObjectNode();
        mapped.put("teamId", team.path("teamId").asInt());
        mapped.put("teamTricode", team.path("teamTricode").asText(""));
        mapped.put("teamName", team.path("teamName").asText(""));
        mapped.put("teamCity", team.path("teamCity").asText(""));
        mapped.put("score", team.path("score").asInt(0));
        int wins = team.path("wins").asInt(0);
        int losses = team.path("losses").asInt(0);
        mapped.put("record", wins + "-" + losses);
        mapped.put("logoUrl", SduiUtils.teamLogoUrl(team.path("teamId").asText()));
        return mapped;
    }

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }
}
