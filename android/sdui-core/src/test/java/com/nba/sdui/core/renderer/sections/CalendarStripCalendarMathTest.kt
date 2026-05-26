package com.nba.sdui.core.renderer.sections

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.TimeZone

class CalendarStripCalendarMathTest {

    @Test
    fun `weeksBetween handles month boundary`() {
        val anchor = LocalDate.parse("2026-01-28")
        val target = LocalDate.parse("2026-02-03")

        val weeks = weeksBetween(anchor, target, DayOfWeek.MONDAY)

        assertEquals(2, weeks)
    }

    @Test
    fun `weekIndexOf returns 0-based page index for selected date`() {
        // Reproduces the calendar-strip off-by-one: with US (Sunday) firstDayOfWeek,
        // the week containing 2026-05-26 (Tue) is May 24-30, which is 34 weeks
        // after the Sep 28 2025 anchor week — page index 34, not 35.
        val anchor = LocalDate.parse("2025-09-28") // Sunday
        val selected = LocalDate.parse("2026-05-26") // Tuesday

        assertEquals(34, weekIndexOf(anchor, selected, DayOfWeek.SUNDAY))
        // weeksBetween is a 1-based count for the same range
        assertEquals(35, weeksBetween(anchor, selected, DayOfWeek.SUNDAY))
    }

    @Test
    fun `weekIndexOf returns 0 for same-week target`() {
        val sameWeekAnchor = LocalDate.parse("2026-05-24") // Sunday
        val sameWeekTarget = LocalDate.parse("2026-05-26") // Tuesday

        assertEquals(0, weekIndexOf(sameWeekAnchor, sameWeekTarget, DayOfWeek.SUNDAY))
    }

    @Test
    fun `weekStartDate aligns to first day of week`() {
        val anchor = LocalDate.parse("2026-05-26")

        val mondayStart = weekStartDate(anchor, 0, DayOfWeek.MONDAY)
        val sundayStart = weekStartDate(anchor, 0, DayOfWeek.SUNDAY)

        assertEquals(LocalDate.parse("2026-05-25"), mondayStart)
        assertEquals(LocalDate.parse("2026-05-24"), sundayStart)
    }

    @Test
    fun `generateWeekDates clamps values outside min and max`() {
        val weekStart = LocalDate.parse("2026-05-25")
        val minDate = LocalDate.parse("2026-05-27")
        val maxDate = LocalDate.parse("2026-05-30")

        val dates = generateWeekDates(weekStart, minDate, maxDate)

        assertEquals(7, dates.size)
        assertEquals(listOf(null, null), dates.take(2))
        assertEquals(
            listOf(
                LocalDate.parse("2026-05-27"),
                LocalDate.parse("2026-05-28"),
                LocalDate.parse("2026-05-29"),
                LocalDate.parse("2026-05-30"),
                null
            ),
            dates.drop(2)
        )
    }

    @Test
    fun `leap year week generation includes feb 29`() {
        val weekStart = LocalDate.parse("2024-02-26")

        val dates = generateWeekDates(weekStart, null, null).filterNotNull()

        assertTrue(dates.contains(LocalDate.parse("2024-02-29")))
        assertEquals(LocalDate.parse("2024-03-03"), dates.last())
    }

    @Test
    fun `effective min and max fallback around selected date`() {
        val selected = LocalDate.parse("2026-05-26")

        val min = effectiveMinDate(null, selected, DayOfWeek.MONDAY)
        val max = effectiveMaxDate(null, selected)

        assertEquals(DayOfWeek.MONDAY, min.dayOfWeek)
        assertEquals(LocalDate.parse("2025-11-24"), min)
        assertEquals(LocalDate.parse("2026-11-26"), max)
    }

    @Test
    fun `locale first day of week rotates columns`() {
        val date = LocalDate.parse("2026-05-26") // Tuesday
        val usFirstDay = WeekFields.of(Locale.US).firstDayOfWeek
        val frFirstDay = WeekFields.of(Locale.FRANCE).firstDayOfWeek

        val usStart = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(usFirstDay))
        val frStart = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(frFirstDay))

        assertEquals(DayOfWeek.SUNDAY, usFirstDay)
        assertEquals(DayOfWeek.MONDAY, frFirstDay)
        assertEquals(LocalDate.parse("2026-05-24"), usStart)
        assertEquals(LocalDate.parse("2026-05-25"), frStart)
        assertFalse(usStart == frStart)
    }

    @Test
    fun `iso identity stays stable in Honolulu timezone`() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            assertEquals("2026-05-26", LocalDate.parse("2026-05-26").toString())
        } finally {
            TimeZone.setDefault(previous)
        }
    }
}
