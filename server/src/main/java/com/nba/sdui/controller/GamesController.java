package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.service.StatsApiClient;
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
@RequestMapping("/api/games")
public class GamesController {

    private static final Logger log = LoggerFactory.getLogger(GamesController.class);
    
    private final StatsApiClient statsApiClient;
    private final ObjectMapper objectMapper;

    public GamesController(StatsApiClient statsApiClient, ObjectMapper objectMapper) {
        this.statsApiClient = statsApiClient;
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
            JsonNode scoreboard = statsApiClient.getScoreboard();
            
            if (scoreboard == null) {
                log.warn("No scoreboard data available");
                return ResponseEntity.ok(objectMapper.createArrayNode());
            }
            
            JsonNode games = scoreboard.path("scoreboard").path("games");
            if (games.isMissingNode() || !games.isArray()) {
                log.warn("No games found in scoreboard");
                return ResponseEntity.ok(objectMapper.createArrayNode());
            }
            
            ArrayNode result = objectMapper.createArrayNode();
            
            for (JsonNode game : games) {
                // Only include in-progress (2) or completed (3) games
                int gameStatus = game.path("gameStatus").asInt(0);
                if (gameStatus < 2) {
                    continue;
                }
                
                String gameId = game.path("gameId").asText();
                String awayTricode = game.path("awayTeam").path("teamTricode").asText();
                String homeTricode = game.path("homeTeam").path("teamTricode").asText();
                String status = game.path("gameStatusText").asText();
                
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
