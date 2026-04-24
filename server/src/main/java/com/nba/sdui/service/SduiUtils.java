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
import java.util.Map;

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
        forYou.put("icon", "sdui:home");
        forYou.put("targetUri", "nba://for-you");
        forYou.put("selected", "for-you".equals(activeScreenId));
        items.add(forYou);

        ObjectNode games = objectMapper.createObjectNode();
        games.put("id", "games");
        games.put("label", "Games");
        games.put("icon", "sdui:basketball");
        games.put("targetUri", "nba://games");
        games.put("selected", gamesSelected);
        items.add(games);

        ObjectNode watch = objectMapper.createObjectNode();
        watch.put("id", "watch");
        watch.put("label", "Watch");
        watch.put("icon", "sdui:video");
        watch.put("targetUri", "nba://watch");
        watch.put("selected", "watch".equals(activeScreenId));
        items.add(watch);

        ObjectNode leaders = objectMapper.createObjectNode();
        leaders.put("id", "leaders");
        leaders.put("label", "Leaders");
        leaders.put("icon", "sdui:leaderboard");
        leaders.put("targetUri", "nba://leaders");
        leaders.put("selected", "leaders".equals(activeScreenId));
        items.add(leaders);

        ObjectNode demos = objectMapper.createObjectNode();
        demos.put("id", "demos");
        demos.put("label", "Kitchen");
        demos.put("icon", "sdui:grid");
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
     * Resolve a team primary colour by tricode.
     *
     * <p>NBA brand guideline: team primary colors are brand assets owned by
     * each team, not design-system tokens, so these hex values are
     * intentionally inline and are not represented in the color-token
     * registry at {@code schema/color-tokens.json}. Values sourced from the
     * NBA team style guides; if Rights &amp; Brand changes a primary, update
     * here — there is no registry lookup to keep in sync.
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

    /**
     * Build the linescore binding set for an {@code AtomicComposite}
     * GamePanel. Target paths point into {@code data.content.*} rather
     * than {@code data.*}: leaf Text/LiveClock elements resolve their
     * value from {@code data.content} via {@code bindRef}, so writes
     * from the Ably payload land in the same dictionary the renderers
     * consult. The {@code gameClock} mapping carries the whole
     * {@code {snapshotSeconds, snapshotAt, isRunning}} snapshot so a
     * single source field drives all three values a LiveClock leaf
     * reads.
     */
    public ObjectNode buildCompositeLinescoreBindings() {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();

        bindings.add(bindingPath("$.homeTeam.score", "content.homeTeam.score"));
        bindings.add(bindingPath("$.awayTeam.score", "content.awayTeam.score"));
        bindings.add(bindingPath("$.gameStatusText", "content.gameStatusText"));
        bindings.add(bindingPath("$.period", "content.period"));
        bindings.add(bindingPath("$.gameClock", "content.clock"));

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

    // ── Team logo URL ──────────────────────────────────────────────────

    /**
     * Build the CDN URL for a team primary logo.
     *
     * <p>Asset-format selection is server-driven: the server
     * decides which URL to emit based on what each client can consume.
     * Today every client consumes PNG (iOS {@code AsyncImage}, Android
     * {@code Coil}, and the web {@code <img>} element all decode PNG
     * natively), so we emit the 512×512 primary-dark PNG — matching the
     * reference-app shape. Keep all team-logo URL construction funneled
     * through this helper so a future per-platform branch (e.g. WebP on
     * Chromium, SVG on clients with an SVG decoder installed) is a
     * one-file change rather than a composer-wide sweep.
     *
     * <p>The legacy {@code /global/L/logo.svg} and {@code /primary/L/logo.svg}
     * URL shapes this helper replaces were silently failing on iOS
     * ({@code AsyncImage} has no SVG decoder) — producing infinite
     * placeholder spinners instead of logos.
     */
    public static String teamLogoUrl(String teamId) {
        return "https://cdn.nba.com/logos/nba/" + teamId + "/primary/D/512x512/logo.png";
    }

    /**
     * Convenience: overload that accepts a numeric team id.
     */
    public static String teamLogoUrl(long teamId) {
        return teamLogoUrl(String.valueOf(teamId));
    }

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

    // ── Section surface (server-driven wrapper around section content) ─

    /**
     * Build the default section-surface block applied by every permanent
     * section that does not override it. Clients' shared SectionContainer
     * reads this and applies platform-native equivalents. A single change
     * here retunes the entire app's rhythm (card inset, elevation, corner
     * radius) without a client release.
     *
     * <p>Default: 16px horizontal margin, 16px vertical margin, raised
     * surface background with a 12px corner radius and a soft 6px-radius
     * shadow at y=2. Matches the reference-app feed card treatment.
     *
     * <p>Vertical margin is 16 (not 8) so a card-chromed section is
     * separated from a flush-to-edge section (like a content rail)
     * by 32pt of air — rail contributes 16pt bottom, card contributes
     * 16pt top. 8pt on the card side read as "no spacing" next to a
     * rail because a flush rail has no visible bottom edge for the
     * eye to latch onto; 16pt makes the break read as an intentional
     * module boundary.
     */
    public ObjectNode defaultSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.set("margin", spacing(16, 16, 16, 16));
        surface.put("background", "token:color.surface.raised");
        surface.put("cornerRadius", 12);

        ObjectNode shadow = objectMapper.createObjectNode();
        shadow.put("color", "#00000014");
        shadow.put("radius", 6);
        shadow.put("offsetX", 0);
        shadow.put("offsetY", 2);
        surface.set("shadow", shadow);

        return surface;
    }

    /**
     * Build a flush (no wrapper chrome) surface block. Used for sections
     * that should render edge-to-edge (hero videos, full-bleed images).
     */
    public ObjectNode flushSurface() {
        return objectMapper.createObjectNode();
    }

    /**
     * Build a surface block for subscription upsell sections — standard
     * section rhythm + branded gradient background + inner padding so
     * the caller's content lays out flush against the surface edges.
     * Used by SubscribeBanner and SubscribeHero composers.
     */
    public ObjectNode subscribeSurface(String topColor, String bottomColor, int padding) {
        ObjectNode surface = defaultSurface();
        ObjectNode gradient = objectMapper.createObjectNode();
        ArrayNode colors = objectMapper.createArrayNode();
        colors.add(topColor);
        colors.add(bottomColor);
        gradient.set("colors", colors);
        gradient.put("direction", "vertical");
        surface.set("background", gradient);
        surface.set("padding", spacingSymmetric(padding, padding));
        return surface;
    }

    /**
     * Build a surface block for the VideoPlayer section — flush
     * edge-to-edge rectangle with a dark background behind the player
     * area. No margin (the player hugs its siblings) and no corner
     * radius (a rounded video frame is jarring against square content
     * below and makes the tap target feel like a card instead of an
     * embedded player). The player's content sizing (16:9 aspect) is
     * owned by the renderer and by `data.displayConfig`, not by this
     * surface block.
     */
    public ObjectNode videoPlayerSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.put("background", "#1A1F2E");
        return surface;
    }

    /**
     * Build a minimal surface block that only provides vertical margin
     * for breathing room between flush-to-the-edge atomic composite
     * sections (content rails, video carousels, section headers). Used
     * where the composite's root Container already owns its own inner
     * chrome (padding, title treatment) but consecutive rails would
     * otherwise touch each other vertically.
     *
     * <p>Outputs only {@code margin: {top, bottom}} — no background,
     * no corner radius, no shadow. That keeps the composite's internal
     * styling untouched and avoids double-chrome.
     */
    /**
     * Build a surface block for a card-chromed, vertically-stacked
     * composite section (e.g. {@code NbaTvSchedule}) — same 16pt vertical
     * rhythm as {@link #railSurface()}, plus 16pt horizontal margin, a
     * rounded sunken-surface background, and a 12pt corner radius. Use
     * this for composites that are NOT horizontal-scrolling: rails bleed
     * edge-to-edge on purpose (so off-screen cards peek in), but a
     * vertical-list composite should sit inside a card like {@code AdSlot}
     * does. The background/radius live on the surface (not on the root
     * container) so the chrome is discoverable at the section envelope
     * level and the inner atomic tree stays content-only.
     */
    public ObjectNode cardSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        surface.set("margin", spacing(16, 16, 16, 16));
        surface.put("background", "token:color.surface.sunken");
        surface.put("cornerRadius", 12);
        return surface;
    }

    public ObjectNode railSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        ObjectNode margin = objectMapper.createObjectNode();
        // 16pt top/bottom — pairs with the 16pt vertical margin on
        // `defaultSurface` (AdSlot) to give 32pt of air between a rail
        // and the card-chromed section that follows it. A flush rail
        // has no visible bottom edge, so the eye reads less spacing
        // than the pixel count suggests; 32pt reads as an intentional
        // module boundary rather than an orphan row.
        margin.put("top", 16);
        margin.put("bottom", 16);
        surface.set("margin", margin);
        return surface;
    }

    /**
     * Build a surface block for a SectionHeader that titles the rail or
     * section immediately below it. A SectionHeader is semantically part
     * of the module that follows — the header and its rail should read
     * as one unit.
     *
     * <p>The gap between a header and its content is owned here, so every
     * screen has the same header→content rhythm. If the rail below needs
     * more or less air from the header, adjust this single constant rather
     * than tweaking {@link #railSurface()} — that one is scoped to
     * rail→preceding-section spacing, which is a different rhythm.
     *
     * <p>Emits {@code margin.top = 16, margin.bottom = 8}:
     * <ul>
     *   <li>Top 16 — pairs with 16pt bottom on preceding card-chromed
     *       sections (AdSlot, GamePanel) to produce a 32pt module
     *       break before the header.</li>
     *   <li>Bottom 8 — combined with the following rail's 16pt top
     *       margin this produces 24pt between the header surface and
     *       the rail surface. Previously this was 0 (header→rail gap =
     *       16pt) but on device the header's title line sat too close
     *       to the top of the first rail card. 8pt here reads as "the
     *       title belongs to the rail" without looking flush.</li>
     * </ul>
     */
    public ObjectNode sectionHeaderSurface() {
        ObjectNode surface = objectMapper.createObjectNode();
        ObjectNode margin = objectMapper.createObjectNode();
        margin.put("top", 16);
        margin.put("bottom", 8);
        surface.set("margin", margin);
        return surface;
    }

    /**
     * Build a surface block for GamePanel cards — standard card
     * chrome (horizontal inset + subtle shadow + rounded corners)
     * with a soft linear gradient background that gives the matchup
     * its own visual weight versus surrounding flat content. Used by
     * every GamePanel composer site (For You hero, live rails,
     * scoreboard rows, Game Detail scoreboard strip).
     *
     * <p>The gradient is intentionally subtle — a near-white top to
     * a pale tint bottom — so scores and team identity remain the
     * dominant information, and does not compete with SubscribeHero /
     * SubscribeBanner's strong brand gradient.
     */
    public ObjectNode gamePanelSurface() {
        ObjectNode surface = defaultSurface();

        ObjectNode gradient = objectMapper.createObjectNode();
        ArrayNode colors = objectMapper.createArrayNode();
        colors.add("#F5F7FA");
        colors.add("#DDE4EE");
        gradient.set("colors", colors);
        gradient.put("direction", "diagonal");
        surface.set("background", gradient);

        return surface;
    }

    /**
     * Build a Spacing node with the four common edges.
     * Any argument may be zero to omit that edge.
     */
    public ObjectNode spacing(int top, int end, int bottom, int start) {
        ObjectNode s = objectMapper.createObjectNode();
        if (top    != 0) s.put("top",    top);
        if (end    != 0) s.put("end",    end);
        if (bottom != 0) s.put("bottom", bottom);
        if (start  != 0) s.put("start",  start);
        return s;
    }

    /**
     * Convenience: symmetric spacing (same vertical, same horizontal).
     */
    public ObjectNode spacingSymmetric(int vertical, int horizontal) {
        return spacing(vertical, horizontal, vertical, horizontal);
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

    // ── String table (i18n) ────────────────────────────────────────────

    /**
     * Locale-keyed string tables for data-binding string key resolution.
     * In production this would be backed by a resource bundle or translation service.
     */
    private static final Map<String, Map<String, String>> STRING_TABLES = Map.of(
            "en", Map.ofEntries(
                    Map.entry("status.pre", "Upcoming"),
                    Map.entry("status.live", "Live"),
                    Map.entry("status.final", "Final"),
                    Map.entry("status.halftime", "Halftime"),
                    Map.entry("period.q1", "Q1"),
                    Map.entry("period.q2", "Q2"),
                    Map.entry("period.q3", "Q3"),
                    Map.entry("period.q4", "Q4"),
                    Map.entry("period.ot", "OT")
            ),
            "es", Map.ofEntries(
                    Map.entry("status.pre", "Próximo"),
                    Map.entry("status.live", "En Vivo"),
                    Map.entry("status.final", "Final"),
                    Map.entry("status.halftime", "Medio Tiempo"),
                    Map.entry("period.q1", "Q1"),
                    Map.entry("period.q2", "Q2"),
                    Map.entry("period.q3", "Q3"),
                    Map.entry("period.q4", "Q4"),
                    Map.entry("period.ot", "TE")
            ),
            "fr", Map.ofEntries(
                    Map.entry("status.pre", "À venir"),
                    Map.entry("status.live", "En Direct"),
                    Map.entry("status.final", "Terminé"),
                    Map.entry("status.halftime", "Mi-temps"),
                    Map.entry("period.q1", "Q1"),
                    Map.entry("period.q2", "Q2"),
                    Map.entry("period.q3", "Q3"),
                    Map.entry("period.q4", "Q4"),
                    Map.entry("period.ot", "Prol.")
            )
    );

    /**
     * Build a string table node for the given locale.
     * Falls back to English when the locale is unknown.
     */
    public ObjectNode buildStringTable(String locale) {
        Map<String, String> table = STRING_TABLES.getOrDefault(
                locale != null ? locale.toLowerCase() : "en",
                STRING_TABLES.get("en")
        );
        ObjectNode node = objectMapper.createObjectNode();
        table.forEach(node::put);
        return node;
    }

    /**
     * Stamp a stringTable onto every section in the response's sections array.
     * Sections that already have a stringTable are left unchanged.
     */
    public void stampStringTableOnSections(ObjectNode response, String locale) {
        JsonNode sections = response.get("sections");
        if (sections == null || !sections.isArray()) return;
        ObjectNode table = buildStringTable(locale);
        for (JsonNode section : sections) {
            if (section.isObject() && !section.has("stringTable")) {
                ((ObjectNode) section).set("stringTable", table.deepCopy());
            }
        }
    }
}
