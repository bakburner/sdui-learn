import SwiftUI

/// Renders a Container atomic element — row or column layout with children.
/// The box model (margin, padding, bg, cornerRadius, shadow, border,
/// badge, width/height/fillWidth, opacity, variant chrome) is applied
/// uniformly by `AtomicBoxModifier`; this renderer only owns its flex
/// layout (HStack / VStack with gap + alignment).
struct AtomicContainerView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        let isRow = element.direction == .row
        let gap = CGFloat(element.gap ?? 0)
        let resolvedAspectRatio = element.aspectRatio.map { CGFloat($0) }

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
        .modifier(ContainerAspectRatioModifier(aspectRatio: resolvedAspectRatio))
        .applyActionTriggers(element.actions, onAction: onAction)
        .sduiAccessibility(element.accessibility)
        .atomicBox(element, screenState: screenState, onAction: onAction)
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

/// Applies aspectRatio to a Container when the server sends one.
private struct ContainerAspectRatioModifier: ViewModifier {
    let aspectRatio: CGFloat?

    func body(content: Content) -> some View {
        if let ratio = aspectRatio {
            content.aspectRatio(ratio, contentMode: .fit)
        } else {
            content
        }
    }
}
