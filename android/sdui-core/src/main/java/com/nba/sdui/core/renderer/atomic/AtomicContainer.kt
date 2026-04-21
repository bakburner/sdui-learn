package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.models.Background
import com.nba.sdui.core.models.BackgroundGradient
import com.nba.sdui.core.models.parseBackground
import com.nba.sdui.core.renderer.applyAccessibility
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
    val isRow = if (element.breakpoint != null && element.direction == "row") {
        screenWidthDp >= element.breakpoint
    } else {
        element.direction == "row"
    }

    val gap = element.gap?.dp ?: 0.dp

    val fillModifier = if (element.fillWidth == true) modifier.fillMaxWidth() else modifier

    val shape = RoundedCornerShape((element.cornerRadius ?: 0).dp)
    val shadowModifier = element.shadow?.let { s ->
        fillModifier.shadow(
            elevation = (s.radius ?: 0.0).toFloat().dp,
            shape = shape
        )
    } ?: fillModifier

    val clippedModifier = element.cornerRadius?.let {
        shadowModifier.clip(shape)
    } ?: shadowModifier

    val bg = parseBackground(element.background)
    val bgModifier = when (bg) {
        is Background.Gradient -> clippedModifier.background(bg.gradient.toBrush())
        is Background.Solid -> clippedModifier.background(parseColor(bg.color))
        is Background.Image -> clippedModifier
        null -> clippedModifier
    }

    val paddedModifier = element.padding?.let {
        bgModifier.padding(
            start = it.start.dp,
            end = it.end.dp,
            top = it.top.dp,
            bottom = it.bottom.dp
        )
    } ?: bgModifier

    val a11y = element.accessibility
    val a11yModifier = paddedModifier.applyAccessibility(a11y)
    val finalModifier = if (a11y?.label != null) a11yModifier.semantics(mergeDescendants = true) {} else a11yModifier

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

    val containerContent: @Composable () -> Unit = {
        if (isRow) {
            Row(
                modifier = finalModifier,
                horizontalArrangement = mainAxisArrangement as Arrangement.Horizontal,
                verticalAlignment = crossAxis as ComposeAlignment.Vertical
            ) {
                element.children?.forEachIndexed { index, child ->
                    val childModifier = if (child.flex != null && child.flex > 0f) {
                        Modifier.weight(child.flex)
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
                        Modifier.weight(child.flex)
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
            "topStart" -> ComposeAlignment.TopStart
            "topEnd" -> ComposeAlignment.TopEnd
            "bottomStart" -> ComposeAlignment.BottomStart
            "bottomEnd" -> ComposeAlignment.BottomEnd
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

/** Convert a [BackgroundGradient] to a Compose [Brush]. */
private fun BackgroundGradient.toBrush(): Brush {
    val colorList = colors.map { parseColor(it) }
    return when (direction) {
        "horizontal" -> Brush.horizontalGradient(colorList)
        "diagonal"   -> Brush.linearGradient(colorList)
        else         -> Brush.verticalGradient(colorList)
    }
}
