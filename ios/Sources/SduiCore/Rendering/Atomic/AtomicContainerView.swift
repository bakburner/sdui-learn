import SwiftUI
#if os(iOS)
import UIKit
#endif

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
        let isRow = resolvedDirection == .row
        let gap = LayoutTokenResolver.cgFloat(element.gap)
        let resolvedAspectRatio = LayoutTokenResolver.aspectRatio(element.aspectRatio)
        let wrap = element.layoutWrap == true
        let crossGap: CGFloat = element.crossAxisGap.map { LayoutTokenResolver.cgFloat($0) } ?? gap

        Group {
            if wrap {
                AnyLayout(WrappingFlexLayout(
                    isRow: isRow,
                    alignment: element.alignment,
                    crossAlignment: element.crossAlignment,
                    mainAxisSpacing: gap,
                    crossAxisSpacing: crossGap
                )) {
                    children
                }
            } else {
                AnyLayout(AtomicFlexStackLayout(
                    isRow: isRow,
                    alignment: element.alignment,
                    crossAlignment: element.crossAlignment,
                    spacing: gap
                )) {
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
        let kids = element.children ?? []
        let rows = kids.enumerated().map { i, c in
            AtomicContainerChild(id: c.id.map { "e:\($0)" } ?? "i:\(i)", child: c)
        }
        ForEach(rows) { item in
            AtomicRouter(element: item.child, screenState: screenState, onAction: onAction, depth: depth)
                .layoutValue(key: AtomicFlexValueKey.self, value: CGFloat(max(item.child.flex ?? 0, 0)))
                .layoutValue(key: AtomicAlignSelfKey.self, value: item.child.alignSelf)
        }
    }

    private var resolvedDirection: UIDirection? {
        guard element.direction == .row,
              let breakpoint = element.breakpoint,
              currentScreenWidth < CGFloat(breakpoint) else {
            return element.direction
        }
        return .column
    }

    private var currentScreenWidth: CGFloat {
        #if os(iOS)
        UIScreen.main.bounds.width
        #else
        CGFloat.greatestFiniteMagnitude
        #endif
    }
}

private struct AtomicContainerChild: Identifiable {
    let id: String
    let child: AtomicElement
}

struct AtomicFlexStackLayout: SwiftUI.Layout {
    let isRow: Bool
    let alignment: Alignment?
    let crossAlignment: CrossAlignment?
    let spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard !subviews.isEmpty else { return .zero }

        let mainProposal = isRow ? proposal.width : proposal.height
        let crossProposal = isRow ? proposal.height : proposal.width
        let sizes = measuredSizes(proposal: proposal, subviews: subviews)
        let totalFlex = subviews.reduce(CGFloat.zero) { $0 + $1[AtomicFlexValueKey.self] }
        let baseSpacing = spacing * CGFloat(max(subviews.count - 1, 0))
        let fixedMain = fixedMainLength(sizes: sizes, subviews: subviews, hasBoundedMain: mainProposal != nil)
        let remainingMain = max((mainProposal ?? 0) - fixedMain - baseSpacing, 0)
        let naturalMain = sizes.enumerated().reduce(CGFloat.zero) { partial, item in
            let flex = subviews[item.offset][AtomicFlexValueKey.self]
            if flex > 0 && totalFlex > 0 && mainProposal != nil {
                return partial + remainingMain * (flex / totalFlex)
            }
            return partial + mainLength(item.element)
        } + baseSpacing
        let naturalCross = sizes.reduce(CGFloat.zero) { max($0, crossLength($1)) }
        let resolvedMain = resolvedContainerMain(
            proposed: mainProposal,
            natural: naturalMain,
            hasFlexibleLayout: totalFlex > 0 || alignment?.usesRemainingMainAxisSpace == true
        )
        let resolvedCross = crossAlignment == .stretch ? (crossProposal ?? naturalCross) : naturalCross

        return isRow
            ? CGSize(width: resolvedMain, height: resolvedCross)
            : CGSize(width: resolvedCross, height: resolvedMain)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard !subviews.isEmpty else { return }

        let containerMain = isRow ? bounds.width : bounds.height
        let containerCross = isRow ? bounds.height : bounds.width
        let sizes = measuredSizes(
            proposal: ProposedViewSize(width: bounds.width, height: bounds.height),
            subviews: subviews
        )
        let totalFlex = subviews.reduce(CGFloat.zero) { $0 + $1[AtomicFlexValueKey.self] }
        let baseSpacing = spacing * CGFloat(max(subviews.count - 1, 0))
        let fixedMain = fixedMainLength(sizes: sizes, subviews: subviews, hasBoundedMain: true)
        let remainingMain = max(containerMain - fixedMain - baseSpacing, 0)
        let childMainLengths = sizes.enumerated().map { index, size in
            let flex = subviews[index][AtomicFlexValueKey.self]
            return flex > 0 && totalFlex > 0 ? remainingMain * (flex / totalFlex) : mainLength(size)
        }
        let occupiedMain = childMainLengths.reduce(0, +) + baseSpacing
        let arrangement = MainAxisArrangement(alignment: alignment, extra: max(containerMain - occupiedMain, 0), count: subviews.count)
        var cursor = arrangement.leading

        for index in subviews.indices {
            let main = childMainLengths[index]
            let measuredCross = crossLength(sizes[index])
            let childAlignSelf = subviews[index][AtomicAlignSelfKey.self]
            let effectiveCrossAlignment = childAlignSelf ?? crossAlignment
            let cross = effectiveCrossAlignment == .stretch ? containerCross : measuredCross
            let crossOffset = crossAxisOffset(containerCross: containerCross, childCross: cross, effectiveAlignment: effectiveCrossAlignment)
            let origin = isRow
                ? CGPoint(x: bounds.minX + cursor, y: bounds.minY + crossOffset)
                : CGPoint(x: bounds.minX + crossOffset, y: bounds.minY + cursor)
            let childProposal = isRow
                ? ProposedViewSize(width: main, height: cross)
                : ProposedViewSize(width: cross, height: main)

            subviews[index].place(at: origin, anchor: .topLeading, proposal: childProposal)
            cursor += main + spacing + arrangement.extraBetween
        }
    }

    private func measuredSizes(proposal: ProposedViewSize, subviews: Subviews) -> [CGSize] {
        subviews.map { subview in
            let childProposal = isRow
                ? ProposedViewSize(width: nil, height: proposal.height)
                : ProposedViewSize(width: proposal.width, height: nil)
            return subview.sizeThatFits(childProposal)
        }
    }

    private func fixedMainLength(sizes: [CGSize], subviews: Subviews, hasBoundedMain: Bool) -> CGFloat {
        sizes.enumerated().reduce(CGFloat.zero) { partial, item in
            let flex = subviews[item.offset][AtomicFlexValueKey.self]
            return flex > 0 && hasBoundedMain ? partial : partial + mainLength(item.element)
        }
    }

    private func resolvedContainerMain(proposed: CGFloat?, natural: CGFloat, hasFlexibleLayout: Bool) -> CGFloat {
        guard let proposed else { return natural }
        return hasFlexibleLayout ? max(proposed, natural) : natural
    }

    private func crossAxisOffset(containerCross: CGFloat, childCross: CGFloat, effectiveAlignment: CrossAlignment?) -> CGFloat {
        switch effectiveAlignment {
        case .center:
            return max((containerCross - childCross) / 2, 0)
        case .end:
            return max(containerCross - childCross, 0)
        case .stretch:
            return 0
        default:
            return 0
        }
    }

    private func mainLength(_ size: CGSize) -> CGFloat {
        isRow ? size.width : size.height
    }

    private func crossLength(_ size: CGSize) -> CGFloat {
        isRow ? size.height : size.width
    }
}

struct AtomicFlexValueKey: LayoutValueKey {
    static let defaultValue: CGFloat = 0
}

private struct MainAxisArrangement {
    let leading: CGFloat
    let extraBetween: CGFloat

    init(alignment: Alignment?, extra: CGFloat, count: Int) {
        switch alignment {
        case .center:
            leading = extra / 2
            extraBetween = 0
        case .end:
            leading = extra
            extraBetween = 0
        case .spaceBetween where count > 1:
            leading = 0
            extraBetween = extra / CGFloat(count - 1)
        case .spaceAround where count > 0:
            let unit = extra / CGFloat(count)
            leading = unit / 2
            extraBetween = unit
        case .spaceEvenly where count > 0:
            let unit = extra / CGFloat(count + 1)
            leading = unit
            extraBetween = unit
        default:
            leading = 0
            extraBetween = 0
        }
    }
}

private extension Alignment {
    var usesRemainingMainAxisSpace: Bool {
        switch self {
        case .center, .end, .spaceAround, .spaceBetween, .spaceEvenly:
            return true
        case .start:
            return false
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
