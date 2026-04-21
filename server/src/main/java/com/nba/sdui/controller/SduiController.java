package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.service.SduiCompositionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for SDUI endpoints.
 *
 * <p>All composition endpoints accept {@link SduiRequestContext}, resolved from
 * bracket-notation query params (GET) or JSON body (POST) by
 * {@link com.nba.sdui.request.BracketParamResolver}.
 *
 * <p>Headers retained: {@code Authorization} (auth), {@code X-Trace-Id} (observability).
 * All other context travels as query parameters per plan-request-transport.md D3.
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

    // ── Game Detail ────────────────────────────────────────────────────

    @GetMapping(value = {"/sdui/game-detail/{gameId}", "/sdui/game/{gameId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getGameDetail(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());

        log.info("SDUI request: gameId={}, gameState={}, locale={}, schemaVersion={}, platform={}",
                gameId, ctx.getGameState(), ctx.getLocale(), ctx.getSchemaVersion(), ctx.getPlatformName());

        try {
            JsonNode screenResponse = compositionService.composeGameDetail(gameId, ctx);

            setResponseHeaders(response, ctx);
            log.info("SDUI response composed successfully");

            // D7: gameState determines cacheability
            CacheControl cache = resolveCacheControl(ctx.getGameState(), "pre");
            return ResponseEntity.ok().cacheControl(cache).body(screenResponse);

        } catch (Exception e) {
            log.error("Error composing SDUI response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = {"/sdui/game-detail/{gameId}", "/sdui/game/{gameId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postGameDetail(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getGameDetail(gameId, ctx, response);
    }

    // ── Scoreboard ─────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getScoreboard(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());

        log.info("SDUI scoreboard request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeScoreboard(ctx);
            setResponseHeaders(response, ctx);
            log.info("SDUI scoreboard response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing SDUI scoreboard response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postScoreboard(SduiRequestContext ctx, HttpServletResponse response) {
        return getScoreboard(ctx, response);
    }

    // ── Stats (polling endpoint — not a composition endpoint) ──────────

    @GetMapping(value = "/stats/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getStats(
            @PathVariable String gameId) {

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        log.info("Stats request: gameId={}", gameId);

        try {
            JsonNode statsResponse = compositionService.getPlayerStats(gameId);
            log.info("Stats response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(statsResponse);
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    // ── For You ────────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/for-you", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getForYou(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI for-you request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeForYou(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePrivate())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing for-you screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/for-you", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postForYou(SduiRequestContext ctx, HttpServletResponse response) {
        return getForYou(ctx, response);
    }

    // ── Watch ──────────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/watch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getWatch(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI watch request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeWatch(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePublic())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing watch screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/watch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postWatch(SduiRequestContext ctx, HttpServletResponse response) {
        return getWatch(ctx, response);
    }

    // ── Games / Live ───────────────────────────────────────────────────

    @GetMapping(value = {"/sdui/games", "/sdui/live"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getGames(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI games request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeLive(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing games screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = {"/sdui/games", "/sdui/live"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postGames(SduiRequestContext ctx, HttpServletResponse response) {
        return getGames(ctx, response);
    }

    // ── Schedule ───────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getSchedule(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI schedule request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeSchedule(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing schedule screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postSchedule(SduiRequestContext ctx, HttpServletResponse response) {
        return getSchedule(ctx, response);
    }

    // ── Demos ──────────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getDemos(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI demos request: locale={}, schemaVersion={}, platform={}",
                ctx.getLocale(), ctx.getSchemaVersion(), ctx.getPlatformName());

        try {
            JsonNode screenResponse = compositionService.composeDemos(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing demos screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postDemos(SduiRequestContext ctx, HttpServletResponse response) {
        return getDemos(ctx, response);
    }

    // ── Leaders ────────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/leaders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getLeaders(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI leaders request: locale={}, schemaVersion={}, platform={}",
                ctx.getLocale(), ctx.getSchemaVersion(), ctx.getPlatformName());

        try {
            JsonNode screenResponse = compositionService.composeLeaders(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing leaders screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/leaders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postLeaders(SduiRequestContext ctx, HttpServletResponse response) {
        return getLeaders(ctx, response);
    }

    // ── Boxscore ───────────────────────────────────────────────────────

    @GetMapping(value = "/sdui/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getBoxscore(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI boxscore request: gameId={}, locale={}, schemaVersion={}", gameId, ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeBoxscore(gameId, ctx);
            setResponseHeaders(response, ctx);
            log.info("SDUI boxscore response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing SDUI boxscore response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postBoxscore(
            @PathVariable String gameId, SduiRequestContext ctx, HttpServletResponse response) {
        return getBoxscore(gameId, ctx, response);
    }

    // ── Parameterized Refresh (Form submit support) ────────────────────

    @GetMapping(value = "/sdui/refresh/{screenId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> refreshScreen(
            @PathVariable String screenId,
            @RequestParam Map<String, String> allParams,
            HttpServletRequest request,
            HttpServletResponse response) {

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("traceId", traceId);
        log.info("SDUI parameterized refresh: screenId={}, params={}", screenId, allParams);

        try {
            JsonNode screenResponse;

            if ("stats-leaders".equals(screenId)) {
                String locale = allParams.getOrDefault("locale", "en");
                screenResponse = compositionService.composeLeadersRefresh(traceId, allParams, locale);
            } else {
                ObjectNode placeholder = objectMapper.createObjectNode();
                placeholder.put("id", screenId);
                placeholder.put("traceId", traceId);
                placeholder.put("schemaVersion", "1.0");

                ObjectNode state = objectMapper.createObjectNode();
                allParams.forEach(state::put);
                placeholder.set("state", state);
                placeholder.set("sections", objectMapper.createArrayNode());
                screenResponse = placeholder;
            }

            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Schema-Version", "1.0");
            log.info("SDUI refresh response composed: screenId={}", screenId);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing SDUI refresh response for screenId={}", screenId, e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    // ── Health ──────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    // ── Private helpers ────────────────────────────────────────────────

    /**
     * Ensure the request context has a trace ID.
     * If the client didn't send X-Trace-Id, generate one.
     */
    private void ensureTraceId(SduiRequestContext ctx) {
        if (ctx.getTraceId() == null || ctx.getTraceId().isBlank()) {
            ctx.setTraceId("trace-" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    /** Set standard response headers from the request context. */
    private void setResponseHeaders(HttpServletResponse response, SduiRequestContext ctx) {
        response.setHeader("X-Trace-Id", ctx.getTraceId());
        response.setHeader("X-Schema-Version", ctx.getSchemaVersion());
    }

    /**
     * Resolve Cache-Control based on game state (D7 mapping).
     */
    private CacheControl resolveCacheControl(String gameState, String defaultState) {
        String state = gameState != null ? gameState : defaultState;
        return switch (state) {
            case "live" -> CacheControl.noCache();
            case "post", "final" -> CacheControl.maxAge(Duration.ofSeconds(3600)).cachePublic();
            default -> CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic(); // pre
        };
    }
}
