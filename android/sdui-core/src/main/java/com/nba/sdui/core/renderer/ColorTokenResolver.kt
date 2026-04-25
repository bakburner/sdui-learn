package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Resolves SDUI color values to a Compose [Color].
 *
 * Accepts two forms on the wire:
 *   - A literal hex color: `"#RRGGBB"` or `"#RRGGBBAA"`.
 *   - A semantic token reference: `"token:<dot.separated.path>"`
 *     (e.g. `"token:color.primary.50"`, `"token:color.brand.nba"`).
 *
 * Token lookup walks a two-tier registry: `semantic` aliases resolve (through
 * any further aliases) to a `palette` primitive, which carries a light/dark
 * pair. The active Material color scheme selects which hex value is returned.
 *
 * Unknown tokens log `token_resolver_missing` and return [Color.Unspecified]
 * so the caller can fall back to a sensible default. `null` / blank input
 * also returns [Color.Unspecified].
 *
 * The palette and semantic maps below are a hand-mirrored snapshot of
 * `schema/color-tokens.json`. When that file changes, regenerate this
 * snapshot to keep the client registry in sync.
 */
object ColorTokenResolver {

    private const val TAG = "ColorTokenResolver"
    private const val TOKEN_PREFIX = "token:"

    private data class PaletteEntry(val light: String, val dark: String)

    // Palette primitives — literal light/dark hex pairs.
    private val palette: Map<String, PaletteEntry> = mapOf(
        // grey
        "color.grey.0"    to PaletteEntry("#FFFFFF", "#FFFFFF"),
        "color.grey.5"    to PaletteEntry("#FAFAFA", "#0D0F12"),
        "color.grey.10"   to PaletteEntry("#F2F2F7", "#1A1F2E"),
        "color.grey.20"   to PaletteEntry("#E5E5EA", "#2A2A4A"),
        "color.grey.30"   to PaletteEntry("#D1D1D6", "#3A3A5A"),
        "color.grey.40"   to PaletteEntry("#C7C7CC", "#48485F"),
        "color.grey.50"   to PaletteEntry("#8E8E93", "#7A8BAA"),
        "color.grey.60"   to PaletteEntry("#636366", "#9999AA"),
        "color.grey.70"   to PaletteEntry("#48484A", "#AAAAAA"),
        "color.grey.80"   to PaletteEntry("#3A3A3C", "#CCCCCC"),
        "color.grey.90"   to PaletteEntry("#1C1C1E", "#E5E5E7"),
        "color.grey.95"   to PaletteEntry("#0F0F10", "#F2F2F4"),
        "color.grey.99"   to PaletteEntry("#050506", "#FAFAFB"),
        "color.grey.100"  to PaletteEntry("#000000", "#FFFFFF"),

        // blue
        "color.blue.0"    to PaletteEntry("#F5F8FF", "#0A1128"),
        "color.blue.10"   to PaletteEntry("#E0EAFF", "#0E1B3E"),
        "color.blue.20"   to PaletteEntry("#B6CDFF", "#12295A"),
        "color.blue.30"   to PaletteEntry("#7FA0F0", "#1D428A"),
        "color.blue.40"   to PaletteEntry("#3D6BD4", "#2B5AB0"),
        "color.blue.50"   to PaletteEntry("#17408B", "#5B8DEE"),
        "color.blue.60"   to PaletteEntry("#1A4FAF", "#7FA8F0"),
        "color.blue.70"   to PaletteEntry("#3D6DC4", "#A3C1F5"),
        "color.blue.80"   to PaletteEntry("#6F94DC", "#C7D9F8"),
        "color.blue.90"   to PaletteEntry("#A8BEE8", "#E0EAFB"),
        "color.blue.95"   to PaletteEntry("#CFDDF3", "#EEF2FD"),
        "color.blue.99"   to PaletteEntry("#F3F6FD", "#F9FBFE"),
        "color.blue.100"  to PaletteEntry("#FFFFFF", "#FFFFFF"),

        // red
        "color.red.0"     to PaletteEntry("#FFF5F5", "#2C0A0F"),
        "color.red.10"    to PaletteEntry("#FFE5E8", "#4A0F16"),
        "color.red.20"    to PaletteEntry("#FFB8C0", "#6B0A18"),
        "color.red.30"    to PaletteEntry("#FF8091", "#8A0D1E"),
        "color.red.40"    to PaletteEntry("#FF4D62", "#A81025"),
        "color.red.50"    to PaletteEntry("#C8102E", "#FF6B6B"),
        "color.red.60"    to PaletteEntry("#D63848", "#FF8E95"),
        "color.red.70"    to PaletteEntry("#E06470", "#FFB0B5"),
        "color.red.80"    to PaletteEntry("#EC9298", "#FFCBCF"),
        "color.red.90"    to PaletteEntry("#F6C3C6", "#FFE1E3"),
        "color.red.95"    to PaletteEntry("#FBDEE0", "#FFEFF0"),
        "color.red.99"    to PaletteEntry("#FEF7F7", "#FFFAFB"),
        "color.red.100"   to PaletteEntry("#FFFFFF", "#FFFFFF"),

        // green
        "color.green.0"   to PaletteEntry("#F4FBF5", "#0A1F12"),
        "color.green.10"  to PaletteEntry("#DCF0DF", "#0D3018"),
        "color.green.20"  to PaletteEntry("#A8D8B2", "#14502A"),
        "color.green.30"  to PaletteEntry("#6EBC83", "#1C6D3A"),
        "color.green.40"  to PaletteEntry("#3D9E5A", "#258A4C"),
        "color.green.50"  to PaletteEntry("#1F8A3F", "#4CB27A"),
        "color.green.60"  to PaletteEntry("#3EA05B", "#70C594"),
        "color.green.70"  to PaletteEntry("#68B87F", "#94D4AE"),
        "color.green.80"  to PaletteEntry("#97CCA5", "#B8E1C5"),
        "color.green.90"  to PaletteEntry("#C4E0CB", "#DCEEE0"),
        "color.green.95"  to PaletteEntry("#DFECE3", "#EDF5EF"),
        "color.green.99"  to PaletteEntry("#F8FBF9", "#FAFCFB"),
        "color.green.100" to PaletteEntry("#FFFFFF", "#FFFFFF"),

        // orange
        "color.orange.0"   to PaletteEntry("#FFF8F0", "#2C1708"),
        "color.orange.10"  to PaletteEntry("#FFEAD0", "#4A2610"),
        "color.orange.20"  to PaletteEntry("#FFCE8B", "#6E3A17"),
        "color.orange.30"  to PaletteEntry("#FFA94D", "#945121"),
        "color.orange.40"  to PaletteEntry("#F58420", "#BA6B2E"),
        "color.orange.50"  to PaletteEntry("#D86E0F", "#F58A3E"),
        "color.orange.60"  to PaletteEntry("#E0863A", "#FFA35F"),
        "color.orange.70"  to PaletteEntry("#E9A066", "#FFB988"),
        "color.orange.80"  to PaletteEntry("#F2BB92", "#FFD0B0"),
        "color.orange.90"  to PaletteEntry("#F9D9C0", "#FFE5D3"),
        "color.orange.95"  to PaletteEntry("#FCE8D6", "#FFEFE3"),
        "color.orange.99"  to PaletteEntry("#FEF9F4", "#FFFBF7"),
        "color.orange.100" to PaletteEntry("#FFFFFF", "#FFFFFF"),

        // yellow
        "color.yellow.0"   to PaletteEntry("#FFFDF0", "#2A2408"),
        "color.yellow.10"  to PaletteEntry("#FFF6CC", "#463B0F"),
        "color.yellow.20"  to PaletteEntry("#FFEB85", "#695A17"),
        "color.yellow.30"  to PaletteEntry("#FFDB3F", "#8F7C20"),
        "color.yellow.40"  to PaletteEntry("#E8C010", "#B69E29"),
        "color.yellow.50"  to PaletteEntry("#C6A208", "#EACB3B"),
        "color.yellow.60"  to PaletteEntry("#D1B035", "#F2D85D"),
        "color.yellow.70"  to PaletteEntry("#DCC165", "#F6E08A"),
        "color.yellow.80"  to PaletteEntry("#E8D392", "#FAE8B2"),
        "color.yellow.90"  to PaletteEntry("#F1E4C0", "#FBEED4"),
        "color.yellow.95"  to PaletteEntry("#F7EFDA", "#FDF4E6"),
        "color.yellow.99"  to PaletteEntry("#FDFAF4", "#FEFAF2"),
        "color.yellow.100" to PaletteEntry("#FFFFFF", "#FFFFFF")
    )

    // Semantic aliases — every value either points at a palette primitive
    // above or at another semantic alias (the resolver chases the chain).
    private val semantic: Map<String, String> = mapOf(
        // primary — neutral/greyscale ramp
        "color.primary.0"    to "color.grey.0",
        "color.primary.10"   to "color.grey.10",
        "color.primary.20"   to "color.grey.20",
        "color.primary.30"   to "color.grey.30",
        "color.primary.40"   to "color.grey.40",
        "color.primary.50"   to "color.grey.50",
        "color.primary.60"   to "color.grey.60",
        "color.primary.70"   to "color.grey.70",
        "color.primary.80"   to "color.grey.80",
        "color.primary.90"   to "color.grey.90",
        "color.primary.95"   to "color.grey.95",
        "color.primary.99"   to "color.grey.99",
        "color.primary.100"  to "color.grey.100",

        // secondary — yellow accent ramp
        "color.secondary.0"    to "color.yellow.0",
        "color.secondary.10"   to "color.yellow.10",
        "color.secondary.20"   to "color.yellow.20",
        "color.secondary.30"   to "color.yellow.30",
        "color.secondary.40"   to "color.yellow.40",
        "color.secondary.50"   to "color.yellow.50",
        "color.secondary.60"   to "color.yellow.60",
        "color.secondary.70"   to "color.yellow.70",
        "color.secondary.80"   to "color.yellow.80",
        "color.secondary.90"   to "color.yellow.90",
        "color.secondary.95"   to "color.yellow.95",
        "color.secondary.99"   to "color.yellow.99",
        "color.secondary.100"  to "color.yellow.100",

        // tertiary — blue accent ramp
        "color.tertiary.0"    to "color.blue.0",
        "color.tertiary.10"   to "color.blue.10",
        "color.tertiary.20"   to "color.blue.20",
        "color.tertiary.30"   to "color.blue.30",
        "color.tertiary.40"   to "color.blue.40",
        "color.tertiary.50"   to "color.blue.50",
        "color.tertiary.60"   to "color.blue.60",
        "color.tertiary.70"   to "color.blue.70",
        "color.tertiary.80"   to "color.blue.80",
        "color.tertiary.90"   to "color.blue.90",
        "color.tertiary.95"   to "color.blue.95",
        "color.tertiary.99"   to "color.blue.99",
        "color.tertiary.100"  to "color.blue.100",

        // feedback.success — green ramp
        "color.feedback.success.0"    to "color.green.0",
        "color.feedback.success.10"   to "color.green.10",
        "color.feedback.success.20"   to "color.green.20",
        "color.feedback.success.30"   to "color.green.30",
        "color.feedback.success.40"   to "color.green.40",
        "color.feedback.success.50"   to "color.green.50",
        "color.feedback.success.60"   to "color.green.60",
        "color.feedback.success.70"   to "color.green.70",
        "color.feedback.success.80"   to "color.green.80",
        "color.feedback.success.90"   to "color.green.90",
        "color.feedback.success.95"   to "color.green.95",
        "color.feedback.success.99"   to "color.green.99",
        "color.feedback.success.100"  to "color.green.100",

        // feedback.error — red ramp
        "color.feedback.error.0"    to "color.red.0",
        "color.feedback.error.10"   to "color.red.10",
        "color.feedback.error.20"   to "color.red.20",
        "color.feedback.error.30"   to "color.red.30",
        "color.feedback.error.40"   to "color.red.40",
        "color.feedback.error.50"   to "color.red.50",
        "color.feedback.error.60"   to "color.red.60",
        "color.feedback.error.70"   to "color.red.70",
        "color.feedback.error.80"   to "color.red.80",
        "color.feedback.error.90"   to "color.red.90",
        "color.feedback.error.95"   to "color.red.95",
        "color.feedback.error.99"   to "color.red.99",
        "color.feedback.error.100"  to "color.red.100",

        // feedback.warning — orange ramp
        "color.feedback.warning.0"    to "color.orange.0",
        "color.feedback.warning.10"   to "color.orange.10",
        "color.feedback.warning.20"   to "color.orange.20",
        "color.feedback.warning.30"   to "color.orange.30",
        "color.feedback.warning.40"   to "color.orange.40",
        "color.feedback.warning.50"   to "color.orange.50",
        "color.feedback.warning.60"   to "color.orange.60",
        "color.feedback.warning.70"   to "color.orange.70",
        "color.feedback.warning.80"   to "color.orange.80",
        "color.feedback.warning.90"   to "color.orange.90",
        "color.feedback.warning.95"   to "color.orange.95",
        "color.feedback.warning.99"   to "color.orange.99",
        "color.feedback.warning.100"  to "color.orange.100",

        // brand — league identity accents
        "color.brand.nba"   to "color.blue.50",
        "color.brand.live"  to "color.red.50",

        // surface — elevated/recessed/canvas layers
        "color.surface.canvas"  to "color.grey.5",
        "color.surface.raised"  to "color.grey.10",
        "color.surface.sunken"  to "color.grey.5",
        "color.surface.promo"   to "color.blue.10",

        // text — foreground roles
        "color.text.primary"    to "color.grey.90",
        "color.text.secondary"  to "color.grey.60",
        "color.text.tertiary"   to "color.grey.50",
        "color.text.inverse"    to "color.grey.0",
        "color.text.onBrand"    to "color.grey.0",

        // border
        "color.border.default"  to "color.grey.20",
        "color.border.subtle"   to "color.grey.10",

        // overlay — scrims
        "color.overlay.scrim"   to "color.grey.100"
    )

    /**
     * Resolve an SDUI color value to a Compose [Color].
     *
     * - `null` / blank → [Color.Unspecified] (caller decides fallback).
     * - `"#RRGGBB"` / `"#RRGGBBAA"` → parsed as a literal hex color.
     * - `"token:<path>"` → looked up against the semantic / palette registry;
     *   returns the light or dark value based on the ambient color scheme.
     *   Unknown tokens log `token_resolver_missing` and fall through to
     *   [Color.Unspecified].
     */
    @Composable
    fun resolve(value: String?): Color {
        if (value.isNullOrBlank()) return Color.Unspecified
        if (!value.startsWith(TOKEN_PREFIX)) {
            return parseHex(value)
        }
        val name = value.removePrefix(TOKEN_PREFIX)
        val entry = followAlias(name)
        if (entry == null) {
            Log.w(TAG, "token_resolver_missing: $value")
            return Color.Unspecified
        }
        val useDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return parseHex(if (useDark) entry.dark else entry.light)
    }

    /**
     * Walk the alias chain: palette hit returns immediately; otherwise look up
     * the next semantic link. The depth guard is defensive — the registry is
     * CI-validated acyclic, so in practice the chain is 1-2 hops.
     */
    private fun followAlias(name: String, depth: Int = 0): PaletteEntry? {
        if (depth > 8) return null
        palette[name]?.let { return it }
        val next = semantic[name] ?: return null
        return followAlias(next, depth + 1)
    }

    /** Parse a `#RRGGBB` / `#RRGGBBAA` string into a Compose [Color]. */
    private fun parseHex(hex: String): Color {
        return try {
            val stripped = hex.removePrefix("#")
            val argb = when (stripped.length) {
                6 -> "FF$stripped"
                8 -> stripped
                else -> "FF000000"
            }
            Color(argb.toLong(16))
        } catch (_: Exception) {
            Color.Unspecified
        }
    }
}
