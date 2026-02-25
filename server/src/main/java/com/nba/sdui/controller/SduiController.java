package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.sdui.service.SduiCompositionService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for SDUI endpoints.
 * 
 * Endpoints:
 * - GET /sdui/scoreboard - Returns SDUI screen response for Today's Games
 * - GET /sdui/game-detail/{gameId} - Returns SDUI screen response for game detail
 * - GET /stats/{gameId} - Returns player stats for polling section
 */
@RestController
public class SduiController {

    private static final Logger log = LoggerFactory.getLogger(SduiController.class);
    
    private final SduiCompositionService compositionService;
    private final ObjectMapper objectMapper;

    public SduiController(SduiCompositionService compositionService, ObjectMapper objectMapper) {
        this.compositionService = compositionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get SDUI screen response for game detail page.
     *
     * @param gameId      The game ID (e.g., "0042300102")
     * @param gameState   Game state hint: "pre", "live", or "final" (default: "pre")
     * @param schemaVersion Client's supported schema version (from header)
     * @param response    HTTP response (for adding headers)
     * @return SDUI Screen JSON response
     */
    @GetMapping(value = "/sdui/game-detail/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getGameDetail(
            @PathVariable String gameId,
            @RequestParam(defaultValue = "pre") String gameState,
            @RequestParam(defaultValue = "A") String variant,
            @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion,
            HttpServletResponse response) {
        
        // Generate trace ID
        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        
        log.info("SDUI request: gameId={}, gameState={}, variant={}, schemaVersion={}",
                gameId, gameState, variant, schemaVersion);
        
        try {
            JsonNode screenResponse = compositionService.composeGameDetail(
                    gameId, gameState, variant, schemaVersion, traceId);
            
            // Add response headers
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Schema-Version", "1.0");
            
            log.info("SDUI response composed successfully");
            return ResponseEntity.ok(screenResponse);
            
        } catch (Exception e) {
            log.error("Error composing SDUI response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    /**
     * Get SDUI screen response for today's games scoreboard.
     */
    @GetMapping(value = "/sdui/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getScoreboard(
            @RequestParam(defaultValue = "A") String variant,
            @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion,
            HttpServletResponse response) {

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        log.info("SDUI scoreboard request: variant={}, schemaVersion={}",
                variant, schemaVersion);

        try {
            JsonNode screenResponse = compositionService.composeScoreboard(
                    variant, schemaVersion, traceId);

            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Schema-Version", "1.0");

            log.info("SDUI scoreboard response composed successfully");
            return ResponseEntity.ok(screenResponse);
        } catch (Exception e) {
            log.error("Error composing SDUI scoreboard response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    /**
     * Get player stats for polling section.
     *
     * @param gameId The game ID
     * @return Stats response suitable for StatLine section
     */
    @GetMapping(value = "/stats/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getStats(
            @PathVariable String gameId,
            @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion) {
        
        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        
        log.info("Stats request: gameId={}", gameId);
        
        try {
            JsonNode statsResponse = compositionService.getPlayerStats(gameId);
            log.info("Stats response composed successfully");
            return ResponseEntity.ok(statsResponse);
            
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
