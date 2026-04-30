package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.request.RequestEnvelopeBuilder
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
@OptIn(ExperimentalLayoutApi::class)
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

    // TODO(phase3): swap for `LocalSduiFormFactor.current` once the
    // form-factor classifier is plumbed end-to-end.
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    val gap = LayoutTokenResolver.dp(element.gap, formFactor)
    val shouldWrap = element.layoutWrap == true
    // crossAxisGap falls back to gap when absent (CSS shorthand behavior)
    val crossGap = if (element.crossAxisGap != null) {
        LayoutTokenResolver.dp(element.crossAxisGap, formFactor)
    } else {
        gap
    }

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
        CrossAlignment.Stretch -> if (isRow) ComposeAlignment.CenterVertically else ComposeAlignment.Start
        else -> if (isRow) ComposeAlignment.Top else ComposeAlignment.Start
    }

    val batchExecutor = LocalActionExecutor.current

    AtomicBox(element, screenState, onAction) { boxModifier ->
        val a11y = element.accessibility
        val baseModifier = run {
            var m = boxModifier.applyAccessibility(a11y)
            if (a11y?.label != null) m = m.semantics(mergeDescendants = true) {}
            val activateActions = getActivateActions(element.actions)
            if (activateActions.isNotEmpty()) {
                m = m.clickable {
                    if (batchExecutor != null) {
                        batchExecutor(activateActions)
                    } else {
                        activateActions.forEach(onAction)
                    }
                }
            }
            m
        }
        val finalModifier = LayoutTokenResolver.aspectRatio(element.aspectRatio)?.let {
            baseModifier.aspectRatio(it)
        } ?: baseModifier

        if (isRow && shouldWrap) {
            FlowRow(
                modifier = finalModifier,
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalArrangement = Arrangement.spacedBy(crossGap)
            ) {
                element.children?.forEach { child ->
                    val flex = child.flex
                    val childModifier = if (flex != null && flex > 0.0) {
                        Modifier.weight(flex.toFloat())
                    } else {
                        Modifier
                    }
                    Box(modifier = childModifier) {
                        AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                }
            }
        } else if (!isRow && shouldWrap) {
            FlowColumn(
                modifier = finalModifier,
                verticalArrangement = Arrangement.spacedBy(gap),
                horizontalArrangement = Arrangement.spacedBy(crossGap)
            ) {
                element.children?.forEach { child ->
                    val flex = child.flex
                    val childModifier = if (flex != null && flex > 0.0) {
                        Modifier.weight(flex.toFloat())
                    } else {
                        Modifier
                    }
                    Box(modifier = childModifier) {
                        AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                }
            }
        } else if (isRow) {
            Row(
                modifier = finalModifier,
                horizontalArrangement = mainAxisArrangement as Arrangement.Horizontal,
                verticalAlignment = crossAxis as ComposeAlignment.Vertical
            ) {
                element.children?.forEachIndexed { index, child ->
                    val flex = child.flex
                    val alignSelf = child.alignSelf
                    val childModifier = if (flex != null && flex > 0.0) {
                        Modifier
                            .weight(flex.toFloat())
                            .then(resolveRowChildCrossAxis(alignSelf, element.crossAlignment))
                    } else {
                        resolveRowChildCrossAxis(alignSelf, element.crossAlignment)
                    }.let { mod ->
                        // alignSelf alignment override (non-stretch)
                        when (alignSelf) {
                            CrossAlignment.Start -> mod.align(ComposeAlignment.Top)
                            CrossAlignment.Center -> mod.align(ComposeAlignment.CenterVertically)
                            CrossAlignment.End -> mod.align(ComposeAlignment.Bottom)
                            else -> mod
                        }
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
                    val alignSelf = child.alignSelf
                    val childModifier = if (flex != null && flex > 0.0) {
                        Modifier
                            .weight(flex.toFloat())
                            .then(resolveColumnChildCrossAxis(alignSelf, element.crossAlignment))
                    } else {
                        resolveColumnChildCrossAxis(alignSelf, element.crossAlignment)
                    }.let { mod ->
                        // alignSelf alignment override (non-stretch)
                        when (alignSelf) {
                            CrossAlignment.Start -> mod.align(ComposeAlignment.Start)
                            CrossAlignment.Center -> mod.align(ComposeAlignment.CenterHorizontally)
                            CrossAlignment.End -> mod.align(ComposeAlignment.End)
                            else -> mod
                        }
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

/** Resolve cross-axis modifier for a child in a Row (cross axis = vertical). */
private fun resolveRowChildCrossAxis(
    alignSelf: CrossAlignment?,
    parentCrossAlignment: CrossAlignment?
): Modifier {
    val effective = alignSelf ?: parentCrossAlignment
    return if (effective == CrossAlignment.Stretch) Modifier.fillMaxHeight() else Modifier
}

/** Resolve cross-axis modifier for a child in a Column (cross axis = horizontal). */
private fun resolveColumnChildCrossAxis(
    alignSelf: CrossAlignment?,
    parentCrossAlignment: CrossAlignment?
): Modifier {
    val effective = alignSelf ?: parentCrossAlignment
    return if (effective == CrossAlignment.Stretch) Modifier.fillMaxWidth() else Modifier
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
