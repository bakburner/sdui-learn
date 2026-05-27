package com.nba.sdui.core.state

/**
 * Replaces `{{stateKey}}` placeholders inside server-emitted strings with the
 * current screen-state value for that key.
 *
 * The server emits actions like
 * `targetUri = "nba://games?date={{calendar_selected_date}}"` so that a single
 * composed payload can carry parameterised navigation/refresh intent without
 * the server having to know the client's current state. The substitution must
 * happen on the client at action-dispatch time, after any preceding `mutate`
 * (or renderer-driven `onStateChange`) has updated the state map.
 *
 * - Unknown keys are left as-is (`{{foo}}` → `{{foo}}`), which preserves the
 *   error signal at the network layer rather than silently producing a
 *   half-substituted URL.
 * - Keys follow `[A-Za-z_][A-Za-z0-9_.]*` so dotted state paths like
 *   `filters.team` are recognised; arbitrary characters are not, which keeps
 *   the regex narrow enough to avoid matching unrelated `{{ … }}` patterns
 *   that may appear in human-readable text.
 */
internal object PlaceholderSubstitutor {

    private val PATTERN = Regex("""\{\{([A-Za-z_][A-Za-z0-9_.]*)\}\}""")

    /**
     * Replace every `{{key}}` in [template] using values from [state].
     * Unknown keys are left intact so the failure surfaces at the network
     * layer instead of producing a half-substituted URL.
     */
    fun substitute(template: String, state: Map<String, Any>): String {
        if (template.isEmpty() || !template.contains("{{")) return template
        return PATTERN.replace(template) { match ->
            val key = match.groupValues[1]
            state[key]?.toString() ?: match.value
        }
    }

    /** Null-safe variant for optional string fields on `SduiAction`. */
    fun substituteOrNull(template: String?, state: Map<String, Any>): String? =
        template?.let { substitute(it, state) }

    /**
     * `paramBindings` variant: unknown state keys resolve to the empty
     * string. Callers filter empty values to drop the query param entirely
     * — that's the long-standing wire contract for optional filter params.
     * Keeping the URI-level "leave intact" behavior would silently emit
     * `?gameId={{missing}}`, which is worse than omitting it.
     */
    private fun substituteForBinding(template: String, state: Map<String, Any>): String {
        if (template.isEmpty()) return template
        if (!template.contains("{{")) return template
        return PATTERN.replace(template) { match ->
            val key = match.groupValues[1]
            state[key]?.toString() ?: ""
        }
    }

    /**
     * Substitute every placeholder field on an [SduiAction] against the
     * current state map. Returns the same instance when nothing changed so
     * callers can fast-path actions that don't carry placeholders.
     */
    fun resolve(action: SduiAction, state: Map<String, Any>): SduiAction {
        val newTargetUri = substituteOrNull(action.targetUri, state)
        val newWebUrl = substituteOrNull(action.webUrl, state)
        val newEndpoint = substituteOrNull(action.endpoint, state)
        val newParamBindings = action.paramBindings?.mapValues { (_, v) ->
            substituteForBinding(v, state)
        }

        val unchanged = newTargetUri == action.targetUri &&
            newWebUrl == action.webUrl &&
            newEndpoint == action.endpoint &&
            newParamBindings == action.paramBindings
        if (unchanged) return action

        return action.copy(
            targetUri = newTargetUri,
            webUrl = newWebUrl,
            endpoint = newEndpoint,
            paramBindings = newParamBindings
        )
    }
}
