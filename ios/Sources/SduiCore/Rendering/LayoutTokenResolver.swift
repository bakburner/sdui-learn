import CoreGraphics
import os

/// Resolves [LayoutScalar] and `token:…` layout references against the bundled
/// registries in `schema/*-tokens.json` (inline snapshot; regenerate when those files change).
enum LayoutTokenResolver {

    private static let logger = Logger(subsystem: "com.nba.sdui", category: "LayoutTokenResolver")
    private static let tokenPrefix = "token:"
    private static let maxAliasDepth = 8

    /// Resolves a layout scalar to points for the given form factor (see `RequestEnvelope.currentFormFactor`).
    static func cgFloat(
        _ scalar: LayoutScalar?,
        formFactor: String = RequestEnvelope.currentFormFactor
    ) -> CGFloat {
        guard let s = scalar else { return 0 }
        return CGFloat(intValue(s, formFactor: formFactor))
    }

    static func intValue(
        _ scalar: LayoutScalar?,
        formFactor: String = RequestEnvelope.currentFormFactor
    ) -> Int {
        guard let s = scalar else { return 0 }
        switch s {
        case .integer(let i):
            return i
        case .string(let str):
            return resolveTokenString(str, formFactor: formFactor)
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

    private static func resolveTokenString(_ wire: String, formFactor: String) -> Int {
        guard wire.hasPrefix(tokenPrefix) else { return 0 }
        let name = String(wire.dropFirst(tokenPrefix.count))
        if palette[name] == nil, semantic[name] == nil {
            logger.debug("token_resolver_missing: \(wire, privacy: .public)")
        }
        return followAlias(name, formFactor: formFactor, depth: 0)
    }

    private static func followAlias(_ name: String, formFactor: String, depth: Int) -> Int {
        if depth > maxAliasDepth { return 0 }
        if let row = palette[name] {
            return value(for: formFactor, in: row)
        }
        if let next = semantic[name] {
            return followAlias(next, formFactor: formFactor, depth: depth + 1)
        }
        return 0
    }

    private static func value(for formFactor: String, in row: [String: Int]) -> Int {
        if let v = row[formFactor] { return v }
        return row["phone"] ?? 0
    }

    // MARK: - Snapshot: semantic aliases

    // swiftformat:disable:next
    // swiftlint:disable:next line_length
    private static let semantic: [String: String] = [
        "spacing.xs": "space.raw.4", "spacing.sm": "space.raw.8", "spacing.md": "space.raw.12", "spacing.lg": "space.raw.16", "spacing.xl": "space.raw.24", "spacing.xxl": "space.raw.32",
        "radius.sm": "radius.raw.4", "radius.md": "radius.raw.8", "radius.lg": "radius.raw.12", "radius.xl": "radius.raw.16", "radius.full": "radius.raw.999",
        "icon.sm": "size.raw.20", "icon.md": "size.raw.32", "icon.lg": "size.raw.40", "logo.team.sm": "size.raw.40", "logo.team.md": "size.raw.48", "logo.team.lg": "size.raw.56", "avatar.sm": "size.raw.40", "avatar.md": "size.raw.48", "avatar.lg": "size.raw.64", "thumbnail.sm": "size.raw.72", "thumbnail.md": "size.raw.96",
        "type.body": "type.raw.14", "type.bodyEm": "type.raw.16", "type.title": "type.raw.20", "type.headline": "type.raw.28",
        "shadow.sm": "shadow.raw.1", "shadow.md": "shadow.raw.2", "shadow.lg": "shadow.raw.3"
    ]

    // MARK: - Snapshot: palette (merged registries)

    private static let palette: [String: [String: Int]] = {
        var m: [String: [String: Int]] = [:]
        addPalette(&m, "space.raw.0", 0, 0, 0, 0, 0, 0)
        addPalette(&m, "space.raw.4", 4, 4, 6, 8, 4, 6)
        addPalette(&m, "space.raw.8", 8, 8, 10, 12, 8, 10)
        addPalette(&m, "space.raw.12", 12, 12, 14, 16, 12, 14)
        addPalette(&m, "space.raw.16", 16, 16, 18, 20, 16, 18)
        addPalette(&m, "space.raw.24", 24, 24, 28, 32, 24, 28)
        addPalette(&m, "space.raw.32", 32, 32, 36, 40, 32, 36)
        addPalette(&m, "radius.raw.0", 0, 0, 0, 0, 0, 0)
        addPalette(&m, "radius.raw.4", 4, 4, 6, 8, 4, 6)
        addPalette(&m, "radius.raw.8", 8, 8, 10, 12, 8, 10)
        addPalette(&m, "radius.raw.12", 12, 12, 14, 16, 12, 14)
        addPalette(&m, "radius.raw.16", 16, 16, 18, 20, 16, 18)
        addPalette(&m, "radius.raw.999", 999, 999, 999, 999, 999, 999)
        addPalette(&m, "size.raw.20", 20, 20, 24, 28, 20, 24)
        addPalette(&m, "size.raw.32", 32, 32, 40, 48, 32, 40)
        addPalette(&m, "size.raw.40", 40, 40, 48, 56, 40, 48)
        addPalette(&m, "size.raw.48", 48, 48, 56, 64, 48, 56)
        addPalette(&m, "size.raw.56", 56, 56, 64, 80, 56, 64)
        addPalette(&m, "size.raw.64", 64, 64, 72, 96, 64, 72)
        addPalette(&m, "size.raw.72", 72, 72, 80, 112, 72, 80)
        addPalette(&m, "size.raw.96", 96, 96, 108, 128, 96, 108)
        addPalette(&m, "type.raw.12", 12, 12, 13, 18, 12, 13)
        addPalette(&m, "type.raw.14", 14, 14, 15, 20, 14, 15)
        addPalette(&m, "type.raw.16", 16, 16, 17, 24, 16, 17)
        addPalette(&m, "type.raw.20", 20, 20, 22, 32, 20, 22)
        addPalette(&m, "type.raw.28", 28, 28, 32, 40, 28, 32)
        addPalette(&m, "shadow.raw.0", 0, 0, 0, 0, 0, 0)
        addPalette(&m, "shadow.raw.1", 1, 1, 1, 2, 1, 1)
        addPalette(&m, "shadow.raw.2", 2, 2, 2, 3, 2, 2)
        addPalette(&m, "shadow.raw.3", 3, 3, 3, 4, 3, 3)
        return m
    }()

    private static func addPalette(
        _ m: inout [String: [String: Int]],
        _ key: String,
        _ phone: Int,
        _ phoneL: Int,
        _ tablet: Int,
        _ tv: Int,
        _ webN: Int,
        _ webW: Int
    ) {
        m[key] = [
            "phone": phone, "phone.landscape": phoneL, "tablet": tablet, "tv": tv,
            "web.narrow": webN, "web.wide": webW
        ]
    }
}
