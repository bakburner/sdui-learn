import SwiftUI

/// AtomicBoxModifier — the single site for every AtomicElement's box model
/// on the iOS client.
///
/// Every primitive renderer ends its modifier chain with
/// `.atomicBox(element, ...)` so margin / padding / background /
/// cornerRadius / shadow / opacity / width / height /
/// fillWidth / variant chrome / badge overlay live in exactly one place
/// rather than being re-implemented per primitive.
///
/// The rendering order is fixed so every primitive produces the same
/// box-model shape on the wire:
///
///     margin box  (outer)
///       └─ opacity
///            └─ shadow + background + cornerRadius
///                 └─ padding
///                      └─ content (inner)
///
/// Rules:
///   - `padding` lives *inside* the variant chrome so bg + corner clip
///     extends to the padded frame.
///   - `width` / `height` / `fillWidth` size the padded frame so fixed
///     dimensions are the outer-box dimensions (border-box semantics).
///   - `margin` is the outermost layer so sibling-to-sibling spacing is
///     untouched by the element's own bg / clip.
///   - `variant` is resolved once here and merged with inline
///     background / cornerRadius / shadow per the variant's
///     `overrideMatrix`.
///
/// Accessibility labels and action triggers are *not* handled here —
/// they remain primitive-specific so each renderer can supply its own
/// semantically appropriate fallback (image `alt`, button `label`,
/// text content) and decide whether taps go through a tap modifier or
/// a native control (e.g. SwiftUI `Button`).
struct AtomicBoxModifier: ViewModifier {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void

    func body(content: Content) -> some View {
        let spec = element.type == "Container"
            ? ContainerVariantResolver.resolve(element.variant)
            : nil
        let fixedWidth = element.width.map { CGFloat($0) }
        let fixedHeight = element.height.map { CGFloat($0) }
        let shouldFillWidth = fixedWidth == nil && element.fillWidth == true
        let frameAlignment = frameAlignmentForColumn(for: element)

        return content
            .padding(edgeInsets(from: element.padding))
            .frame(width: fixedWidth, height: fixedHeight)
            .frame(
                maxWidth: shouldFillWidth ? .infinity : nil,
                alignment: shouldFillWidth ? frameAlignment : .center
            )
            .applyContainerVariant(
                spec: spec,
                variantName: element.variant,
                inlineBackground: element.background,
                inlineCornerRadius: element.cornerRadius,
                inlineCornerRadii: element.cornerRadii,
                inlineShadow: element.shadow
            )
            .applyBadge(element.badge, screenState: screenState, onAction: onAction)
            .opacity(element.opacity ?? 1)
            .padding(edgeInsets(from: element.margin))
    }

    private func frameAlignmentForColumn(for element: AtomicElement) -> SwiftUI.Alignment {
        if element.direction == .row { return .center }
        switch element.crossAlignment {
        case .center: return .center
        case .end: return .trailing
        case .start: return .leading
        default: return .leading
        }
    }
}

extension View {
    /// Applies the unified AtomicElement box model to this view. Every
    /// atomic primitive renderer ends its view builder with this modifier
    /// so the box-model shape is uniform across primitives.
    func atomicBox(
        _ element: AtomicElement,
        screenState: ScreenState,
        onAction: @escaping (Action) -> Void
    ) -> some View {
        modifier(AtomicBoxModifier(element: element, screenState: screenState, onAction: onAction))
    }
}
