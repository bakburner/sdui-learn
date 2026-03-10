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
 * Composes the "For You" SDUI screen — the personalised home feed.
 *
 * Layout:
 *   1. FollowingRail   – horizontal avatar strip of followed teams
 *   2. FeaturedGameCard – hero live / next-up game
 *   3. SectionHeader    – "Top Stories"
 *   4. ContentRail      – highlights / articles
 *   5. SectionHeader    – "Upcoming Games"
 *   6–8. GameCard       – next few games from the scoreboard
 */
@Component
public class ForYouComposer {

    private static final Logger log = LoggerFactory.getLogger(ForYouComposer.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ForYouComposer(ObjectMapper objectMapper,
                          StatsApiClient statsApiClient,
                          SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
    }

    public JsonNode composeForYou(String traceId) {
        log.info("Composing For You screen");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "for-you");
        response.put("title", "For You");
        response.put("analyticsId", "for_you");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("for-you"));

        ArrayNode sections = objectMapper.createArrayNode();

        // 1. FollowingRail
        sections.add(buildFollowingRail());

        // 2. FeaturedGameCard — try live data, fall back to mock
        ObjectNode featured = buildFeaturedFromLive();
        if (featured == null) {
            featured = buildMockFeaturedGame();
        }
        sections.add(featured);

        // 3. SectionHeader "Top Stories"
        sections.add(buildSectionHeader("top-stories-header", "Top Stories", null, null));

        // 4. ContentRail — highlights
        sections.add(buildHighlightsRail());

        // 5. SectionHeader "Upcoming Games"
        sections.add(buildSectionHeader("upcoming-header", "Upcoming Games",
                "See Full Schedule", "nba://scoreboard"));

        // 6–8. GameCards from scoreboard
        addUpcomingGameCards(sections);

        response.set("sections", sections);
        return response;
    }

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildFollowingRail() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "following-rail");
        section.put("type", "FollowingRail");
        section.put("analyticsId", "for_you_following");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Following");

        ArrayNode items = objectMapper.createArrayNode();
        items.add(followingItem("lebron", "L. James",
                "https://cdn.nba.com/headshots/nba/latest/260x190/2544.png",
                "player", "nba://player/2544"));
        items.add(followingItem("cavaliers", "Cavaliers",
                "https://cdn.nba.com/logos/nba/1610612739/global/L/logo.svg",
                "team", "nba://team/1610612739"));
        items.add(followingItem("curry", "S. Curry",
                "https://cdn.nba.com/headshots/nba/latest/260x190/201939.png",
                "player", "nba://player/201939"));
        items.add(followingItem("nba-news", "NBA News",
                "https://cdn.nba.com/manage/2024/04/nba-news-icon.png",
                "channel", "nba://news"));
        items.add(followingItem("twitter", "Twitter",
                "https://cdn.nba.com/manage/2024/04/twitter-icon.png",
                "social", "nba://social/twitter"));
        data.set("items", items);

        section.set("data", data);
        return section;
    }

    private ObjectNode followingItem(String id, String name, String imageUrl,
                                      String entityType, String targetUri) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("id", id);
        item.put("name", name);
        item.put("imageUrl", imageUrl);
        item.put("entityType", entityType);

        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", targetUri);
        item.set("action", action);

        return item;
    }

    private ObjectNode buildFeaturedFromLive() {
        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard == null) return null;

            JsonNode games = scoreboard.path("scoreboard").path("games");
            if (!games.isArray()) return null;

            // Prefer a live game (status 2), else first upcoming (status 1)
            JsonNode liveGame = null;
            JsonNode upcomingGame = null;
            for (JsonNode game : games) {
                int status = game.path("gameStatus").asInt(1);
                if (status == 2 && liveGame == null) liveGame = game;
                if (status == 1 && upcomingGame == null) upcomingGame = game;
            }
            JsonNode picked = liveGame != null ? liveGame : upcomingGame;
            if (picked == null) return null;

            return buildFeaturedGameSection(picked);
        } catch (Exception e) {
            log.warn("Failed to build featured game from live data: {}", e.getMessage());
            return null;
        }
    }

    private ObjectNode buildFeaturedGameSection(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "featured-game-" + gameId);
        section.put("type", "FeaturedGameCard");
        section.put("analyticsId", "for_you_featured_game");

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
        // Show home team name prominently (matches mockup)
        data.put("visualLabel", game.path("homeTeam").path("teamName").asText("").toUpperCase());

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
        section.put("id", "featured-game-mock");
        section.put("type", "FeaturedGameCard");
        section.put("analyticsId", "for_you_featured_game");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400001");
        data.put("gameStatus", 2);
        data.put("gameStatusText", "Q3 5:42");
        data.put("badgeText", "LIVE");
        data.put("visualLabel", "CELTICS");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612738);
        home.put("teamTricode", "BOS");
        home.put("teamName", "Celtics");
        home.put("teamCity", "Boston");
        home.put("score", 87);
        home.put("record", "4-2");
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612738/global/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612747);
        away.put("teamTricode", "LAL");
        away.put("teamName", "Lakers");
        away.put("teamCity", "Los Angeles");
        away.put("score", 82);
        away.put("record", "3-3");
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612747/global/L/logo.svg");
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400001");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    private ObjectNode buildSectionHeader(String id, String title,
                                           String actionLabel, String actionUri) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "SectionHeader");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", title);

        if (actionLabel != null && actionUri != null) {
            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", actionUri);
            action.put("label", actionLabel);
            data.set("action", action);
        }

        section.set("data", data);
        return section;
    }

    private ObjectNode buildHighlightsRail() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "for-you-highlights");
        section.put("type", "ContentRail");
        section.put("analyticsId", "for_you_highlights");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Highlights");

        ArrayNode cards = objectMapper.createArrayNode();
        cards.add(contentCard("hl-1", "Top 10 Plays of the Night",
                "Last night's best highlights",
                "https://cdn.nba.com/manage/2024/04/top10-plays.jpg",
                "video", "2:45", "nba://video/top10-plays"));
        cards.add(contentCard("hl-2", "Dunk of the Night",
                "An incredible poster slam",
                "https://cdn.nba.com/manage/2024/04/dunk-night.jpg",
                "video", "0:32", "nba://video/dunk-night"));
        cards.add(contentCard("hl-3", "Trade Deadline Recap",
                "All the moves from today",
                "https://cdn.nba.com/manage/2024/04/trade-deadline.jpg",
                "article", null, "nba://article/trade-recap"));
        data.set("cards", cards);

        section.set("data", data);
        return section;
    }

    private ObjectNode contentCard(String id, String headline, String subhead,
                                    String thumbnailUrl, String contentType,
                                    String duration, String targetUri) {
        ObjectNode card = objectMapper.createObjectNode();
        card.put("id", id);
        card.put("headline", headline);
        card.put("subhead", subhead);
        card.put("thumbnailUrl", thumbnailUrl);
        card.put("contentType", contentType);
        if (duration != null) card.put("duration", duration);

        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", targetUri);
        card.set("action", action);

        return card;
    }

    private void addUpcomingGameCards(ArrayNode sections) {
        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard != null) {
                JsonNode games = scoreboard.path("scoreboard").path("games");
                int count = 0;
                for (JsonNode game : games) {
                    if (count >= 3) break;
                    String gameId = game.path("gameId").asText(null);
                    if (gameId == null) continue;
                    sections.add(buildGameCardSection(game, gameId));
                    count++;
                }
                if (count > 0) return;
            }
        } catch (Exception e) {
            log.warn("Failed to add upcoming games from live data: {}", e.getMessage());
        }

        // Fallback mock
        sections.add(buildMockGameCard("0022400010", "PHX", 1610612756, "MIL", 1610612749,
                "7:30 PM ET", 1));
        sections.add(buildMockGameCard("0022400011", "DAL", 1610612742, "DEN", 1610612743,
                "10:00 PM ET", 1));
    }

    private ObjectNode buildGameCardSection(JsonNode game, String gameId) {
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-" + gameId);
        section.put("type", "GameCard");
        section.put("analyticsId", "for_you_game_" + gameId);
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
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

    private ObjectNode buildMockGameCard(String gameId, String awayTri, int awayId,
                                          String homeTri, int homeId,
                                          String statusText, int gameStatus) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-" + gameId);
        section.put("type", "GameCard");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", statusText);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", awayId);
        away.put("teamTricode", awayTri);
        away.put("teamName", awayTri);
        away.put("teamCity", awayTri);
        away.put("score", 0);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/" + awayId + "/global/L/logo.svg");
        data.set("awayTeam", away);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", homeId);
        home.put("teamTricode", homeTri);
        home.put("teamName", homeTri);
        home.put("teamCity", homeTri);
        home.put("score", 0);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/" + homeId + "/global/L/logo.svg");
        data.set("homeTeam", home);

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

    // ── Helpers ────────────────────────────────────────────────────────

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
