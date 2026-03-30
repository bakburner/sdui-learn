package com.nba.sdui.core.data

import android.util.Log
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nba.sdui.core.models.DataBinding
import com.nba.sdui.core.models.DataBindingPath

/**
 * Data Binding Resolver - Applies real-time data updates to section data.
 * 
 * Takes an incoming real-time message (from Ably), a section's data binding 
 * configuration, and the current section data, then produces updated section 
 * data with the mapped fields replaced.
 * 
 * Handles:
 * - JSONPath-like source path resolution (e.g., "$.homeTeam.score")
 * - Dot-path target path resolution (e.g., "homeTeam.score")
 * - Graceful handling of missing paths (preserves previous value)
 * - Null value handling (logs warning, keeps previous value)
 */
class DataBindingResolver {
    
    companion object {
        private const val TAG = "DataBindingResolver"
    }
    
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    /**
     * Apply data bindings to update section data with incoming message values.
     * 
     * @param currentData The current section data
     * @param incomingMessage The incoming real-time message
     * @param dataBinding The data binding configuration
     * @param traceId Optional trace ID for logging
     * @param stringTable Optional screen-level string table for stringKey resolution
     * @return Updated section data with bound values applied
     */
    fun applyBindings(
        currentData: Map<String, Any?>,
        incomingMessage: Map<String, Any?>,
        dataBinding: DataBinding,
        traceId: String? = null,
        stringTable: Map<String, String>? = null
    ): Map<String, Any?> {
        
        Log.d(TAG, "Applying ${dataBinding.bindings.size} bindings, traceId=$traceId")
        
        // Convert to JsonNode for easier path traversal
        val messageNode = objectMapper.valueToTree<JsonNode>(incomingMessage)
        val dataNode = objectMapper.valueToTree<ObjectNode>(currentData)
        
        for (binding in dataBinding.bindings) {
            try {
                applyBinding(dataNode, messageNode, binding, traceId)

                // Resolve stringKey: if the binding target has a stringKey, replace the
                // bound value with the corresponding localized string from the string table.
                val stringKey = dataBinding.stringKeys?.get(binding.targetPath)
                if (stringKey != null && stringTable != null) {
                    val resolved = stringTable[stringKey]
                    if (resolved != null) {
                        setTargetPath(dataNode, binding.targetPath,
                            objectMapper.valueToTree(resolved))
                        Log.d(TAG, "stringKey resolved: ${binding.targetPath} -> $stringKey = $resolved")
                    } else {
                        Log.w(TAG, "stringKey lookup failed for key '$stringKey' on ${binding.targetPath}, keeping raw value, traceId=$traceId")
                    }
                } else if (stringKey != null) {
                    Log.w(TAG, "stringKey '$stringKey' present but no stringTable provided, keeping raw value, traceId=$traceId")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply binding: ${binding.sourcePath} -> ${binding.targetPath}, traceId=$traceId", e)
            }
        }
        
        // Convert back to Map
        @Suppress("UNCHECKED_CAST")
        return objectMapper.convertValue(dataNode, Map::class.java) as Map<String, Any?>
    }
    
    /**
     * Apply a single binding.
     */
    private fun applyBinding(
        targetData: ObjectNode,
        sourceMessage: JsonNode,
        binding: DataBindingPath,
        traceId: String?
    ) {
        // Resolve source value from message
        val sourceValue = resolveSourcePath(sourceMessage, binding.sourcePath)
        
        if (sourceValue == null || sourceValue.isNull) {
            Log.w(TAG, "Source value is null for path: ${binding.sourcePath}, keeping previous value, traceId=$traceId")
            return
        }
        
        // Set target value in data
        setTargetPath(targetData, binding.targetPath, sourceValue)
        
        Log.d(TAG, "Applied binding: ${binding.sourcePath} -> ${binding.targetPath} = $sourceValue, traceId=$traceId")
    }
    
    /**
     * Resolve a JSONPath-like source path.
     * Supports: $.field, $.nested.field, $.array[0].field
     */
    private fun resolveSourcePath(node: JsonNode, path: String): JsonNode? {
        // Remove leading $. if present
        val cleanPath = path.removePrefix("$.")
        
        return navigatePath(node, cleanPath)
    }
    
    /**
     * Navigate a dot-separated path through a JsonNode.
     */
    private fun navigatePath(node: JsonNode, path: String): JsonNode? {
        if (path.isEmpty()) return node
        
        val parts = path.split(".")
        var current: JsonNode? = node
        
        for (part in parts) {
            if (current == null || current.isNull) return null
            
            // Check for array index: field[0]
            val arrayMatch = Regex("""(\w+)\[(\d+)]""").matchEntire(part)
            if (arrayMatch != null) {
                val fieldName = arrayMatch.groupValues[1]
                val index = arrayMatch.groupValues[2].toInt()
                current = current.path(fieldName)
                if (!current.isArray || current.size() <= index) return null
                current = current[index]
            } else {
                current = current.path(part)
            }
            
            if (current.isMissingNode) return null
        }
        
        return current
    }
    
    /**
     * Set a value at a dot-separated target path.
     */
    private fun setTargetPath(node: ObjectNode, path: String, value: JsonNode) {
        val parts = path.split(".")
        
        if (parts.size == 1) {
            // Direct field
            node.set<JsonNode>(parts[0], value)
            return
        }
        
        // Navigate to parent and set final field
        var current: ObjectNode = node
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            val child = current.path(part)
            
            if (child.isMissingNode || !child.isObject) {
                // Create intermediate object if needed
                val newObject = objectMapper.createObjectNode()
                current.set<JsonNode>(part, newObject)
                current = newObject
            } else {
                current = child as ObjectNode
            }
        }
        
        // Set the final value
        current.set<JsonNode>(parts.last(), value)
    }
}
