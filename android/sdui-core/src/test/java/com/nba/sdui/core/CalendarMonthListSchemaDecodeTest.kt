package com.nba.sdui.core

import com.fasterxml.jackson.module.kotlin.readValue
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.models.generated.mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CalendarMonthListSchemaDecodeTest {

    @Test
    fun `calendar-month-list section decodes and round-trips`() {
        val payload = """
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
                  "2026-05-25": { "gameCount": 3, "hasTeamGame": true },
                  "2026-05-26": { "gameCount": 1, "hasTeamGame": false }
                },
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "navigate",
                  "uri": "nba://games?date={{games_selected_date}}"
                }
              }
            }
        """.trimIndent()

        val section: Section = mapper.readValue(payload)
        assertEquals("CalendarMonthList", section.type)
        assertEquals("games_selected_date", section.data?.stateKey)
        assertEquals(3L, section.data?.dateMetadata?.get("2026-05-25")?.gameCount)
        assertEquals(true, section.data?.dateMetadata?.get("2026-05-25")?.hasTeamGame)
        assertEquals("navigate", section.data?.onDateSelected?.type?.value)
        assertEquals("nba://games?date={{games_selected_date}}", section.data?.onDateSelected?.targetURI)

        val encoded = mapper.writeValueAsString(section)
        val roundTripped: Section = mapper.readValue(encoded)
        assertNotNull(roundTripped.data?.dateMetadata)
        assertEquals(2, roundTripped.data?.dateMetadata?.size)
        assertEquals(1L, roundTripped.data?.dateMetadata?.get("2026-05-26")?.gameCount)
    }
}
