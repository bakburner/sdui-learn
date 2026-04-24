import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicText")

/// Renders a Text atomic element with typography variants.
///
/// Typography (font / color / alignment / line clamp) lives here; the
/// box model (margin / padding / bg / cornerRadius / shadow / border /
/// opacity / fillWidth / badge) is applied uniformly by
/// `AtomicBoxModifier` via `.atomicBox(...)`.
struct AtomicTextView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.compositeContent) private var compositeContent

    var body: some View {
        if let content = resolvedContent {
            Text(content)
                .font(font(for: resolvedVariant))
                .fontWeight(weight(for: element.weight))
                .foregroundColor(ColorTokenResolver.resolve(element.color, colorScheme: colorScheme))
                .lineLimit(element.maxLines)
                .multilineTextAlignment(resolvedTextAlignment)
                .applyMonospacedDigits(element.monospacedDigits == true)
                // Text-specific framing: only apply a fill-width frame when
                // the server signalled that the text must occupy the
                // available width (`textAlign` or `fillWidth`). Without
                // either, the text takes its intrinsic width so siblings
                // in a Row lay out naturally (e.g. ✓ + label feature row).
                .modifier(TextFrameModifier(
                    fillWidth: shouldFillWidth,
                    alignment: resolvedFrameAlignment
                ))
                .applyActionTriggers(element.actions, onAction: onAction)
                .sduiAccessibility(element.accessibility, fallbackLabel: content)
                .atomicBox(element, screenState: screenState, onAction: onAction)
        }
    }

    private var shouldFillWidth: Bool {
        element.textAlign != nil || element.fillWidth == true
    }

    /// Resolve `content` from `bindRef` when present, falling back to the
    /// inline `content` property. A leaf with a bindRef but no matching
    /// `data.content` entry falls back to its inline value rather than
    /// rendering nothing — this keeps the first paint usable while the
    /// first real-time update is in flight.
    private var resolvedContent: String? {
        if let bound = BindRefResolver.resolveString(bindRef: element.bindRef, in: compositeContent) {
            return bound
        }
        return element.content
    }

    private var resolvedVariant: TextVariant? {
        guard let raw = element.variant else { return nil }
        if let parsed = TextVariant(rawValue: raw) { return parsed }
        logger.debug("variant_resolver_missing: variant=\(raw, privacy: .public) elementId=\(element.id ?? "nil", privacy: .public)")
        return nil
    }

    private var resolvedTextAlignment: TextAlignment {
        if let ta = element.textAlign {
            switch ta {
            case .center: return .center
            case .end: return .trailing
            case .start: return .leading
            }
        }
        return textAlignment(from: element.alignment)
    }

    private var resolvedFrameAlignment: SwiftUI.Alignment {
        if let ta = element.textAlign {
            switch ta {
            case .center: return .center
            case .end: return .trailing
            case .start: return .leading
            }
        }
        return frameAlignment(from: element.alignment)
    }

    /// Map schema TextVariant to a SwiftUI Font. Sizes/weights mirror
    /// `web/src/components/atomic/AtomicText.tsx` and Android's
    /// `AtomicText.kt::mapTypographyVariant` so a section composed on the
    /// server renders at visually equivalent sizes across platforms.
    private func font(for variant: TextVariant?) -> Font {
        switch variant {
        case .displayLarge:  return .system(size: 57, weight: .heavy)
        case .displayMedium: return .system(size: 45, weight: .heavy)
        case .displaySmall:  return .system(size: 36, weight: .bold)
        case .headlineLarge:  return .system(size: 32, weight: .bold)
        case .headlineMedium: return .system(size: 28, weight: .bold)
        case .headlineSmall:  return .system(size: 24, weight: .bold)
        case .titleLarge:  return .system(size: 22, weight: .medium)
        case .titleMedium: return .system(size: 16, weight: .medium)
        case .titleSmall:  return .system(size: 14, weight: .medium)
        case .bodyLarge:  return .system(size: 16, weight: .regular)
        case .bodyMedium: return .system(size: 14, weight: .regular)
        case .bodySmall:  return .system(size: 12, weight: .regular)
        case .labelLarge:  return .system(size: 14, weight: .medium)
        case .labelMedium: return .system(size: 12, weight: .medium)
        case .labelSmall:  return .system(size: 11, weight: .medium)
        case .score:    return .system(size: 48, weight: .bold, design: .rounded)
        case .none:     return .body
        }
    }

    private func weight(for weight: TextWeight?) -> Font.Weight {
        switch weight {
        case .bold: return .bold
        case .semiBold: return .semibold
        case .medium: return .medium
        case .regular: return .regular
        case .none: return .regular
        }
    }

    private func textAlignment(from alignment: Alignment?) -> TextAlignment {
        switch alignment {
        case .center: return .center
        case .end: return .trailing
        case .start: return .leading
        default: return .leading
        }
    }

    private func frameAlignment(from alignment: Alignment?) -> SwiftUI.Alignment {
        switch alignment {
        case .center: return .center
        case .end: return .trailing
        default: return .leading
        }
    }
}

private struct TextFrameModifier: ViewModifier {
    let fillWidth: Bool
    let alignment: SwiftUI.Alignment

    @ViewBuilder
    func body(content: Content) -> some View {
        if fillWidth {
            content.frame(maxWidth: .infinity, alignment: alignment)
        } else {
            content
        }
    }
}
