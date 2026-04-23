package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * SubscribeBanner Renderer — inline subscription upsell banner.
 *
 * Outer chrome (margin, radius, gradient background, inner padding)
 * comes from `section.display` via `SectionContainer` — this renderer
 * only lays out the banner's content (title, subtitle, CTA).
 * See AGENTS.md §15.3.
 *
 * Expected data:
 *   title     – heading text
 *   subtitle  – supporting text
 *   ctaLabel  – button label
 *   ctaAction – Action for the CTA button
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
    val ctaLabel = data["ctaLabel"]?.toString()?.removeSurrounding("\"") ?: "Subscribe"
    val ctaAction = data["ctaAction"] as? Map<String, Any?>

    Column(
        modifier = modifier
            .applyAccessibility(section.accessibility)
            .fillMaxWidth()
            .clickable {
                ctaAction?.let { a ->
                    actionToSduiAction(a)?.let { onAction(it) }
                }
            }
    ) {
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
