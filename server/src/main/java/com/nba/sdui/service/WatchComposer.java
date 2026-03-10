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
 * Composes the "Watch" SDUI screen — video / streaming hub organised into tabs.
 *
 * Layout:
 *   TabGroup with 3 tabs: Featured, NBA TV, League Pass
 *   Each tab contains a mix of ContentRails, GameCards and PromoBanners.
 */
@Component
public class WatchComposer {

    private static final Logger log = LoggerFactory.getLogger(WatchComposer.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public WatchComposer(ObjectMapper objectMapper,
                         StatsApiClient statsApiClient,
                         SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
    }

    public JsonNode composeWatch(String traceId) {
        log.info("Composing Watch screen");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "watch");
        response.put("title", "Watch");
        response.put("analyticsId", "watch");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", utils.buildNavigation("watch"));

        ArrayNode sections = objectMapper.createArrayNode();

        // Single TabGroup section that contains the three tabs
        sections.add(buildTabGroup());

        response.set("sections", sections);
        return response;
    }

    // ── Tab group ──────────────────────────────────────────────────────

    private ObjectNode buildTabGroup() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "watch-tabs");
        section.put("type", "TabGroup");
        section.put("analyticsId", "watch_tabs");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();

        ArrayNode tabs = objectMapper.createArrayNode();
        tabs.add(buildFeaturedTab());
        tabs.add(buildNbaTvTab());
        tabs.add(buildLeaguePassTab());
        data.set("tabs", tabs);

        section.set("data", data);
        return section;
    }

    // ── Featured tab ───────────────────────────────────────────────────

    private ObjectNode buildFeaturedTab() {
        ObjectNode tab = objectMapper.createObjectNode();
        tab.put("id", "watch-featured");
        tab.put("label", "Featured");

        ArrayNode sections = objectMapper.createArrayNode();

        // Hero content rail
        sections.add(buildContentRail("featured-highlights", "Tonight's Highlights",
                new String[][]{
                        {"fh-1", "Game Recap: BOS vs LAL", "Full game highlights",
                                "https://cdn.nba.com/manage/2024/04/bos-lal-recap.jpg",
                                "video", "9:45", "nba://video/bos-lal-recap"},
                        {"fh-2", "Clutch Moments", "The best clutch plays tonight",
                                "https://cdn.nba.com/manage/2024/04/clutch-moments.jpg",
                                "video", "4:12", "nba://video/clutch-moments"},
                        {"fh-3", "Rookie Watch", "Top rookie performances",
                                "https://cdn.nba.com/manage/2024/04/rookie-watch.jpg",
                                "video", "3:20", "nba://video/rookie-watch"}
                }));

        // Promo banner
        sections.add(buildPromoBanner("watch-promo", "NBA League Pass",
                "Watch every out-of-market game live",
                "https://cdn.nba.com/manage/2024/04/league-pass-promo.jpg",
                "nba://subscribe/league-pass"));

        // More content
        sections.add(buildContentRail("featured-originals", "NBA Originals",
                new String[][]{
                        {"fo-1", "Open Court", "Inside the NBA culture",
                                "https://cdn.nba.com/manage/2024/04/open-court.jpg",
                                "video", "22:00", "nba://video/open-court"},
                        {"fo-2", "NBA Countdown", "Pre-game analysis",
                                "https://cdn.nba.com/manage/2024/04/countdown.jpg",
                                "video", "15:30", "nba://video/countdown"}
                }));

        tab.set("sections", sections);
        return tab;
    }

    // ── NBA TV tab ─────────────────────────────────────────────────────

    private ObjectNode buildNbaTvTab() {
        ObjectNode tab = objectMapper.createObjectNode();
        tab.put("id", "watch-nbatv");
        tab.put("label", "NBA TV");

        ArrayNode sections = objectMapper.createArrayNode();

        sections.add(buildSectionHeader("nbatv-live-header", "Live Now", null, null));

        sections.add(buildContentRail("nbatv-live", "On Air",
                new String[][]{
                        {"nbatv-1", "NBA GameTime Live", "Pre-game show airing now",
                                "https://cdn.nba.com/manage/2024/04/gametime-live.jpg",
                                "live", null, "nba://watch/nbatv-live"}
                }));

        sections.add(buildSectionHeader("nbatv-schedule-header", "Coming Up", null, null));

        sections.add(buildContentRail("nbatv-upcoming", "Today's Schedule",
                new String[][]{
                        {"nbatv-2", "NBA Inside Stuff", "Player features and interviews",
                                "https://cdn.nba.com/manage/2024/04/inside-stuff.jpg",
                                "video", "30:00", "nba://watch/inside-stuff"},
                        {"nbatv-3", "NBA Action", "Weekly highlights show",
                                "https://cdn.nba.com/manage/2024/04/nba-action.jpg",
                                "video", "30:00", "nba://watch/nba-action"}
                }));

        tab.set("sections", sections);
        return tab;
    }

    // ── League Pass tab ────────────────────────────────────────────────

    private ObjectNode buildLeaguePassTab() {
        ObjectNode tab = objectMapper.createObjectNode();
        tab.put("id", "watch-leaguepass");
        tab.put("label", "League Pass");

        ArrayNode sections = objectMapper.createArrayNode();

        sections.add(buildSectionHeader("lp-live-header", "Live Games", null, null));

        // Pull real games if available
        addLiveGameCards(sections);

        sections.add(buildContentRail("lp-condensed", "Condensed Games",
                new String[][]{
                        {"lp-c1", "BOS vs MIA Condensed", "Full condensed game",
                                "https://cdn.nba.com/manage/2024/04/bos-mia-condensed.jpg",
                                "video", "12:34", "nba://video/bos-mia-condensed"},
                        {"lp-c2", "DEN vs PHX Condensed", "Full condensed game",
                                "https://cdn.nba.com/manage/2024/04/den-phx-condensed.jpg",
                                "video", "11:58", "nba://video/den-phx-condensed"}
                }));

        tab.set("sections", sections);
        return tab;
    }

    private void addLiveGameCards(ArrayNode sections) {
        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard != null) {
                JsonNode games = scoreboard.path("scoreboard").path("games");
                int count = 0;
                for (JsonNode g : games) {
                    if (count >= 4) break;
                    sections.add(buildGameCard(g));
                    count++;
                }
                if (count > 0) return;
            }
        } catch (Exception e) {
            log.warn("Failed to build live game cards: {}", e.getMessage());
        }

        // Mock fallback
        sections.add(mockGameCard("lp-g1", "GSW", 1610612744, "LAC", 1610612746, "Q2 8:30", 2));
        sections.add(mockGameCard("lp-g2", "MIA", 1610612748, "NYK", 1610612752, "8:00 PM ET", 1));
    }

    // ── Reusable section builders ──────────────────────────────────────

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

    private ObjectNode buildContentRail(String id, String title, String[][] cards) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "ContentRail");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", title);

        ArrayNode cardsArr = objectMapper.createArrayNode();
        for (String[] c : cards) {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("id", c[0]);
            card.put("headline", c[1]);
            card.put("subhead", c[2]);
            card.put("thumbnailUrl", c[3]);
            card.put("contentType", c[4]);
            if (c[5] != null) card.put("duration", c[5]);

            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", c[6]);
            card.set("action", action);

            cardsArr.add(card);
        }
        data.set("cards", cardsArr);

        section.set("data", data);
        return section;
    }

    private ObjectNode buildPromoBanner(String id, String headline, String subhead,
                                         String backgroundUrl, String targetUri) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "PromoBanner");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("headline", headline);
        data.put("subhead", subhead);
        data.put("backgroundImageUrl", backgroundUrl);
        data.put("ctaLabel", "Learn More");

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", targetUri);
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    private ObjectNode buildGameCard(JsonNode game) {
        String gameId = game.path("gameId").asText("0000000000");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "watch-game-" + gameId);
        section.put("type", "GameCard");
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

    private ObjectNode mockGameCard(String id, String awayTri, int awayTeamId,
                                     String homeTri, int homeTeamId,
                                     String statusText, int gameStatus) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "GameCard");
        section.set("refreshPolicy", staticPolicy());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", id);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", statusText);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", awayTeamId);
        away.put("teamTricode", awayTri);
        away.put("teamName", awayTri);
        away.put("teamCity", awayTri);
        away.put("score", gameStatus == 2 ? 55 : 0);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/" + awayTeamId + "/global/L/logo.svg");
        data.set("awayTeam", away);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", homeTeamId);
        home.put("teamTricode", homeTri);
        home.put("teamName", homeTri);
        home.put("teamCity", homeTri);
        home.put("score", gameStatus == 2 ? 48 : 0);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/" + homeTeamId + "/global/L/logo.svg");
        data.set("homeTeam", home);

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
