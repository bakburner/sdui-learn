package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.config.SduiDefaults

/**
 * VideoCarousel Renderer — horizontal scrolling carousel of video thumbnails (16:9).
 *
 * Expected data:
 *   title    – section heading
 *   subtitle – optional
 *   items[]  – VideoThumbnail objects (id, title, subtitle, thumbnailUrl, duration, badgeText, action)
 */
@Composable
fun VideoCarouselRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val title = data["title"]?.toString()?.removeSurrounding("\"")
    val subtitle = data["subtitle"]?.toString()?.removeSurrounding("\"")
    val items = data["items"] as? List<*> ?: return
    val fallbackThumbnailUrl = data["fallbackThumbnailUrl"]?.toString()?.removeSurrounding("\"")
        ?: SduiDefaults.FALLBACK_IMAGE_URL

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        // Header
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal scrolling row
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (item in items) {
                val map = item as? Map<*, *> ?: continue
                VideoThumbnailCard(
                    id = map["id"]?.toString() ?: "",
                    title = map["title"]?.toString() ?: "",
                    subtitle = map["subtitle"]?.toString(),
                    thumbnailUrl = map["thumbnailUrl"]?.toString(),
                    duration = map["duration"]?.toString(),
                    badgeText = map["badgeText"]?.toString(),
                    action = @Suppress("UNCHECKED_CAST") (map["action"] as? Map<String, Any?>),
                    fallbackThumbnailUrl = fallbackThumbnailUrl,
                    onAction = onAction
                )
            }
        }
    }
}

@Composable
private fun VideoThumbnailCard(
    id: String,
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    duration: String?,
    badgeText: String?,
    action: Map<String, Any?>?,
    fallbackThumbnailUrl: String? = null,
    onAction: (SduiAction) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(shape)
            .background(Color(0xFF1A1F2E))
            .clickable {
                action?.let { a ->
                    val sduiAction = actionToSduiAction(a)
                    sduiAction?.let { onAction(it) }
                }
            }
    ) {
        // Thumbnail with overlays
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp) // 16:9 ratio
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {
            thumbnailUrl?.let { url ->
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        fallbackThumbnailUrl?.let { fb ->
                            AsyncImage(
                                model = fb,
                                contentDescription = title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                )
            }

            // Duration badge (bottom-right)
            duration?.let { dur ->
                Text(
                    text = dur,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Badge text (top-left) e.g. "LIVE", "NEW"
            badgeText?.let { badge ->
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color(0xFFC8102E), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Text
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
