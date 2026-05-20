import os
import SwiftUI

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicBoxModifier")

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
        let fixedWidth = element.width.map { LayoutTokenResolver.cgFloat($0) }
        let fixedHeight = element.height.map { LayoutTokenResolver.cgFloat($0) }

        let effectiveWidthMode = element.widthMode
        let effectiveHeightMode = element.heightMode

        // min/max constraints
        let minW: CGFloat? = element.minWidth.map { LayoutTokenResolver.cgFloat($0) }
        let maxW: CGFloat? = element.maxWidth.map { LayoutTokenResolver.cgFloat($0) }
        let minH: CGFloat? = element.minHeight.map { LayoutTokenResolver.cgFloat($0) }
        let maxH: CGFloat? = element.maxHeight.map { LayoutTokenResolver.cgFloat($0) }

        // Effective max from sizing mode (.fill → .infinity unless explicit max is set)
        let effectiveMaxWidth: CGFloat? = {
            switch effectiveWidthMode {
            case .fill where fixedWidth == nil: return maxW ?? .infinity
            default: return maxW
            }
        }()
        let effectiveMaxHeight: CGFloat? = {
            switch effectiveHeightMode {
            case .fill where fixedHeight == nil: return maxH ?? .infinity
            default: return maxH
            }
        }()

        // alignSelf: stretch fills the cross axis
        let stretchMaxWidth: CGFloat? = element.alignSelf == .stretch ? .infinity : nil
        let stretchMaxHeight: CGFloat? = element.alignSelf == .stretch ? .infinity : nil

        let resolvedMaxWidth = effectiveMaxWidth ?? stretchMaxWidth
        let resolvedMaxHeight = effectiveMaxHeight ?? stretchMaxHeight

        let frameAlignment = alignmentForSizingContext(for: element)

        // Effective backgrounds: plural field wins, else wrap singular, else empty.
        let effectiveBackgrounds: [BackgroundUnion] = element.backgrounds
            ?? element.background.map { [$0] }
            ?? []

        // Effective shadows: plural field wins, else wrap singular, else empty.
        let effectiveShadows: [Shadow] = element.shadows
            ?? element.shadow.map { [$0] }
            ?? []

        let primaryBackground = effectiveBackgrounds.first
        let primaryShadow = effectiveShadows.first
        let additionalBackgrounds = Array(effectiveBackgrounds.dropFirst())
        let additionalShadows = Array(effectiveShadows.dropFirst())

        return content
            .padding(edgeInsets(from: element.padding))
            .frame(width: fixedWidth, height: fixedHeight)
            .frame(
                minWidth: minW,
                maxWidth: resolvedMaxWidth,
                minHeight: minH,
                maxHeight: resolvedMaxHeight,
                alignment: frameAlignment
            )
            .modifier(AdditionalBackgroundsModifier(
                backgrounds: additionalBackgrounds,
                cornerRadius: element.cornerRadius,
                cornerRadii: element.cornerRadii
            ))
            .applyContainerVariant(
                spec: spec,
                variantName: element.variant,
                inlineBackground: primaryBackground,
                inlineCornerRadius: element.cornerRadius,
                inlineCornerRadii: element.cornerRadii,
                inlineShadow: primaryShadow
            )
            .modifier(AdditionalShadowsModifier(
                shadows: additionalShadows,
                cornerRadius: element.cornerRadius,
                cornerRadii: element.cornerRadii
            ))
            .applyBadge(element.badge, screenState: screenState, onAction: onAction)
            .opacity(element.opacity ?? 1)
            .padding(edgeInsets(from: element.margin))
    }

    private func alignmentForSizingContext(for element: AtomicElement) -> SwiftUI.Alignment {
        // alignSelf provides explicit alignment when element is filling
        if let alignSelf = element.alignSelf {
            switch alignSelf {
            case .start: return .leading
            case .center: return .center
            case .end: return .trailing
            case .stretch: return .center
            }
        }
        if element.direction == .row { return .center }
        switch element.crossAlignment {
        case .center: return .center
        case .end: return .trailing
        case .start: return .leading
        default: return .leading
        }
    }
}

// MARK: - Additional backgrounds modifier (index 1+ layers)

/// Applies additional background layers (beyond the primary handled by the variant resolver)
/// as a ZStack so they paint on top of the base fill but behind the element's content.
/// Applied before `applyContainerVariant` so the variant's corner clip clips them.
private struct AdditionalBackgroundsModifier: ViewModifier {
    let backgrounds: [BackgroundUnion]
    let cornerRadius: LayoutScalar?
    let cornerRadii: CornerRadii?

    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if backgrounds.isEmpty {
            content
        } else {
            // ZStack order: first subview is at the back, last is at the front.
            // backgrounds[0] here is element.backgrounds[1] (first additional layer),
            // which should paint on top of the base. Later items paint above it.
            content.background {
                ZStack {
                    ForEach(0..<backgrounds.count, id: \.self) { index in
                        backgroundView(for: backgrounds[index], colorScheme: colorScheme)
                    }
                }
            }
        }
    }
}

// MARK: - Additional shadows modifier (index 1+ layers)

/// Applies additional shadow layers beyond the primary handled by the variant resolver.
/// Drop shadows stack as `.shadow()` modifiers; inner shadows use a clipped overlay.
private struct AdditionalShadowsModifier: ViewModifier {
    let shadows: [Shadow]
    let cornerRadius: LayoutScalar?
    let cornerRadii: CornerRadii?

    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if shadows.isEmpty {
            content
        } else {
            content
                .modifier(RecursiveDropShadowModifier(
                    shadows: shadows.filter { ($0.type ?? .drop) == .drop },
                    colorScheme: colorScheme
                ))
                .modifier(InnerShadowsOverlayModifier(
                    shadows: shadows.filter { $0.type == .inner },
                    cornerRadius: cornerRadius,
                    cornerRadii: cornerRadii,
                    colorScheme: colorScheme
                ))
        }
    }
}

/// Recursively applies drop shadow modifiers (one `.shadow()` per layer).
private struct RecursiveDropShadowModifier: ViewModifier {
    let shadows: [Shadow]
    let colorScheme: ColorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if let first = shadows.first {
            content
                .shadow(
                    color: ColorTokenResolver.resolve(first.color, colorScheme: colorScheme)
                        ?? Color.black.opacity(0.08),
                    radius: CGFloat(first.radius ?? 4),
                    x: CGFloat(first.offsetX ?? 0),
                    y: CGFloat(first.offsetY ?? 2)
                )
                .modifier(RecursiveDropShadowModifier(
                    shadows: Array(shadows.dropFirst()),
                    colorScheme: colorScheme
                ))
        } else {
            content
        }
    }
}

/// Applies inner shadow layers as a clipped overlay.
/// SwiftUI lacks first-class inner shadow support; falls back to drop shadow with diagnostic.
private struct InnerShadowsOverlayModifier: ViewModifier {
    let shadows: [Shadow]
    let cornerRadius: LayoutScalar?
    let cornerRadii: CornerRadii?
    let colorScheme: ColorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        if shadows.isEmpty {
            content
        } else {
            // Platform fallback: inner shadows rendered as drop shadows (SwiftUI lacks
            // InsettableShape shadow inversion). Log once per render pass.
            content
                .modifier(RecursiveDropShadowModifier(
                    shadows: shadows,
                    colorScheme: colorScheme
                ))
                .onAppear {
                    logger.info("inner shadow type fell back to drop — SwiftUI platform limitation (\(shadows.count) layer(s))")
                }
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
