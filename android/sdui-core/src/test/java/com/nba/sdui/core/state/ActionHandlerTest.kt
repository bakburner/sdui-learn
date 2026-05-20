package com.nba.sdui.core.state

import android.util.Log
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.nba.sdui.core.models.generated.Action
import com.nba.sdui.core.models.generated.mapper
import com.nba.sdui.core.models.generated.MutateOperation
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ActionHandlerTest {

    private lateinit var actionHandler: ActionHandler
    private lateinit var stateManager: StateManager

    @Before
    fun setUp() {
        actionHandler = ActionHandler()
        stateManager = StateManager()
        SduiActionLogger.enabled = false

        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.isLoggable(any(), any()) } returns false
    }

    @After
    fun tearDown() {
        SduiActionLogger.enabled = true
        unmockkStatic(Log::class)
    }

    @Test
    fun `navigate uses webUrl when it is the only target`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "navigate",
                webUrl = "https://nba.example.com/game/1"
            ),
            stateManager
        )

        assertEquals(
            ActionHandler.ActionResult.NavigateResult(
                uri = "https://nba.example.com/game/1",
                presentation = null,
                modalHeight = null
            ),
            result
        )
    }

    @Test
    fun `navigate uses targetUri when it is the only target`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "navigate",
                targetUri = "nba://game/1"
            ),
            stateManager
        )

        assertEquals(
            ActionHandler.ActionResult.NavigateResult(
                uri = "nba://game/1",
                presentation = null,
                modalHeight = null
            ),
            result
        )
    }

    @Test
    fun `navigate prefers webUrl when both targets are present and targetUri is not native`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "navigate",
                targetUri = "https://native.example.com/game/1",
                webUrl = "https://web.example.com/game/1"
            ),
            stateManager
        )

        assertEquals(
            ActionHandler.ActionResult.NavigateResult(
                uri = "https://web.example.com/game/1",
                presentation = null,
                modalHeight = null
            ),
            result
        )
    }

    @Test
    fun `navigate prefers nba targetUri when both targets are present`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "navigate",
                targetUri = "nba://game/1",
                webUrl = "https://web.example.com/game/1"
            ),
            stateManager
        )

        assertEquals(
            ActionHandler.ActionResult.NavigateResult(
                uri = "nba://game/1",
                presentation = null,
                modalHeight = null
            ),
            result
        )
    }

    @Test
    fun `legacy fallbackUrl action json is rejected by generated decoder`() {
        val json = """
            {
              "trigger": "onActivate",
              "type": "navigate",
              "fallbackUrl": "https://nba.example.com/game/1"
            }
        """.trimIndent()

        try {
            mapper.readValue(json, Action::class.java)
            fail("Expected legacy fallbackUrl to be rejected")
        } catch (error: UnrecognizedPropertyException) {
            assertEquals("fallbackUrl", error.propertyName)
        }
    }

    @Test
    fun `mutate set uses target and value`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "overview"
            ),
            stateManager
        )

        assertEquals(ActionHandler.ActionResult.MutateResult("selectedTab", "overview"), result)
        assertEquals("overview", stateManager.getState("selectedTab"))
    }

    @Test
    fun `mutate null operation behaves as set`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "stats",
                operation = null
            ),
            stateManager
        )

        assertEquals(ActionHandler.ActionResult.MutateResult("selectedTab", "stats"), result)
        assertEquals("stats", stateManager.getState("selectedTab"))
    }

    @Test
    fun `mutate toggle applies operation`() {
        stateManager.setState("expanded", true)

        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "expanded",
                operation = MutateOperation.Toggle
            ),
            stateManager
        )

        assertEquals(ActionHandler.ActionResult.MutateResult("expanded", false), result)
        assertEquals(false, stateManager.getState("expanded"))
    }

    @Test
    fun `mutate increment applies operation`() {
        stateManager.setState("count", 2)

        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "count",
                value = "2",
                operation = MutateOperation.Increment
            ),
            stateManager
        )

        assertEquals(ActionHandler.ActionResult.MutateResult("count", 4), result)
        assertEquals(4, stateManager.getState("count"))
    }

    @Test
    fun `mutate append applies operation`() {
        stateManager.setState("filters", listOf("live"))

        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "filters",
                value = "featured",
                operation = MutateOperation.Append
            ),
            stateManager
        )

        assertEquals(
            ActionHandler.ActionResult.MutateResult("filters", listOf("live", "featured")),
            result
        )
        assertEquals(listOf("live", "featured"), stateManager.getState("filters"))
    }

    @Test
    fun `refresh reads target for simple refresh`() {
        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "refresh",
                target = "hero"
            ),
            stateManager
        )

        assertEquals(ActionHandler.ActionResult.RefreshResult("hero"), result)
    }

    @Test
    fun `refresh resolves param bindings from state`() {
        stateManager.setState("gameId", 42)
        stateManager.setState("tab", "overview")

        val result = actionHandler.handle(
            SduiAction(
                trigger = "onActivate",
                type = "refresh",
                target = "boxscore",
                endpoint = "/sdui/game-detail",
                paramBindings = mapOf(
                    "gameId" to "{{gameId}}",
                    "tab" to "{{tab}}",
                    "empty" to "{{missing}}"
                )
            ),
            stateManager
        )

        assertEquals(
            ActionHandler.ActionResult.ParameterizedRefreshResult(
                endpoint = "/sdui/game-detail",
                target = "boxscore",
                params = mapOf("gameId" to "42", "tab" to "overview")
            ),
            result
        )
    }

    @Test
    fun `resolveFailurePolicy applies per type defaults when onFailure is absent`() {
        assertEquals(
            FailurePolicy.HALT,
            actionHandler.resolveFailurePolicy(SduiAction(trigger = "onActivate", type = "navigate"))
        )
        assertEquals(
            FailurePolicy.SILENT,
            actionHandler.resolveFailurePolicy(SduiAction(trigger = "onActivate", type = "fireAndForget"))
        )
        assertEquals(
            FailurePolicy.CONTINUE,
            actionHandler.resolveFailurePolicy(SduiAction(trigger = "onActivate", type = "mutate"))
        )
        assertEquals(
            FailurePolicy.CONTINUE,
            actionHandler.resolveFailurePolicy(SduiAction(trigger = "onActivate", type = "refresh"))
        )
        assertEquals(
            FailurePolicy.SILENT,
            actionHandler.resolveFailurePolicy(SduiAction(trigger = "onActivate", type = "dismiss"))
        )
        assertEquals(
            FailurePolicy.SILENT,
            actionHandler.resolveFailurePolicy(SduiAction(trigger = "onActivate", type = "toast"))
        )
    }

    @Test
    fun `executeSequence preserves declared action order across successful mutations`() {
        val sequence = listOf(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "overview"
            ),
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "stats"
            )
        )

        val result = actionHandler.executeSequence(sequence, stateManager)

        assertFalse(result.halted)
        assertEquals(sequence.map { ActionHandler.ActionResult.MutateResult("selectedTab", it.value) }, result.results)
        assertEquals("stats", stateManager.getState("selectedTab"))
    }

    @Test
    fun `executeSequence continues after mutate failure by default`() {
        val sequence = listOf(
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = null,
                value = true
            ),
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "stats"
            )
        )

        val result = actionHandler.executeSequence(sequence, stateManager)

        assertFalse(result.halted)
        assertEquals(2, result.results.size)
        assertEquals(
            ActionHandler.ActionResult.MutateNoOp("", "No target specified"),
            result.results.first()
        )
        assertEquals("stats", stateManager.getState("selectedTab"))
    }

    @Test
    fun `navigate success halts sequence before later actions`() {
        val sequence = listOf(
            SduiAction(
                trigger = "onActivate",
                type = "navigate",
                webUrl = "https://nba.example.com/game/1"
            ),
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "stats"
            )
        )

        val result = actionHandler.executeSequence(sequence, stateManager)

        assertTrue(result.halted)
        assertEquals(1, result.results.size)
        assertFalse(stateManager.state.value.containsKey("selectedTab"))
    }

    @Test
    fun `navigate failure halts onActivate sequence before later actions`() {
        val sequence = listOf(
            SduiAction(trigger = "onActivate", type = "navigate"),
            SduiAction(
                trigger = "onActivate",
                type = "mutate",
                target = "selectedTab",
                value = "stats"
            )
        )

        val result = actionHandler.executeSequence(sequence, stateManager)

        assertTrue(result.halted)
        assertEquals(1, result.results.size)
        assertEquals(sequence.first(), result.failedAction)
        assertFalse(stateManager.state.value.containsKey("selectedTab"))
    }
}