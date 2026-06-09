package com.nba.sdui.core.screen

import android.util.Log
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.AblyChannelManager
import com.nba.sdui.core.data.DataBindingResolver
import com.nba.sdui.core.data.SchemaVersionMismatchException
import com.nba.sdui.core.data.SduiException
import com.nba.sdui.core.data.SectionNotFoundException
import com.nba.sdui.core.data.SduiRepository
import com.nba.sdui.core.models.generated.RefreshPolicy
import com.nba.sdui.core.models.generated.RefreshType
import com.nba.sdui.core.models.generated.Screen
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.models.generated.SectionData
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
    private data class SectionPolicySelection(
        val opaquePolicy: RefreshPolicy?,
        val sectionRefreshPolicy: RefreshPolicy?,
        val fingerprint: String
    )
    private data class SectionDriverKeys(
        var opaqueDriverKey: String? = null,
        var sectionRefreshDriverKey: String? = null
    )

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
    private var activeSseSectionIds: Set<String> = emptySet()
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
    private val _currentScreen = MutableStateFlow<Screen?>(null)
    private var currentScreen: Screen?
        get() = _currentScreen.value
        set(value) { _currentScreen.value = value }
    private var currentEndpoint: String? = null   // resolved server path — used for refresh / polling
    private var currentUserParams: Map<String, String> = emptyMap()  // last user-supplied filter params — replayed on pull-to-refresh and poll
    private var screenLevelPollJob: Job? = null
    private val sectionPolicyFingerprints = mutableMapOf<String, String>()
    private val sectionDriverKeys = mutableMapOf<String, SectionDriverKeys>()

    /**
     * Last `X-Correlation-ID` value the server echoed back on a successful
     * fetch. Replayed as the seed correlation on subsequent refreshes,
     * section polls, and live-data binding logs so a single user-visible
     * screen ties to one correlation thread end-to-end.
     */
    private var lastCorrelationId: String? = null

    /**
     * Last successfully loaded screen payload, exposed as a [StateFlow] so the
     * navigation shell recomposes the moment the very first load completes
     * (the value goes from null to non-null). Kept when a later fetch fails so
     * shell navigation (bottom bar) and [parentUri] remain available for escape.
     */
    val shellScreen: StateFlow<Screen?> = _currentScreen.asStateFlow()

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
                val result = repository.fetchScreen(endpoint, buildEnvelope())
                lastCorrelationId = result.correlationId
                applyScreen(result.value)
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
     * (envelope + GET/POST length fallback + `X-Correlation-ID` propagation) and
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
                val result = repository.fetchScreen(
                    path = endpoint,
                    envelope = buildEnvelope(),
                    userParams = userParams,
                    correlationIdOverride = lastCorrelationId
                )
                val refreshScreen = result.value

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

                lastCorrelationId = result.correlationId
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
                val result = repository.fetchScreen(
                    path = endpoint,
                    envelope = buildEnvelope(),
                    userParams = currentUserParams,
                    correlationIdOverride = lastCorrelationId
                )
                lastCorrelationId = result.correlationId
                applyScreen(result.value)
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
    private fun applyScreen(screen: Screen) {
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
        Log.d(TAG, "Screen loaded: correlationId=$lastCorrelationId, sections=${screen.sections.size}")
        _uiState.value = SduiScreenUiState.Success(screen)

        // Defer data-channel bootstrap so the initial render is never blocked.
        // Polling and Ably are server-driven — always inspect refreshPolicy.
        scope.launch {
            reconcileRealtimeDrivers(screen)
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
                    val result = repository.fetchScreen(
                        path = endpoint,
                        envelope = buildEnvelope(),
                        userParams = currentUserParams,
                        correlationIdOverride = lastCorrelationId
                    )
                    lastCorrelationId = result.correlationId
                    applyScreen(result.value)
                } catch (e: Exception) {
                    Log.e(TAG, "Screen-level poll failed", e)
                }
            }
        }
    }

    private fun reconcileRealtimeDrivers(screen: Screen) {
        val screenHasNonStaticDefaultRefresh = screen.defaultRefreshPolicy?.type?.let { it != RefreshType.Static } == true
        val liveSectionIds = screen.sections.map { it.id }.toSet()
        val staleDriverSectionIds = sectionDriverKeys.keys - liveSectionIds
        staleDriverSectionIds.forEach(::stopDriversForSection)

        screen.sections.forEach { section ->
            reconcileSectionDrivers(section, screenHasNonStaticDefaultRefresh)
        }

        val activeSseIds = sectionDriverKeys
            .filterValues { !it.opaqueDriverKey.isNullOrBlank() && it.opaqueDriverKey!!.startsWith("sse:") }
            .keys
        activeSseSectionIds = activeSseIds
        if (activeSseSectionIds.isEmpty()) {
            sseStaleJob?.cancel()
            sseStaleJob = null
        }
    }

    private fun reconcileSectionDrivers(
        section: Section,
        screenHasNonStaticDefaultRefresh: Boolean
    ) {
        val sectionId = section.id
        val selection = splitSectionPolicies(section)
        val previousFingerprint = sectionPolicyFingerprints[sectionId]
        if (selection.fingerprint != previousFingerprint) {
            Log.d(TAG, "RefreshPolicy fingerprint changed for section '$sectionId': '${previousFingerprint ?: "<none>"}' -> '${selection.fingerprint}'")
            sectionPolicyFingerprints[sectionId] = selection.fingerprint
        }

        val sectionRefreshPolicy = if (screenHasNonStaticDefaultRefresh && selection.sectionRefreshPolicy != null) {
            Log.w(
                TAG,
                "Section '$sectionId' has sectionEndpoint policy while screen defaultRefreshPolicy is non-static; " +
                    "skipping sectionEndpoint poll for this section."
            )
            null
        } else {
            selection.sectionRefreshPolicy
        }
        val opaquePolicy = selection.opaquePolicy

        val desiredOpaqueKey = when {
            opaquePolicy?.type == RefreshType.SSE && !opaquePolicy.channel.isNullOrBlank() ->
                opaqueSseDriverKey(sectionId, opaquePolicy.channel!!)
            opaquePolicy?.type == RefreshType.Poll &&
                !opaquePolicy.url.isNullOrBlank() &&
                opaquePolicy.intervalMS != null &&
                opaquePolicy.sectionEndpoint.isNullOrBlank() ->
                opaquePollDriverKey(sectionId, opaquePolicy.url!!, opaquePolicy.intervalMS, opaquePolicy.dataPath)
            else -> null
        }
        val desiredSectionRefreshKey = when {
            sectionRefreshPolicy?.type == RefreshType.Poll &&
                !sectionRefreshPolicy.sectionEndpoint.isNullOrBlank() &&
                sectionRefreshPolicy.intervalMS != null ->
                sectionRefreshDriverKey(sectionId, sectionRefreshPolicy.sectionEndpoint!!, sectionRefreshPolicy.intervalMS)
            else -> null
        }

        val activeKeys = sectionDriverKeys.getOrPut(sectionId) { SectionDriverKeys() }

        if (activeKeys.opaqueDriverKey != desiredOpaqueKey) {
            activeKeys.opaqueDriverKey?.let { stopDriver(it) }
            activeKeys.opaqueDriverKey = null
        }
        if (activeKeys.sectionRefreshDriverKey != desiredSectionRefreshKey) {
            activeKeys.sectionRefreshDriverKey?.let { stopDriver(it) }
            activeKeys.sectionRefreshDriverKey = null
        }

        if (desiredOpaqueKey != null && activeKeys.opaqueDriverKey == null) {
            when {
                opaquePolicy?.type == RefreshType.SSE && !opaquePolicy.channel.isNullOrBlank() -> {
                    subscribeToChannel(
                        sectionId = sectionId,
                        channel = opaquePolicy.channel!!,
                        driverKey = desiredOpaqueKey,
                        shouldPause = opaquePolicy.pauseWhenOffScreen ?: true
                    )
                    activeKeys.opaqueDriverKey = desiredOpaqueKey
                }
                opaquePolicy?.type == RefreshType.Poll &&
                    !opaquePolicy.url.isNullOrBlank() &&
                    opaquePolicy.intervalMS != null &&
                    opaquePolicy.sectionEndpoint.isNullOrBlank() -> {
                    startPollDriver(
                        sectionId = sectionId,
                        driverKey = desiredOpaqueKey,
                        baseIntervalMs = opaquePolicy.intervalMS.toLong(),
                        pollUrl = opaquePolicy.url,
                        sectionEndpoint = null,
                        dataPath = opaquePolicy.dataPath,
                        shouldPause = opaquePolicy.pauseWhenOffScreen ?: true
                    )
                    activeKeys.opaqueDriverKey = desiredOpaqueKey
                }
            }
        }

        if (desiredSectionRefreshKey != null && activeKeys.sectionRefreshDriverKey == null) {
            val sectionRefreshIntervalMs = sectionRefreshPolicy?.intervalMS ?: return
            startPollDriver(
                sectionId = sectionId,
                driverKey = desiredSectionRefreshKey,
                baseIntervalMs = sectionRefreshIntervalMs.toLong(),
                pollUrl = null,
                sectionEndpoint = sectionRefreshPolicy.sectionEndpoint,
                dataPath = sectionRefreshPolicy.dataPath,
                shouldPause = sectionRefreshPolicy.pauseWhenOffScreen ?: true
            )
            activeKeys.sectionRefreshDriverKey = desiredSectionRefreshKey
        }

        if (activeKeys.opaqueDriverKey == null && activeKeys.sectionRefreshDriverKey == null) {
            sectionDriverKeys.remove(sectionId)
            sectionPolicyFingerprints.remove(sectionId)
        }
    }

    private fun splitSectionPolicies(section: Section): SectionPolicySelection {
        val policies = section.refreshPolicy.orEmpty()
        var opaquePolicy: RefreshPolicy? = null
        var sectionRefreshPolicy: RefreshPolicy? = null
        var seenStatic = false

        for ((index, policy) in policies.withIndex()) {
            val isStatic = policy.type == RefreshType.Static
            val isSectionRefresh = policy.type == RefreshType.Poll && !policy.sectionEndpoint.isNullOrBlank()
            val isOpaque = policy.type == RefreshType.SSE ||
                (policy.type == RefreshType.Poll && policy.sectionEndpoint.isNullOrBlank() && !policy.url.isNullOrBlank())

            if (isStatic) {
                if (policies.size > 1) {
                    Log.w(TAG, "Section '${section.id}' includes static refresh policy with additional elements; static must be solo")
                }
                seenStatic = true
                continue
            }

            if (isSectionRefresh) {
                if (sectionRefreshPolicy == null) {
                    sectionRefreshPolicy = policy
                } else {
                    Log.w(TAG, "Section '${section.id}' has multiple section-refresh policies; ignoring extra element at index $index")
                }
                continue
            }

            if (isOpaque) {
                if (opaquePolicy == null) {
                    opaquePolicy = policy
                } else {
                    Log.w(TAG, "Section '${section.id}' has multiple opaque refresh policies; ignoring extra element at index $index")
                }
                continue
            }

            Log.w(TAG, "Section '${section.id}' has unsupported refresh policy element at index $index: type=${policy.type}")
        }

        if (seenStatic && (opaquePolicy != null || sectionRefreshPolicy != null)) {
            Log.w(TAG, "Section '${section.id}' mixes static with active refresh policies; active policies will be used")
        }

        return SectionPolicySelection(
            opaquePolicy = opaquePolicy,
            sectionRefreshPolicy = sectionRefreshPolicy,
            fingerprint = policies.joinToString(separator = "|") { policyFingerprint(it) }
        )
    }

    private fun policyFingerprint(policy: RefreshPolicy): String {
        return listOf(
            "type=${policy.type}",
            "channel=${policy.channel.orEmpty()}",
            "url=${policy.url.orEmpty()}",
            "sectionEndpoint=${policy.sectionEndpoint.orEmpty()}",
            "intervalMs=${policy.intervalMS ?: -1}",
            "dataPath=${policy.dataPath.orEmpty()}",
            "pauseWhenOffScreen=${policy.pauseWhenOffScreen ?: true}"
        ).joinToString(separator = ";")
    }

    private fun sectionRefreshDriverKey(sectionId: String, endpoint: String, intervalMs: Long): String =
        "section-poll:$sectionId:$endpoint:$intervalMs"

    private fun opaquePollDriverKey(sectionId: String, url: String, intervalMs: Long, dataPath: String?): String =
        "opaque-poll:$sectionId:$url:$intervalMs:${dataPath.orEmpty()}"

    private fun opaqueSseDriverKey(sectionId: String, channel: String): String =
        "sse:$sectionId:$channel"

    private fun startPollDriver(
        sectionId: String,
        driverKey: String,
        baseIntervalMs: Long,
        pollUrl: String?,
        sectionEndpoint: String?,
        dataPath: String?,
        shouldPause: Boolean
    ) {
        pollFailureCounts[driverKey] = 0
        pollingJobs[driverKey] = scope.launch {
            startSectionPollJob(
                sectionId = sectionId,
                driverKey = driverKey,
                baseIntervalMs = baseIntervalMs,
                pollUrl = pollUrl,
                sectionEndpoint = sectionEndpoint,
                dataPath = dataPath,
                shouldPause = shouldPause
            )
        }
    }

    private suspend fun startSectionPollJob(
        sectionId: String,
        driverKey: String,
        baseIntervalMs: Long,
        pollUrl: String?,
        sectionEndpoint: String?,
        dataPath: String?,
        shouldPause: Boolean
    ) {
        var currentIntervalMs: Long = baseIntervalMs
        while (currentCoroutineContext().isActive) {
            _isAppForeground.first { it }
            if (shouldPause) {
                visibilityTracker.awaitNearViewport(sectionId)
            }

            delay(currentIntervalMs)
            try {
                if (!sectionEndpoint.isNullOrBlank()) {
                    try {
                        val sectionResult = repository.fetchSection(sectionEndpoint, buildEnvelope(), lastCorrelationId)
                        val newSection = sectionResult.value
                        sectionResult.correlationId?.let { lastCorrelationId = it }
                        mergeSingleSection(newSection)
                        _staleSections.value = _staleSections.value - sectionId
                        val screenHasNonStaticDefaultRefresh =
                            currentScreen?.defaultRefreshPolicy?.type?.let { it != RefreshType.Static } == true
                        reconcileSectionDrivers(newSection, screenHasNonStaticDefaultRefresh)
                    } catch (e: SchemaVersionMismatchException) {
                        Log.w(TAG, "Schema version mismatch on section '$sectionId' — upgrade required", e)
                        _uiState.value = SduiScreenUiState.UpgradeRequired(
                            e.message ?: "Please update the app to continue."
                        )
                        return
                    } catch (e: SectionNotFoundException) {
                        Log.w(TAG, "Section '$sectionId' returned 404 — stopping section drivers", e)
                        stopDriversForSection(sectionId)
                        _staleSections.value = _staleSections.value + sectionId
                        return
                    }
                } else if (!pollUrl.isNullOrBlank()) {
                    val data = repository.fetchRawJson(pollUrl, dataPath, lastCorrelationId)
                    updateSectionData(sectionId, data)
                    _staleSections.value = _staleSections.value - sectionId
                } else {
                    Log.w(TAG, "Poll driver '$driverKey' for section '$sectionId' has no url or sectionEndpoint — skipping tick")
                }

                pollFailureCounts[driverKey] = 0
                currentIntervalMs = baseIntervalMs
            } catch (e: Exception) {
                Log.e(TAG, "Poll failed for driver '$driverKey' section '$sectionId'", e)
                val failures = (pollFailureCounts[driverKey] ?: 0) + 1
                pollFailureCounts[driverKey] = failures
                currentIntervalMs = (currentIntervalMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                if (failures >= POLL_FAILURE_THRESHOLD) {
                    _staleSections.value = _staleSections.value + sectionId
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
                currentData, newData, dataBinding, lastCorrelationId,
                section.stringTable, sectionId
            )
        } else {
            currentData + newData
        }
        updateSectionInScreen(sectionId, toData(merged))
    }

    private fun stopAllPolling() {
        pollingJobs.forEach { (key, job) ->
            Log.d(TAG, "Stop polling: $key")
            job.cancel()
        }
        pollingJobs.clear()
        pollFailureCounts.clear()
        sectionDriverKeys.values.forEach { keys ->
            keys.sectionRefreshDriverKey?.let { pollFailureCounts.remove(it) }
        }
    }

    private fun stopDriver(driverKey: String) {
        if (driverKey.startsWith("sse:")) {
            ablyJobs.remove(driverKey)?.let { job ->
                Log.d(TAG, "Stop Ably driver: $driverKey")
                job.cancel()
            }
            return
        }
        pollingJobs.remove(driverKey)?.let { job ->
            Log.d(TAG, "Stop poll driver: $driverKey")
            job.cancel()
        }
        pollFailureCounts.remove(driverKey)
    }

    private fun stopDriversForSection(sectionId: String) {
        val keys = sectionDriverKeys.remove(sectionId) ?: return
        keys.opaqueDriverKey?.let(::stopDriver)
        keys.sectionRefreshDriverKey?.let(::stopDriver)
        sectionPolicyFingerprints.remove(sectionId)
        sseMessageBuffer.remove(sectionId)
    }

    // ── Ably ─────────────────────────────────────────────────────────

    private var sseStaleJob: Job? = null

    private fun ensureAblyManager() {
        if (ablyChannelManager == null) {
            ablyChannelManager = AblyChannelManager(config.ablyTokenUrl)
            ablyChannelManager?.onConnectionStateChange = { connectionState ->
                when (connectionState) {
                    ConnectionState.disconnected, ConnectionState.suspended, ConnectionState.failed -> {
                        // Start countdown — if still disconnected after SSE_STALE_DELAY_MS, mark SSE sections stale
                        if (sseStaleJob?.isActive != true) {
                            sseStaleJob = scope.launch {
                                delay(SSE_STALE_DELAY_MS)
                                if (activeSseSectionIds.isNotEmpty()) {
                                    _staleSections.value = _staleSections.value + activeSseSectionIds
                                    Log.w(TAG, "Ably disconnected for ${SSE_STALE_DELAY_MS}ms — marking SSE sections stale: $activeSseSectionIds")
                                }
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
    }

    /**
     * Subscribe to an Ably channel and apply server-defined data bindings
     * from opaque incoming messages.  No field-level knowledge of the
     * message content exists here — DataBindingResolver does the mapping.
     */
    private fun subscribeToChannel(
        sectionId: String,
        channel: String,
        driverKey: String,
        shouldPause: Boolean
    ) {
        ensureAblyManager()
        ablyJobs[driverKey]?.cancel()
        Log.i(TAG, "Subscribe Ably: channel='$channel' section='$sectionId' pauseWhenOffScreen=$shouldPause key='$driverKey'")
        ablyJobs[driverKey] = scope.launch {
            try {
                ablyChannelManager?.subscribeToChannel(channel)?.collect { message ->
                    // Per-message isolation: a single bad payload (e.g. a binding
                    // that writes a shape the Data model can't round-trip) must
                    // not propagate out of `collect`, end the launch, and fire
                    // `awaitClose` — that would silently unsubscribe and stop
                    // every subsequent tick. Catch here so the next message
                    // still gets a chance.
                    try {
                        Log.d(TAG, "Ably update for $sectionId: keys=${message.keys}")
                        _staleSections.value = _staleSections.value - sectionId

                        val isForeground = _isAppForeground.value
                        val isVisible = !shouldPause || visibilityTracker.isNearViewport(sectionId)

                        if (!isForeground || !isVisible) {
                            val now = System.currentTimeMillis()
                            sseMessageBuffer[sectionId] = BufferedSseEntry(message, now)
                            pruneExpiredSseBuffer(now)
                            Log.d(TAG, "SSE message buffered for $sectionId (fg=$isForeground, vis=$isVisible)")
                            return@collect
                        }

                        applyAblyMessage(sectionId, message)
                    } catch (e: CancellationException) {
                        // Cooperative cancellation — let it propagate so the
                        // job actually stops when stopAbly() / restart fires.
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Ably message apply failed for $sectionId (subscription stays open)", e)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ably stream error for $sectionId", e)
            }
        }

        // Launch a coroutine that watches visibility changes and applies buffered messages
        if (shouldPause) {
            scope.launch {
                visibilityTracker.visibleSections.collect { visibleSet ->
                    if (sectionId in visibleSet) {
                        val entry = sseMessageBuffer.remove(sectionId)
                        val buffered = entry?.takeIf {
                            System.currentTimeMillis() - it.receivedAtMs <= SSE_BUFFER_TTL_MS
                        }?.message
                        if (buffered != null) {
                            Log.d(TAG, "Applying buffered SSE message for $sectionId")
                            applyAblyMessage(sectionId, buffered)
                        }
                    }
                }
            }
        }
    }

    private fun mergeSectionWithAblyMessage(
        sectionId: String,
        message: Map<String, Any?>,
        screen: Screen
    ): SectionData? {
        val section = screen.sections.find { it.id == sectionId } ?: return null
        val dataBinding = section.dataBinding
        if (dataBinding == null) {
            Log.w(TAG, "No dataBinding config for section $sectionId — message dropped")
            return null
        }
        val currentData = screen.sections
            .find { it.id == sectionId }?.data
            ?.let { toMap(it) } ?: return null
        val updatedData = dataBindingResolver.applyBindings(
            currentData, message, dataBinding, lastCorrelationId,
            section.stringTable, sectionId
        )
        return toData(updatedData)
    }

    private fun applyAblyMessage(sectionId: String, message: Map<String, Any?>) {
        val screen = currentScreen ?: return
        val merged = mergeSectionWithAblyMessage(sectionId, message, screen) ?: return
        updateSectionInScreen(sectionId, merged)
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
            val merged = mergeSectionWithAblyMessage(sectionId, message, screen) ?: continue
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
    private fun updateSectionInScreen(sectionId: String, updatedData: SectionData?) {
        val screen = currentScreen ?: return
        val updatedSections = screen.sections.map { s ->
            if (s.id == sectionId) s.copy(data = updatedData) else s
        }
        val updated = screen.copy(sections = updatedSections)
        currentScreen = updated
        _uiState.value = SduiScreenUiState.Success(updated)
        Log.d(TAG, "Data binding applied to $sectionId")
    }

    private fun toMap(data: SectionData?): Map<String, Any?> {
        if (data == null) return emptyMap()
        @Suppress("UNCHECKED_CAST")
        return mapper.convertValue(data, Map::class.java) as Map<String, Any?>
    }

    private fun toData(data: Map<String, Any?>): SectionData? {
        return mapper.convertValue(data, SectionData::class.java)
    }

    private fun stopAbly() {
        ablyJobs.forEach { (key, job) ->
            Log.d(TAG, "Stop Ably: $key")
            job.cancel()
        }
        ablyJobs.clear()
        sseMessageBuffer.clear()
        activeSseSectionIds = emptySet()
        sseStaleJob?.cancel()
        sseStaleJob = null
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
