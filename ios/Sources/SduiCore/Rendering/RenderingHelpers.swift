import Kingfisher
import SwiftUI
import os

private let actionTriggerLogger = Logger(subsystem: "com.nba.sdui", category: "AtomicActionTriggers")

enum AtomicActionTriggerLogProbe {
    static var debugSink: ((String) -> Void)?

    static func debug(_ message: String) {
        actionTriggerLogger.debug("\(message, privacy: .public)")
        debugSink?(message)
    }
}

// MARK: - Shared helpers used across atomic renderers

/// Convert schema Spacing to EdgeInsets.
func edgeInsets(from spacing: Spacing?, formFactor: String = RequestEnvelope.currentFormFactor) -> EdgeInsets {
    guard let s = spacing else { return EdgeInsets() }
    return EdgeInsets(
        top: LayoutTokenResolver.cgFloat(s.top, formFactor: formFactor),
        leading: LayoutTokenResolver.cgFloat(s.start, formFactor: formFactor),
        bottom: LayoutTokenResolver.cgFloat(s.bottom, formFactor: formFactor),
        trailing: LayoutTokenResolver.cgFloat(s.end, formFactor: formFactor)
    )
}

/// Build a SwiftUI view that renders a `BackgroundUnion` value —
/// solid color, linear gradient, or remote image — suitable for use
/// as `.background { backgroundView(for: bg, colorScheme: cs) }` on
/// any view.
///
/// All three shapes in the schema's `Background` union are handled:
///   • string → token or hex, resolved to a solid `Color`.
///   • object with `colors` (≥2) → `LinearGradient` with direction.
///   • object with `colors` (1)   → solid color fallback.
///   • object with `imageURL`    → cached remote image scaled to fill.
///
/// Used by `SectionContainer` for section-level surfaces and by the
/// atomic `ContainerVariantResolver` for inline backgrounds, so
/// gradient fidelity is consistent everywhere.
@MainActor
@ViewBuilder
func backgroundView(for bg: BackgroundUnion?, colorScheme: ColorScheme) -> some View {
    switch bg {
    case .none:
        Color.clear
    case .some(.string(let value)):
        ColorTokenResolver.resolve(value, colorScheme: colorScheme) ?? Color.clear
    case .some(.background(let bgObj)):
        if let imageUrl = bgObj.imageURL, let url = URL(string: imageUrl) {
            KFImage(url)
                .placeholder { Color.clear }
                .resizable()
                .scaledToFill()
        } else if let colors = bgObj.colors, colors.count > 1 {
            LinearGradient(
                gradient: Gradient(colors: colors.map {
                    ColorTokenResolver.resolve($0, colorScheme: colorScheme) ?? .clear
                }),
                startPoint: backgroundGradientStart(bgObj.direction),
                endPoint: backgroundGradientEnd(bgObj.direction)
            )
        } else if let first = bgObj.colors?.first {
            ColorTokenResolver.resolve(first, colorScheme: colorScheme) ?? Color.clear
        } else {
            Color.clear
        }
    }
}

func backgroundGradientStart(_ direction: Direction?) -> UnitPoint {
    switch direction {
    case .horizontal: return .leading
    case .diagonal: return .topLeading
    case .vertical, .none: return .top
    }
}

func backgroundGradientEnd(_ direction: Direction?) -> UnitPoint {
    switch direction {
    case .horizontal: return .trailing
    case .diagonal: return .bottomTrailing
    case .vertical, .none: return .bottom
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

    @Environment(\.colorScheme) private var colorScheme

    func body(content: Content) -> some View {
        if let s = shadow {
            content.shadow(
                color: ColorTokenResolver.resolve(s.color, colorScheme: colorScheme)
                    ?? Color.black.opacity(0.08),
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

private func dispatchAtomicActions(
    _ actions: [Action],
    onAction: @escaping (Action) -> Void,
    batchExecutor: (([Action]) -> Void)?
) {
    guard !actions.isEmpty else { return }
    if let batchExecutor {
        batchExecutor(actions)
    } else {
        for action in actions {
            onAction(action)
        }
    }
}

enum AtomicActionTriggerDispatcher {
    static func dispatch(
        trigger: ActionTrigger,
        actions: [Action]?,
        onAction: @escaping (Action) -> Void,
        batchExecutor: (([Action]) -> Void)?
    ) {
        if let reason = AtomicActionTriggerRegistry.notHosted[trigger] {
            AtomicActionTriggerLogProbe.debug(
                "atomic trigger not hosted at the element level: trigger=\(trigger.rawValue) reason=\(reason)"
            )
            return
        }

        let matchingActions = AtomicActionTriggerRegistry.actions(for: trigger, in: actions)
        dispatchAtomicActions(matchingActions, onAction: onAction, batchExecutor: batchExecutor)
    }

    static func logUnhostedTriggers(in actions: [Action]?) {
        let triggers = Set((actions ?? []).map(\.trigger))
        for trigger in triggers.sorted(by: { $0.rawValue < $1.rawValue }) {
            guard let reason = AtomicActionTriggerRegistry.notHosted[trigger] else { continue }
            AtomicActionTriggerLogProbe.debug(
                "atomic trigger not hosted at the element level: trigger=\(trigger.rawValue) reason=\(reason)"
            )
        }
    }
}

private func logNotHostedAtomicTriggers(_ actions: [Action]?) {
    AtomicActionTriggerDispatcher.logUnhostedTriggers(in: actions)
}

struct UnsupportedAtomicTriggerLogger: ViewModifier {
    let actions: [Action]?
    @State private var didLog = false

    func body(content: Content) -> some View {
        content.onAppear {
            guard !didLog else { return }
            didLog = true
            logNotHostedAtomicTriggers(actions)
        }
    }
}

struct ActionTapModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void
    @Environment(\.batchActionExecutor) private var batchExecutor

    func body(content: Content) -> some View {
        if !AtomicActionTriggerRegistry.actions(for: .onActivate, in: actions).isEmpty {
            content.onTapGesture {
                AtomicActionTriggerDispatcher.dispatch(
                    trigger: .onActivate,
                    actions: actions,
                    onAction: onAction,
                    batchExecutor: batchExecutor
                )
            }
        } else {
            content
        }
    }
}

struct LongPressActionModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void
    let isEnabled: Bool

    @Environment(\.batchActionExecutor) private var batchExecutor

    func body(content: Content) -> some View {
        if isEnabled, !AtomicActionTriggerRegistry.actions(for: .onLongPress, in: actions).isEmpty {
            content.onLongPressGesture(minimumDuration: 0.5) {
                AtomicActionTriggerDispatcher.dispatch(
                    trigger: .onLongPress,
                    actions: actions,
                    onAction: onAction,
                    batchExecutor: batchExecutor
                )
            }
        } else {
            content
        }
    }
}

struct FocusActionModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void
    let isEnabled: Bool

    @Environment(\.batchActionExecutor) private var batchExecutor
    @FocusState private var isFocused: Bool

    func body(content: Content) -> some View {
        if isEnabled {
            content
                .focused($isFocused)
                .onChange(of: isFocused) { _, focused in
                    let trigger: ActionTrigger = focused ? .onFocus : .onBlur
                    AtomicActionTriggerDispatcher.dispatch(
                        trigger: trigger,
                        actions: actions,
                        onAction: onAction,
                        batchExecutor: batchExecutor
                    )
                }
        } else {
            content
        }
    }
}

struct SubmitActionModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void
    let isEnabled: Bool

    @Environment(\.batchActionExecutor) private var batchExecutor

    func body(content: Content) -> some View {
        if isEnabled {
            content.onSubmit {
                AtomicActionTriggerDispatcher.dispatch(
                    trigger: .onSubmit,
                    actions: actions,
                    onAction: onAction,
                    batchExecutor: batchExecutor
                )
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
    func applyActionTriggers(
        _ actions: [Action]?,
        onAction: @escaping (Action) -> Void,
        supportsLongPress: Bool = false,
        supportsFocus: Bool = false,
        supportsSubmit: Bool = false
    ) -> some View {
        self
            .modifier(UnsupportedAtomicTriggerLogger(actions: actions))
            .applyActions(actions, onAction: onAction)
            .modifier(LongPressActionModifier(actions: actions, onAction: onAction, isEnabled: supportsLongPress))
            .modifier(FocusActionModifier(actions: actions, onAction: onAction, isEnabled: supportsFocus))
            .modifier(SubmitActionModifier(actions: actions, onAction: onAction, isEnabled: supportsSubmit))
            .modifier(VisibilityActionModifier(actions: actions, onAction: onAction))
    }
}

// MARK: - Visibility action modifier

struct VisibilityActionModifier: ViewModifier {
    let actions: [Action]?
    let onAction: (Action) -> Void

    @Environment(\.batchActionExecutor) private var batchExecutor

    func body(content: Content) -> some View {
        if AtomicActionTriggerRegistry.actions(for: .onVisible, in: actions).isEmpty {
            content
        } else {
            // iOS 17 fallback: `.onAppear` inside a `LazyVStack` fires when
            // the child first enters the viewport, which is close enough to
            // our 50% visibility intent for `onVisible` triggers. iOS 18+
            // can be upgraded to `.onScrollVisibilityChange(threshold:)`.
            content.onAppear {
                AtomicActionTriggerDispatcher.dispatch(
                    trigger: .onVisible,
                    actions: actions,
                    onAction: onAction,
                    batchExecutor: batchExecutor
                )
            }
        }
    }
}
