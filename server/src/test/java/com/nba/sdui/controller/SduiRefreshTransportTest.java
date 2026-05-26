package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.service.ParameterizedRefreshService;
import com.nba.sdui.service.SduiCompositionService;
import com.nba.sdui.service.SectionRefreshService;
import com.nba.sdui.versioning.SchemaVersionChecker;
import com.nba.sdui.versioning.SchemaVersionConfig;
import com.nba.sdui.versioning.SchemaVersionFilter;
import com.nba.sdui.versioning.SchemaVersionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@code /v1/sdui/refresh/{screenId}} and the device-class
 * field carried by {@link SduiRequestContext}.
 *
 * <p>Asserts that the refresh endpoint honors the canonical SDUI transport
 * contract: GET-first with bracket-notation envelope params, POST fallback
 * with the same envelope shape in the JSON body, and user filter params on
 * the URL query string regardless of HTTP method.
 *
 * <p>Also asserts that {@code platform[deviceClass]} (GET) and
 * {@code platform.deviceClass} (POST JSON body) round-trip into
 * {@code ctx.getPlatform().getDeviceClass()} via the shared
 * {@link com.nba.sdui.request.BracketParamResolver}, satisfying the Phase 3
 * envelope-transport contract.
 *
 * <p>Guards against the regression where this endpoint was GET-only and
 * threw 405 on POST, breaking the GET/POST symmetry every other composition
 * route relies on.
 */
@WebMvcTest(SduiController.class)
@Import({SchemaVersionChecker.class, SchemaVersionConfig.class, SchemaVersionFilter.class, SchemaVersionRegistry.class})
class SduiRefreshTransportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SduiCompositionService compositionService;

    @MockBean
    private SectionRefreshService sectionRefreshService;

    @MockBean
    private ParameterizedRefreshService parameterizedRefreshService;

    @BeforeEach
    void setUp() throws Exception {
        ObjectNode refreshResponse = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"stats-leaders\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(parameterizedRefreshService.refreshScreen(eq("stats-leaders"), anyString(), any(), any()))
                .thenReturn(Optional.of(refreshResponse));

        ObjectNode scoreboardResponse = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"scoreboard\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(compositionService.composeScoreboard(any(SduiRequestContext.class)))
                .thenReturn(scoreboardResponse);
    }

    @Test
    void getRefreshAcceptsBracketEnvelopeAndExtractsUserParams() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/refresh/stats-leaders")
                        .param("perMode", "Totals")
                        .param("season", "2025-26")
                        .param("seasonType", "Regular Season")
                        .param("locale", "en")
                        .param("platform[deviceClass]", "tablet")
                        .param("market[cohort]", "MARKET_UNKNOWN")
                        .param("experiments[gd_tab_order_v2]", "variant_b")
                        .header("X-Platform", "ios")
                        .header("X-Trace-Id", "trace-123")
        ).andExpect(status().isOk());

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(parameterizedRefreshService).refreshScreen(
                eq("stats-leaders"),
                eq("trace-123"),
                userParams.capture(),
                any()
        );

        Map<String, String> captured = userParams.getValue();
        assertEquals("Totals", captured.get("perMode"));
        assertEquals("2025-26", captured.get("season"));
        assertEquals("Regular Season", captured.get("seasonType"));

        // Envelope keys must NOT leak into user params — composers care only
        // about what the client form sent.
        assertFalse(captured.containsKey("locale"),
                "envelope locale must be stripped: " + captured);
        assertFalse(captured.keySet().stream().anyMatch(k -> k.contains("[")),
                "bracket-notation envelope keys must be stripped: " + captured);
    }

    @Test
    void postRefreshAcceptsJsonEnvelopeAndUrlUserParams() throws Exception {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("locale", "en");
        envelope.put("schemaVersion", "1.0");
        Map<String, Object> platform = new HashMap<>();
        platform.put("deviceClass", "tablet");
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("sse", true);
        platform.put("capabilities", capabilities);
        envelope.put("platform", platform);

        mockMvc.perform(
                post("/v1/sdui/screen/refresh/stats-leaders")
                        .param("perMode", "Totals")
                        .param("season", "2025-26")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(envelope))
                        .header("X-Platform", "ios")
                        .header("X-Trace-Id", "trace-456")
        ).andExpect(status().isOk());

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(parameterizedRefreshService).refreshScreen(
                eq("stats-leaders"),
                eq("trace-456"),
                userParams.capture(),
                any()
        );

        Map<String, String> captured = userParams.getValue();
        assertEquals("Totals", captured.get("perMode"));
        assertEquals("2025-26", captured.get("season"));
        assertFalse(captured.containsKey("platform"),
                "envelope keys from JSON body must not leak into user params: " + captured);
    }

    @Test
    void gamesRefreshRoute_isDualMountedForGetAndPost_withSameShape() throws Exception {
        ObjectNode gamesResponse = (ObjectNode) objectMapper.readTree(
                """
                {
                  "id": "games",
                  "schemaVersion": "1.0",
                  "state": { "games_selected_date": "2026-05-26" },
                  "sections": [
                    {
                      "id": "server:games-calendar~type=CalendarStrip",
                      "type": "CalendarStrip",
                      "data": {
                        "stateKey": "games_selected_date",
                        "selectedDate": "2026-05-26",
                        "defaultDate": "2026-05-26",
                        "onDateSelected": {
                          "trigger": "onActivate",
                          "type": "refresh",
                          "endpoint": "/v1/sdui/screen/refresh/games",
                          "paramBindings": { "date": "{{games_selected_date}}" }
                        }
                      }
                    }
                  ]
                }
                """);
        when(parameterizedRefreshService.refreshScreen(eq("games"), anyString(), any(), any()))
                .thenReturn(Optional.of(gamesResponse));

        String getBody = mockMvc.perform(
                        get("/v1/sdui/screen/refresh/games")
                                .param("date", "2026-05-26")
                                .param("locale", "en")
                                .header("X-Platform", "web")
                                .header("X-Trace-Id", "trace-games-get")
                ).andExpect(status().isOk())
                .andExpect(content().json(gamesResponse.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("locale", "en");
        envelope.put("schemaVersion", "1.0");

        mockMvc.perform(
                        post("/v1/sdui/screen/refresh/games")
                                .param("date", "2026-05-26")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsBytes(envelope))
                                .header("X-Platform", "web")
                                .header("X-Trace-Id", "trace-games-post")
                ).andExpect(status().isOk())
                .andExpect(content().json(getBody));
    }

    /**
     * An unknown screenId must return 404 (no resolver registered) rather than
     * a silent empty placeholder response. Clients treat 404 as "this screen ID
     * is gone" and surface an error state rather than silently showing nothing.
     */
    @Test
    void unknownScreenIdReturns404() throws Exception {
        when(parameterizedRefreshService.refreshScreen(eq("unknown-screen"), anyString(), any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/v1/sdui/screen/refresh/unknown-screen")
                        .param("locale", "en")
                        .header("X-Platform", "web")
        ).andExpect(status().isNotFound());
    }

    /**
     * GET on a route that takes {@link SduiRequestContext} end to end —
     * proves the bracket-notation {@code platform[deviceClass]} param survives
     * Jackson's {@code convertValue} into {@code ctx.getPlatform().getDeviceClass()}.
     */
    @Test
    void getScoreboardCarriesDeviceClassThroughEnvelope() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/scoreboard")
                        .param("locale", "en")
                        .param("platform[deviceClass]", "tablet")
                        .param("market[cohort]", "US_NY_METRO")
                        .header("X-Platform", "ios")
                        .header("X-Trace-Id", "trace-ff-get")
        ).andExpect(status().isOk());

        ArgumentCaptor<SduiRequestContext> ctxCaptor =
                ArgumentCaptor.forClass(SduiRequestContext.class);
        verify(compositionService).composeScoreboard(ctxCaptor.capture());

        SduiRequestContext ctx = ctxCaptor.getValue();
        assertNotNull(ctx.getPlatform(), "platform must be populated from bracket params");
        assertEquals("tablet", ctx.getPlatform().getDeviceClass(),
                "platform[deviceClass] must round-trip into ctx.platform.deviceClass");
    }

    /**
     * POST symmetry: the same device-class value carried in the JSON body
     * must produce identical {@code ctx} state. GET/POST symmetry is part of
     * the envelope transport contract.
     */
    @Test
    void postScoreboardCarriesDeviceClassThroughJsonBody() throws Exception {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("locale", "en");
        envelope.put("schemaVersion", "1.0");
        Map<String, Object> platform = new HashMap<>();
        platform.put("deviceClass", "tablet");
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("sse", true);
        platform.put("capabilities", capabilities);
        envelope.put("platform", platform);

        mockMvc.perform(
                post("/v1/sdui/screen/scoreboard")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(envelope))
                        .header("X-Platform", "ios")
                        .header("X-Trace-Id", "trace-ff-post")
        ).andExpect(status().isOk());

        ArgumentCaptor<SduiRequestContext> ctxCaptor =
                ArgumentCaptor.forClass(SduiRequestContext.class);
        verify(compositionService).composeScoreboard(ctxCaptor.capture());

        SduiRequestContext ctx = ctxCaptor.getValue();
        assertNotNull(ctx.getPlatform(), "platform must be populated from JSON body");
        assertEquals("tablet", ctx.getPlatform().getDeviceClass(),
                "platform.deviceClass must round-trip through JSON deserialization");
    }
}
