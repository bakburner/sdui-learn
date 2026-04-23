package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.models.actionToSduiAction
import com.nba.sdui.core.state.SduiAction

/**
 * SubscribeHero Renderer — full-card subscription upsell with feature
 * list and pricing tiers.
 *
 * Outer chrome (margin, radius, gradient background, inner padding)
 * comes from `section.display` via `SectionContainer` — this renderer
 * only lays out the hero's content (logo, title, subtitle, features,
 * tier cards, optional CTA). Tier-card surfaces remain client-owned
 * for now (inner content chrome). See AGENTS.md §15.3.
 *
 * Expected data:
 *   title     – main heading
 *   subtitle  – supporting tagline
 *   logoUrl   – brand logo
 *   features[] – list of bullet-point strings
 *   tiers[]    – SubscriptionTier objects
 */
@Composable
fun SubscribeHeroRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val title = data["title"]?.toString()?.removeSurrounding("\"") ?: return
    val subtitle = data["subtitle"]?.toString()?.removeSurrounding("\"")
    val logoUrl = data["logoUrl"]?.toString()?.removeSurrounding("\"")
    val features = data["features"] as? List<*> ?: emptyList<Any>()
    val tiers = data["tiers"] as? List<*> ?: emptyList<Any>()

    Column(
        modifier = modifier
            .applyAccessibility(section.accessibility)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        logoUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = title,
                modifier = Modifier.height(40.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        subtitle?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        if (features.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (feature in features) {
                    val text = feature?.toString() ?: continue
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓",
                            color = Color(0xFF00C853),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        for (tier in tiers) {
            val map = tier as? Map<String, Any?> ?: continue
            SubscriptionTierCard(map = map, onAction = onAction)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SubscriptionTierCard(
    map: Map<String, Any?>,
    onAction: (SduiAction) -> Unit
) {
    val name = map["name"]?.toString() ?: ""
    val price = map["price"]?.toString() ?: ""
    val originalPrice = map["originalPrice"]?.toString()
    val badgeText = map["badgeText"]?.toString()
    val tierFeatures = map["features"] as? List<*> ?: emptyList<Any>()
    val ctaLabel = map["ctaLabel"]?.toString() ?: "Subscribe"
    val ctaAction = map["ctaAction"] as? Map<String, Any?>

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        badgeText?.let { badge ->
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFFC8102E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            originalPrice?.let { orig ->
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = orig,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        if (tierFeatures.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            for (feature in tierFeatures) {
                Text(
                    text = "• ${feature ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedButton(
            onClick = {
                ctaAction?.let { a ->
                    actionToSduiAction(a)?.let { onAction(it) }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF1D428A)
            )
        ) {
            Text(text = ctaLabel, fontWeight = FontWeight.Bold)
        }
    }
}
