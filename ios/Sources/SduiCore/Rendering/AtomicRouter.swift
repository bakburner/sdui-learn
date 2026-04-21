import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicRouter")

private let maxDepth = 6

/// Recursive dispatcher for atomic UI elements. Enforces the atomic-tree
/// performance contract: max depth 6, max children 20, max nodes 50.
struct AtomicRouter: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        if depth > maxDepth {
            let _ = logger.warning("Max tree depth exceeded at depth \(depth) — skipping element")
            EmptyView()
        } else {
            routeElement()
                .opacity(element.opacity ?? 1)
        }
    }

    @ViewBuilder
    private func routeElement() -> some View {
        switch element.type {
        case .container:
            AtomicContainerView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case .text:
            AtomicTextView(element: element)

        case .image:
            AtomicImageView(element: element, screenState: screenState, onAction: onAction)

        case .button:
            AtomicButtonView(element: element, screenState: screenState, onAction: onAction)

        case .spacer:
            AtomicSpacerView(element: element)

        case .divider:
            AtomicDividerView(element: element)

        case .scrollContainer:
            AtomicScrollContainerView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case .conditional:
            AtomicConditionalView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case .displayGrid:
            AtomicDisplayGridView(element: element)

        case .sectionSlot:
            AtomicSectionSlotView(element: element, screenState: screenState, onAction: onAction)
        }
    }
}
