package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AdSlot Renderer — placeholder for third-party ad content.
 *
 * In a production app this would integrate with Google Ad Manager or similar SDK.
 * For the prototype, it renders a labeled placeholder box showing the ad-unit path.
 *
 * Expected data:
 *   provider    – e.g. "gam" (Google Ad Manager)
 *   adUnitPath  – the ad-unit identifier string
 *   sizes[][]   – array of [width, height] pairs
 *   targeting{} – key-value targeting map (optional)
 */
@Composable
fun AdSlotRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val adUnitPath = data["adUnitPath"]?.toString()?.removeSurrounding("\"") ?: "unknown"
    val sizes = data["sizes"] as? List<*>
    val sizeLabel = sizes?.firstOrNull()?.let { pair ->
        val list = pair as? List<*>
        if (list != null && list.size >= 2) "${list[0]}×${list[1]}" else null
    } ?: "responsive"

    Box(
        modifier = modifier
            .semantics { contentDescription = "Advertisement" }
            .applyAccessibility(section.accessibility)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
            .height(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ADVERTISEMENT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing * 1.5
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = adUnitPath,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.25f)
            )
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.2f)
            )
        }
    }
}
