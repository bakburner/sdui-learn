package com.nba.sdui.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * State Manager - Screen-level state holder for SDUI.
 * 
 * Holds the current state variables as a key-value map.
 * Mutate actions update this map. Conditional components and
 * TabGroup read from this map to determine visibility and active tab.
 * 
 * Compose's reactivity handles re-rendering automatically when state changes.
 */
class StateManager {
    
    private val _state = MutableStateFlow<Map<String, Any>>(emptyMap())
    val state: StateFlow<Map<String, Any>> = _state.asStateFlow()
    
    /**
     * Initialize state from a map (e.g., from SDUI screen response).
     */
    fun initializeState(initialState: Map<String, Any>?) {
        initialState?.let { newState ->
            _state.update { current ->
                current + newState
            }
        }
    }
    
    /**
     * Set a state value.
     */
    fun setState(key: String, value: Any) {
        _state.update { current ->
            current + (key to value)
        }
    }
    
    /**
     * Get a state value.
     */
    fun getState(key: String): Any? {
        return _state.value[key]
    }
    
    /**
     * Get a typed state value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getStateAs(key: String, clazz: Class<T>): T? {
        return _state.value[key] as? T
    }
    
    /**
     * Clear all state.
     */
    fun clearState() {
        _state.value = emptyMap()
    }
    
    /**
     * Remove a specific state key.
     */
    fun removeState(key: String) {
        _state.update { current ->
            current - key
        }
    }
}
