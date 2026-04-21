import SwiftUI

/// Renders a Divider atomic element.
struct AtomicDividerView: View {
    let element: AtomicElement

    var body: some View {
        let thickness = CGFloat(element.thickness ?? 1)
        let isVertical = element.orientation == .vertical

        if isVertical {
            Rectangle()
                .fill(color(from: element.color) ?? Color.gray.opacity(0.3))
                .frame(width: thickness)
        } else {
            Rectangle()
                .fill(color(from: element.color) ?? Color.gray.opacity(0.3))
                .frame(height: thickness)
        }
    }
}
