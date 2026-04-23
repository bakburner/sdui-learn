package com.nba.sdui.core.renderer.adapters

import android.util.Log
import com.nba.sdui.core.models.generated.Background
import com.nba.sdui.core.models.generated.BackgroundGradient
import com.nba.sdui.core.models.generated.BackgroundUnion
import com.nba.sdui.core.models.generated.Direction
import com.nba.sdui.core.models.generated.Overlay
import com.nba.sdui.core.models.generated.ScaleType

private const val TAG = "BackgroundViewModel"

data class BackgroundGradientViewModel(
    val colors: List<String>,
    val direction: String = "vertical"
)

sealed class BackgroundViewModel {
    data class Solid(val color: String) : BackgroundViewModel()
    data class Gradient(val gradient: BackgroundGradientViewModel) : BackgroundViewModel()
    data class Image(
        val imageUrl: String,
        val scaleType: String = "cover",
        val overlay: BackgroundViewModel? = null
    ) : BackgroundViewModel()
}

fun BackgroundUnion?.toViewModel(): BackgroundViewModel? {
    if (this == null) return null
    return when (this) {
        is BackgroundUnion.StringValue -> BackgroundViewModel.Solid(value)
        is BackgroundUnion.BackgroundValue -> toViewModel(value)
    }
}

private fun toViewModel(background: Background): BackgroundViewModel? {
    val imageUrl = background.imageURL
    if (!imageUrl.isNullOrBlank()) {
        val overlay = background.overlay?.let { toViewModel(it) }
        return BackgroundViewModel.Image(
            imageUrl = imageUrl,
            scaleType = background.scaleType?.value ?: "cover",
            overlay = overlay
        )
    }

    val colors = background.colors
    if (!colors.isNullOrEmpty()) {
        val direction = background.direction?.value ?: "vertical"
        return BackgroundViewModel.Gradient(
            BackgroundGradientViewModel(colors = colors, direction = direction)
        )
    }

    Log.w(TAG, "Background object has no imageUrl or colors")
    return null
}

private fun toViewModel(overlay: Overlay): BackgroundViewModel? {
    return when (overlay) {
        is Overlay.StringValue -> BackgroundViewModel.Solid(overlay.value)
        is Overlay.BackgroundGradientValue -> toViewModel(overlay.value)
    }
}

private fun toViewModel(gradient: BackgroundGradient): BackgroundViewModel? {
    val colors = gradient.colors
    if (colors.isEmpty()) {
        Log.w(TAG, "Background gradient has no colors")
        return null
    }
    val direction = gradient.direction?.value ?: "vertical"
    return BackgroundViewModel.Gradient(
        BackgroundGradientViewModel(colors = colors, direction = direction)
    )
}
