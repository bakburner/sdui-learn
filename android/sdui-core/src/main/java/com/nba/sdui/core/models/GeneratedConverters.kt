package com.nba.sdui.core.models

import com.nba.sdui.core.state.SduiAction

/**
 * Converts a raw Map (from deserialized section data) to SduiAction.
 *
 * Jackson deserializes actions as Map<String, Any?> since section.actions
 * is typed as List<Map<String, Any?>> in [SduiSection]. This converter
 * bridges to the typed [SduiAction] used by ActionHandler.
 */
@Suppress("UNCHECKED_CAST")
fun actionToSduiAction(map: Map<String, Any?>?): SduiAction? {
    if (map == null) return null
    return SduiAction(
        trigger = (map["trigger"] as? String)?.lowercase() ?: "onTap",
        type = (map["type"] as? String)?.lowercase() ?: "navigate",
        targetUri = map["targetUri"] as? String,
        fallbackUrl = map["fallbackUrl"] as? String,
        eventName = map["eventName"] as? String,
        eventParams = (map["eventParams"] as? Map<String, Any>)?.takeIf { it.isNotEmpty() },
        stateKey = map["stateKey"] as? String,
        stateValue = map["stateValue"],
        sectionId = map["sectionId"] as? String,
        endpoint = map["endpoint"] as? String,
        paramBindings = (map["paramBindings"] as? Map<String, Any?>)
            ?.mapValues { (_, v) -> v?.toString() ?: "" },
        message = map["message"] as? String
    )
}
