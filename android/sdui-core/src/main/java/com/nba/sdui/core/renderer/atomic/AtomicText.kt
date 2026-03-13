package com.nba.sdui.core.renderer.atomic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nba.sdui.core.models.AtomicElement
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
    val style = mapTypographyVariant(element.variant)
    val fontWeight = element.weight?.let { mapFontWeight(it) }
    val textColor = element.color?.let { parseColor(it) } ?: Color.Unspecified

    Text(
        text = element.content.orEmpty(),
        style = style,
        fontWeight = fontWeight,
        color = textColor,
        maxLines = element.maxLines ?: Int.MAX_VALUE,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
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
