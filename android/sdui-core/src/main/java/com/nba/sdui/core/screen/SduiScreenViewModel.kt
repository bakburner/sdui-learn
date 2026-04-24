package com.nba.sdui.core.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.AblyChannelManager
import com.nba.sdui.core.data.DataBindingResolver
import com.nba.sdui.core.data.SduiRepository
import com.nba.sdui.core.models.generated.Data
import com.nba.sdui.core.models.generated.RefreshType
import com.nba.sdui.core.models.generated.SduiModels
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.models.generated.mapper
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import com.nba.sdui.core.state.ActionHandler
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.state.SectionVisibilityTracker
import com.nba.sdui.core.state.StateManager
import io.ably.lib.realtime.ConnectionState
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
        private const val POLL_FAILURE_THRESHOLD = 2
        private const val MAX_BACKOFF_MS = 30_000L
        private const val SSE_STALE_DELAY_MS = 10_000L

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

    /** Tracks which sections are near the viewport for visibility-gated refresh. */
    val visibilityTracker = SectionVisibilityTracker()

    /** True when the app is in the foreground. Gates ALL refresh activity. */
    private val _isAppForeground = MutableStateFlow(true)

    /** Buffer of latest SSE messages per section, applied on visibility resume. */
    private val sseMessageBuffer = mutableMapOf<String, Map<String, Any?>>()

    /**
     * Build the request envelope from config.
     * Called before every fetchScreen to include current platform/device/experiment context.
     */
    private fun buildEnvelope(): RequestEnvelopeBuilder =
        RequestEnvelopeBuilder()
            .experiments(config.experiments)
            .appVersion(config.appVersion ?: "1.0.0")
            .deviceClass(config.deviceClass)

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

    /** Emits a one-shot message for Snackbar/toast display (navigate error feedback). */
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    /** Set of section IDs currently marked as stale (refresh failure). */
    private val _staleSections = MutableStateFlow<Set<String>>(emptySet())
    val staleSections: StateFlow<Set<String>> = _staleSections.asStateFlow()

    // ── Internal bookkeeping ─────────────────────────────────────────
    private var currentScreen: SduiModels? = null
    private var currentEndpoint: String? = null   // resolved server path — used for refresh / polling
    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        Log.d(TAG, "ViewModel INIT: screenId=${config.screenId}, baseUrl=${config.baseUrl}")
    }

    // ── Lifecycle ────────────────────────────────────────────────────

    /** Call from ProcessLifecycleOwner ON_STOP — pauses all refresh activity. */
    fun onAppBackgrounded() {
        Log.d(TAG, "App backgrounded — pausing all refresh")
        _isAppForeground.value = false
    }

    /** Call from ProcessLifecycleOwner ON_START — resumes refresh activity. */
    fun onAppForegrounded() {
        Log.d(TAG, "App foregrounded — resuming refresh")
        _isAppForeground.value = true
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
        val isNewScreen = endpoint != currentEndpoint
        currentEndpoint = endpoint

        if (isNewScreen) {
            stopAbly()
            stopAllPolling()
        }

        viewModelScope.launch {
            if (sectionId == null) {
                _uiState.value = SduiScreenUiState.Loading
            }

            try {
                Log.d(TAG, "Loading endpoint: $endpoint")
                val screen = repository.fetchScreen(endpoint, buildEnvelope())
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
                val refreshScreen = repository.fetchScreen(endpoint, buildEnvelope())

                // Merge echoed state from the response
                refreshScreen.state?.forEach { (key, value) ->
                    if (value != null) {
                        stateManager.setState(key, value)
                    } else {
                        stateManager.removeState(key)
                    }
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
                val screen = repository.fetchScreen(endpoint, buildEnvelope())
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
        handleActions(listOf(action))
    }

    fun handleActions(actions: List<SduiAction>) {
        viewModelScope.launch {
            Log.d(TAG, "Executing action sequence: ${actions.size} action(s)")
            val seqResult = actionHandler.executeSequence(actions, stateManager)

            for (result in seqResult.results) {
                _actionResults.emit(result)

                when (result) {
                    is ActionHandler.ActionResult.NavigateError -> {
                        // Show server-provided message or generic fallback
                        val msg = result.feedback?.message ?: "Unable to open page"
                        if (result.feedback?.style == "inline") {
                            Log.w(TAG, "failureFeedback.style=inline is decoded but not hosted inline on Android; emitting user message")
                        }
                        _userMessage.emit(msg)
                    }
                    is ActionHandler.ActionResult.RefreshStale -> {
                        val id = result.sectionId
                        if (id != null) {
                            _staleSections.value = _staleSections.value + id
                        }
                    }
                    else -> { /* no additional UI side effects */ }
                }
            }
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
    private fun applyScreen(screen: SduiModels) {
        currentScreen = screen
        screen.state?.forEach { (key, value) ->
            if (value != null) {
                stateManager.setState(key, value)
            } else {
                stateManager.removeState(key)
            }
        }
        Log.d(TAG, "Screen loaded: traceId=${screen.traceID}, sections=${screen.sections.size}")
        _uiState.value = SduiScreenUiState.Success(screen)

        // Defer data-channel bootstrap so the initial render is never blocked.
        // Polling and Ably are server-driven — always inspect refreshPolicy.
        viewModelScope.launch {
            setupPolling(screen)
            setupAbly(screen)
        }
    }

    // ── Polling ──────────────────────────────────────────────────────

    private val pollFailureCounts = mutableMapOf<String, Int>()

    private fun setupPolling(screen: SduiModels) {
        stopAllPolling()

        screen.sections.forEach { section ->
            val policy = section.refreshPolicy
            val policyIntervalMs = policy?.intervalMS
            if (policy?.type == RefreshType.Poll && policyIntervalMs != null) {
                val baseIntervalMs: Long = policyIntervalMs
                val pollUrl = policy.url
                val dataPath = policy.dataPath
                val shouldPause = policy.pauseWhenOffScreen ?: true

                if (pollUrl != null) {
                    Log.i(TAG, "DIRECT poll: section='${section.id}' url=$pollUrl every ${baseIntervalMs}ms pauseWhenOffScreen=$shouldPause")
                } else {
                    Log.i(TAG, "SDUI poll: section='${section.id}' every ${baseIntervalMs}ms pauseWhenOffScreen=$shouldPause")
                }

                pollFailureCounts[section.id] = 0

                pollingJobs[section.id] = viewModelScope.launch {
                    var currentIntervalMs: Long = baseIntervalMs
                    while (isActive) {
                        // Gate: wait for app foreground
                        _isAppForeground.first { it }

                        // Gate: wait for section to be near viewport (if pauseWhenOffScreen)
                        if (shouldPause) {
                            visibilityTracker.awaitNearViewport(section.id)
                        }

                        delay(currentIntervalMs)
                        try {
                            if (pollUrl != null) {
                                val data = repository.fetchRawJson(pollUrl, dataPath)
                                updateSectionData(section.id, data)
                            } else {
                                val endpoint = currentEndpoint ?: continue
                                val updated = repository.fetchScreen(endpoint, buildEnvelope())
                                applyScreen(updated)
                            }
                            // Success — reset failure count and backoff
                            pollFailureCounts[section.id] = 0
                            currentIntervalMs = baseIntervalMs
                            _staleSections.value = _staleSections.value - section.id
                        } catch (e: Exception) {
                            Log.e(TAG, "Poll failed for section: ${section.id}", e)
                            val failures = (pollFailureCounts[section.id] ?: 0) + 1
                            pollFailureCounts[section.id] = failures
                            // Exponential backoff: double on failure, cap at MAX_BACKOFF_MS
                            currentIntervalMs = (currentIntervalMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                            if (failures >= POLL_FAILURE_THRESHOLD) {
                                _staleSections.value = _staleSections.value + section.id
                            }
                        }
                    }
                }
            }
        }
        if (pollingJobs.isNotEmpty()) Log.i(TAG, "Active polls: ${pollingJobs.keys}")
    }

    /**
     * Apply a direct-URL poll payload to a section's data. Mirrors the
     * Ably handler so the poll and real-time paths are symmetrical, and
     * matches the web `LiveSectionWrapper` two-step behaviour:
     *
     * 1. If the section declares a `dataBinding`, route incoming fields
     *    through it (preserves every key the binding does not touch,
     *    including `ui` for AtomicComposite sections).
     * 2. Otherwise shallow-merge the incoming payload over the current
     *    data. Keys not present in the incoming payload survive — so
     *    `data.ui` is preserved for unconfigured AtomicComposite polls
     *    instead of being wiped out like the old no-op path did.
     */
    private fun updateSectionData(sectionId: String, newData: Map<String, Any>) {
        val screen = currentScreen ?: return
        val section = screen.sections.find { it.id == sectionId } ?: return
        val currentData: Map<String, Any?> = toMap(section.data)
        val dataBinding = section.dataBinding
        val merged: Map<String, Any?> = if (dataBinding != null) {
            dataBindingResolver.applyBindings(
                currentData, newData, dataBinding, screen.traceID,
                section.stringTable, sectionId
            )
        } else {
            currentData + newData
        }
        updateSectionInScreen(sectionId, toData(merged))
    }

    private fun stopAllPolling() {
        pollingJobs.forEach { (id, job) ->
            Log.d(TAG, "Stop polling: $id")
            job.cancel()
        }
        pollingJobs.clear()
    }

    // ── Ably ─────────────────────────────────────────────────────────

    private var sseStaleJob: Job? = null

    private fun setupAbly(screen: SduiModels) {
        val sseSections = screen.sections.filter { s ->
            s.refreshPolicy?.type == RefreshType.SSE && !s.refreshPolicy?.channel.isNullOrBlank()
        }
        if (sseSections.isEmpty()) return

        val sseSectionIds = sseSections.map { it.id }.toSet()

        if (ablyChannelManager == null) {
            ablyChannelManager = AblyChannelManager(config.ablyTokenUrl)
            ablyChannelManager?.onConnectionStateChange = { connectionState ->
                when (connectionState) {
                    ConnectionState.disconnected, ConnectionState.suspended, ConnectionState.failed -> {
                        // Start countdown — if still disconnected after SSE_STALE_DELAY_MS, mark SSE sections stale
                        if (sseStaleJob?.isActive != true) {
                            sseStaleJob = viewModelScope.launch {
                                delay(SSE_STALE_DELAY_MS)
                                _staleSections.value = _staleSections.value + sseSectionIds
                                Log.w(TAG, "Ably disconnected for ${SSE_STALE_DELAY_MS}ms — marking SSE sections stale: $sseSectionIds")
                            }
                        }
                    }
                    ConnectionState.connected -> {
                        sseStaleJob?.cancel()
                        sseStaleJob = null
                        // Don't clear staleness here — wait for actual message receipt in subscribeToChannel
                    }
                    else -> {}
                }
            }
            ablyChannelManager?.initialize()
        }

        sseSections.forEach { section ->
            if (ablyJobs[section.id]?.isActive == true) return@forEach
            subscribeToChannel(section, section.refreshPolicy!!.channel!!)
        }
    }

    /**
     * Subscribe to an Ably channel and apply server-defined data bindings
     * from opaque incoming messages.  No field-level knowledge of the
     * message content exists here — DataBindingResolver does the mapping.
     */
    private fun subscribeToChannel(section: Section, channel: String) {
        ablyJobs[section.id]?.cancel()
        val shouldPause = section.refreshPolicy?.pauseWhenOffScreen ?: true
        Log.i(TAG, "Subscribe Ably: channel='$channel' section='${section.id}' pauseWhenOffScreen=$shouldPause")
        ablyJobs[section.id] = viewModelScope.launch {
            try {
                ablyChannelManager?.subscribeToChannel(channel)?.collect { message ->
                    Log.d(TAG, "Ably update for ${section.id}: keys=${message.keys}")
                    // Successful message — clear section staleness
                    _staleSections.value = _staleSections.value - section.id

                    // Gate: skip applying bindings if app is backgrounded or section is off-screen
                    val isForeground = _isAppForeground.value
                    val isVisible = !shouldPause || visibilityTracker.isNearViewport(section.id)

                    if (!isForeground || !isVisible) {
                        // Buffer the latest message — will be applied on resume
                        sseMessageBuffer[section.id] = message
                        Log.d(TAG, "SSE message buffered for ${section.id} (fg=$isForeground, vis=$isVisible)")
                        return@collect
                    }

                    applyAblyMessage(section, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ably error for ${section.id}", e)
            }
        }

        // Launch a coroutine that watches visibility changes and applies buffered messages
        if (shouldPause) {
            viewModelScope.launch {
                visibilityTracker.visibleSections.collect { visibleSet ->
                    if (section.id in visibleSet) {
                        val buffered = sseMessageBuffer.remove(section.id)
                        if (buffered != null) {
                            Log.d(TAG, "Applying buffered SSE message for ${section.id}")
                            applyAblyMessage(section, buffered)
                        }
                    }
                }
            }
        }
    }

    private fun applyAblyMessage(section: Section, message: Map<String, Any?>) {
        val dataBinding = section.dataBinding
        if (dataBinding != null) {
            val currentData = currentScreen?.sections
                ?.find { it.id == section.id }?.data
                ?.let { toMap(it) } ?: return
            val updatedData = dataBindingResolver.applyBindings(
                currentData, message, dataBinding, currentScreen?.traceID,
                section.stringTable, section.id
            )
            updateSectionInScreen(section.id, toData(updatedData))
        } else {
            Log.w(TAG, "No dataBinding config for section ${section.id} — message dropped")
        }
    }

    /**
     * Replace section data in the current screen and emit a new Success state.
     */
    private fun updateSectionInScreen(sectionId: String, updatedData: Data?) {
        val screen = currentScreen ?: return
        val updatedSections = screen.sections.map { s ->
            if (s.id == sectionId) s.copy(data = updatedData) else s
        }
        val updated = screen.copy(sections = updatedSections)
        currentScreen = updated
        _uiState.value = SduiScreenUiState.Success(updated)
        Log.d(TAG, "Data binding applied to $sectionId")
    }

    private fun toMap(data: Data?): Map<String, Any?> {
        if (data == null) return emptyMap()
        @Suppress("UNCHECKED_CAST")
        return mapper.convertValue(data, Map::class.java) as Map<String, Any?>
    }

    private fun toData(data: Map<String, Any?>): Data? {
        return mapper.convertValue(data, Data::class.java)
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
