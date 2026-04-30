package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.BackgroundUnion
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicButton — renders a Button dispatching actions on click. Supports
 * `primary` (filled), `secondary` (outlined), `tertiary` / `text`
 * (text-only) variants. Box-model concerns (margin / padding / bg /
 * shadow / fillWidth / opacity / badge) come from [AtomicBox]; the
 * Material button owns its own internal chrome per-variant.
 */
@Composable
fun AtomicButton(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val batchExecutor = LocalActionExecutor.current
    val onClick: () -> Unit = {
        val activateActions = getActivateActions(element.actions)
        if (activateActions.isNotEmpty()) {
            if (batchExecutor != null) {
                batchExecutor(activateActions)
            } else {
                activateActions.forEach(onAction)
            }
        }
    }
    val enabled = element.disabled != true
    if (!element.icon.isNullOrBlank()) {
        Log.w("AtomicButton", "button icon is decoded but not rendered on Android yet: icon=${element.icon} elementId=${element.id}")
    }

    // Resolve `label` from `bindRef` when present, falling back to the
    // inline `label`. Lets composers rebind CTA copy without rewriting
    // the ui tree.
    val compositeContent = LocalCompositeContent.current
    val resolvedLabel = BindRefResolver.resolveString(element.bindRef, compositeContent)
        ?: element.label.orEmpty()

    AtomicBox(element, screenState, onAction) { boxModifier ->
        val buttonModifier = boxModifier.applyAccessibility(element.accessibility)

        // Resolve inline color overrides from the element. These let the
        // server override the variant's default chrome (e.g. white-on-brand
        // buttons on subscribe surfaces) without adding new variant values.
        val inlineTextColor = ColorTokenResolver.resolve(element.color).takeIf { it != Color.Unspecified }
        val inlineBgColor = (element.background as? BackgroundUnion.StringValue)?.value?.let {
            ColorTokenResolver.resolve(it).takeIf { c -> c != Color.Unspecified }
        }
        val customColors = if (inlineTextColor != null || inlineBgColor != null) {
            ButtonDefaults.buttonColors(
                containerColor = inlineBgColor ?: Color.Unspecified,
                contentColor = inlineTextColor ?: Color.Unspecified
            )
        } else null

        when (element.variant) {
            "primary", null -> Button(onClick = onClick, enabled = enabled, modifier = buttonModifier, colors = customColors ?: ButtonDefaults.buttonColors()) {
                Text(text = resolvedLabel)
            }
            "secondary" -> OutlinedButton(onClick = onClick, enabled = enabled, modifier = buttonModifier, colors = customColors ?: ButtonDefaults.outlinedButtonColors()) {
                Text(text = resolvedLabel)
            }
            "tertiary", "text" -> TextButton(onClick = onClick, enabled = enabled, modifier = buttonModifier, colors = customColors ?: ButtonDefaults.textButtonColors()) {
                Text(text = resolvedLabel)
            }
            else -> {
                Log.w("AtomicButton", "variant_resolver_missing: variant=\"${element.variant}\" elementId=${element.id}")
                Button(onClick = onClick, enabled = enabled, modifier = buttonModifier, colors = customColors ?: ButtonDefaults.buttonColors()) {
                    Text(text = resolvedLabel)
                }
            }
        }
    }
}
