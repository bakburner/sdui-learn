import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicRouter")

private let maxDepth = 6

/// Recursive dispatcher for atomic UI elements. Enforces the atomic-tree
/// performance contract: max depth 6, max children 20, max nodes 50.
/// Unknown element types are silently skipped with a warning log — same
/// shape as the Android and web atomic routers.
///
/// `element.type` is a plain `String` on the wire. The iOS codegen
/// applies a post-processing step (see `codegen/generate.sh`) that
/// rewrites quicktype's inline-enum output for `AtomicElement.type` to
/// `String`, so this switch can use a `default:` branch for forward
/// compatibility the same way the other clients do.
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
                // Outer margin: applied after the routed element's own
                // background, padding, corner radius, and shadow, so it
                // behaves as sibling-to-sibling spacing rather than
                // interior padding. Padding the outside of an already-
                // styled view is SwiftUI's canonical way to express
                // margin (CSS/Compose convention).
                .padding(edgeInsets(from: element.margin))
        }
    }

    @ViewBuilder
    private func routeElement() -> some View {
        switch element.type {
        case "Container":
            AtomicContainerView(element: element, screenState: screenState, onAction: onAction, depth: depth + 1)

        case "Text":
            AtomicTextView(element: element)

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

        default:
            EmptyView()
                .onAppear {
                    logger.warning("skipping unknown atomic element type=\(element.type, privacy: .public) id=\(element.id ?? "(nil)", privacy: .public)")
                }
        }
    }
}
