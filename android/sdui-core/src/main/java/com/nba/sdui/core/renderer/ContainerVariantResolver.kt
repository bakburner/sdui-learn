package com.nba.sdui.core.renderer

import android.os.Build
import android.util.Log

/**
 * Resolves a server-emitted container variant token (e.g. `hero`, `elevated`,
 * `banner`, `subtle`, `grouped`, `overlay`) to a platform-native spec that the
 * renderer applies to an `AtomicContainer`. The spec carries:
 *
 *  - the semantic color role the background should resolve against
 *    ([SurfaceRole]), or `null` when the variant is outlined / overlay-custom
 *  - the corner radius, tonal and shadow elevations, optional border, and
 *    optional top-to-bottom gradient overlay
 *  - an [OverridePolicy] matrix that tells the renderer which inline style
 *    properties are allowed to override the variant's defaults on this platform
 *
 * The registry values mirror `schema/style-tokens.json` — the JSON file is the
 * governance artifact; this object is the hand-authored snapshot the Android
 * client ships with.
 */
object ContainerVariantResolver {

    private const val TAG = "ContainerVariantResolver"

    /** Semantic Material 3 color roles the renderer resolves to concrete Colors. */
    enum class SurfaceRole {
        SurfaceContainerLow,
        SurfaceContainer,
        Surface,
        Background,
        PrimaryContainer,
        Primary,
        Scrim,
        Custom
    }

    /** Per-axis policy for inline overrides of a variant's default. */
    enum class OverridePolicy { ALLOW, LOCK }

    /**
     * Top-to-bottom gradient overlay painted above the solid surface color.
     * Roles are resolved against [androidx.compose.material3.MaterialTheme]
     * at render time; alpha is applied by the renderer.
     */
    data class GradientOverlaySpec(
        val topColorRole: SurfaceRole,
        val topAlpha: Float,
        val bottomColorRole: SurfaceRole,
        val bottomAlpha: Float
    )

    data class ContainerVariantSpec(
        val cornerRadiusDp: Int?,
        val backgroundColorRole: SurfaceRole?,
        /** Applied to the resolved background color before painting. */
        val backgroundAlpha: Float = 1f,
        /** Concrete color used when [backgroundColorRole] is [SurfaceRole.Custom]. */
        val customBackgroundArgb: Long? = null,
        /**
         * Tonal elevation target in dp. Current renderer paints the surface
         * color role directly (no Surface composable), so tonal elevation is
         * informational — the color role already carries the tonal shift.
         */
        val tonalElevationDp: Int?,
        val shadowElevationDp: Int?,
        val gradientOverlay: GradientOverlaySpec?,
        val borderDp: Int?,
        val borderColorRole: SurfaceRole?,
        val fillWidth: Boolean?,
        val overrideMatrix: Map<String, OverridePolicy>
    )

    /** Axis names must match the keys in `schema/style-tokens.json` overrideMatrix. */
    private val ALL_ALLOW: Map<String, OverridePolicy> = mapOf(
        "padding" to OverridePolicy.ALLOW,
        "cornerRadius" to OverridePolicy.ALLOW,
        "background" to OverridePolicy.ALLOW,
        "shadow" to OverridePolicy.ALLOW,
        "color" to OverridePolicy.ALLOW,
        "gap" to OverridePolicy.ALLOW,
        "opacity" to OverridePolicy.ALLOW,
        "border" to OverridePolicy.ALLOW
    )

    /**
     * Resolve the variant token to a [ContainerVariantSpec].
     *
     * @return `null` when [variant] is null / blank (renderer should fall back
     *   to inline-only behavior) OR when it is a non-null value not present in
     *   the registry (a warning is logged and the renderer falls through to
     *   inline-only behavior).
     */
    fun resolve(variant: String?): ContainerVariantSpec? {
        if (variant.isNullOrBlank()) return null
        return when (variant) {
            "hero" -> hero()
            "elevated" -> elevated()
            "banner" -> banner()
            "subtle" -> subtle()
            "grouped" -> grouped()
            "overlay" -> overlay()
            else -> {
                Log.w(TAG, "variant_resolver_missing: $variant")
                null
            }
        }
    }

    /**
     * Emit the `variant_override_blocked` diagnostic when the renderer detects
     * an inline property on an axis the variant's matrix has locked. The
     * variant default wins; the inline value is ignored.
     */
    fun logOverrideBlocked(variant: String?, axis: String, attemptedValue: Any?) {
        Log.d(
            TAG,
            "variant_override_blocked: variant=$variant axis=$axis attemptedValue=$attemptedValue"
        )
    }

    private fun hero(): ContainerVariantSpec = ContainerVariantSpec(
        cornerRadiusDp = 16,
        backgroundColorRole = SurfaceRole.SurfaceContainer,
        tonalElevationDp = 6,
        shadowElevationDp = 8,
        // Top-of-surface accent wash that fades out toward the bottom, giving
        // the surface a slight sense of lift without a literal material effect.
        gradientOverlay = GradientOverlaySpec(
            topColorRole = SurfaceRole.Primary,
            topAlpha = 0.10f,
            bottomColorRole = SurfaceRole.Primary,
            bottomAlpha = 0.0f
        ),
        borderDp = null,
        borderColorRole = null,
        fillWidth = null,
        overrideMatrix = mapOf(
            "padding" to OverridePolicy.ALLOW,
            "cornerRadius" to OverridePolicy.ALLOW,
            "background" to OverridePolicy.ALLOW,
            "shadow" to OverridePolicy.LOCK,
            "color" to OverridePolicy.ALLOW,
            "gap" to OverridePolicy.ALLOW,
            "opacity" to OverridePolicy.ALLOW,
            "border" to OverridePolicy.ALLOW
        )
    )

    private fun elevated(): ContainerVariantSpec = ContainerVariantSpec(
        cornerRadiusDp = 12,
        backgroundColorRole = SurfaceRole.Surface,
        tonalElevationDp = 2,
        shadowElevationDp = null,
        gradientOverlay = null,
        borderDp = null,
        borderColorRole = null,
        fillWidth = null,
        overrideMatrix = ALL_ALLOW
    )

    private fun banner(): ContainerVariantSpec = ContainerVariantSpec(
        cornerRadiusDp = 0,
        backgroundColorRole = SurfaceRole.PrimaryContainer,
        tonalElevationDp = null,
        shadowElevationDp = null,
        gradientOverlay = null,
        borderDp = null,
        borderColorRole = null,
        fillWidth = true,
        overrideMatrix = ALL_ALLOW
    )

    private fun subtle(): ContainerVariantSpec {
        // surfaceContainer* roles were added in Material 3 with the expressive
        // color scheme. On Android < 12 dynamic color is unavailable, so the
        // neutral `background` role is the closer fallback.
        val role = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SurfaceRole.SurfaceContainerLow
        } else {
            SurfaceRole.Background
        }
        return ContainerVariantSpec(
            cornerRadiusDp = 0,
            backgroundColorRole = role,
            tonalElevationDp = null,
            shadowElevationDp = null,
            gradientOverlay = null,
            borderDp = null,
            borderColorRole = null,
            fillWidth = null,
            overrideMatrix = mapOf(
                "padding" to OverridePolicy.ALLOW,
                "cornerRadius" to OverridePolicy.ALLOW,
                "background" to OverridePolicy.ALLOW,
                "shadow" to OverridePolicy.LOCK,
                "color" to OverridePolicy.ALLOW,
                "gap" to OverridePolicy.ALLOW,
                "opacity" to OverridePolicy.ALLOW,
                "border" to OverridePolicy.ALLOW
            )
        )
    }

    private fun grouped(): ContainerVariantSpec = ContainerVariantSpec(
        cornerRadiusDp = 16,
        backgroundColorRole = null,
        tonalElevationDp = null,
        shadowElevationDp = null,
        gradientOverlay = null,
        borderDp = 1,
        // `outlineVariant` is the ideal role but may not be exposed by older
        // material3 versions; `SurfaceContainer` is a visually compatible
        // fallback the resolver consumer can substitute.
        borderColorRole = SurfaceRole.SurfaceContainer,
        fillWidth = null,
        overrideMatrix = ALL_ALLOW
    )

    private fun overlay(): ContainerVariantSpec {
        // Pre-API 31: `scrim` color role predates Material You's dynamic color;
        // fall back to an opaque black tuned for control legibility.
        val (role, alpha, custom) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Triple(SurfaceRole.Scrim, 0.4f, null)
        } else {
            Triple(SurfaceRole.Custom, 0.4f, 0xFF000000L)
        }
        return ContainerVariantSpec(
            cornerRadiusDp = null,
            backgroundColorRole = role,
            backgroundAlpha = alpha,
            customBackgroundArgb = custom,
            tonalElevationDp = null,
            shadowElevationDp = null,
            gradientOverlay = null,
            borderDp = null,
            borderColorRole = null,
            fillWidth = null,
            overrideMatrix = mapOf(
                "padding" to OverridePolicy.ALLOW,
                "cornerRadius" to OverridePolicy.ALLOW,
                "background" to OverridePolicy.LOCK,
                "shadow" to OverridePolicy.LOCK,
                "color" to OverridePolicy.ALLOW,
                "gap" to OverridePolicy.ALLOW,
                "opacity" to OverridePolicy.LOCK,
                "border" to OverridePolicy.LOCK
            )
        )
    }
}
