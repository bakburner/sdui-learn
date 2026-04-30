import SwiftUI
import os

/// Resolves SDUI color wire values into SwiftUI `Color`. The wire format is
/// either a literal hex (`#RRGGBB` / `#RRGGBBAA`) or a semantic token reference
/// of the form `token:<dot.separated.path>` (e.g. `token:nba.label.accent.brand`).
///
/// The token vocabulary lives in `schema/color-tokens.json` and is mirrored
/// inline below so the library carries no runtime resource dependency;
/// regenerate the `palette` and `semantic` dictionaries whenever the canonical
/// JSON changes.
///
/// Token lookup walks a three-tier registry:
///   1. `palette` — Kinetic primitives (mode-independent hex) and UI tokens
///      (mode-aware, values may be token names referencing other entries).
///   2. `semantic` — aliases mapping one token name to another.
///   3. Recursive resolution — after picking light/dark from a palette entry,
///      if the value is not hex (#...) it is resolved again through followAlias.
///
/// Unknown tokens emit `token_resolver_missing` at debug level and return `nil`
/// so the caller can supply a fallback.
public enum ColorTokenResolver {

    private static let logger = Logger(subsystem: "com.nba.sdui", category: "ColorTokenResolver")
    private static let tokenPrefix = "token:"
    private static let maxAliasDepth = 8

    private struct PaletteEntry {
        let light: String
        let dark: String
    }

    /// Convert a wire color string into a SwiftUI `Color` for the current
    /// color scheme. Returns `nil` for `nil`/empty input or an unresolved
    /// token so the caller can decide on a fallback.
    ///
    /// - `nil` / empty → `nil`.
    /// - `#RRGGBB` / `#RRGGBBAA` → parsed hex literal.
    /// - `token:<path>` → registry lookup; follows alias chains into the
    ///   palette, picks light or dark value, then resolves recursively if
    ///   the value is another token name rather than hex.
    public static func resolve(_ value: String?, colorScheme: ColorScheme) -> Color? {
        guard let raw = value, !raw.isEmpty else { return nil }
        if !raw.hasPrefix(tokenPrefix) {
            return colorFromHex(raw)
        }
        let name = String(raw.dropFirst(tokenPrefix.count))
        guard let entry = followAlias(name) else {
            logger.debug("token_resolver_missing: \(raw, privacy: .public)")
            return nil
        }
        let modeValue = colorScheme == .dark ? entry.dark : entry.light
        return resolveIndirection(modeValue, colorScheme: colorScheme)
    }

    /// Resolve indirection: if the value is already hex (#...) parse it;
    /// otherwise treat it as a token name and resolve recursively.
    private static func resolveIndirection(_ value: String, colorScheme: ColorScheme, depth: Int = 0) -> Color? {
        guard depth < maxAliasDepth else { return nil }
        if value.hasPrefix("#") { return colorFromHex(value) }
        guard let entry = followAlias(value) else { return nil }
        let next = colorScheme == .dark ? entry.dark : entry.light
        return resolveIndirection(next, colorScheme: colorScheme, depth: depth + 1)
    }

    private static func followAlias(_ name: String, depth: Int = 0) -> PaletteEntry? {
        guard depth < maxAliasDepth else { return nil }
        if let entry = palette[name] { return entry }
        guard let next = semantic[name] else { return nil }
        return followAlias(next, depth: depth + 1)
    }

    /// Parse `#RRGGBB` or `#RRGGBBAA`. Leading `#` is optional.
    /// Returns `nil` for any other length.
    private static func colorFromHex(_ hex: String) -> Color? {
        let cleaned = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
        guard cleaned.count == 6 || cleaned.count == 8 else { return nil }

        var rgbValue: UInt64 = 0
        guard Scanner(string: cleaned).scanHexInt64(&rgbValue) else { return nil }

        if cleaned.count == 8 {
            return Color(
                red: Double((rgbValue >> 24) & 0xFF) / 255.0,
                green: Double((rgbValue >> 16) & 0xFF) / 255.0,
                blue: Double((rgbValue >> 8) & 0xFF) / 255.0,
                opacity: Double(rgbValue & 0xFF) / 255.0
            )
        }
        return Color(
            red: Double((rgbValue >> 16) & 0xFF) / 255.0,
            green: Double((rgbValue >> 8) & 0xFF) / 255.0,
            blue: Double(rgbValue & 0xFF) / 255.0
        )
    }

    // MARK: - Palette primitives + UI tokens
    //
    // Snapshot of `schema/color-tokens.json`. Primitives are mode-independent
    // (light == dark). UI tokens are mode-aware — their values may be token
    // names referencing other palette/semantic entries rather than hex.
    private static let palette: [String: PaletteEntry] = [
        // blue
        "nba.color.blue.0":   PaletteEntry(light: "#000000", dark: "#000000"),
        "nba.color.blue.10":  PaletteEntry(light: "#051C2D", dark: "#051C2D"),
        "nba.color.blue.20":  PaletteEntry(light: "#132A59", dark: "#132A59"),
        "nba.color.blue.30":  PaletteEntry(light: "#1D428A", dark: "#1D428A"),
        "nba.color.blue.40":  PaletteEntry(light: "#0064FF", dark: "#0064FF"),
        "nba.color.blue.50":  PaletteEntry(light: "#1A81FF", dark: "#1A81FF"),
        "nba.color.blue.60":  PaletteEntry(light: "#4D9DFF", dark: "#4D9DFF"),
        "nba.color.blue.70":  PaletteEntry(light: "#66ABFF", dark: "#66ABFF"),
        "nba.color.blue.80":  PaletteEntry(light: "#99C7FF", dark: "#99C7FF"),
        "nba.color.blue.90":  PaletteEntry(light: "#CCE3FF", dark: "#CCE3FF"),
        "nba.color.blue.95":  PaletteEntry(light: "#E5F1FF", dark: "#E5F1FF"),
        "nba.color.blue.99":  PaletteEntry(light: "#F5F9FF", dark: "#F5F9FF"),
        "nba.color.blue.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),
        // green
        "nba.color.green.0":   PaletteEntry(light: "#000000", dark: "#000000"),
        "nba.color.green.10":  PaletteEntry(light: "#103514", dark: "#103514"),
        "nba.color.green.20":  PaletteEntry(light: "#206A28", dark: "#206A28"),
        "nba.color.green.30":  PaletteEntry(light: "#317E44", dark: "#317E44"),
        "nba.color.green.40":  PaletteEntry(light: "#3B8A4A", dark: "#3B8A4A"),
        "nba.color.green.50":  PaletteEntry(light: "#45A057", dark: "#45A057"),
        "nba.color.green.60":  PaletteEntry(light: "#4FB664", dark: "#4FB664"),
        "nba.color.green.70":  PaletteEntry(light: "#30D158", dark: "#30D158"),
        "nba.color.green.80":  PaletteEntry(light: "#64D879", dark: "#64D879"),
        "nba.color.green.90":  PaletteEntry(light: "#97E09B", dark: "#97E09B"),
        "nba.color.green.95":  PaletteEntry(light: "#CBE8BD", dark: "#CBE8BD"),
        "nba.color.green.99":  PaletteEntry(light: "#F0F2DE", dark: "#F0F2DE"),
        "nba.color.green.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),
        // grey
        "nba.color.grey.0":   PaletteEntry(light: "#000000", dark: "#000000"),
        "nba.color.grey.10":  PaletteEntry(light: "#191C23", dark: "#191C23"),
        "nba.color.grey.20":  PaletteEntry(light: "#2B2F37", dark: "#2B2F37"),
        "nba.color.grey.30":  PaletteEntry(light: "#3C414A", dark: "#3C414A"),
        "nba.color.grey.40":  PaletteEntry(light: "#4E525C", dark: "#4E525C"),
        "nba.color.grey.50":  PaletteEntry(light: "#838A96", dark: "#838A96"),
        "nba.color.grey.60":  PaletteEntry(light: "#A4A7AD", dark: "#A4A7AD"),
        "nba.color.grey.70":  PaletteEntry(light: "#BFC2C6", dark: "#BFC2C6"),
        "nba.color.grey.80":  PaletteEntry(light: "#DADDDE", dark: "#DADDDE"),
        "nba.color.grey.90":  PaletteEntry(light: "#E7E9EA", dark: "#E7E9EA"),
        "nba.color.grey.95":  PaletteEntry(light: "#F3F4F5", dark: "#F3F4F5"),
        "nba.color.grey.99":  PaletteEntry(light: "#F9FAFA", dark: "#F9FAFA"),
        "nba.color.grey.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),
        // orange
        "nba.color.orange.0":   PaletteEntry(light: "#000000", dark: "#000000"),
        "nba.color.orange.10":  PaletteEntry(light: "#1F0D02", dark: "#1F0D02"),
        "nba.color.orange.20":  PaletteEntry(light: "#3A1805", dark: "#3A1805"),
        "nba.color.orange.30":  PaletteEntry(light: "#5A2508", dark: "#5A2508"),
        "nba.color.orange.40":  PaletteEntry(light: "#7A340B", dark: "#7A340B"),
        "nba.color.orange.50":  PaletteEntry(light: "#9A450F", dark: "#9A450F"),
        "nba.color.orange.60":  PaletteEntry(light: "#BB5814", dark: "#BB5814"),
        "nba.color.orange.70":  PaletteEntry(light: "#E66E1A", dark: "#E66E1A"),
        "nba.color.orange.80":  PaletteEntry(light: "#F18F45", dark: "#F18F45"),
        "nba.color.orange.90":  PaletteEntry(light: "#F6B679", dark: "#F6B679"),
        "nba.color.orange.95":  PaletteEntry(light: "#FAD5A6", dark: "#FAD5A6"),
        "nba.color.orange.99":  PaletteEntry(light: "#FEF2E6", dark: "#FEF2E6"),
        "nba.color.orange.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),
        // red
        "nba.color.red.0":   PaletteEntry(light: "#000000", dark: "#000000"),
        "nba.color.red.10":  PaletteEntry(light: "#410E0B", dark: "#410E0B"),
        "nba.color.red.20":  PaletteEntry(light: "#601410", dark: "#601410"),
        "nba.color.red.30":  PaletteEntry(light: "#8C1D18", dark: "#8C1D18"),
        "nba.color.red.40":  PaletteEntry(light: "#B3261E", dark: "#B3261E"),
        "nba.color.red.50":  PaletteEntry(light: "#DC362E", dark: "#DC362E"),
        "nba.color.red.60":  PaletteEntry(light: "#FE6F67", dark: "#FE6F67"),
        "nba.color.red.70":  PaletteEntry(light: "#EC928E", dark: "#EC928E"),
        "nba.color.red.80":  PaletteEntry(light: "#F2B8B5", dark: "#F2B8B5"),
        "nba.color.red.90":  PaletteEntry(light: "#F9DEDC", dark: "#F9DEDC"),
        "nba.color.red.95":  PaletteEntry(light: "#FCEEEE", dark: "#FCEEEE"),
        "nba.color.red.99":  PaletteEntry(light: "#FFFBF9", dark: "#FFFBF9"),
        "nba.color.red.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),
        // t-black (transparent black)
        "nba.color.t-black.0":  PaletteEntry(light: "#00000000", dark: "#00000000"),
        "nba.color.t-black.5":  PaletteEntry(light: "#0000000D", dark: "#0000000D"),
        "nba.color.t-black.10": PaletteEntry(light: "#0000001A", dark: "#0000001A"),
        "nba.color.t-black.15": PaletteEntry(light: "#00000026", dark: "#00000026"),
        "nba.color.t-black.20": PaletteEntry(light: "#00000033", dark: "#00000033"),
        "nba.color.t-black.25": PaletteEntry(light: "#00000040", dark: "#00000040"),
        "nba.color.t-black.30": PaletteEntry(light: "#0000004D", dark: "#0000004D"),
        "nba.color.t-black.40": PaletteEntry(light: "#00000066", dark: "#00000066"),
        "nba.color.t-black.50": PaletteEntry(light: "#00000080", dark: "#00000080"),
        "nba.color.t-black.60": PaletteEntry(light: "#00000099", dark: "#00000099"),
        "nba.color.t-black.70": PaletteEntry(light: "#000000B2", dark: "#000000B2"),
        "nba.color.t-black.75": PaletteEntry(light: "#000000BF", dark: "#000000BF"),
        "nba.color.t-black.80": PaletteEntry(light: "#000000CC", dark: "#000000CC"),
        "nba.color.t-black.85": PaletteEntry(light: "#000000D9", dark: "#000000D9"),
        "nba.color.t-black.90": PaletteEntry(light: "#000000E5", dark: "#000000E5"),
        "nba.color.t-black.95": PaletteEntry(light: "#000000F2", dark: "#000000F2"),
        // t-white (transparent white)
        "nba.color.t-white.0":  PaletteEntry(light: "#FFFFFF00", dark: "#FFFFFF00"),
        "nba.color.t-white.5":  PaletteEntry(light: "#FFFFFF0D", dark: "#FFFFFF0D"),
        "nba.color.t-white.10": PaletteEntry(light: "#FFFFFF1A", dark: "#FFFFFF1A"),
        "nba.color.t-white.15": PaletteEntry(light: "#FFFFFF26", dark: "#FFFFFF26"),
        "nba.color.t-white.20": PaletteEntry(light: "#FFFFFF33", dark: "#FFFFFF33"),
        "nba.color.t-white.25": PaletteEntry(light: "#FFFFFF40", dark: "#FFFFFF40"),
        "nba.color.t-white.30": PaletteEntry(light: "#FFFFFF4D", dark: "#FFFFFF4D"),
        "nba.color.t-white.40": PaletteEntry(light: "#FFFFFF66", dark: "#FFFFFF66"),
        "nba.color.t-white.50": PaletteEntry(light: "#FFFFFF80", dark: "#FFFFFF80"),
        "nba.color.t-white.60": PaletteEntry(light: "#FFFFFF99", dark: "#FFFFFF99"),
        "nba.color.t-white.70": PaletteEntry(light: "#FFFFFFB2", dark: "#FFFFFFB2"),
        "nba.color.t-white.75": PaletteEntry(light: "#FFFFFFBF", dark: "#FFFFFFBF"),
        "nba.color.t-white.80": PaletteEntry(light: "#FFFFFFCC", dark: "#FFFFFFCC"),
        "nba.color.t-white.85": PaletteEntry(light: "#FFFFFFD9", dark: "#FFFFFFD9"),
        "nba.color.t-white.90": PaletteEntry(light: "#FFFFFFE5", dark: "#FFFFFFE5"),
        "nba.color.t-white.95": PaletteEntry(light: "#FFFFFFF2", dark: "#FFFFFFF2"),
        // yellow
        "nba.color.yellow.0":   PaletteEntry(light: "#000000", dark: "#000000"),
        "nba.color.yellow.10":  PaletteEntry(light: "#281E01", dark: "#281E01"),
        "nba.color.yellow.20":  PaletteEntry(light: "#644B02", dark: "#644B02"),
        "nba.color.yellow.30":  PaletteEntry(light: "#967103", dark: "#967103"),
        "nba.color.yellow.40":  PaletteEntry(light: "#E1AA05", dark: "#E1AA05"),
        "nba.color.yellow.50":  PaletteEntry(light: "#F1BE27", dark: "#F1BE27"),
        "nba.color.yellow.60":  PaletteEntry(light: "#FBCD44", dark: "#FBCD44"),
        "nba.color.yellow.70":  PaletteEntry(light: "#FCD769", dark: "#FCD769"),
        "nba.color.yellow.80":  PaletteEntry(light: "#FCDE82", dark: "#FCDE82"),
        "nba.color.yellow.90":  PaletteEntry(light: "#FEF2CD", dark: "#FEF2CD"),
        "nba.color.yellow.95":  PaletteEntry(light: "#FEF9E6", dark: "#FEF9E6"),
        "nba.color.yellow.99":  PaletteEntry(light: "#FFFEFA", dark: "#FFFEFA"),
        "nba.color.yellow.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),

        // UI tokens (mode-aware) — values reference token names
        "nba.bg.primary":              PaletteEntry(light: "nba.color.primary.95", dark: "nba.color.primary.0"),
        "nba.bg.secondary":            PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.primary.10"),
        "nba.bg.tertiary":             PaletteEntry(light: "nba.color.primary.90", dark: "nba.color.primary.20"),
        "nba.bg.quaternary":           PaletteEntry(light: "nba.color.primary.80", dark: "nba.color.primary.30"),
        "nba.bg.selection":            PaletteEntry(light: "nba.color.primary.0", dark: "nba.color.primary.100"),
        "nba.bg.badge":                PaletteEntry(light: "nba.color.t-white.90", dark: "nba.color.t-white.90"),
        "nba.bg.disabled":             PaletteEntry(light: "nba.color.primary.80", dark: "nba.color.primary.30"),
        "nba.bg.splash-screen":        PaletteEntry(light: "nba.color.tertiary.30", dark: "nba.color.tertiary.0"),
        "nba.bg-dark.primary":         PaletteEntry(light: "nba.color.primary.0", dark: "nba.color.primary.0"),
        "nba.bg-dark.secondary":       PaletteEntry(light: "nba.color.primary.10", dark: "nba.color.primary.10"),
        "nba.bg-dark.tertiary":        PaletteEntry(light: "nba.color.primary.20", dark: "nba.color.primary.20"),
        "nba.bg-dark.quaternary":      PaletteEntry(light: "nba.color.primary.30", dark: "nba.color.primary.30"),
        "nba.bg-inverted.primary":     PaletteEntry(light: "nba.color.primary.0", dark: "nba.color.primary.95"),
        "nba.bg-inverted.secondary":   PaletteEntry(light: "nba.color.primary.10", dark: "nba.color.primary.100"),
        "nba.bg-inverted.tertiary":    PaletteEntry(light: "nba.color.primary.20", dark: "nba.color.primary.90"),
        "nba.bg-inverted.quaternary":  PaletteEntry(light: "nba.color.primary.30", dark: "nba.color.primary.80"),
        "nba.bg-tint.primary":         PaletteEntry(light: "nba.color.secondary.60", dark: "nba.color.secondary.60"),
        "nba.bg-tint.secondary":       PaletteEntry(light: "nba.color.secondary.80", dark: "nba.color.secondary.80"),
        "nba.bg-tint.tertiary":        PaletteEntry(light: "nba.color.secondary.50", dark: "nba.color.secondary.50"),
        "nba.bg-tint.quaternary":      PaletteEntry(light: "nba.color.secondary.40", dark: "nba.color.secondary.50"),
        "nba.label.primary":           PaletteEntry(light: "nba.color.primary.0", dark: "nba.color.primary.100"),
        "nba.label.secondary":         PaletteEntry(light: "nba.color.primary.40", dark: "nba.color.primary.60"),
        "nba.label.tertiary":          PaletteEntry(light: "nba.color.primary.60", dark: "nba.color.primary.50"),
        "nba.label.interactive":       PaletteEntry(light: "nba.color.tertiary.40", dark: "nba.color.tertiary.70"),
        "nba.label.selection":         PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.primary.0"),
        "nba.label-dark.primary":      PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.primary.100"),
        "nba.label-dark.secondary":    PaletteEntry(light: "nba.color.primary.60", dark: "nba.color.primary.60"),
        "nba.label-dark.tertiary":     PaletteEntry(light: "nba.color.primary.50", dark: "nba.color.primary.40"),
        "nba.label-dark.quaternary":   PaletteEntry(light: "nba.color.t-white.25", dark: "nba.color.t-white.25"),
        "nba.label-dark.interactive":  PaletteEntry(light: "nba.color.tertiary.40", dark: "nba.color.tertiary.40"),
        "nba.label-inverted.primary":  PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.primary.0"),
        "nba.label-inverted.secondary": PaletteEntry(light: "nba.color.primary.60", dark: "nba.color.primary.40"),
        "nba.label-inverted.tertiary": PaletteEntry(light: "nba.color.primary.50", dark: "nba.color.primary.60"),
        "nba.label-inverted.quaternary": PaletteEntry(light: "nba.color.t-white.20", dark: "nba.color.t-black.5"),
        "nba.label-inverted.link":     PaletteEntry(light: "nba.color.tertiary.70", dark: "nba.color.tertiary.40"),
        "nba.label-tint.primary":      PaletteEntry(light: "nba.color.secondary.0", dark: "nba.color.secondary.0"),
        "nba.label-tint.secondary":    PaletteEntry(light: "nba.color.secondary.20", dark: "nba.color.secondary.20"),
        "nba.label.accent.brand":      PaletteEntry(light: "#1D428A", dark: "nba.color.primary.100"),
        "nba.label.accent.live":       PaletteEntry(light: "#C8102E", dark: "#C8102E"),
        "nba.label.accent.splash-screen": PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.tertiary.40"),
        "nba.divider.moderate":        PaletteEntry(light: "nba.color.primary.70", dark: "nba.color.primary.40"),
        "nba.divider.subtle":          PaletteEntry(light: "nba.color.primary.80", dark: "nba.color.primary.20"),
        "nba.divider.prominent":       PaletteEntry(light: "nba.color.primary.60", dark: "nba.color.primary.50"),
        "nba.effect.blur":             PaletteEntry(light: "nba.color.t-white.90", dark: "nba.color.t-white.90"),
        "nba.effect.scrim":            PaletteEntry(light: "nba.color.t-black.50", dark: "nba.color.t-black.50"),
        "nba.effect.shadow-color-15":  PaletteEntry(light: "nba.color.t-black.15", dark: "nba.color.t-white.15"),
        "nba.effect.shadow-color-30":  PaletteEntry(light: "nba.color.t-black.30", dark: "nba.color.t-white.30"),
        "nba.feedback.bg-error.primary": PaletteEntry(light: "nba.color.feedback.error.90", dark: "nba.color.feedback.error.10"),
        "nba.feedback.bg-success.primary": PaletteEntry(light: "nba.color.feedback.success.99", dark: "nba.color.feedback.success.10"),
        "nba.feedback.bg-warning.primary": PaletteEntry(light: "nba.color.feedback.warning.99", dark: "nba.color.feedback.warning.10"),
        "nba.feedback.label-error.primary": PaletteEntry(light: "nba.color.feedback.error.50", dark: "nba.color.feedback.error.60"),
        "nba.feedback.label-error.secondary": PaletteEntry(light: "nba.color.feedback.error.30", dark: "nba.color.feedback.error.70"),
        "nba.feedback.label-success.primary": PaletteEntry(light: "nba.color.feedback.success.50", dark: "nba.color.feedback.success.60"),
        "nba.feedback.label-success.secondary": PaletteEntry(light: "nba.color.feedback.success.30", dark: "nba.color.feedback.success.70"),
        "nba.feedback.label-warning.primary": PaletteEntry(light: "nba.color.feedback.warning.50", dark: "nba.color.feedback.warning.70"),
        "nba.feedback.label-warning.secondary": PaletteEntry(light: "nba.color.feedback.warning.30", dark: "nba.color.feedback.warning.60"),
        "nba.button.primary.bg":       PaletteEntry(light: "nba.bg-inverted.secondary", dark: "nba.bg-inverted.secondary"),
        "nba.button.primary.label":    PaletteEntry(light: "nba.label-inverted.primary", dark: "nba.label-inverted.primary"),
        "nba.button.primary.border-color": PaletteEntry(light: "nba.color.t-white.0", dark: "nba.color.t-white.0"),
        "nba.button.secondary.bg":     PaletteEntry(light: "nba.color.t-black.0", dark: "nba.color.t-black.0"),
        "nba.button.secondary.label":  PaletteEntry(light: "nba.label.primary", dark: "nba.label.primary"),
        "nba.button.on-dark.bg":       PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.primary.100"),
        "nba.button.on-dark.label":    PaletteEntry(light: "nba.color.primary.0", dark: "nba.color.primary.0"),
        "nba.button.tint.bg":          PaletteEntry(light: "nba.bg-tint.primary", dark: "nba.bg-tint.primary"),
        "nba.button.tint.label":       PaletteEntry(light: "nba.label-tint.primary", dark: "nba.label-tint.primary"),
        "nba.button.ghost.bg":         PaletteEntry(light: "nba.color.t-white.25", dark: "nba.color.t-white.25"),
        "nba.button.ghost.label":      PaletteEntry(light: "nba.color.primary.100", dark: "nba.color.primary.100"),
        "nba.button.focus-ring":       PaletteEntry(light: "nba.label.interactive", dark: "nba.label.interactive"),
        "nba.opacity.t-dark-4":        PaletteEntry(light: "nba.color.t-black.5", dark: "nba.color.t-white.5"),
        "nba.opacity.t-dark-8":        PaletteEntry(light: "nba.color.t-black.10", dark: "nba.color.t-white.10"),
        "nba.opacity.t-dark-10":       PaletteEntry(light: "nba.color.t-black.10", dark: "nba.color.t-white.10"),
        "nba.opacity.t-dark-16":       PaletteEntry(light: "nba.color.t-black.15", dark: "nba.color.t-white.20"),
        "nba.opacity.t-light-4":       PaletteEntry(light: "nba.color.t-white.5", dark: "nba.color.t-black.5"),
        "nba.opacity.t-light-8":       PaletteEntry(light: "nba.color.t-white.10", dark: "nba.color.t-black.10"),
        "nba.opacity.t-light-10":      PaletteEntry(light: "nba.color.t-white.10", dark: "nba.color.t-black.10"),
        "nba.opacity.t-light-16":      PaletteEntry(light: "nba.color.t-white.15", dark: "nba.color.t-black.20"),
    ]

    // MARK: - Semantic aliases
    private static let semantic: [String: String] = [
        "nba.color.primary.0":   "nba.color.grey.0",
        "nba.color.primary.10":  "nba.color.grey.10",
        "nba.color.primary.20":  "nba.color.grey.20",
        "nba.color.primary.30":  "nba.color.grey.30",
        "nba.color.primary.40":  "nba.color.grey.40",
        "nba.color.primary.50":  "nba.color.grey.50",
        "nba.color.primary.60":  "nba.color.grey.60",
        "nba.color.primary.70":  "nba.color.grey.70",
        "nba.color.primary.80":  "nba.color.grey.80",
        "nba.color.primary.90":  "nba.color.grey.90",
        "nba.color.primary.95":  "nba.color.grey.95",
        "nba.color.primary.99":  "nba.color.grey.99",
        "nba.color.primary.100": "nba.color.grey.100",
        "nba.color.secondary.0":   "nba.color.yellow.0",
        "nba.color.secondary.10":  "nba.color.yellow.10",
        "nba.color.secondary.20":  "nba.color.yellow.20",
        "nba.color.secondary.30":  "nba.color.yellow.30",
        "nba.color.secondary.40":  "nba.color.yellow.40",
        "nba.color.secondary.50":  "nba.color.yellow.50",
        "nba.color.secondary.60":  "nba.color.yellow.60",
        "nba.color.secondary.70":  "nba.color.yellow.70",
        "nba.color.secondary.80":  "nba.color.yellow.80",
        "nba.color.secondary.90":  "nba.color.yellow.90",
        "nba.color.secondary.95":  "nba.color.yellow.95",
        "nba.color.secondary.99":  "nba.color.yellow.99",
        "nba.color.secondary.100": "nba.color.yellow.100",
        "nba.color.tertiary.0":   "nba.color.blue.0",
        "nba.color.tertiary.10":  "nba.color.blue.10",
        "nba.color.tertiary.20":  "nba.color.blue.20",
        "nba.color.tertiary.30":  "nba.color.blue.30",
        "nba.color.tertiary.40":  "nba.color.blue.40",
        "nba.color.tertiary.50":  "nba.color.blue.50",
        "nba.color.tertiary.60":  "nba.color.blue.60",
        "nba.color.tertiary.70":  "nba.color.blue.70",
        "nba.color.tertiary.80":  "nba.color.blue.80",
        "nba.color.tertiary.90":  "nba.color.blue.90",
        "nba.color.tertiary.95":  "nba.color.blue.95",
        "nba.color.tertiary.99":  "nba.color.blue.99",
        "nba.color.tertiary.100": "nba.color.blue.100",
        "nba.color.feedback.success.0":   "nba.color.green.0",
        "nba.color.feedback.success.10":  "nba.color.green.10",
        "nba.color.feedback.success.20":  "nba.color.green.20",
        "nba.color.feedback.success.30":  "nba.color.green.30",
        "nba.color.feedback.success.40":  "nba.color.green.40",
        "nba.color.feedback.success.50":  "nba.color.green.50",
        "nba.color.feedback.success.60":  "nba.color.green.60",
        "nba.color.feedback.success.70":  "nba.color.green.70",
        "nba.color.feedback.success.80":  "nba.color.green.80",
        "nba.color.feedback.success.90":  "nba.color.green.90",
        "nba.color.feedback.success.95":  "nba.color.green.95",
        "nba.color.feedback.success.99":  "nba.color.green.99",
        "nba.color.feedback.success.100": "nba.color.green.100",
        "nba.color.feedback.error.0":   "nba.color.red.0",
        "nba.color.feedback.error.10":  "nba.color.red.10",
        "nba.color.feedback.error.20":  "nba.color.red.20",
        "nba.color.feedback.error.30":  "nba.color.red.30",
        "nba.color.feedback.error.40":  "nba.color.red.40",
        "nba.color.feedback.error.50":  "nba.color.red.50",
        "nba.color.feedback.error.60":  "nba.color.red.60",
        "nba.color.feedback.error.70":  "nba.color.red.70",
        "nba.color.feedback.error.80":  "nba.color.red.80",
        "nba.color.feedback.error.90":  "nba.color.red.90",
        "nba.color.feedback.error.95":  "nba.color.red.95",
        "nba.color.feedback.error.99":  "nba.color.red.99",
        "nba.color.feedback.error.100": "nba.color.red.100",
        "nba.color.feedback.warning.0":   "nba.color.orange.0",
        "nba.color.feedback.warning.10":  "nba.color.orange.10",
        "nba.color.feedback.warning.20":  "nba.color.orange.20",
        "nba.color.feedback.warning.30":  "nba.color.orange.30",
        "nba.color.feedback.warning.40":  "nba.color.orange.40",
        "nba.color.feedback.warning.50":  "nba.color.orange.50",
        "nba.color.feedback.warning.60":  "nba.color.orange.60",
        "nba.color.feedback.warning.70":  "nba.color.orange.70",
        "nba.color.feedback.warning.80":  "nba.color.orange.80",
        "nba.color.feedback.warning.90":  "nba.color.orange.90",
        "nba.color.feedback.warning.95":  "nba.color.orange.95",
        "nba.color.feedback.warning.99":  "nba.color.orange.99",
        "nba.color.feedback.warning.100": "nba.color.orange.100",
    ]
}
