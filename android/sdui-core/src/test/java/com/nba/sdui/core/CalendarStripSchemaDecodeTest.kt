package com.nba.sdui.core

import com.fasterxml.jackson.module.kotlin.readValue
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.models.generated.mapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File

class CalendarStripSchemaDecodeTest {

    @Test
    fun `calendar-strip fixture decodes with expected fields`() {
        val section: Section = mapper.readValue(loadExample("calendar-strip.json"))

        assertEquals("server:games-calendar~type=CalendarStrip", section.id)
        assertEquals("CalendarStrip", section.type)
        assertEquals("server:games-calendar", section.contentSourceID)
        assertEquals("games_calendar_strip", section.analyticsID)
        assertEquals("Games date picker", section.accessibility?.label)

        val data = section.data
        assertNotNull(data)
        assertEquals("games_selected_date", data?.stateKey)
        assertEquals("2026-05-25", data?.selectedDate)
        assertEquals("2026-05-25", data?.defaultDate)
        assertEquals("2025-10-01", data?.minDate)
        assertEquals("2026-06-30", data?.maxDate)
        assertEquals("onActivate", data?.onDateSelected?.trigger?.value)
        assertEquals("refresh", data?.onDateSelected?.type?.value)
        assertEquals("/v1/sdui/screen/games", data?.onDateSelected?.endpoint)
        assertEquals("{{games_selected_date}}", data?.onDateSelected?.paramBindings?.get("date"))
    }

    @Test
    fun `calendar-strip decode fails when defaultDate missing`() {
        val invalid = """
            {
              "id": "server:games-calendar~type=CalendarStrip",
              "type": "CalendarStrip",
              "data": {
                "stateKey": "games_selected_date",
                "selectedDate": "2026-05-25",
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "refresh",
                  "endpoint": "/v1/sdui/screen/games",
                  "paramBindings": { "date": "{{games_selected_date}}" }
                }
              }
            }
        """.trimIndent()

        assertThrows(Exception::class.java) {
            mapper.readValue<Section>(invalid)
        }
    }

    @Test
    fun `calendar-strip decode succeeds when min and max omitted`() {
        val payload = """
            {
              "id": "server:games-calendar~type=CalendarStrip",
              "type": "CalendarStrip",
              "data": {
                "stateKey": "games_selected_date",
                "selectedDate": "2026-05-25",
                "defaultDate": "2026-05-25",
                "onDateSelected": {
                  "trigger": "onActivate",
                  "type": "refresh",
                  "endpoint": "/v1/sdui/screen/games",
                  "paramBindings": { "date": "{{games_selected_date}}" }
                }
              }
            }
        """.trimIndent()

        val section = mapper.readValue<Section>(payload)
        assertNull(section.data?.minDate)
        assertNull(section.data?.maxDate)
    }

    @Test
    fun `calendar-strip decode fails when onDateSelected is array`() {
        val invalid = """
            {
              "id": "server:games-calendar~type=CalendarStrip",
              "type": "CalendarStrip",
              "data": {
                "stateKey": "games_selected_date",
                "selectedDate": "2026-05-25",
                "defaultDate": "2026-05-25",
                "onDateSelected": [
                  {
                    "trigger": "onActivate",
                    "type": "refresh",
                    "endpoint": "/v1/sdui/screen/games",
                    "paramBindings": { "date": "{{games_selected_date}}" }
                  }
                ]
              }
            }
        """.trimIndent()

        assertThrows(Exception::class.java) {
            mapper.readValue<Section>(invalid)
        }
    }

    private fun loadExample(fileName: String): String {
        var current = File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = File(current, "schema/examples/$fileName")
            if (candidate.isFile) {
                return candidate.readText()
            }
            current = current.parentFile ?: return@repeat
        }
        error("schema/examples/$fileName not found from ${System.getProperty("user.dir")}")
    }
}
