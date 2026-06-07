package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import coil.compose.SubcomposeAsyncImage
import com.nba.sdui.core.models.generated.ActionTrigger
import com.nba.sdui.core.network.WireUrlResolver
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.ImageFit
import com.nba.sdui.core.renderer.ImageVariantResolver
import com.nba.sdui.core.renderer.ImageVariantResolver.ImageContentScaleHint
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.request.RequestEnvelopeBuilder
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.state.SduiAction

// Product-approved static last-resort tile: blue background with white
// "demo" label so broken images are immediately identifiable as
// non-content and visually consistent across Android and iOS.
private val DemoPlaceholderBg = Color(0xFF1D428A)

/**
 * AtomicImage — renders an Image element via Coil AsyncImage. The box
 * model (margin / padding / bg / cornerRadius / shadow / border /
 * opacity / width / height / fillWidth / badge) comes from [AtomicBox];
 * this renderer only owns the content-scale and aspect-ratio concerns
 * that are semantically tied to the image itself.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AtomicImage(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    val variantSpec = ImageVariantResolver.resolve(element.variant, formFactor)
    val effectiveAspectRatio: Float? =
        LayoutTokenResolver.aspectRatio(element.aspectRatio) ?: variantSpec?.aspectRatio

    // Resolve `src` from `bindRef` when present, falling back to the
    // inline `src`. Lets composers rebind image URLs in flight without
    // touching the ui tree.
    val compositeContent = LocalCompositeContent.current
    val boundSrc = BindRefResolver.resolveString(element.bindRef, compositeContent) ?: element.src

    var currentSrc by remember { mutableStateOf<String?>(null) }
    var triedFallback by remember { mutableStateOf(false) }
    LaunchedEffect(boundSrc) {
        currentSrc = boundSrc
        triedFallback = false
    }
    val wireBase = LocalSduiWireAssetBaseUrl.current
    val modelUrl = WireUrlResolver.resolve(currentSrc ?: boundSrc, wireBase)
    val placeholderUrl = WireUrlResolver.resolve(element.placeholder, wireBase)

    val contentScale: ContentScale = element.fit?.let { mapContentScale(it) }
        ?: variantSpec?.contentScaleHint?.let(::mapContentScaleHint)
        ?: ContentScale.Fit

    val hasActions = !element.actions.isNullOrEmpty()
    val batchExecutor = LocalActionExecutor.current
    val activateActions = selectActions(element.actions, ActionTrigger.OnActivate)
    val longPressActions = selectActions(element.actions, ActionTrigger.OnLongPress)
    val focusActions = selectActions(element.actions, ActionTrigger.OnFocus)
    val blurActions = selectActions(element.actions, ActionTrigger.OnBlur)
    val isFocusable = focusActions.isNotEmpty() || blurActions.isNotEmpty()
    var hadFocus by remember(element.id) { mutableStateOf(false) }

    AtomicBox(element, screenState, onAction) { boxModifier ->
        var imageModifier = boxModifier
        effectiveAspectRatio?.let { imageModifier = imageModifier.aspectRatio(it) }
        if (hasActions || longPressActions.isNotEmpty()) {
            imageModifier = imageModifier.combinedClickable(
                onClick = { dispatchActions(activateActions, batchExecutor, onAction) },
                onLongClick = if (longPressActions.isNotEmpty()) {
                    { dispatchActions(longPressActions, batchExecutor, onAction) }
                } else {
                    null
                }
            )
        }
        if (isFocusable) {
            imageModifier = imageModifier
                .focusable()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused == hadFocus) return@onFocusChanged
                    hadFocus = focusState.isFocused
                    if (focusState.isFocused) {
                        dispatchActions(focusActions, batchExecutor, onAction)
                    } else {
                        dispatchActions(blurActions, batchExecutor, onAction)
                    }
                }
        }
        SubcomposeAsyncImage(
            model = modelUrl,
            contentDescription = element.accessibility?.label ?: "",
            contentScale = contentScale,
            modifier = imageModifier.applyAccessibility(element.accessibility),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(DemoPlaceholderBg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize().background(DemoPlaceholderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "demo",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            },
            onError = {
                if (!triedFallback && !placeholderUrl.isNullOrBlank() && placeholderUrl != modelUrl) {
                    triedFallback = true
                    currentSrc = element.placeholder
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
