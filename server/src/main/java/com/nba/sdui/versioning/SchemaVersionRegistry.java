package com.nba.sdui.versioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry of schema field and enum introductions by version.
 *
 * <p>Tracks which JSON fields and enum values were introduced in each schema
 * version. Used by {@link SchemaVersionFilter} to strip fields/values that a
 * client's declared version cannot decode.
 *
 * <p>For the prototype, this is a static in-memory registry. Production may
 * use build-time schema diffing to generate this automatically.
 *
 * <h3>Change classification:</h3>
 * <ul>
 *   <li><b>Additive (minor bump):</b> new optional fields, new section types
 *       (unknown types ignored per §10.2), new optional enum values for open shapes</li>
 *   <li><b>Breaking (major bump):</b> new required fields, new enum values for
 *       closed shapes, removed/renamed fields, type changes</li>
 * </ul>
 */
@Component
public class SchemaVersionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SchemaVersionRegistry.class);

    /**
     * Entry recording when a JSON path or enum value was introduced.
     */
    public record VersionedField(String jsonPath, SchemaVersion introducedIn) {}

    /**
     * Entry recording when an enum value for a specific field was introduced.
     */
    public record VersionedEnumValue(String jsonPath, String enumValue, SchemaVersion introducedIn) {}

    private final List<VersionedField> fieldIntroductions = new ArrayList<>();
    private final List<VersionedEnumValue> enumIntroductions = new ArrayList<>();

    public SchemaVersionRegistry() {
        // ── Schema 1.0 baseline ─────────────────────────────────────────
        // All existing fields as of the initial schema are version 1.0.
        // Only additions AFTER 1.0 need explicit registration.

        // ── Example registrations for future versions ───────────────────
        // When schema evolves to 1.1 or 2.0, register new fields here:
        //
        // registerField("sections[*].data.conditionalProperties", SchemaVersion.of(1, 1));
        // registerField("sections[*].data.accessibilityRole", SchemaVersion.of(1, 1));
        // registerEnumValue("sections[*].type", "SubscribeBanner", SchemaVersion.of(2, 0));
        //
        // These are illustrative — real registrations happen when schema changes land.
    }

    /**
     * Register a field path introduced in a given schema version.
     * Path uses dot-notation with [*] for array elements.
     */
    public void registerField(String jsonPath, SchemaVersion introducedIn) {
        fieldIntroductions.add(new VersionedField(jsonPath, introducedIn));
        log.debug("Registered field '{}' introduced in {}", jsonPath, introducedIn);
    }

    /**
     * Register an enum value for a specific field path, introduced in a given version.
     */
    public void registerEnumValue(String jsonPath, String enumValue, SchemaVersion introducedIn) {
        enumIntroductions.add(new VersionedEnumValue(jsonPath, enumValue, introducedIn));
        log.debug("Registered enum value '{}' at '{}' introduced in {}", enumValue, jsonPath, introducedIn);
    }

    /**
     * Get all field paths that were introduced AFTER the given version.
     * These fields should be stripped from responses to clients at that version.
     */
    public Set<String> getFieldsIntroducedAfter(SchemaVersion clientVersion) {
        Set<String> unsupported = new LinkedHashSet<>();
        for (VersionedField entry : fieldIntroductions) {
            if (!clientVersion.supports(entry.introducedIn())) {
                unsupported.add(entry.jsonPath());
            }
        }
        return unsupported;
    }

    /**
     * Get all enum values (by field path) that were introduced AFTER the given version.
     * These enum values should be stripped or defaulted in responses.
     */
    public Map<String, Set<String>> getEnumValuesIntroducedAfter(SchemaVersion clientVersion) {
        Map<String, Set<String>> unsupported = new LinkedHashMap<>();
        for (VersionedEnumValue entry : enumIntroductions) {
            if (!clientVersion.supports(entry.introducedIn())) {
                unsupported.computeIfAbsent(entry.jsonPath(), k -> new LinkedHashSet<>())
                           .add(entry.enumValue());
            }
        }
        return unsupported;
    }

    /**
     * Returns true if there are any schema changes between the client's version
     * and the current version that require response transformation.
     */
    public boolean requiresTransformation(SchemaVersion clientVersion, SchemaVersion currentVersion) {
        if (clientVersion.compareTo(currentVersion) >= 0) {
            return false;
        }
        return !getFieldsIntroducedAfter(clientVersion).isEmpty()
                || !getEnumValuesIntroducedAfter(clientVersion).isEmpty();
    }
}
