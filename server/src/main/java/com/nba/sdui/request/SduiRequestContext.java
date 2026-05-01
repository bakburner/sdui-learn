package com.nba.sdui.request;

import java.util.Collections;
import java.util.Map;

/**
 * Typed request envelope for all SDUI composition requests.
 *
 * <p>Populated from bracket-notation query params on GET requests
 * (e.g. {@code platform[name]=android&device[countryCode]=US})
 * or from a JSON POST body with the same shape.
 *
 * <p>Fields mirror the contract in {@code plan-request-transport.md}.
 */
public class SduiRequestContext {

    // ── Top-level scalars ──────────────────────────────────────────────

    private String locale = "en";
    private String schemaVersion = "1.0";
    private String traceId;

    // ── Nested objects ─────────────────────────────────────────────────

    private Platform platform;
    private Device device;
    private String resolvedCountry;
    private String resolvedMarketCohort;
    private Map<String, String> experiments = Collections.emptyMap();

    // ── Platform ───────────────────────────────────────────────────────

    public static class Platform {
        private String deviceClass;
        private Capabilities capabilities;

        /**
         * Per-boolean capability flags.
         *
         * <p><strong>Scalability concern:</strong> individual boolean flags fragment CDN
         * cache keys — every unique combination of {@code sse × onFocus × …} produces a
         * distinct cache entry. Replace with a small, server-defined <em>platform tier</em>
         * (e.g. {@code "tier:full"}, {@code "tier:passive"}) that the server maps to a
         * capability set. Tier resolution can happen at the edge or in the client.
         */
        public static class Capabilities {
            private boolean sse;
            private boolean onFocus;

            public boolean isSse() { return sse; }
            public void setSse(boolean sse) { this.sse = sse; }
            public boolean isOnFocus() { return onFocus; }
            public void setOnFocus(boolean onFocus) { this.onFocus = onFocus; }
        }

        public String getDeviceClass() { return deviceClass; }
        public void setDeviceClass(String deviceClass) { this.deviceClass = deviceClass; }
        public Capabilities getCapabilities() { return capabilities; }
        public void setCapabilities(Capabilities capabilities) { this.capabilities = capabilities; }
    }

    // ── Device ─────────────────────────────────────────────────────────

    public static class Device {
        /**
         * Per-device identifier. Transported via {@code X-Device-Id} header
         * (not in query params or JSON body) because per-device cardinality
         * would fragment CDN cache keys. Populated by
         * {@link com.nba.sdui.request.BracketParamResolver#applyHeaderFallbacks}.
         */
        private String deviceId;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    // ── Accessors ──────────────────────────────────────────────────────

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    /** Resolved country from edge worker (X-Resolved-Country header). */
    public String getResolvedCountry() { return resolvedCountry; }
    public void setResolvedCountry(String resolvedCountry) { this.resolvedCountry = resolvedCountry; }

    /** Resolved market cohort from edge worker (X-Resolved-Market-Cohort header). */
    public String getResolvedMarketCohort() { return resolvedMarketCohort; }
    public void setResolvedMarketCohort(String resolvedMarketCohort) { this.resolvedMarketCohort = resolvedMarketCohort; }

    public Map<String, String> getExperiments() { return experiments; }
    public void setExperiments(Map<String, String> experiments) {
        this.experiments = experiments != null ? experiments : Collections.emptyMap();
    }

    /**
     * Resolve the effective variant for composition.
     *
     * <p>Looks up {@code experimentId} in the experiments map.
     * Returns the assigned variant if present, otherwise the provided default.
     */
    public String resolveVariant(String experimentId, String defaultVariant) {
        String variant = experiments.get(experimentId);
        return variant != null ? variant : defaultVariant;
    }
}
