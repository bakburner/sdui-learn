package com.nba.sdui.core.renderer.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * Stub renderer for AdSlot. Reserves the ad SDK's eventual mount
 * rectangle — `data.sizes[0]` is the single source of truth for
 * dimensions, shared by the placeholder and (when it lands) the
 * ad SDK itself. Inner placeholder chrome (background color, label
 * text) comes from `data.placeholder`; outer chrome (margin,
 * padding, shadow, radius) comes from `section.surface` via the
 * shared SectionContainer wrapper.
 *
 * This renderer carries no client-side chrome defaults. A payload
 * missing required `sizes` produces an empty stub — reservation
 * dimensions and placeholder content come from the server payload.
 */
@Composable
fun AdSlotRenderer(
    section: Section,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = section.data ?: return
    val sizes = data.sizes ?: return
    val first = sizes.firstOrNull() ?: return
    if (first.size < 2) return
    val width = first[0].toInt()
    val height = first[1].toInt()

    val label = data.label
    val placeholder = data.placeholder
    val bgToken = placeholder?.backgroundColor
    val placeholderText = placeholder?.text ?: ""
    val bgColor = ColorTokenResolver.resolve(bgToken).takeOrElse(Color.Unspecified)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = label ?: "Advertisement" }
            .applyAccessibility(section.accessibility),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .width(width.dp)
                .height(height.dp)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (placeholderText.isNotEmpty()) {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun Color.takeOrElse(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this
