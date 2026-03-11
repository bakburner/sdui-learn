package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.config.SduiDefaults
import com.nba.sdui.core.renderer.adapters.mapHeroPanel
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.models.generated.HeroPanelData

/**
 * HeroPanel Renderer - Displays a single content card (article, video, etc.)
 */
@Composable
fun HeroPanelRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = mapHeroPanel(section)
    
    if (data == null) {
        Text(
            text = "Unable to load content",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    
    HeroPanelItem(
        card = data,
        onAction = onAction,
        modifier = modifier.padding(16.dp),
        fallbackThumbnailUrl = (section.data as? Map<*, *>)?.get("fallbackThumbnailUrl")?.toString()
            ?: SduiDefaults.FALLBACK_IMAGE_URL
    )
}

/**
 * Reusable content card item used by both HeroPanel and ContentRail renderers.
 */
@Composable
fun HeroPanelItem(
    card: HeroPanelData,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 280,
    fallbackThumbnailUrl: String? = null
) {
    Card(
        modifier = modifier
            .width(width.dp)
            .clickable {
                actionToSduiAction(card.action)?.let { onAction(it) }
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Thumbnail
            card.thumbnailUrl?.let { url ->
                Box {
                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = card.headline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop,
                        error = {
                            fallbackThumbnailUrl?.let { fb ->
                                AsyncImage(
                                    model = fb,
                                    contentDescription = card.headline,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    )
                    
                    // Duration badge for videos
                    card.duration?.let { duration ->
                        Text(
                            text = duration,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(8.dp)
                        )
                    }
                }
            }
            
            // Text Content
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Content type badge
                card.contentType?.name?.let { typeName ->
                    Text(
                        text = typeName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                // Headline
                Text(
                    text = card.headline,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Subhead
                card.subhead?.let { subhead ->
                    Text(
                        text = subhead,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
