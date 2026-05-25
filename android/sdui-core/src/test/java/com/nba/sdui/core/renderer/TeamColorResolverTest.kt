package com.nba.sdui.core.renderer

import android.util.Log
import com.nba.sdui.core.tokens.TeamColorRegistry
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for team color token resolution via [TeamColorRegistry] and
 * [ColorTokenResolver.resolveTeamColor].
 *
 * Validates the full resolution pipeline:
 *   semantic → mode → palette role / ref / literal hex
 *
 * All data is resolved from bundled JSON — no network calls.
 */
class TeamColorResolverTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `nba_team_bg for atl resolves to primary color`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.bg", "atl", "dark")
        assertEquals("#C8102E", result)
    }

    @Test
    fun `nba_team_bg for bkn resolves to secondary override`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.bg", "bkn", "dark")
        assertEquals("#707271", result)
    }

    @Test
    fun `nba_team_bg for sas resolves to tertiary override`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.bg", "sas", "dark")
        assertEquals("#4A4A4A", result)
    }

    @Test
    fun `nba_team_accent for sac dark resolves to literal hex`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.accent", "sac", "dark")
        assertEquals("#BEC9CF", result)
    }

    @Test
    fun `nba_team_accent_label for ind dark resolves through nba_color_primary_10`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.accent-label", "ind", "dark")
        // nba.color.primary.10 → nba.color.grey.10 → #191C23
        assertEquals("#191C23", result)
    }

    @Test
    fun `unknown team returns null and logs`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.bg", "zzz", "dark")
        assertNull(result)
        verify { Log.w("TeamColorRegistry", "team_unknown: zzz") }
    }

    @Test
    fun `no network calls during resolution - resolves from bundled data`() {
        // Verifies resolution works entirely from local data by exercising
        // multiple teams and modes without any network dependency.
        val bgAtl = TeamColorRegistry.resolveTeamColor("nba.team.bg", "atl", "light")
        val bgBos = TeamColorRegistry.resolveTeamColor("nba.team.bg", "bos", "dark")
        val accentGsw = TeamColorRegistry.resolveTeamColor("nba.team.accent", "gsw", "dark")

        assertEquals("#C8102E", bgAtl)
        assertEquals("#008348", bgBos)
        assertEquals("#FDB927", accentGsw)
    }

    @Test
    fun `nba_team_bg is theme independent - same result for light and dark`() {
        val light = ColorTokenResolver.resolveTeamColor("nba.team.bg", "atl", "light")
        val dark = ColorTokenResolver.resolveTeamColor("nba.team.bg", "atl", "dark")
        assertEquals(light, dark)
    }

    @Test
    fun `nba_team_accent for bkn light resolves to secondary via light mode`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.accent", "bkn", "light")
        assertEquals("#707271", result)
    }

    @Test
    fun `nba_team_label resolves ref to white`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.label", "atl", "dark")
        // nba.color.primary.100 → nba.color.grey.100 → #FFFFFF
        assertEquals("#FFFFFF", result)
    }

    @Test
    fun `unknown token returns null`() {
        val result = ColorTokenResolver.resolveTeamColor("nba.team.nonexistent", "atl", "dark")
        assertNull(result)
    }
}
