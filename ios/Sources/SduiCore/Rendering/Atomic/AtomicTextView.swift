import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicText")

/// Renders a Text atomic element with typography variants.
struct AtomicTextView: View {
    let element: AtomicElement

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        if let content = element.content {
            Text(content)
                .font(font(for: resolvedVariant))
                .fontWeight(weight(for: element.weight))
                .foregroundColor(ColorTokenResolver.resolve(element.color, colorScheme: colorScheme))
                .lineLimit(element.maxLines)
                .multilineTextAlignment(resolvedTextAlignment)
                .applyMonospacedDigits(element.monospacedDigits == true)
                // Padding goes *inside* the flexible frame so the padded
                // text lays out within the parent's width instead of
                // overflowing it by the padding amount on fixed-width
                // parents (e.g. a 200pt content-rail card).
                .padding(edgeInsets(from: element.padding))
                .modifier(TextFrameModifier(
                    fillWidth: shouldFillWidth,
                    alignment: resolvedFrameAlignment
                ))
                .sduiAccessibility(element.accessibility, fallbackLabel: content)
        }
    }

    /// Text takes its intrinsic width by default so that siblings in a Row
    /// lay out naturally (e.g. `✓` + label feature row). A maxWidth: .infinity
    /// frame is only applied when the server has signalled that the text
    /// must occupy the available width — an explicit `textAlign` (centered
    /// or trailing alignment only makes sense inside a flexible frame) or an
    /// explicit `fillWidth` on the element. Without either signal, the text
    /// is sized to its content and the parent container decides layout.
    private var shouldFillWidth: Bool {
        element.textAlign != nil || element.fillWidth == true
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
        // Material3 display
        case .displayLarge:  return .system(size: 57, weight: .heavy)
        case .displayMedium: return .system(size: 45, weight: .heavy)
        case .displaySmall:  return .system(size: 36, weight: .bold)
        // Material3 headline
        case .headlineLarge:  return .system(size: 32, weight: .bold)
        case .headlineMedium: return .system(size: 28, weight: .bold)
        case .headlineSmall:  return .system(size: 24, weight: .bold)
        // Material3 title
        case .titleLarge:  return .system(size: 22, weight: .medium)
        case .titleMedium: return .system(size: 16, weight: .medium)
        case .titleSmall:  return .system(size: 14, weight: .medium)
        // Material3 body
        case .bodyLarge:  return .system(size: 16, weight: .regular)
        case .bodyMedium: return .system(size: 14, weight: .regular)
        case .bodySmall:  return .system(size: 12, weight: .regular)
        // Material3 label
        case .labelLarge:  return .system(size: 14, weight: .medium)
        case .labelMedium: return .system(size: 12, weight: .medium)
        case .labelSmall:  return .system(size: 11, weight: .medium)
        // Legacy semantic variants (pre-Material3 server output)
        case .heading1: return .largeTitle
        case .heading2: return .title
        case .heading3: return .title3
        case .body:     return .body
        case .caption:  return .caption
        case .label:    return .subheadline
        // NBA-specific
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
