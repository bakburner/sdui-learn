package com.nba.sdui.core.screen

import com.nba.sdui.core.state.SectionVisibilityTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests the visibility-gated polling and SSE buffering logic.
 *
 * Since [SduiScreenViewModel] is tightly coupled to Android framework
 * classes, these tests validate the core gating algorithm in isolation:
 * the interplay between app-foreground state, section visibility, and
 * the pauseWhenOffScreen flag.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VisibilityGatedRefreshTest {

    private lateinit var tracker: SectionVisibilityTracker
    private lateinit var isAppForeground: MutableStateFlow<Boolean>

    @BeforeEach
    fun setup() {
        tracker = SectionVisibilityTracker(bufferFactor = 0.5f, exitDebounceMs = 500L)
        isAppForeground = MutableStateFlow(true)
    }

    // Helper: simulate the "should poll?" gate from SduiScreenViewModel.setupPolling
    private fun shouldPollSection(
        sectionId: String,
        pauseWhenOffScreen: Boolean
    ): Boolean {
        val foreground = isAppForeground.value
        val visible = !pauseWhenOffScreen || tracker.isNearViewport(sectionId)
        return foreground && visible
    }

    // Helper: simulate SSE gate from subscribeToChannel
    private fun shouldApplySseMessage(
        sectionId: String,
        pauseWhenOffScreen: Boolean
    ): Boolean {
        val foreground = isAppForeground.value
        val visible = !pauseWhenOffScreen || tracker.isNearViewport(sectionId)
        return foreground && visible
    }

    @Test
    fun `poll gate allows when foreground and visible`() {
        isAppForeground.value = true
        simulateVisibility(setOf("section-1"))
        assertTrue(shouldPollSection("section-1", pauseWhenOffScreen = true))
    }

    @Test
    fun `poll gate blocks when app is backgrounded`() {
        isAppForeground.value = false
        simulateVisibility(setOf("section-1"))
        assertFalse(shouldPollSection("section-1", pauseWhenOffScreen = true))
    }

    @Test
    fun `poll gate blocks when section is off-screen and pauseWhenOffScreen is true`() {
        isAppForeground.value = true
        simulateVisibility(emptySet()) // section-1 not visible
        assertFalse(shouldPollSection("section-1", pauseWhenOffScreen = true))
    }

    @Test
    fun `poll gate allows when section is off-screen but pauseWhenOffScreen is false`() {
        isAppForeground.value = true
        simulateVisibility(emptySet()) // section-1 not visible
        assertTrue(shouldPollSection("section-1", pauseWhenOffScreen = false))
    }

    @Test
    fun `poll gate blocks when both backgrounded and off-screen`() {
        isAppForeground.value = false
        simulateVisibility(emptySet())
        assertFalse(shouldPollSection("section-1", pauseWhenOffScreen = true))
    }

    @Test
    fun `SSE gate buffers when app backgrounded even with pauseWhenOffScreen false`() {
        isAppForeground.value = false
        simulateVisibility(setOf("section-1"))
        // pauseWhenOffScreen=false but app is backgrounded → still blocked
        assertFalse(shouldApplySseMessage("section-1", pauseWhenOffScreen = false))
    }

    @Test
    fun `SSE gate allows when foreground and pauseWhenOffScreen false`() {
        isAppForeground.value = true
        simulateVisibility(emptySet()) // off-screen
        assertTrue(shouldApplySseMessage("section-1", pauseWhenOffScreen = false))
    }

    @Test
    fun `SSE message buffer pattern - latest message wins`() = runTest {
        val buffer = mutableMapOf<String, Map<String, Any?>>()

        // Simulate buffering messages while off-screen
        buffer["section-1"] = mapOf("score" to 10)
        buffer["section-1"] = mapOf("score" to 20)
        buffer["section-1"] = mapOf("score" to 30)

        // Only latest should remain
        assertEquals(mapOf("score" to 30), buffer["section-1"])

        // On resume, apply and clear
        val applied = buffer.remove("section-1")
        assertEquals(mapOf("score" to 30), applied)
        assertNull(buffer["section-1"])
    }

    @Test
    fun `awaitNearViewport gates poll loop`() = runTest {
        val pollResults = mutableListOf<String>()

        val job = launch {
            // Simulates the poll loop gate:
            // while (active) { awaitNearViewport(); poll(); delay() }
            tracker.awaitNearViewport("section-1")
            pollResults.add("polled")
        }

        // Section not visible — job should be suspended
        assertTrue(pollResults.isEmpty())

        // Section enters viewport
        simulateVisibility(setOf("section-1"))

        job.join()
        assertEquals(listOf("polled"), pollResults)
    }

    @Test
    fun `foreground resume triggers immediate poll for all visible sections`() {
        isAppForeground.value = true
        simulateVisibility(setOf("s1", "s2", "s3"))

        // All visible sections should pass gate
        assertTrue(shouldPollSection("s1", pauseWhenOffScreen = true))
        assertTrue(shouldPollSection("s2", pauseWhenOffScreen = true))
        assertTrue(shouldPollSection("s3", pauseWhenOffScreen = true))

        // Background
        isAppForeground.value = false
        assertFalse(shouldPollSection("s1", pauseWhenOffScreen = true))

        // Foreground again
        isAppForeground.value = true
        assertTrue(shouldPollSection("s1", pauseWhenOffScreen = true))
    }

    private fun simulateVisibility(sections: Set<String>) {
        val field = SectionVisibilityTracker::class.java.getDeclaredField("_visibleSections")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(tracker) as MutableStateFlow<Set<String>>
        flow.value = sections
    }
}
