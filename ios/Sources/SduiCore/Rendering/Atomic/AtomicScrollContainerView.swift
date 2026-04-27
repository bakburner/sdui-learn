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
    @Environment(\.colorScheme) private var colorScheme
    @State private var activePage: Int? = 0

    var body: some View {
        let isHorizontal = element.direction == .row
        let gap = CGFloat(element.gap ?? 0)
        let _ = logUnsupportedScrollConstraints()
        let isPaging = element.paging == true
        let shouldShowPageIndicator = isPaging && element.pageIndicator?.style == .dots && (element.children?.count ?? 0) > 1

        Group {
            if shouldShowPageIndicator {
                ZStack(alignment: swiftUIAlignment(for: element.pageIndicator?.alignment)) {
                    scrollView(isHorizontal: isHorizontal, gap: gap, trackPage: true)
                    PageIndicatorDots(
                        count: element.children?.count ?? 0,
                        activePage: activePage ?? 0,
                        color: ColorTokenResolver.resolve(element.pageIndicator?.color, colorScheme: colorScheme) ?? Color.white.opacity(0.45),
                        activeColor: ColorTokenResolver.resolve(element.pageIndicator?.activeColor, colorScheme: colorScheme) ?? Color.white
                    )
                    .padding(8)
                }
            } else {
                scrollView(isHorizontal: isHorizontal, gap: gap, trackPage: false)
            }
        }
        .atomicBox(element, screenState: screenState, onAction: onAction)
    }

    @ViewBuilder
    private func scrollView(isHorizontal: Bool, gap: CGFloat, trackPage: Bool) -> some View {
        ScrollView(isHorizontal ? .horizontal : .vertical, showsIndicators: element.showIndicators ?? false) {
            AnyLayout(AtomicFlexStackLayout(
                isRow: isHorizontal,
                alignment: element.alignment,
                crossAlignment: element.crossAlignment,
                spacing: gap
            )) {
                children
            }
            .modifier(ScrollTargetLayoutModifier(enabled: element.paging == true || element.snapAlignment != nil || trackPage))
        }
        .modifier(ScrollPositionModifier(activePage: $activePage, enabled: trackPage))
        .modifier(ScrollBehaviorModifier(paging: element.paging == true, snapAlignment: element.snapAlignment))
    }

    @ViewBuilder
    private var children: some View {
        if let kids = element.children {
            ForEach(0..<kids.count, id: \.self) { index in
                let child = kids[index]
                AtomicRouter(element: child, screenState: screenState, onAction: onAction, depth: depth)
                    .modifier(PageFrameModifier(enabled: element.paging == true, isHorizontal: element.direction == .row))
                    .layoutValue(key: AtomicFlexValueKey.self, value: CGFloat(max(child.flex ?? 0, 0)))
                    .id(index)
            }
        }
    }

    private func logUnsupportedScrollConstraints() {
        if let snapAlignment = element.snapAlignment, snapAlignment != .start {
            scrollLogger.warning("snapAlignment \(snapAlignment.rawValue, privacy: .public) is decoded but iOS ScrollView snapping currently aligns targets at start; elementId=\(element.id ?? "nil", privacy: .public)")
        }
    }

    private func swiftUIAlignment(for alignment: BadgeAlignment?) -> SwiftUI.Alignment {
        switch alignment {
        case .topStart: return .topLeading
        case .topCenter: return .top
        case .topEnd: return .topTrailing
        case .centerStart: return .leading
        case .center: return .center
        case .centerEnd: return .trailing
        case .bottomStart: return .bottomLeading
        case .bottomEnd: return .bottomTrailing
        case .bottomCenter, .none: return .bottom
        }
    }
}

private struct PageFrameModifier: ViewModifier {
    let enabled: Bool
    let isHorizontal: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if enabled {
            if isHorizontal {
                content.containerRelativeFrame(.horizontal)
            } else {
                content.containerRelativeFrame(.vertical)
            }
        } else {
            content
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

private struct ScrollPositionModifier: ViewModifier {
    @Binding var activePage: Int?
    let enabled: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if enabled {
            content.scrollPosition(id: $activePage)
        } else {
            content
        }
    }
}

private struct PageIndicatorDots: View {
    let count: Int
    let activePage: Int
    let color: Color
    let activeColor: Color

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<count, id: \.self) { index in
                Circle()
                    .fill(index == activePage ? activeColor : color)
                    .frame(width: 6, height: 6)
            }
        }
        .accessibilityHidden(true)
    }
}
