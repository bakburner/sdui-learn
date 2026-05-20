package com.nba.sdui.core.state

import android.util.Log
import com.nba.sdui.core.models.generated.MutateOperation
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class StateManagerTest {

    private lateinit var stateManager: StateManager

    @Before
    fun setUp() {
        stateManager = StateManager()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `set assigns and null removes`() {
        stateManager.applyOperation(MutateOperation.Set, "selectedTab", "overview")
        assertEquals("overview", stateManager.getState("selectedTab"))

        stateManager.applyOperation(MutateOperation.Set, "selectedTab", null)
        assertNull(stateManager.getState("selectedTab"))
    }

    @Test
    fun `toggle flips boolean without warning`() {
        stateManager.setState("expanded", true)

        stateManager.applyOperation(MutateOperation.Toggle, "expanded", null)

        assertEquals(false, stateManager.getState("expanded"))
        verify(exactly = 0) { Log.w(any(), any<String>()) }
    }

    @Test
    fun `toggle non boolean no ops and warns`() {
        stateManager.setState("expanded", "yes")

        stateManager.applyOperation(MutateOperation.Toggle, "expanded", null)

        assertEquals("yes", stateManager.getState("expanded"))
        verify(exactly = 1) { Log.w("StateManager", match { it.contains("not boolean") }) }
    }

    @Test
    fun `toggle missing current value no ops and warns`() {
        stateManager.applyOperation(MutateOperation.Toggle, "expanded", null)

        assertNull(stateManager.getState("expanded"))
        verify(exactly = 1) { Log.w("StateManager", match { it.contains("not boolean") }) }
    }

    @Test
    fun `increment preserves int and double results`() {
        stateManager.setState("count", 2)
        stateManager.setState("ratio", 1.5)
        stateManager.setState("promoted", 2)
        stateManager.setState("boolDelta", 2)
        stateManager.setState("stringDelta", 2)

        stateManager.applyOperation(MutateOperation.Increment, "count", null)
        stateManager.applyOperation(MutateOperation.Increment, "ratio", 2.25)
        stateManager.applyOperation(MutateOperation.Increment, "promoted", 0.5)
        stateManager.applyOperation(MutateOperation.Increment, "boolDelta", true)
        stateManager.applyOperation(MutateOperation.Increment, "stringDelta", "1.5")

        assertEquals(3, stateManager.getState("count"))
        assertEquals(3.75, stateManager.getState("ratio"))
        assertEquals(2.5, stateManager.getState("promoted"))
        assertEquals(3, stateManager.getState("boolDelta"))
        assertEquals(3.5, stateManager.getState("stringDelta"))
        verify(exactly = 0) { Log.w(any(), any<String>()) }
    }

    @Test
    fun `increment non numeric or missing no ops and warn`() {
        stateManager.setState("count", "two")
        stateManager.setState("flag", true)

        stateManager.applyOperation(MutateOperation.Increment, "count", 3)
        stateManager.applyOperation(MutateOperation.Increment, "flag", 3)
        stateManager.applyOperation(MutateOperation.Increment, "missing", null)

        assertEquals("two", stateManager.getState("count"))
        assertEquals(true, stateManager.getState("flag"))
        assertNull(stateManager.getState("missing"))
        verify(exactly = 3) { Log.w("StateManager", match { it.contains("current value is not numeric") }) }
    }

    @Test
    fun `append handles lists strings and missing keys`() {
        stateManager.setState("filters", listOf("live"))
        stateManager.setState("query", "NBA")

        stateManager.applyOperation(MutateOperation.Append, "filters", "final")
        stateManager.applyOperation(MutateOperation.Append, "query", " Finals")
        stateManager.applyOperation(MutateOperation.Append, "newFilters", "featured")

        assertEquals(listOf("live", "final"), stateManager.getState("filters"))
        assertEquals("NBA Finals", stateManager.getState("query"))
        assertEquals(listOf("featured"), stateManager.getState("newFilters"))
        verify(exactly = 0) { Log.w(any(), any<String>()) }
    }

    @Test
    fun `append missing current with null value no ops and warns`() {
        stateManager.applyOperation(MutateOperation.Append, "filters", null)

        assertNull(stateManager.getState("filters"))
        verify(exactly = 1) { Log.w("StateManager", match { it.contains("incompatible value types") }) }
    }

    @Test
    fun `append incompatible types no op and warns`() {
        stateManager.setState("filters", "featured")

        stateManager.applyOperation(MutateOperation.Append, "filters", 2)

        assertEquals("featured", stateManager.getState("filters"))
        verify(exactly = 1) { Log.w("StateManager", match { it.contains("incompatible value types") }) }
    }
}