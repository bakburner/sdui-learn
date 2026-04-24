import SwiftUI

/// Renders a Divider atomic element. The rule itself owns only its
/// orientation / thickness / color; box-model (margin/padding/bg/radius/
/// shadow/opacity) lives on `AtomicBoxModifier` via `.atomicBox(...)`.
struct AtomicDividerView: View {
    let element: AtomicElement

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let thickness = CGFloat(element.thickness ?? 1)
        let isVertical = element.orientation == .vertical
        let fill = ColorTokenResolver.resolve(element.color, colorScheme: colorScheme)
            ?? Color.gray.opacity(0.3)

        Group {
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
        .atomicBox(element, screenState: ScreenState(), onAction: { _ in })
    }
}
