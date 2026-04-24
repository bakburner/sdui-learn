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

    var body: some View {
        Button(action: handleTap) {
            HStack(spacing: 8) {
                if let icon = element.icon,
                   let symbol = IconTokenResolver.shared.resolve(icon) {
                    Image(systemName: symbol)
                }
                if let label = element.label {
                    Text(label)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
        }
        .buttonStyle(SduiButtonStyle(variant: resolvedVariant))
        .disabled(element.disabled ?? false)
        .modifier(VisibilityActionModifier(actions: element.actions, onAction: onAction))
        .sduiAccessibility(element.accessibility, fallbackLabel: element.label)
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    private var resolvedVariant: ButtonVariant {
        guard let raw = element.variant else { return .primary }
        if let parsed = ButtonVariant(rawValue: raw) { return parsed }
        logger.debug("variant_resolver_missing: variant=\(raw, privacy: .public) elementId=\(element.id ?? "nil", privacy: .public)")
        return .primary
    }

    private func handleTap() {
        guard let actions = element.actions else { return }
        for action in actions where action.trigger == .onTap {
            onAction(action)
        }
    }
}

struct SduiButtonStyle: ButtonStyle {
    let variant: ButtonVariant

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundColor(foregroundColor)
            .background(backgroundColor.opacity(configuration.isPressed ? 0.7 : 1.0))
            .cornerRadius(8)
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
