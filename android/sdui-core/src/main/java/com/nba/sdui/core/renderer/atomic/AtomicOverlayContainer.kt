package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.BadgeAlignment
import com.nba.sdui.core.models.generated.Spacing
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

@Composable
fun AtomicOverlayContainer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    val base = element.base
    if (base == null) {
        Log.w(TAG, "Missing OverlayContainer base; skipping id=${element.id}")
        return
    }

    AtomicBox(element, screenState, onAction) { boxModifier ->
        Box(modifier = boxModifier.applyAccessibility(element.accessibility)) {
            AtomicRouter(base, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
            element.overlays.orEmpty().forEach { overlay ->
                Box(
                    modifier = Modifier
                        .align(composeAlignment(overlay.alignment))
                        .then(insetModifier(overlay.inset))
                ) {
                    AtomicRouter(overlay.element, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                }
            }
        }
    }
}

private fun composeAlignment(alignment: BadgeAlignment?): ComposeAlignment {
    return when (alignment) {
        BadgeAlignment.TopStart -> ComposeAlignment.TopStart
        BadgeAlignment.TopCenter -> ComposeAlignment.TopCenter
        BadgeAlignment.TopEnd -> ComposeAlignment.TopEnd
        BadgeAlignment.CenterStart -> ComposeAlignment.CenterStart
        BadgeAlignment.Center -> ComposeAlignment.Center
        BadgeAlignment.CenterEnd -> ComposeAlignment.CenterEnd
        BadgeAlignment.BottomStart -> ComposeAlignment.BottomStart
        BadgeAlignment.BottomEnd -> ComposeAlignment.BottomEnd
        BadgeAlignment.BottomCenter, null -> ComposeAlignment.BottomCenter
    }
}

private fun insetModifier(inset: Spacing?): Modifier {
    if (inset == null) return Modifier
    return Modifier.padding(
        start = (inset.start ?: 0L).toInt().dp,
        top = (inset.top ?: 0L).toInt().dp,
        end = (inset.end ?: 0L).toInt().dp,
        bottom = (inset.bottom ?: 0L).toInt().dp
    )
}

private const val TAG = "AtomicOverlayContainer"
