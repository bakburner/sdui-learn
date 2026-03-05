package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.service.SduiCompositionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
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

    // ── Demos Kitchen-Sink Screen ─────────────────────────────────────

    /**
     * Get SDUI screen response for the demos kitchen-sink page.
     * Showcases all 10 semantic section types with static mock data.
     */
    @GetMapping(value = "/sdui/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getDemos(
            @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion,
            HttpServletResponse response) {

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        log.info("SDUI demos request: schemaVersion={}", schemaVersion);

        try {
            JsonNode screenResponse = compositionService.composeDemos(traceId);
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Schema-Version", "1.0");
            return ResponseEntity.ok(screenResponse);
        } catch (Exception e) {
            log.error("Error composing demos screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    // ── Boxscore Screen ────────────────────────────────────────────────

    /**
     * Get SDUI screen response for the boxscore view.
     *
     * <p>Returns a {@code Screen} containing a {@code TabGroup} that wraps two
     * {@code BoxscoreTable} sections (away / home).  Screen-level state is
     * pre-populated with default sort column, direction, and active team tab.
     *
     * @param gameId Game ID (e.g. "0042300102")
     */
    @GetMapping(value = "/sdui/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getBoxscore(
            @PathVariable String gameId,
            @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion,
            HttpServletResponse response) {

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        log.info("SDUI boxscore request: gameId={}, schemaVersion={}", gameId, schemaVersion);

        try {
            JsonNode screenResponse = compositionService.composeBoxscore(gameId, traceId);

            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Schema-Version", "1.0");
            log.info("SDUI boxscore response composed successfully");
            return ResponseEntity.ok(screenResponse);
        } catch (Exception e) {
            log.error("Error composing SDUI boxscore response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    // ── Parameterized Refresh (Form submit support) ────────────────────

    /**
     * Generic SDUI screen refresh endpoint that accepts arbitrary query parameters.
     *
     * <p>Clients call this when a {@code Form} submit action fires with
     * {@code paramBindings} resolved from screen state.  The controller passes
     * all query parameters through to the composition service so the server
     * can return an updated screen reflecting the submitted values.
     *
     * <p>Example:
     * <pre>
     *   GET /sdui/refresh/stats-leaders?season=2025-26&seasonType=Regular+Season
     * </pre>
     *
     * @param screenId  Logical screen identifier (e.g. "stats-leaders", "roster")
     * @param request   The HTTP request — all query params are extracted and forwarded
     */
    @GetMapping(value = "/sdui/refresh/{screenId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> refreshScreen(
            @PathVariable String screenId,
            @RequestParam Map<String, String> allParams,
            @RequestHeader(value = "X-Schema-Version", defaultValue = "1.0") String schemaVersion,
            HttpServletRequest request,
            HttpServletResponse response) {

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        log.info("SDUI parameterized refresh: screenId={}, params={}", screenId, allParams);

        try {
            // Build a fresh screen incorporating the submitted param values.
            // The params map flows into screen-level state so the Form reflects
            // whatever the user submitted.
            ObjectNode screenResponse = objectMapper.createObjectNode();
            screenResponse.put("id", screenId);
            screenResponse.put("traceId", traceId);
            screenResponse.put("schemaVersion", "1.0");

            // Echo submitted params back as screen state so form fields retain
            // their selected values after refresh.
            ObjectNode state = objectMapper.createObjectNode();
            allParams.forEach(state::put);
            screenResponse.set("state", state);

            // Placeholder sections — in a real implementation this would
            // delegate to a screen-specific composition method that uses the
            // params to fetch upstream data and compose sections accordingly.
            screenResponse.set("sections", objectMapper.createArrayNode());

            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Schema-Version", "1.0");
            log.info("SDUI refresh response composed: screenId={}", screenId);
            return ResponseEntity.ok(screenResponse);
        } catch (Exception e) {
            log.error("Error composing SDUI refresh response for screenId={}", screenId, e);
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
