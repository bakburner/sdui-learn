package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicSpacer — renders a blank Spacer with configurable width, height, or uniform size.
 */
@Composable
fun AtomicSpacer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var spacerModifier = modifier
    element.size?.let { spacerModifier = spacerModifier.size(it.toInt().dp) }
    element.width?.let { spacerModifier = spacerModifier.width(it.toInt().dp) }
    element.height?.let { spacerModifier = spacerModifier.height(it.toInt().dp) }

    Spacer(modifier = spacerModifier.clearAndSetSemantics {})
}
