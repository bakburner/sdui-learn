package com.nba.sdui.core.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.AblyChannelManager
import com.nba.sdui.core.data.DataBindingResolver
import com.nba.sdui.core.data.SduiRepository
import com.nba.sdui.core.models.SduiScreen
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.state.ActionHandler
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.state.StateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Generic SDUI Screen ViewModel — lives in the sdui-core library.
 *
 * Responsibilities (all screen-agnostic):
 *  - Fetch the SDUI response from the composition service
 *  - Manage screen-level [StateManager] state
 *  - Dispatch / execute actions via [ActionHandler]
 *  - Manage real-time data channels (Ably SSE, per-section polling)
 *  - Apply server-defined data bindings from real-time messages
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

    companion object {
        private const val TAG = "SduiScreenViewModel"

        /**
         * Convert nba:// URI to server endpoint path.
         *
         * Pure prefix swap — NO special-casing of individual screens.
         * The server owns all routing semantics.
         */
        fun resolveEndpoint(uri: String): String {
            val path = uri.removePrefix("nba://")
            return "/sdui/$path"
        }
    }

    // ── Dependencies (all from sdui-core) ────────────────────────────
    protected val repository = SduiRepository(config.baseUrl)
    protected val stateManager = StateManager()
    protected val actionHandler = ActionHandler()
    private val dataBindingResolver = DataBindingResolver()

    // Ably (only initialised when sse is required)
    private var ablyChannelManager: AblyChannelManager? = null
    private val ablyJobs = mutableMapOf<String, Job>()

    // ── Public state ─────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<SduiScreenUiState>(SduiScreenUiState.Loading)
    val uiState: StateFlow<SduiScreenUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val screenState: StateFlow<Map<String, Any>> = stateManager.state

    private val _actionResults = MutableSharedFlow<ActionHandler.ActionResult>()
    val actionResults: SharedFlow<ActionHandler.ActionResult> = _actionResults.asSharedFlow()

    // ── Internal bookkeeping ─────────────────────────────────────────
    private var currentScreen: SduiScreen? = null
    private var currentEndpoint: String? = null   // resolved server path — used for refresh / polling
    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        Log.d(TAG, "ViewModel INIT: screenId=${config.screenId}, baseUrl=${config.baseUrl}")
    }

    // ── Load / Refresh ───────────────────────────────────────────────

    /**
     * Load a screen from an nba:// URI.
     * This is the primary entry point for all screen loads.
     */
    fun loadFromUri(uri: String, sectionId: String? = null) {
        val endpoint = resolveEndpoint(uri)
        loadFromEndpoint(endpoint, sectionId)
    }

    /**
     * Load a screen from a pre-resolved server endpoint.
     * The endpoint already includes path and query parameters.
     */
    fun loadFromEndpoint(endpoint: String, sectionId: String? = null) {
        currentEndpoint = endpoint

        viewModelScope.launch {
            if (sectionId == null) {
                _uiState.value = SduiScreenUiState.Loading
            }

            try {
                Log.d(TAG, "Loading endpoint: $endpoint")
                val screen = repository.fetchScreen(endpoint, config.variant)
                applyScreen(screen)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load: $endpoint", e)
                _uiState.value = SduiScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Surgical section merge — fetches a refresh endpoint and merges the
     * returned sections into the current screen, preserving sections that
     * are not in the response (e.g. the form). State values echoed from
     * the server are applied to keep form selections consistent.
     */
    fun refreshSections(endpoint: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Surgical section refresh: $endpoint")
                val refreshScreen = repository.fetchScreen(endpoint, config.variant)

                // Merge echoed state from the response
                refreshScreen.state?.forEach { (key, value) ->
                    stateManager.setState(key, value)
                }

                val current = currentScreen ?: run {
                    // No current screen — fall back to full load
                    applyScreen(refreshScreen)
                    return@launch
                }

                // Build merged section list: keep existing, replace or append from response
                val mergedSections = current.sections.toMutableList()
                for (newSection in refreshScreen.sections) {
                    val idx = mergedSections.indexOfFirst { it.id == newSection.id }
                    if (idx >= 0) {
                        mergedSections[idx] = newSection
                    } else {
                        mergedSections.add(newSection)
                    }
                }

                val mergedScreen = current.copy(sections = mergedSections)
                currentScreen = mergedScreen
                _uiState.value = SduiScreenUiState.Success(mergedScreen)
            } catch (e: Exception) {
                Log.e(TAG, "Surgical section refresh failed: $endpoint", e)
                // Don't replace screen with error — keep current screen intact
            }
        }
    }

    /** Pull-to-refresh — re-fetches from the stored endpoint. */
    fun refresh() {
        val endpoint = currentEndpoint ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val screen = repository.fetchScreen(endpoint, config.variant)
                applyScreen(screen)
            } catch (e: Exception) {
                Log.e(TAG, "Refresh failed", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // ── Actions & State ──────────────────────────────────────────────

    fun handleAction(action: SduiAction) {
        viewModelScope.launch {
            Log.d(TAG, "Action: type=${action.type}, trigger=${action.trigger}")
            val result = actionHandler.handle(action, stateManager)
            _actionResults.emit(result)
        }
    }

    fun updateState(key: String, value: Any) {
        Log.d(TAG, "State update: $key = $value")
        stateManager.setState(key, value)
    }

    // ── Internal helpers ─────────────────────────────────────────────

    /**
     * Apply a fetched screen: seed state, emit success, start data channels.
     */
    private fun applyScreen(screen: SduiScreen) {
        currentScreen = screen
        screen.state?.forEach { (key, value) ->
            stateManager.setState(key, value)
        }
        Log.d(TAG, "Screen loaded: traceId=${screen.traceId}, sections=${screen.sections.size}")
        _uiState.value = SduiScreenUiState.Success(screen)

        // Defer data-channel bootstrap so the initial render is never blocked.
        // Polling and Ably are server-driven — always inspect refreshPolicy (Rule 9).
        viewModelScope.launch {
            setupPolling(screen)
            setupAbly(screen)
        }
    }

    // ── Polling ──────────────────────────────────────────────────────

    private fun setupPolling(screen: SduiScreen) {
        stopAllPolling()

        screen.sections.forEach { section ->
            val policy = section.refreshPolicy
            val policyIntervalMs = policy?.intervalMs
            if (policy?.type == "poll" && policyIntervalMs != null) {
                val intervalMs = policyIntervalMs.toLong()
                val pollUrl = policy.url
                val dataPath = policy.dataPath

                if (pollUrl != null) {
                    Log.i(TAG, "DIRECT poll: section='${section.id}' url=$pollUrl every ${intervalMs}ms")
                } else {
                    Log.i(TAG, "SDUI poll: section='${section.id}' every ${intervalMs}ms")
                }

                pollingJobs[section.id] = viewModelScope.launch {
                    while (isActive) {
                        delay(intervalMs)
                        try {
                            if (pollUrl != null) {
                                val data = repository.fetchRawJson(pollUrl, dataPath)
                                updateSectionData(section.id, data)
                            } else {
                                val endpoint = currentEndpoint ?: continue
                                val updated = repository.fetchScreen(endpoint, config.variant)
                                applyScreen(updated)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Poll failed for section: ${section.id}", e)
                        }
                    }
                }
            }
        }
        if (pollingJobs.isNotEmpty()) Log.i(TAG, "Active polls: ${pollingJobs.keys}")
    }

    private fun updateSectionData(sectionId: String, newData: Map<String, Any>) {
        val screen = currentScreen ?: return
        Log.d(TAG, "Section data update '$sectionId': keys=${newData.keys}")
        _uiState.value = SduiScreenUiState.Success(screen)
    }

    private fun stopAllPolling() {
        pollingJobs.forEach { (id, job) ->
            Log.d(TAG, "Stop polling: $id")
            job.cancel()
        }
        pollingJobs.clear()
    }

    // ── Ably ─────────────────────────────────────────────────────────

    private fun setupAbly(screen: SduiScreen) {
        // Only initialise Ably when at least one section declares an SSE policy.
        val sseSections = screen.sections.filter { s ->
            s.refreshPolicy?.type == "sse" && !s.refreshPolicy?.channel.isNullOrBlank()
        }
        if (sseSections.isEmpty()) return

        viewModelScope.launch {
            try {
                if (ablyChannelManager == null) {
                    ablyChannelManager = AblyChannelManager(config.ablyTokenUrl)
                    ablyChannelManager?.initialize()
                    Log.i(TAG, "Ably initialised")
                }
                sseSections.forEach { section ->
                    subscribeToChannel(section, section.refreshPolicy!!.channel!!)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ably setup failed", e)
            }
        }
    }

    /**
     * Subscribe to an Ably channel and apply server-defined data bindings
     * from opaque incoming messages.  No field-level knowledge of the
     * message content exists here — DataBindingResolver does the mapping.
     */
    private fun subscribeToChannel(section: SduiSection, channel: String) {
        ablyJobs[section.id]?.cancel()
        Log.i(TAG, "Subscribe Ably: channel='$channel' section='${section.id}'")
        ablyJobs[section.id] = viewModelScope.launch {
            try {
                ablyChannelManager?.subscribeToChannel(channel)?.collect { message ->
                    Log.d(TAG, "Ably update for ${section.id}: keys=${message.keys}")
                    val dataBinding = section.dataBinding
                    if (dataBinding != null) {
                        val currentData = currentScreen?.sections
                            ?.find { it.id == section.id }?.data ?: return@collect
                        val updatedData = dataBindingResolver.applyBindings(
                            currentData, message, dataBinding, currentScreen?.traceId
                        )
                        updateSectionInScreen(section.id, updatedData)
                    } else {
                        Log.w(TAG, "No dataBinding config for section ${section.id} — message dropped")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ably error for ${section.id}", e)
            }
        }
    }

    /**
     * Replace section data in the current screen and emit a new Success state.
     */
    private fun updateSectionInScreen(sectionId: String, updatedData: Map<String, Any?>) {
        val screen = currentScreen ?: return
        val updatedSections = screen.sections.map { s ->
            if (s.id == sectionId) s.copy(data = updatedData) else s
        }
        val updated = screen.copy(sections = updatedSections)
        currentScreen = updated
        _uiState.value = SduiScreenUiState.Success(updated)
        Log.d(TAG, "Data binding applied to $sectionId")
    }

    private fun stopAbly() {
        ablyJobs.forEach { (id, job) ->
            Log.d(TAG, "Stop Ably: $id")
            job.cancel()
        }
        ablyJobs.clear()
        ablyChannelManager?.disconnect()
        ablyChannelManager = null
    }

    // ── Cleanup ──────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        stopAllPolling()
        stopAbly()
        Log.d(TAG, "Cleared — data channels cleaned up")
    }
}
