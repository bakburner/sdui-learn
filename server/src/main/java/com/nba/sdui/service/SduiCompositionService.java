package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SDUI Composition Service.
 * 
 * Assembles SDUI screen responses by:
 * 1. Loading the appropriate example response based on game state
 * 2. Injecting the trace ID
 * 3. Attempting live API composition first
 * 4. Falling back to static examples when live data is unavailable
 * 5. Resolving Ably channel names with the game ID
 */
@Service
public class SduiCompositionService {

    private static final Logger log = LoggerFactory.getLogger(SduiCompositionService.class);
    
    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    
    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public SduiCompositionService(ObjectMapper objectMapper, StatsApiClient statsApiClient) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
    }

    /**
     * Compose a Game Detail SDUI screen response.
     * 
     * @param gameId Game ID
     * @param gameState Game state (pre, live, final)
     * @param clientSchemaVersion Client's schema version
     * @param traceId Trace ID for logging
     */
    public JsonNode composeGameDetail(String gameId, String gameState,
                                      String variant, String clientSchemaVersion, String traceId) throws IOException {
        log.info("Composing game detail: gameId={}, gameState={}, variant={}", gameId, gameState, variant);

        // Always attempt live data first; fallback to examples when unavailable.
        JsonNode baseResponse;
        baseResponse = composeFromLiveData(gameId, gameState);
        if (baseResponse == null) {
            log.warn("Live game detail unavailable, falling back to example for gameState={}", gameState);
            baseResponse = loadExampleResponse(gameState);
        }
        
        if (baseResponse == null) {
            log.warn("No example response found for gameState={}, using 'pre' as default", gameState);
            baseResponse = loadExampleResponse("pre");
        }
        
        // Clone for modification
        ObjectNode response = baseResponse.deepCopy();
        
        // Update IDs and trace
        response.put("id", "game-detail-" + gameId);
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", buildNavigation("game-detail"));
        
        // Resolve Ably channel patterns with actual game ID
        resolveChannelPatterns(response, gameId);
        
        // Apply variant transformations (server-driven composability demo)
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

    /**
     * Compose a Scoreboard SDUI screen response.
     */
    /**
     * Compose a Scoreboard SDUI screen response.
     */
    public JsonNode composeScoreboard(String variant, String clientSchemaVersion, String traceId) throws IOException {
        log.info("Composing scoreboard: variant={}", variant);

        // Always attempt live scoreboard first; fallback to mock/demo data on empty/unavailable live slate.
        ObjectNode response = composeScoreboardFromLiveData();
        if (response == null) {
            log.warn("Live scoreboard unavailable or no in-progress games, falling back to static scoreboard example");
            response = loadExampleByFilename("scoreboard-live.json");
        }

        if (response == null) {
            response = objectMapper.createObjectNode();
            response.put("id", "scoreboard-live");
            response.put("title", "Today's Games");
            response.set("sections", objectMapper.createArrayNode());
        }

        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.set("navigation", buildNavigation("scoreboard"));

        if (variant != null) {
            switch (variant.toUpperCase()) {
                case "E" -> applyScoreboardVariantPromo(response);
                case "F" -> applyScoreboardVariantPromoRail(response);
                default -> log.debug("Scoreboard using default variant (no transformation)");
            }
        }

        return response;
    }
    
    /**
     * Compose response from live NBA API data.
     * Returns null if live data is not available.
     */
    private JsonNode composeFromLiveData(String gameId, String gameState) {
        try {
            // Try to fetch real boxscore data
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
            
            // Build complete SDUI response from live data
            ObjectNode response = objectMapper.createObjectNode();
            response.put("id", "game-detail-" + gameId);
            response.put("type", "game_detail");
            response.put("schemaVersion", schemaVersion);
            
            ArrayNode sections = objectMapper.createArrayNode();
            
            // 1. ScoreboardHeader section from live data
            ObjectNode scoreboardHeader = buildScoreboardHeaderFromLive(game, gameId);
            sections.add(scoreboardHeader);
            
            // 2. StatLine section (top performers from boxscore - direct CDN polling)
            ObjectNode statLineSection = buildStatLineSectionFromLive(game, gameId);
            if (statLineSection != null) {
                sections.add(statLineSection);
            }

            // 2b. Row layout – home/away top performers side-by-side
            ObjectNode rowSection = buildRowSectionFromLive(game, gameId);
            if (rowSection != null) {
                sections.add(rowSection);
            }
            
            // 3. ContentRail from demo example
            ObjectNode contentRail = loadSectionFromExample(gameState, "ContentRail");
            if (contentRail != null) {
                sections.add(contentRail);
            }
            
            // 4. TabGroup with live boxscore data (direct CDN polling)
            ObjectNode tabGroup = buildTabGroupFromLive(game, gameId);
            if (tabGroup != null) {
                sections.add(tabGroup);
            }
            
            // 5. PromoBanner from demo example
            ObjectNode promoBanner = loadSectionFromExample(gameState, "PromoBanner");
            if (promoBanner != null) {
                sections.add(promoBanner);
            }
            
            response.set("sections", sections);
            return response;
            
        } catch (Exception e) {
            log.error("Failed to compose from live data for gameId={}: {}", gameId, e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Load a section by type from demo example file.
     */
    private ObjectNode loadSectionFromExample(String gameState, String sectionType) {
        try {
            JsonNode example = loadExampleResponse(gameState);
            if (example == null) return null;
            
            ArrayNode sections = (ArrayNode) example.get("sections");
            if (sections == null) return null;
            
            for (JsonNode section : sections) {
                if (sectionType.equals(section.path("type").asText())) {
                    return section.deepCopy();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load {} from example: {}", sectionType, e.getMessage());
        }
        return null;
    }
    
    /**
     * Build TabGroup section with live boxscore data.
     * Uses direct CDN URL for polling instead of SDUI server.
     */
    private ObjectNode buildTabGroupFromLive(JsonNode game, String gameId) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-tabs");
        section.put("type", "TabGroup");
        section.put("analyticsId", "game_tabs");
        
        // Direct polling from NBA CDN boxscore endpoint
        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "poll");
        refreshPolicy.put("intervalMs", 30000);
        refreshPolicy.put("url", "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
        refreshPolicy.put("dataPath", "game"); // Extract game object from response
        section.set("refreshPolicy", refreshPolicy);
        
        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "activeTab");
        data.put("defaultTab", "boxscore");
        
        // Tab definitions
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
        
        // Tab contents
        ObjectNode tabContents = objectMapper.createObjectNode();
        
        // Build boxscore content with real player stats
        ArrayNode boxscoreContent = objectMapper.createArrayNode();
        
        // Home team boxscore
        ObjectNode homeBoxscore = buildTeamBoxscoreStatLine(game.path("homeTeam"));
        if (homeBoxscore != null) {
            boxscoreContent.add(homeBoxscore);
        }
        
        // Away team boxscore
        ObjectNode awayBoxscore = buildTeamBoxscoreStatLine(game.path("awayTeam"));
        if (awayBoxscore != null) {
            boxscoreContent.add(awayBoxscore);
        }
        
        tabContents.set("boxscore", boxscoreContent);
        
        // Play-by-play placeholder (would need different API)
        ArrayNode playByPlayContent = objectMapper.createArrayNode();
        ObjectNode playByPlayPlaceholder = objectMapper.createObjectNode();
        playByPlayPlaceholder.put("id", "play-by-play");
        playByPlayPlaceholder.put("type", "StatLine");
        ObjectNode pbpData = objectMapper.createObjectNode();
        pbpData.put("title", "Play-by-Play");
        pbpData.set("stats", objectMapper.createArrayNode());
        playByPlayPlaceholder.set("data", pbpData);
        playByPlayContent.add(playByPlayPlaceholder);
        tabContents.set("playbyplay", playByPlayContent);
        
        data.set("tabContents", tabContents);
        section.set("data", data);
        
        return section;
    }
    
    /**
     * Build a StatLine section for a team's boxscore.
     */
    private ObjectNode buildTeamBoxscoreStatLine(JsonNode team) {
        if (team.isMissingNode() || !team.has("players")) {
            return null;
        }
        
        String teamName = team.path("teamName").asText();
        String teamCity = team.path("teamCity").asText();
        String teamTricode = team.path("teamTricode").asText();
        int teamId = team.path("teamId").asInt();
        
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "boxscore-" + teamTricode.toLowerCase());
        section.put("type", "StatLine");
        
        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", teamCity + " " + teamName);
        
        ArrayNode stats = objectMapper.createArrayNode();
        ArrayNode players = (ArrayNode) team.get("players");
        
        for (JsonNode player : players) {
            JsonNode playerStats = player.path("statistics");
            String minutes = playerStats.path("minutes").asText();
            
            // Skip players who haven't played
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
            statItem.put("teamId", teamId);
            
            // Format minutes nicely (API returns "PT35M20.00S" format)
            String formattedMinutes = formatMinutes(minutes);
            statItem.put("statCategory", "MIN");
            statItem.put("statValue", formattedMinutes);
            
            // Add detailed stats
            ObjectNode additionalStats = objectMapper.createObjectNode();
            additionalStats.put("points", playerStats.path("points").asInt());
            additionalStats.put("rebounds", playerStats.path("reboundsTotal").asInt());
            additionalStats.put("assists", playerStats.path("assists").asInt());
            additionalStats.put("steals", playerStats.path("steals").asInt());
            additionalStats.put("blocks", playerStats.path("blocks").asInt());
            additionalStats.put("fgm", playerStats.path("fieldGoalsMade").asInt());
            additionalStats.put("fga", playerStats.path("fieldGoalsAttempted").asInt());
            additionalStats.put("threePm", playerStats.path("threePointersMade").asInt());
            additionalStats.put("threePa", playerStats.path("threePointersAttempted").asInt());
            additionalStats.put("ftm", playerStats.path("freeThrowsMade").asInt());
            additionalStats.put("fta", playerStats.path("freeThrowsAttempted").asInt());
            additionalStats.put("plusMinus", playerStats.path("plusMinusPoints").asInt());
            statItem.set("additionalStats", additionalStats);
            
            stats.add(statItem);
        }
        
        data.set("stats", stats);
        section.set("data", data);
        
        return section;
    }
    
    /**
     * Format ISO 8601 duration (PT35M20.00S) to MM:SS format.
     */
    private String formatMinutes(String isoMinutes) {
        if (isoMinutes == null || isoMinutes.isEmpty()) {
            return "0:00";
        }
        
        // Handle both ISO format (PT35M20.00S) and already formatted (35:20)
        if (isoMinutes.startsWith("PT")) {
            try {
                String clean = isoMinutes.substring(2); // Remove "PT"
                int mins = 0;
                int secs = 0;
                
                int mIndex = clean.indexOf('M');
                if (mIndex > 0) {
                    mins = Integer.parseInt(clean.substring(0, mIndex));
                    clean = clean.substring(mIndex + 1);
                }
                
                int sIndex = clean.indexOf('S');
                if (sIndex > 0) {
                    double secValue = Double.parseDouble(clean.substring(0, sIndex));
                    secs = (int) secValue;
                }
                
                return String.format("%d:%02d", mins, secs);
            } catch (Exception e) {
                return isoMinutes;
            }
        }
        
        return isoMinutes;
    }

    private ObjectNode buildScoreboardHeaderFromLive(JsonNode game, String gameId) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "scoreboard-header");
        section.put("type", "ScoreboardHeader");
        
        section.set("dataBindings", buildLinescoreBindings());
        
        // Refresh policy for Ably channel
        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "sse");
        refreshPolicy.put("channel", gameId + ":linescore");
        section.set("refreshPolicy", refreshPolicy);
        
        // Build data from live game
        ObjectNode data = objectMapper.createObjectNode();
        
        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");
        
        // Home team data
        ObjectNode homeData = objectMapper.createObjectNode();
        homeData.put("teamId", homeTeam.path("teamId").asInt());
        homeData.put("teamTricode", homeTeam.path("teamTricode").asText());
        homeData.put("teamName", homeTeam.path("teamName").asText());
        homeData.put("teamCity", homeTeam.path("teamCity").asText());
        homeData.put("score", homeTeam.path("score").asInt());
        homeData.put("logoUrl", "https://cdn.nba.com/logos/nba/" + 
                homeTeam.path("teamId").asText() + "/primary/L/logo.svg");
        data.set("homeTeam", homeData);
        
        // Away team data
        ObjectNode awayData = objectMapper.createObjectNode();
        awayData.put("teamId", awayTeam.path("teamId").asInt());
        awayData.put("teamTricode", awayTeam.path("teamTricode").asText());
        awayData.put("teamName", awayTeam.path("teamName").asText());
        awayData.put("teamCity", awayTeam.path("teamCity").asText());
        awayData.put("score", awayTeam.path("score").asInt());
        awayData.put("logoUrl", "https://cdn.nba.com/logos/nba/" + 
                awayTeam.path("teamId").asText() + "/primary/L/logo.svg");
        data.set("awayTeam", awayData);
        
        // Game status
        data.put("gameId", game.path("gameId").asText());
        data.put("gameClock", game.path("gameClock").asText(""));
        data.put("period", game.path("period").asInt());
        data.put("gameStatus", game.path("gameStatus").asInt());
        data.put("gameStatusText", game.path("gameStatusText").asText());
        
        section.set("data", data);
        return section;
    }
    
    /**
     * Build StatLine section from player statistics in boxscore.
     * Sorted by team (home first, away second) then by points descending.
     * Uses direct CDN URL for polling instead of SDUI server.
     */
    private ObjectNode buildStatLineSectionFromLive(JsonNode game, String gameId) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "top-performers");
        section.put("type", "StatLine");
        
        // Direct polling from NBA CDN boxscore endpoint
        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "poll");
        refreshPolicy.put("intervalMs", 30000);
        refreshPolicy.put("url", "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
        refreshPolicy.put("dataPath", "game"); // Extract game object from response
        section.set("refreshPolicy", refreshPolicy);
        
        ArrayNode stats = objectMapper.createArrayNode();
        
        // Get top performers from home team (sorted by points), then away team
        List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.path("homeTeam"), 3);
        List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.path("awayTeam"), 3);
        
        // Add home team performers first, then away
        homePerformers.forEach(stats::add);
        awayPerformers.forEach(stats::add);
        
        if (stats.isEmpty()) {
            return null; // No stat data yet
        }
        
        // Put data in the 'data' field with 'stats' array (matching Android model)
        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Top Performers");
        data.set("stats", stats);
        section.set("data", data);
        
        return section;
    }

    /**
     * Build a Row section that places home and away team stat summaries side-by-side.
     * On narrow screens the children collapse to a vertical stack.
     */
    private ObjectNode buildRowSectionFromLive(JsonNode game, String gameId) {
        try {
            // Build home team child section
            List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.path("homeTeam"), 2);
            List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.path("awayTeam"), 2);
            if (homePerformers.isEmpty() && awayPerformers.isEmpty()) return null;

            String homeTricode = game.path("homeTeam").path("teamTricode").asText("HOME");
            String awayTricode = game.path("awayTeam").path("teamTricode").asText("AWAY");

            ObjectNode homeChild = buildStatLineChild("row-home-stats", homeTricode + " Leaders", homePerformers);
            ObjectNode awayChild = buildStatLineChild("row-away-stats", awayTricode + " Leaders", awayPerformers);

            ArrayNode children = objectMapper.createArrayNode();
            children.add(homeChild);
            children.add(awayChild);

            ObjectNode rowData = objectMapper.createObjectNode();
            rowData.set("children", children);
            rowData.put("spacing", 16);
            rowData.put("breakpoint", 600);

            ObjectNode section = objectMapper.createObjectNode();
            section.put("id", "team-leaders-row");
            section.put("type", "Row");
            section.set("data", rowData);

            return section;
        } catch (Exception e) {
            log.warn("Failed to build Row section from live data: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Helper: build a lightweight StatLine child for use inside a Row.
     */
    private ObjectNode buildStatLineChild(String id, String title, List<ObjectNode> performers) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", id);
        section.put("type", "StatLine");

        ArrayNode stats = objectMapper.createArrayNode();
        performers.forEach(stats::add);

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", title);
        data.set("stats", stats);
        section.set("data", data);
        return section;
    }
    
    /**
     * Get top N performers from a team, sorted by points descending.
     */
    private List<ObjectNode> getTopPerformersFromTeam(JsonNode team, int maxPlayers) {
        List<ObjectNode> performers = new ArrayList<>();
        if (!team.has("players")) return performers;
        
        String teamTricode = team.path("teamTricode").asText();
        int teamId = team.path("teamId").asInt();
        
        ArrayNode players = (ArrayNode) team.get("players");
        
        // Collect all players with their points
        List<Map.Entry<JsonNode, Integer>> playerPoints = new ArrayList<>();
        for (JsonNode player : players) {
            JsonNode playerStats = player.path("statistics");
            int points = playerStats.path("points").asInt();
            if (points >= 5) { // Minimum threshold
                playerPoints.add(Map.entry(player, points));
            }
        }
        
        // Sort by points descending
        playerPoints.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        // Take top N performers
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
            
            // Primary stat: points
            item.put("statCategory", "PTS");
            item.put("statValue", String.valueOf(points));
            item.put("statLabel", "Points");
            
            // Additional stats
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
    
    /**
     * Get player stats for the polling endpoint.
     */
    public JsonNode getPlayerStats(String gameId) throws IOException {
        // Try to fetch from real API first
        try {
            JsonNode boxscore = statsApiClient.getBoxscore(gameId);
            if (boxscore != null) {
                return transformBoxscoreToStatLines(boxscore);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch boxscore from API, using mock data: {}", e.getMessage());
        }
        
        // Fall back to mock data
        return createMockStats(gameId);
    }

    private JsonNode loadExampleResponse(String gameState) throws IOException {
        String filename = switch (gameState.toLowerCase()) {
            case "live" -> "game-detail-live.json";
            case "final" -> "game-detail-final.json";
            default -> "game-detail-pre.json";
        };
        
        // Try loading from classpath first
        try {
            ClassPathResource resource = new ClassPathResource("examples/" + filename);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return objectMapper.readTree(is);
                }
            }
        } catch (Exception e) {
            log.debug("Could not load from classpath, trying file system");
        }
        
        // Try loading from file system (relative to project)
        Path filePath = Path.of("../schema/examples/" + filename);
        if (Files.exists(filePath)) {
            return objectMapper.readTree(Files.readString(filePath));
        }
        
        // Try absolute path from workspace root
        filePath = Path.of("schema/examples/" + filename);
        if (Files.exists(filePath)) {
            return objectMapper.readTree(Files.readString(filePath));
        }
        
        log.error("Could not load example file: {}", filename);
        return null;
    }

    private ObjectNode loadExampleByFilename(String filename) throws IOException {
        try {
            ClassPathResource resource = new ClassPathResource("examples/" + filename);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    return (ObjectNode) objectMapper.readTree(is);
                }
            }
        } catch (Exception e) {
            log.debug("Could not load {} from classpath, trying file system", filename);
        }

        Path filePath = Path.of("../schema/examples/" + filename);
        if (Files.exists(filePath)) {
            return (ObjectNode) objectMapper.readTree(Files.readString(filePath));
        }

        filePath = Path.of("schema/examples/" + filename);
        if (Files.exists(filePath)) {
            return (ObjectNode) objectMapper.readTree(Files.readString(filePath));
        }

        log.error("Could not load example file: {}", filename);
        return null;
    }

    private ObjectNode composeScoreboardFromLiveData() {
        try {
            JsonNode scoreboard = statsApiClient.getScoreboard();
            if (scoreboard == null) {
                return null;
            }

            JsonNode games = scoreboard.path("scoreboard").path("games");
            if (!games.isArray()) {
                return null;
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("id", "scoreboard-live");
            response.put("title", "Today's Games");
            response.put("analyticsId", "scoreboard_live");

            ArrayNode sections = objectMapper.createArrayNode();
            for (JsonNode game : games) {
                String gameId = game.path("gameId").asText(null);
                if (gameId == null || gameId.isBlank()) {
                    continue;
                }
                sections.add(buildScoreboardRowSection(game, gameId));
            }
            if (sections.isEmpty()) {
                log.warn("Live scoreboard has no games for today");
                return null;
            }
            response.set("sections", sections);
            return response;
        } catch (Exception e) {
            log.error("Failed to compose scoreboard from live data: {}", e.getMessage(), e);
            return null;
        }
    }

    private ObjectNode buildScoreboardRowSection(JsonNode game, String gameId) {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "game-" + gameId);
        section.put("type", "ScoreboardHeader");
        section.put("analyticsId", "scoreboard_row_" + gameId);

        ObjectNode data = objectMapper.createObjectNode();
        int gameStatus = game.path("gameStatus").asInt(1);

        if (gameStatus == 2) {
            // Live game: subscribe to Ably linescore channel
            ObjectNode refreshPolicy = objectMapper.createObjectNode();
            refreshPolicy.put("type", "sse");
            refreshPolicy.put("channel", gameId + ":linescore");
            section.set("refreshPolicy", refreshPolicy);

            section.set("dataBindings", buildLinescoreBindings());
        } else {
            section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        }
        data.put("gameId", gameId);
        data.put("gameStatus", gameStatus);
        data.put("gameStatusText", game.path("gameStatusText").asText(""));
        data.put("period", game.path("period").asInt(0));
        data.put("gameClock", game.path("gameClock").asText(""));

        data.set("homeTeam", mapGameCardTeam(game.path("homeTeam")));
        data.set("awayTeam", mapGameCardTeam(game.path("awayTeam")));

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

    private ObjectNode mapGameCardTeam(JsonNode team) {
        ObjectNode mapped = objectMapper.createObjectNode();
        mapped.put("teamId", team.path("teamId").asInt());
        mapped.put("teamTricode", team.path("teamTricode").asText(""));
        mapped.put("teamName", team.path("teamName").asText(""));
        mapped.put("teamCity", team.path("teamCity").asText(""));
        mapped.put("score", team.path("score").asInt(0));
        mapped.put("logoUrl", "https://cdn.nba.com/logos/nba/" + team.path("teamId").asText() + "/global/L/logo.svg");

        if (team.has("wins")) {
            mapped.put("wins", team.path("wins").asInt());
        }
        if (team.has("losses")) {
            mapped.put("losses", team.path("losses").asInt());
        }
        return mapped;
    }

    private ObjectNode mapLeader(JsonNode leader) {
        ObjectNode mapped = objectMapper.createObjectNode();
        mapped.put("name", leader.path("name").asText(""));
        mapped.put("points", leader.path("points").asInt(0));
        mapped.put("rebounds", leader.path("rebounds").asInt(0));
        mapped.put("assists", leader.path("assists").asInt(0));
        return mapped;
    }

    private ObjectNode buildLinescoreBindings() {
        ObjectNode dataBindings = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();

        bindings.add(bindingPath("$.homeTeam.score", "homeTeam.score"));
        bindings.add(bindingPath("$.awayTeam.score", "awayTeam.score"));
        bindings.add(bindingPath("$.gameStatusText", "gameStatusText"));
        bindings.add(bindingPath("$.period", "period"));

        dataBindings.set("bindings", bindings);
        return dataBindings;
    }

    private ObjectNode bindingPath(String sourcePath, String targetPath) {
        ObjectNode path = objectMapper.createObjectNode();
        path.put("sourcePath", sourcePath);
        path.put("targetPath", targetPath);
        return path;
    }

    private ObjectNode buildNavigation(String activeScreenId) {
        ObjectNode navigation = objectMapper.createObjectNode();
        ArrayNode items = objectMapper.createArrayNode();

        boolean gamesSelected = "scoreboard".equals(activeScreenId) || "game-detail".equals(activeScreenId);

        ObjectNode games = objectMapper.createObjectNode();
        games.put("id", "games");
        games.put("label", "Games");
        games.put("icon", "scoreboard");
        games.put("targetUri", "nba://scoreboard");
        games.put("selected", gamesSelected);
        items.add(games);

        ObjectNode teams = objectMapper.createObjectNode();
        teams.put("id", "teams");
        teams.put("label", "Teams");
        teams.put("icon", "groups");
        teams.put("targetUri", "nba://teams");
        teams.put("selected", false);
        items.add(teams);

        ObjectNode standings = objectMapper.createObjectNode();
        standings.put("id", "standings");
        standings.put("label", "Standings");
        standings.put("icon", "table_chart");
        standings.put("targetUri", "nba://standings");
        standings.put("selected", false);
        items.add(standings);

        navigation.set("items", items);
        return navigation;
    }

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

    private void applyVariantB(ObjectNode response) {
        // Variant B: Swap ContentRail and TabGroup positions
        if (!response.has("sections")) return;
        
        ArrayNode sections = (ArrayNode) response.get("sections");
        int contentRailIndex = -1;
        int tabGroupIndex = -1;
        
        for (int i = 0; i < sections.size(); i++) {
            String type = sections.get(i).path("type").asText();
            if ("ContentRail".equals(type)) contentRailIndex = i;
            if ("TabGroup".equals(type)) tabGroupIndex = i;
        }
        
        if (contentRailIndex >= 0 && tabGroupIndex >= 0 && contentRailIndex < tabGroupIndex) {
            // Swap positions
            JsonNode contentRail = sections.get(contentRailIndex);
            JsonNode tabGroup = sections.get(tabGroupIndex);
            sections.set(contentRailIndex, tabGroup);
            sections.set(tabGroupIndex, contentRail);
            log.debug("Applied variant B: swapped ContentRail and TabGroup positions");
        }
    }

    private void applyVariantC(ObjectNode response) {
        // Variant C: Minimal layout - remove PromoBanner and StatLine
        // Demonstrates: sections can be removed without any client code change
        if (!response.has("sections")) return;
        
        ArrayNode sections = (ArrayNode) response.get("sections");
        ArrayNode filtered = objectMapper.createArrayNode();
        
        for (JsonNode section : sections) {
            String type = section.path("type").asText();
            if (!"PromoBanner".equals(type) && !"StatLine".equals(type)) {
                filtered.add(section);
            }
        }
        
        response.set("sections", filtered);
        log.debug("Applied variant C: removed PromoBanner and StatLine, {} sections remaining", filtered.size());
    }

    private void applyVariantD(ObjectNode response) {
        // Variant D: Add a second ContentRail (e.g. "Trending Videos")
        // Demonstrates: same section type reused multiple times, no client change needed
        if (!response.has("sections")) return;
        
        ArrayNode sections = (ArrayNode) response.get("sections");
        
        // Build a second ContentRail with different content
        ObjectNode extraRail = objectMapper.createObjectNode();
        extraRail.put("id", "trending-videos");
        extraRail.put("type", "ContentRail");
        extraRail.put("analyticsId", "trending_videos_rail");
        
        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "static");
        extraRail.set("refreshPolicy", refreshPolicy);
        
        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Trending Videos");
        
        ArrayNode cards = objectMapper.createArrayNode();
        
        ObjectNode card1 = objectMapper.createObjectNode();
        card1.put("id", "trending-1");
        card1.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/top10-plays.jpg");
        card1.put("headline", "Top 10 Plays of the Night");
        card1.put("subhead", "Last night's best moments");
        card1.put("contentType", "video");
        card1.put("duration", "4:30");
        ObjectNode action1 = objectMapper.createObjectNode();
        action1.put("trigger", "onTap");
        action1.put("type", "navigate");
        action1.put("targetUri", "nba://video/top10-plays");
        action1.put("fallbackUrl", "https://www.nba.com/video/top10-plays");
        card1.set("action", action1);
        cards.add(card1);
        
        ObjectNode card2 = objectMapper.createObjectNode();
        card2.put("id", "trending-2");
        card2.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/dunk-contest.jpg");
        card2.put("headline", "Playoff Intensity");
        card2.put("subhead", "The best of postseason basketball");
        card2.put("contentType", "video");
        card2.put("duration", "2:15");
        ObjectNode action2 = objectMapper.createObjectNode();
        action2.put("trigger", "onTap");
        action2.put("type", "navigate");
        action2.put("targetUri", "nba://video/playoff-intensity");
        action2.put("fallbackUrl", "https://www.nba.com/video/playoff-intensity");
        card2.set("action", action2);
        cards.add(card2);
        
        ObjectNode card3 = objectMapper.createObjectNode();
        card3.put("id", "trending-3");
        card3.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/press-conference.jpg");
        card3.put("headline", "Post-Game Press Conference");
        card3.put("subhead", "Hear from the coaches");
        card3.put("contentType", "video");
        card3.put("duration", "6:00");
        ObjectNode action3 = objectMapper.createObjectNode();
        action3.put("trigger", "onTap");
        action3.put("type", "navigate");
        action3.put("targetUri", "nba://video/post-game-presser");
        action3.put("fallbackUrl", "https://www.nba.com/video/post-game-presser");
        card3.set("action", action3);
        cards.add(card3);
        
        data.set("cards", cards);
        extraRail.set("data", data);
        
        // Insert the extra rail after the existing ContentRail
        ArrayNode updated = objectMapper.createArrayNode();
        for (JsonNode section : sections) {
            updated.add(section);
            if ("ContentRail".equals(section.path("type").asText())) {
                updated.add(extraRail);
            }
        }
        
        response.set("sections", updated);
        log.debug("Applied variant D: added Trending Videos ContentRail, {} sections total", updated.size());
    }

    private void applyScoreboardVariantPromo(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        ArrayNode updated = objectMapper.createArrayNode();
        updated.add(buildScoreboardPromoBanner());
        for (JsonNode section : sections) {
            updated.add(section);
        }
        response.set("sections", updated);
        log.debug("Applied scoreboard variant Promo: inserted PromoBanner at index 0, {} sections total", updated.size());
    }

    private void applyScoreboardVariantPromoRail(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        int gameCount = sections.size();

        ArrayNode updated = objectMapper.createArrayNode();
        updated.add(buildScoreboardPromoBanner());

        for (int i = 0; i < sections.size(); i++) {
            updated.add(sections.get(i));
            if (i == 1 && gameCount > 2) {
                updated.add(buildScoreboardContentRail());
            }
        }

        response.set("sections", updated);
        log.debug("Applied scoreboard variant PromoRail: promo at 0, rail after game 2 ({}), {} sections total",
                gameCount > 2 ? "inserted" : "skipped", updated.size());
    }

    private ObjectNode buildScoreboardPromoBanner() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "scoreboard-promo");
        section.put("type", "PromoBanner");
        section.put("analyticsId", "scoreboard_promo_banner");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "NBA League Pass");
        data.put("description", "Watch every out-of-market game live or on demand.");
        data.put("imageUrl", "https://cdn.nba.com/promo/league-pass-banner.jpg");

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://leaguepass");
        action.put("fallbackUrl", "https://www.nba.com/watch/league-pass");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    private ObjectNode buildScoreboardContentRail() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "scoreboard-content-rail");
        section.put("type", "ContentRail");
        section.put("analyticsId", "scoreboard_content_rail");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Around the League");

        ArrayNode cards = objectMapper.createArrayNode();

        ObjectNode card1 = objectMapper.createObjectNode();
        card1.put("id", "league-1");
        card1.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/top10-plays.jpg");
        card1.put("headline", "Top 10 Plays of the Night");
        card1.put("subhead", "Last night's best moments");
        card1.put("contentType", "video");
        ObjectNode a1 = objectMapper.createObjectNode();
        a1.put("trigger", "onTap");
        a1.put("type", "navigate");
        a1.put("targetUri", "nba://video/top10-plays");
        card1.set("action", a1);
        cards.add(card1);

        ObjectNode card2 = objectMapper.createObjectNode();
        card2.put("id", "league-2");
        card2.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/standings.jpg");
        card2.put("headline", "Standings Update");
        card2.put("subhead", "Current playoff picture");
        card2.put("contentType", "article");
        ObjectNode a2 = objectMapper.createObjectNode();
        a2.put("trigger", "onTap");
        a2.put("type", "navigate");
        a2.put("targetUri", "nba://standings");
        card2.set("action", a2);
        cards.add(card2);

        data.set("cards", cards);
        section.set("data", data);
        return section;
    }

    private JsonNode transformBoxscoreToStatLines(JsonNode boxscore) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("title", "Top Performers");
        
        ArrayNode stats = objectMapper.createArrayNode();
        
        // Extract top performers from home and away teams
        extractTopPerformers(boxscore.path("homeTeam"), stats);
        extractTopPerformers(boxscore.path("awayTeam"), stats);
        
        result.set("stats", stats);
        return result;
    }

    private void extractTopPerformers(JsonNode team, ArrayNode stats) {
        if (!team.has("players")) return;
        
        String teamTricode = team.path("teamTricode").asText();
        ArrayNode players = (ArrayNode) team.get("players");
        
        // Get top 2 scorers from each team
        players.forEach(player -> {
            if (stats.size() >= 8) return; // Limit total
            
            int points = player.path("statistics").path("points").asInt();
            if (points >= 15) { // Only include significant scorers
                ObjectNode statLine = objectMapper.createObjectNode();
                statLine.put("playerId", player.path("personId").asInt());
                statLine.put("playerName", player.path("name").asText());
                statLine.put("playerImageUrl", 
                        "https://cdn.nba.com/headshots/nba/latest/1040x760/" + 
                        player.path("personId").asText() + ".png");
                statLine.put("teamTricode", teamTricode);
                statLine.put("statCategory", "PTS");
                statLine.put("statValue", String.valueOf(points));
                statLine.put("statLabel", "Points");
                stats.add(statLine);
            }
        });
    }

    private JsonNode createMockStats(String gameId) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("title", "Top Performers");
        
        ArrayNode stats = objectMapper.createArrayNode();
        
        // Mock data
        stats.add(createStatLine(1627759, "Jaylen Brown", "BOS", "PTS", "31"));
        stats.add(createStatLine(1628369, "Jayson Tatum", "BOS", "PTS", "28"));
        stats.add(createStatLine(202710, "Jimmy Butler", "MIA", "PTS", "26"));
        stats.add(createStatLine(1629216, "Bam Adebayo", "MIA", "REB", "11"));
        
        result.set("stats", stats);
        return result;
    }

    private ObjectNode createStatLine(int playerId, String name, String team, String category, String value) {
        ObjectNode stat = objectMapper.createObjectNode();
        stat.put("playerId", playerId);
        stat.put("playerName", name);
        stat.put("playerImageUrl", 
                "https://cdn.nba.com/headshots/nba/latest/1040x760/" + playerId + ".png");
        stat.put("teamTricode", team);
        stat.put("statCategory", category);
        stat.put("statValue", value);
        stat.put("statLabel", category.equals("PTS") ? "Points" : "Rebounds");
        return stat;
    }
}
