import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "ImageVariantResolver")

/// Wire-level image variant vocabulary. Maps to a platform-native realization
/// via `ImageVariantSpec`. Unknown server strings fall through to the
/// primitive's default rendering.
enum ImageVariant: String {
    case hero
    case thumbnail
    case logo
}

/// Platform-native realization of an image variant for the iOS client.
/// Mirrors the iOS column of `schema/style-tokens.json`.
struct ImageVariantSpec {
    let cornerRadius: CGFloat?
    let aspectRatio: CGFloat?
    let contentMode: ContentMode
    let fillWidth: Bool
    let clip: Bool
    let overrideMatrix: [String: OverridePolicy]
}

enum ImageVariantResolver {

    /// Returns the realized spec for a recognized variant, or `nil` to let
    /// the primitive fall back to its default rendering.
    static func resolve(_ variant: String?) -> ImageVariantSpec? {
        guard let raw = variant, !raw.isEmpty else { return nil }
        guard let value = ImageVariant(rawValue: raw) else {
            logger.debug("unknown image variant \(raw, privacy: .public); rendering with primitive defaults")
            return nil
        }
        return spec(for: value)
    }

    private static func spec(for variant: ImageVariant) -> ImageVariantSpec {
        switch variant {
        case .hero:
            return ImageVariantSpec(
                cornerRadius: 12,
                aspectRatio: 16.0 / 9.0,
                contentMode: .fill,
                fillWidth: true,
                clip: true,
                overrideMatrix: [
                    "padding": .allow,
                    "cornerRadius": .allow,
                    "background": .allow,
                    "shadow": .allow,
                    "color": .allow,
                    "opacity": .allow,
                    "border": .allow
                ]
            )
        case .thumbnail:
            return ImageVariantSpec(
                cornerRadius: 8,
                aspectRatio: nil,
                contentMode: .fill,
                fillWidth: false,
                clip: true,
                overrideMatrix: [
                    "padding": .allow,
                    "cornerRadius": .allow,
                    "background": .allow,
                    "shadow": .allow,
                    "color": .allow,
                    "opacity": .allow,
                    "border": .allow
                ]
            )
        case .logo:
            return ImageVariantSpec(
                cornerRadius: nil,
                aspectRatio: nil,
                contentMode: .fit,
                fillWidth: false,
                clip: false,
                overrideMatrix: [
                    "padding": .allow,
                    "cornerRadius": .allow,
                    "background": .allow,
                    "shadow": .allow,
                    "color": .allow,
                    "opacity": .allow,
                    "border": .allow
                ]
            )
        }
    }
}
