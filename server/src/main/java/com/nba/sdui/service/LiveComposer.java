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
 * Composes the "Live" SDUI screen — all live and upcoming games.
 *
 * Layout:
 *   1. FeaturedGameCard – hero card for the top live game (SSE refresh)
 *   2. SectionHeader    – "Live Now"
 *   3–N. GameCard       – every live game (gameStatus == 2)
 *   N+1. SectionHeader  – "Upcoming Today"
 *   N+2–M. GameCard     – every upcoming game (gameStatus == 1)
 */
@Component
public class LiveComposer {

    private static final Logger log = LoggerFactory.getLogger(LiveComposer.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public LiveComposer(ObjectMapper objectMapper,
                        StatsApiClient statsApiClient,
                        SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
    }

    public JsonNode composeLive(String traceId) {
        log.info("Composing Live screen");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "live");
        response.put("title", "Live");
        response.put("analyticsId", "live");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("live"));

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
            sections.add(buildFeaturedGameCard(heroGame));
        } else {
            sections.add(buildMockFeaturedGame());
        }

        // 2. "Live Now" section
        if (liveGames.size() > 0) {
            sections.add(buildSectionHeader("live-now-header", "Live Now",
                    liveGames.size() + " Games", null));
            for (JsonNode g : liveGames) {
                sections.add(buildGameCard(g, true));
            }
        }

        // 3. "Upcoming Today"
        if (upcomingGames.size() > 0) {
            sections.add(buildSectionHeader("upcoming-header", "Upcoming Today",
                    null, null));
            for (JsonNode g : upcomingGames) {
                sections.add(buildGameCard(g, false));
            }
        }

        // 4. "Final" — completed games
        if (finishedGames.size() > 0) {
            sections.add(buildSectionHeader("final-header", "Final", null, null));
            for (JsonNode g : finishedGames) {
                sections.add(buildGameCard(g, false));
            }
        }

        // If no real data, add mock sections
        if (liveGames.size() == 0 && upcomingGames.size() == 0 && finishedGames.size() == 0) {
            addMockSections(sections);
        }

        response.set("sections", sections);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildFeaturedGameCard(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "live-featured-" + gameId);
        section.put("type", "FeaturedGameCard");
        section.put("analyticsId", "live_featured_game");

        if (gameStatus == 2) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", gameId + ":linescore");
            section.set("refreshPolicy", rp);
            section.set("dataBindings", utils.buildLinescoreBindings());
        } else {
            section.set("refreshPolicy", staticPolicy());
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
        data.put("gameTimeEt", game.path("gameTimeEt").asText(""));
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
        return section;
    }

    private ObjectNode buildMockFeaturedGame() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "live-featured-mock");
        section.put("type", "FeaturedGameCard");
        section.put("analyticsId", "live_featured_game");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400050");
        data.put("gameStatus", 1);
        data.put("gameStatusText", "7:30 PM ET");
        data.put("badgeText", "NEXT UP");
        data.put("visualLabel", "Recommended");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612752);
        home.put("teamTricode", "NYK");
        home.put("teamName", "Knicks");
        home.put("teamCity", "New York");
        home.put("score", 0);
        home.put("record", "4-2");
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612752/global/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612748);
        away.put("teamTricode", "MIA");
        away.put("teamName", "Heat");
        away.put("teamCity", "Miami");
        away.put("score", 0);
        away.put("record", "3-3");
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612748/global/L/logo.svg");
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400050");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    private ObjectNode buildSectionHeader(String id, String title,
                                           String subtitle, String actionUri) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "SectionHeader");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", title);
        if (subtitle != null) data.put("subtitle", subtitle);

        if (actionUri != null) {
            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", actionUri);
            data.set("action", action);
        }

        section.set("data", data);
        return section;
    }

    private ObjectNode buildGameCard(JsonNode game, boolean liveRefresh) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "live-game-" + gameId);
        section.put("type", "GameCard");
        section.put("analyticsId", "live_game_" + gameId);

        if (liveRefresh && gameStatus == 2) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", gameId + ":linescore");
            section.set("refreshPolicy", rp);
            section.set("dataBindings", utils.buildLinescoreBindings());
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
        return section;
    }

    private void addMockSections(ArrayNode sections) {
        sections.add(buildSectionHeader("live-now-header", "Live Now", "2 Games", null));

        // Mock live games
        sections.add(mockGameCard("mock-live-1", "BOS", 1610612738, "LAL", 1610612747,
                "Q3 4:22", 2, true));
        sections.add(mockGameCard("mock-live-2", "GSW", 1610612744, "PHX", 1610612756,
                "Q1 9:15", 2, true));

        sections.add(buildSectionHeader("upcoming-header", "Upcoming Today", null, null));

        sections.add(mockGameCard("mock-up-1", "MIL", 1610612749, "DEN", 1610612743,
                "8:00 PM ET", 1, false));
        sections.add(mockGameCard("mock-up-2", "DAL", 1610612742, "MIA", 1610612748,
                "10:00 PM ET", 1, false));
    }

    private ObjectNode mockGameCard(String id, String awayTri, int awayId,
                                     String homeTri, int homeId,
                                     String statusText, int gameStatus,
                                     boolean liveRefresh) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "GameCard");

        if (liveRefresh) {
            ObjectNode rp = objectMapper.createObjectNode();
            rp.put("type", "sse");
            rp.put("channel", id + ":linescore");
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
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/" + awayId + "/global/L/logo.svg");
        data.set("awayTeam", away);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", homeId);
        home.put("teamTricode", homeTri);
        home.put("teamName", homeTri);
        home.put("teamCity", homeTri);
        home.put("score", gameStatus == 2 ? 68 : 0);
        home.put("record", "4-2");
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/" + homeId + "/global/L/logo.svg");
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
        mapped.put("logoUrl", "https://cdn.nba.com/logos/nba/" + team.path("teamId").asText() + "/global/L/logo.svg");
        return mapped;
    }

    private ObjectNode staticPolicy() {
        ObjectNode rp = objectMapper.createObjectNode();
        rp.put("type", "static");
        return rp;
    }
}
