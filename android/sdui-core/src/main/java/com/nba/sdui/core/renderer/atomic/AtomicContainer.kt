package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Alignment
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.CrossAlignment
import com.nba.sdui.core.models.generated.UIDirection
import com.nba.sdui.core.renderer.ContainerVariantResolver.OverridePolicy
import com.nba.sdui.core.renderer.ContainerVariantResolver.SurfaceRole
import com.nba.sdui.core.renderer.ContainerVariantResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicContainer — renders a Container element as a Row or Column with
 * gap, flex children, and optional responsive breakpoint. All box-model
 * concerns (margin, padding, background, cornerRadius, shadow, border,
 * badge, width/height/fillWidth, opacity, variant chrome) are applied
 * by [AtomicBox] so this composable only owns flex layout.
 *
 * Flex: When a child has a non-null `flex` value, it receives proportional
 * weight along the main axis. Children without `flex` size to content.
 *
 * Breakpoint: When set on this Container, the direction flips from
 * row→column when the screen width is below the breakpoint (dp).
 */
@Composable
fun AtomicContainer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isRow = if (element.breakpoint != null && element.direction == UIDirection.Row) {
        screenWidthDp >= element.breakpoint.toInt()
    } else {
        element.direction == UIDirection.Row
    }

    val gap = element.gap?.toInt()?.dp ?: 0.dp

    val mainAxisArrangement = when (element.alignment) {
        Alignment.Center -> Arrangement.Center
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

    AtomicBox(element, screenState, onAction) { boxModifier ->
        val a11y = element.accessibility
        val finalModifier = run {
            val m = boxModifier.applyAccessibility(a11y)
            if (a11y?.label != null) m.semantics(mergeDescendants = true) {} else m
        }

        if (isRow) {
            Row(
                modifier = finalModifier,
                horizontalArrangement = mainAxisArrangement as Arrangement.Horizontal,
                verticalAlignment = crossAxis as ComposeAlignment.Vertical
            ) {
                element.children?.forEachIndexed { index, child ->
                    val flex = child.flex
                    val childModifier = if (flex != null && flex > 0.0) {
                        Modifier.weight(flex.toFloat())
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
                    val flex = child.flex
                    val childModifier = if (flex != null && flex > 0.0) {
                        Modifier.weight(flex.toFloat())
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
}

/** Parse a hex color string (e.g. "#1A1A2E" or "text.primary") into a Compose [Color]. */
internal fun parseColor(value: String): Color {
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
