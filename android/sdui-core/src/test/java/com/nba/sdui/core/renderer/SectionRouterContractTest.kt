package com.nba.sdui.core.renderer

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SectionRouterContractTest {

    @Test
    fun `supported section types include CalendarStrip`() {
        assertTrue("CalendarStrip" in SUPPORTED_SECTION_TYPES)
        assertTrue("CalendarMonthList" in SUPPORTED_SECTION_TYPES)
    }
}
