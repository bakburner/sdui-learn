package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.FollowingRailItemUi
import com.nba.sdui.core.renderer.adapters.mapFollowingRail
import com.nba.sdui.core.state.SduiAction

/**
 * FollowingRail Renderer — horizontal strip of circular avatar thumbnails
 * representing followed teams or players.
 */
@Composable
fun FollowingRailRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapFollowingRail(section)

    if (model == null) {
        Text(
            text = "Unable to load following rail",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        model.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            items(
                items = model.items,
                key = { it.id }
            ) { item ->
                FollowingRailItem(item = item, onAction = onAction)
            }
        }
    }
}

@Composable
private fun FollowingRailItem(
    item: FollowingRailItemUi,
    onAction: (SduiAction) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .let { mod ->
                if (item.action != null) mod.clickable { onAction(item.action) }
                else mod
            }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = item.name.take(3).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
