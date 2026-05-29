package com.nba.sdui.core.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.SduiRepository
import com.nba.sdui.core.models.generated.SduiModels
import com.nba.sdui.core.state.ActionHandler
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.state.SectionVisibilityTracker
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Generic SDUI Screen ViewModel — lives in the sdui-core library.
 *
 * Thin wrapper around [SduiScreenController]. All orchestration logic lives
 * in the controller so it can be tested with [kotlinx.coroutines.test.TestScope]
 * without Robolectric or the Android test runner.
 *
 * All screen loads go through a single path: [loadFromUri] / [loadFromEndpoint].
 * There are NO endpoint-specific methods — the server's URL contract is resolved
 * in [resolveEndpoint] (simple prefix swap) or supplied directly by navigation
 * payloads.
 *
 * App-level code should either use this ViewModel directly or create
 * a thin subclass/wrapper that maps app-specific config to [SduiScreenConfig].
 */
open class SduiScreenViewModel(
    private val config: SduiScreenConfig
) : ViewModel() {

    /** Origin for resolving root-relative wire asset URLs (e.g. `/sdui-demo/...`). */
    val wireAssetBaseUrl: String get() = config.baseUrl

    internal val controller = SduiScreenController(
        config = config,
        repository = SduiRepository(config.baseUrl, authorizationToken = config.authorizationToken),
        scope = viewModelScope,
    )

    // ── Public API — delegate everything to controller ────────────────
    val uiState: StateFlow<SduiScreenUiState> get() = controller.uiState
    val isRefreshing: StateFlow<Boolean> get() = controller.isRefreshing
    val screenState: StateFlow<Map<String, Any>> get() = controller.screenState
    val actionResults: SharedFlow<ActionHandler.ActionResult> get() = controller.actionResults
    val userMessage: SharedFlow<String> get() = controller.userMessage
    val staleSections: StateFlow<Set<String>> get() = controller.staleSections
    val visibilityTracker: SectionVisibilityTracker get() = controller.visibilityTracker
    val shellScreen: StateFlow<SduiModels?> get() = controller.shellScreen

    fun loadFromUri(uri: String, sectionId: String? = null) = controller.loadFromUri(uri, sectionId)
    fun loadFromEndpoint(endpoint: String, sectionId: String? = null) = controller.loadFromEndpoint(endpoint, sectionId)
    fun replaceCurrentScreen(endpoint: String, userParams: Map<String, String> = emptyMap()) = controller.replaceCurrentScreen(endpoint, userParams)
    fun refresh() = controller.refresh()
    fun handleAction(action: SduiAction) = controller.handleAction(action)
    fun handleActions(actions: List<SduiAction>) = controller.handleActions(actions)
    fun updateState(key: String, value: Any) = controller.updateState(key, value)
    fun onAppBackgrounded() = controller.onAppBackgrounded()
    fun onAppForegrounded() = controller.onAppForegrounded()

    companion object {
        /**
         * Convert nba:// URI to server endpoint path.
         *
         * Pure prefix swap — NO special-casing of individual screens.
         * The server owns all routing semantics.
         */
        fun resolveEndpoint(uri: String): String {
            val path = uri.removePrefix("nba://")
            return "/v1/sdui/screen/$path"
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller.onCleared()
    }
}

