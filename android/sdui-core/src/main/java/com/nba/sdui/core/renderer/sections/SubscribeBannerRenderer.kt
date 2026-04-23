package com.nba.sdui.core.renderer.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.atomic.AtomicRouter
import com.nba.sdui.core.state.SduiAction

/**
 * SubscribeBanner Renderer — reserved SDK integration point for inline
 * subscription upsell.
 *
 * The entire visible surface is expressed as an atomic tree under
 * `section.data.ui`; this renderer is a thin walker over that tree via
 * [AtomicRouter], identical in behaviour to an AtomicComposite section.
 *
 * Outer chrome (margin, radius, gradient background, inner padding) comes
 * from `section.surface` via `SectionContainer` — this renderer only
 * walks the inner atomic tree. See AGENTS.md §15.3.
 *
 * `section.data.ctaAction` is the pre-SDK fallback action; once the IAP
 * SDK lands it will take over the CTA button's tap, reading product
 * identifiers from `section.data.tiers`.
 */
@Composable
fun SubscribeBannerRenderer(
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
