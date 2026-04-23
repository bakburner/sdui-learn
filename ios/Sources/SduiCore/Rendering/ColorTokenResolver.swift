import SwiftUI
import os

/// Resolves SDUI color wire values into SwiftUI `Color`. The wire format is
/// either a literal hex (`#RRGGBB` / `#RRGGBBAA`) or a semantic token reference
/// of the form `token:<dot.separated.path>` (e.g. `token:color.brand.live`).
///
/// The token vocabulary lives in `schema/color-tokens.json` and is mirrored
/// inline below so the library carries no runtime resource dependency;
/// regenerate the `palette` and `semantic` dictionaries whenever the canonical
/// JSON changes. Unknown tokens emit `token_resolver_missing` at debug level
/// and return `nil` so the caller can supply a fallback.
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
    /// - `token:<path>` → registry lookup; follows `aliasOf` chains from
    ///   semantic tokens into the palette, then picks the light or dark value.
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
        let hex = colorScheme == .dark ? entry.dark : entry.light
        return colorFromHex(hex)
    }

    private static func followAlias(_ name: String, depth: Int = 0) -> PaletteEntry? {
        // Defensive cap; the registry is validated acyclic at CI time.
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

    // MARK: - Palette primitives
    //
    // Snapshot of `schema/color-tokens.json` → `palette`. Each entry carries a
    // light and a dark hex literal; the resolver picks one based on the
    // SwiftUI `ColorScheme` supplied by the caller. Regenerate when the JSON
    // changes; the CI validator cross-checks the two sides.
    private static let palette: [String: PaletteEntry] = [
        // greys
        "color.grey.0":   PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),
        "color.grey.5":   PaletteEntry(light: "#FAFAFA", dark: "#0D0F12"),
        "color.grey.10":  PaletteEntry(light: "#F2F2F7", dark: "#1A1F2E"),
        "color.grey.20":  PaletteEntry(light: "#E5E5EA", dark: "#2A2A4A"),
        "color.grey.30":  PaletteEntry(light: "#D1D1D6", dark: "#3A3A5A"),
        "color.grey.40":  PaletteEntry(light: "#C7C7CC", dark: "#48485F"),
        "color.grey.50":  PaletteEntry(light: "#8E8E93", dark: "#7A8BAA"),
        "color.grey.60":  PaletteEntry(light: "#636366", dark: "#9999AA"),
        "color.grey.70":  PaletteEntry(light: "#48484A", dark: "#AAAAAA"),
        "color.grey.80":  PaletteEntry(light: "#3A3A3C", dark: "#CCCCCC"),
        "color.grey.90":  PaletteEntry(light: "#1C1C1E", dark: "#E5E5E7"),
        "color.grey.95":  PaletteEntry(light: "#0F0F10", dark: "#F2F2F4"),
        "color.grey.99":  PaletteEntry(light: "#050506", dark: "#FAFAFB"),
        "color.grey.100": PaletteEntry(light: "#000000", dark: "#FFFFFF"),

        // blues
        "color.blue.0":   PaletteEntry(light: "#F5F8FF", dark: "#0A1128"),
        "color.blue.10":  PaletteEntry(light: "#E0EAFF", dark: "#0E1B3E"),
        "color.blue.20":  PaletteEntry(light: "#B6CDFF", dark: "#12295A"),
        "color.blue.30":  PaletteEntry(light: "#7FA0F0", dark: "#1D428A"),
        "color.blue.40":  PaletteEntry(light: "#3D6BD4", dark: "#2B5AB0"),
        "color.blue.50":  PaletteEntry(light: "#17408B", dark: "#5B8DEE"),
        "color.blue.60":  PaletteEntry(light: "#1A4FAF", dark: "#7FA8F0"),
        "color.blue.70":  PaletteEntry(light: "#3D6DC4", dark: "#A3C1F5"),
        "color.blue.80":  PaletteEntry(light: "#6F94DC", dark: "#C7D9F8"),
        "color.blue.90":  PaletteEntry(light: "#A8BEE8", dark: "#E0EAFB"),
        "color.blue.95":  PaletteEntry(light: "#CFDDF3", dark: "#EEF2FD"),
        "color.blue.99":  PaletteEntry(light: "#F3F6FD", dark: "#F9FBFE"),
        "color.blue.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),

        // reds
        "color.red.0":   PaletteEntry(light: "#FFF5F5", dark: "#2C0A0F"),
        "color.red.10":  PaletteEntry(light: "#FFE5E8", dark: "#4A0F16"),
        "color.red.20":  PaletteEntry(light: "#FFB8C0", dark: "#6B0A18"),
        "color.red.30":  PaletteEntry(light: "#FF8091", dark: "#8A0D1E"),
        "color.red.40":  PaletteEntry(light: "#FF4D62", dark: "#A81025"),
        "color.red.50":  PaletteEntry(light: "#C8102E", dark: "#FF6B6B"),
        "color.red.60":  PaletteEntry(light: "#D63848", dark: "#FF8E95"),
        "color.red.70":  PaletteEntry(light: "#E06470", dark: "#FFB0B5"),
        "color.red.80":  PaletteEntry(light: "#EC9298", dark: "#FFCBCF"),
        "color.red.90":  PaletteEntry(light: "#F6C3C6", dark: "#FFE1E3"),
        "color.red.95":  PaletteEntry(light: "#FBDEE0", dark: "#FFEFF0"),
        "color.red.99":  PaletteEntry(light: "#FEF7F7", dark: "#FFFAFB"),
        "color.red.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),

        // greens
        "color.green.0":   PaletteEntry(light: "#F4FBF5", dark: "#0A1F12"),
        "color.green.10":  PaletteEntry(light: "#DCF0DF", dark: "#0D3018"),
        "color.green.20":  PaletteEntry(light: "#A8D8B2", dark: "#14502A"),
        "color.green.30":  PaletteEntry(light: "#6EBC83", dark: "#1C6D3A"),
        "color.green.40":  PaletteEntry(light: "#3D9E5A", dark: "#258A4C"),
        "color.green.50":  PaletteEntry(light: "#1F8A3F", dark: "#4CB27A"),
        "color.green.60":  PaletteEntry(light: "#3EA05B", dark: "#70C594"),
        "color.green.70":  PaletteEntry(light: "#68B87F", dark: "#94D4AE"),
        "color.green.80":  PaletteEntry(light: "#97CCA5", dark: "#B8E1C5"),
        "color.green.90":  PaletteEntry(light: "#C4E0CB", dark: "#DCEEE0"),
        "color.green.95":  PaletteEntry(light: "#DFECE3", dark: "#EDF5EF"),
        "color.green.99":  PaletteEntry(light: "#F8FBF9", dark: "#FAFCFB"),
        "color.green.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),

        // oranges
        "color.orange.0":   PaletteEntry(light: "#FFF8F0", dark: "#2C1708"),
        "color.orange.10":  PaletteEntry(light: "#FFEAD0", dark: "#4A2610"),
        "color.orange.20":  PaletteEntry(light: "#FFCE8B", dark: "#6E3A17"),
        "color.orange.30":  PaletteEntry(light: "#FFA94D", dark: "#945121"),
        "color.orange.40":  PaletteEntry(light: "#F58420", dark: "#BA6B2E"),
        "color.orange.50":  PaletteEntry(light: "#D86E0F", dark: "#F58A3E"),
        "color.orange.60":  PaletteEntry(light: "#E0863A", dark: "#FFA35F"),
        "color.orange.70":  PaletteEntry(light: "#E9A066", dark: "#FFB988"),
        "color.orange.80":  PaletteEntry(light: "#F2BB92", dark: "#FFD0B0"),
        "color.orange.90":  PaletteEntry(light: "#F9D9C0", dark: "#FFE5D3"),
        "color.orange.95":  PaletteEntry(light: "#FCE8D6", dark: "#FFEFE3"),
        "color.orange.99":  PaletteEntry(light: "#FEF9F4", dark: "#FFFBF7"),
        "color.orange.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF"),

        // yellows
        "color.yellow.0":   PaletteEntry(light: "#FFFDF0", dark: "#2A2408"),
        "color.yellow.10":  PaletteEntry(light: "#FFF6CC", dark: "#463B0F"),
        "color.yellow.20":  PaletteEntry(light: "#FFEB85", dark: "#695A17"),
        "color.yellow.30":  PaletteEntry(light: "#FFDB3F", dark: "#8F7C20"),
        "color.yellow.40":  PaletteEntry(light: "#E8C010", dark: "#B69E29"),
        "color.yellow.50":  PaletteEntry(light: "#C6A208", dark: "#EACB3B"),
        "color.yellow.60":  PaletteEntry(light: "#D1B035", dark: "#F2D85D"),
        "color.yellow.70":  PaletteEntry(light: "#DCC165", dark: "#F6E08A"),
        "color.yellow.80":  PaletteEntry(light: "#E8D392", dark: "#FAE8B2"),
        "color.yellow.90":  PaletteEntry(light: "#F1E4C0", dark: "#FBEED4"),
        "color.yellow.95":  PaletteEntry(light: "#F7EFDA", dark: "#FDF4E6"),
        "color.yellow.99":  PaletteEntry(light: "#FDFAF4", dark: "#FEFAF2"),
        "color.yellow.100": PaletteEntry(light: "#FFFFFF", dark: "#FFFFFF")
    ]

    // MARK: - Semantic aliases
    //
    // Snapshot of `schema/color-tokens.json` → `semantic`. Each value is the
    // name of another entry — palette primitive or another semantic token.
    // The resolver follows the chain until it lands in the palette.
    private static let semantic: [String: String] = [
        // primary (greys)
        "color.primary.0":   "color.grey.0",
        "color.primary.10":  "color.grey.10",
        "color.primary.20":  "color.grey.20",
        "color.primary.30":  "color.grey.30",
        "color.primary.40":  "color.grey.40",
        "color.primary.50":  "color.grey.50",
        "color.primary.60":  "color.grey.60",
        "color.primary.70":  "color.grey.70",
        "color.primary.80":  "color.grey.80",
        "color.primary.90":  "color.grey.90",
        "color.primary.95":  "color.grey.95",
        "color.primary.99":  "color.grey.99",
        "color.primary.100": "color.grey.100",

        // secondary (yellows)
        "color.secondary.0":   "color.yellow.0",
        "color.secondary.10":  "color.yellow.10",
        "color.secondary.20":  "color.yellow.20",
        "color.secondary.30":  "color.yellow.30",
        "color.secondary.40":  "color.yellow.40",
        "color.secondary.50":  "color.yellow.50",
        "color.secondary.60":  "color.yellow.60",
        "color.secondary.70":  "color.yellow.70",
        "color.secondary.80":  "color.yellow.80",
        "color.secondary.90":  "color.yellow.90",
        "color.secondary.95":  "color.yellow.95",
        "color.secondary.99":  "color.yellow.99",
        "color.secondary.100": "color.yellow.100",

        // tertiary (blues)
        "color.tertiary.0":   "color.blue.0",
        "color.tertiary.10":  "color.blue.10",
        "color.tertiary.20":  "color.blue.20",
        "color.tertiary.30":  "color.blue.30",
        "color.tertiary.40":  "color.blue.40",
        "color.tertiary.50":  "color.blue.50",
        "color.tertiary.60":  "color.blue.60",
        "color.tertiary.70":  "color.blue.70",
        "color.tertiary.80":  "color.blue.80",
        "color.tertiary.90":  "color.blue.90",
        "color.tertiary.95":  "color.blue.95",
        "color.tertiary.99":  "color.blue.99",
        "color.tertiary.100": "color.blue.100",

        // feedback.success (greens)
        "color.feedback.success.0":   "color.green.0",
        "color.feedback.success.10":  "color.green.10",
        "color.feedback.success.20":  "color.green.20",
        "color.feedback.success.30":  "color.green.30",
        "color.feedback.success.40":  "color.green.40",
        "color.feedback.success.50":  "color.green.50",
        "color.feedback.success.60":  "color.green.60",
        "color.feedback.success.70":  "color.green.70",
        "color.feedback.success.80":  "color.green.80",
        "color.feedback.success.90":  "color.green.90",
        "color.feedback.success.95":  "color.green.95",
        "color.feedback.success.99":  "color.green.99",
        "color.feedback.success.100": "color.green.100",

        // feedback.error (reds)
        "color.feedback.error.0":   "color.red.0",
        "color.feedback.error.10":  "color.red.10",
        "color.feedback.error.20":  "color.red.20",
        "color.feedback.error.30":  "color.red.30",
        "color.feedback.error.40":  "color.red.40",
        "color.feedback.error.50":  "color.red.50",
        "color.feedback.error.60":  "color.red.60",
        "color.feedback.error.70":  "color.red.70",
        "color.feedback.error.80":  "color.red.80",
        "color.feedback.error.90":  "color.red.90",
        "color.feedback.error.95":  "color.red.95",
        "color.feedback.error.99":  "color.red.99",
        "color.feedback.error.100": "color.red.100",

        // feedback.warning (oranges)
        "color.feedback.warning.0":   "color.orange.0",
        "color.feedback.warning.10":  "color.orange.10",
        "color.feedback.warning.20":  "color.orange.20",
        "color.feedback.warning.30":  "color.orange.30",
        "color.feedback.warning.40":  "color.orange.40",
        "color.feedback.warning.50":  "color.orange.50",
        "color.feedback.warning.60":  "color.orange.60",
        "color.feedback.warning.70":  "color.orange.70",
        "color.feedback.warning.80":  "color.orange.80",
        "color.feedback.warning.90":  "color.orange.90",
        "color.feedback.warning.95":  "color.orange.95",
        "color.feedback.warning.99":  "color.orange.99",
        "color.feedback.warning.100": "color.orange.100",

        // brand
        "color.brand.nba":  "color.blue.50",
        "color.brand.live": "color.red.50",

        // surface
        "color.surface.canvas": "color.grey.10",
        "color.surface.raised": "color.grey.0",
        "color.surface.sunken": "color.grey.5",
        "color.surface.promo":  "color.blue.10",

        // text
        "color.text.primary":   "color.grey.90",
        "color.text.secondary": "color.grey.60",
        "color.text.tertiary":  "color.grey.50",
        "color.text.inverse":   "color.grey.0",
        "color.text.onBrand":   "color.grey.0",

        // border
        "color.border.default": "color.grey.20",
        "color.border.subtle":  "color.grey.10",

        // overlay
        "color.overlay.scrim": "color.grey.100"
    ]
}
