package com.nba.sdui.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SchemaVersionFilter} — verifies that fields and enum
 * values introduced after the client's version are stripped from responses.
 */
class SchemaVersionFilterTest {

    private SchemaVersionRegistry registry;
    private SchemaVersionFilter filter;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        registry = new SchemaVersionRegistry();
        filter = new SchemaVersionFilter(registry);
        mapper = new ObjectMapper();
    }

    @Test
    void noOpWhenClientIsAtCurrentVersion() throws Exception {
        String json = "{\"id\":\"screen\",\"sections\":[{\"type\":\"AtomicComposite\",\"data\":{\"type\":\"Box\"}}]}";
        JsonNode original = mapper.readTree(json);
        JsonNode copy = mapper.readTree(json);

        JsonNode result = filter.apply(original, SchemaVersion.of(1, 0), SchemaVersion.of(1, 0));
        assertEquals(copy, result);
    }

    @Test
    void noOpWhenClientIsNewerThanCurrent() throws Exception {
        String json = "{\"id\":\"screen\",\"sections\":[]}";
        JsonNode original = mapper.readTree(json);

        JsonNode result = filter.apply(original, SchemaVersion.of(2, 0), SchemaVersion.of(1, 0));
        assertEquals(original, result);
    }

    @Test
    void stripsFieldsIntroducedAfterClientVersion() throws Exception {
        // Register a field introduced in 1.1
        registry.registerField("sections[*].data.conditionalProperties", SchemaVersion.of(1, 1));

        String json = """
                {
                  "id": "screen",
                  "sections": [{
                    "type": "AtomicComposite",
                    "data": {
                      "type": "Box",
                      "conditionalProperties": {"live": {"visible": true}},
                      "style": {"padding": "token:nba.spacing.md"}
                    }
                  }]
                }
                """;
        JsonNode response = mapper.readTree(json);

        // Client at 1.0 should not see conditionalProperties (introduced in 1.1)
        JsonNode result = filter.apply(response, SchemaVersion.of(1, 0), SchemaVersion.of(1, 1));

        JsonNode sectionData = result.path("sections").get(0).path("data");
        assertFalse(sectionData.has("conditionalProperties"),
                "conditionalProperties should be stripped for 1.0 client");
        assertTrue(sectionData.has("style"),
                "existing fields should remain");
        assertEquals("Box", sectionData.path("type").textValue());
    }

    @Test
    void stripsEnumValuesIntroducedAfterClientVersion() throws Exception {
        // Register a new section type enum value introduced in 2.0
        registry.registerEnumValue("sections[*].type", "SubscribeUpsell", SchemaVersion.of(2, 0));

        String json = """
                {
                  "id": "screen",
                  "sections": [
                    {"type": "AtomicComposite", "data": {"type": "Box"}},
                    {"type": "SubscribeUpsell", "data": {"type": "Box"}}
                  ]
                }
                """;
        JsonNode response = mapper.readTree(json);

        // Client at 1.0 should have SubscribeUpsell type nullified
        JsonNode result = filter.apply(response, SchemaVersion.of(1, 0), SchemaVersion.of(2, 0));

        assertEquals("AtomicComposite", result.path("sections").get(0).path("type").textValue());
        assertTrue(result.path("sections").get(1).path("type").isNull(),
                "SubscribeUpsell enum should be nullified for 1.0 client");
    }

    @Test
    void clientAtIntroductionVersionRetainsField() throws Exception {
        registry.registerField("sections[*].data.accessibilityRole", SchemaVersion.of(1, 1));

        String json = """
                {
                  "sections": [{
                    "data": {
                      "type": "Box",
                      "accessibilityRole": "heading"
                    }
                  }]
                }
                """;
        JsonNode response = mapper.readTree(json);

        // Client at exactly 1.1 should keep the field
        JsonNode result = filter.apply(response, SchemaVersion.of(1, 1), SchemaVersion.of(1, 1));

        assertTrue(result.path("sections").get(0).path("data").has("accessibilityRole"),
                "field should be retained when client is at introduction version");
    }

    @Test
    void noTransformationWhenRegistryIsEmpty() throws Exception {
        String json = "{\"id\":\"screen\",\"sections\":[{\"type\":\"AtomicComposite\"}]}";
        JsonNode response = mapper.readTree(json);
        String originalStr = mapper.writeValueAsString(response);

        // Even though client is behind, no registered changes means no stripping
        JsonNode result = filter.apply(response, SchemaVersion.of(1, 0), SchemaVersion.of(2, 0));
        assertEquals(originalStr, mapper.writeValueAsString(result));
    }
}
