package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nba.sdui.core.models.generated.Align
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.TextWeight
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicText — renders a Text element using MaterialTheme typography
 * variants. The text itself owns typography (font / color / alignment /
 * line clamp); margin / padding / bg / cornerRadius / shadow / opacity
 * / fillWidth / badge come from [AtomicBox].
 */
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

    AtomicBox(element, screenState, onAction) { boxModifier ->
        var textModifier = boxModifier
        if (hasActions) {
            textModifier = textModifier.clickable {
                val activateActions = getActivateActions(element.actions)
                if (activateActions.isNotEmpty()) {
                    if (batchExecutor != null) {
                        batchExecutor(activateActions)
                    } else {
                        activateActions.forEach(onAction)
                    }
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
internal fun mapTypographyVariant(variant: String?): TextStyle = when (variant) {
    "displayLarge"  -> MaterialTheme.typography.displayLarge
    "displayMedium" -> MaterialTheme.typography.displayMedium
    "displaySmall"  -> MaterialTheme.typography.displaySmall
    "headlineLarge" -> MaterialTheme.typography.headlineLarge
    "headlineMedium" -> MaterialTheme.typography.headlineMedium
    "headlineSmall" -> MaterialTheme.typography.headlineSmall
    "titleLarge"    -> MaterialTheme.typography.titleLarge
    "titleMedium"   -> MaterialTheme.typography.titleMedium
    "titleSmall"    -> MaterialTheme.typography.titleSmall
    "bodyLarge"     -> MaterialTheme.typography.bodyLarge
    "bodyMedium"    -> MaterialTheme.typography.bodyMedium
    "bodySmall"     -> MaterialTheme.typography.bodySmall
    "labelLarge"    -> MaterialTheme.typography.labelLarge
    "labelMedium"   -> MaterialTheme.typography.labelMedium
    "labelSmall"    -> MaterialTheme.typography.labelSmall
    else            -> MaterialTheme.typography.bodyMedium
}

private val KNOWN_TEXT_VARIANTS = setOf(
    "displayLarge", "displayMedium", "displaySmall",
    "headlineLarge", "headlineMedium", "headlineSmall",
    "titleLarge", "titleMedium", "titleSmall",
    "bodyLarge", "bodyMedium", "bodySmall",
    "labelLarge", "labelMedium", "labelSmall"
)

private fun isKnownTextVariant(variant: String): Boolean = variant in KNOWN_TEXT_VARIANTS

private fun mapFontWeight(weight: TextWeight): FontWeight = when (weight) {
    TextWeight.Regular -> FontWeight.Normal
    TextWeight.Medium -> FontWeight.Medium
    TextWeight.SemiBold -> FontWeight.SemiBold
    TextWeight.Bold -> FontWeight.Bold
}
