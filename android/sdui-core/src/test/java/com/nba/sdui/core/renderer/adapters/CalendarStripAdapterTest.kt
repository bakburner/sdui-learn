package com.nba.sdui.core.renderer.adapters

import com.fasterxml.jackson.module.kotlin.readValue
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.models.generated.mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CalendarStripAdapterTest {

    @Test
    fun `mapCalendarStrip maps all fields`() {
        val section = decodeSection(
            """
            {
              "id": "server:games-calendar~type=CalendarStrip",
              "type": "CalendarStrip",
              "data": {
                "stateKey": "games_selected_date",
                "selectedDate": "2026-05-25",
                "defaultDate": "2026-05-26",
                "minDate": "2025-10-01",
                "maxDate": "2026-06-30",
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "refresh",
                  "endpoint": "/v1/sdui/screen/games",
                  "paramBindings": { "date": "{{games_selected_date}}" }
                }
              }
            }
            """.trimIndent()
        )

        val model = mapCalendarStrip(section, screenState = emptyMap())
        assertNotNull(model)
        assertEquals("games_selected_date", model?.stateKey)
        assertEquals("2026-05-25", model?.selectedDate)
        assertEquals("2026-05-26", model?.defaultDate)
        assertEquals("2025-10-01", model?.minDate)
        assertEquals("2026-06-30", model?.maxDate)
        assertEquals("onActivate", model?.onDateSelected?.trigger)
        assertEquals("refresh", model?.onDateSelected?.type)
        assertEquals("/v1/sdui/screen/games", model?.onDateSelected?.endpoint)
    }

    @Test
    fun `mapCalendarStrip returns null when required field missing`() {
        val malformed = decodeSection(
            """
            {
              "id": "server:games-calendar~type=CalendarStrip",
              "type": "CalendarStrip",
              "data": {
                "stateKey": "games_selected_date",
                "defaultDate": "2026-05-26",
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "refresh",
                  "endpoint": "/v1/sdui/screen/games"
                }
              }
            }
            """.trimIndent()
        )

        assertNull(mapCalendarStrip(malformed, screenState = emptyMap()))
    }

    private fun decodeSection(json: String): Section = mapper.readValue(json)
}
