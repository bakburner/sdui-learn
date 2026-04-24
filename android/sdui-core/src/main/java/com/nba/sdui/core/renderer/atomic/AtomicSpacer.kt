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

/**
 * AtomicSpacer — renders a blank Spacer with configurable width,
 * height, or uniform size. Intentionally bypasses [AtomicBox]: a
 * spacer is pure layout and has no box-model chrome (no margin,
 * padding, background, corner radius, border, shadow, opacity, badge).
 */
@Composable
fun AtomicSpacer(element: AtomicElement) {
    var spacerModifier: Modifier = Modifier
    element.size?.let { spacerModifier = spacerModifier.size(it.toInt().dp) }
    element.width?.let { spacerModifier = spacerModifier.width(it.toInt().dp) }
    element.height?.let { spacerModifier = spacerModifier.height(it.toInt().dp) }

    Spacer(modifier = spacerModifier.clearAndSetSemantics {})
}
