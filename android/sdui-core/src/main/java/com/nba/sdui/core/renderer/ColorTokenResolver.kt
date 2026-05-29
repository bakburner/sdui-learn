package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nba.sdui.core.tokens.TeamColorRegistry

/**
 * Resolves SDUI color values to a Compose [Color].
 *
 * Accepts two forms on the wire:
 *   - A literal hex color: `"#RRGGBB"` or `"#RRGGBBAA"`.
 *   - A semantic token reference: `"token:<dot.separated.path>"`
 *     (e.g. `"token:nba.color.primary.50"`, `"token:nba.label.accent.brand"`).
 *
 * Token lookup walks a three-tier registry:
 *   1. `palette` — Kinetic primitives (mode-independent hex) and UI tokens
 *      (mode-aware, values may be token names referencing other palette/semantic
 *      entries).
 *   2. `semantic` — aliases mapping one token name to another.
 *   3. Recursive resolution — after picking light/dark from a palette entry,
 *      if the value is not hex (#...) it is resolved again through followAlias.
 *
 * Unknown tokens log `token_resolver_missing` and return [Color.Unspecified]
 * so the caller can fall back to a sensible default.
 *
 * The palette and semantic maps below are a snapshot of
 * `schema/color-tokens.json`. When that file changes, regenerate this
 * snapshot to keep the client registry in sync.
 */
object ColorTokenResolver {

    private const val TAG = "ColorTokenResolver"
    private const val TOKEN_PREFIX = "token:"
    private const val MAX_DEPTH = 8

    private val resolveCache = mutableMapOf<Pair<String, Boolean>, Color>()

    private data class PaletteEntry(val light: String, val dark: String)

    // ── Palette primitives (mode-independent) + UI tokens (mode-aware) ──

    private val palette: Map<String, PaletteEntry> = mapOf(
        // blue
        "nba.color.blue.0"    to PaletteEntry("#000000", "#000000"),
        "nba.color.blue.10"   to PaletteEntry("#051C2D", "#051C2D"),
        "nba.color.blue.20"   to PaletteEntry("#132A59", "#132A59"),
        "nba.color.blue.30"   to PaletteEntry("#1D428A", "#1D428A"),
        "nba.color.blue.40"   to PaletteEntry("#0064FF", "#0064FF"),
        "nba.color.blue.50"   to PaletteEntry("#1A81FF", "#1A81FF"),
        "nba.color.blue.60"   to PaletteEntry("#4D9DFF", "#4D9DFF"),
        "nba.color.blue.70"   to PaletteEntry("#66ABFF", "#66ABFF"),
        "nba.color.blue.80"   to PaletteEntry("#99C7FF", "#99C7FF"),
        "nba.color.blue.90"   to PaletteEntry("#CCE3FF", "#CCE3FF"),
        "nba.color.blue.95"   to PaletteEntry("#E5F1FF", "#E5F1FF"),
        "nba.color.blue.99"   to PaletteEntry("#F5F9FF", "#F5F9FF"),
        "nba.color.blue.100"  to PaletteEntry("#FFFFFF", "#FFFFFF"),
        // green
        "nba.color.green.0"   to PaletteEntry("#000000", "#000000"),
        "nba.color.green.10"  to PaletteEntry("#103514", "#103514"),
        "nba.color.green.20"  to PaletteEntry("#206A28", "#206A28"),
        "nba.color.green.30"  to PaletteEntry("#317E44", "#317E44"),
        "nba.color.green.40"  to PaletteEntry("#3B8A4A", "#3B8A4A"),
        "nba.color.green.50"  to PaletteEntry("#45A057", "#45A057"),
        "nba.color.green.60"  to PaletteEntry("#4FB664", "#4FB664"),
        "nba.color.green.70"  to PaletteEntry("#30D158", "#30D158"),
        "nba.color.green.80"  to PaletteEntry("#64D879", "#64D879"),
        "nba.color.green.90"  to PaletteEntry("#97E09B", "#97E09B"),
        "nba.color.green.95"  to PaletteEntry("#CBE8BD", "#CBE8BD"),
        "nba.color.green.99"  to PaletteEntry("#F0F2DE", "#F0F2DE"),
        "nba.color.green.100" to PaletteEntry("#FFFFFF", "#FFFFFF"),
        // grey
        "nba.color.grey.0"    to PaletteEntry("#000000", "#000000"),
        "nba.color.grey.10"   to PaletteEntry("#191C23", "#191C23"),
        "nba.color.grey.20"   to PaletteEntry("#2B2F37", "#2B2F37"),
        "nba.color.grey.30"   to PaletteEntry("#3C414A", "#3C414A"),
        "nba.color.grey.40"   to PaletteEntry("#4E525C", "#4E525C"),
        "nba.color.grey.50"   to PaletteEntry("#838A96", "#838A96"),
        "nba.color.grey.60"   to PaletteEntry("#A4A7AD", "#A4A7AD"),
        "nba.color.grey.70"   to PaletteEntry("#BFC2C6", "#BFC2C6"),
        "nba.color.grey.80"   to PaletteEntry("#DADDDE", "#DADDDE"),
        "nba.color.grey.90"   to PaletteEntry("#E7E9EA", "#E7E9EA"),
        "nba.color.grey.95"   to PaletteEntry("#F3F4F5", "#F3F4F5"),
        "nba.color.grey.99"   to PaletteEntry("#F9FAFA", "#F9FAFA"),
        "nba.color.grey.100"  to PaletteEntry("#FFFFFF", "#FFFFFF"),
        // orange
        "nba.color.orange.0"   to PaletteEntry("#000000", "#000000"),
        "nba.color.orange.10"  to PaletteEntry("#1F0D02", "#1F0D02"),
        "nba.color.orange.20"  to PaletteEntry("#3A1805", "#3A1805"),
        "nba.color.orange.30"  to PaletteEntry("#5A2508", "#5A2508"),
        "nba.color.orange.40"  to PaletteEntry("#7A340B", "#7A340B"),
        "nba.color.orange.50"  to PaletteEntry("#9A450F", "#9A450F"),
        "nba.color.orange.60"  to PaletteEntry("#BB5814", "#BB5814"),
        "nba.color.orange.70"  to PaletteEntry("#E66E1A", "#E66E1A"),
        "nba.color.orange.80"  to PaletteEntry("#F18F45", "#F18F45"),
        "nba.color.orange.90"  to PaletteEntry("#F6B679", "#F6B679"),
        "nba.color.orange.95"  to PaletteEntry("#FAD5A6", "#FAD5A6"),
        "nba.color.orange.99"  to PaletteEntry("#FEF2E6", "#FEF2E6"),
        "nba.color.orange.100" to PaletteEntry("#FFFFFF", "#FFFFFF"),
        // red
        "nba.color.red.0"     to PaletteEntry("#000000", "#000000"),
        "nba.color.red.10"    to PaletteEntry("#410E0B", "#410E0B"),
        "nba.color.red.20"    to PaletteEntry("#601410", "#601410"),
        "nba.color.red.30"    to PaletteEntry("#8C1D18", "#8C1D18"),
        "nba.color.red.40"    to PaletteEntry("#B3261E", "#B3261E"),
        "nba.color.red.50"    to PaletteEntry("#DC362E", "#DC362E"),
        "nba.color.red.60"    to PaletteEntry("#FE6F67", "#FE6F67"),
        "nba.color.red.70"    to PaletteEntry("#EC928E", "#EC928E"),
        "nba.color.red.80"    to PaletteEntry("#F2B8B5", "#F2B8B5"),
        "nba.color.red.90"    to PaletteEntry("#F9DEDC", "#F9DEDC"),
        "nba.color.red.95"    to PaletteEntry("#FCEEEE", "#FCEEEE"),
        "nba.color.red.99"    to PaletteEntry("#FFFBF9", "#FFFBF9"),
        "nba.color.red.100"   to PaletteEntry("#FFFFFF", "#FFFFFF"),
        // t-black (transparent black)
        "nba.color.t-black.0"  to PaletteEntry("#00000000", "#00000000"),
        "nba.color.t-black.5"  to PaletteEntry("#0000000D", "#0000000D"),
        "nba.color.t-black.10" to PaletteEntry("#0000001A", "#0000001A"),
        "nba.color.t-black.15" to PaletteEntry("#00000026", "#00000026"),
        "nba.color.t-black.20" to PaletteEntry("#00000033", "#00000033"),
        "nba.color.t-black.25" to PaletteEntry("#00000040", "#00000040"),
        "nba.color.t-black.30" to PaletteEntry("#0000004D", "#0000004D"),
        "nba.color.t-black.40" to PaletteEntry("#00000066", "#00000066"),
        "nba.color.t-black.50" to PaletteEntry("#00000080", "#00000080"),
        "nba.color.t-black.60" to PaletteEntry("#00000099", "#00000099"),
        "nba.color.t-black.70" to PaletteEntry("#000000B2", "#000000B2"),
        "nba.color.t-black.75" to PaletteEntry("#000000BF", "#000000BF"),
        "nba.color.t-black.80" to PaletteEntry("#000000CC", "#000000CC"),
        "nba.color.t-black.85" to PaletteEntry("#000000D9", "#000000D9"),
        "nba.color.t-black.90" to PaletteEntry("#000000E5", "#000000E5"),
        "nba.color.t-black.95" to PaletteEntry("#000000F2", "#000000F2"),
        // t-white (transparent white)
        "nba.color.t-white.0"  to PaletteEntry("#FFFFFF00", "#FFFFFF00"),
        "nba.color.t-white.5"  to PaletteEntry("#FFFFFF0D", "#FFFFFF0D"),
        "nba.color.t-white.10" to PaletteEntry("#FFFFFF1A", "#FFFFFF1A"),
        "nba.color.t-white.15" to PaletteEntry("#FFFFFF26", "#FFFFFF26"),
        "nba.color.t-white.20" to PaletteEntry("#FFFFFF33", "#FFFFFF33"),
        "nba.color.t-white.25" to PaletteEntry("#FFFFFF40", "#FFFFFF40"),
        "nba.color.t-white.30" to PaletteEntry("#FFFFFF4D", "#FFFFFF4D"),
        "nba.color.t-white.40" to PaletteEntry("#FFFFFF66", "#FFFFFF66"),
        "nba.color.t-white.50" to PaletteEntry("#FFFFFF80", "#FFFFFF80"),
        "nba.color.t-white.60" to PaletteEntry("#FFFFFF99", "#FFFFFF99"),
        "nba.color.t-white.70" to PaletteEntry("#FFFFFFB2", "#FFFFFFB2"),
        "nba.color.t-white.75" to PaletteEntry("#FFFFFFBF", "#FFFFFFBF"),
        "nba.color.t-white.80" to PaletteEntry("#FFFFFFCC", "#FFFFFFCC"),
        "nba.color.t-white.85" to PaletteEntry("#FFFFFFD9", "#FFFFFFD9"),
        "nba.color.t-white.90" to PaletteEntry("#FFFFFFE5", "#FFFFFFE5"),
        "nba.color.t-white.95" to PaletteEntry("#FFFFFFF2", "#FFFFFFF2"),
        // yellow
        "nba.color.yellow.0"   to PaletteEntry("#000000", "#000000"),
        "nba.color.yellow.10"  to PaletteEntry("#281E01", "#281E01"),
        "nba.color.yellow.20"  to PaletteEntry("#644B02", "#644B02"),
        "nba.color.yellow.30"  to PaletteEntry("#967103", "#967103"),
        "nba.color.yellow.40"  to PaletteEntry("#E1AA05", "#E1AA05"),
        "nba.color.yellow.50"  to PaletteEntry("#F1BE27", "#F1BE27"),
        "nba.color.yellow.60"  to PaletteEntry("#FBCD44", "#FBCD44"),
        "nba.color.yellow.70"  to PaletteEntry("#FCD769", "#FCD769"),
        "nba.color.yellow.80"  to PaletteEntry("#FCDE82", "#FCDE82"),
        "nba.color.yellow.90"  to PaletteEntry("#FEF2CD", "#FEF2CD"),
        "nba.color.yellow.95"  to PaletteEntry("#FEF9E6", "#FEF9E6"),
        "nba.color.yellow.99"  to PaletteEntry("#FFFEFA", "#FFFEFA"),
        "nba.color.yellow.100" to PaletteEntry("#FFFFFF", "#FFFFFF"),

        // ── UI tokens (mode-aware) — values reference token names ──
        "nba.bg.primary"              to PaletteEntry("nba.color.primary.95", "nba.color.primary.0"),
        "nba.bg.secondary"            to PaletteEntry("nba.color.primary.100", "nba.color.primary.10"),
        "nba.bg.tertiary"             to PaletteEntry("nba.color.primary.90", "nba.color.primary.20"),
        "nba.bg.quaternary"           to PaletteEntry("nba.color.primary.80", "nba.color.primary.30"),
        "nba.bg.selection"            to PaletteEntry("nba.color.primary.0", "nba.color.primary.100"),
        "nba.bg.badge"                to PaletteEntry("nba.color.t-white.90", "nba.color.t-white.90"),
        "nba.bg.disabled"             to PaletteEntry("nba.color.primary.80", "nba.color.primary.30"),
        "nba.bg.splash-screen"        to PaletteEntry("nba.color.tertiary.30", "nba.color.tertiary.0"),
        "nba.bg-dark.primary"         to PaletteEntry("nba.color.primary.0", "nba.color.primary.0"),
        "nba.bg-dark.secondary"       to PaletteEntry("nba.color.primary.10", "nba.color.primary.10"),
        "nba.bg-dark.tertiary"        to PaletteEntry("nba.color.primary.20", "nba.color.primary.20"),
        "nba.bg-dark.quaternary"      to PaletteEntry("nba.color.primary.30", "nba.color.primary.30"),
        "nba.bg-inverted.primary"     to PaletteEntry("nba.color.primary.0", "nba.color.primary.95"),
        "nba.bg-inverted.secondary"   to PaletteEntry("nba.color.primary.10", "nba.color.primary.100"),
        "nba.bg-inverted.tertiary"    to PaletteEntry("nba.color.primary.20", "nba.color.primary.90"),
        "nba.bg-inverted.quaternary"  to PaletteEntry("nba.color.primary.30", "nba.color.primary.80"),
        "nba.bg-tint.primary"         to PaletteEntry("nba.color.secondary.60", "nba.color.secondary.60"),
        "nba.bg-tint.secondary"       to PaletteEntry("nba.color.secondary.80", "nba.color.secondary.80"),
        "nba.bg-tint.tertiary"        to PaletteEntry("nba.color.secondary.50", "nba.color.secondary.50"),
        "nba.bg-tint.quaternary"      to PaletteEntry("nba.color.secondary.40", "nba.color.secondary.50"),
        "nba.label.primary"           to PaletteEntry("nba.color.primary.0", "nba.color.primary.100"),
        "nba.label.secondary"         to PaletteEntry("nba.color.primary.40", "nba.color.primary.60"),
        "nba.label.tertiary"          to PaletteEntry("nba.color.primary.60", "nba.color.primary.50"),
        "nba.label.interactive"       to PaletteEntry("nba.color.tertiary.40", "nba.color.tertiary.70"),
        "nba.label.selection"         to PaletteEntry("nba.color.primary.100", "nba.color.primary.0"),
        "nba.label-dark.primary"      to PaletteEntry("nba.color.primary.100", "nba.color.primary.100"),
        "nba.label-dark.secondary"    to PaletteEntry("nba.color.primary.60", "nba.color.primary.60"),
        "nba.label-dark.tertiary"     to PaletteEntry("nba.color.primary.50", "nba.color.primary.40"),
        "nba.label-dark.quaternary"   to PaletteEntry("nba.color.t-white.25", "nba.color.t-white.25"),
        "nba.label-dark.interactive"  to PaletteEntry("nba.color.tertiary.40", "nba.color.tertiary.40"),
        "nba.label-inverted.primary"  to PaletteEntry("nba.color.primary.100", "nba.color.primary.0"),
        "nba.label-inverted.secondary" to PaletteEntry("nba.color.primary.60", "nba.color.primary.40"),
        "nba.label-inverted.tertiary" to PaletteEntry("nba.color.primary.50", "nba.color.primary.60"),
        "nba.label-inverted.quaternary" to PaletteEntry("nba.color.t-white.20", "nba.color.t-black.5"),
        "nba.label-inverted.link"     to PaletteEntry("nba.color.tertiary.70", "nba.color.tertiary.40"),
        "nba.label-tint.primary"      to PaletteEntry("nba.color.secondary.0", "nba.color.secondary.0"),
        "nba.label-tint.secondary"    to PaletteEntry("nba.color.secondary.20", "nba.color.secondary.20"),
        "nba.label.accent.brand"      to PaletteEntry("#1D428A", "nba.color.primary.100"),
        "nba.label.accent.live"       to PaletteEntry("#C8102E", "#C8102E"),
        "nba.label.accent.splash-screen" to PaletteEntry("nba.color.primary.100", "nba.color.tertiary.40"),
        "nba.divider.moderate"        to PaletteEntry("nba.color.primary.70", "nba.color.primary.40"),
        "nba.divider.subtle"          to PaletteEntry("nba.color.primary.80", "nba.color.primary.20"),
        "nba.divider.prominent"       to PaletteEntry("nba.color.primary.60", "nba.color.primary.50"),
        "nba.effect.blur"             to PaletteEntry("nba.color.t-white.90", "nba.color.t-white.90"),
        "nba.effect.scrim"            to PaletteEntry("nba.color.t-black.50", "nba.color.t-black.50"),
        "nba.effect.shadow-color-15"  to PaletteEntry("nba.color.t-black.15", "nba.color.t-white.15"),
        "nba.effect.shadow-color-30"  to PaletteEntry("nba.color.t-black.30", "nba.color.t-white.30"),
        "nba.feedback.bg-error.primary" to PaletteEntry("nba.color.feedback.error.90", "nba.color.feedback.error.10"),
        "nba.feedback.bg-success.primary" to PaletteEntry("nba.color.feedback.success.99", "nba.color.feedback.success.10"),
        "nba.feedback.bg-warning.primary" to PaletteEntry("nba.color.feedback.warning.99", "nba.color.feedback.warning.10"),
        "nba.feedback.label-error.primary" to PaletteEntry("nba.color.feedback.error.50", "nba.color.feedback.error.60"),
        "nba.feedback.label-error.secondary" to PaletteEntry("nba.color.feedback.error.30", "nba.color.feedback.error.70"),
        "nba.feedback.label-success.primary" to PaletteEntry("nba.color.feedback.success.50", "nba.color.feedback.success.60"),
        "nba.feedback.label-success.secondary" to PaletteEntry("nba.color.feedback.success.30", "nba.color.feedback.success.70"),
        "nba.feedback.label-warning.primary" to PaletteEntry("nba.color.feedback.warning.50", "nba.color.feedback.warning.70"),
        "nba.feedback.label-warning.secondary" to PaletteEntry("nba.color.feedback.warning.30", "nba.color.feedback.warning.60"),
        "nba.button.primary.bg"       to PaletteEntry("nba.bg-inverted.secondary", "nba.bg-inverted.secondary"),
        "nba.button.primary.label"    to PaletteEntry("nba.label-inverted.primary", "nba.label-inverted.primary"),
        "nba.button.primary.border-color" to PaletteEntry("nba.color.t-white.0", "nba.color.t-white.0"),
        "nba.button.secondary.bg"     to PaletteEntry("nba.color.t-black.0", "nba.color.t-black.0"),
        "nba.button.secondary.label"  to PaletteEntry("nba.label.primary", "nba.label.primary"),
        "nba.button.on-dark.bg"       to PaletteEntry("nba.color.primary.100", "nba.color.primary.100"),
        "nba.button.on-dark.label"    to PaletteEntry("nba.color.primary.0", "nba.color.primary.0"),
        "nba.button.tint.bg"          to PaletteEntry("nba.bg-tint.primary", "nba.bg-tint.primary"),
        "nba.button.tint.label"       to PaletteEntry("nba.label-tint.primary", "nba.label-tint.primary"),
        "nba.button.ghost.bg"         to PaletteEntry("nba.color.t-white.25", "nba.color.t-white.25"),
        "nba.button.ghost.label"      to PaletteEntry("nba.color.primary.100", "nba.color.primary.100"),
        "nba.button.focus-ring"       to PaletteEntry("nba.label.interactive", "nba.label.interactive"),
        "nba.opacity.t-dark-4"        to PaletteEntry("nba.color.t-black.5", "nba.color.t-white.5"),
        "nba.opacity.t-dark-8"        to PaletteEntry("nba.color.t-black.10", "nba.color.t-white.10"),
        "nba.opacity.t-dark-10"       to PaletteEntry("nba.color.t-black.10", "nba.color.t-white.10"),
        "nba.opacity.t-dark-16"       to PaletteEntry("nba.color.t-black.15", "nba.color.t-white.20"),
        "nba.opacity.t-light-4"       to PaletteEntry("nba.color.t-white.5", "nba.color.t-black.5"),
        "nba.opacity.t-light-8"       to PaletteEntry("nba.color.t-white.10", "nba.color.t-black.10"),
        "nba.opacity.t-light-10"      to PaletteEntry("nba.color.t-white.10", "nba.color.t-black.10"),
        "nba.opacity.t-light-16"      to PaletteEntry("nba.color.t-white.15", "nba.color.t-black.20")
    )

    // ── Semantic aliases ────────────────────────────────────────────────

    private val semantic: Map<String, String> = mapOf(
        "nba.color.primary.0"    to "nba.color.grey.0",
        "nba.color.primary.10"   to "nba.color.grey.10",
        "nba.color.primary.20"   to "nba.color.grey.20",
        "nba.color.primary.30"   to "nba.color.grey.30",
        "nba.color.primary.40"   to "nba.color.grey.40",
        "nba.color.primary.50"   to "nba.color.grey.50",
        "nba.color.primary.60"   to "nba.color.grey.60",
        "nba.color.primary.70"   to "nba.color.grey.70",
        "nba.color.primary.80"   to "nba.color.grey.80",
        "nba.color.primary.90"   to "nba.color.grey.90",
        "nba.color.primary.95"   to "nba.color.grey.95",
        "nba.color.primary.99"   to "nba.color.grey.99",
        "nba.color.primary.100"  to "nba.color.grey.100",
        "nba.color.secondary.0"    to "nba.color.yellow.0",
        "nba.color.secondary.10"   to "nba.color.yellow.10",
        "nba.color.secondary.20"   to "nba.color.yellow.20",
        "nba.color.secondary.30"   to "nba.color.yellow.30",
        "nba.color.secondary.40"   to "nba.color.yellow.40",
        "nba.color.secondary.50"   to "nba.color.yellow.50",
        "nba.color.secondary.60"   to "nba.color.yellow.60",
        "nba.color.secondary.70"   to "nba.color.yellow.70",
        "nba.color.secondary.80"   to "nba.color.yellow.80",
        "nba.color.secondary.90"   to "nba.color.yellow.90",
        "nba.color.secondary.95"   to "nba.color.yellow.95",
        "nba.color.secondary.99"   to "nba.color.yellow.99",
        "nba.color.secondary.100"  to "nba.color.yellow.100",
        "nba.color.tertiary.0"    to "nba.color.blue.0",
        "nba.color.tertiary.10"   to "nba.color.blue.10",
        "nba.color.tertiary.20"   to "nba.color.blue.20",
        "nba.color.tertiary.30"   to "nba.color.blue.30",
        "nba.color.tertiary.40"   to "nba.color.blue.40",
        "nba.color.tertiary.50"   to "nba.color.blue.50",
        "nba.color.tertiary.60"   to "nba.color.blue.60",
        "nba.color.tertiary.70"   to "nba.color.blue.70",
        "nba.color.tertiary.80"   to "nba.color.blue.80",
        "nba.color.tertiary.90"   to "nba.color.blue.90",
        "nba.color.tertiary.95"   to "nba.color.blue.95",
        "nba.color.tertiary.99"   to "nba.color.blue.99",
        "nba.color.tertiary.100"  to "nba.color.blue.100",
        "nba.color.feedback.success.0"    to "nba.color.green.0",
        "nba.color.feedback.success.10"   to "nba.color.green.10",
        "nba.color.feedback.success.20"   to "nba.color.green.20",
        "nba.color.feedback.success.30"   to "nba.color.green.30",
        "nba.color.feedback.success.40"   to "nba.color.green.40",
        "nba.color.feedback.success.50"   to "nba.color.green.50",
        "nba.color.feedback.success.60"   to "nba.color.green.60",
        "nba.color.feedback.success.70"   to "nba.color.green.70",
        "nba.color.feedback.success.80"   to "nba.color.green.80",
        "nba.color.feedback.success.90"   to "nba.color.green.90",
        "nba.color.feedback.success.95"   to "nba.color.green.95",
        "nba.color.feedback.success.99"   to "nba.color.green.99",
        "nba.color.feedback.success.100"  to "nba.color.green.100",
        "nba.color.feedback.error.0"    to "nba.color.red.0",
        "nba.color.feedback.error.10"   to "nba.color.red.10",
        "nba.color.feedback.error.20"   to "nba.color.red.20",
        "nba.color.feedback.error.30"   to "nba.color.red.30",
        "nba.color.feedback.error.40"   to "nba.color.red.40",
        "nba.color.feedback.error.50"   to "nba.color.red.50",
        "nba.color.feedback.error.60"   to "nba.color.red.60",
        "nba.color.feedback.error.70"   to "nba.color.red.70",
        "nba.color.feedback.error.80"   to "nba.color.red.80",
        "nba.color.feedback.error.90"   to "nba.color.red.90",
        "nba.color.feedback.error.95"   to "nba.color.red.95",
        "nba.color.feedback.error.99"   to "nba.color.red.99",
        "nba.color.feedback.error.100"  to "nba.color.red.100",
        "nba.color.feedback.warning.0"    to "nba.color.orange.0",
        "nba.color.feedback.warning.10"   to "nba.color.orange.10",
        "nba.color.feedback.warning.20"   to "nba.color.orange.20",
        "nba.color.feedback.warning.30"   to "nba.color.orange.30",
        "nba.color.feedback.warning.40"   to "nba.color.orange.40",
        "nba.color.feedback.warning.50"   to "nba.color.orange.50",
        "nba.color.feedback.warning.60"   to "nba.color.orange.60",
        "nba.color.feedback.warning.70"   to "nba.color.orange.70",
        "nba.color.feedback.warning.80"   to "nba.color.orange.80",
        "nba.color.feedback.warning.90"   to "nba.color.orange.90",
        "nba.color.feedback.warning.95"   to "nba.color.orange.95",
        "nba.color.feedback.warning.99"   to "nba.color.orange.99",
        "nba.color.feedback.warning.100"  to "nba.color.orange.100"
    )

    /**
     * Resolve an SDUI color value to a Compose [Color].
     *
     * - `null` / blank → [Color.Unspecified] (caller decides fallback).
     * - `"#RRGGBB"` / `"#RRGGBBAA"` → parsed as a literal hex color.
     * - `"token:<path>"` → looked up against the semantic / palette registry;
     *   returns the light or dark value based on the ambient color scheme.
     *   UI tokens whose light/dark value is another token name are resolved
     *   recursively. Unknown tokens log `token_resolver_missing` and fall
     *   through to [Color.Unspecified].
     */
    @Composable
    fun resolve(value: String?): Color {
        if (value.isNullOrBlank()) return Color.Unspecified
        val useDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val cacheKey = value to useDark
        resolveCache[cacheKey]?.let { return it }
        val resolved = if (!value.startsWith(TOKEN_PREFIX)) {
            parseColorLiteral(value)
        } else {
            val name = value.removePrefix(TOKEN_PREFIX)
            val entry = followAlias(name)
            if (entry == null) {
                Log.w(TAG, "token_resolver_missing: $value")
                Color.Unspecified
            } else {
                val modeValue = if (useDark) entry.dark else entry.light
                resolveIndirection(modeValue, useDark)
            }
        }
        if (resolved != Color.Unspecified) {
            resolveCache[cacheKey] = resolved
        }
        return resolved
    }

    /**
     * Resolve indirection: if the value is already hex (#...) parse it;
     * otherwise treat it as a token name and resolve recursively.
     */
    private fun resolveIndirection(value: String, useDark: Boolean, depth: Int = 0): Color {
        if (depth > MAX_DEPTH) return Color.Unspecified
        if (value.startsWith("#")) return parseHex(value)
        val entry = followAlias(value) ?: return Color.Unspecified
        val next = if (useDark) entry.dark else entry.light
        return resolveIndirection(next, useDark, depth + 1)
    }

    /**
     * Walk the alias chain: palette hit returns immediately; otherwise look up
     * the next semantic link. The depth guard is defensive — the registry is
     * CI-validated acyclic, so in practice the chain is 1-2 hops.
     */
    private fun followAlias(name: String, depth: Int = 0): PaletteEntry? {
        if (depth > MAX_DEPTH) return null
        palette[name]?.let { return it }
        val next = semantic[name] ?: return null
        return followAlias(next, depth + 1)
    }

    /** Parse wire literals: `#RRGGBB`, `#RRGGBBAA`, or `rgba(r,g,b,a)`. */
    private fun parseColorLiteral(raw: String): Color {
        val trimmed = raw.trim()
        if (trimmed.startsWith("rgba(", ignoreCase = true)) {
            return parseRgba(trimmed)
        }
        return parseHex(trimmed)
    }

    /**
     * Parse a `#RRGGBB` / `#RRGGBBAA` string into a Compose [Color].
     *
     * The wire convention is CSS-style `#RRGGBBAA` (alpha trailing) — e.g.
     * `#FFFFFF00` is fully-transparent white, `#FFFFFF1A` is 10% white. Compose's
     * `Color(Long)` ctor expects `AARRGGBB`, so an 8-char wire literal is
     * reassembled with the trailing alpha moved to the front. Without this
     * swap, translucent whites resolve to opaque yellows (the blue channel
     * is mistakenly read as alpha).
     */
    private fun parseHex(hex: String): Color {
        return try {
            val stripped = hex.removePrefix("#")
            val argb = when (stripped.length) {
                6 -> "FF$stripped"
                8 -> stripped.substring(6, 8) + stripped.substring(0, 6)
                else -> return Color.Unspecified
            }
            Color(argb.toLong(16))
        } catch (_: Exception) {
            Color.Unspecified
        }
    }

    private fun parseRgba(rgba: String): Color {
        val inner = rgba.substringAfter('(').substringBeforeLast(')').trim()
        val parts = inner.split(',').map { it.trim() }
        if (parts.size != 4) return Color.Unspecified
        return try {
            val r = parts[0].toFloat()
            val g = parts[1].toFloat()
            val b = parts[2].toFloat()
            val a = parts[3].toFloat().coerceIn(0f, 1f)
            Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a)
        } catch (_: Exception) {
            Color.Unspecified
        }
    }

    // ── Team color resolution ─────────────────────────────────────────────

    /**
     * Resolve an `nba.team.*` token for a specific team and theme.
     *
     * Delegates to [TeamColorRegistry] for the team-specific palette/mode
     * lookup. Returns null if the token or team is unknown.
     *
     * @param token Full token name (e.g. `nba.team.bg`)
     * @param teamId Three-letter lowercase team abbreviation (e.g. `atl`)
     * @param theme `"dark"` or `"light"`
     */
    fun resolveTeamColor(token: String, teamId: String, theme: String): String? {
        return TeamColorRegistry.resolveTeamColor(token, teamId, theme)
    }

    /**
     * Resolve a palette/semantic token name to its hex color string without
     * requiring a Compose context. Used by [TeamColorRegistry] for `ref`-type
     * mode values (e.g. `nba.color.primary.10`).
     *
     * Walks the semantic → palette alias chain and returns the light-mode hex
     * value (team refs always point to mode-independent primitives).
     */
    internal fun resolveTokenToHex(name: String): String? {
        return walkAliasChainToHex(name, 0)
    }

    private fun walkAliasChainToHex(name: String, depth: Int): String? {
        if (depth > MAX_DEPTH) return null
        if (name.startsWith("#")) return name
        val entry = palette[name]
        if (entry != null) {
            val value = entry.light
            return if (value.startsWith("#")) value else walkAliasChainToHex(value, depth + 1)
        }
        val alias = semantic[name] ?: return null
        return walkAliasChainToHex(alias, depth + 1)
    }
}
