package com.nba.sdui.core.screen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nba.sdui.core.config.SduiScreenConfig
import com.nba.sdui.core.data.AblyChannelManager
import com.nba.sdui.core.data.LinescoreUpdate
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
 *  - Apply data bindings from Ably linescore updates
 *
 * App-level code should either use this ViewModel directly or create
 * a thin subclass/wrapper that maps app-specific config to [SduiScreenConfig].
 */
open class SduiScreenViewModel(
    private val config: SduiScreenConfig
) : ViewModel() {

    companion object {
        private const val TAG = "SduiScreenViewModel"
        private const val MIN_POLL_INTERVAL_MS = 5000L
        private const val ENDPOINT_GAME_DETAIL = "game-detail"
        private const val ENDPOINT_SCOREBOARD = "scoreboard"

        /**
         * Convert nba:// URI to server endpoint path.
         */
        fun resolveEndpoint(uri: String): String {
            val path = uri.removePrefix("nba://")
            val gameMatch = Regex("^game/(.+)").find(path)
            if (gameMatch != null) {
                return "/sdui/game-detail/${gameMatch.groupValues[1]}?gameState=live"
            }
            return "/sdui/$path"
        }
    }

    // ── Dependencies (all from sdui-core) ────────────────────────────
    protected val repository = SduiRepository(config.baseUrl)
    protected val stateManager = StateManager()
    protected val actionHandler = ActionHandler()

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
    private var currentScreenId: String? = null
    private var currentEndpoint: String = ENDPOINT_GAME_DETAIL
    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        Log.i(TAG, "Initialized: screenId=${config.screenId}, ably=${config.enableAbly}, polling=${config.enablePolling}")
    }

    // ── Load / Refresh ───────────────────────────────────────────────

    /**
     * Fetch (or re-fetch) the SDUI screen.
     *
     * @param screenId  The screen identifier (e.g. gameId).
     * @param sectionId Optional — when non-null, skip the full-screen loading indicator.
     * @param gameState Game-state hint sent as a query param.
     */
    fun loadScreen(
        screenId: String,
        sectionId: String? = null,
        gameState: String = config.gameState
    ) {
        currentEndpoint = ENDPOINT_GAME_DETAIL
        currentScreenId = screenId

        viewModelScope.launch {
            if (sectionId == null) {
                _uiState.value = SduiScreenUiState.Loading
            }

            try {
                Log.d(TAG, "Loading screen: screenId=$screenId, gameState=$gameState, variant=${config.variant}")

                val screen = repository.getGameDetail(
                    gameId = screenId,
                    gameState = gameState,
                    variant = config.variant
                )
                currentScreen = screen

                // Seed state from response defaults
                screen.state?.forEach { (key, value) ->
                    stateManager.setState(key, value)
                }

                Log.d(TAG, "Screen loaded: traceId=${screen.traceId}, sections=${screen.sections.size}")
                _uiState.value = SduiScreenUiState.Success(screen)

                // Start data channels
                if (config.enablePolling) setupPolling(screen)
                if (config.enableAbly) setupAbly(screen)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load screen", e)
                _uiState.value = SduiScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadScoreboard(sectionId: String? = null) {
        currentEndpoint = ENDPOINT_SCOREBOARD
        currentScreenId = "scoreboard"

        viewModelScope.launch {
            if (sectionId == null) {
                _uiState.value = SduiScreenUiState.Loading
            }

            try {
                Log.d(TAG, "Loading scoreboard: variant=${config.variant}")
                val screen = repository.getScoreboard(
                    variant = config.variant
                )
                currentScreen = screen

                screen.state?.forEach { (key, value) ->
                    stateManager.setState(key, value)
                }

                Log.d(TAG, "Scoreboard loaded: traceId=${screen.traceId}, sections=${screen.sections.size}")
                _uiState.value = SduiScreenUiState.Success(screen)

                if (config.enablePolling) setupPolling(screen)
                if (config.enableAbly) setupAbly(screen)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load scoreboard", e)
                _uiState.value = SduiScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Load a screen from a generic nba:// URI.
     */
    fun loadFromUri(uri: String, sectionId: String? = null) {
        currentEndpoint = uri   // store URI for refresh
        currentScreenId = uri

        viewModelScope.launch {
            if (sectionId == null) {
                _uiState.value = SduiScreenUiState.Loading
            }
            try {
                val endpoint = resolveEndpoint(uri)
                Log.d(TAG, "Loading from URI: $uri → $endpoint")
                val screen = repository.fetchScreen(endpoint, config.variant)
                currentScreen = screen

                screen.state?.forEach { (key, value) ->
                    stateManager.setState(key, value)
                }
                _uiState.value = SduiScreenUiState.Success(screen)
                if (config.enablePolling) setupPolling(screen)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load from URI: $uri", e)
                _uiState.value = SduiScreenUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Pull-to-refresh. */
    fun refresh() {
        val id = currentScreenId ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val screen = when (currentEndpoint) {
                    ENDPOINT_SCOREBOARD -> repository.getScoreboard(config.variant)
                    ENDPOINT_GAME_DETAIL -> repository.getGameDetail(id, "live", config.variant)
                    else -> repository.fetchScreen(resolveEndpoint(currentEndpoint), config.variant)
                }
                currentScreen = screen
                screen.state?.forEach { (k, v) -> stateManager.setState(k, v) }
                _uiState.value = SduiScreenUiState.Success(screen)
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

    // ── Polling ──────────────────────────────────────────────────────

    private fun setupPolling(screen: SduiScreen) {
        stopAllPolling()
        val id = currentScreenId ?: return

        screen.sections.forEach { section ->
            val policy = section.refreshPolicy
            val policyIntervalMs = policy?.intervalMs
            if (policy?.type == "poll" && policyIntervalMs != null) {
                val intervalMs = maxOf(policyIntervalMs.toLong(), MIN_POLL_INTERVAL_MS)
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
                                val updated = when (currentEndpoint) {
                                    ENDPOINT_SCOREBOARD -> repository.getScoreboard(config.variant)
                                    ENDPOINT_GAME_DETAIL -> repository.getGameDetail(id, "live", config.variant)
                                    else -> repository.fetchScreen(resolveEndpoint(currentEndpoint), config.variant)
                                }
                                currentScreen = updated
                                _uiState.value = SduiScreenUiState.Success(updated)
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
        viewModelScope.launch {
            try {
                if (ablyChannelManager == null) {
                    ablyChannelManager = AblyChannelManager(config.ablyTokenUrl)
                    ablyChannelManager?.initialize()
                    Log.i(TAG, "Ably initialised")
                }
                screen.sections.forEach { section ->
                    val policy = section.refreshPolicy
                    val channel = policy?.channel
                    if (policy?.type == "sse" && !channel.isNullOrBlank()) {
                        subscribeToChannel(section, channel)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ably setup failed", e)
            }
        }
    }

    private fun subscribeToChannel(section: SduiSection, channel: String) {
        ablyJobs[section.id]?.cancel()
        Log.i(TAG, "Subscribe Ably: channel='$channel' section='${section.id}'")
        ablyJobs[section.id] = viewModelScope.launch {
            try {
                ablyChannelManager?.subscribeToChannel(channel)?.collect { update ->
                    Log.d(TAG, "Ably update for ${section.id}: home=${update.homeTeamScore} away=${update.awayTeamScore}")
                    applyLinescoreUpdate(section.id, update)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ably error for ${section.id}", e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyLinescoreUpdate(sectionId: String, update: LinescoreUpdate) {
        val screen = currentScreen ?: return
        val updatedSections = screen.sections.map { section ->
            if (section.id == sectionId && section.type == "ScoreboardHeader") {
                val data = section.data?.toMutableMap() ?: mutableMapOf()

                val home = (data["homeTeam"] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf()
                home["score"] = update.homeTeamScore
                data["homeTeam"] = home

                val away = (data["awayTeam"] as? Map<String, Any?>)?.toMutableMap() ?: mutableMapOf()
                away["score"] = update.awayTeamScore
                data["awayTeam"] = away

                data["gameClock"] = update.clock
                data["period"] = update.period
                data["gameStatus"] = update.gameStatus
                data["gameStatusText"] = update.gameStatusText

                section.copy(data = data)
            } else section
        }
        val updated = screen.copy(sections = updatedSections)
        currentScreen = updated
        _uiState.value = SduiScreenUiState.Success(updated)
        Log.d(TAG, "Linescore applied to $sectionId")
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
