package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.mapSectionHeader
import com.nba.sdui.core.state.SduiAction

/**
 * SectionHeader Renderer — title row with optional subtitle and
 * a trailing "See All" / action link.
 */
@Composable
fun SectionHeaderRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapSectionHeader(section)

    if (model == null) {
        Text(
            text = "Unable to load section header",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            model.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        model.action?.let { action ->
            val label = action.message ?: action.targetUri?.substringAfterLast("/") ?: "See All"
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onAction(action) }
                    .padding(start = 8.dp)
            )
        }
    }
}
