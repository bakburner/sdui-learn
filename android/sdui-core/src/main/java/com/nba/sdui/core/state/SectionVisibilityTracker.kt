package com.nba.sdui.core.state

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Tracks which sections are near the viewport in a LazyColumn.
 *
 * Uses [LazyListState.layoutInfo] to determine visible items plus a
 * configurable buffer (default 1.5× viewport lookahead). Section exit
 * is debounced by [exitDebounceMs] to absorb scroll bounce.
 *
 * Consumers read [isNearViewport] to gate polling / SSE processing.
 */
@OptIn(FlowPreview::class)
class SectionVisibilityTracker(
    private val bufferFactor: Float = 0.5f, // 0.5 = 1.5× viewport (half a viewport on each side)
    private val exitDebounceMs: Long = 500L
) {
    companion object {
        private const val TAG = "SectionVisTracker"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _visibleSections = MutableStateFlow<Set<String>>(emptySet())

    private val nearSnapshots = MutableSharedFlow<Set<String>>(extraBufferCapacity = 32)

    init {
        nearSnapshots
            .onEach { near ->
                val current = _visibleSections.value
                val entering = near - current
                if (entering.isNotEmpty()) {
                    _visibleSections.value = current + entering
                    Log.d(TAG, "Sections entered viewport: $entering")
                }
            }
            .launchIn(scope)
        nearSnapshots
            .debounce(exitDebounceMs)
            .onEach { near ->
                val current = _visibleSections.value
                val exiting = current - near
                if (exiting.isNotEmpty()) {
                    _visibleSections.value = current - exiting
                    Log.d(TAG, "Sections exited viewport: $exiting")
                }
            }
            .launchIn(scope)
    }

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
    @Composable
    fun Observe(lazyListState: LazyListState, sectionIds: List<String>) {
        val layoutInfo by remember { derivedStateOf { lazyListState.layoutInfo } }

        LaunchedEffect(layoutInfo, sectionIds, bufferFactor) {
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || sectionIds.isEmpty()) return@LaunchedEffect

            val viewportStart = layoutInfo.viewportStartOffset
            val viewportEnd = layoutInfo.viewportEndOffset
            val viewportSize = viewportEnd - viewportStart
            val buffer = (viewportSize * bufferFactor).toInt()

            val bufferedStart = viewportStart - buffer
            val bufferedEnd = viewportEnd + buffer

            val nearIndices = mutableSetOf<Int>()
            for (item in visibleItems) {
                val itemEnd = item.offset + item.size
                if (itemEnd >= bufferedStart && item.offset <= bufferedEnd) {
                    nearIndices.add(item.index)
                }
            }

            val firstVisible = visibleItems.firstOrNull()?.index ?: 0
            val lastVisible = visibleItems.lastOrNull()?.index ?: 0
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

            nearSnapshots.tryEmit(nearSectionIds)
        }
    }
}
