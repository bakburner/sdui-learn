package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction

private const val DEFAULT_FALLBACK =
    "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"

/**
 * AtomicImage — renders an Image element via Coil AsyncImage with
 * optional sizing, aspect ratio, content scale, corner radius,
 * and placeholder fallback on load error.
 */
@Composable
fun AtomicImage(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageModifier = modifier

    if (element.fillWidth == true) {
        imageModifier = imageModifier.fillMaxWidth()
    }
    element.cornerRadius?.let {
        imageModifier = imageModifier.clip(RoundedCornerShape(it.dp))
    }
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

    val fallbackUrl = element.placeholder ?: DEFAULT_FALLBACK
    var currentSrc by remember(element.src) { mutableStateOf(element.src) }
    var triedFallback by remember(element.src) { mutableStateOf(false) }

    AsyncImage(
        model = currentSrc,
        contentDescription = element.accessibility?.label ?: element.alt ?: "",
        contentScale = mapContentScale(element.fit),
        modifier = imageModifier.applyAccessibility(element.accessibility),
        onError = {
            if (!triedFallback) {
                triedFallback = true
                currentSrc = fallbackUrl
            }
        }
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
