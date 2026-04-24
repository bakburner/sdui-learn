package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.ImageFit
import com.nba.sdui.core.renderer.ImageVariantResolver
import com.nba.sdui.core.renderer.ImageVariantResolver.ImageContentScaleHint
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.toSduiAction
import com.nba.sdui.core.state.SduiAction

private const val DEFAULT_FALLBACK =
    "https://cdn.nba.com/manage/2025/04/nba-247-logoman-yt-thumbnail__1_.png"

/**
 * AtomicImage — renders an Image element via Coil AsyncImage. The box
 * model (margin / padding / bg / cornerRadius / shadow / border /
 * opacity / width / height / fillWidth / badge) comes from [AtomicBox];
 * this renderer only owns the content-scale and aspect-ratio concerns
 * that are semantically tied to the image itself.
 */
@Composable
fun AtomicImage(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val variantSpec = ImageVariantResolver.resolve(element.variant)
    val effectiveAspectRatio: Float? = element.aspectRatio?.toFloat() ?: variantSpec?.aspectRatio

    // Resolve `src` from `bindRef` when present, falling back to the
    // inline `src`. Lets composers rebind image URLs in flight without
    // touching the ui tree.
    val compositeContent = LocalCompositeContent.current
    val boundSrc = BindRefResolver.resolveString(element.bindRef, compositeContent) ?: element.src

    val fallbackUrl = element.placeholder ?: DEFAULT_FALLBACK
    var currentSrc by remember(boundSrc) { mutableStateOf(boundSrc) }
    var triedFallback by remember(boundSrc) { mutableStateOf(false) }

    val contentScale: ContentScale = element.fit?.let { mapContentScale(it) }
        ?: variantSpec?.contentScaleHint?.let(::mapContentScaleHint)
        ?: ContentScale.Fit

    val hasActions = !element.actions.isNullOrEmpty()

    AtomicBox(element, screenState, onAction) { boxModifier ->
        var imageModifier = boxModifier
        effectiveAspectRatio?.let { imageModifier = imageModifier.aspectRatio(it) }
        if (hasActions) {
            imageModifier = imageModifier.clickable {
                element.actions?.firstOrNull()?.toSduiAction()?.let(onAction)
            }
        }
        AsyncImage(
            model = currentSrc,
            contentDescription = element.accessibility?.label ?: element.alt ?: "",
            contentScale = contentScale,
            modifier = imageModifier.applyAccessibility(element.accessibility),
            onError = {
                if (!triedFallback) {
                    triedFallback = true
                    currentSrc = fallbackUrl
                }
            }
        )
    }
}

private fun mapContentScale(fit: ImageFit): ContentScale = when (fit) {
    ImageFit.Cover -> ContentScale.Crop
    ImageFit.Contain -> ContentScale.Fit
    ImageFit.Fill -> ContentScale.FillBounds
    ImageFit.None -> ContentScale.None
}

private fun mapContentScaleHint(hint: ImageContentScaleHint): ContentScale = when (hint) {
    ImageContentScaleHint.Crop       -> ContentScale.Crop
    ImageContentScaleHint.Fit        -> ContentScale.Fit
    ImageContentScaleHint.FillBounds -> ContentScale.FillBounds
    ImageContentScaleHint.None       -> ContentScale.None
}
