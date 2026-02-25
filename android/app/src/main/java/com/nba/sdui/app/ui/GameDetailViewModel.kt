package com.nba.sdui.app.ui

import com.nba.sdui.app.SduiConfig
import com.nba.sdui.core.screen.SduiScreenViewModel

/**
 * App-level ViewModel for the Game Detail screen.
 *
 * This is a thin wrapper around [SduiScreenViewModel] from the sdui-core
 * library.  Its only job is to map the app-specific [SduiConfig] (which
 * knows about BuildConfig, Mode enum, etc.) into the library-level
 * [SduiScreenConfig] that the base ViewModel expects.
 *
 * All orchestration — fetching, polling, Ably, state management, action
 * handling — is handled by the library.
 */
class GameDetailViewModel(
    baseUrl: String,
    ablyTokenUrl: String,
    config: SduiConfig
) : SduiScreenViewModel(
    config = config.toScreenConfig(baseUrl, ablyTokenUrl)
) {

    /**
     * Convenience: load using the original gameId.
     */
    fun loadGameDetail(gameId: String, sectionId: String? = null, gameState: String = "live") {
        loadScreen(screenId = gameId, sectionId = sectionId, gameState = gameState)
    }
}
