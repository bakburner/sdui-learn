package com.nba.sdui.core.state

import android.util.Log

/**
 * SDUI Action - Declarative action attached to components.
 * 
 * Actions are triggered by user interaction (onTap, onVisible, etc.)
 * and dispatched to the ActionHandler.
 */
data class SduiAction(
    val trigger: String, // "onTap", "onLongPress", "onVisible", "onSwipe"
    val type: String, // "navigate", "analytics", "mutate", "refresh", "dismiss"
    val targetUri: String? = null,
    val fallbackUrl: String? = null,
    val eventName: String? = null,
    val eventParams: Map<String, Any>? = null,
    val stateKey: String? = null,
    val stateValue: Any? = null,
    val sectionId: String? = null
)

/**
 * Action Handler - Dispatches and executes SDUI actions.
 * 
 * The handler receives Action objects from component interactions and executes them:
 * - navigate: Open a deeplink URI or log the navigation target
 * - analytics: Log the event name and parameters
 * - mutate: Update the state manager with the specified key/value
 * - refresh: Trigger a re-fetch of a section or the entire screen
 * - dismiss: Close modals/overlays
 */
class ActionHandler {
    
    companion object {
        private const val TAG = "ActionHandler"
    }
    
    /**
     * Handle an SDUI action and return the result.
     */
    fun handle(action: SduiAction, stateManager: StateManager): ActionResult {
        Log.d(TAG, "Handling action: type=${action.type}, trigger=${action.trigger}")
        
        return when (action.type) {
            "navigate" -> handleNavigate(action)
            "analytics" -> handleAnalytics(action)
            "mutate" -> handleMutate(action, stateManager)
            "refresh" -> handleRefresh(action)
            "dismiss" -> handleDismiss(action)
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
            return ActionResult.Error("No navigation target specified")
        }
        
        Log.i(TAG, "Navigate to: $uri")
        // In a real implementation, this would trigger navigation
        // For the prototype, we return the URI for the UI to show in a snackbar
        return ActionResult.NavigateResult(uri, action.fallbackUrl)
    }
    
    private fun handleAnalytics(action: SduiAction): ActionResult {
        val eventName = action.eventName ?: "unnamed_event"
        val params = action.eventParams ?: emptyMap()
        
        Log.i(TAG, "Analytics event: $eventName, params=$params")
        // In a real implementation, this would fire to analytics backends
        return ActionResult.AnalyticsResult(eventName, params)
    }
    
    private fun handleMutate(action: SduiAction, stateManager: StateManager): ActionResult {
        val key = action.stateKey
        val value = action.stateValue
        
        if (key == null) {
            Log.w(TAG, "Mutate action missing stateKey")
            return ActionResult.Error("No state key specified")
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
    
    private fun handleRefresh(action: SduiAction): ActionResult {
        val sectionId = action.sectionId
        
        Log.i(TAG, "Refresh: sectionId=$sectionId")
        // In a real implementation, this would trigger a re-fetch
        return ActionResult.RefreshResult(sectionId)
    }
    
    private fun handleDismiss(action: SduiAction): ActionResult {
        Log.i(TAG, "Dismiss action triggered")
        return ActionResult.DismissResult
    }
    
    /**
     * Result of handling an SDUI action.
     */
    sealed class ActionResult {
        data class NavigateResult(val uri: String, val fallbackUrl: String?) : ActionResult()
        data class AnalyticsResult(val eventName: String, val params: Map<String, Any>) : ActionResult()
        data class MutateResult(val key: String, val value: Any?) : ActionResult()
        data class RefreshResult(val sectionId: String?) : ActionResult()
        data object DismissResult : ActionResult()
        data class Error(val message: String) : ActionResult()
        data class Unknown(val actionType: String) : ActionResult()
    }
}
