package com.nba.sdui.core.renderer

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import com.nba.sdui.core.models.generated.AccessibilityProperties
import com.nba.sdui.core.models.generated.LiveRegion
import com.nba.sdui.core.models.generated.Role

/**
 * Applies server-provided [AccessibilityProperties] to a Compose [Modifier].
 *
 * When [a11y] is null the modifier is returned unchanged.
 * When [a11y.hidden] is true the element is removed from the accessibility tree.
 */
fun Modifier.applyAccessibility(a11y: AccessibilityProperties?): Modifier {
    if (a11y == null) return this

    if (a11y.hidden == true) {
        return this.clearAndSetSemantics {}
    }

    return this.semantics(mergeDescendants = false) {
        a11y.label?.let { contentDescription = it }
        a11y.role?.let { r ->
            when (r) {
                Role.Button -> role = androidx.compose.ui.semantics.Role.Button
                Role.Image -> role = androidx.compose.ui.semantics.Role.Image
                Role.Tab -> role = androidx.compose.ui.semantics.Role.Tab
                Role.Heading -> heading()
                else -> { /* unsupported roles are silently ignored */ }
            }
        }
        a11y.headingLevel?.let { heading() }
        a11y.liveRegion?.let { region ->
            when (region) {
                LiveRegion.Polite -> liveRegion = LiveRegionMode.Polite
                LiveRegion.Assertive -> liveRegion = LiveRegionMode.Assertive
                else -> { }
            }
        }
        a11y.sortOrder?.let { traversalIndex = it.toFloat() }
        a11y.hint?.let { stateDescription = it }
    }
}
