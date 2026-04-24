import Foundation

/// Wire-level button variant vocabulary. Clients resolve each case to
/// their platform's idiomatic button styling (see
/// `SduiButtonStyle` in `AtomicButtonView.swift` for the iOS
/// realization).
///
/// The schema emits `AtomicElement.variant` as a plain `String` (see
/// `docs/sdui-design-system.md` on strict-decode + renderer-layer
/// fallback); this enum is the iOS client's parser, so an older client
/// receiving a newer variant name falls through to `primary` rather
/// than failing to decode the section.
///
/// Kept in sync with `ButtonVariant` in `schema/sdui-schema.json`.
enum ButtonVariant: String, Codable {
    case primary
    case secondary
    case tertiary
    case text
}
