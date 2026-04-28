package com.nba.sdui.core.renderer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContainerVariantResolverTest {

    @Test
    fun `resolve null variant returns null`() {
        assertNull(ContainerVariantResolver.resolve(null))
    }

    @Test
    fun `resolve blank variant returns null`() {
        assertNull(ContainerVariantResolver.resolve(""))
    }

    @Test
    fun `resolve unknown variant returns null`() {
        assertNull(ContainerVariantResolver.resolve("nonexistent"))
    }

    @Test
    fun `hero phone returns default shadow elevation 8dp`() {
        val spec = ContainerVariantResolver.resolve("hero", "phone")
        assertNotNull(spec)
        assertEquals(8, spec!!.shadowElevationDp)
        assertEquals(16, spec.cornerRadiusDp)
    }

    @Test
    fun `hero tablet returns increased shadow elevation 12dp`() {
        val spec = ContainerVariantResolver.resolve("hero", "tablet")
        assertNotNull(spec)
        assertEquals(12, spec!!.shadowElevationDp)
        assertEquals(16, spec.cornerRadiusDp)
    }

    @Test
    fun `hero default formFactor is phone`() {
        val spec = ContainerVariantResolver.resolve("hero")
        assertNotNull(spec)
        assertEquals(8, spec!!.shadowElevationDp)
    }

    @Test
    fun `grouped phone returns 16dp corner radius`() {
        val spec = ContainerVariantResolver.resolve("grouped", "phone")
        assertNotNull(spec)
        assertEquals(16, spec!!.cornerRadiusDp)
    }

    @Test
    fun `grouped tablet returns 16dp corner radius`() {
        val spec = ContainerVariantResolver.resolve("grouped", "tablet")
        assertNotNull(spec)
        assertEquals(16, spec!!.cornerRadiusDp)
    }
}
