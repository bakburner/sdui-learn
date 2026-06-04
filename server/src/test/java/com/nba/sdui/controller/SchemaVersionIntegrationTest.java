package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for schema version negotiation in the SDUI controller.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Clients below minSupportedVersion receive upgrade-required response + header</li>
 *   <li>Clients at or above current version receive normal responses</li>
 *   <li>Version filter is applied to composed responses</li>
 * </ul>
 */
@WebMvcTest(SduiController.class)
@Import({SchemaVersionChecker.class, SchemaVersionConfig.class, SchemaVersionFilter.class, SchemaVersionRegistry.class})
class SchemaVersionIntegrationTest {

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

    @Autowired
    private SchemaVersionConfig versionConfig;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode scoreboardResponse = objectMapper.readTree(
                "{\"id\":\"scoreboard\",\"schemaVersion\":\"1.0\",\"sections\":[{\"type\":\"AtomicComposite\",\"data\":{\"type\":\"Box\"}}]}");
        when(compositionService.composeScoreboard(any(SduiRequestContext.class)))
                .thenReturn(scoreboardResponse);

        JsonNode forYouResponse = objectMapper.readTree(
                "{\"id\":\"for-you\",\"schemaVersion\":\"1.0\",\"sections\":[]}");
        when(compositionService.composeForYou(any(SduiRequestContext.class)))
                .thenReturn(forYouResponse);
    }

    @Test
    void clientAtCurrentVersionReceivesNormalResponse() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/scoreboard")
                        .param("schemaVersion", "1.0")
        )
                .andExpect(status().isOk())
                .andExpect(header().string("X-Schema-Version", "1.0"))
                .andExpect(header().doesNotExist(SchemaVersionChecker.MISMATCH_HEADER))
                .andExpect(jsonPath("$.data.id").value("scoreboard"))
                .andExpect(jsonPath("$.data.sections").isArray())
                .andExpect(jsonPath("$.meta.degraded").value(false));
    }

    @Test
    void clientBelowMinVersionReceivesUpgradeRequired() throws Exception {
        // Temporarily set min to 2.0 to trigger mismatch
        String originalMin = versionConfig.getMinSupportedVersion();
        versionConfig.setMinSupportedVersion("2.0");

        try {
            mockMvc.perform(
                    get("/v1/sdui/screen/scoreboard")
                            .param("schemaVersion", "1.0")
            )
                    .andExpect(status().isOk())
                    .andExpect(header().string(SchemaVersionChecker.MISMATCH_HEADER,
                            SchemaVersionChecker.UPGRADE_REQUIRED))
                    .andExpect(jsonPath("$.data.id").value("upgrade-required"))
                    .andExpect(jsonPath("$.data.sections[0].type").value("AtomicComposite"))
                    .andExpect(jsonPath("$.data.sections[0].id").value("error-schema-version-mismatch"));
        } finally {
            versionConfig.setMinSupportedVersion(originalMin);
        }
    }

    @Test
    void mismatchHeaderValueIsCorrect() throws Exception {
        String originalMin = versionConfig.getMinSupportedVersion();
        versionConfig.setMinSupportedVersion("2.0");

        try {
            mockMvc.perform(
                    get("/v1/sdui/screen/for-you")
                            .param("schemaVersion", "1.0")
            )
                    .andExpect(status().isOk())
                    .andExpect(header().string(SchemaVersionChecker.MISMATCH_HEADER, "upgrade-required"));
        } finally {
            versionConfig.setMinSupportedVersion(originalMin);
        }
    }

    @Test
    void clientWithoutSchemaVersionUsesDefault() throws Exception {
        // Default is 1.0 per SduiRequestContext, should work fine with minSupportedVersion=1.0
        mockMvc.perform(get("/v1/sdui/screen/scoreboard"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Schema-Version", "1.0"))
                .andExpect(header().doesNotExist(SchemaVersionChecker.MISMATCH_HEADER));
    }

    @Test
    void responseEchoesServerCurrentVersionInHeader() throws Exception {
        mockMvc.perform(
                get("/v1/sdui/screen/scoreboard")
                        .param("schemaVersion", "1.0")
        )
                .andExpect(status().isOk())
                .andExpect(header().string("X-Schema-Version", "1.0"));
    }
}
