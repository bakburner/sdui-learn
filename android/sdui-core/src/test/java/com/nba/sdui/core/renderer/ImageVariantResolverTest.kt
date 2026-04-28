package com.nba.sdui.core.renderer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImageVariantResolverTest {

    @Test
    fun `resolve null variant returns null`() {
        assertNull(ImageVariantResolver.resolve(null))
    }

    @Test
    fun `resolve blank variant returns null`() {
        assertNull(ImageVariantResolver.resolve(""))
    }

    @Test
    fun `resolve unknown variant returns null`() {
        assertNull(ImageVariantResolver.resolve("nonexistent"))
    }

    @Test
    fun `thumbnail phone returns 8dp corner radius`() {
        val spec = ImageVariantResolver.resolve("thumbnail", "phone")
        assertNotNull(spec)
        assertEquals(8, spec!!.cornerRadiusDp)
    }

    @Test
    fun `thumbnail tablet returns 12dp corner radius`() {
        val spec = ImageVariantResolver.resolve("thumbnail", "tablet")
        assertNotNull(spec)
        assertEquals(12, spec!!.cornerRadiusDp)
    }

    @Test
    fun `thumbnail default formFactor is phone`() {
        val spec = ImageVariantResolver.resolve("thumbnail")
        assertNotNull(spec)
        assertEquals(8, spec!!.cornerRadiusDp)
    }
}
