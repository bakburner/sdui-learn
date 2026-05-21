package com.nba.sdui.core.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.LayoutScalar
import com.nba.sdui.core.models.generated.SduiModels
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.models.generated.Spacing
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.renderer.SectionErrorBoundary
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import com.nba.sdui.core.renderer.SectionRouter
import com.nba.sdui.core.renderer.SectionSkeletonHeightCache
import com.nba.sdui.core.renderer.atomic.LocalSduiWireAssetBaseUrl
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.state.SectionVisibilityTracker

/**
 * Generic SDUI screen content — renders Loading / Error / Success states.
 *
 * This Composable is deliberately "headless": it does NOT include app-bar
 * title/back chrome. Those ship as the first {@code AtomicComposite} section
 * when the server calls {@code prependAppBarHeaderIfNeeded}. App code may wrap
 * this with bottom navigation and prototype-only dev controls (theme, variants).
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
    onNavigateBack: (() -> Unit)? = null,
    parentUri: String? = null,
    visibilityTracker: SectionVisibilityTracker? = null,
    wireAssetBaseUrl: String = "",
    modifier: Modifier = Modifier
) {
    val showEscape = uiState is SduiScreenUiState.Error || uiState is SduiScreenUiState.UpgradeRequired
    if (showEscape && onNavigateBack != null) {
        BackHandler { onNavigateBack() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is SduiScreenUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            is SduiScreenUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetry = onRetry,
                    onNavigateBack = onNavigateBack,
                    parentUri = parentUri,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is SduiScreenUiState.UpgradeRequired -> {
                UpgradeRequiredContent(
                    message = uiState.message,
                    onNavigateBack = onNavigateBack,
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
                    onStateChange = onStateChange,
                    visibilityTracker = visibilityTracker,
                    wireAssetBaseUrl = wireAssetBaseUrl
                )
            }
        }
    }
}

@Composable
private fun SectionItem(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit
) {
    val density = LocalDensity.current
    val hints = section.layoutHints
    Column(
        modifier = Modifier.onSizeChanged { sz ->
            val h = sz.height
            if (h > 0) {
                SectionSkeletonHeightCache.record(section.id, with(density) { h.toDp() })
            }
        }
    ) {
        if (hints?.dividerAbove == true) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(modifier = Modifier.height((hints?.marginTop ?: 0L).toInt().dp))
        SectionErrorBoundary(
            sectionId = section.id,
            sectionType = section.type,
            sectionStates = section.sectionStates,
            data = section.data,
            onAction = onAction
        ) {
            SectionRouter(
                section = section,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        }
        Spacer(modifier = Modifier.height((hints?.marginBottom ?: 0L).toInt().dp))
        if (hints?.dividerBelow == true) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// ── Private helpers ──────────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    parentUri: String? = null,
    modifier: Modifier = Modifier
) {
    val padLg = layoutSpacingDp("token:nba.spacing.lg")
    val gapSm = layoutSpacingDp("token:nba.spacing.sm")
    val gapLg = layoutSpacingDp("token:nba.spacing.lg")

    Column(
        modifier = modifier.padding(padLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Failed to load screen",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(gapSm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(gapLg))
        Row(horizontalArrangement = Arrangement.spacedBy(gapSm)) {
            if (onNavigateBack != null) {
                OutlinedButton(onClick = onNavigateBack) {
                    Text(if (parentUri != null) "Go back" else "Home")
                }
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun UpgradeRequiredContent(
    message: String,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val padLg = layoutSpacingDp("token:nba.spacing.lg")
    val gapSm = layoutSpacingDp("token:nba.spacing.sm")
    val gapLg = layoutSpacingDp("token:nba.spacing.lg")

    Column(
        modifier = modifier.padding(padLg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Update Required",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(gapSm))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onNavigateBack != null) {
            Spacer(modifier = Modifier.height(gapLg))
            OutlinedButton(onClick = onNavigateBack) {
                Text("Home")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessContent(
    screen: SduiModels,
    screenState: Map<String, Any>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    visibilityTracker: SectionVisibilityTracker? = null,
    wireAssetBaseUrl: String = ""
) {
    val lazyListState = rememberLazyListState()

    // Observe scroll position for visibility-gated refresh
    visibilityTracker?.Observe(
        lazyListState = lazyListState,
        sectionIds = screen.sections.map { it.id }
    )

    CompositionLocalProvider(LocalSduiWireAssetBaseUrl provides wireAssetBaseUrl) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = screen.contentInsets.toScreenPaddingValues()
            ) {
                items(
                    items = screen.sections,
                    key = { it.id }
                ) { section ->
                    SectionItem(
                        section = section,
                        screenState = screenState,
                        onAction = onAction,
                        onStateChange = onStateChange
                    )
                }
            }
        }
    }
}

@Composable
private fun layoutSpacingDp(token: String): Dp {
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    return LayoutTokenResolver.dp(LayoutScalar.StringValue(token), formFactor)
}

private fun Spacing?.toScreenPaddingValues(): PaddingValues {
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    val s = this ?: return PaddingValues(0.dp)
    return PaddingValues(
        top = LayoutTokenResolver.dp(s.top, formFactor),
        bottom = LayoutTokenResolver.dp(s.bottom, formFactor),
        start = LayoutTokenResolver.dp(s.start, formFactor),
        end = LayoutTokenResolver.dp(s.end, formFactor)
    )
}
