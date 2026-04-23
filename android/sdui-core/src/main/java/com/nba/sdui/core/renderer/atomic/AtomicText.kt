package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicText — renders a Text element using MaterialTheme typography variants.
 */
@Composable
fun AtomicText(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
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
        "start" -> TextAlign.Start
        "center" -> TextAlign.Center
        "end" -> TextAlign.End
        else -> null
    }

    Text(
        text = element.content.orEmpty(),
        style = style,
        fontWeight = fontWeight,
        color = textColor,
        textAlign = textAlign,
        maxLines = element.maxLines ?: Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.applyAccessibility(element.accessibility)
    )
}

/**
 * Map schema variant strings to MaterialTheme typography styles.
 */
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

private fun mapFontWeight(weight: String): FontWeight = when (weight) {
    "thin"       -> FontWeight.Thin
    "extraLight" -> FontWeight.ExtraLight
    "light"      -> FontWeight.Light
    "normal"     -> FontWeight.Normal
    "medium"     -> FontWeight.Medium
    "semiBold"   -> FontWeight.SemiBold
    "bold"       -> FontWeight.Bold
    "extraBold"  -> FontWeight.ExtraBold
    "black"      -> FontWeight.Black
    else         -> FontWeight.Normal
}
