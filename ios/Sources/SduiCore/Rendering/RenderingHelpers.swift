import SwiftUI

// MARK: - Shared helpers used across atomic renderers

/// Parse a hex color string to SwiftUI Color.
func color(from hex: String?) -> Color? {
    guard let hex = hex else { return nil }
    let cleaned = hex.trimmingCharacters(in: CharacterSet(charactersIn: "#"))
    guard cleaned.count == 6 || cleaned.count == 8 else { return nil }

    var rgbValue: UInt64 = 0
    Scanner(string: cleaned).scanHexInt64(&rgbValue)

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

/// Convert schema Spacing to EdgeInsets.
func edgeInsets(from spacing: Spacing?) -> EdgeInsets {
    guard let s = spacing else { return EdgeInsets() }
    return EdgeInsets(
        top: CGFloat(s.top ?? 0),
        leading: CGFloat(s.start ?? 0),
        bottom: CGFloat(s.bottom ?? 0),
        trailing: CGFloat(s.end ?? 0)
    )
}

/// Resolve a BackgroundUnion to a SwiftUI Color.
func resolveBackground(_ bg: BackgroundUnion?) -> Color {
    guard let bg = bg else { return .clear }
    switch bg {
    case .string(let hex):
        return color(from: hex) ?? .clear
    case .background(let bgObj):
        if let colors = bgObj.colors, let first = colors.first {
            return color(from: first) ?? .clear
        }
        return .clear
    }
}

// MARK: - Badge alignment mapping

func swiftUIAlignment(from badgeAlignment: BadgeAlignment?) -> SwiftUI.Alignment {
    switch badgeAlignment {
    case .topStart: return .topLeading
    case .topCenter: return .top
    case .topEnd: return .topTrailing
    case .centerStart: return .leading
    case .center: return .center
    case .centerEnd: return .trailing
    case .bottomStart: return .bottomLeading
    case .bottomCenter: return .bottom
    case .bottomEnd: return .bottomTrailing
    case .none: return .topTrailing
    }
}

// MARK: - Monospaced digits modifier

extension View {
    @ViewBuilder
    func applyMonospacedDigits(_ enabled: Bool) -> some View {
        if enabled {
            self.monospacedDigit()
        } else {
            self
        }
    }
}

// MARK: - Shadow modifier

struct ShadowModifier: ViewModifier {
    let shadow: Shadow?

    func body(content: Content) -> some View {
        if let s = shadow {
            content.shadow(
                color: color(from: s.color) ?? Color.black.opacity(0.08),
                radius: CGFloat(s.radius ?? 4),
                x: CGFloat(s.offsetX ?? 0),
                y: CGFloat(s.offsetY ?? 2)
            )
        } else {
            content
        }
    }
}

extension View {
    func applyShadow(_ shadow: Shadow?) -> some View {
        modifier(ShadowModifier(shadow: shadow))
    }
}

// MARK: - Badge overlay modifier

struct BadgeOverlayModifier: ViewModifier {
    let badge: Badge?
    let screenState: ScreenState
    let onAction: (Action) -> Void

    func body(content: Content) -> some View {
        if let badge = badge {
            content.overlay(alignment: swiftUIAlignment(from: badge.alignment)) {
                AtomicRouter(element: badge.element, screenState: screenState, onAction: onAction, depth: 0)
            }
        } else {
            content
        }
    }
}

extension View {
    func applyBadge(_ badge: Badge?, screenState: ScreenState, onAction: @escaping (Action) -> Void) -> some View {
        modifier(BadgeOverlayModifier(badge: badge, screenState: screenState, onAction: onAction))
    }
}

// MARK: - Action tap modifier

struct ActionTapModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void

    func body(content: Content) -> some View {
        if let tapActions = actions?.filter({ $0.trigger == .onTap }), !tapActions.isEmpty {
            content.onTapGesture {
                for action in tapActions {
                    onAction(action)
                }
            }
        } else {
            content
        }
    }
}

extension View {
    func applyActions(_ actions: [Action]?, onAction: @escaping (Action) -> Void) -> some View {
        modifier(ActionTapModifier(actions: actions, onAction: onAction))
    }

    /// Applies both tap and visibility action handlers in one chainable call.
    /// ADR-009: 50% visibility threshold. Dwell + dedup are handled by
    /// ``ImpressionTracker`` inside the dispatcher.
    func applyActionTriggers(_ actions: [Action]?, onAction: @escaping (Action) -> Void) -> some View {
        self
            .applyActions(actions, onAction: onAction)
            .modifier(VisibilityActionModifier(actions: actions, onAction: onAction))
    }
}

// MARK: - Visibility action modifier

struct VisibilityActionModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void

    func body(content: Content) -> some View {
        let visibleActions = (actions ?? []).filter { $0.trigger == .onVisible }
        if visibleActions.isEmpty {
            content
        } else {
            // iOS 17 fallback: `.onAppear` inside a `LazyVStack` fires when
            // the child first enters the viewport, which is close enough to
            // our 50% visibility intent for `onVisible` triggers. iOS 18+
            // can be upgraded to `.onScrollVisibilityChange(threshold:)`.
            content.onAppear {
                for action in visibleActions { onAction(action) }
            }
        }
    }
}
