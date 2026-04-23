import SwiftUI

/// Renders a Divider atomic element.
struct AtomicDividerView: View {
    let element: AtomicElement

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let thickness = CGFloat(element.thickness ?? 1)
        let isVertical = element.orientation == .vertical
        let fill = ColorTokenResolver.resolve(element.color, colorScheme: colorScheme)
            ?? Color.gray.opacity(0.3)

        if isVertical {
            Rectangle()
                .fill(fill)
                .frame(width: thickness)
        } else {
            Rectangle()
                .fill(fill)
                .frame(height: thickness)
        }
    }
}
