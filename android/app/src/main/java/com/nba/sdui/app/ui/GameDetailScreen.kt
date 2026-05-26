package com.nba.sdui.app.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nba.sdui.app.BuildConfig
import com.nba.sdui.app.SduiConfig
import com.nba.sdui.core.models.generated.LayoutScalar
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.screen.SduiNavigationShell
import com.nba.sdui.core.screen.SduiScreenContent
import com.nba.sdui.core.state.ActionHandler
import com.nba.sdui.core.renderer.atomic.LocalActionExecutor

/**
 * SDUI Screen — app-level wrapper.
 *
 * Screen title, back affordances, and variant chips are all server-composed as
 * {@code AtomicComposite} sections. This layer keeps only the prototype dev
 * controls (theme toggle) outside the SDUI payload.
 */
@Composable
fun GameDetailScreen(
    config: SduiConfig,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    onNavigateUri: (String) -> Unit = {},
    onShowToast: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    viewModel: GameDetailViewModel = viewModel {
        GameDetailViewModel(
            baseUrl = BuildConfig.SDUI_ANDROID_BASE_URL,
            ablyTokenUrl = BuildConfig.ABLY_TOKEN_URL,
            config = config
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val screenState by viewModel.screenState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(config) {
        viewModel.load(config.uri)
    }

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
                    viewModel.load(config.uri, result.target)
                }
                is ActionHandler.ActionResult.ParameterizedRefreshResult -> {
                    viewModel.replaceCurrentScreen(result.endpoint, result.params)
                }
                is ActionHandler.ActionResult.ToastResult -> {
                    onShowToast(result.message)
                }
                else -> {}
            }
        }
    }

    val shellScreen = viewModel.shellScreen
    val themePad = LayoutTokenResolver.dp(LayoutScalar.StringValue("token:nba.spacing.sm"))

    val handleNavigateBack: () -> Unit = {
        val parent = shellScreen?.parentURI
        if (!parent.isNullOrBlank()) {
            onNavigateUri(parent)
        } else {
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SduiNavigationShell(
                navigation = shellScreen?.navigation,
                onNavigate = onNavigateUri,
                modifier = Modifier.weight(1f)
            ) { contentModifier ->
                CompositionLocalProvider(
                    LocalActionExecutor provides { actions -> viewModel.handleActions(actions) }
                ) {
                    SduiScreenContent(
                        uiState = uiState,
                        screenState = screenState,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        onRetry = { viewModel.load(config.uri) },
                        onAction = { viewModel.handleAction(it) },
                        onStateChange = { key, value -> viewModel.updateState(key, value) },
                        onNavigateBack = handleNavigateBack,
                        parentUri = shellScreen?.parentURI,
                        visibilityTracker = viewModel.visibilityTracker,
                        wireAssetBaseUrl = viewModel.wireAssetBaseUrl,
                        modifier = contentModifier
                    )
                }
            }
        }

        TextButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = themePad, end = themePad)
        ) {
            Text(
                if (darkTheme) "Light" else "Dark",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
