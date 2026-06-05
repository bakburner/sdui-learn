package com.nba.sdui.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionSurface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.nba.sdui.domain.tokens.Tokens;

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
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    public SduiUtils(ObjectMapper objectMapper, Tokens tokens) {
        this.objectMapper = objectMapper;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
    }

    // ── Navigation ─────────────────────────────────────────────────────

    public ObjectNode buildNavigation(String activeScreenId) {
        ObjectNode navigation = objectMapper.createObjectNode();
        ArrayNode items = objectMapper.createArrayNode();

        boolean gamesSelected = "games".equals(activeScreenId) || "game-detail".equals(activeScreenId);

        ObjectNode forYou = objectMapper.createObjectNode();
        forYou.put("id", "for-you");
        forYou.put("label", "For You");
        forYou.put("icon", tokens.icon("home"));
        forYou.put("targetUri", "nba://for-you");
        forYou.put("selected", "for-you".equals(activeScreenId));
        items.add(forYou);

        ObjectNode games = objectMapper.createObjectNode();
        games.put("id", "games");
        games.put("label", "Games");
        games.put("icon", tokens.icon("basketball"));
        games.put("targetUri", "nba://games");
        games.put("selected", gamesSelected);
        items.add(games);

        ObjectNode watch = objectMapper.createObjectNode();
        watch.put("id", "watch");
        watch.put("label", "Watch");
        watch.put("icon", tokens.icon("video"));
        watch.put("targetUri", "nba://watch");
        watch.put("selected", "watch".equals(activeScreenId));
        items.add(watch);

        ObjectNode leaders = objectMapper.createObjectNode();
        leaders.put("id", "leaders");
        leaders.put("label", "Leaders");
        leaders.put("icon", tokens.icon("leaderboard"));
        leaders.put("targetUri", "nba://leaders");
        leaders.put("selected", "leaders".equals(activeScreenId));
        items.add(leaders);

        ObjectNode demos = objectMapper.createObjectNode();
        demos.put("id", "demos");
        demos.put("label", "Kitchen");
        demos.put("icon", tokens.icon("grid"));
        demos.put("targetUri", "nba://demos");
        demos.put("selected", "demos".equals(activeScreenId));
        items.add(demos);

        ObjectNode home = objectMapper.createObjectNode();
        home.put("id", "home");
        home.put("label", "NBA.com");
        home.put("icon", tokens.icon("basketball"));
        home.put("targetUri", "nba://home");
        home.put("selected", "home".equals(activeScreenId));
        items.add(home);

        navigation.set("items", items);
        return navigation;
    }

    /**
     * Bottom-nav tab destinations: wire navigation and strip legacy {@code title}.
     * Header chrome is omitted — the selected tab label is sufficient.
     */
    public void applyTabDestinationNavigation(ObjectNode response, String activeScreenId) {
        response.remove("title");
        response.set("navigation", buildNavigation(activeScreenId));
    }

    /**
     * Default scroll-feed padding when the composer did not set {@code contentInsets}.
     * Matches former client hardcodes: horizontal {@code spacing("md")},
     * bottom {@code spacing("lg")}.
     *
     * <p>Call this explicitly at the end of every composer that emits a scrollable
     * feed. Tab-destination composers (bottom-nav screens) in particular need this
     * so the last card does not sit flush against the bottom navigation.
     */
    public void ensureScreenContentInsets(ObjectNode response) {
        if (response.has("contentInsets")) {
            return;
        }
        ObjectNode insets = objectMapper.createObjectNode();
        insets.put("start", tokens.spacing("md"));
        insets.put("end", tokens.spacing("md"));
        insets.put("bottom", tokens.spacing("lg"));
        response.set("contentInsets", insets);
    }

    /**
     * When the screen carries {@code title} and/or {@code parentUri}, prepend an
     * {@code AtomicComposite} app-bar section (token spacing, back navigate action)
     * and remove top-level {@code title} so clients do not render a platform app bar.
     */
    public void prependAppBarHeaderIfNeeded(ObjectNode response) {
        String screenId = response.path("id").asText("screen");
        String title = response.has("title") ? response.path("title").asText(null) : null;
        String backUri = response.has("parentUri") ? response.path("parentUri").asText(null) : null;
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasBack = backUri != null && !backUri.isBlank();
        if (!hasTitle && !hasBack) {
            response.remove("title");
            return;
        }

        String sectionId = screenId + ":app-bar";
        Section header = atomicBuilder.buildAppBarHeaderComposite(
                sectionId, screenId + "_app_bar", title, backUri);
        header.setSurface(new SectionSurface());

        ArrayNode sections = response.has("sections") && response.get("sections").isArray()
                ? (ArrayNode) response.get("sections")
                : objectMapper.createArrayNode();
        ArrayNode merged = objectMapper.createArrayNode();
        merged.add(objectMapper.valueToTree(header));
        sections.forEach(merged::add);
        response.set("sections", merged);
        response.remove("title");
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
     * <p>Values sourced from the Kinetic Design System export
     * ({@code docs/Kinetic Design System Tokens/…NBA Team Colors.csv}).
     * The design team maintains this palette in Figma; if a team rebrands,
     * update here to match the latest Kinetic export.
     */
    public static String getTeamPrimaryColor(String tricode) {
        return switch (tricode) {
            case "ATL" -> "#C8102E";
            case "BOS" -> "#008348";
            case "BKN" -> "#000000";
            case "CHA" -> "#00788C";
            case "CHI" -> "#CE1141";
            case "CLE" -> "#860038";
            case "DAL" -> "#0064B1";
            case "DEN" -> "#0E2240";
            case "DET" -> "#1D428A";
            case "GSW" -> "#1D428A";
            case "HOU" -> "#CE1141";
            case "IND" -> "#0C2340";
            case "LAC" -> "#12173F";
            case "LAL" -> "#31006F";
            case "MEM" -> "#5D76A9";
            case "MIA" -> "#98002E";
            case "MIL" -> "#00471B";
            case "MIN" -> "#0C2340";
            case "NOP" -> "#0A2240";
            case "NYK" -> "#1D428A";
            case "OKC" -> "#007AC1";
            case "ORL" -> "#0050B5";
            case "PHI" -> "#1D428A";
            case "PHX" -> "#1D1160";
            case "POR" -> "#E03A3E";
            case "SAC" -> "#5A2D81";
            case "SAS" -> "#000000";
            case "TOR" -> "#CE1141";
            case "UTA" -> "#4E008E";
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
     * consult.
     *
     * <p>Source paths must match the upstream linescore publisher's
     * field names. The publisher emits {@code clock} (ISO-8601 duration
     * string, e.g. {@code "PT00M00.70S"}) and {@code clockRunning}
     * (boolean or string {@code "1"}/{@code "0"}) at the message root —
     * matching the production NBA Android {@code AblyGame} model. The
     * {@code liveClockSnapshot} transform reads the duration via the
     * source path, falls back to the root for {@code clockRunning}, and
     * writes the normalized {@code {snapshotSeconds, snapshotAt,
     * isRunning}} tuple to {@code content.clock}.
     */
    public ObjectNode buildCompositeLinescoreBindings() {
        ObjectNode dataBinding = objectMapper.createObjectNode();
        ArrayNode bindings = objectMapper.createArrayNode();

        bindings.add(bindingPath("$.homeTeam.score", "content.homeTeam.score"));
        bindings.add(bindingPath("$.awayTeam.score", "content.awayTeam.score"));
        bindings.add(bindingPath("$.gameStatusText", "content.gameStatusText"));
        bindings.add(bindingPath("$.period", "content.period"));
        bindings.add(bindingPath("$.clock", "content.clock", "liveClockSnapshot"));

        dataBinding.set("bindings", bindings);
        return dataBinding;
    }

    public ObjectNode bindingPath(String sourcePath, String targetPath) {
        ObjectNode path = objectMapper.createObjectNode();
        path.put("sourcePath", sourcePath);
        path.put("targetPath", targetPath);
        return path;
    }

    public ObjectNode bindingPath(String sourcePath, String targetPath, String transform) {
        AtomicCompositeBuilder.validateTransform(transform);
        ObjectNode path = bindingPath(sourcePath, targetPath);
        if (transform != null) {
            path.put("transform", transform);
        }
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


    public ObjectNode loadExampleByFilename(String filename) throws IOException {
        JsonNode loaded = loadExampleJsonFile(filename);
        return loaded instanceof ObjectNode objectNode ? objectNode : null;
    }

    private JsonNode loadExampleJsonFile(String filename) throws IOException {
        JsonNode loaded = null;
        try {
            ClassPathResource resource = new ClassPathResource("examples/" + filename);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    loaded = objectMapper.readTree(is);
                }
            }
        } catch (Exception e) {
            log.debug("Could not load from classpath, trying file system");
        }

        if (loaded == null) {
            Path filePath = Path.of("../schema/examples/" + filename);
            if (Files.exists(filePath)) {
                loaded = objectMapper.readTree(Files.readString(filePath));
            }
        }

        if (loaded == null) {
            Path filePath = Path.of("schema/examples/" + filename);
            if (Files.exists(filePath)) {
                loaded = objectMapper.readTree(Files.readString(filePath));
            }
        }

        if (loaded == null) {
            log.error("Could not load example file: {}", filename);
            return null;
        }
        return normalizeLegacyNavigateFields(loaded);
    }

    /**
     * Example fixtures may still carry pre-schema {@code fallbackUrl} on navigate
     * actions; clients decode strictly on {@code webUrl}.
     */
    private JsonNode normalizeLegacyNavigateFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            if (obj.has("fallbackUrl")) {
                if (!obj.has("webUrl")) {
                    obj.set("webUrl", obj.get("fallbackUrl"));
                }
                obj.remove("fallbackUrl");
            }
            obj.properties().forEach(entry -> normalizeLegacyNavigateFields(entry.getValue()));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                normalizeLegacyNavigateFields(child);
            }
        }
        return node;
    }

    /**
     * One {@code section.subsections} entry per tab, each carrying an
     * {@code onActivate → mutate} action for tab selection (core action semantic).
     */
    public ArrayNode tabSelectSubsections(ArrayNode tabs, String stateKey) {
        ArrayNode subsections = objectMapper.createArrayNode();
        for (JsonNode tab : tabs) {
            String tabId = tab.path("id").asText();
            String stateValue = tab.path("stateValue").asText(tabId);
            ObjectNode sub = objectMapper.createObjectNode();
            sub.put("id", tabId);
            ArrayNode actions = objectMapper.createArrayNode();
            ObjectNode mutate = objectMapper.createObjectNode();
            mutate.put("trigger", "onActivate");
            mutate.put("type", "mutate");
            mutate.put("target", stateKey);
            mutate.put("value", stateValue);
            actions.add(mutate);
            sub.set("actions", actions);
            subsections.add(sub);
        }
        return subsections;
    }

    /**
     * Build a Spacing node whose edges are layout-token wire strings.
     */
    public ObjectNode spacingTokens(String top, String end, String bottom, String start) {
        ObjectNode s = objectMapper.createObjectNode();
        if (top != null && !top.isBlank()) s.put("top", top);
        if (end != null && !end.isBlank()) s.put("end", end);
        if (bottom != null && !bottom.isBlank()) s.put("bottom", bottom);
        if (start != null && !start.isBlank()) s.put("start", start);
        return s;
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
        return (ObjectNode) objectMapper.valueToTree(
                atomicBuilder.buildErrorState(sectionId, title, message, icon, retryUri));
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
        retryAction.put("trigger", "onActivate");
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
                    Map.entry("period.ot", "OT"),
                    Map.entry("screen.schedule", "Schedule"),
                    Map.entry("filter.season", "Season"),
                    Map.entry("filter.year", "Year"),
                    Map.entry("filter.month", "Month"),
                    Map.entry("filter.day", "Day"),
                    Map.entry("filter.all", "All"),
                    Map.entry("filter.apply", "Apply"),
                    Map.entry("month.january", "January"),
                    Map.entry("month.february", "February"),
                    Map.entry("month.march", "March"),
                    Map.entry("month.april", "April"),
                    Map.entry("month.october", "October"),
                    Map.entry("month.november", "November"),
                    Map.entry("month.december", "December")
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
                    Map.entry("period.ot", "TE"),
                    Map.entry("screen.schedule", "Calendario"),
                    Map.entry("filter.season", "Temporada"),
                    Map.entry("filter.year", "Año"),
                    Map.entry("filter.month", "Mes"),
                    Map.entry("filter.day", "Día"),
                    Map.entry("filter.all", "Todos"),
                    Map.entry("filter.apply", "Aplicar"),
                    Map.entry("month.january", "Enero"),
                    Map.entry("month.february", "Febrero"),
                    Map.entry("month.march", "Marzo"),
                    Map.entry("month.april", "Abril"),
                    Map.entry("month.october", "Octubre"),
                    Map.entry("month.november", "Noviembre"),
                    Map.entry("month.december", "Diciembre")
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
                    Map.entry("period.ot", "Prol."),
                    Map.entry("screen.schedule", "Calendrier"),
                    Map.entry("filter.season", "Saison"),
                    Map.entry("filter.year", "Année"),
                    Map.entry("filter.month", "Mois"),
                    Map.entry("filter.day", "Jour"),
                    Map.entry("filter.all", "Tous"),
                    Map.entry("filter.apply", "Appliquer"),
                    Map.entry("month.january", "Janvier"),
                    Map.entry("month.february", "Février"),
                    Map.entry("month.march", "Mars"),
                    Map.entry("month.april", "Avril"),
                    Map.entry("month.october", "Octobre"),
                    Map.entry("month.november", "Novembre"),
                    Map.entry("month.december", "Décembre")
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
     * Look up a single localized string by key.
     * Falls back to English, then to the raw key if not found.
     */
    public static String getLocalizedString(String locale, String key) {
        Map<String, String> table = STRING_TABLES.getOrDefault(
                locale != null ? locale.toLowerCase() : "en",
                STRING_TABLES.get("en")
        );
        return table.getOrDefault(key, key);
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
