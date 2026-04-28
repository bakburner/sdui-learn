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
    private String gameState;
    private String traceId;

    // ── Nested objects ─────────────────────────────────────────────────

    private Platform platform;
    private Device device;
    private Map<String, String> experiments = Collections.emptyMap();

    // ── Platform ───────────────────────────────────────────────────────

    public static class Platform {
        private String name;
        private String appVersion;
        private String osVersion;
        private String deviceClass;
        /**
         * Layout token axis: {@code phone}, {@code phone.landscape}, {@code tablet},
         * {@code web.narrow}, {@code web.wide}, {@code tv}. Clients must send a value
         * on every request; when absent, the server treats it as {@code phone} for resolution only.
         */
        private String formFactor;
        private Capabilities capabilities;

        public static class Capabilities {
            private boolean sse;
            private boolean onFocus;

            public boolean isSse() { return sse; }
            public void setSse(boolean sse) { this.sse = sse; }
            public boolean isOnFocus() { return onFocus; }
            public void setOnFocus(boolean onFocus) { this.onFocus = onFocus; }
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
        public String getOsVersion() { return osVersion; }
        public void setOsVersion(String osVersion) { this.osVersion = osVersion; }
        public String getDeviceClass() { return deviceClass; }
        public void setDeviceClass(String deviceClass) { this.deviceClass = deviceClass; }
        public String getFormFactor() { return formFactor; }
        public void setFormFactor(String formFactor) { this.formFactor = formFactor; }
        public Capabilities getCapabilities() { return capabilities; }
        public void setCapabilities(Capabilities capabilities) { this.capabilities = capabilities; }
    }

    // ── Device ─────────────────────────────────────────────────────────

    public static class Device {
        private String deviceId;
        private String zipCode;
        private String countryCode;
        private String region;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getZipCode() { return zipCode; }
        public void setZipCode(String zipCode) { this.zipCode = zipCode; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
    }

    // ── Accessors ──────────────────────────────────────────────────────

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public String getSchemaVersion() { return schemaVersion; }
    public void setSchemaVersion(String schemaVersion) { this.schemaVersion = schemaVersion; }

    public String getGameState() { return gameState; }
    public void setGameState(String gameState) { this.gameState = gameState; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public Device getDevice() { return device; }
    public void setDevice(Device device) { this.device = device; }

    public Map<String, String> getExperiments() { return experiments; }
    public void setExperiments(Map<String, String> experiments) {
        this.experiments = experiments != null ? experiments : Collections.emptyMap();
    }

    // ── Convenience ────────────────────────────────────────────────────

    /** Returns platform name, falling back to null if platform is unset. */
    public String getPlatformName() {
        return platform != null ? platform.getName() : null;
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
