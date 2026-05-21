package com.nba.sdui.core.state

import android.util.Log
import com.nba.sdui.core.models.generated.MutateOperation
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

    companion object {
        private const val TAG = "StateManager"
    }
    
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

    /**
     * Apply a schema-declared mutate operation to a screen state key.
     *
     * @return `true` when state was updated; `false` when the operation was a
     *         logged no-op (type mismatch or unknown op).
     */
    fun applyOperation(operation: MutateOperation?, key: String, value: Any?): Boolean {
        return when (operation ?: MutateOperation.Set) {
            MutateOperation.Set -> {
                if (value != null) {
                    setState(key, value)
                } else {
                    removeState(key)
                }
                true
            }

            MutateOperation.Toggle -> {
                val current = getState(key)
                if (current is Boolean) {
                    setState(key, !current)
                    true
                } else {
                    Log.w(TAG, "mutate toggle noop: current value is not boolean key=$key current=$current")
                    false
                }
            }

            MutateOperation.Increment -> {
                val current = getState(key)
                val currentNumber = asStoredNumber(current)
                if (currentNumber == null) {
                    Log.w(TAG, "mutate increment noop: current value is not numeric key=$key current=$current")
                    return false
                }

                val delta = asDouble(value) ?: 1.0
                val next = currentNumber + delta
                if (isWholeNumber(next) && next in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) {
                    setState(key, next.toInt())
                } else {
                    setState(key, next)
                }
                true
            }

            MutateOperation.Append -> {
                val current = getState(key)
                when {
                    current is List<*> -> {
                        setState(key, current + value)
                        true
                    }
                    current is String && value is String -> {
                        setState(key, current + value)
                        true
                    }
                    current == null && value != null -> {
                        setState(key, listOf(value))
                        true
                    }
                    else -> {
                        Log.w(
                            TAG,
                            "mutate append noop: incompatible value types key=$key current=$current incoming=$value"
                        )
                        false
                    }
                }
            }
        }
    }

    private fun asStoredNumber(value: Any?): Double? = when (value) {
        is Double -> if (value.isFinite()) value else null
        is Float -> if (value.isFinite()) value.toDouble() else null
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Short -> value.toDouble()
        is Byte -> value.toDouble()
        else -> null
    }

    private fun asDouble(value: Any?): Double? = when (value) {
        is Double -> if (value.isFinite()) value else null
        is Float -> if (value.isFinite()) value.toDouble() else null
        is Int -> value.toDouble()
        is Long -> value.toDouble()
        is Short -> value.toDouble()
        is Byte -> value.toDouble()
        is Boolean -> if (value) 1.0 else 0.0
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun isWholeNumber(value: Double): Boolean = value % 1.0 == 0.0
}
