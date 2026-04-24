package com.nba.sdui.core.renderer.sections

import androidx.compose.runtime.Composable
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.atomic.AtomicRouter
import com.nba.sdui.core.state.SduiAction

/**
 * SubscribeHero Renderer — reserved SDK integration point for the full-
 * screen subscription upsell.
 *
 * The entire visible surface (logo, title, subtitle, feature list, tier
 * cards, CTAs) is expressed as an atomic tree under `section.data.ui`;
 * this renderer is a thin walker over that tree via [AtomicRouter],
 * identical in behaviour to an AtomicComposite section.
 *
 * Outer chrome (margin, radius, gradient background, inner padding) comes
 * from `section.surface` via `SectionContainer` — this renderer only
 * walks the inner atomic tree.
 *
 * `section.data.tiers` carries IAP product identifiers reserved for the
 * future IAP SDK; the renderer reads nothing from it today.
 */
@Composable
fun SubscribeHeroRenderer(
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
