package com.nba.sdui.core.renderer

import android.util.Log
import com.nba.sdui.core.renderer.ContainerVariantResolver.OverridePolicy

/**
 * Resolves a server-emitted image variant token (`thumbnail`) to a
 * platform-native spec the [AtomicImage] renderer applies to a Coil
 * AsyncImage. Each value in the vocabulary carries a platform-native
 * treatment inline props cannot cleanly express; values that reduce to
 * inline cornerRadius + aspectRatio + fit do not belong here.
 *
 * The registry values mirror `schema/style-tokens.json` — the JSON file is
 * the governance artifact; this object is the hand-authored snapshot the
 * Android client ships with.
 */
object ImageVariantResolver {

    private const val TAG = "ImageVariantResolver"

    /** Neutral content-scale vocabulary mapped to Compose `ContentScale` by caller. */
    enum class ImageContentScaleHint { Crop, Fit, FillBounds, None }

    data class ImageVariantSpec(
        val cornerRadiusDp: Int?,
        val aspectRatio: Float?,
        val contentScaleHint: ImageContentScaleHint,
        val fillWidth: Boolean?,
        /**
         * Whether to clip the image to its corner-radius shape. Logo imagery
         * must retain its native aspect ratio and not clip.
         */
        val clip: Boolean = true,
        val overrideMatrix: Map<String, OverridePolicy>
    )

    private val ALL_ALLOW: Map<String, OverridePolicy> = mapOf(
        "padding" to OverridePolicy.ALLOW,
        "cornerRadius" to OverridePolicy.ALLOW,
        "background" to OverridePolicy.ALLOW,
        "shadow" to OverridePolicy.ALLOW,
        "color" to OverridePolicy.ALLOW,
        "opacity" to OverridePolicy.ALLOW,
        "border" to OverridePolicy.ALLOW
    )

    /**
     * Resolve the variant token to an [ImageVariantSpec].
     *
     * @return `null` when [variant] is null / blank OR when it is a non-null
     *   value not present in the registry (a warning is logged and the
     *   renderer falls through to inline-only behavior).
     */
    fun resolve(variant: String?, formFactor: String = "phone"): ImageVariantSpec? {
        if (variant.isNullOrBlank()) return null
        return when (variant) {
            "thumbnail" -> {
                val isTablet = formFactor == "tablet"
                ImageVariantSpec(
                cornerRadiusDp = if (isTablet) 12 else 8,
                aspectRatio = null,
                contentScaleHint = ImageContentScaleHint.Crop,
                fillWidth = false,
                clip = true,
                overrideMatrix = ALL_ALLOW
            )
            }
            else -> {
                Log.w(TAG, "variant_resolver_missing: $variant")
                null
            }
        }
    }

    /** See [ContainerVariantResolver.logOverrideBlocked]. */
    fun logOverrideBlocked(variant: String?, axis: String, attemptedValue: Any?) {
        Log.d(
            TAG,
            "variant_override_blocked: variant=$variant axis=$axis attemptedValue=$attemptedValue"
        )
    }
}
