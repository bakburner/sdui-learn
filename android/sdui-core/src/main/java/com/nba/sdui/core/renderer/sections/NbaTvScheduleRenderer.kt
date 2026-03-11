package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.core.config.SduiDefaults

/**
 * NbaTvSchedule Renderer — hero image for current program + time-slot list.
 *
 * Expected data:
 *   heroImageUrl  – hero banner image
 *   heroTitle     – current program title
 *   heroSubtitle  – descriptive text
 *   liveNow       – boolean, show live badge
 *   slots[]       – NbaTvSlot objects (id, title, subtitle, startTime, endTime, thumbnailUrl, isLive, action)
 */
@Composable
fun NbaTvScheduleRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val heroImageUrl = data["heroImageUrl"]?.toString()?.removeSurrounding("\"")
    val heroTitle = data["heroTitle"]?.toString()?.removeSurrounding("\"")
    val heroSubtitle = data["heroSubtitle"]?.toString()?.removeSurrounding("\"")
    val liveNow = data["liveNow"] == true
    val fallbackThumbnailUrl = data["fallbackThumbnailUrl"]?.toString()?.removeSurrounding("\"")
        ?: SduiDefaults.FALLBACK_IMAGE_URL
    val slots = data["slots"] as? List<*> ?: emptyList<Any>()

    Column(modifier = modifier) {
        // Hero section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            heroImageUrl?.let { url ->
                SubcomposeAsyncImage(
                    model = url,
                    contentDescription = heroTitle,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    error = {
                        fallbackThumbnailUrl?.let { fb ->
                            AsyncImage(
                                model = fb,
                                contentDescription = heroTitle,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                )
            }

            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Hero text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                if (liveNow) {
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .background(Color(0xFFC8102E), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                heroTitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                heroSubtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Schedule heading
        Text(
            text = "Today's Schedule",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // Slot list
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            for (slot in slots) {
                @Suppress("UNCHECKED_CAST")
                val map = slot as? Map<String, Any?> ?: continue
                NbaTvSlotRow(map = map, onAction = onAction)
            }
        }
    }
}

@Composable
private fun NbaTvSlotRow(
    map: Map<String, Any?>,
    onAction: (SduiAction) -> Unit
) {
    val title = map["title"]?.toString() ?: ""
    val subtitle = map["subtitle"]?.toString()
    val startTime = map["startTime"]?.toString() ?: ""
    val isLive = map["isLive"] == true
    val action = @Suppress("UNCHECKED_CAST") (map["action"] as? Map<String, Any?>)

    // Parse time for display (show just the time portion)
    val displayTime = try {
        startTime.substringAfter("T").take(5)
    } catch (_: Exception) {
        startTime
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1F2E))
            .clickable {
                action?.let { a ->
                    actionToSduiAction(a)?.let { onAction(it) }
                }
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = displayTime,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.width(50.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isLive) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFFC8102E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
