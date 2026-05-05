package com.nba.sdui.versioning;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for SDUI schema versioning.
 *
 * <p>Bound from {@code sdui.schema.*} in application.yml:
 * <pre>
 * sdui:
 *   schema:
 *     current-version: "1.0"
 *     min-supported-version: "1.0"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "sdui.schema")
public class SchemaVersionConfig {

    /**
     * The current (latest) schema version the server emits.
     * Clients at this version or newer receive unmodified payloads.
     */
    private String currentVersion = "1.0";

    /**
     * The minimum schema version the server will serve.
     * Clients below this version receive an upgrade-required signal.
     */
    private String minSupportedVersion = "1.0";

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getMinSupportedVersion() {
        return minSupportedVersion;
    }

    public void setMinSupportedVersion(String minSupportedVersion) {
        this.minSupportedVersion = minSupportedVersion;
    }

    /** Parsed current version for comparison. */
    public SchemaVersion currentVersion() {
        return SchemaVersion.parse(currentVersion);
    }

    /** Parsed minimum supported version for comparison. */
    public SchemaVersion minSupportedVersion() {
        return SchemaVersion.parse(minSupportedVersion);
    }
}
