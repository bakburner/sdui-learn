package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.nba.sdui.domain.AccessibilityHelper;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.error.UnsupportedSectionException;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.domain.port.StatsPort;
import com.nba.sdui.integration.model.boxscore.BoxscoreGame;
import com.nba.sdui.integration.model.boxscore.BoxscorePlayer;
import com.nba.sdui.integration.model.boxscore.BoxscoreResponse;
import com.nba.sdui.integration.model.boxscore.BoxscoreStatistics;
import com.nba.sdui.integration.model.boxscore.BoxscoreTeam;

/**
 * Composes the Game Detail SDUI screen from live NBA API data with
 * example-file fallback, and applies server-driven variant transformations
 * (B, C, D).
 *
 * <p>Game state (pre/live/post) is derived from the boxscore API response,
 * not from client input.
 */

@Component
public class GameDetailComposer {

    private static final Logger log = LoggerFactory.getLogger(GameDetailComposer.class);
    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final StatsPort statsPort;
    private final BoxscoreComposer boxscoreComposer;
    private final SectionRefreshService sectionRefreshService;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public GameDetailComposer(ObjectMapper objectMapper,
                              StatsPort statsPort,
                              BoxscoreComposer boxscoreComposer,
                              SectionRefreshService sectionRefreshService,
                              SduiUtils utils,
                              SectionSurfaces surfaces,
                              Tokens tokens) {
        this.objectMapper = objectMapper;
        this.statsPort = statsPort;
        this.boxscoreComposer = boxscoreComposer;
        this.sectionRefreshService = sectionRefreshService;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
    }

    @PostConstruct
    void registerSectionRefreshResolvers() {
        sectionRefreshService.registerResolver("stats-api:game-", this::refreshGameDetailSection);
    }

    /**
     * Result of composing a Game Detail screen — carries both the JSON
     * response and the server-derived game state for cache-control decisions.
     */
    public record GameDetailResult(JsonNode response, String derivedGameState) {}

    // ── Public entry point ─────────────────────────────────────────────

    /**
     * Compose a Game Detail SDUI screen response.
     *
     * <p>Game state is derived from the boxscore API ({@code game.gameStatus}):
     * 1 → pre, 2 → live, 3 → post. The caller uses the derived state for
     * cache-control headers.
     */
    public GameDetailResult composeGameDetail(String gameId,
                                              String variant, String clientSchemaVersion,
                                              String traceId, String locale) throws IOException {
        log.info("Composing game detail: gameId={}, variant={}, locale={}", gameId, variant, locale);

        String derivedGameState = "pre";
        JsonNode baseResponse = composeFromLiveData(gameId);
        if (baseResponse != null) {
            derivedGameState = deriveGameState(gameId);
        } else {
            log.warn("Live game detail unavailable for gameId={} — composing ErrorState screen", gameId);
            return new GameDetailResult(
                    buildErrorScreen(gameId, traceId, locale),
                    derivedGameState);
        }

        ObjectNode response = baseResponse.deepCopy();

        String contentSourceId = "stats-api:game-" + gameId;
        rederiveSectionIds(response, contentSourceId);

        response.put("id", "game-detail-" + gameId);
        // Screen-level default refresh is static; sections own pre-game polling and live SSE.
        ObjectNode defaultRefreshPolicy = objectMapper.createObjectNode();
        defaultRefreshPolicy.put("type", "static");
        response.set("defaultRefreshPolicy", defaultRefreshPolicy);
        response.put("schemaVersion", schemaVersion);
        response.put("parentUri", "nba://scoreboard");
        response.set("navigation", utils.buildNavigation("game-detail"));

        // Expose available A/B variants so clients never need URI-sniffing.
        ArrayNode variants = objectMapper.createArrayNode();
        variants.add(objectMapper.createObjectNode().put("id", "A").put("label", "Default").put("description", "All sections, standard order"));
        variants.add(objectMapper.createObjectNode().put("id", "B").put("label", "Reorder").put("description", "Content rail and TabGroup swapped"));
        variants.add(objectMapper.createObjectNode().put("id", "C").put("label", "Minimal").put("description", "Video player, scoreboard, and tabs only"));
        variants.add(objectMapper.createObjectNode().put("id", "D").put("label", "Extra Rail").put("description", "Trending videos rail added after content rail"));
        ObjectNode variantsWrapper = objectMapper.createObjectNode();
        variantsWrapper.put("experimentId", "game_detail_variant");
        variantsWrapper.set("options", variants);
        response.set("variants", variantsWrapper);

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

        applyGameSectionRefreshPolicies(response, derivedGameState, gameId);

        utils.prependAppBarHeaderIfNeeded(response);
        prependVariantChipsIfPresent(response, gameId, variant);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return new GameDetailResult(response, derivedGameState);
    }

    /**
     * Prepend a server-composed variant chip section to the screen when the
     * response declares {@code variants}. Replaces the per-client variant picker
     * UIs: every platform just renders the chip buttons via existing atomics and
     * dispatches the {@code navigate} action whose targetUri carries the chosen
     * variant in the {@code experiments[<experimentId>]} query param.
     */
    private void prependVariantChipsIfPresent(ObjectNode response, String gameId, String activeVariant) {
        if (!response.has("variants")) return;
        ObjectNode variants = (ObjectNode) response.get("variants");
        String experimentId = variants.path("experimentId").asText(null);
        ArrayNode options = variants.has("options") && variants.get("options").isArray()
                ? (ArrayNode) variants.get("options") : null;
        if (experimentId == null || experimentId.isBlank() || options == null || options.isEmpty()) return;

        String currentUri = "nba://game/" + gameId;
        String normalized = (activeVariant == null || activeVariant.isBlank())
                ? "A" : activeVariant.toUpperCase();
        ObjectNode chips = atomicBuilder.buildVariantChipsComposite(
                "game-detail-variant-chips", "game_detail_variant_chips",
                currentUri, experimentId, options, normalized);
        chips.set("surface", surfaces.flushSurface());

        ArrayNode sections = response.has("sections") && response.get("sections").isArray()
                ? (ArrayNode) response.get("sections")
                : objectMapper.createArrayNode();
        ArrayNode merged = objectMapper.createArrayNode();
        // Insert after the app-bar (index 0) so the chips sit just below the
        // header. If no app-bar is present, the chips go at the very top.
        int insertAt = 0;
        if (sections.size() > 0) {
            String firstId = sections.get(0).path("id").asText("");
            if (firstId.endsWith(":app-bar")) {
                merged.add(sections.get(0));
                insertAt = 1;
            }
        }
        merged.add(chips);
        for (int i = insertAt; i < sections.size(); i++) {
            merged.add(sections.get(i));
        }
        response.set("sections", merged);
    }

    // ── Live-data composition ──────────────────────────────────────────

    /**
     * Derive the game state string from the boxscore API for the given game.
     * NBA API gameStatus: 1 = pre, 2 = live, 3 = post/final.
     */
    private String deriveGameState(String gameId) {
        try {
            BoxscoreResponse boxscore = statsPort.getBoxscore(gameId);
            if (boxscore == null || boxscore.getGame() == null) return "pre";
            int status = boxscore.getGame().getGameStatus();
            return switch (status) {
                case 2 -> "live";
                case 3 -> "post";
                default -> "pre";
            };
        } catch (Exception e) {
            log.warn("Failed to derive game state for gameId={}, defaulting to 'pre'", gameId, e);
            return "pre";
        }
    }

    private JsonNode composeFromLiveData(String gameId) {
        try {
            BoxscoreResponse boxscore = statsPort.getBoxscore(gameId);
            if (boxscore == null) {
                log.warn("No boxscore data available for gameId={}", gameId);
                return null;
            }

            log.info("Successfully fetched live boxscore for gameId={}", gameId);
            BoxscoreGame game = boxscore.getGame();
            if (game == null) {
                log.warn("No game data in boxscore for gameId={}", gameId);
                return null;
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("id", "game-detail-" + gameId);
            response.put("schemaVersion", schemaVersion);
            response.put("parentUri", "nba://scoreboard");

            // Content source for all sections derived from this game
            String contentSourceId = "stats-api:game-" + gameId;

            // Default screen state — boxscore team tabs default to away (first listed)
            BoxscoreTeam awayTeamForState = game.getAwayTeam();
            String awayTricode = awayTeamForState != null && awayTeamForState.getTeamTricode() != null
                    ? awayTeamForState.getTeamTricode() : "AWAY";
            ObjectNode screenState = objectMapper.createObjectNode();
            screenState.put("gd_boxscore_team", awayTricode);
            screenState.put("gd_boxscore_away_sortCol", "points");
            screenState.put("gd_boxscore_away_sortDir", "desc");
            screenState.put("gd_boxscore_home_sortCol", "points");
            screenState.put("gd_boxscore_home_sortDir", "desc");
            response.set("state", screenState);

            ArrayNode sections = objectMapper.createArrayNode();

            // 1. VideoPlayer — inline video for the game (platform SDK integration)
            sections.add(buildVideoPlayerSection(gameId, game, contentSourceId));

            // 2. GamePanel (scoreboard displayConfig)
            sections.add(buildGamePanelScoreboardFromLive(game, gameId, contentSourceId));

            // 3. StatLine (top performers)
            ObjectNode statLineSection = buildStatLineSectionFromLive(game, gameId, contentSourceId);
            if (statLineSection != null) {
                sections.add(statLineSection);
            }

            // 3b. Responsive row – home/away top performers side-by-side
            ObjectNode rowSection = buildRowSectionFromLive(game, gameId, contentSourceId);
            if (rowSection != null) {
                sections.add(rowSection);
            }

            // 4. ContentRail (AtomicComposite) — server-built editorial preview rail.
            sections.add(buildGameDetailContentRail(contentSourceId));

            // 5. TabGroup — Box Score + Highlights tabs
            ObjectNode tabGroup = buildGameDetailTabGroupFromLive(game, gameId, contentSourceId);
            if (tabGroup != null) {
                sections.add(tabGroup);
            }

            response.set("sections", sections);

            // Overlays — server-composed modal content triggered by SDK callbacks
            response.set("overlays", buildOverlays(gameId, contentSourceId));

            return response;

        } catch (Exception e) {
            log.error("Failed to compose from live data for gameId={}: {}", gameId, e.getMessage(), e);
        }
        return null;
    }

    private JsonNode refreshGameDetailSection(String sectionId, SduiRequestContext ctx) throws IOException {
        SectionIdDeriver.Parsed parsed = SectionIdDeriver.parse(sectionId);
        if (!parsed.isDerived() || !parsed.source().startsWith("stats-api:game-")) {
            throw new IllegalArgumentException("Invalid game detail sectionId: " + sectionId);
        }

        String gameId = parsed.source().substring("stats-api:game-".length());
        if (!"scoreboard".equals(parsed.slug())) {
            throw new UnsupportedSectionException(
                "Game detail composer does not support section refresh for slug='" + parsed.slug() + "'");
        }

        BoxscoreResponse boxscore = statsPort.getBoxscore(gameId);
        if (boxscore == null) {
            // No live boxscore available — surface as a refresh failure so the client
            // can render its sectionStates error treatment rather than receiving
            // invented content.
            throw new IOException("No live boxscore for gameId=" + gameId);
        }

        BoxscoreGame game = boxscore.getGame();
        if (game == null) {
            throw new IOException("No game data in boxscore for gameId=" + gameId);
        }

        ObjectNode section = buildGamePanelScoreboardFromLive(game, gameId, parsed.source());
        // Override the section's refresh policy to the canonical game-state-aware policy
        // so the returned section always carries consistent 60 s poll or SSE semantics.
        String gameState = deriveGameState(gameId);
        section.set("refreshPolicy",
                buildRefreshPolicyForGameSection(gameState, section.path("id").asText(""), gameId));
        return section;
    }

    /**
     * Build a minimal screen carrying a single {@code ErrorState} section when
     * the live stats API has no data for the requested {@code gameId}. Per
     * AGENTS.md §8.0, the server surfaces an {@code ErrorState} rather than
     * inventing fallback content.
     */
    private ObjectNode buildErrorScreen(String gameId, String traceId, String locale) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "game-detail-" + gameId);
        response.put("schemaVersion", schemaVersion);
        response.put("parentUri", "nba://scoreboard");
        response.set("navigation", utils.buildNavigation("game-detail"));

        ObjectNode defaultRefreshPolicy = objectMapper.createObjectNode();
        defaultRefreshPolicy.put("type", "static");
        response.set("defaultRefreshPolicy", defaultRefreshPolicy);

        ArrayNode sections = objectMapper.createArrayNode();
        ObjectNode error = utils.buildErrorSection(
                SectionIdDeriver.derive("stats-api:game-" + gameId, "AtomicComposite", "error-not-found"),
                "Game not available",
                "We couldn't load this game. Please try again later.",
                "not_found",
                "nba://scoreboard");
        error.put("contentSourceId", "stats-api:game-" + gameId);
        sections.add(error);
        response.set("sections", sections);

        utils.prependAppBarHeaderIfNeeded(response);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    /**
     * Build the editorial content rail shown beneath the live game data on the
     * game-detail screen. Composed in code (not loaded from an example file) so
     * the server is the single source of truth for every section it emits.
     */
    private ObjectNode buildGameDetailContentRail(String contentSourceId) {
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "content-rail");
        String[][] cards = {
            {"content-1", "Game Preview", "What to watch for tonight", FALLBACK_THUMB, "article", null, "nba://article/game-preview"},
            {"content-2", "Key Matchups", "Players to follow", FALLBACK_THUMB, "article", null, "nba://article/key-matchups"},
            {"content-3", "Pregame Interview", "Coach's keys to the game", FALLBACK_THUMB, "video", "3:45", "nba://video/pregame-interview"}
        };
        ObjectNode section = atomicBuilder.buildContentRail(sectionId, "game_detail_content_rail", "Preview", cards);
        section.put("contentSourceId", contentSourceId);
        section.set("surface", surfaces.railSurface());
        return section;
    }

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildBoxscoreTabGroupFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        BoxscoreTeam homeTeam = game.getHomeTeam();
        BoxscoreTeam awayTeam = game.getAwayTeam();
        if (homeTeam == null || awayTeam == null) return null;

        String homeTricode = homeTeam.getTeamTricode() != null ? homeTeam.getTeamTricode() : "HOME";
        String awayTricode = awayTeam.getTeamTricode() != null ? awayTeam.getTeamTricode() : "AWAY";
        int gameStatus = game.getGameStatus();

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "TabGroup", "boxscore-tabs"));
        section.put("contentSourceId", contentSourceId);
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
                awayTeam, gameId, contentSourceId, "away",
                "gd_boxscore_away_sortCol", "gd_boxscore_away_sortDir", gameStatus);
        awayTable.set("surface", surfaces.flushSurface());
        ObjectNode homeTable = boxscoreComposer.buildBoxscoreTableSection(
                homeTeam, gameId, contentSourceId, "home",
                "gd_boxscore_home_sortCol", "gd_boxscore_home_sortDir", gameStatus);
        homeTable.set("surface", surfaces.flushSurface());

        ObjectNode tabContents = objectMapper.createObjectNode();
        ArrayNode awayContent = objectMapper.createArrayNode();
        awayContent.add(awayTable);
        tabContents.set(awayTricode, awayContent);

        ArrayNode homeContent = objectMapper.createArrayNode();
        homeContent.add(homeTable);
        tabContents.set(homeTricode, homeContent);

        data.set("tabContents", tabContents);
        section.set("data", data);
        section.set("subsections", utils.tabSelectSubsections(tabs, "gd_boxscore_team"));
        section.set("surface", surfaces.stripSurfaceWithoutBackground());
        return section;
    }


    private ObjectNode buildGamePanelScoreboardFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        int gameStatus = game.getGameStatus();
        boolean live = gameStatus == 2;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "scoreboard");

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        if (live) {
            refreshPolicy.put("type", "sse");
            refreshPolicy.put("channel", gameId + ":linescore");
            refreshPolicy.put("pauseWhenOffScreen", false);
        } else if (gameStatus == 1) {
            // Pre-game: poll the SDUI section endpoint every 5 minutes so the
            // client re-composes the section. When the game goes live the server
            // returns sse policy and the client transitions the subscription.
            refreshPolicy.put("type", "poll");
            refreshPolicy.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
            refreshPolicy.put("intervalMs", 300_000);
            refreshPolicy.put("pauseWhenOffScreen", true);
        } else {
            refreshPolicy.put("type", "static");
        }

        AtomicCompositeBuilder.GameClockSnapshot clock = live
                ? new AtomicCompositeBuilder.GameClockSnapshot(
                        parseGameClockSeconds(game.getGameClock() != null ? game.getGameClock() : ""),
                        java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                        AtomicCompositeBuilder.INITIAL_CLOCK_RUNNING)
                : null;

        ObjectNode section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                null,
                "scoreboard",
                game.getGameId() != null ? game.getGameId() : "",
                gameStatus,
                game.getGameStatusText() != null ? game.getGameStatusText() : "",
                null,
                atomicBuilder.gamePanelTeam(game.getAwayTeam()),
                atomicBuilder.gamePanelTeam(game.getHomeTeam()),
                clock,
                null,
                refreshPolicy,
                utils.buildCompositeLinescoreBindings(),
                surfaces.gamePanelSurface());

        section.put("contentSourceId", contentSourceId);
        section.set("sectionStates", utils.buildSectionStates(
                sectionId, "Unable to load live scores", "shimmer", 180));
        return section;
    }

    private static int parseGameClockSeconds(String iso) {
        if (iso == null || iso.isEmpty()) return 0;
        try {
            return (int) java.time.Duration.parse(iso).getSeconds();
        } catch (Exception e) {
            return 0;
        }
    }

    private ObjectNode buildStatLineSectionFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        ArrayNode stats = objectMapper.createArrayNode();

        List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.getHomeTeam(), 3);
        List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.getAwayTeam(), 3);

        homePerformers.forEach(stats::add);
        awayPerformers.forEach(stats::add);

        if (stats.isEmpty()) {
            return null;
        }

        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "top-performers");
        ObjectNode section = atomicBuilder.buildStatLineFromNodes(
                sectionId, null, "Top Performers", "vertical", stats);
        section.put("contentSourceId", contentSourceId);

        // No refreshPolicy / sectionStates: this stat line is an
        // AtomicComposite whose ui tree is composed with literal player
        // names and stat values baked in at compose time. Attaching a
        // poll without corresponding `dataBinding` targets would cause
        // clients to drop the polled payload (correct behaviour on the
        // symmetrical poll handler) — leaving the section as a ghost
        // "refreshing" section that never updates. Re-introduce poll +
        // dataBinding together once the ui tree references stat fields
        // via templated paths instead of literals.

        return section;
    }

    private ObjectNode buildRowSectionFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        try {
            List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.getHomeTeam(), 2);
            List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.getAwayTeam(), 2);
            if (homePerformers.isEmpty() && awayPerformers.isEmpty()) return null;

            String homeTricode = game.getHomeTeam() != null && game.getHomeTeam().getTeamTricode() != null
                    ? game.getHomeTeam().getTeamTricode() : "HOME";
            String awayTricode = game.getAwayTeam() != null && game.getAwayTeam().getTeamTricode() != null
                    ? game.getAwayTeam().getTeamTricode() : "AWAY";

            ObjectNode homeChild = buildStatLineChild("row-home-stats", homeTricode + " Leaders", homePerformers);
            ObjectNode awayChild = buildStatLineChild("row-away-stats", awayTricode + " Leaders", awayPerformers);

            ObjectNode root = atomicBuilder.responsiveRow(tokens.spacing("lg"), 600);
            ArrayNode children = objectMapper.createArrayNode();

            ObjectNode homeSlot = atomicBuilder.sectionSlot("row-home", homeChild);
            atomicBuilder.setFlex(homeSlot, 1);
            children.add(homeSlot);

            ObjectNode awaySlot = atomicBuilder.sectionSlot("row-away", awayChild);
            atomicBuilder.setFlex(awaySlot, 1);
            children.add(awaySlot);

            root.set("children", children);
            String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "team-leaders-row");
            ObjectNode section = atomicBuilder.wrapAsComposite(sectionId, null, root);
            section.put("contentSourceId", contentSourceId);
            return section;
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

    private List<ObjectNode> getTopPerformersFromTeam(BoxscoreTeam team, int maxPlayers) {
        List<ObjectNode> performers = new ArrayList<>();
        if (team == null || team.getPlayers() == null || team.getPlayers().isEmpty()) return performers;

        String teamTricode = team.getTeamTricode() != null ? team.getTeamTricode() : "";
        int teamId = team.getTeamId() != null ? team.getTeamId() : 0;

        List<Map.Entry<BoxscorePlayer, Integer>> playerPoints = new ArrayList<>();
        for (BoxscorePlayer player : team.getPlayers()) {
            BoxscoreStatistics playerStats = player.getStatistics();
            int points = playerStats != null ? playerStats.getPoints() : 0;
            if (points >= 5) {
                playerPoints.add(Map.entry(player, points));
            }
        }

        playerPoints.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        int count = 0;
        for (Map.Entry<BoxscorePlayer, Integer> entry : playerPoints) {
            if (count >= maxPlayers) break;

            BoxscorePlayer player = entry.getKey();
            int points = entry.getValue();
            BoxscoreStatistics playerStats = player.getStatistics();

            ObjectNode item = objectMapper.createObjectNode();
            int personId = player.getPersonId() != null ? player.getPersonId() : 0;
            item.put("playerId", personId);

            String playerName = player.getName() != null ? player.getName() : "";
            if (playerName.isEmpty()) {
                String firstName = player.getFirstName() != null ? player.getFirstName() : "";
                String familyName = player.getFamilyName() != null ? player.getFamilyName() : "";
                playerName = firstName + " " + familyName;
            }
            item.put("playerName", playerName);
            item.put("playerImageUrl",
                    "https://cdn.nba.com/headshots/nba/latest/1040x760/" + personId + ".png");
            item.put("teamTricode", teamTricode);
            item.put("teamId", teamId);

            item.put("statCategory", "PTS");
            item.put("statValue", String.valueOf(points));
            item.put("statLabel", "Points");

            ObjectNode additionalStats = objectMapper.createObjectNode();
            additionalStats.put("rebounds", playerStats != null ? playerStats.getReboundsTotal() : 0);
            additionalStats.put("assists", playerStats != null ? playerStats.getAssists() : 0);
            additionalStats.put("minutes", playerStats != null && playerStats.getMinutes() != null
                    ? playerStats.getMinutes() : "");
            item.set("additionalStats", additionalStats);

            performers.add(item);
            count++;
        }

        return performers;
    }

    // ── VideoPlayer section ─────────────────────────────────────────────

    /**
     * VideoPlayer section — reserved SDK integration point for the video
     * player. Today the visible surface is the pre-SDK placeholder composed
     * under {@code data.ui}; after the video SDK lands the same tree becomes
     * the loading / error placeholder the SDK overlays. SDK inputs
     * ({@code playerType}, {@code contentId}, {@code autoplay},
     * {@code capabilities}) live at the top of {@code data} so the SDK reads
     * them without walking the tree.
     */
    private ObjectNode buildVideoPlayerSection(String gameId, BoxscoreGame game, String contentSourceId) {
        int gameStatus = game.getGameStatus();

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "VideoPlayer", "video-player"));
        section.put("contentSourceId", contentSourceId);
        section.put("type", "VideoPlayer");
        section.put("analyticsId", "game_detail_video_player");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        section.set("surface", surfaces.videoPlayerSurface());

        ObjectNode root = atomicBuilder.container("column", "center", "center");
        root.put("widthMode", "fill");
        root.put("height", 440);
        root.put("background", "#000000");
        ArrayNode rootChildren = objectMapper.createArrayNode();
        rootChildren.add(atomicBuilder.text("▶", "displayLarge", "bold", "#FFFFFF", null));
        rootChildren.add(atomicBuilder.spacer(tokens.spacing("md")));
        rootChildren.add(atomicBuilder.text("Video Player", "titleMedium", null, "#FFFFFF", null));
        rootChildren.add(atomicBuilder.text("game • " + gameId, "bodySmall", null,
                "rgba(255,255,255,0.6)", null));
        root.set("children", rootChildren);

        ObjectNode data = atomicBuilder.wrapUi(root);
        data.put("playerType", "game");
        data.put("contentId", gameId);
        data.put("autoplay", gameStatus == 2);

        ArrayNode capabilities = objectMapper.createArrayNode();
        capabilities.add("pip");
        capabilities.add("fullscreenRotation");
        data.set("capabilities", capabilities);

        ObjectNode displayConfig = objectMapper.createObjectNode();
        displayConfig.put("aspectRatio", "16:9");
        data.set("displayConfig", displayConfig);

        section.set("data", data);
        return section;
    }

    // ── Overlays (server-composed modal content) ─────────────────────

    private ObjectNode buildOverlays(String gameId, String contentSourceId) {
        ObjectNode overlays = objectMapper.createObjectNode();

        overlays.set("couchRightsWarning", buildOverlaySection(
                contentSourceId, "couch-rights-warning",
                tokens.icon("warning"),
                "Viewing Time Limited",
                "Your couch rights viewing window is active. You have limited time remaining on this stream.",
                "Got It",
                "dismiss"
        ));

        overlays.set("couchRightsExpired", buildOverlaySection(
                contentSourceId, "couch-rights-expired",
                tokens.icon("warning"),
                "Viewing Time Expired",
                "Your couch rights viewing window has ended. Subscribe to League Pass for unlimited access.",
                "Subscribe Now",
                "nba://leaguepass"
        ));

        overlays.set("unentitled", buildOverlaySection(
                contentSourceId, "unentitled",
                tokens.icon("lock"),
                "Subscription Required",
                "This content requires an active NBA League Pass subscription.",
                "View Plans",
                "nba://leaguepass"
        ));

        return overlays;
    }

    private ObjectNode buildOverlaySection(String contentSourceId, String slug, String icon, String title,
                                            String message, String ctaLabel, String ctaTarget) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "Container");
        root.put("direction", "column");
        root.put("alignment", "center");
        root.put("crossAlignment", "center");
        root.set("padding", atomicBuilder.padding(
                24, // §3.6: no semantic spacing token for 24
                24, // §3.6: no semantic spacing token for 24
                tokens.spacing("xl"),
                tokens.spacing("xl")
        ));
        root.put("background", tokens.color("nba.bg.primary"));
        root.put("cornerRadius", tokens.radius("lg"));

        ArrayNode children = objectMapper.createArrayNode();

        ObjectNode iconEl = objectMapper.createObjectNode();
        iconEl.put("type", "Text");
        iconEl.put("content", icon);
        iconEl.put("variant", "headlineLarge");
        iconEl.put("textAlign", "center");
        children.add(iconEl);

        ObjectNode spacer1 = objectMapper.createObjectNode();
        spacer1.put("type", "Spacer");
        spacer1.put("height", tokens.spacing("lg"));
        children.add(spacer1);

        ObjectNode titleEl = objectMapper.createObjectNode();
        titleEl.put("type", "Text");
        titleEl.put("content", title);
        titleEl.put("variant", "headlineMedium");
        titleEl.put("weight", "bold");
        titleEl.put("color", tokens.color("nba.label.primary"));
        titleEl.put("textAlign", "center");
        children.add(titleEl);

        ObjectNode spacer2 = objectMapper.createObjectNode();
        spacer2.put("type", "Spacer");
        spacer2.put("height", 8);
        children.add(spacer2);

        ObjectNode messageEl = objectMapper.createObjectNode();
        messageEl.put("type", "Text");
        messageEl.put("content", message);
        messageEl.put("variant", "bodyMedium");
        messageEl.put("color", tokens.color("nba.label.secondary"));
        messageEl.put("textAlign", "center");
        children.add(messageEl);

        ObjectNode spacer3 = objectMapper.createObjectNode();
        spacer3.put("type", "Spacer");
        spacer3.put("height", 24);
        children.add(spacer3);

        ObjectNode button = objectMapper.createObjectNode();
        button.put("type", "Button");
        button.put("label", ctaLabel);
        button.put("variant", "primary");
        ArrayNode actions = objectMapper.createArrayNode();
        ObjectNode action = objectMapper.createObjectNode();
        action.put("trigger", "onActivate");
        if ("dismiss".equals(ctaTarget)) {
            action.put("type", "dismiss");
        } else {
            action.put("type", "navigate");
            action.put("targetUri", ctaTarget);
        }
        actions.add(action);
        button.set("actions", actions);
        children.add(button);

        root.set("children", children);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug));
        section.put("contentSourceId", contentSourceId);
        section.put("type", "AtomicComposite");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        ObjectNode data = objectMapper.createObjectNode();
        data.set("ui", root);
        section.set("data", data);
        return section;
    }

    private ObjectNode padHelper(int start, int end, int top, int bottom) {
        ObjectNode p = objectMapper.createObjectNode();
        p.put("start", start);
        p.put("end", end);
        p.put("top", top);
        p.put("bottom", bottom);
        return p;
    }

    // ── Extended TabGroup with Highlights tab ─────────────────────────

    private ObjectNode buildGameDetailTabGroupFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        BoxscoreTeam homeTeam = game.getHomeTeam();
        BoxscoreTeam awayTeam = game.getAwayTeam();
        if (homeTeam == null || awayTeam == null) return null;

        String homeTricode = homeTeam.getTeamTricode() != null ? homeTeam.getTeamTricode() : "HOME";
        String awayTricode = awayTeam.getTeamTricode() != null ? awayTeam.getTeamTricode() : "AWAY";
        int gameStatus = game.getGameStatus();

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "TabGroup", "game-detail-tabs"));
        section.put("contentSourceId", contentSourceId);
        section.put("type", "TabGroup");
        section.put("analyticsId", "game_detail_tabs");

        ObjectNode data = objectMapper.createObjectNode();
        data.put("stateKey", "gd_active_tab");
        data.put("defaultTab", "boxscore");

        ArrayNode tabs = objectMapper.createArrayNode();

        ObjectNode boxscoreTab = objectMapper.createObjectNode();
        boxscoreTab.put("id", "tab-boxscore");
        boxscoreTab.put("label", "Box Score");
        boxscoreTab.put("stateKey", "gd_active_tab");
        boxscoreTab.put("stateValue", "boxscore");
        tabs.add(boxscoreTab);

        ObjectNode highlightsTab = objectMapper.createObjectNode();
        highlightsTab.put("id", "tab-highlights");
        highlightsTab.put("label", "Highlights");
        highlightsTab.put("stateKey", "gd_active_tab");
        highlightsTab.put("stateValue", "highlights");
        tabs.add(highlightsTab);

        data.set("tabs", tabs);

        ObjectNode tabContents = objectMapper.createObjectNode();

        // Box Score tab — team sub-tabs with boxscore tables
        ArrayNode boxscoreContent = objectMapper.createArrayNode();
        ObjectNode teamTabGroup = buildBoxscoreTabGroupFromLive(game, gameId, contentSourceId);
        if (teamTabGroup != null) {
            boxscoreContent.add(teamTabGroup);
        }
        tabContents.set("boxscore", boxscoreContent);

        // Highlights tab — VOD cards with mutate actions (1.5 player-adjacent content)
        ArrayNode highlightsContent = objectMapper.createArrayNode();
        highlightsContent.add(buildHighlightsSection(gameId, contentSourceId));
        tabContents.set("highlights", highlightsContent);

        data.set("tabContents", tabContents);
        section.set("data", data);
        section.set("subsections", utils.tabSelectSubsections(tabs, "gd_active_tab"));
        section.set("surface", surfaces.stripSurfaceWithoutBackground());

        return section;
    }

    /**
     * Builds a highlights section using mock data. Each VOD card carries a mutate action
     * that sets screenState.selectedHighlight — the VideoPlayer section's contentId can
     * be bound to this state key via Conditional, enabling player-adjacent content switching
     * without navigation (Gap 6 / Phase 1.5).
     */
    private ObjectNode buildHighlightsSection(String gameId, String contentSourceId) {
        String[][] highlights = {
                {"hl-1", "Monster Dunk", "https://cdn.nba.com/manage/2025/02/giannis-dunk-752x428.jpg", "0:32", "media-0029400101"},
                {"hl-2", "Buzzer Beater", "https://cdn.nba.com/manage/2025/02/curry-buzzer-752x428.jpg", "0:45", "media-0029400102"},
                {"hl-3", "Chase-Down Block", "https://cdn.nba.com/manage/2025/02/ad-block-752x428.jpg", "0:28", "media-0029400103"},
                {"hl-4", "Full Game Highlights", "https://cdn.nba.com/manage/2025/02/bos-mia-recap-752x428.jpg", "9:45", "media-0029400105"},
        };

        ObjectNode scroll = objectMapper.createObjectNode();
        scroll.put("type", "ScrollContainer");
        scroll.put("direction", "row");
        scroll.put("gap", tokens.spacing("md"));
        scroll.put("showIndicators", false);
        ArrayNode scrollChildren = objectMapper.createArrayNode();

        for (String[] hl : highlights) {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("type", "Container");
            card.put("direction", "column");
            card.put("id", hl[0]);
            card.put("cornerRadius", 8); // §3.6: no semantic token mapping for corner radius 8
            card.put("background", tokens.color("nba.bg.primary"));

            ObjectNode shadowObj = objectMapper.createObjectNode();
            shadowObj.put("color", "#00000020");
            shadowObj.put("radius", 4);
            shadowObj.put("offsetX", 0);
            shadowObj.put("offsetY", 2);
            card.set("shadow", shadowObj);

            ArrayNode cardActions = objectMapper.createArrayNode();
            ObjectNode mutateAction = objectMapper.createObjectNode();
            mutateAction.put("trigger", "onActivate");
            mutateAction.put("type", "mutate");
            mutateAction.put("target", "screenState.selectedHighlight");
            mutateAction.put("operation", "set");
            mutateAction.put("value", hl[4]);
            cardActions.add(mutateAction);
            card.set("actions", cardActions);

            ArrayNode cardChildren = objectMapper.createArrayNode();

            ObjectNode img = objectMapper.createObjectNode();
            img.put("type", "Image");
            img.put("src", hl[2]);
            img.put("width", 200);
            img.put("height", 112);
            img.put("fit", "cover");
            img.put("cornerRadius", 8); // §3.6: no semantic token mapping for corner radius 8
            AccessibilityHelper.addImage(objectMapper, img, hl[0]);

            ObjectNode durationBadgeEl = objectMapper.createObjectNode();
            durationBadgeEl.put("type", "Container");
            durationBadgeEl.put("direction", "row");
            durationBadgeEl.put("cornerRadius", tokens.radius("sm"));
            durationBadgeEl.put("background", "#000000B3");
            durationBadgeEl.put("opacity", 0.85);
            ObjectNode dbPad = objectMapper.createObjectNode();
            dbPad.put("start", tokens.spacing("sm"));
            dbPad.put("end", tokens.spacing("sm"));
            dbPad.put("top", tokens.spacing("xs"));
            dbPad.put("bottom", tokens.spacing("xs"));
            durationBadgeEl.set("padding", dbPad);
            ArrayNode dbChildren = objectMapper.createArrayNode();
            ObjectNode dbText = objectMapper.createObjectNode();
            dbText.put("type", "Text");
            dbText.put("content", hl[3]);
            dbText.put("variant", "labelSmall");
            dbText.put("color", tokens.color("nba.label-inverted.primary"));
            dbChildren.add(dbText);
            durationBadgeEl.set("children", dbChildren);

            ObjectNode badgeObj = objectMapper.createObjectNode();
            badgeObj.set("element", durationBadgeEl);
            badgeObj.put("alignment", "bottomEnd");
            img.set("badge", badgeObj);

            cardChildren.add(img);

            ObjectNode spacer = objectMapper.createObjectNode();
            spacer.put("type", "Spacer");
            spacer.put("height", 6);
            cardChildren.add(spacer);

            ObjectNode title = objectMapper.createObjectNode();
            title.put("type", "Text");
            title.put("content", hl[1]);
            title.put("variant", "bodySmall");
            title.put("weight", "semiBold");
            title.put("color", tokens.color("nba.label.primary"));
            title.put("maxLines", 2);
            ObjectNode titlePad = objectMapper.createObjectNode();
            titlePad.put("start", 8); // §3.6: no semantic spacing token for 8
            titlePad.put("end", 8); // §3.6: no semantic spacing token for 8
            titlePad.put("top", 0); // §3.6: no semantic value for zero
            titlePad.put("bottom", tokens.spacing("sm"));
            title.set("padding", titlePad);
            cardChildren.add(title);

            card.set("children", cardChildren);
            scrollChildren.add(card);
        }

        scroll.set("children", scrollChildren);

        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "Container");
        root.put("direction", "column");
        ObjectNode rootPad = objectMapper.createObjectNode();
        rootPad.put("start", 0); // §3.6: no semantic value for zero
        rootPad.put("end", 0); // §3.6: no semantic value for zero
        rootPad.put("top", 8); // §3.6: no semantic spacing token for 8
        rootPad.put("bottom", 8); // §3.6: no semantic spacing token for 8
        root.set("padding", rootPad);
        ArrayNode rootChildren = objectMapper.createArrayNode();
        rootChildren.add(scroll);
        root.set("children", rootChildren);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "highlights"));
        section.put("contentSourceId", contentSourceId);
        section.put("type", "AtomicComposite");
        section.put("analyticsId", "game_detail_highlights");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        ObjectNode data = objectMapper.createObjectNode();
        data.set("ui", root);
        section.set("data", data);
        return section;
    }

    // ── Section refresh policy ─────────────────────────────────────────

    /**
     * Override the {@code refreshPolicy} on every scoreboard section to match the
     * canonical rules for the current game state:
     *
     * <ul>
     *   <li><b>Pre-game (any data source)</b> — section-level poll every 60 s.
     *       When the game goes live the server returns {@code sse} policy on the
     *       next poll and the client transitions automatically.</li>
     *   <li><b>Live, real data</b> — SSE via Ably channel ({@code gameId:linescore}).</li>
     *   <li><b>Live, mock data</b> — section-level poll every 60 s; no real Ably
     *       channel is available for mock games.</li>
     *   <li><b>Post-game (any data source)</b> — section-level poll every 60 s so
     *       E2E tests can verify the refresh lifecycle end to end.</li>
     * </ul>
     *
     * <p>Called at the end of {@link #composeGameDetail} after all variant
     * transforms so every code path that produces sections is covered uniformly.
     */
    private void applyGameSectionRefreshPolicies(ObjectNode response, String gameState, String gameId) {
        JsonNode sections = response.get("sections");
        if (sections == null || !sections.isArray()) return;
        for (JsonNode node : sections) {
            if (!node.isObject()) continue;
            ObjectNode section = (ObjectNode) node;
            String slug = SectionIdDeriver.extractSlug(section.path("id").asText(""));
            if (!"scoreboard".equals(slug)) continue;
            String sectionId = section.path("id").asText("");
            section.set("refreshPolicy",
                    buildRefreshPolicyForGameSection(gameState, sectionId, gameId));
        }
    }

    /**
     * Build the {@code refreshPolicy} node for a scoreboard-type section.
     *
     * <p>Mirrors the games-screen game-card policy: SSE on {@code {gameId}:linescore}
     * when the game is in progress, otherwise a 60 s section poll so the client
     * picks up the live transition without a screen refresh.
     *
     * @param gameState  {@code "pre"}, {@code "live"}, or {@code "post"}
     * @param sectionId  fully-derived section ID (used to build {@code sectionEndpoint})
     * @param gameId     NBA game ID (used to build the Ably channel name)
     */
    private ObjectNode buildRefreshPolicyForGameSection(String gameState,
                                                        String sectionId, String gameId) {
        ObjectNode policy = objectMapper.createObjectNode();
        if ("live".equals(gameState)) {
            policy.put("type", "sse");
            policy.put("channel", gameId + ":linescore");
            policy.put("pauseWhenOffScreen", false);
        } else {
            // pre or post: section-level poll every 60 s so the client picks up
            // the live transition without a screen refresh.
            policy.put("type", "poll");
            policy.put("sectionEndpoint", "/v1/sdui/section/" + sectionId);
            policy.put("intervalMs", 60_000);
            policy.put("pauseWhenOffScreen", true);
        }
        return policy;
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

    // ── Section ID normalization ────────────────────────────────────────

    /**
     * Re-derive section IDs so every section — whether from live composition
     * or from an example fallback JSON — carries the canonical
     * {@code contentSourceId~type=SectionType~slug=name} format.
     *
     * <p>For sections that already carry a derived ID (contains {@code ~type=}),
     * the ID is left untouched. For flat-ID sections (example JSON), the
     * existing ID is treated as the slug, the section's {@code type} is read,
     * and {@link SectionIdDeriver#derive} produces the canonical form.
     *
     * <p>This runs once at the top of {@link #composeGameDetail} so that every
     * downstream consumer (variant transforms, section refresh routing,
     * client-side merge-by-id) sees a uniform ID format.
     */
    private void rederiveSectionIds(ObjectNode response, String contentSourceId) {
        if (!response.has("sections") || !response.get("sections").isArray()) return;
        ArrayNode sections = (ArrayNode) response.get("sections");
        for (int i = 0; i < sections.size(); i++) {
            ObjectNode section = (ObjectNode) sections.get(i);
            String id = section.path("id").asText("");
            if (SectionIdDeriver.isDerived(id)) continue;
            String type = section.path("type").asText("AtomicComposite");
            section.put("id", SectionIdDeriver.derive(contentSourceId, type, id));
            if (!section.has("contentSourceId")) {
                section.put("contentSourceId", contentSourceId);
            }
        }
    }

    // ── Variant transformations ────────────────────────────────────────

    /**
     * Build a slug→index lookup for the sections array. Variant transforms
     * use this instead of scanning by raw ID, making them ID-format-agnostic.
     */
    private static Map<String, Integer> buildSlugIndex(ArrayNode sections) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < sections.size(); i++) {
            String id = sections.get(i).path("id").asText();
            index.put(SectionIdDeriver.extractSlug(id), i);
        }
        return index;
    }

    private void applyVariantB(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        Map<String, Integer> slugIndex = buildSlugIndex(sections);
        Integer contentRailIndex = slugIndex.get("content-rail");
        Integer tabGroupIndex = slugIndex.get("game-detail-tabs");

        if (contentRailIndex != null && tabGroupIndex != null && contentRailIndex < tabGroupIndex) {
            JsonNode contentRail = sections.get(contentRailIndex);
            JsonNode tabGroup = sections.get(tabGroupIndex);
            sections.set(contentRailIndex, tabGroup);
            sections.set(tabGroupIndex, contentRail);
            log.debug("Applied variant B: swapped ContentRail and TabGroup positions");
        }
    }

    private static final Set<String> VARIANT_C_KEEP = Set.of(
            "scoreboard", "game-detail-tabs"
    );

    private void applyVariantC(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        ArrayNode filtered = objectMapper.createArrayNode();

        for (JsonNode section : sections) {
            String slug = SectionIdDeriver.extractSlug(section.path("id").asText());
            String type = section.path("type").asText("");
            if ("VideoPlayer".equals(type) || VARIANT_C_KEEP.contains(slug)) {
                filtered.add(section);
            }
        }

        response.set("sections", filtered);
        log.debug("Applied variant C (minimal): kept VideoPlayer + whitelist, {} sections remaining", filtered.size());
    }

    private void applyVariantD(ObjectNode response) {
        if (!response.has("sections")) return;

        ArrayNode sections = (ArrayNode) response.get("sections");
        Map<String, Integer> slugIndex = buildSlugIndex(sections);

        String[][] trendingCards = {
            {"trending-1", "Top 10 Plays of the Night", "Last night's best moments", FALLBACK_THUMB, "video", "4:30", "nba://video/top10-plays"},
            {"trending-2", "Playoff Intensity", "The best of postseason basketball", FALLBACK_THUMB, "video", "2:15", "nba://video/playoff-intensity"},
            {"trending-3", "Post-Game Press Conference", "Hear from the coaches", FALLBACK_THUMB, "video", "6:00", "nba://video/post-game-presser"}
        };
        ObjectNode extraHeader = atomicBuilder.buildSectionHeader(
                "trending-videos-header", "Trending Videos", null, null, null);
        extraHeader.set("surface", surfaces.sectionHeaderSurface());
        ObjectNode extraRail = atomicBuilder.buildContentRail("trending-videos",
                "trending_videos_rail", null, trendingCards);
        extraRail.set("surface", surfaces.railSurface());

        Integer insertAfter = slugIndex.get("content-rail");
        if (insertAfter != null) {
            ArrayNode updated = objectMapper.createArrayNode();
            for (int i = 0; i < sections.size(); i++) {
                updated.add(sections.get(i));
                if (i == insertAfter) {
                    updated.add(extraHeader);
                    updated.add(extraRail);
                }
            }
            response.set("sections", updated);
            log.debug("Applied variant D: added Trending Videos rail, {} sections total", updated.size());
        } else {
            log.debug("Applied variant D: content-rail not found, skipping insertion");
        }
    }
}
