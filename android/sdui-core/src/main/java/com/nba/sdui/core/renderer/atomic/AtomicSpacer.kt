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
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.request.RequestEnvelopeBuilder

/**
 * AtomicSpacer — renders a blank Spacer with configurable width,
 * height, or uniform size. Intentionally bypasses [AtomicBox]: a
 * spacer is pure layout and has no box-model chrome (no margin,
 * padding, background, corner radius, border, shadow, opacity, badge).
 */
@Composable
fun AtomicSpacer(element: AtomicElement) {
    // TODO(phase3): swap for `LocalSduiFormFactor.current` once the
    // form-factor classifier is plumbed end-to-end.
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    var spacerModifier: Modifier = Modifier
    element.size?.let { spacerModifier = spacerModifier.size(it.toInt().dp) }
    element.width?.let { spacerModifier = spacerModifier.width(LayoutTokenResolver.dp(it, formFactor)) }
    element.height?.let { spacerModifier = spacerModifier.height(LayoutTokenResolver.dp(it, formFactor)) }

    Spacer(modifier = spacerModifier.clearAndSetSemantics {})
}
