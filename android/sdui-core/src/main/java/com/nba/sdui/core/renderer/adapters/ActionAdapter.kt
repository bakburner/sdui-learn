package com.nba.sdui.core.renderer.adapters

import com.nba.sdui.core.models.generated.Action
import com.nba.sdui.core.models.generated.FailureFeedback as GeneratedFailureFeedback
import com.nba.sdui.core.state.FailureFeedback
import com.nba.sdui.core.state.SduiAction

fun Action.toSduiAction(): SduiAction {
    return SduiAction(
        trigger = trigger.value,
        type = type.value,
        targetUri = targetURI,
        fallbackUrl = webURL,
        eventName = event,
        eventParams = params?.filterValues { it != null }?.mapValues { it.value as Any },
        stateKey = target,
        stateValue = value,
        sectionId = target,
        endpoint = endpoint,
        paramBindings = paramBindings,
        message = message,
        onFailure = onFailure?.value,
        failureFeedback = failureFeedback?.toViewModel()
    )
}

private fun GeneratedFailureFeedback.toViewModel(): FailureFeedback {
    return FailureFeedback(
        message = message,
        style = style?.value
    )
}
