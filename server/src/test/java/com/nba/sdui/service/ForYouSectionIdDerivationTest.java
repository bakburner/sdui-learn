package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for section ID derivation in ForYouComposer.
 * Verifies every section in the response has a non-null {@code contentSourceId}
 * and that the section {@code id} is derived from it via {@link SectionIdDeriver}.
 */
class ForYouSectionIdDerivationTest {

    private ForYouComposer composer;

    @BeforeEach
    void setUp() {
        ObjectMapper om = new ObjectMapper();
        StatsApiClient statsApiClient = new StatsApiClient(om);
        SduiUtils utils = new SduiUtils(om);
        composer = new ForYouComposer(om, statsApiClient, utils);
    }

    @Test
    void allSectionsHaveContentSourceId() {
        JsonNode response = composer.composeForYou("test-trace-id", "en");

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
        JsonNode response = composer.composeForYou("test-trace-id", "en");

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
        JsonNode response = composer.composeForYou("test-trace-id", "en");

        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText("");
            assertTrue(SectionIdDeriver.isDerived(sectionId),
                    "Section id '" + sectionId + "' must use '~type=' derived format");
        }
    }

    @Test
    void noPositionalComponentsInIds() {
        JsonNode response = composer.composeForYou("test-trace-id", "en");

        ArrayNode sections = (ArrayNode) response.get("sections");
        for (JsonNode section : sections) {
            String sectionId = section.path("id").asText("");
            assertFalse(sectionId.matches(".*~slug=\\d+$"),
                    "Section id '" + sectionId + "' must not use positional indices as slug");
        }
    }

    @Test
    void stampStringTableAppliesDefaultContentInsets() {
        JsonNode response = composer.composeForYou("test-trace-id", "en");
        JsonNode insets = response.get("contentInsets");
        assertNotNull(insets, "Screen must carry server-owned contentInsets");
        assertEquals(LayoutTokens.SPACING_MD, insets.path("start").asText());
        assertEquals(LayoutTokens.SPACING_MD, insets.path("end").asText());
        assertEquals(LayoutTokens.SPACING_LG, insets.path("bottom").asText());
    }
}
