package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.renderer.SectionRouter
import com.nba.sdui.core.state.SduiAction

private const val MAX_SECTION_SLOT_DEPTH = 2
private const val TAG = "AtomicSectionSlot"

/**
 * AtomicSectionSlot — delegates rendering back to [SectionRouter], completing
 * the bidirectional bridge between the atomic and section layers.
 *
 * ```
 * SectionRouter ──(AtomicComposite)──▶ AtomicRouter
 * AtomicRouter  ──(SectionSlot)──────▶ SectionRouter
 * ```
 *
 * A [sectionSlotDepth] counter prevents infinite recursion when an
 * AtomicComposite contains a SectionSlot that itself renders an AtomicComposite.
 * The limit is [MAX_SECTION_SLOT_DEPTH] (2 cycles).
 */
@Composable
fun AtomicSectionSlot(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier,
    sectionSlotDepth: Int = 0
) {
    if (sectionSlotDepth >= MAX_SECTION_SLOT_DEPTH) {
        Log.w(TAG, "Max SectionSlot depth ($MAX_SECTION_SLOT_DEPTH) exceeded — skipping element: ${element.id}")
        return
    }

    val section = element.section
    if (section == null) {
        Log.w(TAG, "SectionSlot element ${element.id} has no section payload")
        return
    }

    SectionRouter(
        section = section,
        screenState = screenState,
        onAction = onAction,
        onStateChange = onStateChange,
        modifier = modifier,
        sectionSlotDepth = sectionSlotDepth + 1
    )
}
