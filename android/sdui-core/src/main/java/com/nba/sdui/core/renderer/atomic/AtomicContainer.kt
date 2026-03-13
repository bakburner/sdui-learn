package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.models.BackgroundGradient
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicContainer — renders a Container element as a Column or Row with
 * optional padding, gap, background color, and gradient.
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
    val isRow = element.direction == "row"
    val gap = element.gap?.dp ?: 0.dp

    val fillModifier = if (element.fillWidth == true) modifier.fillMaxWidth() else modifier

    val clippedModifier = element.cornerRadius?.let {
        fillModifier.clip(RoundedCornerShape(it.dp))
    } ?: fillModifier

    val bgModifier = element.backgroundGradient?.let { gradient ->
        clippedModifier.background(gradient.toBrush())
    } ?: element.backgroundColor?.let { hex ->
        clippedModifier.background(parseColor(hex))
    } ?: clippedModifier

    val paddedModifier = element.padding?.let {
        bgModifier.padding(
            start = it.start.dp,
            end = it.end.dp,
            top = it.top.dp,
            bottom = it.bottom.dp
        )
    } ?: bgModifier

    val mainAxisArrangement = when (element.alignment) {
        "center"       -> if (isRow) Arrangement.Center else Arrangement.Center
        "end"          -> if (isRow) Arrangement.End else Arrangement.Bottom
        "spaceBetween" -> Arrangement.SpaceBetween
        "spaceAround"  -> Arrangement.SpaceAround
        "spaceEvenly"  -> Arrangement.SpaceEvenly
        else           -> if (isRow) Arrangement.Start else Arrangement.Top
    }

    val crossAxis = when (element.crossAlignment) {
        "center"  -> if (isRow) ComposeAlignment.CenterVertically else ComposeAlignment.CenterHorizontally
        "end"     -> if (isRow) ComposeAlignment.Bottom else ComposeAlignment.End
        "stretch" -> if (isRow) ComposeAlignment.CenterVertically else ComposeAlignment.CenterHorizontally
        else      -> if (isRow) ComposeAlignment.Top else ComposeAlignment.Start
    }

    if (isRow) {
        Row(
            modifier = paddedModifier,
            horizontalArrangement = mainAxisArrangement as Arrangement.Horizontal,
            verticalAlignment = crossAxis as ComposeAlignment.Vertical
        ) {
            element.children?.forEachIndexed { index, child ->
                AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                if (index < (element.children.size - 1) && gap > 0.dp) {
                    Spacer(modifier = Modifier.width(gap))
                }
            }
        }
    } else {
        Column(
            modifier = paddedModifier,
            verticalArrangement = mainAxisArrangement as Arrangement.Vertical,
            horizontalAlignment = crossAxis as ComposeAlignment.Horizontal
        ) {
            element.children?.forEachIndexed { index, child ->
                AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                if (index < (element.children.size - 1) && gap > 0.dp) {
                    Spacer(modifier = Modifier.height(gap))
                }
            }
        }
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

/** Convert a [BackgroundGradient] to a Compose [Brush]. */
private fun BackgroundGradient.toBrush(): Brush {
    val colorList = colors.map { parseColor(it) }
    return when (direction) {
        "horizontal" -> Brush.horizontalGradient(colorList)
        "diagonal"   -> Brush.linearGradient(colorList)
        else         -> Brush.verticalGradient(colorList)
    }
}
