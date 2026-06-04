package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.SduiCompositionService;
import com.nba.sdui.orchestration.SectionRefreshService;
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
 * Contract tests for the unified {@code /v1/sdui/screen/{id}} route with
 * optional query-param support and the device-class field carried by
 * {@link SduiRequestContext}.
 *
 * <p>Asserts that the unified screen endpoint honors the canonical SDUI
 * transport contract: GET-first with bracket-notation envelope params,
 * POST fallback with the same envelope shape in the JSON body, and user
 * filter params on the URL query string regardless of HTTP method.
 *
 * <p>Also verifies the hard cut: {@code /v1/sdui/screen/refresh/{id}} is
 * fully removed and returns 404.
 */
@WebMvcTest(SduiController.class)
@Import({SchemaVersionChecker.class, SchemaVersionConfig.class, SchemaVersionFilter.class, SchemaVersionRegistry.class, com.nba.sdui.metrics.SduiMetrics.class})
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
        ObjectNode leadersResponse = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"leaders\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(parameterizedRefreshService.refreshScreen(eq("leaders"), anyString(), any(), any()))
                .thenReturn(Optional.of(leadersResponse));

        ObjectNode scoreboardResponse = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"scoreboard\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(compositionService.composeScoreboard(any(SduiRequestContext.class)))
                .thenReturn(scoreboardResponse);
    }

    @Test
    void getLeadersWithParams_acceptsBracketEnvelopeAndExtractsUserParams() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/leaders")
                        .param("perMode", "Totals")
                        .param("season", "2025-26")
                        .param("seasonType", "Regular Season")
                        .param("locale", "en")
                        .param("platform[deviceClass]", "tablet")
                        .param("market[cohort]", "MARKET_UNKNOWN")
                        .param("experiments[gd_tab_order_v2]", "variant_b")
                        .header("X-Analytics-Platform", "ios")
                        .header("X-Correlation-ID", "11111111-1111-1111-1111-111111111111")
        ).andExpect(status().isOk());

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(parameterizedRefreshService).refreshScreen(
                eq("leaders"),
                eq("11111111-1111-1111-1111-111111111111"),
                userParams.capture(),
                any()
        );

        Map<String, String> captured = userParams.getValue();
        assertEquals("Totals", captured.get("perMode"));
        assertEquals("2025-26", captured.get("season"));
        assertEquals("Regular Season", captured.get("seasonType"));

        assertFalse(captured.containsKey("locale"),
                "envelope locale must be stripped: " + captured);
        assertFalse(captured.keySet().stream().anyMatch(k -> k.contains("[")),
                "bracket-notation envelope keys must be stripped: " + captured);
    }

    @Test
    void postLeadersWithParams_acceptsJsonEnvelopeAndUrlUserParams() throws Exception {
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
                post("/v1/sdui/screen/leaders")
                        .param("perMode", "Totals")
                        .param("season", "2025-26")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(envelope))
                        .header("X-Analytics-Platform", "ios")
                        .header("X-Correlation-ID", "22222222-2222-2222-2222-222222222222")
        ).andExpect(status().isOk());

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(parameterizedRefreshService).refreshScreen(
                eq("leaders"),
                eq("22222222-2222-2222-2222-222222222222"),
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
    void gamesRoute_isDualMountedForGetAndPost_withSameShape() throws Exception {
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
                          "endpoint": "/v1/sdui/screen/games",
                          "paramBindings": { "date": "{{games_selected_date}}" }
                        }
                      }
                    }
                  ]
                }
                """);
        when(parameterizedRefreshService.refreshScreen(eq("games"), anyString(), any(), any()))
                .thenReturn(Optional.of(gamesResponse));

        // The controller wraps every screen-channel response in
        // ResponseEnvelope<T>(data, meta). The body shape is
        // {"data": <gamesResponse>, "meta": {"degraded": false, ...}}.
        ObjectNode expectedEnvelope = objectMapper.createObjectNode();
        expectedEnvelope.set("data", gamesResponse);

        String getBody = mockMvc.perform(
                        get("/v1/sdui/screen/games")
                                .param("date", "2026-05-26")
                                .param("locale", "en")
                                .header("X-Analytics-Platform", "web")
                                .header("X-Correlation-ID", "33333333-3333-3333-3333-333333333333")
                ).andExpect(status().isOk())
                .andExpect(content().json(expectedEnvelope.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("locale", "en");
        envelope.put("schemaVersion", "1.0");

        mockMvc.perform(
                        post("/v1/sdui/screen/games")
                                .param("date", "2026-05-26")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsBytes(envelope))
                                .header("X-Analytics-Platform", "web")
                                .header("X-Correlation-ID", "44444444-4444-4444-4444-444444444444")
                ).andExpect(status().isOk())
                .andExpect(content().json(getBody));
    }

    /**
     * The legacy {@code /v1/sdui/screen/refresh/{screenId}} route has been
     * hard-cut. Requests to it must return 404 — Spring's default for an
     * unmapped path. This pins the removal and prevents silent re-introduction.
     */
    @Test
    void legacyRefreshRoute_returns404_afterHardCut() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/refresh/games")
                        .param("date", "2026-05-26")
                        .param("locale", "en")
                        .header("X-Analytics-Platform", "web")
        ).andExpect(status().isNotFound());
    }

    @Test
    void getScoreboardCarriesDeviceClassThroughEnvelope() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/scoreboard")
                        .param("locale", "en")
                        .param("platform[deviceClass]", "tablet")
                        .param("market[cohort]", "US_NY_METRO")
                        .header("X-Analytics-Platform", "ios")
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
                        .header("X-Analytics-Platform", "ios")
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
