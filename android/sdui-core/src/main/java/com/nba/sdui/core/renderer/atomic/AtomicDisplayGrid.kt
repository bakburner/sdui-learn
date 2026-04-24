package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Align
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.Column as GridColumn
import com.nba.sdui.core.models.generated.WidthEnum
import com.nba.sdui.core.models.generated.WidthUnion
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicDisplayGrid — display-only, non-interactive, server-ordered grid
 * of text cells. Zero client interaction (no sort, filter, expand,
 * select, or tap). Cell values are pre-formatted strings.
 *
 * Box-model chrome (margin / padding / bg / cornerRadius / shadow /
 * opacity) comes from [AtomicBox]; the table owns only its grid layout.
 */
@Composable
fun AtomicDisplayGrid(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit
) {
    val columns = element.columns ?: return
    val rows = element.rows ?: return
    val headerStyle = MaterialTheme.typography.labelMedium
    val cellStyle = MaterialTheme.typography.bodyMedium
    val striped = element.striped == true

    AtomicBox(element, screenState, onAction) { boxModifier ->
        Column(modifier = boxModifier.fillMaxWidth().applyAccessibility(element.accessibility)) {
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
}

@Composable
private fun RowScope.CellSlot(col: GridColumn, content: @Composable () -> Unit) {
    val widthMod = when (val width = col.width) {
        is WidthUnion.IntegerValue -> Modifier.width(width.value.toInt().dp).padding(end = 4.dp)
        is WidthUnion.EnumValue -> when (width.value) {
            WidthEnum.Flex -> Modifier.weight(1f).padding(end = 4.dp)
        }
        else -> Modifier.weight(1f).padding(end = 4.dp)
    }
    androidx.compose.foundation.layout.Box(modifier = widthMod) { content() }
}

private fun mapAlign(align: Align?): TextAlign = when (align) {
    Align.Center -> TextAlign.Center
    Align.End -> TextAlign.End
    else -> TextAlign.Start
}
