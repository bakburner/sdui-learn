import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicButton")

/// Renders a Button atomic element. The button's own chrome (padding,
/// bg, border, radius) is provided by `SduiButtonStyle` per-variant;
/// element-level margin / opacity / fillWidth / background / shadow /
/// badge are applied by `AtomicBoxModifier` via `.atomicBox(...)`.
struct AtomicButtonView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void

    @Environment(\.batchActionExecutor) private var batchExecutor
    @Environment(\.compositeContent) private var compositeContent
    @Environment(\.isInFormActionContext) private var isInFormActionContext

    var body: some View {
        Button(action: handleTap) {
            HStack(spacing: 8) {
                if let icon = element.icon,
                   let symbol = IconTokenResolver.shared.resolve(icon) {
                    Image(systemName: symbol)
                }
                if let label = resolvedLabel, !label.isEmpty {
                    Text(label)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .buttonStyle(SduiButtonStyle(
            variant: resolvedVariant,
            inlineForeground: resolvedInlineColor,
            inlineBackground: resolvedInlineBg
        ))
        .disabled(element.disabled ?? false)
        .applyActionTriggers(
            element.actions,
            onAction: onAction,
            supportsFocus: true,
            supportsSubmit: isInFormActionContext
        )
        .sduiAccessibility(element.accessibility, fallbackLabel: element.label)
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    /// Resolve `label` from `bindRef` when present, falling back to the
    /// inline `label`. Lets composers rebind CTA copy (e.g. "Subscribe
    /// for $9.99" → "Resume subscription") without rewriting the ui tree.
    private var resolvedLabel: String? {
        if let bound = BindRefResolver.resolveString(bindRef: element.bindRef, in: compositeContent) {
            return bound
        }
        return element.label
    }

    @Environment(\.colorScheme) private var colorScheme

    /// Inline text color override from `element.color` (ColorToken).
    private var resolvedInlineColor: Color? {
        ColorTokenResolver.resolve(element.color, colorScheme: colorScheme)
    }

    /// Inline background override from `element.background` (solid ColorToken case).
    private var resolvedInlineBg: Color? {
        guard let bg = element.background else { return nil }
        switch bg {
        case .string(let token):
            return ColorTokenResolver.resolve(token, colorScheme: colorScheme)
        case .background(let bgObj):
            if let colors = bgObj.colors, let first = colors.first {
                return ColorTokenResolver.resolve(first, colorScheme: colorScheme)
            }
            return nil
        }
    }

    private var resolvedVariant: ButtonVariant {
        guard let raw = element.variant else { return .primary }
        if let parsed = ButtonVariant(rawValue: raw) { return parsed }
        logger.debug("variant_resolver_missing: variant=\(raw, privacy: .public) elementId=\(element.id ?? "nil", privacy: .public)")
        return .primary
    }

    private func handleTap() {
        AtomicActionTriggerDispatcher.dispatch(
            trigger: .onActivate,
            actions: element.actions,
            onAction: onAction,
            batchExecutor: batchExecutor
        )
    }
}

struct SduiButtonStyle: ButtonStyle {
    let variant: ButtonVariant
    var inlineForeground: Color? = nil
    var inlineBackground: Color? = nil

    func makeBody(configuration: Configuration) -> some View {
        let bg = inlineBackground ?? backgroundColor
        return configuration.label
            .foregroundColor(inlineForeground ?? foregroundColor)
            .background(bg.opacity(configuration.isPressed ? 0.7 : 1.0))
            .cornerRadius(variant == .text || variant == .tertiary ? 0 : 8)
    }

    private var foregroundColor: Color {
        switch variant {
        case .primary: return .white
        case .secondary: return .accentColor
        case .tertiary: return .accentColor
        case .text: return .accentColor
        }
    }

    private var backgroundColor: Color {
        switch variant {
        case .primary: return .accentColor
        case .secondary: return .accentColor.opacity(0.15)
        case .tertiary: return .clear
        case .text: return .clear
        }
    }
}
