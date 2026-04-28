package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.service.SduiCompositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests for {@code /sdui/refresh/{screenId}} and the form-factor
 * field carried by {@link SduiRequestContext}.
 *
 * <p>Asserts that the refresh endpoint honors the canonical SDUI transport
 * contract: GET-first with bracket-notation envelope params, POST fallback
 * with the same envelope shape in the JSON body, and user filter params on
 * the URL query string regardless of HTTP method.
 *
 * <p>Also asserts that {@code platform[formFactor]} (GET) and
 * {@code platform.formFactor} (POST JSON body) round-trip into
 * {@code ctx.getPlatform().getFormFactor()} via the shared
 * {@link com.nba.sdui.request.BracketParamResolver}, satisfying the Phase 3
 * envelope-transport contract.
 *
 * <p>Guards against the regression where this endpoint was GET-only and
 * threw 405 on POST, breaking the GET/POST symmetry every other composition
 * route relies on.
 */
@WebMvcTest(SduiController.class)
class SduiRefreshTransportTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SduiCompositionService compositionService;

    @BeforeEach
    void setUp() throws Exception {
        ObjectNode refreshResponse = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"stats-leaders\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(compositionService.composeLeadersRefresh(anyString(), any(), anyString()))
                .thenReturn(refreshResponse);

        ObjectNode scoreboardResponse = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"scoreboard\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(compositionService.composeScoreboard(any(SduiRequestContext.class)))
                .thenReturn(scoreboardResponse);
    }

    @Test
    void getRefreshAcceptsBracketEnvelopeAndExtractsUserParams() throws Exception {
        mockMvc.perform(
                get("/sdui/refresh/stats-leaders")
                        .param("perMode", "Totals")
                        .param("season", "2025-26")
                        .param("seasonType", "Regular Season")
                        .param("locale", "en")
                        .param("platform[name]", "ios")
                        .param("platform[formFactor]", "tablet")
                        .param("device[countryCode]", "US")
                        .param("experiments[gd_tab_order_v2]", "variant_b")
                        .header("X-Trace-Id", "trace-123")
        ).andExpect(status().isOk());

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(compositionService).composeLeadersRefresh(
                eq("trace-123"),
                userParams.capture(),
                eq("en")
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
        platform.put("name", "ios");
        platform.put("appVersion", "8.3.0");
        platform.put("formFactor", "tablet");
        envelope.put("platform", platform);

        mockMvc.perform(
                post("/sdui/refresh/stats-leaders")
                        .param("perMode", "Totals")
                        .param("season", "2025-26")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(envelope))
                        .header("X-Trace-Id", "trace-456")
        ).andExpect(status().isOk());

        ArgumentCaptor<Map<String, String>> userParams = ArgumentCaptor.forClass(Map.class);
        verify(compositionService).composeLeadersRefresh(
                eq("trace-456"),
                userParams.capture(),
                eq("en")
        );

        Map<String, String> captured = userParams.getValue();
        assertEquals("Totals", captured.get("perMode"));
        assertEquals("2025-26", captured.get("season"));
        assertFalse(captured.containsKey("platform"),
                "envelope keys from JSON body must not leak into user params: " + captured);
    }

    /**
     * GET on a route that takes {@link SduiRequestContext} end to end —
     * proves the bracket-notation {@code platform[formFactor]} param survives
     * Jackson's {@code convertValue} into {@code ctx.getPlatform().getFormFactor()}.
     */
    @Test
    void getScoreboardCarriesFormFactorThroughEnvelope() throws Exception {
        mockMvc.perform(
                get("/sdui/scoreboard")
                        .param("locale", "en")
                        .param("platform[name]", "ios")
                        .param("platform[formFactor]", "tablet")
                        .header("X-Trace-Id", "trace-ff-get")
        ).andExpect(status().isOk());

        ArgumentCaptor<SduiRequestContext> ctxCaptor =
                ArgumentCaptor.forClass(SduiRequestContext.class);
        verify(compositionService).composeScoreboard(ctxCaptor.capture());

        SduiRequestContext ctx = ctxCaptor.getValue();
        assertNotNull(ctx.getPlatform(), "platform must be populated from bracket params");
        assertEquals("ios", ctx.getPlatform().getName());
        assertEquals("tablet", ctx.getPlatform().getFormFactor(),
                "platform[formFactor] must round-trip into ctx.platform.formFactor");
    }

    /**
     * POST symmetry: the same form-factor value carried in the JSON body
     * must produce identical {@code ctx} state. GET/POST symmetry is part of
     * the envelope transport contract.
     */
    @Test
    void postScoreboardCarriesFormFactorThroughJsonBody() throws Exception {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("locale", "en");
        envelope.put("schemaVersion", "1.0");
        Map<String, Object> platform = new HashMap<>();
        platform.put("name", "ios");
        platform.put("formFactor", "tablet");
        envelope.put("platform", platform);

        mockMvc.perform(
                post("/sdui/scoreboard")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(envelope))
                        .header("X-Trace-Id", "trace-ff-post")
        ).andExpect(status().isOk());

        ArgumentCaptor<SduiRequestContext> ctxCaptor =
                ArgumentCaptor.forClass(SduiRequestContext.class);
        verify(compositionService).composeScoreboard(ctxCaptor.capture());

        SduiRequestContext ctx = ctxCaptor.getValue();
        assertNotNull(ctx.getPlatform(), "platform must be populated from JSON body");
        assertEquals("ios", ctx.getPlatform().getName());
        assertEquals("tablet", ctx.getPlatform().getFormFactor(),
                "platform.formFactor must round-trip through JSON deserialization");
    }
}
