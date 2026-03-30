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
 * SDUI Screen — app-level wrapper.
 *
 * This Composable adds app-specific chrome (TopAppBar, variant chips,
 * and back navigation when applicable) and then delegates all SDUI
 * rendering to [SduiScreenContent] from sdui-core.
 *
 * There is **no ScreenType dispatch** — every screen goes through the
 * same URI-based load path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    config: SduiConfig,
    modifier: Modifier = Modifier,
    onNavigateUri: (String) -> Unit = {},
    onShowToast: (String) -> Unit = {},
    onVariantChange: (String) -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: GameDetailViewModel = viewModel {
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
        viewModel.load(config.uri)
    }

    // Handle action results (app-level side-effects)
    LaunchedEffect(viewModel) {
        viewModel.actionResults.collect { result ->
            when (result) {
                is ActionHandler.ActionResult.NavigateResult -> {
                    onNavigateUri(result.uri)
                }
                is ActionHandler.ActionResult.FireAndForgetResult -> {
                    Log.d("SDUI", "FireAndForget: ${result.eventName} - ${result.params}")
                }
                is ActionHandler.ActionResult.RefreshResult -> {
                    viewModel.load(config.uri, result.sectionId)
                }
                is ActionHandler.ActionResult.ParameterizedRefreshResult -> {
                    viewModel.refreshSections(result.url)
                }
                is ActionHandler.ActionResult.ToastResult -> {
                    onShowToast(result.message)
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
                    text = (uiState as? SduiScreenUiState.Success)?.screen?.title ?: "NBA"
                )
            },
            navigationIcon = {
                val parentUri = (uiState as? SduiScreenUiState.Success)?.screen?.parentUri
                if (parentUri != null) {
                    IconButton(onClick = {
                        onNavigateUri(parentUri)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            }
        )

        // Server-driven variant chips (Rule 9 / Rule 10 — no client-side URI sniffing)
        val serverVariants = (uiState as? SduiScreenUiState.Success)?.screen?.variants ?: emptyList()
        if (serverVariants.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                serverVariants.forEach { v ->
                    FilterChip(
                        selected = config.experiments.values.contains(v.id),
                        onClick = { onVariantChange(v.id) },
                        label = { Text(v.label) }
                    )
                }
            }
        }

        // ── Library-provided SDUI content ────────────────────────────
        val currentScreen = (uiState as? SduiScreenUiState.Success)?.screen
        SduiNavigationShell(
            navigation = currentScreen?.navigation,
            onNavigate = onNavigateUri,
            modifier = Modifier.weight(1f)
        ) { contentModifier ->
            SduiScreenContent(
                uiState = uiState,
                screenState = screenState,
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                onRetry = { viewModel.load(config.uri) },
                onAction = { viewModel.handleAction(it) },
                onStateChange = { key, value -> viewModel.updateState(key, value) },
                modifier = contentModifier
            )
        }
    }
}
