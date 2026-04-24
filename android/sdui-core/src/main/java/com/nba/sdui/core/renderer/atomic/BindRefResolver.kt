package com.nba.sdui.core.renderer.atomic

import androidx.compose.runtime.compositionLocalOf

/**
 * Content blob flowing through an `AtomicComposite`'s subtree. Set by
 * `SectionRouter` when dispatching `AtomicComposite`, read by leaf
 * primitives that carry a `bindRef`. Non-composite contexts leave
 * this null.
 */
val LocalCompositeContent = compositionLocalOf<Map<String, Any?>?> { null }

/**
 * Resolver for the `bindRef` property on atomic leaf elements.
 *
 * `bindRef` is a dot-path into the enclosing `AtomicComposite`'s
 * `data.content` object. Each primitive has a canonical live field
 * the resolver targets — `content` for `Text`, `src` for `Image`,
 * `label` for `Button`, and an object-shaped
 * `{snapshotSeconds, snapshotAt, isRunning}` for `LiveClock`.
 * Placing the reference on the consuming node (rather than declaring
 * a central path-into-tree binding) lets the composer reshape the ui
 * tree without breaking real-time updates.
 */
object BindRefResolver {

    fun resolveString(bindRef: String?, content: Map<String, Any?>?): String? {
        val value = resolveValue(bindRef, content) ?: return null
        return when (value) {
            is String -> value
            is Number -> {
                val d = value.toDouble()
                if (d == d.toLong().toDouble() && kotlin.math.abs(d) < 1e16) {
                    d.toLong().toString()
                } else {
                    d.toString()
                }
            }
            is Boolean -> value.toString()
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun resolveDictionary(bindRef: String?, content: Map<String, Any?>?): Map<String, Any?>? {
        val value = resolveValue(bindRef, content) ?: return null
        return value as? Map<String, Any?>
    }

    fun resolveValue(bindRef: String?, content: Map<String, Any?>?): Any? {
        if (bindRef.isNullOrEmpty() || content == null) return null
        val parts = bindRef.split('.')
        if (parts.isEmpty()) return null

        var current: Any? = content[parts.first()] ?: return null
        for (part in parts.drop(1)) {
            val map = current as? Map<*, *> ?: return null
            current = map[part] ?: return null
        }
        return current
    }
}
