package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.models.GeneratedConverters.actionToSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicImage — renders an Image element via Coil AsyncImage with
 * optional sizing, aspect ratio, content scale, and tap actions.
 */
@Composable
fun AtomicImage(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageModifier = modifier
    element.width?.let { imageModifier = imageModifier.width(it.dp) }
    element.height?.let { imageModifier = imageModifier.height(it.dp) }
    element.aspectRatio?.let { imageModifier = imageModifier.aspectRatio(it) }

    val hasActions = !element.actions.isNullOrEmpty()
    if (hasActions) {
        imageModifier = imageModifier.clickable {
            element.actions?.firstOrNull()?.let { actionMap ->
                @Suppress("UNCHECKED_CAST")
                actionToSduiAction(actionMap as? Map<String, Any?>)?.let(onAction)
            }
        }
    }

    AsyncImage(
        model = element.src,
        contentDescription = element.id,
        contentScale = mapContentScale(element.fit),
        modifier = imageModifier
    )
}

private fun mapContentScale(fit: String?): ContentScale = when (fit) {
    "cover"    -> ContentScale.Crop
    "contain"  -> ContentScale.Fit
    "fill"     -> ContentScale.FillBounds
    "fitWidth" -> ContentScale.FillWidth
    "fitHeight" -> ContentScale.FillHeight
    "none"     -> ContentScale.None
    else       -> ContentScale.Fit
}
