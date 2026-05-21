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
        JsonNode baseResponse;
        baseResponse = composeFromLiveData(gameId);
        if (baseResponse != null) {
            derivedGameState = deriveGameState(gameId);
        } else {
            log.warn("Live game detail unavailable, falling back to example for gameState={}", derivedGameState);
            baseResponse = utils.loadExampleResponse(derivedGameState);
        }

        if (baseResponse == null) {
            log.warn("No example response found for gameState={}, using 'pre' as default", derivedGameState);
            baseResponse = utils.loadExampleResponse("pre");
        }

        ObjectNode response = baseResponse.deepCopy();

        response.put("id", "game-detail-" + gameId);
        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        response.put("parentUri", "nba://scoreboard");
        response.set("navigation", utils.buildNavigation("game-detail"));

        // Expose available A/B variants so clients never need URI-sniffing.
        ArrayNode variants = objectMapper.createArrayNode();
        variants.add(objectMapper.createObjectNode().put("id", "A").put("label", "Default").put("description", "All sections, standard order"));
        variants.add(objectMapper.createObjectNode().put("id", "B").put("label", "Reorder").put("description", "Content rail and TabGroup swapped"));
        variants.add(objectMapper.createObjectNode().put("id", "C").put("label", "Minimal").put("description", "Player stats and promo banner removed"));
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

        utils.prependAppBarHeaderIfNeeded(response);
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return new GameDetailResult(response, derivedGameState);
    }

    // ── Live-data composition ──────────────────────────────────────────

    /**
     * Derive the game state string from the boxscore API for the given game.
     * NBA API gameStatus: 1 = pre, 2 = live, 3 = post/final.
     */
    private String deriveGameState(String gameId) {
        try {
            JsonNode boxscore = statsApiClient.getBoxscore(gameId);
            if (boxscore == null) return "pre";
            int status = boxscore.path("game").path("gameStatus").asInt(1);
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
            response.put("schemaVersion", schemaVersion);
            response.put("parentUri", "nba://scoreboard");

            // Content source for all sections derived from this game
            String contentSourceId = "stats-api:game-" + gameId;

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

            // 4. ContentRail (AtomicComposite) from example
            ObjectNode contentRail = loadSectionFromExample("content-rail");
            if (contentRail != null) {
                contentRail.put("id", SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "content-rail"));
                contentRail.put("contentSourceId", contentSourceId);
                sections.add(contentRail);
            }

            // 5. PromoBanner (AtomicComposite) from example
            ObjectNode promoBanner = loadSectionFromExample("promo-banner");
            if (promoBanner != null) {
                promoBanner.put("id", SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "promo-banner"));
                promoBanner.put("contentSourceId", contentSourceId);
                sections.add(promoBanner);
            }

            // 6. TabGroup — Box Score + Highlights tabs
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

    private ObjectNode loadSectionFromExample(String sectionId) {
        try {
            JsonNode example = utils.loadExampleResponse("pre");
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

    private ObjectNode buildBoxscoreTabGroupFromLive(JsonNode game, String gameId, String contentSourceId) {
        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");
        if (homeTeam.isMissingNode() || awayTeam.isMissingNode()) return null;

        String homeTricode = homeTeam.path("teamTricode").asText("HOME");
        String awayTricode = awayTeam.path("teamTricode").asText("AWAY");
        int gameStatus = game.path("gameStatus").asInt(1);

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
        awayTable.set("surface", utils.flushSurface());
        ObjectNode homeTable = boxscoreComposer.buildBoxscoreTableSection(
                homeTeam, gameId, contentSourceId, "home",
                "gd_boxscore_home_sortCol", "gd_boxscore_home_sortDir", gameStatus);
        homeTable.set("surface", utils.flushSurface());

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
        section.set("surface", utils.stripSurfaceWithoutBackground());
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
        section.set("subsections", utils.tabSelectSubsections(tabs, "activeTab"));

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

    private ObjectNode buildGamePanelScoreboardFromLive(JsonNode game, String gameId, String contentSourceId) {
        int gameStatus = game.path("gameStatus").asInt();
        boolean live = gameStatus == 2;

        ObjectNode refreshPolicy = objectMapper.createObjectNode();
        refreshPolicy.put("type", "sse");
        refreshPolicy.put("channel", gameId + ":linescore");
        refreshPolicy.put("pauseWhenOffScreen", false);

        AtomicCompositeBuilder.GameClockSnapshot clock = live
                ? new AtomicCompositeBuilder.GameClockSnapshot(
                        parseGameClockSeconds(game.path("gameClock").asText("")),
                        java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                        AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING)
                : null;

        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "scoreboard");
        ObjectNode section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                null,
                "scoreboard",
                game.path("gameId").asText(),
                gameStatus,
                game.path("gameStatusText").asText(""),
                null,
                atomicBuilder.gamePanelTeamFromJson(game.path("awayTeam")),
                atomicBuilder.gamePanelTeamFromJson(game.path("homeTeam")),
                clock,
                null,
                refreshPolicy,
                utils.buildCompositeLinescoreBindings(),
                utils.gamePanelSurface());

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

    private ObjectNode buildStatLineSectionFromLive(JsonNode game, String gameId, String contentSourceId) {
        ArrayNode stats = objectMapper.createArrayNode();

        List<ObjectNode> homePerformers = getTopPerformersFromTeam(game.path("homeTeam"), 3);
        List<ObjectNode> awayPerformers = getTopPerformersFromTeam(game.path("awayTeam"), 3);

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

    private ObjectNode buildRowSectionFromLive(JsonNode game, String gameId, String contentSourceId) {
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
    private ObjectNode buildVideoPlayerSection(String gameId, JsonNode game, String contentSourceId) {
        int gameStatus = game.path("gameStatus").asInt(1);

        ObjectNode section = objectMapper.createObjectNode();
        section.put("id", SectionIdDeriver.derive(contentSourceId, "VideoPlayer", "video-player"));
        section.put("contentSourceId", contentSourceId);
        section.put("type", "VideoPlayer");
        section.put("analyticsId", "game_detail_video_player");
        section.set("refreshPolicy", objectMapper.createObjectNode().put("type", "static"));
        section.set("surface", utils.videoPlayerSurface());

        ObjectNode root = atomicBuilder.container("column", "center", "center");
        root.put("widthMode", "fill");
        root.put("height", 440);
        root.put("background", "#000000");
        ArrayNode rootChildren = objectMapper.createArrayNode();
        rootChildren.add(atomicBuilder.text("▶", "displayLarge", "bold", "#FFFFFF", null));
        rootChildren.add(atomicBuilder.spacer(LayoutTokens.SPACING_MD));
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
                "sdui:warning",
                "Viewing Time Limited",
                "Your couch rights viewing window is active. You have limited time remaining on this stream.",
                "Got It",
                "dismiss"
        ));

        overlays.set("couchRightsExpired", buildOverlaySection(
                contentSourceId, "couch-rights-expired",
                "sdui:warning",
                "Viewing Time Expired",
                "Your couch rights viewing window has ended. Subscribe to League Pass for unlimited access.",
                "Subscribe Now",
                "nba://leaguepass"
        ));

        overlays.set("unentitled", buildOverlaySection(
                contentSourceId, "unentitled",
                "sdui:lock",
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
        root.set("padding", padHelper(24, 24, 32, 32));
        root.put("background", ColorTokens.SURFACE_CANVAS);
        root.put("cornerRadius", LayoutTokens.RADIUS_LG);

        ArrayNode children = objectMapper.createArrayNode();

        ObjectNode iconEl = objectMapper.createObjectNode();
        iconEl.put("type", "Text");
        iconEl.put("content", icon);
        iconEl.put("variant", "headlineLarge");
        iconEl.put("textAlign", "center");
        children.add(iconEl);

        ObjectNode spacer1 = objectMapper.createObjectNode();
        spacer1.put("type", "Spacer");
        spacer1.put("height", LayoutTokens.SPACING_LG);
        children.add(spacer1);

        ObjectNode titleEl = objectMapper.createObjectNode();
        titleEl.put("type", "Text");
        titleEl.put("content", title);
        titleEl.put("variant", "headlineMedium");
        titleEl.put("weight", "bold");
        titleEl.put("color", ColorTokens.TEXT_PRIMARY);
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
        messageEl.put("color", ColorTokens.TEXT_SECONDARY);
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

    private ObjectNode buildGameDetailTabGroupFromLive(JsonNode game, String gameId, String contentSourceId) {
        JsonNode homeTeam = game.path("homeTeam");
        JsonNode awayTeam = game.path("awayTeam");
        if (homeTeam.isMissingNode() || awayTeam.isMissingNode()) return null;

        String homeTricode = homeTeam.path("teamTricode").asText("HOME");
        String awayTricode = awayTeam.path("teamTricode").asText("AWAY");
        int gameStatus = game.path("gameStatus").asInt(1);

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
        section.set("surface", utils.stripSurfaceWithoutBackground());

        ObjectNode layoutHints = objectMapper.createObjectNode();
        layoutHints.put("marginTop", 0);
        section.set("layoutHints", layoutHints);

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
        scroll.put("gap", 12);
        scroll.put("showIndicators", false);
        ArrayNode scrollChildren = objectMapper.createArrayNode();

        for (String[] hl : highlights) {
            ObjectNode card = objectMapper.createObjectNode();
            card.put("type", "Container");
            card.put("direction", "column");
            card.put("id", hl[0]);
            card.put("cornerRadius", 8);
            card.put("background", ColorTokens.SURFACE_CANVAS);

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
            img.put("cornerRadius", 8);
            AccessibilityHelper.addImage(objectMapper, img, hl[0]);

            ObjectNode durationBadgeEl = objectMapper.createObjectNode();
            durationBadgeEl.put("type", "Container");
            durationBadgeEl.put("direction", "row");
            durationBadgeEl.put("cornerRadius", 4);
            durationBadgeEl.put("background", "#000000B3");
            durationBadgeEl.put("opacity", 0.85);
            ObjectNode dbPad = objectMapper.createObjectNode();
            dbPad.put("start", 4); dbPad.put("end", 4); dbPad.put("top", 2); dbPad.put("bottom", 2);
            durationBadgeEl.set("padding", dbPad);
            ArrayNode dbChildren = objectMapper.createArrayNode();
            ObjectNode dbText = objectMapper.createObjectNode();
            dbText.put("type", "Text");
            dbText.put("content", hl[3]);
            dbText.put("variant", "labelSmall");
            dbText.put("color", ColorTokens.TEXT_INVERSE);
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
            title.put("color", ColorTokens.TEXT_PRIMARY);
            title.put("maxLines", 2);
            ObjectNode titlePad = objectMapper.createObjectNode();
            titlePad.put("start", 8); titlePad.put("end", 8); titlePad.put("top", 0); titlePad.put("bottom", 4);
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
        rootPad.put("start", 0); rootPad.put("end", 0); rootPad.put("top", 8); rootPad.put("bottom", 8);
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
            if (id.endsWith("::content-rail")) contentRailIndex = i;
            if (id.endsWith("::game-detail-tabs")) tabGroupIndex = i;
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
            if (!id.endsWith("::promo-banner") && !id.endsWith("::player-stats")) {
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
        ObjectNode extraHeader = atomicBuilder.buildSectionHeader(
                "trending-videos-header", "Trending Videos", null, null, null);
        extraHeader.set("surface", utils.sectionHeaderSurface());
        ObjectNode extraRail = atomicBuilder.buildContentRail("trending-videos",
                "trending_videos_rail", null, trendingCards);
        extraRail.set("surface", utils.railSurface());

        ArrayNode updated = objectMapper.createArrayNode();
        for (JsonNode section : sections) {
            updated.add(section);
            if (section.path("id").asText().endsWith("::content-rail")) {
                updated.add(extraHeader);
                updated.add(extraRail);
            }
        }

        response.set("sections", updated);
        log.debug("Applied variant D: added Trending Videos rail, {} sections total", updated.size());
    }
}
