package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.models.DisplayGridColumn
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicDisplayGrid — display-only, non-interactive, server-ordered grid of text cells.
 *
 * Zero client interaction: no sort, no filter, no expand, no select, no tap.
 * Cell values are pre-formatted strings — no client-side formatting or computation.
 *
 * For sort, scroll-sync, frozen columns, pagination, or row interactivity,
 * use a dedicated section renderer (BoxscoreTable, SeasonLeadersTable, etc.).
 */
@Composable
fun AtomicDisplayGrid(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = element.columns ?: return
    val rows = element.rows ?: return
    val headerStyle = mapTypographyVariant(element.headerVariant ?: "labelMedium")
    val cellStyle = mapTypographyVariant(element.cellVariant ?: "bodyMedium")
    val striped = element.striped == true

    Column(modifier = modifier.fillMaxWidth().applyAccessibility(element.accessibility)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics { heading() }
        ) {
            columns.forEach { col ->
                CellSlot(col) {
                    Text(
                        text = col.label,
                        style = headerStyle,
                        textAlign = mapAlign(col.align),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        rows.forEachIndexed { index, row ->
            val rowBg = if (striped && index % 2 == 1) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                Color.Transparent
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(rowBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                columns.forEach { col ->
                    CellSlot(col) {
                        Text(
                            text = row[col.key].orEmpty(),
                            style = cellStyle,
                            textAlign = mapAlign(col.align),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.CellSlot(col: DisplayGridColumn, content: @Composable () -> Unit) {
    val widthMod = when (col.width) {
        is Number -> Modifier.weight(1f).padding(end = 4.dp)
        "flex"    -> Modifier.weight(1f).padding(end = 4.dp)
        else      -> Modifier.weight(1f).padding(end = 4.dp)
    }
    androidx.compose.foundation.layout.Box(modifier = widthMod) { content() }
}

private fun mapAlign(align: String?): TextAlign = when (align) {
    "center" -> TextAlign.Center
    "end"    -> TextAlign.End
    else     -> TextAlign.Start
}
