package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.nba.sdui.core.renderer.adapters.GamePanelUiModel
import com.nba.sdui.core.renderer.adapters.GamePanelVisualState
import com.nba.sdui.core.renderer.adapters.mapGamePanel
import com.nba.sdui.core.renderer.interactions.SectionInteractions
import com.nba.sdui.core.state.SduiAction

@Composable
fun GamePanelRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapGamePanel(section) ?: return
    if (model.variant == "featured") {
        FeaturedGamePanelContent(model, onAction, modifier)
    } else {
        StandardGamePanelContent(section, model, onAction, modifier)
    }
}

@Composable
private fun StandardGamePanelContent(
    section: SduiSection,
    model: GamePanelUiModel,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryAction = SectionInteractions.primaryAction(section)

    Card(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clickable(enabled = primaryAction != null) { primaryAction?.let(onAction) },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1F2E)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    model.awayLogoUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "${model.awayTricode} logo",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column {
                        Text(
                            text = model.awayTricode,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        model.awayRecord?.let { record ->
                            Text(
                                text = record,
                                color = Color(0xFF8892A4),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = model.homeTricode,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        model.homeRecord?.let { record ->
                            Text(
                                text = record,
                                color = Color(0xFF8892A4),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    model.homeLogoUrl?.let { url ->
                        Spacer(modifier = Modifier.width(6.dp))
                        AsyncImage(
                            model = url,
                            contentDescription = "${model.homeTricode} logo",
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (model.visualState) {
                GamePanelVisualState.PRE -> {
                    Text(
                        text = model.statusText,
                        color = Color(0xFFB4C0D3),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    model.recordsText?.let { records ->
                        Text(
                            text = records,
                            color = Color(0xFF96A2B5),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                GamePanelVisualState.LIVE -> {
                    ScoreRow(
                        homeTricode = model.homeTricode,
                        homeScore = model.homeScore,
                        awayTricode = model.awayTricode,
                        awayScore = model.awayScore
                    )
                    Text(
                        text = model.statusText,
                        color = Color(0xFFFF6B6B),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    ScoreRow(
                        homeTricode = model.homeTricode,
                        homeScore = model.homeScore,
                        awayTricode = model.awayTricode,
                        awayScore = model.awayScore
                    )
                    Text(
                        text = model.statusText,
                        color = Color(0xFFB4C0D3),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Leaders",
                color = Color(0xFF96A2B5),
                style = MaterialTheme.typography.labelSmall
            )
            model.leaderLines.forEach { line ->
                Text(
                    text = line,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            model.broadcaster?.let { broadcaster ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = broadcaster,
                    color = Color(0xFF8892A4),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun FeaturedGamePanelContent(
    model: GamePanelUiModel,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    brush = if (model.visualState == GamePanelVisualState.LIVE) liveGradient else defaultGradient,
                    shape = shape
                )
                .clip(shape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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
                        val badgeColor = if (model.visualState == GamePanelVisualState.LIVE)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (model.awayLogoUrl != null) {
                            AsyncImage(
                                model = model.awayLogoUrl,
                                contentDescription = model.awayTricode,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (model.visualState != GamePanelVisualState.PRE) {
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

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = model.statusText,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (model.homeLogoUrl != null) {
                            AsyncImage(
                                model = model.homeLogoUrl,
                                contentDescription = model.homeTricode,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (model.visualState != GamePanelVisualState.PRE) {
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

@Composable
private fun ScoreRow(
    homeTricode: String,
    homeScore: String,
    awayTricode: String,
    awayScore: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF101521))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$awayTricode $awayScore",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$homeTricode $homeScore",
            color = Color.White,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}
