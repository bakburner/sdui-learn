package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.nba.sdui.core.models.generated.Align
import com.nba.sdui.core.models.generated.ActionTrigger
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.TextWeight
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.tokens.LayoutTokenRegistry

/**
 * AtomicText — renders a Text element using MaterialTheme typography
 * variants. The text itself owns typography (font / color / alignment /
 * line clamp); margin / padding / bg / cornerRadius / shadow / opacity
 * / fillWidth / badge come from [AtomicBox].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtomicText(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val baseStyle = mapTypographyVariant(element.variant)
    if (element.variant != null && !isKnownTextVariant(element.variant)) {
        Log.w("AtomicText", "variant_resolver_missing: variant=\"${element.variant}\" elementId=${element.id}")
    }
    val style = if (element.monospacedDigits == true) {
        baseStyle.copy(fontFeatureSettings = "tnum")
    } else {
        baseStyle
    }
    val fontWeight = element.weight?.let { mapFontWeight(it) }
    val textColor = ColorTokenResolver.resolve(element.color)
    val textAlign = when (element.textAlign) {
        Align.Start -> TextAlign.Start
        Align.Center -> TextAlign.Center
        Align.End -> TextAlign.End
        else -> null
    }

    // Resolve `content` from `bindRef` when present, falling back to the
    // inline `content` property. A leaf with a bindRef but no matching
    // `data.content` entry falls back to its inline value rather than
    // rendering nothing — this keeps the first paint usable while the
    // first real-time update is in flight.
    val compositeContent = LocalCompositeContent.current
    val boundPreview = BindRefResolver.resolveString(element.bindRef, compositeContent)
    val resolvedContent = remember(boundPreview, element.content) {
        boundPreview ?: element.content.orEmpty()
    }

    val hasActions = !element.actions.isNullOrEmpty()
    val batchExecutor = LocalActionExecutor.current
    val activateActions = selectActions(element.actions, ActionTrigger.OnActivate)
    val longPressActions = selectActions(element.actions, ActionTrigger.OnLongPress)
    val focusActions = selectActions(element.actions, ActionTrigger.OnFocus)
    val blurActions = selectActions(element.actions, ActionTrigger.OnBlur)
    val isFocusable = focusActions.isNotEmpty() || blurActions.isNotEmpty()
    var hadFocus by remember(element.id) { mutableStateOf(false) }

    AtomicBox(element, screenState, onAction) { boxModifier ->
        var textModifier = boxModifier
        if (hasActions || longPressActions.isNotEmpty()) {
            textModifier = textModifier.combinedClickable(
                onClick = { dispatchActions(activateActions, batchExecutor, onAction) },
                onLongClick = if (longPressActions.isNotEmpty()) {
                    { dispatchActions(longPressActions, batchExecutor, onAction) }
                } else {
                    null
                }
            )
        }
        if (isFocusable) {
            textModifier = textModifier
                .focusable()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused == hadFocus) return@onFocusChanged
                    hadFocus = focusState.isFocused
                    if (focusState.isFocused) {
                        dispatchActions(focusActions, batchExecutor, onAction)
                    } else {
                        dispatchActions(blurActions, batchExecutor, onAction)
                    }
                }
        }
        Text(
            text = resolvedContent,
            style = style,
            fontWeight = fontWeight,
            color = textColor,
            textAlign = textAlign,
            maxLines = element.maxLines?.toInt() ?: Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier = textModifier.applyAccessibility(element.accessibility)
        )
    }
}

@Composable
internal fun mapTypographyVariant(variant: String?): TextStyle {
    val fallback = MaterialTheme.typography.bodyMedium
    if (variant.isNullOrBlank()) return fallback
    val config = LocalConfiguration.current
    val formFactor = LayoutTokenResolver.currentFormFactor(config)
    val spec = LayoutTokenResolver.typographyForVariant(variant, formFactor) ?: return fallback
    // Registry-driven: size + weight + lineHeight come from
    // `schema/typography-tokens.json`. Element-level `weight` (applied separately by the
    // renderer) still overrides the category's base weight for emphasis variants.
    return TextStyle(
        fontSize = spec.size.sp,
        fontWeight = FontWeight(spec.weight),
        lineHeight = (spec.size * spec.lineHeight).sp
    )
}

private fun isKnownTextVariant(variant: String): Boolean =
    LayoutTokenRegistry.typographyVariants.containsKey("nba.typography.$variant")

private fun mapFontWeight(weight: TextWeight): FontWeight = when (weight) {
    TextWeight.Regular -> FontWeight.Normal
    TextWeight.Medium -> FontWeight.Medium
    TextWeight.SemiBold -> FontWeight.SemiBold
    TextWeight.Bold -> FontWeight.Bold
}
