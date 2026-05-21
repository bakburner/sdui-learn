package com.nba.sdui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example/fixture JSON must match the wire contract — no deprecated atomic
 * sizing fields that strict clients no longer decode.
 */
class ExampleWireContractTest {

    private static final List<String> FORBIDDEN_ATOMIC_KEYS = List.of("fillWidth", "fillHeight");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void schemaExamplesMustNotEmitDeprecatedSizingFields() throws IOException {
        // Gradle runs tests with `server/` as cwd, so resolve schema/examples
        // relative to the repo root.
        Path root = resolveSchemaExamplesPath();
        List<String> violations = new ArrayList<>();
        try (var paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                try {
                    JsonNode tree = objectMapper.readTree(Files.readString(p));
                    collectForbiddenKeys(tree, p.toString(), violations);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertTrue(violations.isEmpty(),
                "Deprecated sizing keys found in schema/examples: " + violations);
    }

    /**
     * Locate {@code schema/examples} relative to the test working directory.
     * Gradle runs from {@code server/} but the path works from the repo root
     * as well, so we try both.
     */
    private static Path resolveSchemaExamplesPath() {
        Path[] candidates = {
                Path.of("..", "schema", "examples"),
                Path.of("schema", "examples")
        };
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not locate schema/examples relative to cwd=" + Path.of("").toAbsolutePath());
    }

    private void collectForbiddenKeys(JsonNode node, String path, List<String> violations) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(field -> {
                if (FORBIDDEN_ATOMIC_KEYS.contains(field)) {
                    violations.add(path + " :: " + field);
                }
                collectForbiddenKeys(node.get(field), path, violations);
            });
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                collectForbiddenKeys(child, path, violations);
            }
        }
    }
}
