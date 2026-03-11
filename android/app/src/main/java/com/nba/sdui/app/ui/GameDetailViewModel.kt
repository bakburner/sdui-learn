package com.nba.sdui.app.ui

import com.nba.sdui.app.SduiConfig
import com.nba.sdui.core.screen.SduiScreenViewModel

/**
 * App-level ViewModel for any SDUI screen.
 *
 * This is a thin wrapper around [SduiScreenViewModel] from the sdui-core
 * library.  Its only job is to map the app-specific [SduiConfig] (which
 * knows about BuildConfig, etc.) into the library-level [SduiScreenConfig]
 * that the base ViewModel expects.
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
     * Load (or reload) the screen identified by [uri].
     *
     * @param uri        nba:// URI, e.g. "nba://game/0042300102" or "nba://for-you"
     * @param sectionId  optional — reload only one section
     */
    fun load(uri: String, sectionId: String? = null) {
        loadFromUri(uri = uri, sectionId = sectionId)
    }
}
