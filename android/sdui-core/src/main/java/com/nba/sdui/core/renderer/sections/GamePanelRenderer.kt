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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.adapters.GamePanelUiModel
import com.nba.sdui.core.renderer.adapters.GamePanelVisualState
import com.nba.sdui.core.renderer.adapters.mapGamePanel
import com.nba.sdui.core.renderer.atomic.parseColor
import com.nba.sdui.core.renderer.adapters.BackgroundGradientViewModel
import com.nba.sdui.core.renderer.adapters.BackgroundViewModel
import com.nba.sdui.core.state.SduiAction

@Composable
fun GamePanelRenderer(
    section: Section,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapGamePanel(section) ?: return
    GamePanelContent(section, model, onAction, modifier)
}

@Composable
private fun GamePanelContent(
    section: Section,
    model: GamePanelUiModel,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val config = model.displayConfig
    val isFeatured = model.variant == "featured"
    // Featured cards bump corner radius + elevation so they read as carousel
    // leads. Standard (or missing) keeps the displayConfig-driven values,
    // which match today's non-carousel GamePanel surfaces.
    val effectiveCornerRadius = if (isFeatured && config.cornerRadius == 12) 16 else config.cornerRadius
    val effectiveElevation = if (isFeatured) (config.elevation.coerceAtLeast(6)) else config.elevation
    val innerPadding = if (isFeatured) 24.dp else 20.dp
    val shape = RoundedCornerShape(effectiveCornerRadius.dp)

    val isLive = model.visualState == GamePanelVisualState.LIVE
    val activeBg = if (isLive) (config.liveBackground ?: config.background) else config.background

    val backgroundBrush: Brush? = when (activeBg) {
        is BackgroundViewModel.Gradient -> activeBg.gradient.toBrush()
        is BackgroundViewModel.Image -> {
            Brush.horizontalGradient(listOf(Color(0xFF1D428A), Color(0xFF1D428A)))
        }
        else -> null
    }
    val backgroundSolid: Color? = when (activeBg) {
        is BackgroundViewModel.Solid -> parseColor(activeBg.color)
        else -> null
    }

    val badgeColor = config.badgeColor?.let { parseColor(it) }
        ?: if (isLive) Color(0xFFC8102E) else Color(0xFF666666)

    val scoreStyle = if (config.scoreTextStyle == "prominent") {
        MaterialTheme.typography.headlineMedium
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val scoreWeight = if (config.scoreTextStyle == "prominent") {
        FontWeight.ExtraBold
    } else {
        FontWeight.Bold
    }

    Card(
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = effectiveElevation.dp),
        modifier = modifier
            .applyAccessibility(section.accessibility)
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = "${model.awayTricode} vs ${model.homeTricode}" }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .let { mod ->
                if (model.primaryAction != null) mod.clickable { onAction(model.primaryAction) }
                else mod
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .let { mod ->
                    if (config.cardHeight != null) mod.height(config.cardHeight.dp) else mod
                }
                .let { mod ->
                    when {
                        backgroundBrush != null -> mod.background(brush = backgroundBrush, shape = shape)
                        backgroundSolid != null -> mod.background(color = backgroundSolid, shape = shape)
                        else -> mod.background(color = Color(0xFF1A1F2E), shape = shape)
                    }
                }
                .clip(shape)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                        model.awayLogoUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = model.awayTricode,
                                modifier = Modifier.size(config.logoSize.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (model.visualState != GamePanelVisualState.PRE) {
                            Text(
                                text = model.awayScore,
                                color = Color.White,
                                style = scoreStyle,
                                fontWeight = scoreWeight,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
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
                        if (model.visualState == GamePanelVisualState.PRE) {
                            model.broadcaster?.let { broadcaster ->
                                Text(
                                    text = broadcaster,
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        model.homeLogoUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = model.homeTricode,
                                modifier = Modifier.size(config.logoSize.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        if (model.visualState != GamePanelVisualState.PRE) {
                            Text(
                                text = model.homeScore,
                                color = Color.White,
                                style = scoreStyle,
                                fontWeight = scoreWeight,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
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

                if (model.leaderLines.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Leaders",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.labelSmall
                        )
                        model.leaderLines.forEach { line ->
                            Text(
                                text = line,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                if (model.visualState != GamePanelVisualState.PRE && model.broadcaster != null) {
                    Text(
                        text = model.broadcaster,
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundGradientViewModel.toBrush(): Brush {
    val colors = this.colors.map { parseColor(it) }
    return when (direction) {
        "horizontal" -> Brush.horizontalGradient(colors)
        "diagonal" -> Brush.linearGradient(colors)
        else -> Brush.verticalGradient(colors)
    }
}
