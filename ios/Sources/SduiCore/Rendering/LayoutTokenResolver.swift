import CoreGraphics
import os

/// Resolves [LayoutScalar] and `token:…` layout references against the bundled
/// registries in `schema/*-tokens.json` (inline snapshot; regenerate when those files change).
enum LayoutTokenResolver {

    private static let logger = Logger(subsystem: "com.nba.sdui", category: "LayoutTokenResolver")
    private static let tokenPrefix = "token:"
    private static let maxAliasDepth = 8

    /// Resolves a layout scalar to points for the given form factor and theme.
    static func cgFloat(
        _ scalar: LayoutScalar?,
        formFactor: String = RequestEnvelope.currentFormFactor,
        theme: String = RequestEnvelope.currentTheme
    ) -> CGFloat {
        guard let s = scalar else { return 0 }
        return CGFloat(intValue(s, formFactor: formFactor, theme: theme))
    }

    static func intValue(
        _ scalar: LayoutScalar?,
        formFactor: String = RequestEnvelope.currentFormFactor,
        theme: String = RequestEnvelope.currentTheme
    ) -> Int {
        guard let s = scalar else { return 0 }
        switch s {
        case .integer(let i):
            return i
        case .string(let str):
            return resolveTokenString(str, formFactor: formFactor, theme: theme)
        }
    }

    static func aspectRatio(_ union: AspectRatioUnion?) -> CGFloat? {
        guard let u = union else { return nil }
        switch u {
        case .double(let d):
            return CGFloat(d)
        case .enumeration(let e):
            switch e {
            case .the169: return 16.0 / 9.0
            case .the43: return 4.0 / 3.0
            case .the11: return 1.0
            case .the32: return 3.0 / 2.0
            case .the219: return 21.0 / 9.0
            }
        }
    }

    // MARK: - Token resolution (aliasOf → palette)

    private static func resolveTokenString(_ wire: String, formFactor: String, theme: String) -> Int {
        guard wire.hasPrefix(tokenPrefix) else { return 0 }
        let name = String(wire.dropFirst(tokenPrefix.count))
        if palette[name] == nil, semantic[name] == nil {
            logger.debug("token_resolver_missing: \(wire, privacy: .public)")
        }
        return followAlias(name, formFactor: formFactor, theme: theme, depth: 0)
    }

    private static func followAlias(_ name: String, formFactor: String, theme: String, depth: Int) -> Int {
        if depth > maxAliasDepth { return 0 }
        if let row = palette[name] {
            return resolveMatrix(row, theme: theme, formFactor: formFactor)
        }
        if let next = semantic[name] {
            return followAlias(next, formFactor: formFactor, theme: theme, depth: depth + 1)
        }
        return 0
    }

    /// 4-step fallback: exact → theme-wildcard → formFactor-wildcard → universal
    private static func resolveMatrix(_ matrix: [String: [String: Int]], theme: String, formFactor: String) -> Int {
        if let themeRow = matrix[theme], let v = themeRow[formFactor] { return v }
        if let themeRow = matrix[theme], let v = themeRow["*"] { return v }
        if let wildRow = matrix["*"], let v = wildRow[formFactor] { return v }
        if let wildRow = matrix["*"], let v = wildRow["*"] { return v }
        return 0
    }

    // MARK: - Snapshot: semantic aliases

    // swiftformat:disable:next
    // swiftlint:disable:next line_length
    private static let semantic: [String: String] = [
        // spacing (Kinetic)
        "nba.spacing.xs": "nba.space.raw.2", "nba.spacing.sm": "nba.space.raw.4", "nba.spacing.md": "nba.space.raw.12", "nba.spacing.lg": "nba.space.raw.16", "nba.spacing.xl": "nba.space.raw.32", "nba.spacing.2xl": "nba.space.raw.40",
        // radius (Kinetic)
        "nba.radius.xs": "nba.radius.raw.2", "nba.radius.sm": "nba.radius.raw.4", "nba.radius.md": "nba.radius.raw.12", "nba.radius.lg": "nba.radius.raw.16", "nba.radius.xl": "nba.radius.raw.24", "nba.radius.2xl": "nba.radius.raw.32", "nba.radius.full": "nba.radius.raw.9999",
        // Legacy aliases (deprecated — backward compat with cached payloads)
        "spacing.xs": "nba.space.raw.2", "spacing.sm": "nba.space.raw.4", "spacing.md": "nba.space.raw.12", "spacing.lg": "nba.space.raw.16", "spacing.xl": "nba.space.raw.32",
        "radius.sm": "nba.radius.raw.4", "radius.md": "nba.radius.raw.12", "radius.lg": "nba.radius.raw.16", "radius.full": "nba.radius.raw.9999"
    ]

    // MARK: - Snapshot: palette (merged registries — matrix shape: token → theme → formFactor → value)

    private static let palette: [String: [String: [String: Int]]] = {
        var m: [String: [String: [String: Int]]] = [:]
        // spacing (Kinetic)
        addPalette(&m, "nba.space.raw.0",  0,  0,  0,  0,  0,  0)
        addPalette(&m, "nba.space.raw.2",  2,  2,  2,  4,  2,  2)
        addPalette(&m, "nba.space.raw.4",  4,  4,  6,  6,  4,  6)
        addPalette(&m, "nba.space.raw.8",  8,  8,  10, 12, 8,  10)
        addPalette(&m, "nba.space.raw.12", 12, 12, 15, 18, 12, 15)
        addPalette(&m, "nba.space.raw.16", 16, 16, 20, 24, 16, 20)
        addPalette(&m, "nba.space.raw.32", 32, 32, 40, 48, 32, 40)
        addPalette(&m, "nba.space.raw.40", 40, 40, 48, 56, 40, 48)
        // corner radius (Kinetic — flat across form factors)
        addPalette(&m, "nba.radius.raw.0",    0,    0,    0,    0,    0,    0)
        addPalette(&m, "nba.radius.raw.2",    2,    2,    2,    2,    2,    2)
        addPalette(&m, "nba.radius.raw.4",    4,    4,    4,    4,    4,    4)
        addPalette(&m, "nba.radius.raw.8",    8,    8,    8,    8,    8,    8)
        addPalette(&m, "nba.radius.raw.12",   12,   12,   12,   12,   12,   12)
        addPalette(&m, "nba.radius.raw.16",   16,   16,   16,   16,   16,   16)
        addPalette(&m, "nba.radius.raw.24",   24,   24,   24,   24,   24,   24)
        addPalette(&m, "nba.radius.raw.32",   32,   32,   32,   32,   32,   32)
        addPalette(&m, "nba.radius.raw.9999", 9999, 9999, 9999, 9999, 9999, 9999)
        return m
    }()

    private static func addPalette(
        _ m: inout [String: [String: [String: Int]]],
        _ key: String,
        _ phone: Int,
        _ phoneL: Int,
        _ tablet: Int,
        _ tv: Int,
        _ webN: Int,
        _ webW: Int
    ) {
        m[key] = ["*": [
            "phone": phone, "phone.landscape": phoneL, "tablet": tablet, "tv": tv,
            "web.narrow": webN, "web.wide": webW
        ]]
    }
}
