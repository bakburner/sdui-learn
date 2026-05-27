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
        assertNull(model?.expandedAction)
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

    @Test
    fun `mapCalendarMonthList maps all fields`() {
        val section = decodeSection(
            """
            {
              "id": "server:calendar~type=CalendarMonthList",
              "type": "CalendarMonthList",
              "data": {
                "stateKey": "games_selected_date",
                "selectedDate": "2026-05-25",
                "defaultDate": "2026-05-26",
                "minDate": "2025-10-01",
                "maxDate": "2026-06-30",
                "dateMetadata": {
                  "2026-05-25": { "gameCount": 2, "hasTeamGame": true },
                  "2026-05-26": { "gameCount": 0, "hasTeamGame": false }
                },
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "navigate",
                  "uri": "nba://games?date={{games_selected_date}}"
                }
              }
            }
            """.trimIndent()
        )

        val model = mapCalendarMonthList(section)
        assertNotNull(model)
        assertEquals("games_selected_date", model?.stateKey)
        assertEquals("2026-05-25", model?.selectedDate)
        assertEquals("2026-05-26", model?.defaultDate)
        assertEquals("2025-10-01", model?.minDate)
        assertEquals("2026-06-30", model?.maxDate)
        assertEquals(2, model?.dateMetadata?.get("2026-05-25")?.gameCount)
        assertEquals(true, model?.dateMetadata?.get("2026-05-25")?.hasTeamGame)
        assertEquals("navigate", model?.onDateSelected?.type)
        assertEquals("nba://games?date={{games_selected_date}}", model?.onDateSelected?.targetUri)
    }

    @Test
    fun `mapCalendarMonthList returns null when required field missing`() {
        val malformed = decodeSection(
            """
            {
              "id": "server:calendar~type=CalendarMonthList",
              "type": "CalendarMonthList",
              "data": {
                "selectedDate": "2026-05-25",
                "defaultDate": "2026-05-26",
                "minDate": "2025-10-01",
                "maxDate": "2026-06-30",
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "navigate",
                  "uri": "nba://games?date={{games_selected_date}}"
                }
              }
            }
            """.trimIndent()
        )

        assertNull(mapCalendarMonthList(malformed))
    }

    private fun decodeSection(json: String): Section = mapper.readValue(json)
}
