package com.nba.sdui.core.state

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/**
 * Tracks which sections are near the viewport in a LazyColumn.
 *
 * Uses [LazyListState.layoutInfo] to determine visible items plus a
 * configurable buffer (default 1.5× viewport lookahead). Section exit
 * is debounced by [exitDebounceMs] to absorb scroll bounce.
 *
 * Consumers read [isNearViewport] to gate polling / SSE processing.
 */
class SectionVisibilityTracker(
    private val bufferFactor: Float = 0.5f, // 0.5 = 1.5× viewport (half a viewport on each side)
    private val exitDebounceMs: Long = 500L
) {
    companion object {
        private const val TAG = "SectionVisTracker"
    }

    private val _visibleSections = MutableStateFlow<Set<String>>(emptySet())

    /**
     * Flow of section IDs that are currently near the viewport.
     * Exit events are debounced; entry is immediate.
     */
    val visibleSections: StateFlow<Set<String>> = _visibleSections.asStateFlow()

    /**
     * Check if a section is currently near the viewport.
     */
    fun isNearViewport(sectionId: String): Boolean =
        sectionId in _visibleSections.value

    /**
     * Suspend until the given section enters the near-viewport zone.
     * Returns immediately if already visible.
     */
    suspend fun awaitNearViewport(sectionId: String) {
        if (isNearViewport(sectionId)) return
        _visibleSections.first { sectionId in it }
    }

    /**
     * Call from a Composable to observe [LazyListState] and update visibility.
     * The [sectionIds] list must be in the same order as the LazyColumn items.
     */
    @OptIn(FlowPreview::class)
    @Composable
    fun Observe(lazyListState: LazyListState, sectionIds: List<String>) {
        val layoutInfo by remember { derivedStateOf { lazyListState.layoutInfo } }

        LaunchedEffect(layoutInfo) {
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || sectionIds.isEmpty()) return@LaunchedEffect

            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val viewportSize = viewportEnd - viewportStart
            val buffer = (viewportSize * bufferFactor).toInt()

            val bufferedStart = viewportStart - buffer
            val bufferedEnd = viewportEnd + buffer

            // Determine which item indices fall within the buffered viewport
            val nearIndices = mutableSetOf<Int>()
            for (item in visibleItems) {
                val itemEnd = item.offset + item.size
                if (itemEnd >= bufferedStart && item.offset <= bufferedEnd) {
                    nearIndices.add(item.index)
                }
            }

            // Also include items just outside visible range but within buffer
            val firstVisible = visibleItems.firstOrNull()?.index ?: 0
            val lastVisible = visibleItems.lastOrNull()?.index ?: 0
            // Estimate items in buffer zone (approximation: 1 item per viewport fraction)
            val bufferItems = ((bufferFactor * visibleItems.size).toInt()).coerceAtLeast(1)
            for (i in (firstVisible - bufferItems).coerceAtLeast(0)..firstVisible) {
                nearIndices.add(i)
            }
            for (i in lastVisible..(lastVisible + bufferItems).coerceAtMost(sectionIds.size - 1)) {
                nearIndices.add(i)
            }

            val nearSectionIds = nearIndices
                .filter { it in sectionIds.indices }
                .map { sectionIds[it] }
                .toSet()

            val current = _visibleSections.value
            // Immediate entry: add new sections right away
            val entering = nearSectionIds - current
            if (entering.isNotEmpty()) {
                _visibleSections.value = current + entering
                Log.d(TAG, "Sections entered viewport: $entering")
            }

            // Debounced exit: sections that left but might come back
            val exiting = current - nearSectionIds
            if (exiting.isNotEmpty()) {
                kotlinx.coroutines.delay(exitDebounceMs)
                // Re-check: only remove if still not near after debounce
                val stillNear = _visibleSections.value
                val confirmedExits = exiting.filter { it !in nearSectionIds }
                if (confirmedExits.isNotEmpty()) {
                    _visibleSections.value = stillNear - confirmedExits.toSet()
                    Log.d(TAG, "Sections exited viewport: $confirmedExits")
                }
            }
        }
    }
}
