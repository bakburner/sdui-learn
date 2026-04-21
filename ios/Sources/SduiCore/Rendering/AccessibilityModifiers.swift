import SwiftUI

/// Unified accessibility modifier for atomic + section renderers.
/// Maps SDUI's ``AccessibilityProperties`` onto SwiftUI's accessibility
/// API. Mirrors Android's
/// [`AccessibilityModifiers.kt`](../../../../../android/sdui-core/src/main/java/com/nba/sdui/core/renderer/AccessibilityModifiers.kt)
/// semantics:
/// - `label`      → `.accessibilityLabel(_:)`
/// - `hint`       → `.accessibilityHint(_:)`
/// - `hidden`     → `.accessibilityHidden(true)` (skip remaining props)
/// - `role`       → `.accessibilityAddTraits(_:)`
/// - `liveRegion` → `.accessibilityAddTraits(.updatesFrequently)` /
///                  `.isLiveRegion` on iOS 17+
/// - `headingLevel` → `.accessibilityHeading(level:)` on role=heading
/// - `sortOrder`  → `.accessibilitySortPriority(_:)` (higher visits first
///                  on SwiftUI, whereas ADR inverts it; we invert below)
struct AccessibilityAtomicModifier: ViewModifier {
    let props: AccessibilityProperties?
    /// Fallback label used when the server didn't supply one. Typically
    /// the atomic element's visible text / alt text.
    let fallbackLabel: String?

    func body(content: Content) -> some View {
        guard let props else {
            if let fallbackLabel, !fallbackLabel.isEmpty {
                return AnyView(content.accessibilityLabel(fallbackLabel))
            }
            return AnyView(content)
        }

        if props.hidden == true {
            return AnyView(content.accessibilityHidden(true))
        }

        var view: AnyView = AnyView(content)

        if let label = props.label, !label.isEmpty {
            view = AnyView(view.accessibilityLabel(label))
        } else if let fallbackLabel, !fallbackLabel.isEmpty {
            view = AnyView(view.accessibilityLabel(fallbackLabel))
        }

        if let hint = props.hint, !hint.isEmpty {
            view = AnyView(view.accessibilityHint(hint))
        }

        if let role = props.role, let traits = Self.traits(for: role) {
            view = AnyView(view.accessibilityAddTraits(traits))
            if role == .heading, let level = props.headingLevel,
               let heading = Self.heading(for: level) {
                view = AnyView(view.accessibilityHeading(heading))
            }
        }

        if let liveRegion = props.liveRegion, liveRegion != .off {
            // Per ADR-008: assertive == .updatesFrequently semantics.
            // SwiftUI's `.isLiveRegion` (iOS 17+) is the closest primitive
            // we have; both live-region values promote to it for now.
            view = AnyView(view.accessibilityAddTraits(.updatesFrequently))
        }

        if let sortOrder = props.sortOrder {
            // SDUI sort order: lower is visited first. SwiftUI sort
            // priority: higher is visited first. Invert.
            view = AnyView(view.accessibilitySortPriority(Double(-sortOrder)))
        }

        return view
    }

    private static func traits(for role: Role) -> AccessibilityTraits? {
        switch role {
        case .button: return .isButton
        case .image: return .isImage
        case .link: return .isLink
        case .heading: return .isHeader
        case .tab: return [.isButton] // iOS has no `.isTab`; `.isButton` + label suffices
        case .table: return []
        case .row, .cell, .list, .listitem: return []
        case .tabpanel: return []
        case .none: return []
        }
    }

    private static func heading(for level: Int) -> AccessibilityHeadingLevel? {
        switch level {
        case 1: return .h1
        case 2: return .h2
        case 3: return .h3
        case 4: return .h4
        case 5: return .h5
        case 6: return .h6
        default: return .unspecified
        }
    }
}

extension View {
    /// Apply SDUI accessibility properties to a view. When `props` is
    /// `nil` the view is unchanged except for the optional fallback label.
    func sduiAccessibility(_ props: AccessibilityProperties?, fallbackLabel: String? = nil) -> some View {
        modifier(AccessibilityAtomicModifier(props: props, fallbackLabel: fallbackLabel))
    }
}
