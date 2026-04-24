package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.runtime.Composable
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.state.SduiAction

private const val MAX_TREE_DEPTH = 6

/**
 * AtomicRouter — dispatches an [AtomicElement] to the correct atomic
 * primitive composable. The router itself is styling-free: every
 * box-model concern (margin, padding, background, cornerRadius,
 * shadow, border, opacity, width / height / fillWidth, variant chrome,
 * badge overlay) is applied by [AtomicBox] inside each primitive.
 *
 * This is the atomic counterpart to SectionRouter. A defensive [depth]
 * guard prevents malformed payloads from causing stack overflows or
 * deep Compose measure-pass issues. Server-side validation is the
 * primary enforcement; this is a safety net for stale caches or
 * manual JSON authoring.
 *
 * [onStateChange] is threaded through so that SectionSlot elements can
 * pass it to SectionRouter for stateful sections (Form, TabGroup).
 *
 * [sectionSlotDepth] tracks how many SectionSlot → AtomicComposite
 * cycles have occurred. See [AtomicSectionSlot] for the recursion
 * guard.
 */
@Composable
fun AtomicRouter(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    if (depth > MAX_TREE_DEPTH) {
        Log.w("AtomicRouter", "Max tree depth ($MAX_TREE_DEPTH) exceeded — skipping element: ${element.type}")
        return
    }
    when (element.type) {
        "Container"       -> AtomicContainer(element, screenState, onAction, depth, onStateChange, sectionSlotDepth)
        "Text"            -> AtomicText(element, screenState, onAction)
        "Image"           -> AtomicImage(element, screenState, onAction)
        "Button"          -> AtomicButton(element, screenState, onAction)
        "Spacer"          -> AtomicSpacer(element)
        "Divider"         -> AtomicDivider(element, screenState, onAction)
        "ScrollContainer" -> AtomicScrollContainer(element, screenState, onAction, depth, onStateChange, sectionSlotDepth)
        "Conditional"     -> AtomicConditional(element, screenState, onAction, depth, onStateChange, sectionSlotDepth)
        "DisplayGrid"     -> AtomicDisplayGrid(element, screenState, onAction)
        "SectionSlot"     -> AtomicSectionSlot(element, screenState, onAction, onStateChange, sectionSlotDepth)
        else              -> Log.w("AtomicRouter", "Unknown atomic element type: ${element.type}")
    }
}
