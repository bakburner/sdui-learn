package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nba.sdui.core.models.SduiSection
import com.nba.sdui.core.renderer.SectionRouter
import com.nba.sdui.core.state.SduiAction

private val mapper = ObjectMapper().registerKotlinModule()

/**
 * Row Renderer – Responsive layout primitive.
 *
 * Renders child sections side-by-side when screen width >= breakpoint,
 * and stacks them vertically when screen width < breakpoint.
 *
 * Data contract:
 *   children:   Array<Section>  – required
 *   spacing:    Int             – gap in dp (default 16)
 *   breakpoint: Int             – collapse threshold in dp (default 600)
 */
@Composable
fun RowRenderer(
    section: SduiSection,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data
    if (data == null) {
        Log.w("RowRenderer", "No data for Row section ${section.id}")
        return
    }

    // Parse children – they arrive as List<Map<String, Any?>> and must be converted to SduiSection
    val children: List<SduiSection> = parseChildren(data)

    if (children.isEmpty()) {
        Log.w("RowRenderer", "Row section ${section.id} has no children")
        return
    }

    val spacing = (data["spacing"] as? Number)?.toInt() ?: 16
    val breakpoint = (data["breakpoint"] as? Number)?.toInt() ?: 600

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isWide = screenWidthDp >= breakpoint

    if (isWide) {
        // Side-by-side layout
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            children.forEach { child ->
                Box(modifier = Modifier.weight(1f)) {
                    SectionRouter(
                        section = child,
                        screenState = screenState,
                        onAction = onAction,
                        onStateChange = onStateChange
                    )
                }
            }
        }
    } else {
        // Stacked layout
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.dp)
        ) {
            children.forEach { child ->
                SectionRouter(
                    section = child,
                    screenState = screenState,
                    onAction = onAction,
                    onStateChange = onStateChange
                )
            }
        }
    }
}

/**
 * Parse children sections from the loosely-typed data map.
 * Uses Jackson to convert List<Map> → List<SduiSection>.
 */
@Suppress("UNCHECKED_CAST")
private fun parseChildren(data: Map<String, Any?>): List<SduiSection> {
    val raw = data["children"] as? List<*> ?: return emptyList()
    return try {
        raw.mapNotNull { child ->
            if (child is Map<*, *>) {
                mapper.convertValue(child, SduiSection::class.java)
            } else null
        }
    } catch (e: Exception) {
        Log.e("RowRenderer", "Failed to parse children: ${e.message}")
        emptyList()
    }
}
