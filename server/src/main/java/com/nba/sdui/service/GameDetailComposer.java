package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Composes the Game Detail SDUI screen from live NBA API data with
 * example-file fallback, and applies server-driven variant transformations
 * (B, C, D).
 */
@Component
public class GameDetailComposer {

    private static final Logger log = LoggerFactory.getLogger(GameDetailComposer.class);
    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final BoxscoreComposer boxscoreComposer;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public GameDetailComposer(ObjectMapper objectMapper,
                              StatsApiClient statsApiClient,
                              BoxscoreComposer boxscoreComposer,
                              SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.boxscoreComposer = boxscoreComposer;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    // ── Public entry point ─────────────────────────────────────────────

    /**
     * Compose a Game Detail SDUI screen response.
     */
    public JsonNode composeGameDetail(String gameId, String gameState,
                                      String variant, String clientSchemaVersion,
                                      String traceId, String locale) throws IOException {
        log.info("Composing game detail: gameId={}, gameState={}, variant={}, locale={}", gameId, gameState, variant, locale);

        JsonNode baseResponse;
        baseResponse = composeFromLiveData(gameId, gameState);
        if (baseResponse == null) {
            log.warn("Live game detail unavailable, falling back to example for gameState={}", gameState);
            baseResponse = utils.loadExampleResponse(gameState);
        }

        if (baseResponse == null) {
            log.warn("No example response found for gameState={}, using 'pre' as default", gameState);
            baseResponse = utils.loadExampleResponse("pre");
        }

        ObjectNode response = baseResponse.deepCopy();

        response.put("id", "game-detail-" + gameId);
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.put("parentUri", "nba://scoreboard");
        response.set("navigation", utils.buildNavigation("game-detail"));
        response.set("stringTable", utils.buildStringTable(locale));

        // Expose available A/B variants so clients never need URI-sniffing (Rule 10).
        ArrayNode variants = objectMapper.createArrayNode();
        variants.add(objectMapper.createObjectNode().put("id", "A").put("label", "Default").put("description", "All sections, standard order"));
        variants.add(objectMapper.createObjectNode().put("id", "B").put("label", "Reorder").put("description", "Content rail and TabGroup swapped"));
        variants.add(objectMapper.createObjectNode().put("id", "C").put("label", "Minimal").put("description", "Player stats and promo banner removed"));
        variants.add(objectMapper.createObjectNode().put("id", "D").put("label", "Extra Rail").put("description", "Trending videos rail added after content rail"));
        response.set("variants", variants);

        resolveChannelPatterns(response, gameId);

        if (variant != null) {
            switch (variant.toUpperCase()) {
                case "B" -> applyVariantB(response);
                case "C" -> applyVariantC(response);
                case "D" -> applyVariantD(response);
                default -> log.debug("Using default variant A (no transformation)");
            }
        }

        log.debug("SDUI response composed: variant={}, sections={}",
                variant, response.has("sections") ? response.get("sections").size() : 0);

        return response;
    }

    // ── Live-data composition ──────────────────────────────────────────

    private JsonNode composeFromLiveData(String gameId, String gameState) {
        try {
            JsonNode boxscore = statsApiClient.getBoxscore(gameId);
            if (boxscore == null) {
                log.warn("No boxscore data available for gameId={}", gameId);
                return null;
            }

            log.info("Successfully fetched live boxscore for gameId={}", gameId);
            JsonNode game = boxscore.path("game");
            if (game.isMissingNode()) {
                log.warn("No game data in boxscore for gameId={}", gameId);
                return null;
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("id", "game-detail-" + gameId);
            response.put("type", "game_detail");
            response.put("schemaVersion", schemaVersion);
            response.put("parentUri", "nba://scoreboard");

            // Default screen state — boxscore team tabs default to away (first listed)
            String awayTricode = game.path("awayTeam").path("teamTricode").asText("AWAY");
            ObjectNode screenState = objectMapper.createObjectNode();
            screenState.put("gd_boxscore_team", awayTricode);
            screenState.put("gd_boxscore_away_sortCol", "points");
            screenState.put("gd_boxscore_away_sortDir", "desc");
            screenState.put("gd_boxscore_home_sortCol", "points");
            screenState.put("gd_boxscore_home_sortDir", "desc");
            response.set("state", screenState);

            ArrayNode sections = objectMapper.createArrayNode();

            // 1. GamePanel (scoreboard displayConfig)
            sections.add(buildGamePanelScoreboardFromLive(game, gameId));

            // 2. StatLine (top performers)
            ObjectNode statLineSection = buildStatLineSectionFromLive(game, gameId);
            if (statLineSection != null) {
                sections.add(statLineSection);
            }

            // 2b. Responsive row – home/away top performers side-by-side
            ObjectNode rowSection = buildRowSectionFromLive(game, gameId);
            if (rowSection != null) {
                sections.add(rowSection);
            }

            // 3. ContentRail (AtomicComposite) from example
            ObjectNode contentRail = loadSectionFromExample(gameState, "content-rail");
            if (contentRail != null) {
                sections.add(contentRail);
            }

            // 4. PromoBanner (AtomicComposite) from example
            ObjectNode promoBanner = loadSectionFromExample(gameState, "promo-banner");
            if (promoBanner != null) {
                sections.add(promoBanner);
            }

            // 5. BoxscoreTable TabGroup at the bottom
            ObjectNode tabGroup = buildBoxscoreTabGroupFromLive(game, gameId);
            if (tabGroup != null) {
                sections.add(tabGroup);
            }

            response.set("sections", sections);
            return response;

        } catch (Exception e) {
            log.error("Failed to compose from live data for gameId={}: {}", gameId, e.getMessage(), e);
        }
        return null;
    }

    private ObjectNode loadSectionFromExample(String gameState, String sectionId) {
        try {
            JsonNode example = utils.loadExampleResponse(gameState);
            if (example == null) return null;

            ArrayNode sections = (ArrayNode) example.get("sections");
            if (sections == null) return null;

            for (JsonNode section : sections) {
                if (sectionId.equals(section.path("id").asText())) {
                    return section.deepCopy();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load section id={} from example: {}", sectionId, e.getMessage());
        }
        return null;
    }

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildBoxscoreTabGroupFromLive(JsonNode game, String gameId) {
        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");
        if (homeTeam.isMissingNode() || awayTeam.isMissingNode()) return null;

        String homeTricode = homeTeam.path("teamTricode").asText("HOME");
        String awayTricode = awayTeam.path("teamTricode").asText("AWAY");
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-detail-boxscore-tabs");
        section.put("type", "TabGroup");
        section.put("analyticsId", "game_detail_boxscore_tabs");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "gd_boxscore_team");
        data.put("defaultTab", awayTricode);

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode awayTab = objectMapper.createObjectNode();
        awayTab.put("id", "tab-" + awayTricode.toLowerCase());
        awayTab.put("label", awayTricode);
        awayTab.put("stateKey", "gd_boxscore_team");
        awayTab.put("stateValue", awayTricode);
        tabs.add(awayTab);

        ObjectNode homeTab = objectMapper.createObjectNode();
        homeTab.put("id", "tab-" + homeTricode.toLowerCase());
        homeTab.put("label", homeTricode);
        homeTab.put("stateKey", "gd_boxscore_team");
        homeTab.put("stateValue", homeTricode);
        tabs.add(homeTab);

        data.set("tabs", tabs);

        // Reuse the full BoxscoreTable builder from BoxscoreComposer
        ObjectNode awayTable = boxscoreComposer.buildBoxscoreTableSection(
                awayTeam, gameId, "gd_boxscore_away_sortCol", "gd_boxscore_away_sortDir", gameStatus);
        ObjectNode homeTable = boxscoreComposer.buildBoxscoreTableSection(
                homeTeam, gameId, "gd_boxscore_home_sortCol", "gd_boxscore_home_sortDir", gameStatus);

        ObjectNode tabContents = objectMapper.createObjectNode();
        ArrayNode awayContent = objectMapper.createArrayNode();
        awayContent.add(awayTable);
        tabContents.set(awayTricode, awayContent);

        ArrayNode homeContent = objectMapper.createArrayNode();
        homeContent.add(homeTable);
        tabContents.set(homeTricode, homeContent);

        data.set("tabContents", tabContents);
        section.set("data", data);
        return section;
    }

    private ObjectNode buildTabGroupFromLive(JsonNode game, String gameId) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-tabs");
        section.put("type", "TabGroup");
        section.put("analyticsId", "game_tabs");

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "poll");
        refreshPolicy.put("intervalMs", 30000);
        refreshPolicy.put("url", "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
        refreshPolicy.put("dataPath", "game");
        section.set("refreshPolicy", refreshPolicy);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "activeTab");
        data.put("defaultTab", "boxscore");

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode boxscoreTab = objectMapper.createObjectNode();
        boxscoreTab.put("id", "tab-boxscore");
        boxscoreTab.put("label", "Box Score");
        boxscoreTab.put("stateKey", "activeTab");
        boxscoreTab.put("stateValue", "boxscore");
        tabs.add(boxscoreTab);

        ObjectNode playByPlayTab = objectMapper.createObjectNode();
        playByPlayTab.put("id", "tab-playbyplay");
        playByPlayTab.put("label", "Play-by-Play");
        playByPlayTab.put("stateKey", "activeTab");
        playByPlayTab.put("stateValue", "playbyplay");
        tabs.add(playByPlayTab);

        data.set("tabs", tabs);

        ObjectNode tabContents = objectMapper.createObjectNode();

        ArrayNode boxscoreContent = objectMapper.createArrayNode();
        ObjectNode homeBoxscore = buildTeamBoxscoreStatLine(game.path("homeTeam"));
        if (homeBoxscore != null) {
            boxscoreContent.add(homeBoxscore);
        }
        ObjectNode awayBoxscore = buildTeamBoxscoreStatLine(game.path("awayTeam"));
        if (awayBoxscore != null) {
            boxscoreContent.add(awayBoxscore);
        }
        tabContents.set("boxscore", boxscoreContent);

        ArrayNode playByPlayContent = objectMapper.createArrayNode();
        playByPlayContent.add(atomicBuilder.buildStatLine(
                "play-by-play", null, "Play-by-Play", "vertical", new String[][]{}));
        tabContents.set("playbyplay", playByPlayContent);

        data.set("tabContents", tabContents);
        section.set("data", data);

        return section;
    }

    private ObjectNode buildTeamBoxscoreStatLine(JsonNode team) {
        if (team.isMissingNode() || !team.has("players")) {
            return null;
        }

        String teamName = team.path("teamName").asText();
        String teamCity = team.path("teamCity").asText();
        String teamTricode = team.path("teamTricode").asText();
        int teamId = team.path("teamId").asInt();

        ArrayNode stats = objectMapper.createArrayNode();
        ArrayNode players = (ArrayNode) team.get("players");

        for (JsonNode player : players) {
            JsonNode playerStats = player.path("statistics");
            String minutes = playerStats.path("minutes").asText();

            if (minutes == null || minutes.isEmpty() || "00:00".equals(minutes) || "PT00M00.00S".equals(minutes)) {
                continue;
            }

            ObjectNode statItem = objectMapper.createObjectNode();
            statItem.put("playerId", player.path("personId").asInt());

            String playerName = player.path("name").asText();
            if (playerName.isEmpty()) {
                String firstName = player.path("firstName").asText();
                String familyName = player.path("familyName").asText();
                playerName = firstName + " " + familyName;
            }
            statItem.put("playerName", playerName);
            statItem.put("teamTricode", teamTricode);

            String formattedMinutes = SduiUtils.formatMinutes(minutes);
            statItem.put("statCategory", "MIN");
            statItem.put("statValue", formattedMinutes);

            String imgUrl = "https://cdn.nba.com/headshots/nba/latest/1040x760/"
                    + player.path("personId").asInt() + ".png";
            statItem.put("playerImageUrl", imgUrl);

            stats.add(statItem);
        }

        return atomicBuilder.buildStatLineFromNodes(
                "boxscore-" + teamTricode.toLowerCase(), null,
                teamCity + " " + teamName, "vertical", stats);
    }

    private ObjectNode buildGamePanelScoreboardFromLive(JsonNode game, String gameId) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "scoreboard");
        section.put("type", "GamePanel");

        section.set("dataBinding", utils.buildLinescoreBindings());

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "sse");
        refreshPolicy.put("channel", gameId + ":linescore");
        section.set("refreshPolicy", refreshPolicy);

        section.set("sectionStates", utils.buildSectionStates(
                "scoreboard", "Unable to load live scores", "shimmer", 180));

        ObjectNode data = objectMapper.createObjectNode();

        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");

        ObjectNode homeData = objectMapper.createObjectNode();
        homeData.put("teamId", homeTeam.path("teamId").asInt());
        homeData.put("teamTricode", homeTeam.path("teamTricode").asText());
        homeData.put("teamName", homeTeam.path("teamName").asText());
        homeData.put("teamCity", homeTeam.path("teamCity").asText());
        homeData.put("score", homeTeam.path("score").asInt());
        homeData.put("logoUrl", "https://cdn.nba.com/logos/nba/" +
                homeTeam.path("teamId").asText() + "/primary/L/logo.svg");
        data.set("homeTeam", homeData);

        ObjectNode awayData = objectMapper.createObjectNode();
        awayData.put("teamId", awayTeam.path("teamId").asInt());
        awayData.put("teamTricode", awayTeam.path("teamTricode").asText());
        awayData.put("teamName", awayTeam.path("teamName").asText());
        awayData.put("teamCity", awayTeam.path("teamCity").asText());
        awayData.put("score", awayTeam.path("score").asInt());
        awayData.put("logoUrl", "https://cdn.nba.com/logos/nba/" +
                awayTeam.path("teamId").asText() + "/primary/L/logo.svg");
        data.set("awayTeam", awayData);

        data.put("gameId", game.path("gameId").asText());
        data.put("gameClock", game.path("gameClock").asText(""));
        data.put("period", game.path("period").asInt());
        data.put("gameStatus", game.path("gameStatus").asInt());
        data.put("gameStatusText", game.path("gameStatusText").asText());
        data.set("displayConfig", atomicBuilder.scoreboardConfig(null));

        section.set("data", data);
        return section;
    }

    private ObjectNode buildStatLineSectionFromLive(JsonNode game, String gameId) {
        ArrayNode stats = objectMapper.createArrayNode();

        List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.path("homeTeam"), 3);
        List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.path("awayTeam"), 3);

        homePerformers.forEach(stats::add);
        awayPerformers.forEach(stats::add);

        if (stats.isEmpty()) {
            return null;
        }

        ObjectNode section = atomicBuilder.buildStatLineFromNodes(
                "top-performers", null, "Top Performers", "vertical", stats);

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "poll");
        refreshPolicy.put("intervalMs", 30000);
        refreshPolicy.put("url", "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
        refreshPolicy.put("dataPath", "game");
        atomicBuilder.attachRefreshPolicy(section, refreshPolicy);

        atomicBuilder.attachSectionStates(section, utils.buildSectionStates(
                "top-performers", "Unable to load player stats", "placeholder", 120));

        return section;
    }

    private ObjectNode buildRowSectionFromLive(JsonNode game, String gameId) {
        try {
            List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.path("homeTeam"), 2);
            List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.path("awayTeam"), 2);
            if (homePerformers.isEmpty() && awayPerformers.isEmpty()) return null;

            String homeTricode = game.path("homeTeam").path("teamTricode").asText("HOME");
            String awayTricode = game.path("awayTeam").path("teamTricode").asText("AWAY");

            ObjectNode homeChild = buildStatLineChild("row-home-stats", homeTricode + " Leaders", homePerformers);
            ObjectNode awayChild = buildStatLineChild("row-away-stats", awayTricode + " Leaders", awayPerformers);

            ObjectNode root = atomicBuilder.responsiveRow(16, 600);
            ArrayNode children = objectMapper.createArrayNode();

            ObjectNode homeSlot = atomicBuilder.sectionSlot("row-home", homeChild);
            atomicBuilder.setFlex(homeSlot, 1);
            children.add(homeSlot);

            ObjectNode awaySlot = atomicBuilder.sectionSlot("row-away", awayChild);
            atomicBuilder.setFlex(awaySlot, 1);
            children.add(awaySlot);

            root.set("children", children);
            return atomicBuilder.wrapAsComposite("team-leaders-row", null, root);
        } catch (Exception e) {
            log.warn("Failed to build responsive row from live data: {}", e.getMessage());
            return null;
        }
    }

    private ObjectNode buildStatLineChild(String id, String title, List<ObjectNode> performers) {
        ArrayNode stats = objectMapper.createArrayNode();
        performers.forEach(stats::add);
        return atomicBuilder.buildStatLineFromNodes(id, null, title, "vertical", stats);
    }

    private List<ObjectNode> getTopPerformersFromTeam(JsonNode team, int maxPlayers) {
        List<ObjectNode> performers = new ArrayList<>();
        if (!team.has("players")) return performers;

        String teamTricode = team.path("teamTricode").asText();
        int teamId = team.path("teamId").asInt();

        ArrayNode players = (ArrayNode) team.get("players");

        List<Map.Entry<JsonNode, Integer>> playerPoints = new ArrayList<>();
        for (JsonNode player : players) {
            JsonNode playerStats = player.path("statistics");
            int points = playerStats.path("points").asInt();
            if (points >= 5) {
                playerPoints.add(Map.entry(player, points));
            }
        }

        playerPoints.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int count = 0;
        for (Map.Entry<JsonNode, Integer> entry : playerPoints) {
            if (count >= maxPlayers) break;

            JsonNode player = entry.getKey();
            int points = entry.getValue();
            JsonNode playerStats = player.path("statistics");

            ObjectNode item = objectMapper.createObjectNode();
            item.put("playerId", player.path("personId").asInt());

            String playerName = player.path("name").asText();
            if (playerName.isEmpty()) {
                String firstName = player.path("firstName").asText();
                String familyName = player.path("familyName").asText();
                playerName = firstName + " " + familyName;
            }
            item.put("playerName", playerName);
            item.put("playerImageUrl",
                    "https://cdn.nba.com/headshots/nba/latest/1040x760/" +
                    player.path("personId").asText() + ".png");
            item.put("teamTricode", teamTricode);
            item.put("teamId", teamId);

            item.put("statCategory", "PTS");
            item.put("statValue", String.valueOf(points));
            item.put("statLabel", "Points");

            ObjectNode additionalStats = objectMapper.createObjectNode();
            additionalStats.put("rebounds", playerStats.path("reboundsTotal").asInt());
            additionalStats.put("assists", playerStats.path("assists").asInt());
            additionalStats.put("minutes", playerStats.path("minutes").asText());
            item.set("additionalStats", additionalStats);

            performers.add(item);
            count++;
        }

        return performers;
    }

    // ── Channel resolution ─────────────────────────────────────────────

    private void resolveChannelPatterns(ObjectNode response, String gameId) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            if (section.has("refreshPolicy")) {
                JsonNode refreshPolicy = section.get("refreshPolicy");
                if (refreshPolicy.has("channel")) {
                    String channel = refreshPolicy.get("channel").asText();
                    String resolvedChannel = channel.replace("{gameId}", gameId);
                    ((ObjectNode) refreshPolicy).put("channel", resolvedChannel);
                }
            }
        }
    }

    // ── Variant transformations ────────────────────────────────────────

    private void applyVariantB(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        int contentRailIndex = -1;
        int tabGroupIndex = -1;

        for (int i = 0; i < sections.size(); i++) {
            String id = sections.get(i).path("id").asText();
            if ("content-rail".equals(id)) contentRailIndex = i;
            if ("game-tabs".equals(id)) tabGroupIndex = i;
        }

        if (contentRailIndex >= 0 && tabGroupIndex >= 0 && contentRailIndex < tabGroupIndex) {
            JsonNode contentRail = sections.get(contentRailIndex);
            JsonNode tabGroup = sections.get(tabGroupIndex);
            sections.set(contentRailIndex, tabGroup);
            sections.set(tabGroupIndex, contentRail);
            log.debug("Applied variant B: swapped ContentRail and TabGroup positions");
        }
    }

    private void applyVariantC(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        ArrayNode filtered = objectMapper.createArrayNode();

        for (JsonNode section : sections) {
            String id = section.path("id").asText();
            if (!"promo-banner".equals(id) && !"player-stats".equals(id)) {
                filtered.add(section);
            }
        }

        response.set("sections", filtered);
        log.debug("Applied variant C: removed promo-banner and player-stats, {} sections remaining", filtered.size());
    }

    private void applyVariantD(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");

        String[][] trendingCards = {
            {"trending-1", "Top 10 Plays of the Night", "Last night's best moments", FALLBACK_THUMB, "video", "4:30", "nba://video/top10-plays"},
            {"trending-2", "Playoff Intensity", "The best of postseason basketball", FALLBACK_THUMB, "video", "2:15", "nba://video/playoff-intensity"},
            {"trending-3", "Post-Game Press Conference", "Hear from the coaches", FALLBACK_THUMB, "video", "6:00", "nba://video/post-game-presser"}
        };
        ObjectNode extraRail = atomicBuilder.buildContentRail("trending-videos", "trending_videos_rail", "Trending Videos", trendingCards);

        ArrayNode updated = objectMapper.createArrayNode();
        for (JsonNode section : sections) {
            updated.add(section);
            if ("content-rail".equals(section.path("id").asText())) {
                updated.add(extraRail);
            }
        }

        response.set("sections", updated);
        log.debug("Applied variant D: added Trending Videos rail, {} sections total", updated.size());
    }
}
