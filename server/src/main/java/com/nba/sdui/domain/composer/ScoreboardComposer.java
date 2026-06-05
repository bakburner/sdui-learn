package com.nba.sdui.domain.composer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.nba.sdui.domain.AtomicCompositeBuilder;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.tokens.Tokens;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.models.generated.Section;
import com.nba.sdui.models.generated.SectionSurface;

/**
 * Composes the Scoreboard SDUI screen from live NBA scoreboard data, and
 * applies server-driven variant transformations (E — promo banner,
 * F — promo + content rail). When live data is unavailable, the screen is
 * composed with a single {@code ErrorState} section (AGENTS.md §8.0) — no
 * invented fallback content.
 */
@Component
public class ScoreboardComposer {

    private static final Logger log = LoggerFactory.getLogger(ScoreboardComposer.class);
    private static final String FALLBACK_THUMB =
            "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png";

    private final ObjectMapper objectMapper;
    private final ScoreboardPort scoreboardPort;
    private final SduiUtils utils;
    private final SectionSurfaces surfaces;
    private final Tokens tokens;
    private final AtomicCompositeBuilder atomicBuilder;

    @Value("${sdui.schema.version:1.0}")
    private String schemaVersion;

    public ScoreboardComposer(ObjectMapper objectMapper,
                              ScoreboardPort scoreboardPort,
                              SduiUtils utils,
                              SectionSurfaces surfaces,
                              Tokens tokens) {
        this.objectMapper = objectMapper;
        this.scoreboardPort = scoreboardPort;
        this.utils = utils;
        this.surfaces = surfaces;
        this.tokens = tokens;
        this.atomicBuilder = new AtomicCompositeBuilder(objectMapper, tokens);
    }

    // ── Public entry point ─────────────────────────────────────────────

    /**
     * Compose a Scoreboard SDUI screen response.
     */
    public Screen composeScoreboard(String variant, String clientSchemaVersion,
                                    String traceId, String locale) throws IOException {
        log.info("Composing scoreboard: variant={}, locale={}", variant, locale);

        Composition composition = composeScoreboardFromLiveData();
        if (composition == null) {
            log.warn("Live scoreboard unavailable — composing ErrorState screen");
            composition = buildErrorScreen();
        }

        ObjectNode response = composition.shell;
        List<Section> sections = composition.sections;

        response.put("schemaVersion", schemaVersion);
        utils.applyTabDestinationNavigation(response, "scoreboard");

        if (variant != null) {
            switch (variant.toUpperCase()) {
                case "E" -> applyScoreboardVariantPromo(sections);
                case "F" -> applyScoreboardVariantPromoRail(sections);
                default -> log.debug("Scoreboard using default variant (no transformation)");
            }
        }

        response.set("sections", objectMapper.valueToTree(sections));
        utils.ensureScreenContentInsets(response);
        utils.stampStringTableOnSections(response, locale);
        try {
            return objectMapper.treeToValue(response, Screen.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to bind composed Scoreboard screen to Screen.class", e);
        }
    }

    /**
     * Internal carrier for the in-progress composition: the screen-level shell
     * (id, defaultRefreshPolicy, analyticsId, etc.) plus the typed section list
     * the variant transforms operate on. The shell is bound to {@link Screen}
     * at the public boundary; sections accumulate as typed objects throughout
     * composition.
     */
    private record Composition(ObjectNode shell, List<Section> sections) {}

    /**
     * Build a minimal Scoreboard screen carrying a single {@code ErrorState}
     * section. Used when the live scoreboard API is unavailable. Per
     * AGENTS.md §8.0 the server surfaces an {@code ErrorState} rather than
     * inventing fallback game cards.
     */
    private Composition buildErrorScreen() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "scoreboard-live");
        ObjectNode defaultRefreshPolicy = objectMapper.createObjectNode();
        defaultRefreshPolicy.put("type", "static");
        response.set("defaultRefreshPolicy", defaultRefreshPolicy);

        List<Section> sections = new ArrayList<>();
        String contentSourceId = "stats-api:scoreboard";
        ObjectNode error = utils.buildErrorSection(
                SectionIdDeriver.derive(contentSourceId, "AtomicComposite", "error-no-scores"),
                "Scores unavailable",
                "We couldn't load today's scoreboard. Please try again later.",
                "wifi_off",
                "nba://scoreboard");
        error.put("contentSourceId", contentSourceId);
        sections.add(toSection(error));
        return new Composition(response, sections);
    }

    // ── Live-data composition ──────────────────────────────────────────

    private Composition composeScoreboardFromLiveData() {
        try {
            ScoreboardResponse scoreboard = scoreboardPort.getScoreboard();
            if (scoreboard == null || scoreboard.getGames().isEmpty()) {
                if (scoreboard != null) {
                    log.warn("Live scoreboard has no games for today");
                }
                return null;
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("id", "scoreboard-live");
            response.put("analyticsId", "scoreboard_live");

            List<Section> sections = new ArrayList<>();
            for (Game game : scoreboard.getGames()) {
                String gameId = game.getGameId();
                if (gameId == null || gameId.isBlank()) {
                    continue;
                }
                sections.add(buildScoreboardRowSection(game, gameId));
            }
            if (sections.isEmpty()) {
                log.warn("Live scoreboard has no games for today");
                return null;
            }
            return new Composition(response, sections);
        } catch (Exception e) {
            log.error("Failed to compose scoreboard from live data: {}", e.getMessage(), e);
            return null;
        }
    }

    // ── Section builders ───────────────────────────────────────────────

    private Section buildScoreboardRowSection(Game game, String gameId) {
        int gameStatus = game.getGameStatus();
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
                    parseGameClockSeconds(game.getGameClock()),
                    java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                    AtomicCompositeBuilder.INITIAL_CLOCK_RUNNING);
        } else {
            refreshPolicy = objectMapper.createObjectNode().put("type", "static");
        }

        String contentSourceId = "stats-api:scoreboard-game-" + gameId;
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");

        Section sectionNode = atomicBuilder.buildGamePanelComposite(
                sectionId,
                "scoreboard_row_" + gameId,
                "scoreboard",
                gameId,
                gameStatus,
                game.getGameStatusText() != null ? game.getGameStatusText() : "",
                null,
                atomicBuilder.gamePanelTeam(game.getAwayTeam()),
                atomicBuilder.gamePanelTeam(game.getHomeTeam()),
                clock,
                "nba://game/" + gameId,
                refreshPolicy,
                bindings,
                objectMapper.valueToTree(surfaces.gamePanelSurface()));
        sectionNode.setContentSourceId(contentSourceId);
        return sectionNode;
    }

    private Section toSection(ObjectNode node) {
        try {
            return objectMapper.treeToValue(node, Section.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to bind composed section to Section.class", e);
        }
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

    private void applyScoreboardVariantPromo(List<Section> sections) {
        sections.add(0, buildScoreboardPromoBanner());
        log.debug("Applied scoreboard variant Promo: inserted PromoBanner at index 0, {} sections total", sections.size());
    }

    private void applyScoreboardVariantPromoRail(List<Section> sections) {
        int gameCount = sections.size();

        List<Section> updated = new ArrayList<>();
        updated.add(buildScoreboardPromoBanner());

        for (int i = 0; i < sections.size(); i++) {
            updated.add(sections.get(i));
            if (i == 1 && gameCount > 2) {
                addScoreboardContentRail(updated);
            }
        }

        sections.clear();
        sections.addAll(updated);
        log.debug("Applied scoreboard variant PromoRail: promo at 0, rail after game 2 ({}), {} sections total",
                gameCount > 2 ? "inserted" : "skipped", sections.size());
    }

    private Section buildScoreboardPromoBanner() {
        String contentSourceId = "ads:gam-scoreboard-promo";
        String sectionId = SectionIdDeriver.derive(contentSourceId, "AtomicComposite");
        Section section = atomicBuilder.buildPromoBanner(
                sectionId, "scoreboard_promo_banner",
                "NBA League Pass", null,
                "Watch every out-of-market game live or on demand.",
                FALLBACK_THUMB, "Learn More", "nba://leaguepass");
        section.setContentSourceId(contentSourceId);
        section.setSurface(surfaces.subscribeSurface(
                "#0C1B3A",
                tokens.color("nba.label.accent.brand"),
                20));
        return section;
    }

    private void addScoreboardContentRail(List<Section> sections) {
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
        Section header = atomicBuilder.buildSectionHeader(
                headerSectionId, "Around the League", null, null, null);
        header.setContentSourceId(headerContentSourceId);
        header.setSurface(surfaces.sectionHeaderSurface());
        sections.add(header);

        String railContentSourceId = "feed:scoreboard";
        String railSectionId = SectionIdDeriver.derive(railContentSourceId, "AtomicComposite", "content-rail");
        Section rail = atomicBuilder.buildContentRail(railSectionId,
                "scoreboard_content_rail", null, cards);
        rail.setContentSourceId(railContentSourceId);
        rail.setSurface(surfaces.railSurface());
        sections.add(rail);
    }
}
