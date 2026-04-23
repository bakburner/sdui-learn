package com.nba.sdui.core.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.generated.SectionSurface
import com.nba.sdui.core.models.generated.Spacing
import com.nba.sdui.core.renderer.adapters.BackgroundViewModel
import com.nba.sdui.core.renderer.adapters.toViewModel

/**
 * Shared section-surface wrapper applied by [SectionRouter] to every
 * permanent section. Reads [SectionSurface] (margin, padding,
 * background, cornerRadius, shadow, border) and applies it
 * platform-natively, so permanent-section renderers never set their
 * own outer chrome.
 *
 * Supports three `background` shapes — matching the schema's
 * `Background` union:
 *   • string  → token or hex, resolved to a solid [Color]
 *   • object with `colors`    → [Brush] linear gradient with direction
 *   • object with `imageUrl`  → remote [AsyncImage] (surface layer
 *                                sits below the content Box)
 *
 * See AGENTS.md §15.3 for the governance rule this wrapper enforces,
 * and `SduiUtils.defaultSurface()` on the server for the default
 * surface values composers emit.
 */
@Composable
fun SectionContainer(
    surface: SectionSurface?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val margin = surface?.margin.toPaddingValues()
    val padding = surface?.padding.toPaddingValues()
    val radius = (surface?.cornerRadius ?: 0L).toInt().dp
    val shadow = surface?.shadow
    val border = surface?.border
    val bg = surface?.background.toViewModel()

    var outer: Modifier = modifier.padding(margin)

    if (shadow != null) {
        outer = outer.shadow(
            elevation = (shadow.radius ?: 4.0).dp,
            shape = RoundedCornerShape(radius),
            clip = false
        )
    }

    outer = outer.clip(RoundedCornerShape(radius))

    val solidOrGradientBrush: Brush? = when (bg) {
        is BackgroundViewModel.Solid -> {
            val c = ColorTokenResolver.resolve(bg.color)
            if (c == Color.Unspecified) null else Brush.linearGradient(listOf(c, c))
        }
        is BackgroundViewModel.Gradient -> gradientBrush(bg)
        else -> null
    }

    if (solidOrGradientBrush != null) {
        outer = outer.background(solidOrGradientBrush)
    }

    if (border != null) {
        val borderColor = ColorTokenResolver.resolve(border.color)
        if (borderColor != Color.Unspecified) {
            outer = outer.border(
                width = (border.width ?: 1.0).dp,
                color = borderColor,
                shape = RoundedCornerShape(radius)
            )
        }
    }

    Box(modifier = outer) {
        if (bg is BackgroundViewModel.Image) {
            AsyncImage(
                model = bg.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
private fun gradientBrush(bg: BackgroundViewModel.Gradient): Brush? {
    val stops = bg.gradient.colors
        .map { ColorTokenResolver.resolve(it) }
        .filter { it != Color.Unspecified }
    if (stops.size < 2) return null
    return when (bg.gradient.direction) {
        "horizontal" -> Brush.horizontalGradient(stops)
        "diagonal" -> Brush.linearGradient(stops)
        else -> Brush.verticalGradient(stops)
    }
}

private fun Spacing?.toPaddingValues(): PaddingValues {
    val s = this ?: return PaddingValues(0.dp)
    return PaddingValues(
        top = (s.top ?: 0L).toInt().dp,
        bottom = (s.bottom ?: 0L).toInt().dp,
        start = (s.start ?: 0L).toInt().dp,
        end = (s.end ?: 0L).toInt().dp
    )
}
