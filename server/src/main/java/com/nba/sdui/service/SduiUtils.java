package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared SDUI utilities used by all composer classes.
 *
 * <p>Contains navigation building, column definitions, formatting helpers,
 * team colour lookup, linescore bindings, stat-line factory, and example
 * file loading.
 */
@Component
public class SduiUtils {

    private static final Logger log = LoggerFactory.getLogger(SduiUtils.class);

    private final ObjectMapper objectMapper;
    private final AtomicCompositeBuilder atomicBuilder;

    public SduiUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    // ── Navigation ─────────────────────────────────────────────────────

    public ObjectNode buildNavigation(String activeScreenId) {
        ObjectNode navigation = objectMapper.createObjectNode();
        ArrayNode items = objectMapper.createArrayNode();

        boolean gamesSelected = "games".equals(activeScreenId) || "game-detail".equals(activeScreenId);

        ObjectNode forYou = objectMapper.createObjectNode();
        forYou.put("id", "for-you");
        forYou.put("label", "For You");
        forYou.put("icon", "home");
        forYou.put("targetUri", "nba://for-you");
        forYou.put("selected", "for-you".equals(activeScreenId));
        items.add(forYou);

        ObjectNode games = objectMapper.createObjectNode();
        games.put("id", "games");
        games.put("label", "Games");
        games.put("icon", "sports_basketball");
        games.put("targetUri", "nba://games");
        games.put("selected", gamesSelected);
        items.add(games);

        ObjectNode watch = objectMapper.createObjectNode();
        watch.put("id", "watch");
        watch.put("label", "Watch");
        watch.put("icon", "play_circle");
        watch.put("targetUri", "nba://watch");
        watch.put("selected", "watch".equals(activeScreenId));
        items.add(watch);

        ObjectNode leaders = objectMapper.createObjectNode();
        leaders.put("id", "leaders");
        leaders.put("label", "Leaders");
        leaders.put("icon", "leaderboard");
        leaders.put("targetUri", "nba://leaders");
        leaders.put("selected", "leaders".equals(activeScreenId));
        items.add(leaders);

        ObjectNode demos = objectMapper.createObjectNode();
        demos.put("id", "demos");
        demos.put("label", "Kitchen");
        demos.put("icon", "widgets");
        demos.put("targetUri", "nba://demos");
        demos.put("selected", "demos".equals(activeScreenId));
        items.add(demos);

        navigation.set("items", items);
        return navigation;
    }

    // ── Boxscore column definitions ────────────────────────────────────

    /**
     * Standard boxscore column definitions.
     * Clients render left-to-right; the first (player) column is frozen.
     */
    public ArrayNode buildBoxscoreColumns() {
        ArrayNode columns = objectMapper.createArrayNode();
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

    public ObjectNode colDef(String key, String label, boolean sortable,
                              boolean highlighted, String width) {
        ObjectNode col = objectMapper.createObjectNode();
        col.put("key", key);
        col.put("label", label);
        col.put("sortable", sortable);
        col.put("highlighted", highlighted);
        if (width != null) col.put("width", width);
        return col;
    }

    // ── Formatting helpers ─────────────────────────────────────────────

    /**
     * Format a decimal like 0.565 → ".565" (NBA percentage style).
     */
    public static String formatPct(double pct) {
        if (pct == 0.0) return ".000";
        return String.format("%.3f", pct).substring(1); // strip leading zero
    }

    /**
     * Format ISO 8601 duration (PT35M20.00S) to MM:SS format.
     */
    public static String formatMinutes(String isoMinutes) {
        if (isoMinutes == null || isoMinutes.isEmpty()) {
            return "0:00";
        }

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

    // ── Team colours ───────────────────────────────────────────────────

    /**
     * Resolve a rough team primary colour by tricode. Clients may override.
     */
    public static String getTeamPrimaryColor(String tricode) {
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

    // ── Linescore bindings ─────────────────────────────────────────────

    public ObjectNode buildLinescoreBindings() {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();

        bindings.add(bindingPath("$.homeTeam.score", "homeTeam.score"));
        bindings.add(bindingPath("$.awayTeam.score", "awayTeam.score"));
        bindings.add(bindingPath("$.gameStatusText", "gameStatusText"));
        bindings.add(bindingPath("$.period", "period"));

        dataBinding.set("bindings", bindings);
        return dataBinding;
    }

    public ObjectNode bindingPath(String sourcePath, String targetPath) {
        ObjectNode path = objectMapper.createObjectNode();
        path.put("sourcePath", sourcePath);
        path.put("targetPath", targetPath);
        return path;
    }

    // ── Stat-line factory ──────────────────────────────────────────────

    public ObjectNode createStatLine(int playerId, String name, String team,
                                      String category, String value) {
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

    // ── Example file loading ───────────────────────────────────────────

    public JsonNode loadExampleResponse(String gameState) throws IOException {
        String filename = switch (gameState.toLowerCase()) {
            case "live" -> "game-detail-live.json";
            case "final" -> "game-detail-final.json";
            default -> "game-detail-pre.json";
        };

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

        Path filePath = Path.of("../schema/examples/" + filename);
        if (Files.exists(filePath)) {
            return objectMapper.readTree(Files.readString(filePath));
        }

        filePath = Path.of("schema/examples/" + filename);
        if (Files.exists(filePath)) {
            return objectMapper.readTree(Files.readString(filePath));
        }

        log.error("Could not load example file: {}", filename);
        return null;
    }

    public ObjectNode loadExampleByFilename(String filename) throws IOException {
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

    // ── Error State ────────────────────────────────────────────────────

    /**
     * Build an ErrorState section that clients can render inline.
     *
     * @param sectionId  Unique section id (e.g. "error-invalid-game")
     * @param title      Short headline (e.g. "Game not found")
     * @param message    Longer explanatory text
     * @param icon       Optional icon name (e.g. "error", "wifi_off")
     * @param retryUri   Optional nba:// URI for the retry action
     */
    public ObjectNode buildErrorSection(String sectionId, String title, String message,
                                         String icon, String retryUri) {
        return atomicBuilder.buildErrorState(sectionId, title, message, icon, retryUri);
    }

    // ── Section states (error/loading UX) ────────────────────────────

    /**
     * Build a sectionStates node for sections with live data (SSE/poll).
     * Provides server-controlled loading skeleton and error message/retry.
     */
    public ObjectNode buildSectionStates(String sectionId, String errorMessage,
                                          String skeletonType, int minHeightDp) {
        ObjectNode states = objectMapper.createObjectNode();

        ObjectNode loading = objectMapper.createObjectNode();
        loading.put("skeleton", skeletonType);
        loading.put("minHeightDp", minHeightDp);
        states.set("loading", loading);

        ObjectNode error = objectMapper.createObjectNode();
        error.put("message", errorMessage);
        error.put("hideOnError", false);

        ObjectNode retryAction = objectMapper.createObjectNode();
        retryAction.put("trigger", "onTap");
        retryAction.put("type", "refresh");
        retryAction.put("target", sectionId);
        error.set("retryAction", retryAction);

        states.set("error", error);
        return states;
    }
}
