import SwiftUI

/// Renders a ScrollContainer atomic element — scrollable child list.
/// The scroll viewport owns only its scroll layout (axis, gap, indicator
/// visibility); margin / padding / bg / cornerRadius / shadow / opacity
/// live on `AtomicBoxModifier` via `.atomicBox(...)`.
struct AtomicScrollContainerView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        let isHorizontal = element.direction == .row
        let gap = CGFloat(element.gap ?? 0)

        ScrollView(isHorizontal ? .horizontal : .vertical, showsIndicators: element.showIndicators ?? false) {
            if isHorizontal {
                HStack(spacing: gap) {
                    children
                }
            } else {
                VStack(spacing: gap) {
                    children
                }
            }
        }
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
}
