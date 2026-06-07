package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.ActionTrigger
import com.nba.sdui.core.models.generated.BackgroundUnion
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.IconTokenResolver
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
    val activateActions = selectActions(element.actions, ActionTrigger.OnActivate)
    val focusActions = selectActions(element.actions, ActionTrigger.OnFocus)
    val blurActions = selectActions(element.actions, ActionTrigger.OnBlur)
    var hadFocus by remember(element.id) { mutableStateOf(false) }
    val onClick: () -> Unit = {
        dispatchActions(activateActions, batchExecutor, onAction)
    }
    val enabled = element.disabled != true

    // Resolve `label` from `bindRef` when present, falling back to the
    // inline `label`. Lets composers rebind CTA copy without rewriting
    // the ui tree.
    val compositeContent = LocalCompositeContent.current
    val resolvedLabel = BindRefResolver.resolveString(element.bindRef, compositeContent)
        ?: element.label.orEmpty()

    AtomicBox(element, screenState, onAction) { boxModifier ->
        val buttonModifier = boxModifier
            .applyAccessibility(element.accessibility)
            .onFocusChanged { focusState ->
                if (focusState.isFocused == hadFocus) return@onFocusChanged
                hadFocus = focusState.isFocused
                if (focusState.isFocused) {
                    dispatchActions(focusActions, batchExecutor, onAction)
                } else {
                    dispatchActions(blurActions, batchExecutor, onAction)
                }
            }

        val inlineTextColor = ColorTokenResolver.resolve(element.color).takeIf { it != Color.Unspecified }
        val inlineBgColor = (element.backgrounds?.firstOrNull() as? BackgroundUnion.StringValue)?.value?.let {
            ColorTokenResolver.resolve(it).takeIf { c -> c != Color.Unspecified }
        }

        val isTextVariant = element.variant == "tertiary" || element.variant == "text"
        val customColors: ButtonColors? = when {
            inlineTextColor == null && inlineBgColor == null -> null
            isTextVariant -> ButtonDefaults.textButtonColors(
                contentColor = inlineTextColor ?: Color.Unspecified,
                containerColor = inlineBgColor ?: Color.Transparent
            )
            else -> ButtonDefaults.buttonColors(
                containerColor = inlineBgColor ?: Color.Unspecified,
                contentColor = inlineTextColor ?: Color.Unspecified
            )
        }

        val iconVector = element.icon?.let { IconTokenResolver.imageVector(it) }
        if (!element.icon.isNullOrBlank() && iconVector == null) {
            Log.w("AtomicButton", "button icon not mapped: icon=${element.icon} elementId=${element.id}")
        }

        val labelContent: @Composable () -> Unit = {
            Row {
                if (iconVector != null) {
                    Icon(imageVector = iconVector, contentDescription = null)
                    if (resolvedLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                    }
                }
                if (resolvedLabel.isNotEmpty()) {
                    Text(text = resolvedLabel)
                }
            }
        }

        when (element.variant) {
            "primary", null -> Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                colors = customColors ?: ButtonDefaults.buttonColors()
            ) { labelContent() }
            "secondary" -> OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                colors = if (inlineTextColor != null || inlineBgColor != null) {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = inlineTextColor ?: Color.Unspecified,
                        containerColor = inlineBgColor ?: Color.Unspecified
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) { labelContent() }
            "tertiary", "text" -> TextButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier,
                colors = customColors ?: ButtonDefaults.textButtonColors()
            ) { labelContent() }
            else -> {
                Log.w("AtomicButton", "variant_resolver_missing: variant=\"${element.variant}\" elementId=${element.id}")
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = buttonModifier,
                    colors = customColors ?: ButtonDefaults.buttonColors()
                ) { labelContent() }
            }
        }
    }
}
