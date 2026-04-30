package com.nba.sdui.core.renderer

import android.util.Log
import com.nba.sdui.core.models.generated.AspectRatioEnum
import com.nba.sdui.core.models.generated.AspectRatioUnion
import com.nba.sdui.core.models.generated.LayoutScalar
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
 * Unit tests for [LayoutTokenResolver]. Validates:
 *   - numeric `LayoutScalar` passes through unchanged
 *   - semantic tokens resolve per-form-factor through the alias chain
 *   - unknown tokens log `token_resolver_missing` and return 0
 *   - non-`token:` strings log a debug diagnostic and return 0
 *   - aspect ratio enum values map to `width / height` floats
 *   - alias chain hops correctly (e.g. `radius.full` → `radius.raw.9999`)
 *
 * Mirrors `LayoutTokenResolverTests.swift` on iOS so cross-platform
 * resolution semantics stay equivalent.
 */
class LayoutTokenResolverTest {

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
    fun `numeric scalar passes through unchanged`() {
        val scalar = LayoutScalar.IntegerValue(42L)
        assertEquals(42, LayoutTokenResolver.intValue(scalar, formFactor = "phone"))
        assertEquals(42, LayoutTokenResolver.intValue(scalar, formFactor = "tablet"))
    }

    @Test
    fun `null scalar resolves to zero`() {
        assertEquals(0, LayoutTokenResolver.intValue(null, formFactor = "phone"))
    }

    @Test
    fun `spacing md resolves per form factor`() {
        val token = LayoutScalar.StringValue("token:nba.spacing.md")
        assertEquals(8, LayoutTokenResolver.intValue(token, formFactor = "phone"))
        assertEquals(10, LayoutTokenResolver.intValue(token, formFactor = "tablet"))
    }

    @Test
    fun `unknown form factor row falls back to zero`() {
        val token = LayoutScalar.StringValue("token:nba.spacing.md")
        assertEquals(0, LayoutTokenResolver.intValue(token, formFactor = "watch.tiny"))
    }

    @Test
    fun `unknown token returns zero and logs token_resolver_missing`() {
        val token = LayoutScalar.StringValue("token:nba.spacing.does.not.exist")
        assertEquals(0, LayoutTokenResolver.intValue(token, formFactor = "phone"))
        verify { Log.d("LayoutTokenResolver", "token_resolver_missing: token:nba.spacing.does.not.exist") }
    }

    @Test
    fun `string without token prefix returns zero and logs`() {
        val notAToken = LayoutScalar.StringValue("spacing.md")
        assertEquals(0, LayoutTokenResolver.intValue(notAToken, formFactor = "phone"))
        verify { Log.d("LayoutTokenResolver", "token_resolver_missing: spacing.md") }
    }

    @Test
    fun `aspect ratio enum 16 by 9 returns 16 over 9`() {
        val ratio = LayoutTokenResolver.aspectRatio(
            AspectRatioUnion.EnumValue(AspectRatioEnum.The169)
        )
        assertEquals(16f / 9f, ratio!!, 0.0001f)
    }

    @Test
    fun `aspect ratio enum 1 by 1 returns 1`() {
        val ratio = LayoutTokenResolver.aspectRatio(
            AspectRatioUnion.EnumValue(AspectRatioEnum.The11)
        )
        assertEquals(1f, ratio!!, 0.0001f)
    }

    @Test
    fun `aspect ratio double passes through`() {
        val ratio = LayoutTokenResolver.aspectRatio(AspectRatioUnion.DoubleValue(2.5))
        assertEquals(2.5f, ratio!!, 0.0001f)
    }

    @Test
    fun `aspect ratio null passes through as null so callers can short-circuit`() {
        assertNull(LayoutTokenResolver.aspectRatio(null))
    }

    @Test
    fun `radius full alias chain resolves to 9999`() {
        val token = LayoutScalar.StringValue("token:nba.radius.full")
        assertEquals(9999, LayoutTokenResolver.intValue(token, formFactor = "phone"))
        assertEquals(9999, LayoutTokenResolver.intValue(token, formFactor = "tablet"))
        assertEquals(9999, LayoutTokenResolver.intValue(token, formFactor = "web.wide"))
    }
}
