package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.BadgeAlignment
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.ContainerVariantResolver
import com.nba.sdui.core.renderer.ContainerVariantResolver.OverridePolicy
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.renderer.adapters.BackgroundGradientViewModel
import com.nba.sdui.core.renderer.adapters.BackgroundViewModel
import com.nba.sdui.core.renderer.adapters.toViewModel
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import com.nba.sdui.core.state.SduiAction

private const val TAG = "AtomicBox"

/**
 * AtomicBox — the single site for every AtomicElement's box model on
 * the Android client.
 *
 * Every primitive renderer wraps its content in [AtomicBox] so margin
 * / padding / background / cornerRadius / shadow / border / opacity /
 * width / height / fillWidth / variant chrome / badge overlay live in
 * exactly one place instead of being re-implemented per primitive.
 *
 * The rendering order is fixed so every primitive produces the same
 * box-model shape on the wire:
 *
 *     margin (outer)
 *       └─ opacity
 *            └─ shadow + background + border + cornerRadius
 *                 └─ padding
 *                      └─ content (inner)
 *
 * Rules:
 *   - `margin` is the outermost layer so sibling-to-sibling spacing is
 *     untouched by the element's own bg / clip.
 *   - `padding` lives *inside* the variant chrome so bg + corner clip
 *     extends to the padded frame.
 *   - `width` / `height` / `fillWidth` size the padded frame (border-box
 *     semantics).
 *   - `variant` is resolved once here and merged with inline
 *     background / cornerRadius / shadow per the variant's
 *     `overrideMatrix`.
 *
 * Each primitive composable accepts a pre-built [Modifier] via the
 * `content` lambda and applies it to its outermost native composable
 * (Row / Column / LazyRow / AsyncImage / Text / HorizontalDivider /
 * ...). Accessibility labels and action triggers remain per-primitive
 * so each renderer can supply its own semantically appropriate
 * fallback label and tap handling.
 */
@Composable
fun AtomicBox(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val boxModifier = Modifier.buildAtomicBox(element)

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
            content(boxModifier)
            element.badge.element?.let { badgeElement ->
                Box(modifier = Modifier.align(badgeAlignment)) {
                    AtomicRouter(badgeElement, screenState, onAction)
                }
            }
        }
    } else {
        content(boxModifier)
    }
}

/**
 * Builds the full box-model [Modifier] chain for [element] in the
 * canonical order (margin → opacity → shadow → bg + border + corner →
 * padding → sizing). Exposed for primitives that need to customise
 * the content lambda but still want the standard box model.
 */
@Composable
fun Modifier.buildAtomicBox(element: AtomicElement): Modifier {
    // TODO(phase3): swap for `LocalSduiFormFactor.current` once the
    // form-factor classifier is plumbed end-to-end.
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    val variantSpec = ContainerVariantResolver.resolve(element.variant, formFactor)

    val effectiveCornerRadius: Int? = resolveAxis(
        axis = "cornerRadius",
        inline = element.cornerRadius?.let { LayoutTokenResolver.intValue(it, formFactor) },
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
    if (inlineBackground is BackgroundViewModel.Image) {
        Log.w(TAG, "Atomic background image is decoded but constrained out of mobile atomic rendering; use section.surface background or an Image child")
    }
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

    val radii = element.cornerRadii
    val perCornerTopStart = radii?.topStart?.let { LayoutTokenResolver.intValue(it, formFactor) }
    val perCornerTopEnd = radii?.topEnd?.let { LayoutTokenResolver.intValue(it, formFactor) }
    val perCornerBottomStart = radii?.bottomStart?.let { LayoutTokenResolver.intValue(it, formFactor) }
    val perCornerBottomEnd = radii?.bottomEnd?.let { LayoutTokenResolver.intValue(it, formFactor) }
    val anyPerCornerSet = (perCornerTopStart ?: 0) > 0 ||
        (perCornerTopEnd ?: 0) > 0 ||
        (perCornerBottomStart ?: 0) > 0 ||
        (perCornerBottomEnd ?: 0) > 0
    val hasAnyCorner = (effectiveCornerRadius != null && effectiveCornerRadius > 0) || anyPerCornerSet

    val shape = if (radii != null && anyPerCornerSet) {
        val fallback = effectiveCornerRadius ?: 0
        RoundedCornerShape(
            topStart = (perCornerTopStart ?: fallback).dp,
            topEnd = (perCornerTopEnd ?: fallback).dp,
            bottomEnd = (perCornerBottomEnd ?: fallback).dp,
            bottomStart = (perCornerBottomStart ?: fallback).dp
        )
    } else {
        RoundedCornerShape((effectiveCornerRadius ?: 0).dp)
    }

    val variantBackgroundColor: Color? = if (useVariantBackground) {
        resolveSurfaceRole(
            variantSpec?.backgroundColorRole,
            variantSpec?.customBackgroundArgb
        )?.copy(alpha = (variantSpec?.backgroundAlpha ?: 1f).coerceIn(0f, 1f))
    } else null

    var result: Modifier = this

    // margin (outermost)
    element.margin?.let {
        result = result.padding(
            start = LayoutTokenResolver.dp(it.start, formFactor),
            end = LayoutTokenResolver.dp(it.end, formFactor),
            top = LayoutTokenResolver.dp(it.top, formFactor),
            bottom = LayoutTokenResolver.dp(it.bottom, formFactor)
        )
    }

    // opacity
    element.opacity?.let {
        result = result.alpha(it.toFloat())
    }

    // shadow
    if (effectiveShadowElevationDp != null && effectiveShadowElevationDp > 0) {
        result = result.shadow(elevation = effectiveShadowElevationDp.dp, shape = shape)
    }

    // corner clip
    if (hasAnyCorner) {
        result = result.clip(shape)
    }

    // background
    result = when {
        variantBackgroundColor != null -> result.background(variantBackgroundColor)
        inlineBackground is BackgroundViewModel.Gradient -> result.background(inlineBackground.gradient.toBrush())
        inlineBackground is BackgroundViewModel.Solid -> result.background(ColorTokenResolver.resolve(inlineBackground.color))
        else -> result
    }

    // gradient overlay (hero variant only today)
    variantSpec?.gradientOverlay?.let { overlay ->
        val top = resolveSurfaceRole(overlay.topColorRole, null)
            ?.copy(alpha = overlay.topAlpha.coerceIn(0f, 1f)) ?: Color.Transparent
        val bottom = resolveSurfaceRole(overlay.bottomColorRole, null)
            ?.copy(alpha = overlay.bottomAlpha.coerceIn(0f, 1f)) ?: Color.Transparent
        result = result.background(Brush.verticalGradient(listOf(top, bottom)))
    }

    // border
    variantSpec?.let { spec ->
        if (spec.borderDp != null && spec.borderDp > 0) {
            val borderColor = resolveSurfaceRole(spec.borderColorRole, null)
                ?: MaterialTheme.colorScheme.outline
            result = result.border(spec.borderDp.dp, borderColor, shape)
        }
    }

    // padding (inner, sits inside bg/clip)
    element.padding?.let {
        result = result.padding(
            start = LayoutTokenResolver.dp(it.start, formFactor),
            end = LayoutTokenResolver.dp(it.end, formFactor),
            top = LayoutTokenResolver.dp(it.top, formFactor),
            bottom = LayoutTokenResolver.dp(it.bottom, formFactor)
        )
    }

    // sizing — explicit width/height beat fillWidth. Preserve null-short-circuit
    // semantics: a missing scalar must NOT collapse the frame to 0.dp.
    if (effectiveFillWidth && element.width == null) {
        result = result.fillMaxWidth()
    }
    element.width?.let { result = result.width(LayoutTokenResolver.dp(it, formFactor)) }
    element.height?.let { result = result.height(LayoutTokenResolver.dp(it, formFactor)) }

    return result
}

@Composable
private fun BackgroundGradientViewModel.toBrush(): Brush {
    val colorList = colors.map { ColorTokenResolver.resolve(it) }
    return when (direction) {
        "horizontal" -> Brush.horizontalGradient(colorList)
        "diagonal"   -> Brush.linearGradient(colorList)
        else         -> Brush.verticalGradient(colorList)
    }
}
