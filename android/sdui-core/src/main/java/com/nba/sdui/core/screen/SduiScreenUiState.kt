package com.nba.sdui.core.screen

import com.nba.sdui.core.models.generated.Screen

/**
 * Generic UI state for any SDUI-powered screen.
 *
 * Library consumers observe this via [SduiScreenViewModel.uiState].
 */
sealed class SduiScreenUiState {
    /** Initial loading state — no data yet. */
    data object Loading : SduiScreenUiState()

    /** Screen data loaded successfully. */
    data class Success(val screen: Screen) : SduiScreenUiState()

    /** An error occurred while loading or refreshing. */
    data class Error(val message: String) : SduiScreenUiState()

    /**
     * The server signaled that this client's schema version is no longer
     * supported. The user should be prompted to update the app.
     */
    data class UpgradeRequired(val message: String) : SduiScreenUiState()
}
