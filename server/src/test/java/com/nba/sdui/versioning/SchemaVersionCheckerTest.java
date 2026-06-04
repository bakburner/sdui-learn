package com.nba.sdui.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SchemaVersionChecker} — verifies upgrade-required
 * detection and ErrorState response composition.
 */
class SchemaVersionCheckerTest {

    private SchemaVersionConfig config;
    private SchemaVersionChecker checker;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        config = new SchemaVersionConfig();
        config.setCurrentVersion("2.0");
        config.setMinSupportedVersion("1.0");
        mapper = new ObjectMapper();
        checker = new SchemaVersionChecker(config, mapper);
    }

    @Test
    void clientAtMinVersionIsSupported() {
        assertFalse(checker.isUpgradeRequired("1.0"));
    }

    @Test
    void clientAboveMinVersionIsSupported() {
        assertFalse(checker.isUpgradeRequired("1.1"));
        assertFalse(checker.isUpgradeRequired("2.0"));
    }

    @Test
    void clientBelowMinVersionRequiresUpgrade() {
        config.setMinSupportedVersion("1.1");
        checker = new SchemaVersionChecker(config, mapper);

        assertTrue(checker.isUpgradeRequired("1.0"));
        assertTrue(checker.isUpgradeRequired("0.9"));
    }

    @Test
    void invalidVersionStringRequiresUpgrade() {
        assertTrue(checker.isUpgradeRequired("invalid"));
        assertTrue(checker.isUpgradeRequired(""));
    }

    @Test
    void upgradeRequiredResponseContainsErrorSection() {
        config.setMinSupportedVersion("2.0");
        checker = new SchemaVersionChecker(config, mapper);

        JsonNode response = checker.composeUpgradeRequiredResponse("1.0", "trace-abc");

        assertEquals("upgrade-required", response.path("id").textValue());
        // Body `traceId` removed in A2a (envelope/correlation lives only in the
        // X-Correlation-ID header now); upgrade-required body shape no longer
        // carries it. The trace string is still threaded into MDC for logging.
        assertEquals("2.0", response.path("schemaVersion").textValue());

        JsonNode sections = response.path("sections");
        assertTrue(sections.isArray());
        assertEquals(1, sections.size());

        JsonNode errorSection = sections.get(0);
        assertEquals("error-schema-version-mismatch", errorSection.path("id").textValue());
        assertEquals("AtomicComposite", errorSection.path("type").textValue());
    }

    @Test
    void upgradeResponseIncludesVersionDiagnosticText() {
        config.setMinSupportedVersion("2.0");
        checker = new SchemaVersionChecker(config, mapper);

        JsonNode response = checker.composeUpgradeRequiredResponse("1.0", "trace-xyz");

        // Find the diagnostic text node
        JsonNode children = response.path("sections").get(0).path("data").path("children");
        boolean foundDiagnostic = false;
        for (JsonNode child : children) {
            String content = child.path("props").path("content").textValue();
            if (content != null && content.contains("Your version: 1.0")) {
                foundDiagnostic = true;
                assertTrue(content.contains("Required: 2.0+"));
            }
        }
        assertTrue(foundDiagnostic, "Response should contain version diagnostic text");
    }
}
