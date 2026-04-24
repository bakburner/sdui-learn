import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicRouter")

private let maxDepth = 6

/// Recursive dispatcher for atomic UI elements. Enforces the atomic-tree
/// performance contract: max depth 6, max children 20, max nodes 50.
/// Unknown element types are silently skipped with a warning log — same
/// shape as the Android and web atomic routers.
///
/// Box-model concerns (margin, padding, background, cornerRadius, shadow,
/// border, opacity, width, height, fillWidth, badge, variant chrome) are
/// applied uniformly by `AtomicBoxModifier` inside each primitive
/// renderer, so this router is dispatch-only.
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
        }
    }

    @ViewBuilder
    private func routeElement() -> some View {
        switch element.type {
        case "Container":
            AtomicContainerView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case "Text":
            AtomicTextView(element: element, screenState: screenState, onAction: onAction)

        case "Image":
            AtomicImageView(element: element, screenState: screenState, onAction: onAction)

        case "Button":
            AtomicButtonView(element: element, screenState: screenState, onAction: onAction)

        case "Spacer":
            AtomicSpacerView(element: element)

        case "Divider":
            AtomicDividerView(element: element)

        case "ScrollContainer":
            AtomicScrollContainerView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case "Conditional":
            AtomicConditionalView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case "DisplayGrid":
            AtomicDisplayGridView(element: element)

        case "SectionSlot":
            AtomicSectionSlotView(element: element, screenState: screenState, onAction: onAction)

        case "LiveClock":
            AtomicLiveClockView(element: element)

        default:
            EmptyView()
                .onAppear {
                    logger.warning("skipping unknown atomic element type=\(element.type, privacy: .public) id=\(element.id ?? "(nil)", privacy: .public)")
                }
        }
    }
}
