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
                .applyActionTriggers(element.actions, onAction: onAction, supportsLongPress: true)
                .sduiAccessibility(element.accessibility, fallbackLabel: content)
                .atomicBox(element, screenState: screenState, onAction: onAction)
        }
    }

    private var shouldFillWidth: Bool {
        element.textAlign != nil || element.widthMode == .fill
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

    /// Resolve a wire `TextVariant` to a SwiftUI `Font` via the bundled typography
    /// registry (`LayoutTokenResolver.typographyForVariant`). The variant enum on the
    /// wire is a shorthand for the full token name `nba.typography.<variant>`; sizing
    /// and base weight come from `schema/typography-tokens.json`, ensuring iOS,
    /// Android, and web all render the same variant at the same size.
    ///
    /// Element-level `weight` (applied via `.fontWeight(...)`) still overrides the
    /// category's base weight when the composer needs an emphasis variant.
    private func font(for variant: TextVariant?) -> Font {
        guard let variant else { return .body }
        guard let spec = LayoutTokenResolver.typographyForVariant(variant.rawValue) else {
            logger.debug("typography_token_missing: variant=\(variant.rawValue, privacy: .public)")
            return .body
        }
        return .system(size: CGFloat(spec.size), weight: fontWeight(forCategoryWeight: spec.weight))
    }

    /// Map an integer registry weight (100–900, plus custom values from variable-font
    /// axes) to the nearest SwiftUI `Font.Weight`.
    private func fontWeight(forCategoryWeight w: Int) -> Font.Weight {
        switch w {
        case ..<200: return .ultraLight
        case ..<300: return .thin
        case ..<400: return .light
        case ..<500: return .regular
        case ..<600: return .medium
        case ..<700: return .semibold
        case ..<800: return .bold
        case ..<900: return .heavy
        default:     return .black
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
