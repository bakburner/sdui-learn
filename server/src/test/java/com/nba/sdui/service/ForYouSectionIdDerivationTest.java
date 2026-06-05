package com.nba.sdui.service;

import com.nba.sdui.testsupport.TestTokens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.nba.sdui.domain.SduiUtils;
import com.nba.sdui.domain.SectionIdDeriver;
import com.nba.sdui.domain.SectionSurfaces;
import com.nba.sdui.domain.composer.ForYouComposer;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.remote.SeasonCalendarService;
import com.nba.sdui.remote.StatsApiAdapter;
import com.nba.sdui.remote.StatsApiClient;

/**
 * Integration test for section ID derivation in ForYouComposer.
 * Verifies every section in the response has a non-null {@code contentSourceId}
 * and that the section {@code id} is derived from it via {@link SectionIdDeriver}.
 */
class ForYouSectionIdDerivationTest {

    private ForYouComposer composer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        StatsApiClient statsApiClient = new StatsApiClient(objectMapper, new SeasonCalendarService());
        SduiUtils utils = new SduiUtils(objectMapper, TestTokens.INSTANCE);
        SectionSurfaces surfaces = new SectionSurfaces(objectMapper, utils, TestTokens.INSTANCE);
        composer = new ForYouComposer(objectMapper, new StatsApiAdapter(statsApiClient), utils, surfaces, TestTokens.INSTANCE,
                new SectionRefreshService());
    }

    @Test
    void allSectionsHaveContentSourceId() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));

        ArrayNode sections = (ArrayNode) response.get("sections");
        assertNotNull(sections, "Response must have sections");
        assertFalse(sections.isEmpty(), "Sections must not be empty");

        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText(null);
            String contentSourceId = section.path("contentSourceId").asText(null);

            assertNotNull(contentSourceId,
                    "Section '" + sectionId + "' must have contentSourceId");
            assertFalse(contentSourceId.isBlank(),
                    "Section '" + sectionId + "' contentSourceId must not be blank");
        }
    }

    @Test
    void sectionIdsContainContentSourceId() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));

        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText("");
            String contentSourceId = section.path("contentSourceId").asText("");

            assertTrue(sectionId.contains(contentSourceId),
                    "Section id '" + sectionId + "' must contain its contentSourceId '" + contentSourceId + "'");
        }
    }

    @Test
    void sectionIdsContainSeparator() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));

        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText("");
            assertTrue(SectionIdDeriver.isDerived(sectionId),
                    "Section id '" + sectionId + "' must use '~type=' derived format");
        }
    }

    @Test
    void noPositionalComponentsInIds() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));

        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText("");
            assertFalse(sectionId.matches(".*~slug=\\d+$"),
                    "Section id '" + sectionId + "' must not use positional indices as slug");
        }
    }

    @Test
    void stampStringTableAppliesDefaultContentInsets() {
        JsonNode response = objectMapper.valueToTree(composer.composeForYou("test-trace-id", "en"));
        JsonNode insets = response.get("contentInsets");
        assertNotNull(insets, "Screen must carry server-owned contentInsets");
        assertEquals(TestTokens.INSTANCE.spacing("md"), insets.path("start").asText());
        assertEquals(TestTokens.INSTANCE.spacing("md"), insets.path("end").asText());
        assertEquals(TestTokens.INSTANCE.spacing("lg"), insets.path("bottom").asText());
    }
}
