package com.nba.sdui.core.renderer.atomic

import androidx.compose.runtime.Composable
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicConditional — evaluates [element.condition] against [screenState]
 * and renders either [element.trueChild] or [element.falseChild].
 *
 * The conditional has no visual representation of its own; box-model
 * chrome belongs to the chosen child (which runs through [AtomicBox]
 * via [AtomicRouter]).
 *
 * Condition strings are simple dot-path keys looked up in screenState.
 * The value is truthy when non-null, non-false, and non-empty-string.
 */
@Composable
fun AtomicConditional(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    val conditionMet = evaluateCondition(element.condition, screenState)
    val child = if (conditionMet) element.trueChild else element.falseChild
    child?.let {
        AtomicRouter(
            it,
            screenState,
            onAction,
            depth = depth + 1,
            onStateChange = onStateChange,
            sectionSlotDepth = sectionSlotDepth
        )
    }
}

/**
 * Evaluate a dot-path condition key against the state map.
 *
 * Supports simple dot-path traversal: "user.isLoggedIn" looks up
 * screenState["user"] (as Map) then ["isLoggedIn"].
 */
internal fun evaluateCondition(condition: String?, state: Map<String, Any>): Boolean {
    if (condition.isNullOrBlank()) return false
    val parts = condition.split(".")
    var current: Any? = state
    for (part in parts) {
        current = when (current) {
            is Map<*, *> -> current[part]
            else -> return false
        }
    }
    return when (current) {
        null -> false
        is Boolean -> current
        is String -> current.isNotEmpty()
        is Number -> current.toDouble() != 0.0
        else -> true
    }
}
