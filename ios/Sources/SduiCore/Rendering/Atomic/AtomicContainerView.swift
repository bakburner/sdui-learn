import SwiftUI

/// Renders a Container atomic element — row or column layout with children.
struct AtomicContainerView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        let isRow = element.direction == .row
        let gap = CGFloat(element.gap ?? 0)
        let padding = edgeInsets(from: element.padding)
        let variantSpec = ContainerVariantResolver.resolve(element.variant)
        let fixedWidth = element.width.map { CGFloat($0) }
        let fixedHeight = element.height.map { CGFloat($0) }
        // Wire-level fillWidth is honored here on iOS to match web
        // (`width: 100%`) and Android (`Modifier.fillMaxWidth()`). Without
        // this, a row child that needs to claim "the remaining horizontal
        // space" (e.g. the middle title column in a three-column list row
        // with a fixed-width label on the left and a badge on the right)
        // collapses to intrinsic width and the row's space distributes
        // unpredictably. Explicit `width` wins over fillWidth so a
        // 200pt card is still 200pt even if marked fillWidth upstream.
        let shouldFillWidth = fixedWidth == nil && element.fillWidth == true

        Group {
            if isRow {
                HStack(alignment: crossAlign, spacing: gap) {
                    children
                }
            } else {
                VStack(alignment: crossHAlign, spacing: gap) {
                    children
                }
            }
        }
        .padding(padding)
        .frame(width: fixedWidth, height: fixedHeight)
        .frame(maxWidth: shouldFillWidth ? .infinity : nil,
               alignment: shouldFillWidth && !isRow ? frameAlignmentForColumn : .center)
        .applyContainerVariant(
            spec: variantSpec,
            variantName: element.variant,
            inlineBackground: element.background,
            inlineCornerRadius: element.cornerRadius,
            inlineCornerRadii: element.cornerRadii,
            inlineShadow: element.shadow
        )
        .applyBadge(element.badge, screenState: screenState, onAction: onAction)
        .applyActionTriggers(element.actions, onAction: onAction)
        .sduiAccessibility(element.accessibility)
    }

    /// For a fill-width column we want the inner content left-aligned by
    /// default (leading) unless the element declares otherwise via
    /// `crossAlignment`. A row's fill-width frame uses center alignment
    /// because the HStack itself positions its children.
    private var frameAlignmentForColumn: SwiftUI.Alignment {
        switch element.crossAlignment {
        case .center: return .center
        case .end: return .trailing
        case .start: return .leading
        default: return .leading
        }
    }

    @ViewBuilder
    private var children: some View {
        if let kids = element.children {
            ForEach(Array(kids.enumerated()), id: \.offset) { _, child in
                AtomicRouter(element: child, screenState: screenState, onAction: onAction, depth: depth)
            }
        }
    }

    private var crossAlign: VerticalAlignment {
        switch element.crossAlignment {
        case .center: return .center
        case .end: return .bottom
        case .start: return .top
        default: return .center
        }
    }

    private var crossHAlign: HorizontalAlignment {
        switch element.crossAlignment {
        case .center: return .center
        case .end: return .trailing
        case .start: return .leading
        default: return .leading
        }
    }
}
