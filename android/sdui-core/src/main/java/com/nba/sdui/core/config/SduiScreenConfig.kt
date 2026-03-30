package com.nba.sdui.core.config

/**
 * Library-level configuration for an SDUI screen.
 *
 * This is the contract between the consuming app and the sdui-core library.
 * Apps create instances of this class (typically via their own config presets)
 * and pass them to [SduiScreenViewModel] or [SduiScreenContent].
 *
 * This class must NOT reference app-level types (BuildConfig, app-specific enums, etc.).
 */
data class SduiScreenConfig(
    /** Base URL of the SDUI composition service (e.g. "http://10.0.2.2:8080") */
    val baseUrl: String,
    /** URL for Ably token refresh (e.g. "https://identity.nba.com/rttoken") */
    val ablyTokenUrl: String,
    /** Screen identifier sent to the server (e.g. game ID) */
    val screenId: String,
    /** Game state hint sent as query param (e.g. "live", "pre", "final") */
    val gameState: String = "live",
    /** Experiment assignments from Amplitude (experimentId → variant). */
    val experiments: Map<String, String> = emptyMap(),
    /** App version string (semver), e.g. "8.3.0" */
    val appVersion: String? = null,
    /** Device class: "phone", "tablet", "tv" */
    val deviceClass: String = "phone",
) {
    /**
     * Backwards-compatible variant accessor.
     * Resolves from the experiments map using a well-known experiment ID,
     * defaulting to "A" if no experiment assignment is present.
     */
    @Deprecated("Use experiments map directly", replaceWith = ReplaceWith("experiments"))
    val variant: String
        get() = experiments.values.firstOrNull() ?: "A"
}
