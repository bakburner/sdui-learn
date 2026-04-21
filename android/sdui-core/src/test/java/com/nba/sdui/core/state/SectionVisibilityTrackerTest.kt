package com.nba.sdui.core.state

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SectionVisibilityTracker] — validates the flow-based
 * visibility logic independent of Compose (the [Observe] composable is
 * tested via instrumentation tests).
 *
 * These tests exercise [visibleSections], [isNearViewport], and
 * [awaitNearViewport] by directly manipulating the backing StateFlow
 * through a test-only helper.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SectionVisibilityTrackerTest {

    private lateinit var tracker: SectionVisibilityTracker

    @BeforeEach
    fun setup() {
        tracker = SectionVisibilityTracker(
            bufferFactor = 0.5f,
            exitDebounceMs = 500L
        )
    }

    @Test
    fun `initially no sections are visible`() {
        assertTrue(tracker.visibleSections.value.isEmpty())
        assertFalse(tracker.isNearViewport("section-1"))
    }

    @Test
    fun `awaitNearViewport returns immediately if section already visible`() = runTest {
        // Simulate section entering viewport by updating the flow externally
        simulateVisibilityUpdate(tracker, setOf("section-1"))

        // Should return immediately — no suspension
        val result = async { tracker.awaitNearViewport("section-1") }
        result.await() // would hang if not immediately visible
        assertTrue(tracker.isNearViewport("section-1"))
    }

    @Test
    fun `awaitNearViewport suspends until section enters viewport`() = runTest {
        var completed = false

        val job = launch {
            tracker.awaitNearViewport("section-2")
            completed = true
        }

        // Not yet visible — coroutine should still be suspended
        assertFalse(completed)

        // Simulate section entering
        simulateVisibilityUpdate(tracker, setOf("section-2"))

        // Now it should complete
        job.join()
        assertTrue(completed)
    }

    @Test
    fun `isNearViewport reflects current visibility`() = runTest {
        assertFalse(tracker.isNearViewport("section-1"))

        simulateVisibilityUpdate(tracker, setOf("section-1", "section-2"))
        assertTrue(tracker.isNearViewport("section-1"))
        assertTrue(tracker.isNearViewport("section-2"))
        assertFalse(tracker.isNearViewport("section-3"))

        simulateVisibilityUpdate(tracker, setOf("section-2"))
        assertFalse(tracker.isNearViewport("section-1"))
        assertTrue(tracker.isNearViewport("section-2"))
    }

    @Test
    fun `visibleSections flow emits updates`() = runTest {
        val collected = mutableListOf<Set<String>>()
        val job = launch {
            tracker.visibleSections.collect { collected.add(it) }
        }

        simulateVisibilityUpdate(tracker, setOf("a"))
        simulateVisibilityUpdate(tracker, setOf("a", "b"))
        simulateVisibilityUpdate(tracker, setOf("b"))

        job.cancel()

        // Initial empty + 3 updates
        assertTrue(collected.size >= 3)
        assertTrue(collected.last().contains("b"))
        assertFalse(collected.last().contains("a"))
    }

    /**
     * Test-only helper to push visibility state directly into the tracker's
     * backing flow, bypassing the Compose [Observe] composable.
     *
     * Uses reflection to set the internal [_visibleSections] MutableStateFlow.
     */
    private fun simulateVisibilityUpdate(tracker: SectionVisibilityTracker, sections: Set<String>) {
        val field = SectionVisibilityTracker::class.java.getDeclaredField("_visibleSections")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(tracker) as kotlinx.coroutines.flow.MutableStateFlow<Set<String>>
        flow.value = sections
    }
}
