package com.nba.sdui.core.screen

import android.util.Log
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.AblyChannelManager
import com.nba.sdui.core.data.DataBindingResolver
import com.nba.sdui.core.data.SchemaVersionMismatchException
import com.nba.sdui.core.data.SduiException
import com.nba.sdui.core.data.SectionNotFoundException
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Plain Kotlin class that owns all SDUI screen orchestration logic.
 *
 * Extracted from [SduiScreenViewModel] so the logic is testable with
 * [kotlinx.coroutines.test.TestScope] without Robolectric or the Android
 * test runner. The ViewModel is a thin delegating wrapper around this class.
 *
 * @param config         Screen-level configuration (base URL, experiments, etc.).
 * @param repository     Injectable network/cache gateway; tests pass a fake.
 * @param scope          Injectable coroutine scope; tests pass TestScope().
 * @param stateManager   Shared screen state bag; injectable for testing.
 * @param actionHandler  Action execution engine; injectable for testing.
 */
internal class SduiScreenController(
    private val config: SduiScreenConfig,
    internal val repository: SduiRepository,
    private val scope: CoroutineScope,
    private val stateManager: StateManager = StateManager(),
    private val actionHandler: ActionHandler = ActionHandler(),
) {

    companion object {
        private const val TAG = "SduiScreenController"
        private const val POLL_FAILURE_THRESHOLD = 2
        private const val MAX_BACKOFF_MS = 30_000L
        private const val SSE_STALE_DELAY_MS = 10_000L
        private const val SSE_BUFFER_TTL_MS = 5 * 60 * 1000L
    }

    private val dataBindingResolver = DataBindingResolver()

    /** Tracks which sections are near the viewport for visibility-gated refresh. */
    val visibilityTracker = SectionVisibilityTracker()

    /** True when the app is in the foreground. Gates ALL refresh activity. */
    private val _isAppForeground = MutableStateFlow(true)

    private data class BufferedSseEntry(val message: Map<String, Any?>, val receivedAtMs: Long)

    /** Buffer of latest SSE messages per section, applied on visibility resume. */
    private val sseMessageBuffer = mutableMapOf<String, BufferedSseEntry>()

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
    private val _currentScreen = MutableStateFlow<SduiModels?>(null)
    private var currentScreen: SduiModels?
        get() = _currentScreen.value
        set(value) { _currentScreen.value = value }
    private var currentEndpoint: String? = null   // resolved server path — used for refresh / polling
    private var currentUserParams: Map<String, String> = emptyMap()  // last user-supplied filter params — replayed on pull-to-refresh and poll
    private var screenLevelPollJob: Job? = null

    /**
     * Last successfully loaded screen payload, exposed as a [StateFlow] so the
     * navigation shell recomposes the moment the very first load completes
     * (the value goes from null to non-null). Kept when a later fetch fails so
     * shell navigation (bottom bar) and [parentUri] remain available for escape.
     */
    val shellScreen: StateFlow<SduiModels?> = _currentScreen.asStateFlow()

    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        Log.d(TAG, "Controller INIT: screenId=${config.screenId}, baseUrl=${config.baseUrl}")
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
        flushSseBufferBatched()
    }

    // ── Load / Refresh ───────────────────────────────────────────────

    /**
     * Load a screen from an nba:// URI.
     * This is the primary entry point for all screen loads.
     */
    fun loadFromUri(uri: String, sectionId: String? = null) {
        val path = uri.removePrefix("nba://")
        val endpoint = "/v1/sdui/screen/$path"
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
            currentUserParams = emptyMap()
            stopAbly()
            stopAllPolling()
            screenLevelPollJob?.cancel()
            screenLevelPollJob = null
        }

        scope.launch {
            if (sectionId == null) {
                _uiState.value = SduiScreenUiState.Loading
            }

            try {
                Log.d(TAG, "Loading endpoint: $endpoint")
                val screen = repository.fetchScreen(endpoint, buildEnvelope())
                applyScreen(screen)
            } catch (e: SchemaVersionMismatchException) {
                Log.w(TAG, "Schema version mismatch — upgrade required", e)
                _uiState.value = SduiScreenUiState.UpgradeRequired(
                    e.message ?: "Please update the app to continue."
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load: $endpoint", e)
                _uiState.value = SduiScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Screen-channel full replace — fetches the supplied endpoint with
     * user-bound filter params via the canonical `fetchScreen` transport
     * (envelope + GET/POST length fallback + `X-Trace-Id` propagation) and
     * fully replaces the current screen with the response.
     *
     * This is the single entry point for all screen-channel refetches that
     * carry user parameters (date pickers, form submits, filter changes).
     * Pull-to-refresh and screen-level poll ticks use [refresh] /
     * [startScreenLevelPoll] which also route through the screen channel
     * via [currentEndpoint].
     *
     * The response is always treated as the new authoritative screen state:
     * its section roster fully replaces the current one. Sections that were
     * on the previous screen but are absent from the response are removed
     * (e.g. an `upcoming_games` section that no longer applies when the
     * date picker moves to a day with only completed games).
     *
     * If the server returns a payload whose [id] does not match the current
     * screen's id, that is a contract bug — a parameterized refresh must
     * return the same screen it was invoked against, not a different one.
     * The mismatched response is dropped and the current screen is left
     * untouched.
     *
     * A successful replace resets the screen-level poll timer so that
     * out-of-band fetches don't cause a double-fetch one tick later.
     */
    fun replaceCurrentScreen(
        endpoint: String,
        userParams: Map<String, String> = emptyMap()
    ) {
        scope.launch {
            try {
                Log.d(TAG, "replaceCurrentScreen: endpoint=$endpoint params=$userParams")
                val refreshScreen = repository.fetchScreen(
                    path = endpoint,
                    envelope = buildEnvelope(),
                    userParams = userParams,
                    traceIdOverride = currentScreen?.traceID
                )

                val current = currentScreen
                if (current != null && refreshScreen.id != current.id) {
                    Log.w(
                        TAG,
                        "Refresh response id='${refreshScreen.id}' does not match current screen " +
                            "id='${current.id}'; dropping response (parameterized refresh must return " +
                            "the same screen it was invoked against)."
                    )
                    return@launch
                }

                currentUserParams = userParams
                applyScreen(refreshScreen)
            } catch (e: Exception) {
                Log.e(TAG, "replaceCurrentScreen failed: $endpoint", e)
            }
        }
    }

    /** Pull-to-refresh — re-fetches from the stored endpoint with current user params. */
    fun refresh() {
        val endpoint = currentEndpoint ?: return
        scope.launch {
            _isRefreshing.value = true
            try {
                val screen = repository.fetchScreen(
                    path = endpoint,
                    envelope = buildEnvelope(),
                    userParams = currentUserParams
                )
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
        scope.launch {
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
                        val id = result.target
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
        val previous = currentScreen
        val oldIds = previous?.sections?.map { it.id }?.toSet().orEmpty()
        val newIds = screen.sections.map { it.id }.toSet()
        for (id in oldIds - newIds) {
            dataBindingResolver.resetCounters(id)
        }
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
        scope.launch {
            setupPolling(screen)
            setupAbly(screen)
        }

        val defaultPolicy = screen.defaultRefreshPolicy
        val intervalMs = defaultPolicy?.intervalMS?.toLong()
        if (defaultPolicy?.type == RefreshType.Poll && intervalMs != null) {
            startScreenLevelPoll(intervalMs)
        } else {
            screenLevelPollJob?.cancel()
            screenLevelPollJob = null
        }
    }

    // ── Polling ──────────────────────────────────────────────────────

    private val pollFailureCounts = mutableMapOf<String, Int>()

    private fun startScreenLevelPoll(intervalMs: Long) {
        screenLevelPollJob?.cancel()
        screenLevelPollJob = scope.launch {
            while (isActive) {
                _isAppForeground.first { it }
                delay(intervalMs)
                try {
                    val endpoint = currentEndpoint ?: continue
                    val updated = repository.fetchScreen(
                        path = endpoint,
                        envelope = buildEnvelope(),
                        userParams = currentUserParams
                    )
                    applyScreen(updated)
                } catch (e: Exception) {
                    Log.e(TAG, "Screen-level poll failed", e)
                }
            }
        }
    }

    private fun setupPolling(screen: SduiModels) {
        stopAllPolling()

        val screenIsRefreshing = screen.defaultRefreshPolicy?.type == RefreshType.Poll

        screen.sections.forEach { section ->
            val policy = section.refreshPolicy
            val policyIntervalMs = policy?.intervalMS
            if (policy?.type == RefreshType.Poll && policyIntervalMs != null) {
                val baseIntervalMs: Long = policyIntervalMs
                val pollUrl = policy.url
                val sectionEndpoint = policy.sectionEndpoint
                val dataPath = policy.dataPath
                val shouldPause = policy.pauseWhenOffScreen ?: true

                // Guard: sectionEndpoint and a non-static screen defaultRefreshPolicy are mutually exclusive.
                if (sectionEndpoint != null && screenIsRefreshing) {
                    Log.w(TAG, "Section '${section.id}' has sectionEndpoint but screen defaultRefreshPolicy " +
                        "is Poll — skipping sectionEndpoint poll; screen-level refresh owns this section.")
                    return@forEach
                }

                // sectionEndpoint takes precedence over url when both are set (schema §RefreshPolicy).
                if (sectionEndpoint != null) {
                    Log.i(TAG, "SECTION poll: section='${section.id}' sectionEndpoint=$sectionEndpoint every ${baseIntervalMs}ms pauseWhenOffScreen=$shouldPause")
                } else if (pollUrl != null) {
                    Log.i(TAG, "DIRECT poll: section='${section.id}' url=$pollUrl every ${baseIntervalMs}ms pauseWhenOffScreen=$shouldPause")
                } else {
                    Log.w(TAG, "POLL section='${section.id}' has neither url nor sectionEndpoint — ticks will no-op")
                }

                pollFailureCounts[section.id] = 0

                pollingJobs[section.id] = scope.launch {
                    startSectionPollJob(section, baseIntervalMs, pollUrl, sectionEndpoint, dataPath, shouldPause)
                }
            }
        }
        if (pollingJobs.isNotEmpty()) Log.i(TAG, "Active polls: ${pollingJobs.keys}")
    }

    private suspend fun startSectionPollJob(
        section: Section,
        baseIntervalMs: Long,
        pollUrl: String?,
        sectionEndpoint: String?,
        dataPath: String?,
        shouldPause: Boolean
    ) {
        var currentIntervalMs: Long = baseIntervalMs
        while (currentCoroutineContext().isActive) {
            // Gate: wait for app foreground
            _isAppForeground.first { it }

            // Gate: wait for section to be near viewport (if pauseWhenOffScreen)
            if (shouldPause) {
                visibilityTracker.awaitNearViewport(section.id)
            }

            delay(currentIntervalMs)
            try {
                // sectionEndpoint takes precedence over url when both are set (schema §RefreshPolicy).
                if (sectionEndpoint != null) {
                    try {
                        val newSection = repository.fetchSection(sectionEndpoint, buildEnvelope(), currentScreen?.traceID)
                        // Load-bearing ordering: restartRealtimeForSection cancels THIS coroutine
                        // via pollingJobs[section.id]?.cancel(). Cancellation is cooperative, so
                        // we keep running until the next suspending call. The remaining statements
                        // here (mergeSingleSection, _staleSections update, return) are all
                        // non-suspending. Do NOT insert any suspending call between this line
                        // and `return` — a suspension would observe the cancellation and abort
                        // the merge, leaving the screen in an inconsistent state.
                        restartRealtimeForSection(section.id, newSection)
                        mergeSingleSection(newSection)
                        _staleSections.value = _staleSections.value - section.id
                        return
                    } catch (e: SchemaVersionMismatchException) {
                        Log.w(TAG, "Schema version mismatch on section '${section.id}' — upgrade required", e)
                        _uiState.value = SduiScreenUiState.UpgradeRequired(
                            e.message ?: "Please update the app to continue."
                        )
                        return
                    } catch (e: SectionNotFoundException) {
                        Log.w(TAG, "Section '${section.id}' returned 404 — stopping poll", e)
                        pollingJobs[section.id]?.cancel()
                        pollingJobs.remove(section.id)
                        _staleSections.value = _staleSections.value + section.id
                        return
                    }
                } else if (pollUrl != null) {
                    val data = repository.fetchRawJson(pollUrl, dataPath, currentScreen?.traceID)
                    updateSectionData(section.id, data)
                } else {
                    Log.w(TAG, "Poll section '${section.id}' has neither url nor sectionEndpoint — skipping tick")
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

    private fun mergeSingleSection(newSection: Section) {
        val screen = currentScreen ?: return
        val mergedSections = screen.sections.toMutableList()
        val idx = mergedSections.indexOfFirst { it.id == newSection.id }
        if (idx >= 0) {
            mergedSections[idx] = newSection
        } else {
            mergedSections.add(newSection)
        }
        val mergedScreen = screen.copy(sections = mergedSections)
        currentScreen = mergedScreen
        _uiState.value = SduiScreenUiState.Success(mergedScreen)
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
        pollFailureCounts.clear()
    }

    private fun restartRealtimeForSection(sectionId: String, newSection: Section) {
        // Cancel old poll job for this section only
        pollingJobs[sectionId]?.cancel()
        pollingJobs.remove(sectionId)
        pollFailureCounts.remove(sectionId)

        // Cancel old Ably job for this section only
        ablyJobs[sectionId]?.cancel()
        ablyJobs.remove(sectionId)
        sseMessageBuffer.remove(sectionId)

        // Start the new section's policy
        val policy = newSection.refreshPolicy ?: return
        when (policy.type) {
            RefreshType.Poll -> {
                val policyIntervalMs = policy.intervalMS ?: return
                val pollUrl = policy.url
                val sectionEndpoint = policy.sectionEndpoint
                val dataPath = policy.dataPath
                val shouldPause = policy.pauseWhenOffScreen ?: true

                pollFailureCounts[sectionId] = 0
                pollingJobs[sectionId] = scope.launch {
                    startSectionPollJob(newSection, policyIntervalMs.toLong(), pollUrl, sectionEndpoint, dataPath, shouldPause)
                }
            }
            RefreshType.SSE -> {
                val channel = policy.channel ?: return
                subscribeToChannel(newSection, channel)
            }
            else -> { /* static — no action */ }
        }
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
                            sseStaleJob = scope.launch {
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
        ablyJobs[section.id] = scope.launch {
            try {
                ablyChannelManager?.subscribeToChannel(channel)?.collect { message ->
                    // Per-message isolation: a single bad payload (e.g. a binding
                    // that writes a shape the Data model can't round-trip) must
                    // not propagate out of `collect`, end the launch, and fire
                    // `awaitClose` — that would silently unsubscribe and stop
                    // every subsequent tick. Catch here so the next message
                    // still gets a chance.
                    try {
                        Log.d(TAG, "Ably update for ${section.id}: keys=${message.keys}")
                        _staleSections.value = _staleSections.value - section.id

                        val isForeground = _isAppForeground.value
                        val isVisible = !shouldPause || visibilityTracker.isNearViewport(section.id)

                        if (!isForeground || !isVisible) {
                            val now = System.currentTimeMillis()
                            sseMessageBuffer[section.id] = BufferedSseEntry(message, now)
                            pruneExpiredSseBuffer(now)
                            Log.d(TAG, "SSE message buffered for ${section.id} (fg=$isForeground, vis=$isVisible)")
                            return@collect
                        }

                        applyAblyMessage(section, message)
                    } catch (e: CancellationException) {
                        // Cooperative cancellation — let it propagate so the
                        // job actually stops when stopAbly() / restart fires.
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Ably message apply failed for ${section.id} (subscription stays open)", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ably stream error for ${section.id}", e)
            }
        }

        // Launch a coroutine that watches visibility changes and applies buffered messages
        if (shouldPause) {
            scope.launch {
                visibilityTracker.visibleSections.collect { visibleSet ->
                    if (section.id in visibleSet) {
                        val entry = sseMessageBuffer.remove(section.id)
                        val buffered = entry?.takeIf {
                            System.currentTimeMillis() - it.receivedAtMs <= SSE_BUFFER_TTL_MS
                        }?.message
                        if (buffered != null) {
                            Log.d(TAG, "Applying buffered SSE message for ${section.id}")
                            applyAblyMessage(section, buffered)
                        }
                    }
                }
            }
        }
    }

    private fun mergeSectionWithAblyMessage(
        section: Section,
        message: Map<String, Any?>,
        screen: SduiModels
    ): Data? {
        val dataBinding = section.dataBinding
        if (dataBinding == null) {
            Log.w(TAG, "No dataBinding config for section ${section.id} — message dropped")
            return null
        }
        val currentData = screen.sections
            .find { it.id == section.id }?.data
            ?.let { toMap(it) } ?: return null
        val updatedData = dataBindingResolver.applyBindings(
            currentData, message, dataBinding, screen.traceID,
            section.stringTable, section.id
        )
        return toData(updatedData)
    }

    private fun applyAblyMessage(section: Section, message: Map<String, Any?>) {
        val screen = currentScreen ?: return
        val merged = mergeSectionWithAblyMessage(section, message, screen) ?: return
        updateSectionInScreen(section.id, merged)
    }

    private fun pruneExpiredSseBuffer(now: Long) {
        val cutoff = now - SSE_BUFFER_TTL_MS
        for (id in sseMessageBuffer.filterValues { it.receivedAtMs < cutoff }.keys) {
            sseMessageBuffer.remove(id)
        }
    }

    private fun flushSseBufferBatched() {
        if (sseMessageBuffer.isEmpty()) return
        val now = System.currentTimeMillis()
        val entries = sseMessageBuffer.entries.mapNotNull { (id, entry) ->
            if (now - entry.receivedAtMs > SSE_BUFFER_TTL_MS) null else id to entry.message
        }
        sseMessageBuffer.clear()
        if (entries.isEmpty()) return

        var screen = currentScreen ?: return
        var appliedCount = 0
        for ((sectionId, message) in entries) {
            val section = screen.sections.find { it.id == sectionId } ?: continue
            val merged = mergeSectionWithAblyMessage(section, message, screen) ?: continue
            screen = screen.copy(
                sections = screen.sections.map { if (it.id == sectionId) it.copy(data = merged) else it }
            )
            _staleSections.value = _staleSections.value - sectionId
            appliedCount++
        }
        if (appliedCount > 0) {
            currentScreen = screen
            _uiState.value = SduiScreenUiState.Success(screen)
            Log.d(TAG, "Applied $appliedCount buffered SSE message(s) on foreground")
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
        sseMessageBuffer.clear()
        ablyChannelManager?.disconnect()
        ablyChannelManager = null
    }

    // ── Cleanup ──────────────────────────────────────────────────────

    fun onCleared() {
        screenLevelPollJob?.cancel()
        stopAllPolling()
        stopAbly()
        Log.d(TAG, "Cleared — data channels cleaned up")
    }
}
