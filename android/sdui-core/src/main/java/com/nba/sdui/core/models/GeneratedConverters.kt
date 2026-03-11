package com.nba.sdui.core.models

import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.models.generated.Action

/**
 * Converts generated Java Action to Kotlin SduiAction for use by ActionHandler.
 */
fun actionToSduiAction(action: Action?): SduiAction? {
    if (action == null) return null
    val eventParams = action.params?.additionalProperties?.mapValues { (_, v) -> v as Any }
        ?: emptyMap<String, Any>()
    return SduiAction(
        trigger = action.trigger?.name?.lowercase() ?: "onTap",
        type = action.type?.name?.lowercase() ?: "navigate",
        targetUri = action.targetUri,
        fallbackUrl = action.webUrl,
        eventName = action.event,
        eventParams = if (eventParams.isEmpty()) null else eventParams,
        stateKey = action.target,
        stateValue = action.value,
        sectionId = action.target,
        endpoint = action.endpoint,
        paramBindings = action.paramBindings?.additionalProperties
            ?.mapValues { (_, v) -> v?.toString() ?: "" },
        message = action.message
    )
}

/**
 * Converts a raw Map (from deserialized section data) to SduiAction.
 *
 * This overload handles actions embedded in section data maps where
 * Jackson has already deserialized them as Map<String, Any?> rather
 * than typed [Action] objects.
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
