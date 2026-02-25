package com.nba.sdui.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nba.sdui.app.BuildConfig
import com.nba.sdui.app.SduiConfig
import com.nba.sdui.core.screen.SduiNavigationShell
import com.nba.sdui.core.screen.SduiScreenContent
import com.nba.sdui.core.screen.SduiScreenUiState
import com.nba.sdui.core.state.ActionHandler

/**
 * Game Detail Screen — app-level wrapper.
 *
 * This Composable adds app-specific chrome (TopAppBar, variant chips,
 * and back navigation when applicable) and then delegates all SDUI
 * rendering to [SduiScreenContent] from sdui-core.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    config: SduiConfig,
    modifier: Modifier = Modifier,
    onNavigateUri: (String) -> Unit = {},
    onVariantChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: GameDetailViewModel = viewModel(
        // Key on the full config so a NEW ViewModel is created whenever
        // variant or gameId changes.
        key = "sdui_${config.gameId}_${config.variant}_${config.screenType}"
    ) {
        GameDetailViewModel(
            baseUrl = BuildConfig.SDUI_BASE_URL,
            ablyTokenUrl = BuildConfig.ABLY_TOKEN_URL,
            config = config
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Fetch SDUI response on first composition (keyed on full config)
    LaunchedEffect(config) {
        when (config.screenType) {
            SduiConfig.ScreenType.SCOREBOARD -> viewModel.loadScoreboard()
            SduiConfig.ScreenType.GAME_DETAIL -> viewModel.loadGameDetail(config.gameId ?: "")
        }
    }

    // Handle action results (app-level side-effects)
    LaunchedEffect(Unit) {
        viewModel.actionResults.collect { result ->
            when (result) {
                is ActionHandler.ActionResult.NavigateResult -> {
                    onNavigateUri(result.uri)
                }
                is ActionHandler.ActionResult.AnalyticsResult -> {
                    Log.d("SDUI", "Analytics: ${result.eventName} - ${result.params}")
                }
                is ActionHandler.ActionResult.RefreshResult -> {
                    when (config.screenType) {
                        SduiConfig.ScreenType.SCOREBOARD -> viewModel.loadScoreboard(result.sectionId)
                        SduiConfig.ScreenType.GAME_DETAIL -> viewModel.loadGameDetail(config.gameId ?: "", result.sectionId)
                    }
                }
                else -> {}
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ── App-specific chrome ──────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = when {
                        config.screenType == SduiConfig.ScreenType.SCOREBOARD -> "Today's Games"
                        else -> "Game: ${config.gameId}"
                    }
                )
            },
            navigationIcon = {
                if (config.screenType == SduiConfig.ScreenType.GAME_DETAIL) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val variants = when (config.screenType) {
                SduiConfig.ScreenType.SCOREBOARD -> listOf("A" to "Default", "E" to "Promo", "F" to "Promo + Rail")
                SduiConfig.ScreenType.GAME_DETAIL -> listOf("A" to "Default", "B" to "Reorder", "C" to "Minimal", "D" to "Extra Rail")
            }
            variants.forEach { (id, label) ->
                FilterChip(
                    selected = config.variant == id,
                    onClick = { onVariantChange(id) },
                    label = { Text(label) }
                )
            }
        }

        // ── Library-provided SDUI content ────────────────────────────
        val currentScreen = (uiState as? SduiScreenUiState.Success)?.screen
        SduiNavigationShell(
            navigation = currentScreen?.navigation,
            onNavigate = onNavigateUri
        ) { contentModifier ->
            SduiScreenContent(
                uiState = uiState,
                screenState = screenState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                onRetry = {
                    when (config.screenType) {
                        SduiConfig.ScreenType.SCOREBOARD -> viewModel.loadScoreboard()
                        SduiConfig.ScreenType.GAME_DETAIL -> viewModel.loadGameDetail(config.gameId ?: "")
                    }
                },
                onAction = { viewModel.handleAction(it) },
                onStateChange = { key, value -> viewModel.updateState(key, value) },
                modifier = contentModifier
            )
        }
    }
}
