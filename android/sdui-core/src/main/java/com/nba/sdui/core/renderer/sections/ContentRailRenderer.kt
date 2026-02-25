package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.mapContentRail
import com.nba.sdui.core.state.SduiAction

/**
 * ContentRail Renderer - Displays a horizontal scrolling strip of content cards.
 */
@Composable
fun ContentRailRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = mapContentRail(section)
    
    if (data == null) {
        Text(
            text = "Unable to load content rail",
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
        // Section Title
        data.title?.let { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Horizontal Scrolling Cards
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = data.cards,
                key = { it.id }
            ) { card ->
                ContentCardItem(
                    card = card,
                    onAction = onAction,
                    width = 260
                )
            }
        }
    }
}
