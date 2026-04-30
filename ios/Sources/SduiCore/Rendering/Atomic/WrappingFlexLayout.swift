import SwiftUI

/// A custom SwiftUI Layout that wraps children to the next line/column when
/// they overflow the main axis. Used when `layoutWrap == true` on a Container.
///
/// For row direction: children flow horizontally and wrap to a new row below.
/// For column direction: children flow vertically and wrap to a new column to the right.
@available(iOS 16.0, *)
struct WrappingFlexLayout: SwiftUI.Layout {
    let isRow: Bool
    let alignment: Alignment?
    let crossAlignment: CrossAlignment?
    let mainAxisSpacing: CGFloat
    let crossAxisSpacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        guard !subviews.isEmpty else { return .zero }

        let containerMain = isRow ? proposal.width : proposal.height
        let lines = computeLines(subviews: subviews, containerMain: containerMain, proposal: proposal)

        let totalCrossSpacing = CGFloat(max(lines.count - 1, 0)) * crossAxisSpacing
        let totalCross = lines.reduce(CGFloat.zero) { $0 + $1.cross } + totalCrossSpacing
        let maxMain = lines.reduce(CGFloat.zero) { max($0, $1.main) }

        return isRow
            ? CGSize(width: containerMain ?? maxMain, height: totalCross)
            : CGSize(width: totalCross, height: containerMain ?? maxMain)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard !subviews.isEmpty else { return }

        let containerMain = isRow ? bounds.width : bounds.height
        let lines = computeLines(subviews: subviews, containerMain: containerMain, proposal: proposal)

        var crossCursor: CGFloat = 0

        for line in lines {
            let arrangement = MainAxisWrapArrangement(
                alignment: alignment,
                extra: max(containerMain - line.main, 0),
                count: line.items.count
            )
            var mainCursor = arrangement.leading

            for item in line.items {
                let childSize = item.size
                let childMain = isRow ? childSize.width : childSize.height
                let childCross = isRow ? childSize.height : childSize.width
                let crossOffset = crossOffsetForChild(
                    childCross: childCross,
                    lineCross: line.cross,
                    alignSelf: subviews[item.index][AtomicAlignSelfKey.self],
                    crossAlignment: crossAlignment
                )

                let origin = isRow
                    ? CGPoint(x: bounds.minX + mainCursor, y: bounds.minY + crossCursor + crossOffset)
                    : CGPoint(x: bounds.minX + crossCursor + crossOffset, y: bounds.minY + mainCursor)

                let childProposal = isRow
                    ? ProposedViewSize(width: childMain, height: line.cross)
                    : ProposedViewSize(width: line.cross, height: childMain)

                subviews[item.index].place(at: origin, anchor: .topLeading, proposal: childProposal)
                mainCursor += childMain + mainAxisSpacing + arrangement.extraBetween
            }
            crossCursor += line.cross + crossAxisSpacing
        }
    }

    // MARK: - Line computation

    private struct LineItem {
        let index: Int
        let size: CGSize
    }

    private struct Line {
        var items: [LineItem]
        var main: CGFloat
        var cross: CGFloat
    }

    private func computeLines(subviews: Subviews, containerMain: CGFloat?, proposal: ProposedViewSize) -> [Line] {
        guard !subviews.isEmpty else { return [] }
        let maxMain = containerMain ?? .greatestFiniteMagnitude
        var lines: [Line] = []
        var current = Line(items: [], main: 0, cross: 0)

        for index in subviews.indices {
            let childProposal = isRow
                ? ProposedViewSize(width: nil, height: proposal.height)
                : ProposedViewSize(width: proposal.width, height: nil)
            let size = subviews[index].sizeThatFits(childProposal)
            let childMain = isRow ? size.width : size.height
            let childCross = isRow ? size.height : size.width

            let spacingIfNotFirst = current.items.isEmpty ? 0 : mainAxisSpacing
            if !current.items.isEmpty && current.main + spacingIfNotFirst + childMain > maxMain {
                lines.append(current)
                current = Line(items: [], main: 0, cross: 0)
            }

            let spacing = current.items.isEmpty ? 0 : mainAxisSpacing
            current.items.append(LineItem(index: index, size: size))
            current.main += spacing + childMain
            current.cross = max(current.cross, childCross)
        }

        if !current.items.isEmpty {
            lines.append(current)
        }
        return lines
    }

    private func crossOffsetForChild(
        childCross: CGFloat,
        lineCross: CGFloat,
        alignSelf: CrossAlignment?,
        crossAlignment: CrossAlignment?
    ) -> CGFloat {
        let effective = alignSelf ?? crossAlignment
        switch effective {
        case .center:
            return max((lineCross - childCross) / 2, 0)
        case .end:
            return max(lineCross - childCross, 0)
        case .stretch:
            return 0
        default:
            return 0
        }
    }
}

// MARK: - Main axis arrangement for wrapping layout

private struct MainAxisWrapArrangement {
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

/// Layout value key for per-child alignSelf override.
struct AtomicAlignSelfKey: LayoutValueKey {
    static let defaultValue: CrossAlignment? = nil
}
