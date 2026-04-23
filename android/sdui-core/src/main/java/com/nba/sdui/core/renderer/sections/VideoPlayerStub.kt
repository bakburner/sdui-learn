package com.nba.sdui.core.renderer.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.atomic.AtomicRouter
import com.nba.sdui.core.state.SduiAction

/**
 * VideoPlayerStub — reserved SDK integration point for the video player.
 *
 * Today the visible surface is the pre-SDK placeholder composed by the
 * server as an atomic tree under `section.data.ui`; this renderer is a
 * thin walker over that tree via [AtomicRouter]. Once the platform video
 * SDK (HLS/DASH, PiP, AirPlay/Chromecast) lands it mounts here using the
 * SDK inputs at the top of `section.data` (`playerType`, `contentId`,
 * `autoplay`, `capabilities`, `displayConfig`) and the atomic tree
 * becomes the SDK's loading / error placeholder.
 *
 * Outer chrome comes from `section.surface` via `SectionContainer`.
 * See AGENTS.md §15.3.
 */
@Composable
fun VideoPlayerStub(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val root = section.data?.ui ?: return
    AtomicRouter(
        element = root,
        screenState = screenState,
        onAction = onAction,
        modifier = modifier,
        onStateChange = onStateChange
    )
}
