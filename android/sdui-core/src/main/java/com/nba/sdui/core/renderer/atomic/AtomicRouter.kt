package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicRouter — dispatches an [AtomicElement] to the correct atomic primitive composable.
 *
 * This is the atomic counterpart to SectionRouter. It renders a tree of server-composed
 * UI primitives using platform-native Compose components.
 */
@Composable
fun AtomicRouter(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (element.type) {
        "Container"       -> AtomicContainer(element, screenState, onAction, modifier)
        "Text"            -> AtomicText(element, modifier)
        "Image"           -> AtomicImage(element, onAction, modifier)
        "Button"          -> AtomicButton(element, onAction, modifier)
        "Spacer"          -> AtomicSpacer(element, modifier)
        "Divider"         -> AtomicDivider(element, modifier)
        "ScrollContainer" -> AtomicScrollContainer(element, screenState, onAction, modifier)
        "Conditional"     -> AtomicConditional(element, screenState, onAction, modifier)
        "DataTable"       -> AtomicDataTable(element, screenState, onAction, modifier)
        else              -> Log.w("AtomicRouter", "Unknown atomic element type: ${element.type}")
    }
}
