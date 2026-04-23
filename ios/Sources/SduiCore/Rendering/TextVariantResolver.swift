import Foundation

/// Wire-level text variant vocabulary. The Material 3 typography scale
/// plus the legacy semantic variants (`heading1`/`body`/etc.) and the
/// NBA-specific `score` variant. Renderers map each case to a native
/// `Font` (see `AtomicTextView.font(for:)`).
///
/// The schema emits `AtomicElement.variant` as a plain `String` (see
/// `docs/sdui-design-system.md` §6 on strict-decode + renderer-layer
/// fallback); this enum is the iOS client's parser, so an older client
/// receiving a newer variant name falls through to the primitive
/// default rather than failing to decode the section.
///
/// Kept in sync with `TextVariant` in `schema/sdui-schema.json`.
enum TextVariant: String, Codable {
    case displayLarge, displayMedium, displaySmall
    case headlineLarge, headlineMedium, headlineSmall
    case titleLarge, titleMedium, titleSmall
    case bodyLarge, bodyMedium, bodySmall
    case labelLarge, labelMedium, labelSmall
    case heading1, heading2, heading3
    case body, caption, label
    case score
}
