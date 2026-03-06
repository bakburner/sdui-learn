package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * SDUI Composition Service — thin façade.
 *
 * Delegates screen assembly to purpose-built composers while keeping the
 * stats-polling helpers (getPlayerStats / createMockStats) co-located with
 * the StatsApiClient dependency they need.
 */
@Service
public class SduiCompositionService {

    private static final Logger log = LoggerFactory.getLogger(SduiCompositionService.class);

    private final ObjectMapper objectMapper;
    private final StatsApiClient statsApiClient;
    private final SduiUtils utils;
    private final GameDetailComposer gameDetailComposer;
    private final ScoreboardComposer scoreboardComposer;
    private final BoxscoreComposer boxscoreComposer;
    private final DemoScreenComposer demoScreenComposer;

    public SduiCompositionService(ObjectMapper objectMapper,
                                   StatsApiClient statsApiClient,
                                   SduiUtils utils,
                                   GameDetailComposer gameDetailComposer,
                                   ScoreboardComposer scoreboardComposer,
                                   BoxscoreComposer boxscoreComposer,
                                   DemoScreenComposer demoScreenComposer) {
        this.objectMapper = objectMapper;
        this.statsApiClient = statsApiClient;
        this.utils = utils;
        this.gameDetailComposer = gameDetailComposer;
        this.scoreboardComposer = scoreboardComposer;
        this.boxscoreComposer = boxscoreComposer;
        this.demoScreenComposer = demoScreenComposer;
    }

    // ── Screen delegation ──────────────────────────────────────────────

    public JsonNode composeGameDetail(String gameId, String gameState,
                                      String variant, String clientSchemaVersion,
                                      String traceId) throws IOException {
        return gameDetailComposer.composeGameDetail(gameId, gameState, variant,
                clientSchemaVersion, traceId);
    }

    public JsonNode composeScoreboard(String variant, String clientSchemaVersion,
                                      String traceId) throws IOException {
        return scoreboardComposer.composeScoreboard(variant, clientSchemaVersion, traceId);
    }

    public JsonNode composeBoxscore(String gameId, String traceId) throws IOException {
        return boxscoreComposer.composeBoxscore(gameId, traceId);
    }

    public JsonNode composeDemos(String traceId) {
        return demoScreenComposer.composeDemos(traceId);
    }

    public ObjectNode composeLeadersRefresh(String traceId, Map<String, String> params) {
        return demoScreenComposer.composeLeadersRefresh(traceId, params);
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

        ArrayNode stats = objectMapper.createArrayNode();
        stats.add(utils.createStatLine(1627759, "Jaylen Brown", "BOS", "PTS", "31"));
        stats.add(utils.createStatLine(1628369, "Jayson Tatum", "BOS", "PTS", "28"));
        stats.add(utils.createStatLine(202710, "Jimmy Butler", "MIA", "PTS", "26"));
        stats.add(utils.createStatLine(1629216, "Bam Adebayo", "MIA", "REB", "11"));

        result.set("stats", stats);
        return result;
    }
}

