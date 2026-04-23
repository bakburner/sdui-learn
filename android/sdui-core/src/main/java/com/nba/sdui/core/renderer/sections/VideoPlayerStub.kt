package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.state.SduiAction

/**
 * Stub renderer for VideoPlayer sections. Will be replaced with the
 * platform video SDK in a later phase; until then renders a
 * placeholder play icon. Outer chrome (background, corner radius)
 * comes from `section.display` via `SectionContainer`. The renderer
 * only owns the 16:9 content frame and placeholder glyph —
 * see AGENTS.md §15.1(2) and §15.3.
 */
@Composable
fun VideoPlayerStub(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val playerType = (data["playerType"] as? String) ?: "unknown"
    val contentId = (data["contentId"] as? String) ?: "unknown"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Video Player",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "$playerType • $contentId",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
