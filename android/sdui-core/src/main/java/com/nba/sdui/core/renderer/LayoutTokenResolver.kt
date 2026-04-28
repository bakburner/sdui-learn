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
        // spacing-tokens.json
        "spacing.xs"    to "space.raw.4",
        "spacing.sm"    to "space.raw.8",
        "spacing.md"    to "space.raw.12",
        "spacing.lg"    to "space.raw.16",
        "spacing.xl"    to "space.raw.24",
        "spacing.xxl"   to "space.raw.32",

        // corner-radius-tokens.json
        "radius.sm"     to "radius.raw.4",
        "radius.md"     to "radius.raw.8",
        "radius.lg"     to "radius.raw.12",
        "radius.xl"     to "radius.raw.16",
        "radius.full"   to "radius.raw.999",

        // size-tokens.json
        "icon.sm"       to "size.raw.20",
        "icon.md"       to "size.raw.32",
        "icon.lg"       to "size.raw.40",
        "logo.team.sm"  to "size.raw.40",
        "logo.team.md"  to "size.raw.48",
        "logo.team.lg"  to "size.raw.56",
        "avatar.sm"     to "size.raw.40",
        "avatar.md"     to "size.raw.48",
        "avatar.lg"     to "size.raw.64",
        "thumbnail.sm"  to "size.raw.72",
        "thumbnail.md"  to "size.raw.96",

        // typography-tokens.json
        "type.body"     to "type.raw.14",
        "type.bodyEm"   to "type.raw.16",
        "type.title"    to "type.raw.20",
        "type.headline" to "type.raw.28",

        // shadow-tokens.json
        "shadow.sm"     to "shadow.raw.1",
        "shadow.md"     to "shadow.raw.2",
        "shadow.lg"     to "shadow.raw.3"
    )

    // ── Snapshot: merged palette (per form factor) ───────────────────
    //
    // Order of values per row: phone, phone.landscape, tablet, tv, web.narrow, web.wide.
    private val palette: Map<String, Map<String, Int>> = buildMap {
        // spacing-tokens.json
        addRow("space.raw.0",  0,  0,  0,  0,  0,  0)
        addRow("space.raw.4",  4,  4,  6,  8,  4,  6)
        addRow("space.raw.8",  8,  8,  10, 12, 8,  10)
        addRow("space.raw.12", 12, 12, 14, 16, 12, 14)
        addRow("space.raw.16", 16, 16, 18, 20, 16, 18)
        addRow("space.raw.24", 24, 24, 28, 32, 24, 28)
        addRow("space.raw.32", 32, 32, 36, 40, 32, 36)

        // corner-radius-tokens.json
        addRow("radius.raw.0",   0,   0,   0,   0,   0,   0)
        addRow("radius.raw.4",   4,   4,   6,   8,   4,   6)
        addRow("radius.raw.8",   8,   8,   10,  12,  8,   10)
        addRow("radius.raw.12",  12,  12,  14,  16,  12,  14)
        addRow("radius.raw.16",  16,  16,  18,  20,  16,  18)
        addRow("radius.raw.999", 999, 999, 999, 999, 999, 999)

        // size-tokens.json
        addRow("size.raw.20", 20, 20, 24,  28,  20, 24)
        addRow("size.raw.32", 32, 32, 40,  48,  32, 40)
        addRow("size.raw.40", 40, 40, 48,  56,  40, 48)
        addRow("size.raw.48", 48, 48, 56,  64,  48, 56)
        addRow("size.raw.56", 56, 56, 64,  80,  56, 64)
        addRow("size.raw.64", 64, 64, 72,  96,  64, 72)
        addRow("size.raw.72", 72, 72, 80,  112, 72, 80)
        addRow("size.raw.96", 96, 96, 108, 128, 96, 108)

        // typography-tokens.json
        addRow("type.raw.12", 12, 12, 13, 18, 12, 13)
        addRow("type.raw.14", 14, 14, 15, 20, 14, 15)
        addRow("type.raw.16", 16, 16, 17, 24, 16, 17)
        addRow("type.raw.20", 20, 20, 22, 32, 20, 22)
        addRow("type.raw.28", 28, 28, 32, 40, 28, 32)

        // shadow-tokens.json
        addRow("shadow.raw.0", 0, 0, 0, 0, 0, 0)
        addRow("shadow.raw.1", 1, 1, 1, 2, 1, 1)
        addRow("shadow.raw.2", 2, 2, 2, 3, 2, 2)
        addRow("shadow.raw.3", 3, 3, 3, 4, 3, 3)
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
