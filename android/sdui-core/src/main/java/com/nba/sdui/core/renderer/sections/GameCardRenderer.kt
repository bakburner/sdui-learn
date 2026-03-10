package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.GameCardVisualState
import com.nba.sdui.core.renderer.adapters.mapGameCard
import com.nba.sdui.core.renderer.interactions.SectionInteractions
import com.nba.sdui.core.state.SduiAction

@Composable
fun GameCardRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapGameCard(section) ?: return
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
                GameCardVisualState.PRE -> {
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
                GameCardVisualState.LIVE -> {
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
