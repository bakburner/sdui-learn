package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Alignment
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.BadgeAlignment
import com.nba.sdui.core.models.generated.CrossAlignment
import com.nba.sdui.core.models.generated.UIDirection
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.ContainerVariantResolver
import com.nba.sdui.core.renderer.ContainerVariantResolver.OverridePolicy
import com.nba.sdui.core.renderer.ContainerVariantResolver.SurfaceRole
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.BackgroundGradientViewModel
import com.nba.sdui.core.renderer.adapters.BackgroundViewModel
import com.nba.sdui.core.renderer.adapters.toViewModel
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicContainer — renders a Container element as a Column or Row with
 * optional padding, gap, background color, gradient, flex children, and responsive breakpoint.
 *
 * Flex: When a child has a non-null `flex` value, it receives proportional weight
 * along the main axis (like CSS flex-grow or Compose weight). Children without
 * `flex` size to content.
 *
 * Breakpoint: When set on this Container, the direction flips from row→column
 * when the screen width is below the breakpoint (dp). This replaces the old
 * Row section type with a purely atomic, server-composed primitive.
 */
@Composable
fun AtomicContainer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    // Responsive breakpoint: flip row→column when screen is narrow
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isRow = if (element.breakpoint != null && element.direction == UIDirection.Row) {
        screenWidthDp >= element.breakpoint.toInt()
    } else {
        element.direction == UIDirection.Row
    }

    val gap = element.gap?.toInt()?.dp ?: 0.dp

    val variantSpec = ContainerVariantResolver.resolve(element.variant)

    // Resolve effective values by merging inline props with the variant spec.
    // Inline wins on ALLOW axes; variant wins on LOCK axes (the inline attempt
    // is logged as variant_override_blocked).
    val effectiveCornerRadius: Int? = resolveAxis(
        axis = "cornerRadius",
        inline = element.cornerRadius?.toInt(),
        variantValue = variantSpec?.cornerRadiusDp,
        overrideMatrix = variantSpec?.overrideMatrix,
        variantName = element.variant
    )

    val effectiveShadowElevationDp: Int? = resolveAxis(
        axis = "shadow",
        inline = element.shadow?.radius?.toInt(),
        variantValue = variantSpec?.shadowElevationDp,
        overrideMatrix = variantSpec?.overrideMatrix,
        variantName = element.variant
    )

    val effectiveFillWidth: Boolean = element.fillWidth == true ||
        (element.fillWidth == null && variantSpec?.fillWidth == true)

    val inlineBackground = element.background.toViewModel()
    val backgroundLocked = variantSpec?.overrideMatrix?.get("background") == OverridePolicy.LOCK
    val useVariantBackground: Boolean = when {
        variantSpec == null -> false
        inlineBackground == null -> variantSpec.backgroundColorRole != null
        backgroundLocked -> {
            ContainerVariantResolver.logOverrideBlocked(
                element.variant, "background", element.background
            )
            variantSpec.backgroundColorRole != null
        }
        else -> false
    }

    // Per-corner `cornerRadii` wins over the single-value `cornerRadius`
    // when present. Any corner key omitted falls back to `cornerRadius` (or
    // 0 if that is also absent). Used for content-card cards that want
    // rounded tops + square bottoms so headline text does not collide with
    // a bottom-corner curve.
    val shape = run {
        val radii = element.cornerRadii
        val fallback = effectiveCornerRadius ?: 0
        if (radii != null &&
            ((radii.topStart ?: 0) != 0L || (radii.topEnd ?: 0) != 0L ||
             (radii.bottomStart ?: 0) != 0L || (radii.bottomEnd ?: 0) != 0L)) {
            RoundedCornerShape(
                topStart = (radii.topStart?.toInt() ?: fallback).dp,
                topEnd = (radii.topEnd?.toInt() ?: fallback).dp,
                bottomEnd = (radii.bottomEnd?.toInt() ?: fallback).dp,
                bottomStart = (radii.bottomStart?.toInt() ?: fallback).dp
            )
        } else {
            RoundedCornerShape(fallback.dp)
        }
    }

    val hasAnyCorner = (effectiveCornerRadius != null && effectiveCornerRadius > 0) ||
        element.cornerRadii?.let {
            (it.topStart ?: 0) > 0 || (it.topEnd ?: 0) > 0 ||
            (it.bottomStart ?: 0) > 0 || (it.bottomEnd ?: 0) > 0
        } == true

    val fillModifier = if (effectiveFillWidth) modifier.fillMaxWidth() else modifier

    // Explicit wire-level `width` / `height` take priority over fillWidth —
    // a fixed-width card in a horizontal rail (content rails, video rails)
    // needs a deterministic width so the children don't stretch the
    // container to their intrinsic size.
    val sizedModifier = run {
        var m = fillModifier
        element.width?.let { w -> m = m.width(w.toInt().dp) }
        element.height?.let { h -> m = m.height(h.toInt().dp) }
        m
    }

    val shadowModifier = effectiveShadowElevationDp?.takeIf { it > 0 }?.let { elev ->
        sizedModifier.shadow(elevation = elev.dp, shape = shape)
    } ?: sizedModifier

    val clippedModifier = if (hasAnyCorner) {
        shadowModifier.clip(shape)
    } else {
        shadowModifier
    }

    // Apply tonal elevation via the semantic surface-color role itself —
    // surfaceContainer / surfaceContainerLow already carry the tonal shift in
    // Material 3. The prototype does not use a Surface composable wrapper.
    val variantBackgroundColor: Color? = if (useVariantBackground) {
        resolveSurfaceRole(
            variantSpec?.backgroundColorRole,
            variantSpec?.customBackgroundArgb
        )?.let { it.copy(alpha = (variantSpec?.backgroundAlpha ?: 1f).coerceIn(0f, 1f)) }
    } else null

    val bgModifier = when {
        variantBackgroundColor != null -> clippedModifier.background(variantBackgroundColor)
        inlineBackground is BackgroundViewModel.Gradient -> clippedModifier.background(inlineBackground.gradient.toBrush())
        inlineBackground is BackgroundViewModel.Solid -> clippedModifier.background(ColorTokenResolver.resolve(inlineBackground.color))
        else -> clippedModifier
    }

    // Accent wash painted over the solid surface to give the hero variant a
    // subtle sense of lift. Layered under content (the modifier still applies
    // before children are laid out) and inside the corner-radius clip.
    val overlayModifier = variantSpec?.gradientOverlay?.let { overlay ->
        val top = resolveSurfaceRole(overlay.topColorRole, null)
            ?.copy(alpha = overlay.topAlpha.coerceIn(0f, 1f)) ?: Color.Transparent
        val bottom = resolveSurfaceRole(overlay.bottomColorRole, null)
            ?.copy(alpha = overlay.bottomAlpha.coerceIn(0f, 1f)) ?: Color.Transparent
        bgModifier.background(Brush.verticalGradient(listOf(top, bottom)))
    } ?: bgModifier

    val borderedModifier = variantSpec?.let { spec ->
        if (spec.borderDp != null && spec.borderDp > 0) {
            val borderColor = resolveSurfaceRole(spec.borderColorRole, null)
                ?: MaterialTheme.colorScheme.outline
            overlayModifier.border(spec.borderDp.dp, borderColor, shape)
        } else overlayModifier
    } ?: overlayModifier

    val paddedModifier = element.padding?.let {
        borderedModifier.padding(
            start = (it.start ?: 0L).toInt().dp,
            end = (it.end ?: 0L).toInt().dp,
            top = (it.top ?: 0L).toInt().dp,
            bottom = (it.bottom ?: 0L).toInt().dp
        )
    } ?: borderedModifier

    val a11y = element.accessibility
    val a11yModifier = paddedModifier.applyAccessibility(a11y)
    val finalModifier = if (a11y?.label != null) a11yModifier.semantics(mergeDescendants = true) {} else a11yModifier

    val mainAxisArrangement = when (element.alignment) {
        Alignment.Center -> if (isRow) Arrangement.Center else Arrangement.Center
        Alignment.End -> if (isRow) Arrangement.End else Arrangement.Bottom
        Alignment.SpaceBetween -> Arrangement.SpaceBetween
        Alignment.SpaceAround -> Arrangement.SpaceAround
        Alignment.SpaceEvenly -> Arrangement.SpaceEvenly
        else -> if (isRow) Arrangement.Start else Arrangement.Top
    }

    val crossAxis = when (element.crossAlignment) {
        CrossAlignment.Center -> if (isRow) ComposeAlignment.CenterVertically else ComposeAlignment.CenterHorizontally
        CrossAlignment.End -> if (isRow) ComposeAlignment.Bottom else ComposeAlignment.End
        CrossAlignment.Stretch -> if (isRow) ComposeAlignment.CenterVertically else ComposeAlignment.CenterHorizontally
        else -> if (isRow) ComposeAlignment.Top else ComposeAlignment.Start
    }

    val containerContent: @Composable () -> Unit = {
        if (isRow) {
            Row(
                modifier = finalModifier,
                horizontalArrangement = mainAxisArrangement as Arrangement.Horizontal,
                verticalAlignment = crossAxis as ComposeAlignment.Vertical
            ) {
                element.children?.forEachIndexed { index, child ->
                    val childModifier = if (child.flex != null && child.flex > 0f) {
                        Modifier.weight(child.flex.toFloat())
                    } else {
                        Modifier
                    }
                    Box(modifier = childModifier) {
                        AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                    if (index < (element.children.size - 1) && gap > 0.dp) {
                        Spacer(modifier = Modifier.width(gap))
                    }
                }
            }
        } else {
            Column(
                modifier = finalModifier,
                verticalArrangement = mainAxisArrangement as Arrangement.Vertical,
                horizontalAlignment = crossAxis as ComposeAlignment.Horizontal
            ) {
                element.children?.forEachIndexed { index, child ->
                    val childModifier = if (child.flex != null && child.flex > 0f) {
                        Modifier.weight(child.flex.toFloat())
                    } else {
                        Modifier
                    }
                    Box(modifier = childModifier) {
                        AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                    if (index < (element.children.size - 1) && gap > 0.dp) {
                        Spacer(modifier = Modifier.height(gap))
                    }
                }
            }
        }
    }

    if (element.badge != null) {
        val badgeAlignment = when (element.badge.alignment) {
            BadgeAlignment.TopStart -> ComposeAlignment.TopStart
            BadgeAlignment.TopEnd -> ComposeAlignment.TopEnd
            BadgeAlignment.BottomStart -> ComposeAlignment.BottomStart
            BadgeAlignment.BottomEnd -> ComposeAlignment.BottomEnd
            BadgeAlignment.TopCenter -> ComposeAlignment.TopCenter
            BadgeAlignment.BottomCenter -> ComposeAlignment.BottomCenter
            BadgeAlignment.Center -> ComposeAlignment.Center
            BadgeAlignment.CenterStart -> ComposeAlignment.CenterStart
            BadgeAlignment.CenterEnd -> ComposeAlignment.CenterEnd
            else -> ComposeAlignment.TopEnd
        }
        Box {
            containerContent()
            element.badge.element?.let { badgeElement ->
                Box(modifier = Modifier.align(badgeAlignment)) {
                    AtomicRouter(badgeElement, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                }
            }
        }
    } else {
        containerContent()
    }
}

/** Parse a hex color string (e.g. "#1A1A2E" or "text.primary") into a Compose [Color]. */
internal fun parseColor(value: String): Color {
    // TODO: resolve semantic tokens (e.g. "text.primary") via theme token map
    return try {
        val hex = value.removePrefix("#")
        val argb = when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> "FF000000"
        }
        Color(argb.toLong(16))
    } catch (_: Exception) {
        Color.Unspecified
    }
}

/**
 * Convert a [BackgroundGradientViewModel] to a Compose [Brush]. Each stop is run
 * through the color resolver so tokens (`"token:..."`) and raw hex strings
 * are both supported.
 */
@Composable
private fun BackgroundGradientViewModel.toBrush(): Brush {
    val colorList = colors.map { ColorTokenResolver.resolve(it) }
    return when (direction) {
        "horizontal" -> Brush.horizontalGradient(colorList)
        "diagonal"   -> Brush.linearGradient(colorList)
        else         -> Brush.verticalGradient(colorList)
    }
}

/**
 * Merge an inline numeric axis value against a variant default using the
 * variant's override-matrix policy. Inline wins on ALLOW; variant wins on
 * LOCK, and the inline attempt is logged.
 */
internal fun <T : Any> resolveAxis(
    axis: String,
    inline: T?,
    variantValue: T?,
    overrideMatrix: Map<String, OverridePolicy>?,
    variantName: String?
): T? {
    if (overrideMatrix == null) return inline
    if (inline == null) return variantValue
    val policy = overrideMatrix[axis] ?: OverridePolicy.ALLOW
    return if (policy == OverridePolicy.LOCK) {
        ContainerVariantResolver.logOverrideBlocked(variantName, axis, inline)
        variantValue
    } else {
        inline
    }
}

/**
 * Resolve a [SurfaceRole] token against the ambient Material 3 color scheme.
 * Returns `null` when [role] is null. `Custom` falls back to [customArgb] or
 * [Color.Unspecified] when no argb is supplied.
 */
@Composable
internal fun resolveSurfaceRole(role: SurfaceRole?, customArgb: Long?): Color? {
    if (role == null) return null
    val scheme = MaterialTheme.colorScheme
    return when (role) {
        SurfaceRole.SurfaceContainerLow -> scheme.surfaceContainerLow
        SurfaceRole.SurfaceContainer    -> scheme.surfaceContainer
        SurfaceRole.Surface             -> scheme.surface
        SurfaceRole.Background          -> scheme.background
        SurfaceRole.PrimaryContainer    -> scheme.primaryContainer
        SurfaceRole.Primary             -> scheme.primary
        SurfaceRole.Scrim               -> scheme.scrim
        SurfaceRole.Custom              -> customArgb?.let { Color(it) } ?: Color.Unspecified
    }
}
