import Foundation

/// Wire-level text variant vocabulary. The Material 3 typography scale
/// plus the NBA-specific `score` variant (monospaced digits for live
/// scores and clocks). Renderers map each case to a native `Font`
/// (see `AtomicTextView.font(for:)`).
///
/// The schema emits `AtomicElement.variant` as a plain `String`; this
/// enum is the iOS client's parser, so an older client receiving a
/// newer variant name falls through to the primitive default rather
/// than failing to decode the section.
///
/// Kept in sync with `TextVariant` in `schema/sdui-schema.json`.
enum TextVariant: String, Codable {
    case displayLarge, displayMedium, displaySmall
    case headlineLarge, headlineMedium, headlineSmall
    case titleLarge, titleMedium, titleSmall
    case bodyLarge, bodyMedium, bodySmall
    case labelLarge, labelMedium, labelSmall
    case score
}
