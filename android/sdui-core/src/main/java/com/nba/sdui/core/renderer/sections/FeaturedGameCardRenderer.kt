package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.nba.sdui.core.renderer.adapters.GameCardVisualState
import com.nba.sdui.core.renderer.adapters.mapFeaturedGameCard
import com.nba.sdui.core.state.SduiAction

/**
 * FeaturedGameCard Renderer — large hero-style game card with team logos,
 * scores, status badge, and optional background image / gradient.
 */
@Composable
fun FeaturedGameCardRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapFeaturedGameCard(section)

    if (model == null) {
        Text(
            text = "Unable to load featured game",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    val shape = RoundedCornerShape(16.dp)
    val liveGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1D428A), Color(0xFFC8102E))
    )
    val defaultGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1D428A), Color(0xFF1D428A))
    )

    Card(
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .let { mod ->
                if (model.primaryAction != null) mod.clickable { onAction(model.primaryAction) }
                else mod
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = if (model.visualState == GameCardVisualState.LIVE) liveGradient else defaultGradient,
                    shape = shape
                )
                .clip(shape)
        ) {
            // Content overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: visualLabel + badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    model.visualLabel?.let { label ->
                        Text(
                            text = label,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    } ?: Spacer(Modifier.width(0.dp))

                    model.badgeText?.let { badge ->
                        val badgeColor = if (model.visualState == GameCardVisualState.LIVE)
                            Color(0xFFC8102E) else Color(0xFF666666)
                        Text(
                            text = badge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .background(
                                    color = badgeColor,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Teams row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Away team
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (model.awayLogoUrl != null) {
                            AsyncImage(
                                model = model.awayLogoUrl,
                                contentDescription = model.awayTricode,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (model.visualState != GameCardVisualState.PRE) {
                            Text(
                                text = model.awayScore,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        model.awayName?.let { name ->
                            Text(
                                text = name,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        model.awayRecord?.let { record ->
                            Text(
                                text = record,
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Status center
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = model.statusText,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Home team
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (model.homeLogoUrl != null) {
                            AsyncImage(
                                model = model.homeLogoUrl,
                                contentDescription = model.homeTricode,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (model.visualState != GameCardVisualState.PRE) {
                            Text(
                                text = model.homeScore,
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        model.homeName?.let { name ->
                            Text(
                                text = name,
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        model.homeRecord?.let { record ->
                            Text(
                                text = record,
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
