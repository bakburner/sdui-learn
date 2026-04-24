import SwiftUI
import os

private let scrollLogger = Logger(subsystem: "com.nba.sdui", category: "AtomicScrollContainer")

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
        let _ = logUnsupportedScrollConstraints()

        ScrollView(isHorizontal ? .horizontal : .vertical, showsIndicators: element.showIndicators ?? false) {
            AnyLayout(AtomicFlexStackLayout(
                isRow: isHorizontal,
                alignment: element.alignment,
                crossAlignment: element.crossAlignment,
                spacing: gap
            )) {
                children
            }
            .modifier(ScrollTargetLayoutModifier(enabled: element.paging == true || element.snapAlignment != nil))
        }
        .modifier(ScrollBehaviorModifier(paging: element.paging == true, snapAlignment: element.snapAlignment))
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    @ViewBuilder
    private var children: some View {
        if let kids = element.children {
            ForEach(Array(kids.enumerated()), id: \.offset) { _, child in
                AtomicRouter(element: child, screenState: screenState, onAction: onAction, depth: depth)
                    .layoutValue(key: AtomicFlexValueKey.self, value: CGFloat(max(child.flex ?? 0, 0)))
            }
        }
    }

    private func logUnsupportedScrollConstraints() {
        if let snapAlignment = element.snapAlignment, snapAlignment != .start {
            scrollLogger.warning("snapAlignment \(snapAlignment.rawValue, privacy: .public) is decoded but iOS ScrollView snapping currently aligns targets at start; elementId=\(element.id ?? "nil", privacy: .public)")
        }
    }
}

private struct ScrollTargetLayoutModifier: ViewModifier {
    let enabled: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if enabled {
            content.scrollTargetLayout()
        } else {
            content
        }
    }
}

private struct ScrollBehaviorModifier: ViewModifier {
    let paging: Bool
    let snapAlignment: Align?

    @ViewBuilder
    func body(content: Content) -> some View {
        if paging {
            content.scrollTargetBehavior(.paging)
        } else if snapAlignment != nil {
            content.scrollTargetBehavior(.viewAligned)
        } else {
            content
        }
    }
}
