package com.nba.sdui.core.renderer.atomic

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.Orientation
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicDivider — renders a horizontal or vertical divider line.
 */
@Composable
fun AtomicDivider(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val thickness = element.thickness?.toInt()?.dp ?: 1.dp
    val resolved = ColorTokenResolver.resolve(element.color)
    val color = resolved.takeIf { it != Color.Unspecified }

    if (element.orientation == Orientation.Vertical) {
        if (color != null) {
            VerticalDivider(thickness = thickness, color = color, modifier = modifier.clearAndSetSemantics {})
        } else {
            VerticalDivider(thickness = thickness, modifier = modifier.clearAndSetSemantics {})
        }
    } else {
        if (color != null) {
            HorizontalDivider(thickness = thickness, color = color, modifier = modifier.clearAndSetSemantics {})
        } else {
            HorizontalDivider(thickness = thickness, modifier = modifier.clearAndSetSemantics {})
        }
    }
}
