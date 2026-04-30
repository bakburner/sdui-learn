package com.nba.sdui.core.renderer.atomic

import androidx.compose.runtime.compositionLocalOf
import com.nba.sdui.core.models.generated.Action
import com.nba.sdui.core.models.generated.ActionTrigger
import com.nba.sdui.core.renderer.adapters.toSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * Provides batch action execution to the atomic subtree. When an element's
 * primary activation trigger fires, primitives collect ALL matching actions
 * and pass the ordered list to this executor so cross-action failure policies
 * (halt/continue/silent) are honored across the batch.
 *
 * Set by the screen host (e.g. GameDetailScreen) alongside the single-action
 * `onAction` lambda. Primitives prefer this for activation; `onAction` remains
 * for backward compat with section renderers that fire individual actions.
 */
val LocalActionExecutor = compositionLocalOf<((List<SduiAction>) -> Unit)?> { null }

/**
 * Filter an element's actions to those matching the primary activation trigger
 * (onActivate or the deprecated alias onTap). Returns them in declared order.
 */
fun getActivateActions(actions: List<Action>?): List<SduiAction> {
    if (actions.isNullOrEmpty()) return emptyList()
    return actions.filter { action ->
        action.trigger == ActionTrigger.OnActivate || action.trigger == ActionTrigger.OnTap
    }.map { it.toSduiAction() }
}
