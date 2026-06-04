package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.integration.model.scoreboard.Game;
import com.nba.sdui.integration.model.scoreboard.ScoreboardResponse;
import com.nba.sdui.integration.model.scoreboard.ScoreboardTeam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for game data endpoints.
 * 
 * Provides simplified game data for client consumption.
 */
@RestController
@RequestMapping("/v1/api/games")
public class GamesController {

    private static final Logger log = LoggerFactory.getLogger(GamesController.class);

    private final ScoreboardPort scoreboardPort;
    private final ObjectMapper objectMapper;

    public GamesController(ScoreboardPort scoreboardPort, ObjectMapper objectMapper) {
        this.scoreboardPort = scoreboardPort;
        this.objectMapper = objectMapper;
    }

    /**
     * Get today's games in a simplified format for dropdown selection.
     * 
     * Only returns games that are in progress (gameStatus=2) or completed (gameStatus=3).
     * Games in pregame state (gameStatus=1) are excluded as they don't have boxscore data.
     * 
     * Returns array of: { gameId, label, status }
     * where label is "{awayTeam} @ {homeTeam}" format
     */
    @GetMapping(value = "/today", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getTodaysGames() {
        log.info("Fetching today's games for selection");

        try {
            ScoreboardResponse scoreboard = scoreboardPort.getScoreboard();

            if (scoreboard == null || scoreboard.getGames().isEmpty()) {
                log.warn("No scoreboard data available");
                return ResponseEntity.ok(objectMapper.createArrayNode());
            }

            ArrayNode result = objectMapper.createArrayNode();

            for (Game game : scoreboard.getGames()) {
                // Only include in-progress (2) or completed (3) games
                int gameStatus = game.getGameStatus();
                if (gameStatus < 2) {
                    continue;
                }

                String gameId = game.getGameId() != null ? game.getGameId() : "";
                ScoreboardTeam away = game.getAwayTeam();
                ScoreboardTeam home = game.getHomeTeam();
                String awayTricode = away != null && away.getTeamTricode() != null ? away.getTeamTricode() : "";
                String homeTricode = home != null && home.getTeamTricode() != null ? home.getTeamTricode() : "";
                String status = game.getGameStatusText() != null ? game.getGameStatusText() : "";

                ObjectNode item = objectMapper.createObjectNode();
                item.put("gameId", gameId);
                item.put("label", awayTricode + " @ " + homeTricode);
                item.put("status", status);
                result.add(item);
            }

            log.info("Returning {} games (filtered to in-progress/completed)", result.size());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error fetching today's games", e);
            return ResponseEntity.ok(objectMapper.createArrayNode());
        }
    }
}
