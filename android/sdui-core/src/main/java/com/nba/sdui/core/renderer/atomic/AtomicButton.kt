package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.toSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicButton — renders a Button element dispatching actions on click.
 * Supports "primary" (default filled), "secondary" (outlined), "tertiary" (text), and "text" variants.
 */
@Composable
fun AtomicButton(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val onClick: () -> Unit = {
        element.actions?.firstOrNull()?.toSduiAction()?.let(onAction)
    }
    val enabled = element.disabled != true
    val accessibilityModifier = modifier.applyAccessibility(element.accessibility)

    when (element.variant) {
        "primary", null -> Button(onClick = onClick, enabled = enabled, modifier = accessibilityModifier) {
            Text(text = element.label.orEmpty())
        }
        "secondary" -> OutlinedButton(onClick = onClick, enabled = enabled, modifier = accessibilityModifier) {
            Text(text = element.label.orEmpty())
        }
        "tertiary", "text" -> TextButton(onClick = onClick, enabled = enabled, modifier = accessibilityModifier) {
            Text(text = element.label.orEmpty())
        }
        else -> {
            Log.w("AtomicButton", "variant_resolver_missing: variant=\"${element.variant}\" elementId=${element.id}")
            Button(onClick = onClick, enabled = enabled, modifier = accessibilityModifier) {
                Text(text = element.label.orEmpty())
            }
        }
    }
}
