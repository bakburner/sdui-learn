package com.nba.sdui.core.renderer

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.SectionStates

/**
 * Generic section loading skeleton — renders shimmer, spinner, placeholder, or nothing
 * based on the server-declared SectionStates.loading configuration.
 *
 * Respects minHeightDp to prevent layout shift during section-level refresh.
 */
@Composable
fun SectionSkeleton(
    sectionStates: SectionStates?,
    modifier: Modifier = Modifier
) {
    val skeleton = sectionStates?.loading?.skeleton?.value ?: "shimmer"
    val minHeight = (sectionStates?.loading?.minHeightDP ?: 80L).toInt().dp

    when (skeleton) {
        "none" -> { /* render nothing */ }

        "spinner" -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        "placeholder" -> {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val placeholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(placeholderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(placeholderColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(placeholderColor)
                )
            }
        }

        else -> {
            // Default: shimmer
            val transition = rememberInfiniteTransition(label = "shimmer")
            val translateX by transition.animateFloat(
                initialValue = -300f,
                targetValue = 300f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shimmerTranslateX"
            )

            val shimmerColors = listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            )
            val brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(translateX, 0f),
                end = Offset(translateX + 300f, 0f)
            )

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(brush)
                )
            }
        }
    }
}
