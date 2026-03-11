package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.adapters.SeasonLeadersTableUiModel
import com.nba.sdui.core.renderer.adapters.mapSeasonLeadersTable
import com.nba.sdui.core.state.SduiAction

private const val TAG = "SeasonLeadersTable"
private val RANK_COL_WIDTH = 36.dp
private val PLAYER_COL_WIDTH = 140.dp
private val TEAM_COL_WIDTH = 52.dp
private val STAT_COL_WIDTH = 56.dp
private val ROW_HEIGHT = 44.dp
private val DARK_BG = Color(0xFF1A1A2E)
private val HEADER_TEXT_COLOR = Color(0xFF777777)
private val SUBTITLE_COLOR = Color(0xFF9AA6BA)

/**
 * SeasonLeadersTable Renderer – Ranked table of season statistical leaders.
 *
 * Layout: frozen rank + player + team columns, horizontally scrollable stat columns.
 * Highlighted column for the current sort stat. Pagination info shown above the table.
 */
@Composable
fun SeasonLeadersTableRenderer(
    section: SduiSection,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapSeasonLeadersTable(section)

    if (model == null) {
        Log.w(TAG, "Unable to parse leaders table data for section ${section.id}")
        return
    }

    if (model.players.isEmpty()) {
        Text(
            text = model.emptyMessage ?: "No leaders data available",
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        return
    }

    val bgColor = section.backgroundColor?.let { parseHex(it) } ?: DARK_BG
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(bgColor, shape = MaterialTheme.shapes.medium)
    ) {
        // ── Header ───────────────────────────────────────────────────
        if (model.title != null || model.subtitle != null) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                model.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                model.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = SUBTITLE_COLOR,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // ── Pagination info ──────────────────────────────────────────
        if (model.totalRows != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${model.totalRows} Rows",
                    style = MaterialTheme.typography.labelSmall,
                    color = HEADER_TEXT_COLOR
                )
                if (model.page != null && model.pageSize != null && model.pageSize > 0) {
                    val totalPages = (model.totalRows + model.pageSize - 1) / model.pageSize
                    Text(
                        text = "Page ${model.page} of $totalPages",
                        style = MaterialTheme.typography.labelSmall,
                        color = HEADER_TEXT_COLOR
                    )
                }
            }
        }

        // ── Column headers ───────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            // Frozen header: Rank
            Box(
                modifier = Modifier.width(RANK_COL_WIDTH),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HEADER_TEXT_COLOR,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Frozen header: Player
            Box(
                modifier = Modifier.width(PLAYER_COL_WIDTH),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "PLAYER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HEADER_TEXT_COLOR,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                )
            }

            // Frozen header: Team
            Box(
                modifier = Modifier.width(TEAM_COL_WIDTH),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TEAM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HEADER_TEXT_COLOR,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Scrollable stat headers
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                model.columns.forEach { col ->
                    val isSorted = model.sortColumn == col.key
                    val arrow = if (isSorted) {
                        if (model.sortDirection == "asc") " ▲" else " ▼"
                    } else ""

                    Box(
                        modifier = Modifier
                            .width(col.width?.dp ?: STAT_COL_WIDTH)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${col.label}$arrow",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Medium,
                            color = if (col.highlighted) Color(0xFF1D8CF8) else HEADER_TEXT_COLOR,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 2.dp, color = Color(0xFF333333))

        // ── Player rows ──────────────────────────────────────────────
        model.players.forEach { player ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
            ) {
                // Frozen: Rank
                Box(
                    modifier = Modifier
                        .width(RANK_COL_WIDTH)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${player.rank}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Frozen: Player name
                Box(
                    modifier = Modifier
                        .width(PLAYER_COL_WIDTH)
                        .fillMaxHeight()
                        .padding(start = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Frozen: Team
                Box(
                    modifier = Modifier
                        .width(TEAM_COL_WIDTH)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = player.team,
                        style = MaterialTheme.typography.bodySmall,
                        color = SUBTITLE_COLOR,
                        textAlign = TextAlign.Center
                    )
                }

                // Scrollable stat cells
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    model.columns.forEach { col ->
                        val value = player.stats[col.key]
                        val display = if (value != null) formatStatValue(value) else "-"

                        Box(
                            modifier = Modifier
                                .width(col.width?.dp ?: STAT_COL_WIDTH)
                                .fillMaxHeight()
                                .then(
                                    if (col.highlighted) {
                                        Modifier.background(Color(0xFF1D8CF8).copy(alpha = 0.10f))
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = display,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (col.highlighted) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color(0xFF333333).copy(alpha = 0.5f)
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────

private fun formatStatValue(value: Any?): String {
    return when (value) {
        is Double -> {
            if (value == value.toLong().toDouble()) {
                value.toLong().toString()
            } else {
                String.format("%.1f", value)
            }
        }
        is Float -> String.format("%.1f", value)
        is Number -> value.toString()
        else -> value?.toString() ?: "-"
    }
}

private fun parseHex(hex: String): Color? {
    return try {
        val cleaned = hex.removePrefix("#")
        val argb = when (cleaned.length) {
            6 -> "FF$cleaned"
            8 -> cleaned
            else -> return null
        }
        Color(argb.toLong(16))
    } catch (_: Exception) {
        null
    }
}
