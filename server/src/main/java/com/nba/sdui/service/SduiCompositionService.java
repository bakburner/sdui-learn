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
import java.util.LinkedHashMap;
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
        response.put("parentUri", "nba://scoreboard");
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

    // ── Boxscore Screen ────────────────────────────────────────────────

    /**
     * Compose a dedicated Boxscore SDUI screen.
     *
     * Layout:
     * <pre>
     *   Screen
     *     state: { boxscore_team, boxscore_away_sortCol, boxscore_away_sortDir,
     *              boxscore_home_sortCol, boxscore_home_sortDir }
     *     sections:
     *       └── TabGroup (team toggle — eager, inline)
     *           ├── Tab "{awayTricode}" → BoxscoreTable
     *           └── Tab "{homeTricode}" → BoxscoreTable
     * </pre>
     *
     * Source data: NBA CDN boxscore endpoint → {@code game.homeTeam / game.awayTeam}.
     */
    public JsonNode composeBoxscore(String gameId, String traceId) throws IOException {
        log.info("Composing boxscore screen: gameId={}", gameId);

        JsonNode boxscore = statsApiClient.getBoxscore(gameId);
        JsonNode game = boxscore != null ? boxscore.path("game") : null;

        boolean hasLiveData = game != null && !game.isMissingNode() && game.has("homeTeam");

        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "boxscore-" + gameId);
        response.put("type", "boxscore");
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.put("parentUri", "nba://scoreboard");
        response.set("navigation", buildNavigation("game-detail"));

        if (!hasLiveData) {
            // Pre-game / unavailable — return shell with empty message
            log.warn("No boxscore data available for gameId={}, returning empty screen", gameId);
            response.set("state", objectMapper.createObjectNode());
            ArrayNode sections = objectMapper.createArrayNode();
            ObjectNode emptySection = objectMapper.createObjectNode();
            emptySection.put("id", "boxscore-empty");
            emptySection.put("type", "BoxscoreTable");
            ObjectNode emptyData = objectMapper.createObjectNode();
            emptyData.put("teamTricode", "");
            emptyData.put("teamName", "");
            emptyData.set("columns", buildBoxscoreColumns());
            emptyData.set("players", objectMapper.createArrayNode());
            emptyData.put("emptyMessage", "Box score will be available once the game begins.");
            emptySection.set("data", emptyData);
            sections.add(emptySection);
            response.set("sections", sections);
            return response;
        }

        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");
        String homeTricode = homeTeam.path("teamTricode").asText("HOME");
        String awayTricode = awayTeam.path("teamTricode").asText("AWAY");
        int gameStatus = game.path("gameStatus").asInt(1);

        // ── Screen state (pre-populated defaults) ──────────────────────
        ObjectNode state = objectMapper.createObjectNode();
        state.put("boxscore_team", awayTricode);
        state.put("boxscore_away_sortCol", "points");
        state.put("boxscore_away_sortDir", "desc");
        state.put("boxscore_home_sortCol", "points");
        state.put("boxscore_home_sortDir", "desc");
        response.set("state", state);

        // ── Sections ───────────────────────────────────────────────────
        ArrayNode sections = objectMapper.createArrayNode();

        // Build individual BoxscoreTable sections per team
        ObjectNode awayTable = buildBoxscoreTableSection(
                awayTeam, gameId, "boxscore_away_sortCol", "boxscore_away_sortDir", gameStatus);
        ObjectNode homeTable = buildBoxscoreTableSection(
                homeTeam, gameId, "boxscore_home_sortCol", "boxscore_home_sortDir", gameStatus);

        // Wrap in TabGroup for team toggling (eager / inline)
        ObjectNode tabGroup = objectMapper.createObjectNode();
        tabGroup.put("id", "boxscore-team-tabs");
        tabGroup.put("type", "TabGroup");
        tabGroup.put("analyticsId", "boxscore_team_toggle");

        ObjectNode tabData = objectMapper.createObjectNode();
        tabData.put("stateKey", "boxscore_team");
        tabData.put("defaultTab", awayTricode);

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode awayTab = objectMapper.createObjectNode();
        awayTab.put("id", "tab-" + awayTricode.toLowerCase());
        awayTab.put("label", awayTricode);
        awayTab.put("stateKey", "boxscore_team");
        awayTab.put("stateValue", awayTricode);
        tabs.add(awayTab);

        ObjectNode homeTab = objectMapper.createObjectNode();
        homeTab.put("id", "tab-" + homeTricode.toLowerCase());
        homeTab.put("label", homeTricode);
        homeTab.put("stateKey", "boxscore_team");
        homeTab.put("stateValue", homeTricode);
        tabs.add(homeTab);

        tabData.set("tabs", tabs);

        // Tab contents — each tab maps to a single-element array containing
        // the corresponding BoxscoreTable section.
        ObjectNode tabContents = objectMapper.createObjectNode();
        ArrayNode awayContent = objectMapper.createArrayNode();
        awayContent.add(awayTable);
        tabContents.set(awayTricode, awayContent);

        ArrayNode homeContent = objectMapper.createArrayNode();
        homeContent.add(homeTable);
        tabContents.set(homeTricode, homeContent);

        tabData.set("tabContents", tabContents);
        tabGroup.set("data", tabData);

        sections.add(tabGroup);
        response.set("sections", sections);

        log.info("Boxscore screen composed: {} @ {}, gameStatus={}, awayPlayers={}, homePlayers={}",
                awayTricode, homeTricode, gameStatus,
                awayTeam.path("players").size(), homeTeam.path("players").size());

        return response;
    }

    // ── BoxscoreTable section builder ──────────────────────────────────

    /**
     * Build a single {@code BoxscoreTable} section for one team.
     */
    private ObjectNode buildBoxscoreTableSection(JsonNode team, String gameId,
                                                  String sortColKey, String sortDirKey,
                                                  int gameStatus) {
        String teamTricode = team.path("teamTricode").asText("");
        String teamName = team.path("teamName").asText("");
        String teamCity = team.path("teamCity").asText("");
        int teamId = team.path("teamId").asInt();

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "boxscore-table-" + teamTricode.toLowerCase());
        section.put("type", "BoxscoreTable");
        section.put("analyticsId", "boxscore_table_" + teamTricode.toLowerCase());

        // Live games get a poll refresh policy
        if (gameStatus == 2) {
            ObjectNode refreshPolicy = objectMapper.createObjectNode();
            refreshPolicy.put("type", "poll");
            refreshPolicy.put("intervalMs", 30000);
            refreshPolicy.put("url",
                    "https://cdn.nba.com/static/json/liveData/boxscore/boxscore_" + gameId + ".json");
            refreshPolicy.put("dataPath", "game");
            section.set("refreshPolicy", refreshPolicy);
        }

        ObjectNode data = objectMapper.createObjectNode();
        data.put("teamTricode", teamTricode);
        data.put("teamName", teamCity + " " + teamName);
        data.put("teamColor", getTeamPrimaryColor(teamTricode));
        data.put("teamLogoUrl",
                "https://cdn.nba.com/logos/nba/" + teamId + "/primary/L/logo.svg");

        // Column definitions — client uses these for header rendering &
        // mapping into each player's stats map
        data.set("columns", buildBoxscoreColumns());

        // Player rows
        ArrayNode playerRows = objectMapper.createArrayNode();
        ArrayNode players = team.has("players") ? (ArrayNode) team.get("players") : objectMapper.createArrayNode();

        for (JsonNode player : players) {
            ObjectNode row = mapPlayerToBoxscoreRow(player, teamTricode);
            if (row != null) {
                playerRows.add(row);
            }
        }
        data.set("players", playerRows);

        // Team totals
        if (team.has("statistics")) {
            data.set("teamTotals", mapTeamStatistics(team.path("statistics")));
        }

        // Sort state references (keys into Screen.state)
        data.put("sortStateKey", sortColKey);
        data.put("sortDirectionStateKey", sortDirKey);

        // Pre-game fallback
        if (playerRows.isEmpty()) {
            data.put("emptyMessage", "Box score will be available once the game begins.");
        }

        section.set("data", data);
        return section;
    }

    /**
     * Standard boxscore column definitions.
     * Clients render left-to-right; the first (player) column is frozen.
     */
    private ArrayNode buildBoxscoreColumns() {
        ArrayNode columns = objectMapper.createArrayNode();
        // The order here defines the default column order the client should use
        columns.add(colDef("min",  "MIN",  true, false, null));
        columns.add(colDef("pts",  "PTS",  true, true,  null));
        columns.add(colDef("reb",  "REB",  true, false, null));
        columns.add(colDef("ast",  "AST",  true, false, null));
        columns.add(colDef("stl",  "STL",  true, false, null));
        columns.add(colDef("blk",  "BLK",  true, false, null));
        columns.add(colDef("to",   "TO",   true, false, null));
        columns.add(colDef("pf",   "PF",   true, false, null));
        columns.add(colDef("fgm",  "FGM",  true, false, null));
        columns.add(colDef("fga",  "FGA",  true, false, null));
        columns.add(colDef("fgPct","FG%",  true, false, null));
        columns.add(colDef("tpm",  "3PM",  true, false, null));
        columns.add(colDef("tpa",  "3PA",  true, false, null));
        columns.add(colDef("tpPct","3P%",  true, false, null));
        columns.add(colDef("ftm",  "FTM",  true, false, null));
        columns.add(colDef("fta",  "FTA",  true, false, null));
        columns.add(colDef("ftPct","FT%",  true, false, null));
        columns.add(colDef("oreb", "OREB", true, false, null));
        columns.add(colDef("dreb", "DREB", true, false, null));
        columns.add(colDef("pm",   "+/-",  true, false, null));
        return columns;
    }

    private ObjectNode colDef(String key, String label, boolean sortable,
                               boolean highlighted, String width) {
        ObjectNode col = objectMapper.createObjectNode();
        col.put("key", key);
        col.put("label", label);
        col.put("sortable", sortable);
        col.put("highlighted", highlighted);
        if (width != null) col.put("width", width);
        return col;
    }

    /**
     * Map a single NBA API player node to a {@code BoxscorePlayerRow}.
     * Returns null for players with status != ACTIVE if they have no stats.
     */
    private ObjectNode mapPlayerToBoxscoreRow(JsonNode player, String teamTricode) {
        int personId = player.path("personId").asInt();
        String status = player.path("status").asText("");
        String played = player.path("played").asText("0");
        String notPlayingReason = player.path("notPlayingReason").asText("");

        // Derive display name — prefer nameI (abbreviated), then name, then first+last
        String name = player.path("nameI").asText("");
        if (name.isEmpty()) {
            name = player.path("name").asText("");
        }
        if (name.isEmpty()) {
            name = player.path("firstName").asText("") + " " + player.path("familyName").asText("");
        }

        ObjectNode row = objectMapper.createObjectNode();
        row.put("playerId", String.valueOf(personId));
        row.put("name", name.trim());
        row.put("position", player.path("position").asText(""));
        row.put("jerseyNumber", player.path("jerseyNum").asText(""));
        row.put("imageUrl",
                "https://cdn.nba.com/headshots/nba/latest/1040x760/" + personId + ".png");
        row.put("starter", "1".equals(player.path("starter").asText("0")));

        // Stats map — keyed to match column definitions above
        JsonNode s = player.path("statistics");
        ObjectNode stats = objectMapper.createObjectNode();

        boolean didPlay = "1".equals(played) && !"PT0M00.00S".equals(s.path("minutes").asText(""));

        if (didPlay) {
            stats.put("min", formatMinutes(s.path("minutes").asText("")));
            stats.put("pts", s.path("points").asInt());
            stats.put("reb", s.path("reboundsTotal").asInt());
            stats.put("ast", s.path("assists").asInt());
            stats.put("stl", s.path("steals").asInt());
            stats.put("blk", s.path("blocks").asInt());
            stats.put("to",  s.path("turnovers").asInt());
            stats.put("pf",  s.path("foulsPersonal").asInt());
            stats.put("fgm", s.path("fieldGoalsMade").asInt());
            stats.put("fga", s.path("fieldGoalsAttempted").asInt());
            stats.put("fgPct", formatPct(s.path("fieldGoalsPercentage").asDouble()));
            stats.put("tpm", s.path("threePointersMade").asInt());
            stats.put("tpa", s.path("threePointersAttempted").asInt());
            stats.put("tpPct", formatPct(s.path("threePointersPercentage").asDouble()));
            stats.put("ftm", s.path("freeThrowsMade").asInt());
            stats.put("fta", s.path("freeThrowsAttempted").asInt());
            stats.put("ftPct", formatPct(s.path("freeThrowsPercentage").asDouble()));
            stats.put("oreb", s.path("reboundsOffensive").asInt());
            stats.put("dreb", s.path("reboundsDefensive").asInt());
            stats.put("pm",  (int) s.path("plusMinusPoints").asDouble());
        } else {
            // DNP — provide notPlayingReason as a stat entry so clients can display it
            stats.put("min", notPlayingReason.isEmpty() ? "DNP" : notPlayingReason);
        }

        row.set("stats", stats);

        // Navigate to player profile on tap
        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://player/" + personId);
        actions.add(action);
        row.set("actions", actions);

        return row;
    }

    /**
     * Map the team-level statistics node to a {@code BoxscorePlayerStatistics} map
     * (same key namespace as player rows so the totals row aligns to columns).
     */
    private ObjectNode mapTeamStatistics(JsonNode s) {
        ObjectNode totals = objectMapper.createObjectNode();
        totals.put("min", formatMinutes(s.path("minutes").asText("")));
        totals.put("pts", s.path("points").asInt());
        totals.put("reb", s.path("reboundsTotal").asInt());
        totals.put("ast", s.path("assists").asInt());
        totals.put("stl", s.path("steals").asInt());
        totals.put("blk", s.path("blocks").asInt());
        totals.put("to",  s.path("turnovers").asInt());
        totals.put("pf",  s.path("foulsPersonal").asInt());
        totals.put("fgm", s.path("fieldGoalsMade").asInt());
        totals.put("fga", s.path("fieldGoalsAttempted").asInt());
        totals.put("fgPct", formatPct(s.path("fieldGoalsPercentage").asDouble()));
        totals.put("tpm", s.path("threePointersMade").asInt());
        totals.put("tpa", s.path("threePointersAttempted").asInt());
        totals.put("tpPct", formatPct(s.path("threePointersPercentage").asDouble()));
        totals.put("ftm", s.path("freeThrowsMade").asInt());
        totals.put("fta", s.path("freeThrowsAttempted").asInt());
        totals.put("ftPct", formatPct(s.path("freeThrowsPercentage").asDouble()));
        totals.put("oreb", s.path("reboundsOffensive").asInt());
        totals.put("dreb", s.path("reboundsDefensive").asInt());
        totals.put("pm",  (int) s.path("plusMinusPoints").asDouble());
        return totals;
    }

    /**
     * Format a decimal like 0.565 → ".565" (NBA percentage style).
     */
    private String formatPct(double pct) {
        if (pct == 0.0) return ".000";
        return String.format("%.3f", pct).substring(1); // strip leading zero
    }

    /**
     * Resolve a rough team primary colour by tricode. Clients may override.
     */
    private String getTeamPrimaryColor(String tricode) {
        return switch (tricode) {
            case "ATL" -> "#E03A3E";
            case "BOS" -> "#007A33";
            case "BKN" -> "#000000";
            case "CHA" -> "#1D1160";
            case "CHI" -> "#CE1141";
            case "CLE" -> "#860038";
            case "DAL" -> "#00538C";
            case "DEN" -> "#0E2240";
            case "DET" -> "#C8102E";
            case "GSW" -> "#1D428A";
            case "HOU" -> "#CE1141";
            case "IND" -> "#002D62";
            case "LAC" -> "#C8102E";
            case "LAL" -> "#552583";
            case "MEM" -> "#5D76A9";
            case "MIA" -> "#98002E";
            case "MIL" -> "#00471B";
            case "MIN" -> "#0C2340";
            case "NOP" -> "#0C2340";
            case "NYK" -> "#006BB6";
            case "OKC" -> "#007AC1";
            case "ORL" -> "#0077C0";
            case "PHI" -> "#006BB6";
            case "PHX" -> "#1D1160";
            case "POR" -> "#E03A3E";
            case "SAC" -> "#5A2D81";
            case "SAS" -> "#C4CED4";
            case "TOR" -> "#CE1141";
            case "UTA" -> "#002B5C";
            case "WAS" -> "#002B5C";
            default -> "#17408B"; // NBA blue
        };
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
            response.put("parentUri", "nba://scoreboard");
            
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

    // ── Demos Kitchen-Sink Screen ──────────────────────────────────────

    /**
     * Compose a kitchen-sink demo screen showcasing all 10 semantic section types
     * with static mock data.  No external API calls.
     */
    public JsonNode composeDemos(String traceId) {
        ObjectNode screen = objectMapper.createObjectNode();
        screen.put("id", "demos");
        screen.put("schemaVersion", schemaVersion);
        screen.put("title", "SDUI Section Types");
        screen.put("analyticsId", "demos-kitchen-sink");
        screen.put("traceId", traceId);
        screen.put("parentUri", "nba://scoreboard");

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "static");
        screen.set("defaultRefreshPolicy", refreshPolicy);

        screen.set("navigation", buildNavigation("demos"));

        ArrayNode sections = objectMapper.createArrayNode();

        // 1. ScoreboardHeader — mock matchup
        sections.add(buildDemoScoreboardHeader());

        // 2. StatLine — mock player leaders
        sections.add(buildDemoStatLine());

        // 3. PromoBanner — mock promotional card
        sections.add(buildDemoPromoBanner());

        // 4. ContentCard — standalone card
        sections.add(buildDemoContentCard());

        // 5. ContentRail — horizontal scroll of cards
        sections.add(buildDemoContentRail());

        // 6. GameCard — mock game tile
        sections.add(buildDemoGameCard());

        // 7. Row — key-value row
        sections.add(buildDemoRow());

        // 8. TabGroup — tabs wrapping sub-sections
        sections.add(buildDemoTabGroup());

        // 9. BoxscoreTable — stats table
        sections.add(buildDemoBoxscoreTable());

        // 10. Form — interactive form with dropdowns
        sections.add(buildDemoForm());

        screen.set("sections", sections);
        return screen;
    }

    // ── Demo Section Builders ──────────────────────────────────────────

    /**
     * 1. ScoreboardHeader — Lakers vs Celtics, period 3, 89-94.
     */
    private ObjectNode buildDemoScoreboardHeader() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-scoreboard-header");
        section.put("type", "ScoreboardHeader");
        section.put("analyticsId", "demo_scoreboard_header");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400999");
        data.put("gameStatus", 2);
        data.put("gameStatusText", "Q3 4:32");
        data.put("period", 3);
        data.put("gameClock", "PT04M32.00S");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612738);
        home.put("teamTricode", "BOS");
        home.put("teamName", "Celtics");
        home.put("teamCity", "Boston");
        home.put("score", 94);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612747);
        away.put("teamTricode", "LAL");
        away.put("teamName", "Lakers");
        away.put("teamCity", "Los Angeles");
        away.put("score", 89);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612747/primary/L/logo.svg");
        data.set("awayTeam", away);

        section.set("data", data);
        return section;
    }

    /**
     * 2. StatLine — 3 mock players with PTS/REB/AST.
     */
    private ObjectNode buildDemoStatLine() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-stat-line");
        section.put("type", "StatLine");
        section.put("analyticsId", "demo_stat_line");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ArrayNode stats = objectMapper.createArrayNode();

        stats.add(createStatLine(1628369, "Jayson Tatum", "BOS", "PTS", "32"));
        stats.add(createStatLine(203507, "LeBron James", "LAL", "PTS", "28"));
        stats.add(createStatLine(203076, "Anthony Davis", "LAL", "REB", "14"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Top Performers");
        data.set("stats", stats);
        section.set("data", data);
        return section;
    }

    /**
     * 3. PromoBanner — "Welcome to SDUI" with gradient background.
     */
    private ObjectNode buildDemoPromoBanner() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-promo-banner");
        section.put("type", "PromoBanner");
        section.put("analyticsId", "demo_promo_banner");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Welcome to SDUI");
        data.put("description", "All 10 semantic section types rendered from a single server response.");
        data.put("imageUrl", "https://cdn.nba.com/promo/league-pass-banner.jpg");
        data.put("backgroundColor", "#17408B");
        data.put("textColor", "#FFFFFF");

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://scoreboard");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    /**
     * 4. ContentCard — single highlight card with thumbnail.
     */
    private ObjectNode buildDemoContentCard() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-content-card");
        section.put("type", "ContentCard");
        section.put("analyticsId", "demo_content_card");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("id", "card-highlight");
        data.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/top10-plays.jpg");
        data.put("headline", "Celtics Lead Series 3-1");
        data.put("subhead", "Boston takes commanding lead after Game 4 victory");
        data.put("contentType", "article");

        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://article/celtics-lead-series");
        action.put("fallbackUrl", "https://www.nba.com/news");
        data.set("action", action);

        section.set("data", data);
        return section;
    }

    /**
     * 5. ContentRail — 4 cards (Top Plays, Player Spotlight, etc.).
     */
    private ObjectNode buildDemoContentRail() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-content-rail");
        section.put("type", "ContentRail");
        section.put("analyticsId", "demo_content_rail");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Featured Content");

        ArrayNode cards = objectMapper.createArrayNode();

        String[][] cardData = {
            {"rail-1", "Top 10 Plays", "Last night's best moments", "video", "https://cdn.nba.com/manage/2024/04/top10-plays.jpg"},
            {"rail-2", "Player Spotlight", "Jayson Tatum's monster game", "article", "https://cdn.nba.com/manage/2024/04/player-spotlight.jpg"},
            {"rail-3", "Draft Preview", "Top prospects for 2025 draft", "article", "https://cdn.nba.com/manage/2024/04/draft-preview.jpg"},
            {"rail-4", "Playoff Bracket", "Updated bracket after today's results", "interactive", "https://cdn.nba.com/manage/2024/04/playoff-bracket.jpg"}
        };

        for (String[] cd : cardData) {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("id", cd[0]);
            card.put("headline", cd[1]);
            card.put("subhead", cd[2]);
            card.put("contentType", cd[3]);
            card.put("thumbnailUrl", cd[4]);

            ObjectNode action = objectMapper.createObjectNode();
            action.put("trigger", "onTap");
            action.put("type", "navigate");
            action.put("targetUri", "nba://content/" + cd[0]);
            card.set("action", action);

            cards.add(card);
        }

        data.set("cards", cards);
        section.set("data", data);
        return section;
    }

    /**
     * 6. GameCard — mock game tile for a single game.
     */
    private ObjectNode buildDemoGameCard() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-game-card");
        section.put("type", "GameCard");
        section.put("analyticsId", "demo_game_card");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("gameId", "0022400888");
        data.put("gameStatus", 3);
        data.put("gameStatusText", "Final");

        ObjectNode home = objectMapper.createObjectNode();
        home.put("teamId", 1610612744);
        home.put("teamTricode", "GSW");
        home.put("teamName", "Warriors");
        home.put("teamCity", "Golden State");
        home.put("score", 112);
        home.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612744/primary/L/logo.svg");
        data.set("homeTeam", home);

        ObjectNode away = objectMapper.createObjectNode();
        away.put("teamId", 1610612745);
        away.put("teamTricode", "HOU");
        away.put("teamName", "Rockets");
        away.put("teamCity", "Houston");
        away.put("score", 105);
        away.put("logoUrl", "https://cdn.nba.com/logos/nba/1610612745/primary/L/logo.svg");
        data.set("awayTeam", away);

        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onTap");
        action.put("type", "navigate");
        action.put("targetUri", "nba://game/0022400888");
        actions.add(action);
        data.set("actions", actions);

        section.set("data", data);
        return section;
    }

    /**
     * 7. Row — league leader row (label/value pair with children).
     */
    private ObjectNode buildDemoRow() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-row");
        section.put("type", "Row");
        section.put("analyticsId", "demo_row");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("spacing", 16);
        data.put("breakpoint", 600);

        ArrayNode children = objectMapper.createArrayNode();

        // Left child — scoring leader
        ObjectNode leftChild = objectMapper.createObjectNode();
        leftChild.put("id", "row-scoring-leader");
        leftChild.put("type", "StatLine");
        ObjectNode leftData = objectMapper.createObjectNode();
        leftData.put("title", "Scoring Leader");
        ArrayNode leftStats = objectMapper.createArrayNode();
        leftStats.add(createStatLine(203999, "Nikola Jokić", "DEN", "PTS", "26.4"));
        leftData.set("stats", leftStats);
        leftChild.set("data", leftData);
        children.add(leftChild);

        // Right child — assists leader
        ObjectNode rightChild = objectMapper.createObjectNode();
        rightChild.put("id", "row-assists-leader");
        rightChild.put("type", "StatLine");
        ObjectNode rightData = objectMapper.createObjectNode();
        rightData.put("title", "Assists Leader");
        ArrayNode rightStats = objectMapper.createArrayNode();
        rightStats.add(createStatLine(201566, "Trae Young", "ATL", "AST", "11.1"));
        rightData.set("stats", rightStats);
        rightChild.set("data", rightData);
        children.add(rightChild);

        data.set("children", children);
        section.set("data", data);
        return section;
    }

    /**
     * 8. TabGroup — two tabs ("Overview", "Stats") each with a ContentCard child.
     */
    private ObjectNode buildDemoTabGroup() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-tab-group");
        section.put("type", "TabGroup");
        section.put("analyticsId", "demo_tab_group");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "demo_active_tab");
        data.put("defaultTab", "overview");

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode overviewTab = objectMapper.createObjectNode();
        overviewTab.put("id", "tab-overview");
        overviewTab.put("label", "Overview");
        overviewTab.put("stateKey", "demo_active_tab");
        overviewTab.put("stateValue", "overview");
        tabs.add(overviewTab);

        ObjectNode statsTab = objectMapper.createObjectNode();
        statsTab.put("id", "tab-stats");
        statsTab.put("label", "Stats");
        statsTab.put("stateKey", "demo_active_tab");
        statsTab.put("stateValue", "stats");
        tabs.add(statsTab);

        data.set("tabs", tabs);

        // Tab contents
        ObjectNode tabContents = objectMapper.createObjectNode();

        // Overview tab content — a ContentCard
        ArrayNode overviewContent = objectMapper.createArrayNode();
        ObjectNode overviewCard = objectMapper.createObjectNode();
        overviewCard.put("id", "tab-overview-card");
        overviewCard.put("type", "ContentCard");
        ObjectNode overviewCardData = objectMapper.createObjectNode();
        overviewCardData.put("id", "overview-highlight");
        overviewCardData.put("headline", "Season Overview");
        overviewCardData.put("subhead", "The 2024-25 season has been full of surprises");
        overviewCardData.put("contentType", "article");
        overviewCardData.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/season-overview.jpg");
        overviewCard.set("data", overviewCardData);
        overviewContent.add(overviewCard);
        tabContents.set("overview", overviewContent);

        // Stats tab content — a ContentCard
        ArrayNode statsContent = objectMapper.createArrayNode();
        ObjectNode statsCard = objectMapper.createObjectNode();
        statsCard.put("id", "tab-stats-card");
        statsCard.put("type", "ContentCard");
        ObjectNode statsCardData = objectMapper.createObjectNode();
        statsCardData.put("id", "stats-summary");
        statsCardData.put("headline", "League Statistical Leaders");
        statsCardData.put("subhead", "Points, rebounds, assists and more");
        statsCardData.put("contentType", "interactive");
        statsCardData.put("thumbnailUrl", "https://cdn.nba.com/manage/2024/04/stats-leaders.jpg");
        statsCard.set("data", statsCardData);
        statsContent.add(statsCard);
        tabContents.set("stats", statsContent);

        data.set("tabContents", tabContents);
        section.set("data", data);
        return section;
    }

    /**
     * 9. BoxscoreTable — 3 players, 5 stat columns.
     */
    private ObjectNode buildDemoBoxscoreTable() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-boxscore-table");
        section.put("type", "BoxscoreTable");
        section.put("analyticsId", "demo_boxscore_table");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("teamTricode", "BOS");
        data.put("teamName", "Boston Celtics");
        data.put("teamColor", "#007A33");
        data.put("teamLogoUrl", "https://cdn.nba.com/logos/nba/1610612738/primary/L/logo.svg");

        ArrayNode columns = objectMapper.createArrayNode();
        columns.add(colDef("min", "MIN", true, false, null));
        columns.add(colDef("pts", "PTS", true, true, null));
        columns.add(colDef("reb", "REB", true, false, null));
        columns.add(colDef("ast", "AST", true, false, null));
        columns.add(colDef("fgPct", "FG%", true, false, null));
        data.set("columns", columns);

        ArrayNode players = objectMapper.createArrayNode();

        players.add(demoPlayerRow("1628369", "J. Tatum", "SF", "0", true,
                Map.of("min", "38:12", "pts", 32, "reb", 8, "ast", 5, "fgPct", ".545")));
        players.add(demoPlayerRow("1627759", "J. Brown", "SG", "7", true,
                Map.of("min", "36:45", "pts", 26, "reb", 5, "ast", 3, "fgPct", ".480")));
        players.add(demoPlayerRow("1629684", "D. White", "PG", "9", false,
                Map.of("min", "32:10", "pts", 18, "reb", 4, "ast", 6, "fgPct", ".500")));

        data.set("players", players);

        data.put("sortStateKey", "demo_boxscore_sortCol");
        data.put("sortDirectionStateKey", "demo_boxscore_sortDir");

        section.set("data", data);
        return section;
    }

    /**
     * Helper to construct a demo BoxscoreTable player row.
     */
    private ObjectNode demoPlayerRow(String playerId, String name, String position,
                                      String jerseyNumber, boolean starter,
                                      Map<String, Object> stats) {
        ObjectNode row = objectMapper.createObjectNode();
        row.put("playerId", playerId);
        row.put("name", name);
        row.put("position", position);
        row.put("jerseyNumber", jerseyNumber);
        row.put("imageUrl", "https://cdn.nba.com/headshots/nba/latest/1040x760/" + playerId + ".png");
        row.put("starter", starter);

        ObjectNode statsNode = objectMapper.createObjectNode();
        stats.forEach((key, value) -> {
            if (value instanceof Integer i) {
                statsNode.put(key, i);
            } else {
                statsNode.put(key, value.toString());
            }
        });
        row.set("stats", statsNode);
        return row;
    }

    /**
     * 10. Form — two dropdowns (Season, Season Type) with a submit action.
     */
    private ObjectNode buildDemoForm() {
        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", "demo-form");
        section.put("type", "Form");
        section.put("analyticsId", "demo_form");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));

        ObjectNode data = objectMapper.createObjectNode();
        data.put("title", "Stats Lookup");

        ArrayNode fields = objectMapper.createArrayNode();

        // Season dropdown
        ObjectNode seasonField = objectMapper.createObjectNode();
        seasonField.put("id", "season");
        seasonField.put("type", "dropdown");
        seasonField.put("label", "Season");
        seasonField.put("stateKey", "form_season");

        ArrayNode seasonOptions = objectMapper.createArrayNode();
        for (String s : new String[]{"2024-25", "2023-24", "2022-23"}) {
            ObjectNode opt = objectMapper.createObjectNode();
            opt.put("label", s);
            opt.put("value", s);
            seasonOptions.add(opt);
        }
        seasonField.set("options", seasonOptions);
        seasonField.put("defaultValue", "2024-25");
        fields.add(seasonField);

        // Season Type dropdown
        ObjectNode typeField = objectMapper.createObjectNode();
        typeField.put("id", "seasonType");
        typeField.put("type", "dropdown");
        typeField.put("label", "Season Type");
        typeField.put("stateKey", "form_season_type");

        ArrayNode typeOptions = objectMapper.createArrayNode();
        for (String[] st : new String[][]{
            {"Regular Season", "regular"},
            {"Playoffs", "playoffs"},
            {"All-Star", "allstar"}
        }) {
            ObjectNode opt = objectMapper.createObjectNode();
            opt.put("label", st[0]);
            opt.put("value", st[1]);
            typeOptions.add(opt);
        }
        typeField.set("options", typeOptions);
        typeField.put("defaultValue", "regular");
        fields.add(typeField);

        data.set("fields", fields);

        // Submit action
        ObjectNode submitAction = objectMapper.createObjectNode();
        submitAction.put("trigger", "onSubmit");
        submitAction.put("type", "refresh");
        submitAction.put("targetUri", "nba://refresh/stats-leaders");

        ObjectNode paramBindings = objectMapper.createObjectNode();
        paramBindings.put("season", "{{form_season}}");
        paramBindings.put("seasonType", "{{form_season_type}}");
        submitAction.set("paramBindings", paramBindings);

        ArrayNode actions = objectMapper.createArrayNode();
        actions.add(submitAction);
        data.set("actions", actions);

        section.set("data", data);
        return section;
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

        ObjectNode demos = objectMapper.createObjectNode();
        demos.put("id", "demos");
        demos.put("label", "Demos");
        demos.put("icon", "widgets");
        demos.put("targetUri", "nba://demos");
        demos.put("selected", "demos".equals(activeScreenId));
        items.add(demos);

        ObjectNode boxscore = objectMapper.createObjectNode();
        boxscore.put("id", "boxscore");
        boxscore.put("label", "Box Score");
        boxscore.put("icon", "table_chart");
        boxscore.put("targetUri", "nba://boxscore/0042300102");
        boxscore.put("selected", activeScreenId != null && activeScreenId.startsWith("boxscore"));
        items.add(boxscore);

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
