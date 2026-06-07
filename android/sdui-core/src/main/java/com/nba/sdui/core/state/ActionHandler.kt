package com.nba.sdui.core.state

import android.util.Log
import com.nba.sdui.core.models.generated.MutateOperation

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
 * Actions are triggered by user interaction (onActivate, onVisible, etc.)
 * and dispatched to the ActionHandler.
 */
data class SduiAction(
    val trigger: String, // "onActivate", "onLongPress", "onVisible", "onSubmit", …
    val type: String, // "navigate", "fireAndForget", "mutate", "refresh", "dismiss", "toast"
    val targetUri: String? = null,
    val webUrl: String? = null,
    val eventName: String? = null,
    val eventParams: Map<String, Any>? = null,
    val target: String? = null,
    val value: Any? = null,
    val endpoint: String? = null,
    val operation: MutateOperation? = null,
    val paramBindings: Map<String, String>? = null,
    val message: String? = null,
    val onFailure: String? = null,
    val failureFeedback: FailureFeedback? = null,
    val presentation: String? = null,
    val modalHeight: String? = null
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
        // Resolve `{{stateKey}}` placeholders against the live state map
        // before the per-type handlers run. This lets the server emit
        // parameterised navigate/refresh actions (e.g. nba://games?date={{x}})
        // without each renderer having to know how to splice state into
        // server-emitted strings.
        val resolved = PlaceholderSubstitutor.resolve(action, stateManager.state.value)
        SduiActionLogger.debug(resolved, "dispatch")

        return when (resolved.type) {
            "navigate" -> handleNavigate(resolved)
            "fireAndForget" -> handleFireAndForget(resolved)
            "mutate" -> handleMutate(resolved, stateManager)
            "refresh" -> handleRefresh(resolved, stateManager)
            "dismiss" -> handleDismiss(resolved)
            "toast" -> handleToast(resolved)
            else -> {
                Log.w(TAG, "Unknown action type: ${resolved.type}")
                ActionResult.Unknown(resolved.type)
            }
        }
    }
    
    private fun handleNavigate(action: SduiAction): ActionResult {
        val uri = action.targetUri?.takeIf { it.startsWith("nba://") } ?: action.webUrl ?: action.targetUri
        
        if (uri == null) {
            Log.w(TAG, "Navigate action missing targetUri and webUrl")
            return ActionResult.NavigateError("", "No navigation target specified", action.failureFeedback)
        }
        
        when (action.presentation) {
            null, "push" -> Unit
            "external" -> SduiActionLogger.debug(action, "navigate external uri=$uri")
            "modal", "fullscreen" -> Log.w(TAG, "presentation=${action.presentation} modalHeight=${action.modalHeight} is preserved but native host is not implemented; host may fall back")
            "replace" -> SduiActionLogger.debug(action, "navigate replace uri=$uri")
            else -> Log.w(TAG, "Unsupported navigation presentation=${action.presentation}; preserving value for host fallback")
        }
        SduiActionLogger.debug(action, "navigate uri=$uri presentation=${action.presentation ?: "push"}")
        // In a real implementation, this would trigger navigation
        // For the prototype, we return the URI for the UI to show in a snackbar
        return ActionResult.NavigateResult(
            uri = uri,
            presentation = action.presentation,
            modalHeight = action.modalHeight
        )
    }
    
    private fun handleFireAndForget(action: SduiAction): ActionResult {
        val eventName = action.eventName ?: "unnamed_event"
        val params = action.eventParams ?: emptyMap()

        // Fire-and-forget has no on-screen side effect, so debug logging
        // is the only way to verify the beacon was actually emitted
        // during local testing. iOS mirrors this in `ActionDispatcher`.
        SduiActionLogger.debug(action, "fireAndForget event=$eventName params=$params")
        return ActionResult.FireAndForgetResult(eventName, params)
    }

    private fun handleMutate(action: SduiAction, stateManager: StateManager): ActionResult {
        val key = action.target
        val value = action.value

        if (key == null) {
            Log.w(TAG, "Mutate action missing target")
            return ActionResult.MutateNoOp("", "No target specified")
        }

        val operation = action.operation?.value ?: MutateOperation.Set.value
        SduiActionLogger.debug(action, "mutate op=$operation $key=$value")
        if (!stateManager.applyOperation(action.operation, key, value)) {
            return ActionResult.MutateNoOp(key, "Operation had no effect")
        }

        return ActionResult.MutateResult(key, stateManager.getState(key))
    }
    
    private fun handleRefresh(action: SduiAction, stateManager: StateManager): ActionResult {
        val target = action.target
        val endpoint = action.endpoint
        val paramBindings = action.paramBindings

        if (!paramBindings.isNullOrEmpty() && endpoint != null) {
            // Parameterized refresh: paramBindings values were already
            // resolved against the state map by PlaceholderSubstitutor in
            // handle(). Drop empty values so the transport doesn't emit
            // dangling `?key=` query params.
            val resolvedParams = paramBindings.filterValues { it.isNotEmpty() }

            SduiActionLogger.debug(action, "refresh parameterized endpoint=$endpoint params=$resolvedParams")
            return ActionResult.ParameterizedRefreshResult(endpoint, target, resolvedParams)
        }

        SduiActionLogger.debug(action, "refresh target=${target ?: "screen"}")
        return ActionResult.RefreshResult(target)
    }

    private fun handleDismiss(action: SduiAction): ActionResult {
        SduiActionLogger.debug(action, "dismiss")
        return ActionResult.DismissResult
    }

    private fun handleToast(action: SduiAction): ActionResult {
        val message = action.message ?: "No message"
        SduiActionLogger.debug(action, "toast message=$message")
        return ActionResult.ToastResult(message)
    }
    
    /**
     * Result of handling an SDUI action.
     */
    sealed class ActionResult {
        /** Whether this result represents a failure. */
        fun isFailure(): Boolean = this is NavigateError || this is MutateNoOp || this is RefreshStale || this is Error

        data class NavigateResult(
            val uri: String,
            val presentation: String? = null,
            val modalHeight: String? = null
        ) : ActionResult()
        data class NavigateError(val uri: String, val reason: String, val feedback: FailureFeedback?) : ActionResult()
        data class FireAndForgetResult(val eventName: String, val params: Map<String, Any>) : ActionResult()
        data class MutateResult(val key: String, val value: Any?) : ActionResult()
        data class MutateNoOp(val key: String, val reason: String) : ActionResult()
        data class RefreshResult(val target: String?) : ActionResult()
        data class RefreshStale(val target: String?, val reason: String) : ActionResult()
        /**
         * Parameterized refresh result. The viewmodel will hand
         * `(endpoint, params)` to `SduiRepository.fetchScreen`, which applies
         * the canonical envelope/POST-fallback transport. `target` (when
         * non-null) tells the merge step which section to surgically replace
         * if the response includes it.
         */
        data class ParameterizedRefreshResult(
            val endpoint: String,
            val target: String?,
            val params: Map<String, String>
        ) : ActionResult()
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
