package com.nba.sdui.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Post-composition filter that strips fields and enum values unsupported
 * by the client's declared schema version.
 *
 * <p>This implements dynamic field stripping (per the plan: preferred for
 * prototype over multiple composer versions). The filter walks the composed
 * JSON response and removes:
 * <ul>
 *   <li>Fields introduced after the client's version</li>
 *   <li>Enum values introduced after the client's version (nulled out)</li>
 * </ul>
 *
 * <p>Per AGENTS.md §1.3, strict decoding is intentional. The server must not
 * emit values the client cannot decode — this filter is the enforcement point.
 */
@Component
public class SchemaVersionFilter {

    private static final Logger log = LoggerFactory.getLogger(SchemaVersionFilter.class);

    private final SchemaVersionRegistry registry;

    public SchemaVersionFilter(SchemaVersionRegistry registry) {
        this.registry = registry;
    }

    /**
     * Apply version-aware stripping to a composed response.
     *
     * @param response The composed JSON response (mutated in place)
     * @param clientVersion The client's declared schema version
     * @param currentVersion The server's current schema version
     * @return The same response node (mutated), or the original if no changes needed
     */
    public JsonNode apply(JsonNode response, SchemaVersion clientVersion, SchemaVersion currentVersion) {
        if (clientVersion.compareTo(currentVersion) >= 0) {
            return response;
        }

        Set<String> unsupportedFields = registry.getFieldsIntroducedAfter(clientVersion);
        Map<String, Set<String>> unsupportedEnums = registry.getEnumValuesIntroducedAfter(clientVersion);

        if (unsupportedFields.isEmpty() && unsupportedEnums.isEmpty()) {
            log.debug("Client version {} is behind current {}, but no registered transformations needed",
                    clientVersion, currentVersion);
            return response;
        }

        log.info("Applying schema version filter: client={}, current={}, stripping {} fields, {} enum paths",
                clientVersion, currentVersion, unsupportedFields.size(), unsupportedEnums.size());

        stripFields(response, unsupportedFields, "");
        stripEnumValues(response, unsupportedEnums, "");

        return response;
    }

    private static final int MAX_RECURSION_DEPTH = 50;

    /**
     * Recursively strip fields from the JSON tree that match registered paths.
     */
    private void stripFields(JsonNode node, Set<String> unsupportedPaths, String currentPath) {
        stripFields(node, unsupportedPaths, currentPath, 0);
    }

    private void stripFields(JsonNode node, Set<String> unsupportedPaths, String currentPath, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            log.warn("stripFields: max recursion depth reached at path={}", currentPath);
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();

                if (unsupportedPaths.contains(fieldPath)) {
                    log.debug("Stripping unsupported field: {}", fieldPath);
                    fields.remove();
                    continue;
                }

                // Recurse
                stripFields(entry.getValue(), unsupportedPaths, fieldPath, depth + 1);
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            // For arrays, use [*] wildcard path notation
            String arrayItemPath = currentPath + "[*]";
            for (int i = 0; i < arr.size(); i++) {
                stripFields(arr.get(i), unsupportedPaths, arrayItemPath, depth + 1);
            }
        }
    }

    /**
     * Recursively find enum fields and null out values introduced after
     * the client's version.
     */
    private void stripEnumValues(JsonNode node, Map<String, Set<String>> unsupportedEnums, String currentPath) {
        stripEnumValues(node, unsupportedEnums, currentPath, 0);
    }

    private void stripEnumValues(JsonNode node, Map<String, Set<String>> unsupportedEnums, String currentPath, int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            log.warn("stripEnumValues: max recursion depth reached at path={}", currentPath);
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            java.util.List<String> toNullify = new java.util.ArrayList<>();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldPath = currentPath.isEmpty() ? entry.getKey() : currentPath + "." + entry.getKey();
                JsonNode value = entry.getValue();

                // Check if this field path has unsupported enum values
                Set<String> badValues = unsupportedEnums.get(fieldPath);
                if (badValues != null && value.isTextual() && badValues.contains(value.textValue())) {
                    log.debug("Nullifying unsupported enum value '{}' at path '{}'",
                            value.textValue(), fieldPath);
                    toNullify.add(entry.getKey());
                    continue;
                }

                // Recurse
                stripEnumValues(value, unsupportedEnums, fieldPath, depth + 1);
            }

            // Apply nullifications after iteration
            for (String key : toNullify) {
                obj.putNull(key);
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            String arrayItemPath = currentPath + "[*]";
            for (int i = 0; i < arr.size(); i++) {
                stripEnumValues(arr.get(i), unsupportedEnums, arrayItemPath, depth + 1);
            }
        }
    }
}
