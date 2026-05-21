package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.AspectRatioEnum
import com.nba.sdui.core.models.generated.AspectRatioUnion
import com.nba.sdui.core.models.generated.LayoutScalar
import com.nba.sdui.core.request.RequestEnvelopeBuilder

/**
 * Resolves [LayoutScalar] values and `token:…` layout references against
 * a hand-mirrored snapshot of the bundled token registries under `schema/`
 * (spacing, corner radius, size, typography, shadow). Mirrors
 * `ios/Sources/SduiCore/Rendering/LayoutTokenResolver.swift`.
 *
 * Rules of the road:
 *   - `LayoutScalar.IntegerValue` is returned as-is (raw dp/px on the wire).
 *   - `LayoutScalar.StringValue` must start with the `token:` prefix; the
 *     remainder is resolved through the alias chain (depth ≤ 8) into the
 *     palette and selected by the supplied form factor.
 *   - Strings without the `token:` prefix log a debug diagnostic and
 *     resolve to 0 (lenient resolver — strict decode happened upstream).
 *   - Unknown token names log `token_resolver_missing` and return 0.
 *   - Missing form factor row falls back to the `phone` row.
 *
 * Today the form factor comes from [RequestEnvelopeBuilder.defaultFormFactor].
 * TODO: switch to a `LocalSduiFormFactor` `CompositionLocal` once the
 * form-factor classifier is plumbed end-to-end (Phase 3 of the SDUI plan).
 */
object LayoutTokenResolver {

    private const val TAG = "LayoutTokenResolver"
    private const val TOKEN_PREFIX = "token:"
    private const val MAX_ALIAS_DEPTH = 8

    /** Resolve a [LayoutScalar] to a Compose [Dp]. `null` → 0.dp. */
    fun dp(
        scalar: LayoutScalar?,
        formFactor: String = RequestEnvelopeBuilder.defaultFormFactor()
    ): Dp = intValue(scalar, formFactor).dp

    /** Resolve a [LayoutScalar] to an integer value (dp/px logical). `null` → 0. */
    fun intValue(
        scalar: LayoutScalar?,
        formFactor: String = RequestEnvelopeBuilder.defaultFormFactor()
    ): Int {
        return when (scalar) {
            null -> 0
            is LayoutScalar.IntegerValue -> scalar.value.toInt()
            is LayoutScalar.StringValue -> resolveTokenString(scalar.value, formFactor)
        }
    }

    /**
     * Resolve [AspectRatioUnion] (`DoubleValue` or named ratio enum) to a
     * `width / height` ratio suitable for `Modifier.aspectRatio`. `null`
     * passes through so callers can keep their "only apply when non-null"
     * gating semantics.
     */
    fun aspectRatio(union: AspectRatioUnion?): Float? {
        return when (union) {
            null -> null
            is AspectRatioUnion.DoubleValue -> union.value.toFloat()
            is AspectRatioUnion.EnumValue -> when (union.value) {
                AspectRatioEnum.The11 -> 1f
                AspectRatioEnum.The169 -> 16f / 9f
                AspectRatioEnum.The219 -> 21f / 9f
                AspectRatioEnum.The32 -> 3f / 2f
                AspectRatioEnum.The43 -> 4f / 3f
            }
        }
    }

    // ── Token resolution ─────────────────────────────────────────────

    private fun resolveTokenString(wire: String, formFactor: String): Int {
        if (!wire.startsWith(TOKEN_PREFIX)) {
            Log.d(TAG, "token_resolver_missing: $wire")
            return 0
        }
        val name = wire.removePrefix(TOKEN_PREFIX)
        if (palette[name] == null && semantic[name] == null) {
            Log.d(TAG, "token_resolver_missing: $wire")
        }
        return followAlias(name, formFactor, depth = 0)
    }

    private fun followAlias(name: String, formFactor: String, depth: Int): Int {
        if (depth > MAX_ALIAS_DEPTH) return 0
        palette[name]?.let { return valueFor(formFactor, it) }
        val next = semantic[name] ?: return 0
        return followAlias(next, formFactor, depth + 1)
    }

    private fun valueFor(formFactor: String, row: Map<String, Int>): Int {
        return row[formFactor] ?: row["phone"] ?: 0
    }

    // ── Snapshot: semantic aliases ───────────────────────────────────
    //
    // Mirror of the schema/<kind>-tokens.json semantic maps. Regenerate
    // (by hand for now) when those files change; keep deterministic
    // ordering so diffs are reviewable.
    private val semantic: Map<String, String> = mapOf(
        // spacing-tokens.json (Kinetic)
        "nba.spacing.xs"  to "nba.space.raw.2",
        "nba.spacing.sm"  to "nba.space.raw.4",
        "nba.spacing.md"  to "nba.space.raw.12",
        "nba.spacing.lg"  to "nba.space.raw.16",
        "nba.spacing.xl"  to "nba.space.raw.32",
        "nba.spacing.2xl" to "nba.space.raw.40",

        // corner-radius-tokens.json (Kinetic)
        "nba.radius.xs"   to "nba.radius.raw.2",
        "nba.radius.sm"   to "nba.radius.raw.4",
        "nba.radius.md"   to "nba.radius.raw.12",
        "nba.radius.lg"   to "nba.radius.raw.16",
        "nba.radius.xl"   to "nba.radius.raw.24",
        "nba.radius.2xl"  to "nba.radius.raw.32",
        "nba.radius.full" to "nba.radius.raw.9999",

        // Legacy aliases (deprecated — kept for backward compat with cached payloads)
        "spacing.xs"  to "nba.space.raw.2",
        "spacing.sm"  to "nba.space.raw.4",
        "spacing.md"  to "nba.space.raw.12",
        "spacing.lg"  to "nba.space.raw.16",
        "spacing.xl"  to "nba.space.raw.32",
        "radius.sm"   to "nba.radius.raw.4",
        "radius.md"   to "nba.radius.raw.12",
        "radius.lg"   to "nba.radius.raw.16",
        "radius.full" to "nba.radius.raw.9999",

    )

    // ── Snapshot: merged palette (per form factor) ───────────────────
    //
    // Order of values per row: phone, phone.landscape, tablet, tv, web.narrow, web.wide.
    private val palette: Map<String, Map<String, Int>> = buildMap {
        // spacing (Kinetic)
        addRow("nba.space.raw.0",  0,  0,  0,  0,  0,  0)
        addRow("nba.space.raw.2",  2,  2,  2,  4,  2,  2)
        addRow("nba.space.raw.4",  4,  4,  6,  6,  4,  6)
        addRow("nba.space.raw.8",  8,  8,  10, 12, 8,  10)
        addRow("nba.space.raw.12", 12, 12, 15, 18, 12, 15)
        addRow("nba.space.raw.16", 16, 16, 20, 24, 16, 20)
        addRow("nba.space.raw.32", 32, 32, 40, 48, 32, 40)
        addRow("nba.space.raw.40", 40, 40, 48, 56, 40, 48)

        // corner radius (Kinetic — flat across form factors)
        addRow("nba.radius.raw.0",    0,    0,    0,    0,    0,    0)
        addRow("nba.radius.raw.2",    2,    2,    2,    2,    2,    2)
        addRow("nba.radius.raw.4",    4,    4,    4,    4,    4,    4)
        addRow("nba.radius.raw.8",    8,    8,    8,    8,    8,    8)
        addRow("nba.radius.raw.12",   12,   12,   12,   12,   12,   12)
        addRow("nba.radius.raw.16",   16,   16,   16,   16,   16,   16)
        addRow("nba.radius.raw.24",   24,   24,   24,   24,   24,   24)
        addRow("nba.radius.raw.32",   32,   32,   32,   32,   32,   32)
        addRow("nba.radius.raw.9999", 9999, 9999, 9999, 9999, 9999, 9999)
    }

    private fun MutableMap<String, Map<String, Int>>.addRow(
        key: String,
        phone: Int,
        phoneLandscape: Int,
        tablet: Int,
        tv: Int,
        webNarrow: Int,
        webWide: Int
    ) {
        put(
            key,
            mapOf(
                "phone" to phone,
                "phone.landscape" to phoneLandscape,
                "tablet" to tablet,
                "tv" to tv,
                "web.narrow" to webNarrow,
                "web.wide" to webWide
            )
        )
    }
}
