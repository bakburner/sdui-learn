package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.state.SduiAction

private const val MAX_TREE_DEPTH = 6

/**
 * AtomicRouter — dispatches an [AtomicElement] to the correct atomic primitive composable.
 *
 * This is the atomic counterpart to SectionRouter. It renders a tree of server-composed
 * UI primitives using platform-native Compose components.
 *
 * A defensive [depth] guard prevents malformed payloads from causing stack overflows
 * or deep Compose measure-pass issues. Server-side validation is the primary enforcement;
 * this is a safety net for stale caches or manual JSON authoring.
 *
 * [onStateChange] is threaded through so that SectionSlot elements can pass it to
 * SectionRouter for stateful sections (Form, TabGroup).
 *
 * [sectionSlotDepth] tracks how many SectionSlot→AtomicComposite cycles have occurred.
 * See [AtomicSectionSlot] for the recursion guard.
 */
@Composable
fun AtomicRouter(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    if (depth > MAX_TREE_DEPTH) {
        Log.w("AtomicRouter", "Max tree depth ($MAX_TREE_DEPTH) exceeded — skipping element: ${element.type}")
        return
    }
    val opacityModifier = element.opacity?.let { modifier.alpha(it.toFloat()) } ?: modifier
    when (element.type) {
        "Container"       -> AtomicContainer(element, screenState, onAction, opacityModifier, depth, onStateChange, sectionSlotDepth)
        "Text"            -> AtomicText(element, screenState, onAction, opacityModifier)
        "Image"           -> AtomicImage(element, screenState, onAction, opacityModifier)
        "Button"          -> AtomicButton(element, screenState, onAction, opacityModifier)
        "Spacer"          -> AtomicSpacer(element, screenState, onAction, opacityModifier)
        "Divider"         -> AtomicDivider(element, screenState, onAction, opacityModifier)
        "ScrollContainer" -> AtomicScrollContainer(element, screenState, onAction, opacityModifier, depth, onStateChange, sectionSlotDepth)
        "Conditional"     -> AtomicConditional(element, screenState, onAction, opacityModifier, depth, onStateChange, sectionSlotDepth)
        "DisplayGrid"     -> AtomicDisplayGrid(element, screenState, onAction, opacityModifier)
        "SectionSlot"     -> AtomicSectionSlot(element, screenState, onAction, onStateChange, opacityModifier, sectionSlotDepth)
        else              -> Log.w("AtomicRouter", "Unknown atomic element type: ${element.type}")
    }
}
