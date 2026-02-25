package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.mapPromoBanner
import com.nba.sdui.core.state.SduiAction

/**
 * PromoBanner Renderer — promotional banner with optional image and CTA.
 *
 * Expected data fields:
 *   title         – small uppercase label (e.g. "NBA League Pass")
 *   headline      – large heading text
 *   subhead       – supporting description
 *   description   – alias for subhead (used if subhead is absent)
 *   imageUrl      – optional leading icon / logo
 *   actions       – list of Action objects; first is used for tap
 */
@Composable
fun PromoBannerRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapPromoBanner(section) ?: return

    val bgColor = section.backgroundColor?.let { parseColor(it) }
        ?: Color(0xFF2A2A5E)

    Box(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(bgColor, bgColor.copy(alpha = 0.85f))
                )
            )
            .clickable { model.primaryAction?.let(onAction) }
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Optional leading image / logo
            if (model.imageUrl != null) {
                AsyncImage(
                    model = model.imageUrl,
                    contentDescription = model.title,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (model.title != null) {
                    Text(
                        text = model.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (model.headline != null) {
                    Text(
                        text = model.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (model.subhead != null) {
                    Text(
                        text = model.subhead,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                if (model.primaryAction != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ElevatedButton(
                        onClick = { onAction(model.primaryAction) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1A1A2E)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Learn More",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun parseColor(hex: String): Color? {
    return try {
        val sanitised = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$sanitised"))
    } catch (_: Exception) {
        null
    }
}
