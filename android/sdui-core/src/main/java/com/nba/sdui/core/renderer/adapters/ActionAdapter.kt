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
        webUrl = webURL,
        eventName = event,
        eventParams = params?.filterValues { it != null }?.mapValues { it.value as Any },
        target = target,
        value = value,
        endpoint = endpoint,
        operation = operation,
        paramBindings = paramBindings,
        message = message,
        onFailure = onFailure?.value,
        failureFeedback = failureFeedback?.toViewModel(),
        presentation = presentation?.value,
        modalHeight = modalHeight?.value
    )
}

private fun GeneratedFailureFeedback.toViewModel(): FailureFeedback {
    return FailureFeedback(
        message = message,
        style = style?.value
    )
}
