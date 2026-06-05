package com.nba.sdui.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.controller.SduiController;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the routing contract: when {@code /v1/sdui/screen/{id}} is called with
 * user query params, {@link ParameterizedRefreshService} is consulted. When
 * called without params, the regular composer entry point is used.
 */
@WebMvcTest(SduiController.class)
@Import({SchemaVersionChecker.class, SchemaVersionConfig.class, SchemaVersionFilter.class, SchemaVersionRegistry.class, com.nba.sdui.metrics.SduiMetrics.class})
class ParameterizedRefreshRoutingTest {

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
        ObjectNode gamesScreen = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"games\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        com.nba.sdui.models.generated.Screen gamesScreenTyped =
                objectMapper.treeToValue(gamesScreen, com.nba.sdui.models.generated.Screen.class);
        when(compositionService.composeLive(any(SduiRequestContext.class)))
                .thenReturn(gamesScreenTyped);
        when(parameterizedRefreshService.refreshScreen(eq("games"), anyString(), any(), any()))
                .thenReturn(Optional.of(gamesScreenTyped));

        ObjectNode leadersScreen = (ObjectNode) objectMapper.readTree(
                "{\"id\":\"leaders\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        com.nba.sdui.models.generated.Screen leadersScreenTyped =
                objectMapper.treeToValue(leadersScreen, com.nba.sdui.models.generated.Screen.class);
        when(compositionService.composeLeaders(any(SduiRequestContext.class)))
                .thenReturn(leadersScreenTyped);
        when(parameterizedRefreshService.refreshScreen(eq("leaders"), anyString(), any(), any()))
                .thenReturn(Optional.of(leadersScreenTyped));
    }

    @Test
    void gamesWithParams_routesThroughParameterizedRefreshService() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/games")
                        .param("date", "2026-05-18")
                        .param("locale", "en")
                        .header("X-Analytics-Platform", "web")
        ).andExpect(status().isOk());

        verify(parameterizedRefreshService).refreshScreen(
                eq("games"), anyString(), any(), any());
        verify(compositionService, never()).composeLive(any());
    }

    @Test
    void gamesWithoutParams_routesThroughRegularComposer() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/games")
                        .param("locale", "en")
                        .header("X-Analytics-Platform", "web")
        ).andExpect(status().isOk());

        verify(compositionService).composeLive(any());
        verify(parameterizedRefreshService, never()).refreshScreen(
                eq("games"), anyString(), any(), any());
    }

    @Test
    void leadersWithParams_routesThroughParameterizedRefreshService() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/leaders")
                        .param("season", "2024-25")
                        .param("perMode", "totals")
                        .param("locale", "en")
                        .header("X-Analytics-Platform", "web")
        ).andExpect(status().isOk());

        verify(parameterizedRefreshService).refreshScreen(
                eq("leaders"), anyString(), any(), any());
        verify(compositionService, never()).composeLeaders(any());
    }

    @Test
    void leadersWithoutParams_routesThroughRegularComposer() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/leaders")
                        .param("locale", "en")
                        .header("X-Analytics-Platform", "web")
        ).andExpect(status().isOk());

        verify(compositionService).composeLeaders(any());
        verify(parameterizedRefreshService, never()).refreshScreen(
                eq("leaders"), anyString(), any(), any());
    }
}
