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
import org.junit.Assert.assertTrue
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
        assertEquals(12, LayoutTokenResolver.intValue(token, formFactor = "phone"))
        assertEquals(15, LayoutTokenResolver.intValue(token, formFactor = "tablet"))
        assertEquals(18, LayoutTokenResolver.intValue(token, formFactor = "tv"))
        assertEquals(12, LayoutTokenResolver.intValue(token, formFactor = "web"))
    }

    @Test
    fun `unknown form factor row falls back to phone`() {
        val token = LayoutScalar.StringValue("token:nba.spacing.md")
        assertEquals(12, LayoutTokenResolver.intValue(token, formFactor = "watch.tiny"))
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
    fun `radius full token resolves to 9999`() {
        val token = LayoutScalar.StringValue("token:nba.radius.full")
        assertEquals(9999, LayoutTokenResolver.intValue(token, formFactor = "phone"))
        assertEquals(9999, LayoutTokenResolver.intValue(token, formFactor = "tablet"))
        assertEquals(9999, LayoutTokenResolver.intValue(token, formFactor = "web"))
    }

    @Test
    fun `typography headlineLarge resolves category and phone scalar size`() {
        val spec = LayoutTokenResolver.typography(
            token = "token:nba.typography.headlineLarge",
            formFactor = LayoutTokenResolver.FormFactor.PHONE
        )
        requireNotNull(spec)
        assertEquals("nba.font.knockout", spec.familyRef)
        assertEquals(360, spec.weight)
        assertEquals("uppercase", spec.textCase)
        assertEquals(0.8, spec.lineHeight, 0.0001)
        val size = spec.size as LayoutTokenResolver.TypographySpec.Size.Scalar
        assertEquals(32, size.value)
    }

    @Test
    fun `typography bodyMedium web returns opaque envelope size`() {
        val spec = LayoutTokenResolver.typography(
            token = "token:nba.typography.bodyMedium",
            formFactor = LayoutTokenResolver.FormFactor.WEB
        )
        requireNotNull(spec)
        val webSize = spec.size as LayoutTokenResolver.TypographySpec.Size.Web
        assertTrue(webSize.value is com.nba.sdui.core.generated.WebSize.Envelope)
    }

    @Test
    fun `unknown typography token returns null`() {
        val spec = LayoutTokenResolver.typography(
            token = "token:nba.typography.unknown",
            formFactor = LayoutTokenResolver.FormFactor.PHONE
        )
        assertNull(spec)
    }

    @Test
    fun `shadow md resolves structured shadow`() {
        val shadow = LayoutTokenResolver.shadowSpec("token:nba.shadow.md")
        requireNotNull(shadow)
        assertEquals(8, shadow.radius)
        assertEquals(2, shadow.offsetY)
        assertEquals("rgba(0,0,0,0.15)", shadow.color)
    }

    @Test
    fun `unknown shadow token returns null`() {
        assertNull(LayoutTokenResolver.shadowSpec("token:nba.shadow.unknown"))
    }

    @Test
    fun `non token shadow string returns null`() {
        assertNull(LayoutTokenResolver.shadowSpec("nba.shadow.md"))
    }

    @Test
    fun `motion duration fast resolves for phone`() {
        val duration = LayoutTokenResolver.motionDuration(
            token = "token:nba.motion.duration.fast",
            formFactor = LayoutTokenResolver.FormFactor.PHONE
        )
        assertEquals(150, duration)
    }

    @Test
    fun `motion easing default resolves`() {
        val easing = LayoutTokenResolver.motionEasing("token:nba.motion.easing.default")
        assertEquals("cubic-bezier(0.16, 1, 0.3, 1)", easing)
    }

    @Test
    fun `form factor changes resolve from baked registry without network`() {
        // The registry is a compile-time object; switching form factor cannot
        // reach any I/O path. We assert the resolver returns the expected
        // per-form-factor values and treat the absence of any HTTP dependency
        // on the resolver call graph as the structural guarantee that resize
        // never hits the network.
        val token = LayoutScalar.StringValue("token:nba.spacing.lg")
        val phoneValue = LayoutTokenResolver.intValue(
            scalar = token,
            formFactor = LayoutTokenResolver.FormFactor.PHONE
        )
        val tabletValue = LayoutTokenResolver.intValue(
            scalar = token,
            formFactor = LayoutTokenResolver.FormFactor.TABLET
        )
        val tvValue = LayoutTokenResolver.intValue(
            scalar = token,
            formFactor = LayoutTokenResolver.FormFactor.TV
        )
        val webValue = LayoutTokenResolver.intValue(
            scalar = token,
            formFactor = LayoutTokenResolver.FormFactor.WEB
        )

        assertEquals(16, phoneValue)
        assertEquals(20, tabletValue)
        assertEquals(24, tvValue)
        assertEquals(16, webValue)
    }
}
