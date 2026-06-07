package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nba.sdui.models.generated.Action;
import com.nba.sdui.models.generated.AtomicComposite;
import com.nba.sdui.models.generated.AtomicElement;
import com.nba.sdui.models.generated.Capability;
import com.nba.sdui.models.generated.DisplayConfig;
import com.nba.sdui.models.generated.ExperimentVariantOption;
import com.nba.sdui.models.generated.ExperimentVariants;
import com.nba.sdui.models.generated.Overlays;
import com.nba.sdui.models.generated.RefreshPolicy;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionStates;
import com.nba.sdui.models.generated.State;
import com.nba.sdui.models.generated.Subsection;
import com.nba.sdui.models.generated.TabContents;
import com.nba.sdui.models.generated.TabData;
import com.nba.sdui.models.generated.TabGroup;
import com.nba.sdui.models.generated.VideoPlayer;
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

    private final StatsPort statsPort;
    private final BoxscoreComposer boxscoreComposer;
    private final SectionRefreshService sectionRefreshService;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public GameDetailComposer(StatsPort statsPort,
                              BoxscoreComposer boxscoreComposer,
                              SectionRefreshService sectionRefreshService,
                              SduiUtils utils,
                              SectionSurfaces surfaces,
                              Tokens tokens) {
        this.statsPort = statsPort;
        this.boxscoreComposer = boxscoreComposer;
        this.sectionRefreshService = sectionRefreshService;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(tokens);
    }

    @PostConstruct
    void registerSectionRefreshResolvers() {
        sectionRefreshService.registerResolver(
                SectionIdDeriver.prefixFor("stats-api:game-"),
                this::refreshGameDetailSection);
    }

    /**
     * Result of composing a Game Detail screen — carries both the typed
     * response and the server-derived game state for cache-control decisions.
     */
    public record GameDetailResult(Screen response, String derivedGameState) {}

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
        Screen response = composeFromLiveData(gameId);
        if (response != null) {
            derivedGameState = deriveGameState(gameId);
        } else {
            log.warn("Live game detail unavailable for gameId={} — composing ErrorState screen", gameId);
            return new GameDetailResult(
                    buildErrorScreen(gameId, traceId, locale),
                    derivedGameState);
        }

        String contentSourceId = "stats-api:game-" + gameId;
        rederiveSectionIds(response, contentSourceId);

        response.setId("game-detail-" + gameId);
        // Screen-level default refresh is static; sections own pre-game polling and live SSE.
        RefreshPolicy defaultRefreshPolicy = new RefreshPolicy();
        defaultRefreshPolicy.setType(RefreshPolicy.RefreshType.STATIC);
        response.setDefaultRefreshPolicy(defaultRefreshPolicy);
        response.setSchemaVersion(schemaVersion);
        response.setParentUri("nba://scoreboard");
        response.setNavigation(utils.buildNavigation("game-detail"));

        // Expose available A/B variants so clients never need URI-sniffing.
        ExperimentVariants variantsWrapper = new ExperimentVariants();
        variantsWrapper.setExperimentId("game_detail_variant");
        List<ExperimentVariantOption> options = new ArrayList<>();
        options.add(new ExperimentVariantOption()
                .withId("A").withLabel("Default").withDescription("All sections, standard order"));
        options.add(new ExperimentVariantOption()
                .withId("B").withLabel("Reorder").withDescription("Content rail and TabGroup swapped"));
        options.add(new ExperimentVariantOption()
                .withId("C").withLabel("Minimal").withDescription("Video player, scoreboard, and tabs only"));
        options.add(new ExperimentVariantOption()
                .withId("D").withLabel("Extra Rail").withDescription("Trending videos rail added after content rail"));
        variantsWrapper.setOptions(options);
        response.setVariants(variantsWrapper);

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
                variant, response.getSections() != null ? response.getSections().size() : 0);

        applyGameSectionRefreshPolicies(response, derivedGameState, gameId);
        prependVariantChipsIfPresent(response, gameId, variant);

        utils.prependAppBarHeaderIfNeeded(response);
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
    private void prependVariantChipsIfPresent(Screen response, String gameId, String activeVariant) {
        ExperimentVariants variants = response.getVariants();
        if (variants == null) return;
        String experimentId = variants.getExperimentId();
        List<ExperimentVariantOption> options = variants.getOptions();
        if (experimentId == null || experimentId.isBlank() || options == null || options.isEmpty()) return;

        String currentUri = "nba://game/" + gameId;
        String normalized = (activeVariant == null || activeVariant.isBlank())
                ? "A" : activeVariant.toUpperCase();
        Section chips = atomicBuilder.buildVariantChipsComposite(
                SectionIdDeriver.derive("game-detail-" + gameId, "AtomicComposite", "variantChips"),
                "game_detail_variant_chips",
                currentUri, experimentId, options, normalized);
        chips.setSurface(surfaces.flushSurface());

        List<Section> sections = response.getSections() != null
                ? response.getSections()
                : new ArrayList<>();
        List<Section> merged = new ArrayList<>();
        // Insert after the app-bar (index 0) so the chips sit just below the
        // header. If no app-bar is present, the chips go at the very top.
        int insertAt = 0;
        if (!sections.isEmpty()) {
            String firstId = sections.get(0).getId();
            if (SectionIdDeriver.endsWithSlug(firstId, "appBar")) {
                merged.add(sections.get(0));
                insertAt = 1;
            }
        }
        merged.add(chips);
        for (int i = insertAt; i < sections.size(); i++) {
            merged.add(sections.get(i));
        }
        response.setSections(merged);
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

    private Screen composeFromLiveData(String gameId) {
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

            Screen response = new Screen();
            response.setId("game-detail-" + gameId);
            response.setSchemaVersion(schemaVersion);
            response.setParentUri("nba://scoreboard");

            // Content source for all sections derived from this game
            String contentSourceId = "stats-api:game-" + gameId;

            // Default screen state — boxscore team tabs default to away (first listed)
            BoxscoreTeam awayTeamForState = game.getAwayTeam();
            String awayTricode = awayTeamForState != null && awayTeamForState.getTeamTricode() != null
                    ? awayTeamForState.getTeamTricode() : "AWAY";
            State screenState = new State();
            screenState.setAdditionalProperty("gd_boxscore_team", awayTricode);
            screenState.setAdditionalProperty("gd_boxscore_away_sortCol", "points");
            screenState.setAdditionalProperty("gd_boxscore_away_sortDir", "desc");
            screenState.setAdditionalProperty("gd_boxscore_home_sortCol", "points");
            screenState.setAdditionalProperty("gd_boxscore_home_sortDir", "desc");
            response.setState(screenState);

            List<Section> sections = new ArrayList<>();

            // 1. VideoPlayer — inline video for the game (platform SDK integration)
            sections.add(buildVideoPlayerSection(gameId, game, contentSourceId));

            // 2. GamePanel (scoreboard displayConfig)
            sections.add(buildGamePanelScoreboardFromLive(game, gameId, contentSourceId));

            // 3. StatLine (top performers)
            Section statLineSection = buildStatLineSectionFromLive(game, gameId, contentSourceId);
            if (statLineSection != null) {
                sections.add(statLineSection);
            }

            // 3b. Responsive row – home/away top performers side-by-side
            Section rowSection = buildRowSectionFromLive(game, gameId, contentSourceId);
            if (rowSection != null) {
                sections.add(rowSection);
            }

            // 4. ContentRail (AtomicComposite) — server-built editorial preview rail.
            sections.add(buildGameDetailContentRail(contentSourceId));

            // 5. TabGroup — Box Score + Highlights tabs
            Section tabGroup = buildGameDetailTabGroupFromLive(game, gameId, contentSourceId);
            if (tabGroup != null) {
                sections.add(tabGroup);
            }

            response.setSections(sections);

            // Overlays — server-composed modal content triggered by SDK callbacks
            response.setOverlays(buildOverlays(gameId, contentSourceId));

            return response;

        } catch (Exception e) {
            log.error("Failed to compose from live data for gameId={}: {}", gameId, e.getMessage(), e);
        }
        return null;
    }

    private Section refreshGameDetailSection(String sectionId, SduiRequestContext ctx) throws IOException {
        SectionIdDeriver.Parsed parsed = SectionIdDeriver.parse(sectionId);
        String gameSourcePrefix = SectionIdDeriver.prefixFor("stats-api:game-");
        if (!parsed.isDerived() || !parsed.source().startsWith(gameSourcePrefix)) {
            throw new IllegalArgumentException("Invalid game detail sectionId: " + sectionId);
        }

        String gameId = parsed.source().substring(gameSourcePrefix.length());
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

        Section section = buildGamePanelScoreboardFromLive(game, gameId, parsed.source());
        // Override the section's refresh policy to the canonical game-state-aware policy
        // so the returned section always carries consistent 60 s poll or SSE semantics.
        String gameState = deriveGameState(gameId);
        section.setRefreshPolicy(buildRefreshPolicyForGameSection(
                gameState, section.getId() == null ? "" : section.getId(), gameId));
        return section;
    }

    /**
     * Build a minimal screen carrying a single {@code ErrorState} section when
     * the live stats API has no data for the requested {@code gameId}. Per
     * AGENTS.md §8.0, the server surfaces an {@code ErrorState} rather than
     * inventing fallback content.
     */
    private Screen buildErrorScreen(String gameId, String traceId, String locale) {
        Screen response = new Screen();
        response.setId("game-detail-" + gameId);
        response.setSchemaVersion(schemaVersion);
        response.setParentUri("nba://scoreboard");
        response.setNavigation(utils.buildNavigation("game-detail"));

        RefreshPolicy defaultRefreshPolicy = new RefreshPolicy();
        defaultRefreshPolicy.setType(RefreshPolicy.RefreshType.STATIC);
        response.setDefaultRefreshPolicy(defaultRefreshPolicy);

        List<Section> sections = new ArrayList<>();
        Section error = utils.buildErrorSection(
                SectionIdDeriver.derive("stats-api:game-" + gameId, "AtomicComposite", "errorNotFound"),
                "Game not available",
                "We couldn't load this game. Please try again later.",
                "not_found",
                "nba://scoreboard");
        error.setContentSourceId("stats-api:game-" + gameId);
        sections.add(error);
        response.setSections(sections);

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
    private Section buildGameDetailContentRail(String contentSourceId) {
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "contentRail");
        String[][] cards = {
            {"content-1", "Game Preview", "What to watch for tonight", FALLBACK_THUMB, "article", null, "nba://article/game-preview"},
            {"content-2", "Key Matchups", "Players to follow", FALLBACK_THUMB, "article", null, "nba://article/key-matchups"},
            {"content-3", "Pregame Interview", "Coach's keys to the game", FALLBACK_THUMB, "video", "3:45", "nba://video/pregame-interview"}
        };
        Section section = atomicBuilder.buildContentRail(sectionId, "game_detail_content_rail", "Preview", cards);
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.railSurface());
        return section;
    }

    // ── Section builders ───────────────────────────────────────────────

    private Section buildBoxscoreTabGroupFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        BoxscoreTeam homeTeam = game.getHomeTeam();
        BoxscoreTeam awayTeam = game.getAwayTeam();
        if (homeTeam == null || awayTeam == null) return null;

        String homeTricode = homeTeam.getTeamTricode() != null ? homeTeam.getTeamTricode() : "HOME";
        String awayTricode = awayTeam.getTeamTricode() != null ? awayTeam.getTeamTricode() : "AWAY";
        int gameStatus = game.getGameStatus();

        Section section = new Section();
        section.setId(SectionIdDeriver.derive(contentSourceId, "TabGroup", "boxscoreTabs"));
        section.setContentSourceId(contentSourceId);
        section.setType(Section.Type.TAB_GROUP);
        section.setAnalyticsId("game_detail_boxscore_tabs");

        TabGroup data = new TabGroup();
        data.setStateKey("gd_boxscore_team");
        data.setDefaultTab(awayTricode);

        TabData awayTab = new TabData();
        awayTab.setId("tab-" + awayTricode.toLowerCase());
        awayTab.setLabel(awayTricode);
        awayTab.setStateKey("gd_boxscore_team");
        awayTab.setStateValue(awayTricode);

        TabData homeTab = new TabData();
        homeTab.setId("tab-" + homeTricode.toLowerCase());
        homeTab.setLabel(homeTricode);
        homeTab.setStateKey("gd_boxscore_team");
        homeTab.setStateValue(homeTricode);

        List<TabData> tabsList = List.of(awayTab, homeTab);
        data.setTabs(tabsList);

        // Reuse the full BoxscoreTable builder from BoxscoreComposer.
        Section awayTableSection = boxscoreComposer.buildBoxscoreTableSection(
                awayTeam, gameId, contentSourceId, "away",
                "gd_boxscore_away_sortCol", "gd_boxscore_away_sortDir", gameStatus);
        awayTableSection.setSurface(surfaces.flushSurface());

        Section homeTableSection = boxscoreComposer.buildBoxscoreTableSection(
                homeTeam, gameId, contentSourceId, "home",
                "gd_boxscore_home_sortCol", "gd_boxscore_home_sortDir", gameStatus);
        homeTableSection.setSurface(surfaces.flushSurface());

        TabContents tabContents = new TabContents();
        tabContents.setAdditionalProperty(awayTricode, List.of(awayTableSection));
        tabContents.setAdditionalProperty(homeTricode, List.of(homeTableSection));
        data.setTabContents(tabContents);

        section.setData(data);
        section.setSubsections(tabSelectSubsections(tabsList, "gd_boxscore_team"));
        section.setSurface(surfaces.stripSurfaceWithoutBackground());
        return section;
    }

    /**
     * Build one {@link Subsection} per tab carrying an {@code onActivate→mutate}
     * action for tab selection.
     */
    private List<Subsection> tabSelectSubsections(List<TabData> tabs, String stateKey) {
        List<Subsection> subsections = new ArrayList<>(tabs.size());
        for (TabData tab : tabs) {
            String stateValue = tab.getStateValue() != null ? tab.getStateValue() : tab.getId();
            Action mutate = new Action();
            mutate.setTrigger(Action.ActionTrigger.ON_ACTIVATE);
            mutate.setType(Action.ActionType.MUTATE);
            mutate.setTarget(stateKey);
            mutate.setValue(stateValue);
            Subsection sub = new Subsection();
            sub.setId(tab.getId());
            sub.setActions(List.of(mutate));
            subsections.add(sub);
        }
        return subsections;
    }


    private Section buildGamePanelScoreboardFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        int gameStatus = game.getGameStatus();
        boolean live = gameStatus == 2;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "scoreboard");

        RefreshPolicy refreshPolicy;
        if (live) {
            refreshPolicy = new RefreshPolicy()
                    .withType(RefreshPolicy.RefreshType.SSE)
                    .withChannel(gameId + ":linescore")
                    .withPauseWhenOffScreen(false);
        } else if (gameStatus == 1) {
            // Pre-game: poll the SDUI section endpoint every 5 minutes so the
            // client re-composes the section. When the game goes live the server
            // returns sse policy and the client transitions the subscription.
            refreshPolicy = new RefreshPolicy()
                    .withType(RefreshPolicy.RefreshType.POLL)
                    .withSectionEndpoint("/v1/sdui/section/" + sectionId)
                    .withIntervalMs(300_000)
                    .withPauseWhenOffScreen(true);
        } else {
            refreshPolicy = new RefreshPolicy().withType(RefreshPolicy.RefreshType.STATIC);
        }

        AtomicCompositeBuilder.GameClockSnapshot clock = live
                ? new AtomicCompositeBuilder.GameClockSnapshot(
                        parseGameClockSeconds(game.getGameClock() != null ? game.getGameClock() : ""),
                        java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                        AtomicCompositeBuilder.INITIAL_CLOCK_RUNNING)
                : null;

        Section section = atomicBuilder.buildGamePanelComposite(
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

        section.setContentSourceId(contentSourceId);
        section.setSectionStates(
                utils.buildSectionStates(sectionId, "Unable to load live scores", "shimmer", 180));
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

    private Section buildStatLineSectionFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        List<String[]> homePerformers = getTopPerformersFromTeam(game.getHomeTeam(), 3);
        List<String[]> awayPerformers = getTopPerformersFromTeam(game.getAwayTeam(), 3);

        List<String[]> stats = new ArrayList<>();
        stats.addAll(homePerformers);
        stats.addAll(awayPerformers);

        if (stats.isEmpty()) {
            return null;
        }

        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "topPerformers");
        Section section = atomicBuilder.buildStatLine(
                sectionId, null, "Top Performers", "vertical",
                stats.toArray(new String[0][]));
        section.setContentSourceId(contentSourceId);

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

    private Section buildRowSectionFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        try {
            List<String[]> homePerformers = getTopPerformersFromTeam(game.getHomeTeam(), 2);
            List<String[]> awayPerformers = getTopPerformersFromTeam(game.getAwayTeam(), 2);
            if (homePerformers.isEmpty() && awayPerformers.isEmpty()) return null;

            String homeTricode = game.getHomeTeam() != null && game.getHomeTeam().getTeamTricode() != null
                    ? game.getHomeTeam().getTeamTricode() : "HOME";
            String awayTricode = game.getAwayTeam() != null && game.getAwayTeam().getTeamTricode() != null
                    ? game.getAwayTeam().getTeamTricode() : "AWAY";

            Section homeChild = buildStatLineChild("row-home-stats", homeTricode + " Leaders", homePerformers);
            Section awayChild = buildStatLineChild("row-away-stats", awayTricode + " Leaders", awayPerformers);

            AtomicElement root = atomicBuilder.responsiveRow(tokens.spacing("lg"), 600);
            List<AtomicElement> children = new ArrayList<>();

            AtomicElement homeSlot = atomicBuilder.sectionSlot("row-home", homeChild);
            atomicBuilder.setFlex(homeSlot, 1);
            children.add(homeSlot);

            AtomicElement awaySlot = atomicBuilder.sectionSlot("row-away", awayChild);
            atomicBuilder.setFlex(awaySlot, 1);
            children.add(awaySlot);

            root.setChildren(children);
            String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "teamLeadersRow");
            Section section = atomicBuilder.wrapAsComposite(sectionId, null, root);
            section.setContentSourceId(contentSourceId);
            return section;
        } catch (Exception e) {
            log.warn("Failed to build responsive row from live data: {}", e.getMessage());
            return null;
        }
    }

    private Section buildStatLineChild(String id, String title, List<String[]> performers) {
        return atomicBuilder.buildStatLine(id, null, title, "vertical",
                performers.toArray(new String[0][]));
    }

    private List<String[]> getTopPerformersFromTeam(BoxscoreTeam team, int maxPlayers) {
        List<String[]> performers = new ArrayList<>();
        if (team == null || team.getPlayers() == null || team.getPlayers().isEmpty()) return performers;

        String teamTricode = team.getTeamTricode() != null ? team.getTeamTricode() : "";

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

            int personId = player.getPersonId() != null ? player.getPersonId() : 0;
            String playerName = player.getName() != null ? player.getName() : "";
            if (playerName.isEmpty()) {
                String firstName = player.getFirstName() != null ? player.getFirstName() : "";
                String familyName = player.getFamilyName() != null ? player.getFamilyName() : "";
                playerName = firstName + " " + familyName;
            }
            String playerImageUrl = "https://cdn.nba.com/headshots/nba/latest/1040x760/" + personId + ".png";

            performers.add(new String[]{
                    String.valueOf(personId),
                    playerName,
                    teamTricode,
                    "PTS",
                    String.valueOf(points),
                    playerImageUrl
            });
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
    private Section buildVideoPlayerSection(String gameId, BoxscoreGame game, String contentSourceId) {
        int gameStatus = game.getGameStatus();

        Section section = new Section();
        section.setId(SectionIdDeriver.derive(contentSourceId, "VideoPlayer", "videoPlayer"));
        section.setContentSourceId(contentSourceId);
        section.setType(Section.Type.VIDEO_PLAYER);
        section.setAnalyticsId("game_detail_video_player");

        RefreshPolicy refreshPolicy = new RefreshPolicy();
        refreshPolicy.setType(RefreshPolicy.RefreshType.STATIC);
        section.setRefreshPolicy(refreshPolicy);
        section.setSurface(surfaces.videoPlayerSurface());

        AtomicElement root = atomicBuilder.container("column", "center", "center");
        atomicBuilder.widthMode(root, "fill");
        root.setHeight(440);
        root.setBackgrounds(List.of("#000000"));
        List<AtomicElement> rootChildren = new ArrayList<>();
        rootChildren.add(atomicBuilder.text("▶", "displayLarge", "bold", "#FFFFFF", null));
        rootChildren.add(atomicBuilder.spacer(tokens.spacing("md")));
        rootChildren.add(atomicBuilder.text("Video Player", "titleMedium", null, "#FFFFFF", null));
        rootChildren.add(atomicBuilder.text("game • " + gameId, "bodySmall", null,
                "rgba(255,255,255,0.6)", null));
        root.setChildren(rootChildren);

        VideoPlayer data = new VideoPlayer();
        data.setPlayerType(VideoPlayer.PlayerType.GAME);
        data.setContentId(gameId);
        data.setAutoplay(gameStatus == 2);
        data.setCapabilities(List.of(Capability.PIP, Capability.FULLSCREEN_ROTATION));
        data.setUi(root);

        DisplayConfig displayConfig = new DisplayConfig();
        displayConfig.setAspectRatio("16:9");
        data.setDisplayConfig(displayConfig);

        section.setData(data);
        return section;
    }

    // ── Overlays (server-composed modal content) ─────────────────────

    private Overlays buildOverlays(String gameId, String contentSourceId) {
        Overlays overlays = new Overlays();

        overlays.setAdditionalProperty("couchRightsWarning", buildOverlaySection(
                contentSourceId, "couchRightsWarning",
                tokens.icon("warning"),
                "Viewing Time Limited",
                "Your couch rights viewing window is active. You have limited time remaining on this stream.",
                "Got It",
                "dismiss"
        ));

        overlays.setAdditionalProperty("couchRightsExpired", buildOverlaySection(
                contentSourceId, "couchRightsExpired",
                tokens.icon("warning"),
                "Viewing Time Expired",
                "Your couch rights viewing window has ended. Subscribe to League Pass for unlimited access.",
                "Subscribe Now",
                "nba://leaguepass"
        ));

        overlays.setAdditionalProperty("unentitled", buildOverlaySection(
                contentSourceId, "unentitled",
                tokens.icon("lock"),
                "Subscription Required",
                "This content requires an active NBA League Pass subscription.",
                "View Plans",
                "nba://leaguepass"
        ));

        return overlays;
    }

    private Section buildOverlaySection(String contentSourceId, String slug, String icon, String title,
                                            String message, String ctaLabel, String ctaTarget) {
        AtomicElement root = atomicBuilder.container("column", "center", "center");
        root.setPadding(atomicBuilder.padding(
                24, // §3.6: no semantic spacing token for 24
                24, // §3.6: no semantic spacing token for 24
                tokens.spacing("xl"),
                tokens.spacing("xl")
        ));
        root.setBackgrounds(List.of(tokens.color("nba.bg.primary")));
        root.setCornerRadius(tokens.radius("lg"));

        List<AtomicElement> children = new ArrayList<>();

        AtomicElement iconEl = atomicBuilder.text(icon, "headlineLarge", null, null, null);
        iconEl.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
        children.add(iconEl);

        children.add(atomicBuilder.spacer(tokens.spacing("lg")));

        AtomicElement titleEl = atomicBuilder.text(title, "headlineMedium", "bold",
                tokens.color("nba.label.primary"), null);
        titleEl.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
        children.add(titleEl);

        children.add(atomicBuilder.spacer(8));

        AtomicElement messageEl = atomicBuilder.text(message, "bodyMedium", null,
                tokens.color("nba.label.secondary"), null);
        messageEl.setTextAlign(AtomicElement.TextAlign.fromValue("center"));
        children.add(messageEl);

        children.add(atomicBuilder.spacer(24));

        Action action = new Action();
        action.setTrigger(Action.ActionTrigger.fromValue("onActivate"));
        if ("dismiss".equals(ctaTarget)) {
            action.setType(Action.ActionType.fromValue("dismiss"));
        } else {
            action.setType(Action.ActionType.fromValue("navigate"));
            action.setTargetUri(ctaTarget);
        }
        children.add(atomicBuilder.button(ctaLabel, "primary", action));

        root.setChildren(children);

        return atomicBuilder.wrapAsComposite(
                SectionIdDeriver.derive(contentSourceId, "AtomicComposite", slug),
                null,
                root);
    }

    // ── Extended TabGroup with Highlights tab ─────────────────────────

    private Section buildGameDetailTabGroupFromLive(BoxscoreGame game, String gameId, String contentSourceId) {
        BoxscoreTeam homeTeam = game.getHomeTeam();
        BoxscoreTeam awayTeam = game.getAwayTeam();
        if (homeTeam == null || awayTeam == null) return null;

        int gameStatus = game.getGameStatus();

        Section section = new Section();
        section.setId(SectionIdDeriver.derive(contentSourceId, "TabGroup", "gameDetailTabs"));
        section.setContentSourceId(contentSourceId);
        section.setType(Section.Type.TAB_GROUP);
        section.setAnalyticsId("game_detail_tabs");

        TabGroup data = new TabGroup();
        data.setStateKey("gd_active_tab");
        data.setDefaultTab("boxscore");

        TabData boxscoreTab = new TabData();
        boxscoreTab.setId("tab-boxscore");
        boxscoreTab.setLabel("Box Score");
        boxscoreTab.setStateKey("gd_active_tab");
        boxscoreTab.setStateValue("boxscore");

        TabData highlightsTab = new TabData();
        highlightsTab.setId("tab-highlights");
        highlightsTab.setLabel("Highlights");
        highlightsTab.setStateKey("gd_active_tab");
        highlightsTab.setStateValue("highlights");

        List<TabData> tabsList = List.of(boxscoreTab, highlightsTab);
        data.setTabs(tabsList);

        TabContents tabContents = new TabContents();

        // Box Score tab — team sub-tabs with boxscore tables
        List<Section> boxscoreContent = new ArrayList<>();
        Section teamTabGroup = buildBoxscoreTabGroupFromLive(game, gameId, contentSourceId);
        if (teamTabGroup != null) {
            boxscoreContent.add(teamTabGroup);
        }
        tabContents.setAdditionalProperty("boxscore", boxscoreContent);

        // Highlights tab — VOD cards with mutate actions (1.5 player-adjacent content)
        tabContents.setAdditionalProperty("highlights",
                List.of(buildHighlightsSection(gameId, contentSourceId)));

        data.setTabContents(tabContents);
        section.setData(data);
        section.setSubsections(tabSelectSubsections(tabsList, "gd_active_tab"));
        section.setSurface(surfaces.stripSurfaceWithoutBackground());

        return section;
    }

    /**
     * Builds a highlights section using mock data. Each VOD card carries a mutate action
     * that sets screenState.selectedHighlight — the VideoPlayer section's contentId can
     * be bound to this state key via Conditional, enabling player-adjacent content switching
     * without navigation (Gap 6 / Phase 1.5).
     */
    private Section buildHighlightsSection(String gameId, String contentSourceId) {
        String[][] highlights = {
                {"hl-1", "Monster Dunk", "https://cdn.nba.com/manage/2025/02/giannis-dunk-752x428.jpg", "0:32", "media-0029400101"},
                {"hl-2", "Buzzer Beater", "https://cdn.nba.com/manage/2025/02/curry-buzzer-752x428.jpg", "0:45", "media-0029400102"},
                {"hl-3", "Chase-Down Block", "https://cdn.nba.com/manage/2025/02/ad-block-752x428.jpg", "0:28", "media-0029400103"},
                {"hl-4", "Full Game Highlights", "https://cdn.nba.com/manage/2025/02/bos-mia-recap-752x428.jpg", "9:45", "media-0029400105"},
        };

        AtomicElement scroll = atomicBuilder.scrollContainer("row", tokens.spacing("md"), false);
        List<AtomicElement> scrollChildren = new ArrayList<>();

        for (String[] hl : highlights) {
            AtomicElement card = atomicBuilder.container("column", null, null);
            card.setId(hl[0]);
            card.setCornerRadius(8); // §3.6: no semantic token mapping for corner radius 8
            card.setBackgrounds(List.of(tokens.color("nba.bg.primary")));
            atomicBuilder.shadow(card, "#00000020", 4, 0, 2);

            Action mutateAction = new Action();
            mutateAction.setTrigger(Action.ActionTrigger.fromValue("onActivate"));
            mutateAction.setType(Action.ActionType.fromValue("mutate"));
            mutateAction.setTarget("screenState.selectedHighlight");
            mutateAction.setOperation(Action.MutateOperation.fromValue("set"));
            mutateAction.setValue(hl[4]);
            card.setActions(List.of(mutateAction));

            List<AtomicElement> cardChildren = new ArrayList<>();

            AtomicElement img = atomicBuilder.image(hl[2], 200, 112, "cover");
            img.setCornerRadius(8); // §3.6: no semantic token mapping for corner radius 8
            AccessibilityHelper.addImage(img, hl[0]);

            AtomicElement durationBadgeEl = atomicBuilder.container("row", null, null);
            durationBadgeEl.setCornerRadius(tokens.radius("sm"));
            durationBadgeEl.setBackgrounds(List.of("#000000B3"));
            durationBadgeEl.setOpacity(0.85);
            durationBadgeEl.setPadding(atomicBuilder.padding(
                    tokens.spacing("sm"), tokens.spacing("sm"),
                    tokens.spacing("xs"), tokens.spacing("xs")));
            AtomicElement dbText = atomicBuilder.text(hl[3], "labelSmall", null,
                    tokens.color("nba.label-inverted.primary"), null);
            durationBadgeEl.setChildren(List.of(dbText));

            atomicBuilder.badge(img, durationBadgeEl, "bottomEnd");

            cardChildren.add(img);
            cardChildren.add(atomicBuilder.spacer(6));

            AtomicElement title = atomicBuilder.text(hl[1], "bodySmall", "semiBold",
                    tokens.color("nba.label.primary"), 2);
            title.setPadding(atomicBuilder.padding(
                    8, // §3.6: no semantic spacing token for 8
                    8, // §3.6: no semantic spacing token for 8
                    0, // §3.6: no semantic value for zero
                    tokens.spacing("sm")));
            cardChildren.add(title);

            card.setChildren(cardChildren);
            scrollChildren.add(card);
        }

        scroll.setChildren(scrollChildren);

        AtomicElement root = atomicBuilder.container("column", null, null);
        root.setPadding(atomicBuilder.padding(
                0, // §3.6: no semantic value for zero
                0, // §3.6: no semantic value for zero
                8, // §3.6: no semantic spacing token for 8
                8)); // §3.6: no semantic spacing token for 8
        root.setChildren(List.of(scroll));

        Section section = atomicBuilder.wrapAsComposite(
                SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "highlights"),
                "game_detail_highlights",
                root);
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
    private void applyGameSectionRefreshPolicies(Screen response, String gameState, String gameId) {
        List<Section> sections = response.getSections();
        if (sections == null) return;
        for (Section section : sections) {
            String slug = SectionIdDeriver.extractSlug(section.getId() != null ? section.getId() : "");
            if (!"scoreboard".equals(slug)) continue;
            String sectionId = section.getId() != null ? section.getId() : "";
            section.setRefreshPolicy(buildRefreshPolicyForGameSection(gameState, sectionId, gameId));
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
    private RefreshPolicy buildRefreshPolicyForGameSection(String gameState,
                                                            String sectionId, String gameId) {
        RefreshPolicy policy = new RefreshPolicy();
        if ("live".equals(gameState)) {
            policy.setType(RefreshPolicy.RefreshType.SSE);
            policy.setChannel(gameId + ":linescore");
            policy.setPauseWhenOffScreen(false);
        } else {
            // pre or post: section-level poll every 60 s so the client picks up
            // the live transition without a screen refresh.
            policy.setType(RefreshPolicy.RefreshType.POLL);
            policy.setSectionEndpoint("/v1/sdui/section/" + sectionId);
            policy.setIntervalMs(60_000);
            policy.setPauseWhenOffScreen(true);
        }
        return policy;
    }

    // ── Channel resolution ─────────────────────────────────────────────

    private void resolveChannelPatterns(Screen response, String gameId) {
        List<Section> sections = response.getSections();
        if (sections == null) return;
        for (Section section : sections) {
            RefreshPolicy refreshPolicy = section.getRefreshPolicy();
            if (refreshPolicy == null) continue;
            String channel = refreshPolicy.getChannel();
            if (channel == null) continue;
            refreshPolicy.setChannel(channel.replace("{gameId}", gameId));
        }
    }

    // ── Section ID normalization ────────────────────────────────────────

    /**
     * Re-derive section IDs so every section — whether from live composition
     * or from an example fallback JSON — carries the canonical
     * {@code contentSourceId__type-SectionType__slug-name} format.
     *
     * <p>For sections that already carry a derived ID (contains {@code __type-}),
     * the ID is left untouched. For flat-ID sections (example JSON), the
     * existing ID is treated as the slug, the section's {@code type} is read,
     * and {@link SectionIdDeriver#derive} produces the canonical form.
     *
     * <p>This runs once at the top of {@link #composeGameDetail} so that every
     * downstream consumer (variant transforms, section refresh routing,
     * client-side merge-by-id) sees a uniform ID format.
     */
    private void rederiveSectionIds(Screen response, String contentSourceId) {
        List<Section> sections = response.getSections();
        if (sections == null) return;
        for (Section section : sections) {
            String id = section.getId() != null ? section.getId() : "";
            if (SectionIdDeriver.isDerived(id)) continue;
            String type = section.getType() != null ? section.getType().value() : "AtomicComposite";
            section.setId(SectionIdDeriver.derive(contentSourceId, type, toCamelSlug(id)));
            if (section.getContentSourceId() == null) {
                section.setContentSourceId(contentSourceId);
            }
        }
    }

    /**
     * Coerce a free-form flat ID (e.g. {@code "content-rail"}) into a
     * lowerCamelCase slug accepted by {@link SectionIdDeriver#derive}. Used
     * when promoting example-JSON flat IDs into the derived form.
     */
    private static String toCamelSlug(String raw) {
        if (raw == null || raw.isEmpty()) return "section";
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                if (sb.length() == 0 && c >= '0' && c <= '9') {
                    sb.append('s').append(c);
                } else {
                    sb.append(sb.length() == 0
                            ? Character.toLowerCase(c)
                            : (nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c)));
                }
                nextUpper = false;
            } else {
                nextUpper = true;
            }
        }
        return sb.length() == 0 ? "section" : sb.toString();
    }

    // ── Variant transformations ────────────────────────────────────────

    /**
     * Build a slug→index lookup for the sections list. Variant transforms
     * use this instead of scanning by raw ID, making them ID-format-agnostic.
     */
    private static Map<String, Integer> buildSlugIndex(List<Section> sections) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < sections.size(); i++) {
            String id = sections.get(i).getId() != null ? sections.get(i).getId() : "";
            index.put(SectionIdDeriver.extractSlug(id), i);
        }
        return index;
    }

    private void applyVariantB(Screen response) {
        List<Section> sections = response.getSections();
        if (sections == null) return;
        Map<String, Integer> slugIndex = buildSlugIndex(sections);
        Integer contentRailIndex = slugIndex.get("contentRail");
        Integer tabGroupIndex = slugIndex.get("gameDetailTabs");

        if (contentRailIndex != null && tabGroupIndex != null && contentRailIndex < tabGroupIndex) {
            Section contentRail = sections.get(contentRailIndex);
            Section tabGroup = sections.get(tabGroupIndex);
            sections.set(contentRailIndex, tabGroup);
            sections.set(tabGroupIndex, contentRail);
            log.debug("Applied variant B: swapped ContentRail and TabGroup positions");
        }
    }

    private static final Set<String> VARIANT_C_KEEP = Set.of(
            "scoreboard", "gameDetailTabs"
    );

    private void applyVariantC(Screen response) {
        List<Section> sections = response.getSections();
        if (sections == null) return;
        List<Section> filtered = new ArrayList<>();
        for (Section section : sections) {
            String slug = SectionIdDeriver.extractSlug(section.getId() != null ? section.getId() : "");
            String type = section.getType() != null ? section.getType().value() : "";
            if ("VideoPlayer".equals(type) || VARIANT_C_KEEP.contains(slug)) {
                filtered.add(section);
            }
        }
        response.setSections(filtered);
        log.debug("Applied variant C (minimal): kept VideoPlayer + whitelist, {} sections remaining", filtered.size());
    }

    private void applyVariantD(Screen response) {
        List<Section> sections = response.getSections();
        if (sections == null) return;
        Map<String, Integer> slugIndex = buildSlugIndex(sections);

        String[][] trendingCards = {
            {"trending-1", "Top 10 Plays of the Night", "Last night's best moments", FALLBACK_THUMB, "video", "4:30", "nba://video/top10-plays"},
            {"trending-2", "Playoff Intensity", "The best of postseason basketball", FALLBACK_THUMB, "video", "2:15", "nba://video/playoff-intensity"},
            {"trending-3", "Post-Game Press Conference", "Hear from the coaches", FALLBACK_THUMB, "video", "6:00", "nba://video/post-game-presser"}
        };
        Section extraHeader = atomicBuilder.buildSectionHeader(
                SectionIdDeriver.derive("variant:trending-videos", "AtomicComposite", "trendingHeader"),
                "Trending Videos", null, null, null);
        extraHeader.setSurface(surfaces.sectionHeaderSurface());
        Section extraRail = atomicBuilder.buildContentRail(
                SectionIdDeriver.derive("variant:trending-videos", "AtomicComposite", "trendingVideos"),
                "trending_videos_rail", null, trendingCards);
        extraRail.setSurface(surfaces.railSurface());

        Integer insertAfter = slugIndex.get("contentRail");
        if (insertAfter != null) {
            List<Section> updated = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                updated.add(sections.get(i));
                if (i == insertAfter) {
                    updated.add(extraHeader);
                    updated.add(extraRail);
                }
            }
            response.setSections(updated);
            log.debug("Applied variant D: added Trending Videos rail, {} sections total", updated.size());
        } else {
            log.debug("Applied variant D: contentRail not found, skipping insertion");
        }
    }
}
