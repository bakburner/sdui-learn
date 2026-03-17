package com.nba.sdui.core.state

import android.util.Log

/**
 * Sequence behavior when an action fails.
 * Clients apply per-type defaults when the server omits onFailure.
 */
enum class FailurePolicy {
    HALT, CONTINUE, SILENT;

    companion object {
        fun fromString(value: String?): FailurePolicy? = when (value) {
            "halt" -> HALT
            "continue" -> CONTINUE
            "silent" -> SILENT
            else -> null
        }
    }
}

/**
 * Server-provided error message and presentation hint for action failures.
 */
data class FailureFeedback(
    val message: String? = null,
    val style: String? = null // "snackbar", "toast", "inline"
)

/**
 * SDUI Action - Declarative action attached to components.
 * 
 * Actions are triggered by user interaction (onTap, onVisible, etc.)
 * and dispatched to the ActionHandler.
 */
data class SduiAction(
    val trigger: String, // "onTap", "onLongPress", "onVisible", "onSwipe"
    val type: String, // "navigate", "fireAndForget", "mutate", "refresh", "dismiss", "toast"
    val targetUri: String? = null,
    val fallbackUrl: String? = null,
    val eventName: String? = null,
    val eventParams: Map<String, Any>? = null,
    val stateKey: String? = null,
    val stateValue: Any? = null,
    val sectionId: String? = null,
    val endpoint: String? = null,
    val paramBindings: Map<String, String>? = null,
    val message: String? = null,
    val onFailure: String? = null,
    val failureFeedback: FailureFeedback? = null
)

/**
 * Action Handler - Dispatches and executes SDUI actions.
 * 
 * The handler receives Action objects from component interactions and executes them:
 * - navigate: Open a deeplink URI or log the navigation target
 * - fireAndForget: Forward event payload to registered backends (analytics, logging, etc.)
 * - mutate: Update the state manager with the specified key/value
 * - refresh: Trigger a re-fetch of a section or the entire screen
 * - dismiss: Close modals/overlays
 */
class ActionHandler {
    
    companion object {
        private const val TAG = "ActionHandler"

        /** Per-type default failure policy when onFailure is absent from the action. */
        private val DEFAULT_FAILURE_POLICY = mapOf(
            "navigate" to FailurePolicy.HALT,
            "fireAndForget" to FailurePolicy.SILENT,
            "mutate" to FailurePolicy.CONTINUE,
            "refresh" to FailurePolicy.CONTINUE,
            "dismiss" to FailurePolicy.SILENT,
            "toast" to FailurePolicy.SILENT
        )
    }

    /**
     * Resolve the failure policy for an action.
     * Uses action.onFailure if present, otherwise falls back to per-type default.
     */
    fun resolveFailurePolicy(action: SduiAction): FailurePolicy {
        return FailurePolicy.fromString(action.onFailure)
            ?: DEFAULT_FAILURE_POLICY[action.type]
            ?: FailurePolicy.CONTINUE
    }

    /**
     * Execute a sequence of actions in declared order.
     *
     * - On failure, consults resolveFailurePolicy() to determine halt/continue/silent.
     * - Navigate success always halts (navigation takes over the UI).
     * - Already-fired actions are committed — there is no rollback.
     *
     * @return list of results for actions that executed (may be shorter than input if halted)
     */
    fun executeSequence(actions: List<SduiAction>, stateManager: StateManager): SequenceResult {
        val results = mutableListOf<ActionResult>()

        for (action in actions) {
            val result = handle(action, stateManager)
            results.add(result)

            // Navigate success always halts — navigation takes over the screen
            if (result is ActionResult.NavigateResult) {
                return SequenceResult(results, halted = true)
            }

            // Check if this was a failure result
            if (result.isFailure()) {
                val policy = resolveFailurePolicy(action)
                when (policy) {
                    FailurePolicy.HALT -> {
                        Log.w(TAG, "Action ${action.type} failed with halt policy — stopping sequence")
                        return SequenceResult(results, halted = true, failedAction = action)
                    }
                    FailurePolicy.CONTINUE -> {
                        Log.w(TAG, "Action ${action.type} failed with continue policy — proceeding")
                    }
                    FailurePolicy.SILENT -> {
                        // Swallow silently
                    }
                }
            }
        }

        return SequenceResult(results, halted = false)
    }
    
    /**
     * Handle an SDUI action and return the result.
     */
    fun handle(action: SduiAction, stateManager: StateManager): ActionResult {
        Log.d(TAG, "Handling action: type=${action.type}, trigger=${action.trigger}")
        
        return when (action.type) {
            "navigate" -> handleNavigate(action)
            "fireAndForget" -> handleFireAndForget(action)
            "mutate" -> handleMutate(action, stateManager)
            "refresh" -> handleRefresh(action, stateManager)
            "dismiss" -> handleDismiss(action)
            "toast" -> handleToast(action)
            else -> {
                Log.w(TAG, "Unknown action type: ${action.type}")
                ActionResult.Unknown(action.type)
            }
        }
    }
    
    private fun handleNavigate(action: SduiAction): ActionResult {
        val uri = action.targetUri ?: action.fallbackUrl
        
        if (uri == null) {
            Log.w(TAG, "Navigate action missing targetUri and fallbackUrl")
            return ActionResult.NavigateError("", "No navigation target specified", action.failureFeedback)
        }
        
        Log.i(TAG, "Navigate to: $uri")
        // In a real implementation, this would trigger navigation
        // For the prototype, we return the URI for the UI to show in a snackbar
        return ActionResult.NavigateResult(uri, action.fallbackUrl)
    }
    
    private fun handleFireAndForget(action: SduiAction): ActionResult {
        val eventName = action.eventName ?: "unnamed_event"
        val params = action.eventParams ?: emptyMap()
        
        Log.i(TAG, "FireAndForget event: $eventName, params=$params")
        // In a real implementation, this would forward to registered backends (analytics, logging, etc.)
        return ActionResult.FireAndForgetResult(eventName, params)
    }
    
    private fun handleMutate(action: SduiAction, stateManager: StateManager): ActionResult {
        val key = action.stateKey
        val value = action.stateValue
        
        if (key == null) {
            Log.w(TAG, "Mutate action missing stateKey")
            return ActionResult.MutateNoOp("", "No state key specified")
        }
        
        if (value != null) {
            Log.i(TAG, "Mutate state: $key = $value")
            stateManager.setState(key, value)
        } else {
            Log.i(TAG, "Remove state: $key")
            stateManager.removeState(key)
        }
        
        return ActionResult.MutateResult(key, value)
    }
    
    private fun handleRefresh(action: SduiAction, stateManager: StateManager): ActionResult {
        val sectionId = action.sectionId
        val endpoint = action.endpoint
        val paramBindings = action.paramBindings

        if (!paramBindings.isNullOrEmpty() && endpoint != null) {
            // Parameterized refresh: resolve bindings from screen state → query params
            val resolvedParams = paramBindings.mapValues { (_, template) ->
                // Strip mustache delimiters: "{{form_season}}" → "form_season"
                val stateKey = template.removePrefix("{{").removeSuffix("}}")
                stateManager.getState(stateKey)?.toString() ?: ""
            }
            val queryString = resolvedParams.entries
                .filter { it.value.isNotEmpty() }
                .joinToString("&") { "${it.key}=${it.value}" }
            val separator = if ("?" in endpoint) "&" else "?"
            val url = "$endpoint$separator$queryString"

            Log.i(TAG, "Parameterized refresh: url=$url")
            // In a real implementation, this would fetch the URL and replace screen data.
            // For the prototype, we return the resolved URL for the UI layer to handle.
            return ActionResult.ParameterizedRefreshResult(url, resolvedParams)
        }

        Log.i(TAG, "Refresh: sectionId=$sectionId")
        return ActionResult.RefreshResult(sectionId)
    }
    
    private fun handleDismiss(action: SduiAction): ActionResult {
        Log.i(TAG, "Dismiss action triggered")
        return ActionResult.DismissResult
    }

    private fun handleToast(action: SduiAction): ActionResult {
        val message = action.message ?: "No message"
        Log.i(TAG, "Toast: $message")
        return ActionResult.ToastResult(message)
    }
    
    /**
     * Result of handling an SDUI action.
     */
    sealed class ActionResult {
        /** Whether this result represents a failure. */
        fun isFailure(): Boolean = this is NavigateError || this is MutateNoOp || this is RefreshStale || this is Error

        data class NavigateResult(val uri: String, val fallbackUrl: String?) : ActionResult()
        data class NavigateError(val uri: String, val reason: String, val feedback: FailureFeedback?) : ActionResult()
        data class FireAndForgetResult(val eventName: String, val params: Map<String, Any>) : ActionResult()
        data class MutateResult(val key: String, val value: Any?) : ActionResult()
        data class MutateNoOp(val key: String, val reason: String) : ActionResult()
        data class RefreshResult(val sectionId: String?) : ActionResult()
        data class RefreshStale(val sectionId: String?, val reason: String) : ActionResult()
        data class ParameterizedRefreshResult(val url: String, val params: Map<String, String>) : ActionResult()
        data object DismissResult : ActionResult()
        data class ToastResult(val message: String) : ActionResult()
        data class Error(val message: String) : ActionResult()
        data class Unknown(val actionType: String) : ActionResult()
    }

    /**
     * Result of executing a sequence of actions.
     */
    data class SequenceResult(
        val results: List<ActionResult>,
        val halted: Boolean,
        val failedAction: SduiAction? = null
    )
}
