package com.nba.sdui.core.renderer.sections

import androidx.compose.runtime.Composable
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.atomic.AtomicRouter
import com.nba.sdui.core.state.SduiAction

/**
 * SubscribeUpsell Renderer — reserved SDK integration point for the
 * subscription upsell surface (inline banner and full-screen hero share
 * one renderer; layout differences are expressed entirely in the atomic
 * tree).
 *
 * The entire visible surface is expressed as an atomic tree under
 * `section.data.ui`; this renderer is a thin walker over that tree via
 * [AtomicRouter], identical in behaviour to an AtomicComposite section.
 *
 * Outer chrome (margin, radius, gradient background, inner padding) comes
 * from `section.surface` via `SectionContainer` — this renderer only
 * walks the inner atomic tree.
 *
 * `section.data.ctaAction` is the optional pre-SDK fallback action; once
 * the IAP SDK lands it will take over the CTA button's tap, reading
 * product identifiers from `section.data.tiers`.
 */
@Composable
fun SubscribeUpsellRenderer(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit = { _, _ -> }
) {
    val root = section.data?.ui ?: return
    AtomicRouter(
        element = root,
        screenState = screenState,
        onAction = onAction,
        onStateChange = onStateChange
    )
}
