package com.nba.sdui.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.metrics.SduiMetrics;
import com.nba.sdui.request.SduiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.composer.BoxscoreComposer;
import com.nba.sdui.domain.composer.CalendarComposer;
import com.nba.sdui.domain.composer.DemoScreenComposer;
import com.nba.sdui.domain.composer.ForYouComposer;
import com.nba.sdui.domain.composer.GameDetailComposer;
import com.nba.sdui.domain.composer.HomeComposer;
import com.nba.sdui.domain.composer.LiveComposer;
import com.nba.sdui.domain.composer.ScheduleComposer;
import com.nba.sdui.domain.composer.ScoreboardComposer;
import com.nba.sdui.domain.composer.WatchComposer;
import com.nba.sdui.domain.port.StatsPort;
import com.nba.sdui.integration.model.boxscore.BoxscoreGame;
import com.nba.sdui.integration.model.boxscore.BoxscorePlayer;
import com.nba.sdui.integration.model.boxscore.BoxscoreResponse;
import com.nba.sdui.integration.model.boxscore.BoxscoreStatistics;
import com.nba.sdui.integration.model.boxscore.BoxscoreTeam;
import com.nba.sdui.remote.StatsApiClient;

/**
 * SDUI Composition Service — thin façade.
 *
 * <p>Accepts {@link SduiRequestContext} from the controller and extracts the
 * parameters each composer needs. Composers retain their existing signatures
 * to minimise churn.
 *
 * <p>Delegates screen assembly to purpose-built composers while keeping the
 * stats-polling helpers (getPlayerStats / createMockStats) co-located with
 * the StatsApiClient dependency they need.
 */
@Service
public class SduiCompositionService {

    private static final Logger log = LoggerFactory.getLogger(SduiCompositionService.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final StatsPort statsPort;
    private final SduiUtils utils;
    private final SduiMetrics metrics;
    private final GameDetailComposer gameDetailComposer;
    private final ScoreboardComposer scoreboardComposer;
    private final BoxscoreComposer boxscoreComposer;
    private final DemoScreenComposer demoScreenComposer;
    private final ForYouComposer forYouComposer;
    private final WatchComposer watchComposer;
    private final LiveComposer liveComposer;
    private final ScheduleComposer scheduleComposer;
    private final HomeComposer homeComposer;
    private final CalendarComposer calendarComposer;

    /** Default experiment ID used for game-detail variant resolution. */
    private static final String GAME_DETAIL_EXPERIMENT = "game_detail_variant";
    /** Default experiment ID used for scoreboard variant resolution. */
    private static final String SCOREBOARD_EXPERIMENT = "scoreboard_variant";

    public SduiCompositionService(ObjectMapper objectMapper,
                                   StatsApiClient statsApiClient,
                                   StatsPort statsPort,
                                   SduiUtils utils,
                                   SduiMetrics metrics,
                                   GameDetailComposer gameDetailComposer,
                                   ScoreboardComposer scoreboardComposer,
                                   BoxscoreComposer boxscoreComposer,
                                   DemoScreenComposer demoScreenComposer,
                                   ForYouComposer forYouComposer,
                                   WatchComposer watchComposer,
                                   LiveComposer liveComposer,
                                   ScheduleComposer scheduleComposer,
                                   HomeComposer homeComposer,
                                   CalendarComposer calendarComposer) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.statsPort = statsPort;
        this.utils = utils;
        this.metrics = metrics;
        this.gameDetailComposer = gameDetailComposer;
        this.scoreboardComposer = scoreboardComposer;
        this.boxscoreComposer = boxscoreComposer;
        this.demoScreenComposer = demoScreenComposer;
        this.forYouComposer = forYouComposer;
        this.watchComposer = watchComposer;
        this.liveComposer = liveComposer;
        this.scheduleComposer = scheduleComposer;
        this.homeComposer = homeComposer;
        this.calendarComposer = calendarComposer;
    }

    // ── Screen delegation ──────────────────────────────────────────────

    public GameDetailComposer.GameDetailResult composeGameDetail(String gameId, SduiRequestContext ctx) throws IOException {
        long start = System.nanoTime();
        try {
            String variant = ctx.resolveVariant(GAME_DETAIL_EXPERIMENT, "A");
            return gameDetailComposer.composeGameDetail(gameId, variant,
                    ctx.getSchemaVersion(), ctx.getTraceId(), ctx.getLocale());
        } finally {
            metrics.recordComposition("game-" + gameId, "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeScoreboard(SduiRequestContext ctx) throws IOException {
        long start = System.nanoTime();
        try {
            String variant = ctx.resolveVariant(SCOREBOARD_EXPERIMENT, "A");
            return objectMapper.valueToTree(scoreboardComposer.composeScoreboard(variant,
                    ctx.getSchemaVersion(), ctx.getTraceId(), ctx.getLocale()));
        } finally {
            metrics.recordComposition("scoreboard", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeBoxscore(String gameId, SduiRequestContext ctx) throws IOException {
        long start = System.nanoTime();
        try {
            return boxscoreComposer.composeBoxscore(gameId, ctx.getTraceId(), ctx.getLocale());
        } finally {
            metrics.recordComposition("boxscore-" + gameId, "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeDemos(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            String deviceClass = ctx.getPlatform() != null ? ctx.getPlatform().getDeviceClass() : "phone";
            return demoScreenComposer.composeDemos(ctx.getTraceId(), deviceClass, ctx.getLocale());
        } finally {
            metrics.recordComposition("demos", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeLeaders(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            String deviceClass = ctx.getPlatform() != null ? ctx.getPlatform().getDeviceClass() : "phone";
            return demoScreenComposer.composeLeaders(ctx.getTraceId(), deviceClass, ctx.getLocale());
        } finally {
            metrics.recordComposition("leaders", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeForYou(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            return objectMapper.valueToTree(forYouComposer.composeForYou(ctx.getTraceId(), ctx.getLocale()));
        } finally {
            metrics.recordComposition("for-you", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeWatch(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            return objectMapper.valueToTree(watchComposer.composeWatch(ctx.getTraceId(), ctx.getLocale()));
        } finally {
            metrics.recordComposition("watch", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeLive(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            return objectMapper.valueToTree(
                    liveComposer.composeLive(ctx.getTraceId(), ctx.getLocale()));
        } finally {
            metrics.recordComposition("live", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeSchedule(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            return objectMapper.valueToTree(
                    scheduleComposer.composeSchedule(ctx.getTraceId(), ctx.getLocale()));
        } finally {
            metrics.recordComposition("schedule", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeHome(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            return objectMapper.valueToTree(homeComposer.composeHome(ctx.getTraceId(), ctx.getLocale()));
        } finally {
            metrics.recordComposition("home", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeCalendar(SduiRequestContext ctx) {
        long start = System.nanoTime();
        try {
            return calendarComposer.composeCalendar(ctx.getTraceId(), ctx.getLocale());
        } finally {
            metrics.recordComposition("calendar", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    public JsonNode composeCalendar(SduiRequestContext ctx, String selectedDateParam) {
        long start = System.nanoTime();
        try {
            return calendarComposer.composeCalendar(ctx.getTraceId(), ctx.getLocale(), selectedDateParam);
        } finally {
            metrics.recordComposition("calendar", "screen", false, Duration.ofNanos(System.nanoTime() - start));
        }
    }

    // ── Stats-polling helpers (kept here — they need StatsApiClient) ──

    public JsonNode getPlayerStats(String gameId) throws IOException {
        try {
            JsonNode boxscore = statsApiClient.getBoxscore(gameId);
            if (boxscore != null) {
                return transformBoxscoreToStatLines(boxscore);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch boxscore from API, using mock data: {}", e.getMessage());
        }
        return createMockStats(gameId);
    }

    // ── Private helpers ────────────────────────────────────────────────

    private JsonNode transformBoxscoreToStatLines(JsonNode boxscore) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("title", "Top Performers");
        result.put("layout", "vertical");

        ArrayNode stats = objectMapper.createArrayNode();
        extractTopPerformers(boxscore.path("homeTeam"), stats);
        extractTopPerformers(boxscore.path("awayTeam"), stats);

        result.set("stats", stats);
        return result;
    }

    private void extractTopPerformers(JsonNode team, ArrayNode stats) {
        if (!team.has("players")) return;

        String teamTricode = team.path("teamTricode").asText();
        ArrayNode players = (ArrayNode) team.get("players");

        players.forEach(player -> {
            if (stats.size() >= 8) return;

            int points = player.path("statistics").path("points").asInt();
            if (points >= 15) {
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
        result.put("layout", "vertical");

        ArrayNode stats = objectMapper.createArrayNode();
        stats.add(utils.createStatLine(1627759, "Jaylen Brown", "BOS", "PTS", "31"));
        stats.add(utils.createStatLine(1628369, "Jayson Tatum", "BOS", "PTS", "28"));
        stats.add(utils.createStatLine(202710, "Jimmy Butler", "MIA", "PTS", "26"));
        stats.add(utils.createStatLine(1629216, "Bam Adebayo", "MIA", "REB", "11"));

        result.set("stats", stats);
        return result;
    }
}

