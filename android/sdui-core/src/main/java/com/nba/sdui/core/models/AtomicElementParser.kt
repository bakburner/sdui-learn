package com.nba.sdui.core.models

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

/**
 * Parses the `data.ui` field of an AtomicComposite section into an [AtomicElement] tree.
 */
object AtomicElementParser {

    private val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    /**
     * Given the raw `data` map from an `AtomicComposite` section, extract and
     * deserialize `data.ui` into an [AtomicElement] tree.
     *
     * Returns null if the data is missing or malformed.
     */
    fun parse(data: Map<String, Any?>?): AtomicElement? {
        val rootObj = data?.get("ui") ?: return null
        return try {
            mapper.convertValue(rootObj, AtomicElement::class.java)
        } catch (e: Exception) {
            android.util.Log.w("AtomicParser", "Failed to parse AtomicElement tree", e)
            null
        }
    }
}
