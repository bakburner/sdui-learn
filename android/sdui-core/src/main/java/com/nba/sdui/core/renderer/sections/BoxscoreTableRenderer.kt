package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.BoxscoreTableUiModel
import com.nba.sdui.core.renderer.adapters.BoxscoreColumnDef
import com.nba.sdui.core.renderer.adapters.BoxscorePlayerRowUi
import com.nba.sdui.core.renderer.adapters.mapBoxscoreTable
import com.nba.sdui.core.state.SduiAction

private const val TAG = "BoxscoreTable"
private val PLAYER_COL_WIDTH = 140.dp
private val STAT_COL_WIDTH = 52.dp
private val ROW_HEIGHT = 44.dp

/**
 * BoxscoreTable Renderer – Frozen player column + horizontally scrollable stat columns.
 *
 * Client owns all rendering decisions:
 * - Frozen player column: circular headshot, name, jersey/position
 * - Scrollable stat columns: client decides column width, text formatting
 * - Sort: column header taps fire mutate actions to update screen state
 * - Totals row: frozen at bottom, excluded from sort
 * - DNP players: show reason text, no stat values
 */
@Composable
fun BoxscoreTableRenderer(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapBoxscoreTable(section, screenState)

    if (model == null) {
        Text(
            text = "Unable to load boxscore",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    // Empty state
    if (model.players.isEmpty()) {
        val msg = model.emptyMessage ?: "No player data available"
        Text(
            text = msg,
            modifier = modifier
                .fillMaxWidth()
                .padding(32.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        return
    }

    // Sort players client-side
    val sortedPlayers by remember(model.players, model.sortColumn, model.sortDirection) {
        derivedStateOf {
            sortPlayers(model.players, model.sortColumn, model.sortDirection)
        }
    }

    val teamColor = model.teamColor?.let { parseHexColor(it) }
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth().applyAccessibility(section.accessibility)) {
        // Header row
        Row(modifier = Modifier.fillMaxWidth().semantics { heading() }) {
            // Player column header
            Box(
                modifier = Modifier
                    .width(PLAYER_COL_WIDTH)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "PLAYER",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Scrollable stat column headers
            Row(modifier = Modifier.horizontalScroll(scrollState)) {
                model.columns.forEach { col ->
                    val isSorted = model.sortColumn == col.key
                    val arrow = if (isSorted) {
                        if (model.sortDirection == "asc") " ▲" else " ▼"
                    } else ""

                    Box(
                        modifier = Modifier
                            .width(col.width?.dp ?: STAT_COL_WIDTH)
                            .clickable(enabled = col.sortable) {
                                handleColumnSort(
                                    col.key,
                                    model.sortStateKey,
                                    model.sortDirectionStateKey,
                                    model.sortColumn,
                                    model.sortDirection,
                                    onStateChange
                                )
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${col.label}${arrow}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSorted) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSorted) {
                                teamColor ?: MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp)

        // Starter / bench divider tracking
        var seenBench = false

        // Player rows
        sortedPlayers.forEach { player ->
            if (!player.starter && !seenBench) {
                seenBench = true
                BenchDivider()
            }

            key(boxscorePlayerRowKey(player)) {
            Row(modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT)) {
                // Frozen player column
                PlayerCell(
                    name = player.name,
                    imageUrl = player.imageUrl,
                    jerseyNumber = player.jerseyNumber,
                    position = player.position,
                    played = player.played,
                    notPlayingReason = player.notPlayingReason,
                    width = PLAYER_COL_WIDTH
                )

                // Scrollable stat cells
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    if (player.played) {
                        model.columns.forEach { col ->
                            val value = player.stats[col.key]
                            StatCell(
                                value = value?.toString() ?: "-",
                                width = col.width?.dp ?: STAT_COL_WIDTH,
                                highlighted = col.highlighted
                            )
                        }
                    } else {
                        // DNP — span reason across columns
                        Box(
                            modifier = Modifier
                                .width(STAT_COL_WIDTH * model.columns.size)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = player.notPlayingReason ?: "DNP",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            }
        }

        // Totals row
        model.teamTotals?.let { totals ->
            HorizontalDivider(thickness = 2.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                // "TOTAL" label in the player column
                Box(
                    modifier = Modifier
                        .width(PLAYER_COL_WIDTH)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "TOTAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Scrollable totals
                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    model.columns.forEach { col ->
                        val value = totals[col.key]
                        StatCell(
                            value = value?.toString() ?: "",
                            width = col.width?.dp ?: STAT_COL_WIDTH,
                            highlighted = col.highlighted,
                            bold = true
                        )
                    }
                }
            }
        }
    }
}

// ── Player Cell ──────────────────────────────────────────────────────

@Composable
private fun PlayerCell(
    name: String,
    imageUrl: String?,
    jerseyNumber: String?,
    position: String?,
    played: Boolean,
    notPlayingReason: String?,
    width: Dp
) {
    Row(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Headshot
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (played) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            val subtitle = listOfNotNull(jerseyNumber?.let { "#$it" }, position)
                .joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ── Stat Cell ────────────────────────────────────────────────────────

@Composable
private fun StatCell(
    value: String,
    width: Dp,
    highlighted: Boolean = false,
    bold: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .then(
                if (highlighted) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold || highlighted) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

// ── Bench Divider ────────────────────────────────────────────────────

@Composable
private fun BenchDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "BENCH",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

// ── Sort Logic ───────────────────────────────────────────────────────

private fun handleColumnSort(
    colKey: String,
    sortStateKey: String?,
    sortDirectionStateKey: String?,
    currentSortColumn: String?,
    currentSortDirection: String,
    onStateChange: (String, Any) -> Unit
) {
    if (sortStateKey == null || sortDirectionStateKey == null) return

    if (currentSortColumn == colKey) {
        // Toggle direction
        val newDir = if (currentSortDirection == "asc") "desc" else "asc"
        onStateChange(sortDirectionStateKey, newDir)
    } else {
        // New column, default to desc
        onStateChange(sortStateKey, colKey)
        onStateChange(sortDirectionStateKey, "desc")
    }
}

private fun boxscorePlayerRowKey(player: BoxscorePlayerRowUi): Any =
    player.playerId.takeIf { it != 0 } ?: player.name

private fun sortPlayers(
    players: List<BoxscorePlayerRowUi>,
    sortColumn: String?,
    sortDirection: String
): List<BoxscorePlayerRowUi> {
    if (sortColumn == null) return players

    // Separate starters and bench — maintain grouping
    val starters = players.filter { it.starter }
    val bench = players.filter { !it.starter }

    val comparator = Comparator<BoxscorePlayerRowUi> { a, b ->
        if (!a.played && !b.played) return@Comparator 0
        if (!a.played) return@Comparator 1
        if (!b.played) return@Comparator -1

        val aVal = (a.stats[sortColumn] as? Number)?.toDouble() ?: 0.0
        val bVal = (b.stats[sortColumn] as? Number)?.toDouble() ?: 0.0
        if (sortDirection == "asc") aVal.compareTo(bVal) else bVal.compareTo(aVal)
    }

    return starters.sortedWith(comparator) + bench.sortedWith(comparator)
}

// ── Color parsing ────────────────────────────────────────────────────

private fun parseHexColor(hex: String): Color? {
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
