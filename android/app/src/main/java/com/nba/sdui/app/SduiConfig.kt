package com.nba.sdui.app

import com.nba.sdui.core.config.SduiScreenConfig

/**
 * App-level configuration for SDUI screen routing.
 *
 * Every screen is URI-driven — there is **no ScreenType enum**.
 * The server entirely owns layout, refresh policies (polling, Ably),
 * and variant definitions.  The client only needs the URI and
 * experiment assignments from Amplitude.
 *
 * Use [toScreenConfig] to convert to the library-level [SduiScreenConfig]
 * that sdui-core consumes.
 */
data class SduiConfig(
    /** The nba:// URI that identifies this screen. */
    val uri: String,
    /** Experiment assignments from Amplitude (experimentId → variant). */
    val experiments: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Generic server-driven screen.
         * All refresh policy / real-time configuration comes from the
         * server response — see Rule 9.
         */
        fun fromUri(uri: String, experiments: Map<String, String> = emptyMap()) = SduiConfig(
            uri = uri,
            experiments = experiments
        )
    }

    /**
     * Convert to the library-level [SduiScreenConfig].
     *
     * The app provides its BuildConfig values for base URLs; everything else
     * is derived from this config instance.
     */
    fun toScreenConfig(baseUrl: String, ablyTokenUrl: String): SduiScreenConfig =
        SduiScreenConfig(
            baseUrl = baseUrl,
            ablyTokenUrl = ablyTokenUrl,
            screenId = uri,
            gameState = "live",
            experiments = experiments
        )
}
