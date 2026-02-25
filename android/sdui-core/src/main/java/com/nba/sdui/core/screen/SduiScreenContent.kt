package com.nba.sdui.core.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.SduiScreen
import com.nba.sdui.core.renderer.SectionRouter
import com.nba.sdui.core.state.SduiAction

/**
 * Generic SDUI screen content — renders Loading / Error / Success states.
 *
 * This Composable is deliberately "headless": it does NOT include any
 * app-level chrome (TopAppBar, navigation icons, etc.).  App code should
 * wrap this inside its own Scaffold / Column with whatever chrome is needed.
 *
 * Usage from app layer:
 * ```
 * Column {
 *     TopAppBar(...)            // app-level chrome
 *     SduiScreenContent(...)    // library-level rendering
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiScreenContent(
    uiState: SduiScreenUiState,
    screenState: Map<String, Any>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is SduiScreenUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is SduiScreenUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is SduiScreenUiState.Success -> {
                SuccessContent(
                    screen = uiState.screen,
                    screenState = screenState,
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    onAction = onAction,
                    onStateChange = onStateChange
                )
            }
        }
    }
}

// ── Private helpers ──────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Failed to load screen",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    screen: SduiScreen,
    screenState: Map<String, Any>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(
                items = screen.sections,
                key = { it.id }
            ) { section ->
                SectionRouter(
                    section = section,
                    screenState = screenState,
                    onAction = onAction,
                    onStateChange = onStateChange
                )
            }
        }
    }
}
