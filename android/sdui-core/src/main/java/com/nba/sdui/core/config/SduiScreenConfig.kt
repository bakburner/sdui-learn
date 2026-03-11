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
    /** Server variant for A/B testing (e.g. "A", "B", "C", "D") */
    val variant: String = "A",
)
