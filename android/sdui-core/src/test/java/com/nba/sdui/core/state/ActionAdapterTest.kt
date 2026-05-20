package com.nba.sdui.core.state

import com.nba.sdui.core.models.generated.Action
import com.nba.sdui.core.models.generated.ActionTrigger
import com.nba.sdui.core.models.generated.ActionType
import com.nba.sdui.core.models.generated.MutateOperation
import com.nba.sdui.core.renderer.adapters.toSduiAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionAdapterTest {

    @Test
    fun `adapter preserves canonical action fields`() {
        val action = Action(
            trigger = ActionTrigger.OnActivate,
            type = ActionType.Mutate,
            target = "selectedTab",
            value = "overview",
            operation = MutateOperation.Set,
            webURL = "https://nba.example.com/game/1"
        )

        val mapped = action.toSduiAction()
        val fieldNames = SduiAction::class.java.declaredFields.map { it.name }.toSet()

        assertEquals("selectedTab", mapped.target)
        assertEquals("overview", mapped.value)
        assertEquals("https://nba.example.com/game/1", mapped.webUrl)
        assertEquals(MutateOperation.Set, mapped.operation)
        assertTrue(fieldNames.containsAll(setOf("target", "value", "webUrl")))
    }
}