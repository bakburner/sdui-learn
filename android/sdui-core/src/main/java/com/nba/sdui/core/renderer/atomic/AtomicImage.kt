package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.BadgeAlignment
import com.nba.sdui.core.models.generated.ImageFit
import com.nba.sdui.core.renderer.ImageVariantResolver
import com.nba.sdui.core.renderer.ImageVariantResolver.ImageContentScaleHint
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.toSduiAction
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
    val variantSpec = ImageVariantResolver.resolve(element.variant)

    // Inline wins when provided; variant supplies the default when the inline
    // value is null. The current image variants have all-ALLOW override
    // matrices, so the LOCK branch never triggers — it's kept for parity with
    // the container resolver contract.
    val effectiveCornerRadius: Int? = element.cornerRadius?.toInt() ?: variantSpec?.cornerRadiusDp
    val effectiveAspectRatio: Float? = element.aspectRatio?.toFloat() ?: variantSpec?.aspectRatio
    val shouldFillWidth: Boolean = element.fillWidth == true ||
        (element.fillWidth == null && variantSpec?.fillWidth == true)
    val shouldClip: Boolean = variantSpec?.clip ?: true

    var imageModifier = modifier

    if (shouldFillWidth) {
        imageModifier = imageModifier.fillMaxWidth()
    }
    if (shouldClip) {
        val radii = element.cornerRadii
        val hasAnyRadii = radii != null && (
            (radii.topStart ?: 0L) != 0L || (radii.topEnd ?: 0L) != 0L ||
            (radii.bottomStart ?: 0L) != 0L || (radii.bottomEnd ?: 0L) != 0L
        )
        if (hasAnyRadii) {
            val fallback = effectiveCornerRadius ?: 0
            imageModifier = imageModifier.clip(RoundedCornerShape(
                topStart = (radii!!.topStart?.toInt() ?: fallback).dp,
                topEnd = (radii.topEnd?.toInt() ?: fallback).dp,
                bottomEnd = (radii.bottomEnd?.toInt() ?: fallback).dp,
                bottomStart = (radii.bottomStart?.toInt() ?: fallback).dp
            ))
        } else {
            effectiveCornerRadius?.let {
                imageModifier = imageModifier.clip(RoundedCornerShape(it.dp))
            }
        }
    }
    element.width?.let { imageModifier = imageModifier.width(it.toInt().dp) }
    element.height?.let { imageModifier = imageModifier.height(it.toInt().dp) }
    effectiveAspectRatio?.let { imageModifier = imageModifier.aspectRatio(it) }

    val hasActions = !element.actions.isNullOrEmpty()
    if (hasActions) {
        imageModifier = imageModifier.clickable {
            element.actions?.firstOrNull()?.toSduiAction()?.let(onAction)
        }
    }

    val fallbackUrl = element.placeholder ?: DEFAULT_FALLBACK
    var currentSrc by remember(element.src) { mutableStateOf(element.src) }
    var triedFallback by remember(element.src) { mutableStateOf(false) }

    val contentScale: ContentScale = element.fit?.let { mapContentScale(it) }
        ?: variantSpec?.contentScaleHint?.let(::mapContentScaleHint)
        ?: ContentScale.Fit

    val imageComposable: @Composable () -> Unit = {
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

    if (element.badge != null) {
        val badgeAlignment = when (element.badge.alignment) {
            BadgeAlignment.TopStart -> Alignment.TopStart
            BadgeAlignment.TopEnd -> Alignment.TopEnd
            BadgeAlignment.BottomStart -> Alignment.BottomStart
            BadgeAlignment.BottomEnd -> Alignment.BottomEnd
            BadgeAlignment.TopCenter -> Alignment.TopCenter
            BadgeAlignment.BottomCenter -> Alignment.BottomCenter
            BadgeAlignment.Center -> Alignment.Center
            BadgeAlignment.CenterStart -> Alignment.CenterStart
            BadgeAlignment.CenterEnd -> Alignment.CenterEnd
            else -> Alignment.TopEnd
        }
        Box {
            imageComposable()
            element.badge.element?.let { badgeElement ->
                Box(modifier = Modifier.align(badgeAlignment)) {
                    AtomicRouter(badgeElement, screenState, onAction)
                }
            }
        }
    } else {
        imageComposable()
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
