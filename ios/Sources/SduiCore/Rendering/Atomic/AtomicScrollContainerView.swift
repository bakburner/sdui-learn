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
    @Environment(\.colorScheme) private var colorScheme
    @State private var activePage: Int? = 0

    var body: some View {
        let isHorizontal = element.direction == .row
        let gap = LayoutTokenResolver.cgFloat(element.gap)
        let isPaging = element.paging == true
        let childCount = element.children?.count ?? 0
        let indicatorStyle = element.pageIndicator?.style
        let shouldShowDots = isPaging && indicatorStyle == .dots && childCount > 1
        let shouldShowDashes = isPaging && indicatorStyle == .dashes && childCount > 1

        Group {
            if shouldShowDashes {
                ZStack(alignment: .bottom) {
                    scrollView(isHorizontal: isHorizontal, gap: gap, trackPage: true)
                    PageIndicatorDashes(
                        count: childCount,
                        activePage: activePage ?? 0,
                        color: ColorTokenResolver.resolve(element.pageIndicator?.color, colorScheme: colorScheme) ?? Color.white.opacity(0.4),
                        activeColor: ColorTokenResolver.resolve(element.pageIndicator?.activeColor, colorScheme: colorScheme) ?? Color.white
                    )
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                }
            } else if shouldShowDots {
                pageIndicatorScaffold(alignment: element.pageIndicator?.alignment) {
                    scrollView(isHorizontal: isHorizontal, gap: gap, trackPage: true)
                } dots: {
                    PageIndicatorDots(
                        count: childCount,
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

    /// Stacks scroll and dots in layout order (VStack) instead of overlaying.
    @ViewBuilder
    private func pageIndicatorScaffold(
        alignment: BadgeAlignment?,
        @ViewBuilder scroll: () -> some View,
        @ViewBuilder dots: () -> some View
    ) -> some View {
        let dotsOnTop: Bool = {
            switch alignment {
            case .topStart, .topCenter, .topEnd: return true
            default: return false
            }
        }()
        VStack(alignment: .leading, spacing: 0) {
            if dotsOnTop {
                dotsFrame(alignment: alignment) { dots() }
                scroll()
            } else {
                scroll()
                dotsFrame(alignment: alignment) { dots() }
            }
        }
    }

    @ViewBuilder
    private func dotsFrame(alignment: BadgeAlignment?, @ViewBuilder content: () -> some View) -> some View {
        let a = alignment ?? .bottomCenter
        HStack(alignment: .center, spacing: 0) {
            switch a {
            case .topStart, .bottomStart, .centerStart:
                content()
                Spacer(minLength: 0)
            case .topEnd, .bottomEnd, .centerEnd:
                Spacer(minLength: 0)
                content()
            case .topCenter, .bottomCenter, .center:
                Spacer(minLength: 0)
                content()
                Spacer(minLength: 0)
            }
        }
        .frame(maxWidth: .infinity)
    }

    @ViewBuilder
    private func scrollView(isHorizontal: Bool, gap: CGFloat, trackPage: Bool) -> some View {
        let needsPositionTracking = trackPage || (element.snapAlignment != nil && element.snapAlignment != .start)
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
        .modifier(ScrollPositionModifier(activePage: $activePage, enabled: needsPositionTracking, anchor: scrollAnchor(isHorizontal: isHorizontal)))
        .modifier(ScrollBehaviorModifier(paging: element.paging == true, snapAlignment: element.snapAlignment))
    }

    private func scrollAnchor(isHorizontal: Bool) -> UnitPoint? {
        switch element.snapAlignment {
        case .center: return .center
        case .end: return isHorizontal ? .trailing : .bottom
        default: return nil
        }
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
    let anchor: UnitPoint?

    @ViewBuilder
    func body(content: Content) -> some View {
        if enabled {
            if let anchor {
                content.scrollPosition(id: $activePage, anchor: anchor)
            } else {
                content.scrollPosition(id: $activePage)
            }
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

private struct PageIndicatorDashes: View {
    let count: Int
    let activePage: Int
    let color: Color
    let activeColor: Color

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<count, id: \.self) { index in
                RoundedRectangle(cornerRadius: 2)
                    .fill(index == activePage ? activeColor : color)
                    .frame(height: 3)
            }
        }
        .accessibilityHidden(true)
    }
}
