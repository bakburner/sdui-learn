package com.nba.sdui.app

import com.nba.sdui.core.config.SduiScreenConfig

/**
 * App-level configuration for SDUI screen routing.
 *
 * This is the **app-level** config.  Use [toScreenConfig] to convert to the
 * library-level [SduiScreenConfig] that sdui-core consumes.
 */
data class SduiConfig(
    val screenType: ScreenType,
    val gameId: String? = null,
    val variant: String = "A",
    val enableAbly: Boolean,
    val enablePolling: Boolean,
    val uri: String? = null
) {
    enum class ScreenType { SCOREBOARD, GAME_DETAIL, GENERIC }
    
    companion object {
        fun scoreboard(variant: String = "A") = SduiConfig(
            screenType = ScreenType.SCOREBOARD,
            variant = variant,
            enableAbly = false,
            enablePolling = true
        )

        fun gameDetail(gameId: String, variant: String = "A") = SduiConfig(
            screenType = ScreenType.GAME_DETAIL,
            gameId = gameId,
            variant = variant,
            enableAbly = true,
            enablePolling = true
        )

        /**
         * Factory for any server-driven screen.
         * No Ably/polling — the server's refreshPolicy drives data updates.
         */
        fun fromUri(uri: String) = SduiConfig(
            screenType = ScreenType.GENERIC,
            uri = uri,
            enableAbly = false,
            enablePolling = false
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
            screenId = gameId ?: uri ?: "scoreboard",
            gameState = "live",
            variant = variant,
            enableAbly = enableAbly,
            enablePolling = enablePolling
        )
}
