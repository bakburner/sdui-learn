package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.mapScoreboardHeader
import com.nba.sdui.core.renderer.interactions.SectionInteractions
import com.nba.sdui.core.state.SduiAction
import com.nba.sdui.models.generated.TeamData

/**
 * ScoreboardHeader Renderer - Displays game score with team logos.
 * 
 * Shows:
 * - Away team logo, tricode, score
 * - Game status (clock, period, or "Final")
 * - Home team logo, tricode, score
 */
@Composable
fun ScoreboardHeaderRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = mapScoreboardHeader(section)
    
    if (data == null) {
        // Fallback for invalid data
        Text(
            text = "Unable to load scoreboard",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    
    val primaryAction = SectionInteractions.primaryAction(section)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(enabled = primaryAction != null) { primaryAction?.let(onAction) }
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Away Team
            TeamScoreColumn(
                team = data.awayTeam,
                isHome = false
            )
            
            // Game Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = data.statusText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                data.periodLabel?.let { period ->
                    Text(
                        text = period,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
            
            // Home Team
            TeamScoreColumn(
                team = data.homeTeam,
                isHome = true
            )
        }
    }
}

@Composable
private fun TeamScoreColumn(
    team: TeamData,
    isHome: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(100.dp)
    ) {
        // Team Logo
        AsyncImage(
            model = team.logoUrl ?: "https://cdn.nba.com/logos/nba/${team.teamId}/global/L/logo.svg",
            contentDescription = "${team.teamName} logo",
            modifier = Modifier.size(60.dp),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Team Tricode
        Text(
            text = team.teamTricode,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        // Score
        Text(
            text = team.score.toString(),
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
