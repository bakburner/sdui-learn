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
