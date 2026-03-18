package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nba.sdui.core.models.Background
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.models.parseBackground
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.config.SduiDefaults

/**
 * SubscribeBanner Renderer — inline subscription upsell banner.
 *
 * Expected data:
 *   title             – heading text
 *   subtitle          – supporting text
 *   background – optional background (BackgroundImage object)
 *   ctaLabel          – button label
 *   ctaAction         – Action for the CTA button
 */
@Composable
fun SubscribeBannerRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val title = data["title"]?.toString()?.removeSurrounding("\"") ?: return
    val subtitle = data["subtitle"]?.toString()?.removeSurrounding("\"")
    val bg = parseBackground(data["background"])
    val backgroundUrl = (bg as? Background.Image)?.imageUrl
    val ctaLabel = data["ctaLabel"]?.toString()?.removeSurrounding("\"") ?: "Subscribe"
    val ctaAction = data["ctaAction"] as? Map<String, Any?>
    val fallbackThumbnailUrl = data["fallbackThumbnailUrl"]?.toString()?.removeSurrounding("\"")
        ?: SduiDefaults.FALLBACK_IMAGE_URL

    Box(
        modifier = modifier
            .applyAccessibility(section.accessibility)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF1D428A), Color(0xFF862633))
                )
            )
            .clickable {
                ctaAction?.let { a ->
                    actionToSduiAction(a)?.let { onAction(it) }
                }
            }
            .padding(20.dp)
    ) {
        // Background image (if provided)
        backgroundUrl?.let { url ->
            SubcomposeAsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop,
                alpha = 0.3f,
                error = {
                    fallbackThumbnailUrl?.let { fb ->
                        AsyncImage(
                            model = fb,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.3f
                        )
                    }
                }
            )
        }

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            ElevatedButton(
                onClick = {
                    ctaAction?.let { a ->
                        actionToSduiAction(a)?.let { onAction(it) }
                    }
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1D428A)
                )
            ) {
                Text(text = ctaLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
