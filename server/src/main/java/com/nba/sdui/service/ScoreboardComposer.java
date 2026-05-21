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

/**
 * Composes the Scoreboard SDUI screen from live NBA scoreboard data with
 * example-file fallback, and applies server-driven variant transformations
 * (E — promo banner, F — promo + content rail).
 */
@Component
public class ScoreboardComposer {

    private static final Logger log = LoggerFactory.getLogger(ScoreboardComposer.class);
    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ScoreboardComposer(ObjectMapper objectMapper,
                              StatsApiClient statsApiClient,
                              SduiUtils utils) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper);
    }

    // ── Public entry point ─────────────────────────────────────────────

    /**
     * Compose a Scoreboard SDUI screen response.
     */
    public JsonNode composeScoreboard(String variant, String clientSchemaVersion,
                                      String traceId, String locale) throws IOException {
        log.info("Composing scoreboard: variant={}, locale={}", variant, locale);

        ObjectNode response = composeScoreboardFromLiveData();
        if (response == null) {
            log.warn("Live scoreboard unavailable or no in-progress games, falling back to static scoreboard example");
            response = utils.loadExampleByFilename("scoreboard-live.json");
        }

        if (response == null) {
            response = objectMapper.createObjectNode();
            response.put("id", "scoreboard-live");
            response.set("sections", objectMapper.createArrayNode());
        }

        response.put("traceId", traceId);
        response.put("schemaVersion", schemaVersion);
        utils.applyTabDestinationNavigation(response, "scoreboard");

        if (variant != null) {
            switch (variant.toUpperCase()) {
                case "E" -> applyScoreboardVariantPromo(response);
                case "F" -> applyScoreboardVariantPromoRail(response);
                default -> log.debug("Scoreboard using default variant (no transformation)");
            }
        }

        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        return response;
    }

    // ── Live-data composition ──────────────────────────────────────────

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

    // ── Section builders ───────────────────────────────────────────────

    private ObjectNode buildScoreboardRowSection(JsonNode game, String gameId) {
        int gameStatus = game.path("gameStatus").asInt(1);
        boolean live = gameStatus == 2;

        ObjectNode refreshPolicy;
        ObjectNode bindings = null;
        AtomicCompositeBuilder.GameClockSnapshot clock = null;
        if (live) {
            refreshPolicy = objectMapper.createObjectNode();
            refreshPolicy.put("type", "sse");
            refreshPolicy.put("channel", gameId + ":linescore");
            refreshPolicy.put("pauseWhenOffScreen", false);
            bindings = utils.buildCompositeLinescoreBindings();
            clock = new AtomicCompositeBuilder.GameClockSnapshot(
                    parseGameClockSeconds(game.path("gameClock").asText("")),
                    java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                    AtomicCompositeBuilder.DEMO_INITIAL_CLOCK_RUNNING);
        } else {
            refreshPolicy = objectMapper.createObjectNode().put("type", "static");
        }

        String contentSourceId = "stats-api:scoreboard-game-" + gameId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");

        ObjectNode section = atomicBuilder.buildGamePanelComposite(
                sectionId,
                "scoreboard_row_" + gameId,
                "scoreboard",
                gameId,
                gameStatus,
                game.path("gameStatusText").asText(""),
                null,
                atomicBuilder.gamePanelTeamFromJson(game.path("awayTeam")),
                atomicBuilder.gamePanelTeamFromJson(game.path("homeTeam")),
                clock,
                "nba://game/" + gameId,
                refreshPolicy,
                bindings,
                utils.gamePanelSurface());
        section.put("contentSourceId", contentSourceId);
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

    private ObjectNode mapLeader(JsonNode leader) {
        ObjectNode mapped = objectMapper.createObjectNode();
        mapped.put("name", leader.path("name").asText(""));
        mapped.put("points", leader.path("points").asInt(0));
        mapped.put("rebounds", leader.path("rebounds").asInt(0));
        mapped.put("assists", leader.path("assists").asInt(0));
        return mapped;
    }

    // ── Variant transformations ────────────────────────────────────────

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
                addScoreboardContentRail(updated);
            }
        }

        response.set("sections", updated);
        log.debug("Applied scoreboard variant PromoRail: promo at 0, rail after game 2 ({}), {} sections total",
                gameCount > 2 ? "inserted" : "skipped", updated.size());
    }

    private ObjectNode buildScoreboardPromoBanner() {
        String contentSourceId = "ads:gam-scoreboard-promo";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        ObjectNode section = atomicBuilder.buildPromoBanner(
                sectionId, "scoreboard_promo_banner",
                "NBA League Pass", null,
                "Watch every out-of-market game live or on demand.",
                FALLBACK_THUMB, "Learn More", "nba://leaguepass");
        section.put("contentSourceId", contentSourceId);
        section.set("surface", utils.subscribeSurface(
                "#0C1B3A",
                ColorTokens.BRAND_NBA,
                20));
        return section;
    }

    private void addScoreboardContentRail(ArrayNode sections) {
        String[][] cards = {
                {"league-1", "Top 10 Plays of the Night",
                        "Last night's best moments", FALLBACK_THUMB,
                        "video", null, "nba://video/top10-plays"},
                {"league-2", "Standings Update",
                        "Current playoff picture", FALLBACK_THUMB,
                        "article", null, "nba://standings"}
        };
        String headerContentSourceId = "feed:scoreboard";
        String headerSectionId = SectionIdDeriver.derive(headerContentSourceId, "AtomicComposite", "content-rail-header");
        ObjectNode header = atomicBuilder.buildSectionHeader(
                headerSectionId, "Around the League", null, null, null);
        header.put("contentSourceId", headerContentSourceId);
        header.set("surface", utils.sectionHeaderSurface());
        sections.add(header);

        String railContentSourceId = "feed:scoreboard";
        String railSectionId = SectionIdDeriver.derive(railContentSourceId, "AtomicComposite", "content-rail");
        ObjectNode rail = atomicBuilder.buildContentRail(railSectionId,
                "scoreboard_content_rail", null, cards);
        rail.put("contentSourceId", railContentSourceId);
        rail.set("surface", utils.railSurface());
        sections.add(rail);
    }
}
