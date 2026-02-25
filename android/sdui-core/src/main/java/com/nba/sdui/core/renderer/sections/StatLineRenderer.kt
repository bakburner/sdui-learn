package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.mapStatLineList
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.models.generated.StatLineData

/**
 * StatLine Renderer - Displays a list of player stat rows.
 * 
 * Shows:
 * - Section title (optional)
 * - List of stat lines with player image, name, stat category, and value
 */
@Composable
fun StatLineRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = mapStatLineList(section)
    
    if (data == null) {
        Text(
            text = "Unable to load stats",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Section Title
        data.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        
        // Stat Lines
        data.stats.forEach { stat ->
            StatLineRow(
                stat = stat,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun StatLineRow(
    stat: StatLineData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player Image
        stat.playerImageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = stat.playerName,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        // Player Name and Team
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.playerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            stat.teamTricode?.let { team ->
                Text(
                    text = team,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        // Stat Category
        Text(
            text = stat.statCategory,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        
        // Stat Value
        Text(
            text = stat.statValue,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
