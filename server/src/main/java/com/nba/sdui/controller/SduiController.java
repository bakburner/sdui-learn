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

    // ── Home (NBA.com style) ───────────────────────────────────────────

    @GetMapping(value = "/sdui/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getHome(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI home request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        try {
            JsonNode screenResponse = compositionService.composeHome(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePublic())
                    .body(screenResponse);
        } catch (Exception e) {
            log.error("Error composing home screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/sdui/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postHome(SduiRequestContext ctx, HttpServletResponse response) {
        return getHome(ctx, response);
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

    /**
     * Parameterized refresh endpoint. User-supplied filter params (e.g.
     * {@code perMode}, {@code season}) always travel in the URL query string,
     * regardless of HTTP method. The request envelope ({@link SduiRequestContext})
     * follows the same GET/POST rule as every other composition route — bracket
     * notation in the query for GET, JSON body for POST when the envelope is
     * large or sensitive.
     */
    @GetMapping(value = "/sdui/refresh/{screenId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getRefreshScreen(
            @PathVariable String screenId,
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletRequest request,
            HttpServletResponse response) {
        return refreshScreen(screenId, allParams, ctx, request, response);
    }

    @PostMapping(value = "/sdui/refresh/{screenId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postRefreshScreen(
            @PathVariable String screenId,
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletRequest request,
            HttpServletResponse response) {
        return refreshScreen(screenId, allParams, ctx, request, response);
    }

    private ResponseEntity<JsonNode> refreshScreen(
            String screenId,
            Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletRequest request,
            HttpServletResponse response) {

        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null) {
            traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put("traceId", traceId);

        Map<String, String> userParams = stripEnvelopeKeys(allParams);
        log.info("SDUI parameterized refresh: screenId={}, userParams={}, method={}",
                screenId, userParams, request.getMethod());

        try {
            JsonNode screenResponse;

            if ("stats-leaders".equals(screenId)) {
                String locale = ctx.getLocale() != null ? ctx.getLocale() : "en";
                screenResponse = compositionService.composeLeadersRefresh(traceId, userParams, locale);
            } else {
                ObjectNode placeholder = objectMapper.createObjectNode();
                placeholder.put("id", screenId);
                placeholder.put("traceId", traceId);
                placeholder.put("schemaVersion", "1.0");

                ObjectNode state = objectMapper.createObjectNode();
                userParams.forEach(state::put);
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

    /**
     * Strip envelope keys from a flat {@code @RequestParam} map so what remains
     * is the user-supplied filter params (the things a Form submitted). Envelope
     * keys are either bracket-notation ({@code platform[name]}, {@code device[zipCode]},
     * {@code experiments[exp_id]}) or top-level scalars the envelope already owns
     * ({@code locale}, {@code schemaVersion}, {@code gameState}).
     */
    private static Map<String, String> stripEnvelopeKeys(Map<String, String> params) {
        java.util.LinkedHashMap<String, String> filtered = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            if (k.contains("[")) {
                continue;
            }
            if ("locale".equals(k) || "schemaVersion".equals(k) || "gameState".equals(k)) {
                continue;
            }
            filtered.put(k, e.getValue());
        }
        return filtered;
    }

    // ── Init ──────────────────────────────────────────────────────────

    /**
     * Bootstrap endpoint — returns the initial navigation URI so clients
     * do not need to hardcode a starting screen.
     */
    @GetMapping(value = "/sdui/init", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> init(SduiRequestContext ctx, HttpServletResponse response) {
        ensureTraceId(ctx);
        setResponseHeaders(response, ctx);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("bootstrapUri", "nba://for-you");
        body.put("schemaVersion", ctx.getSchemaVersion());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                .body(body);
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
