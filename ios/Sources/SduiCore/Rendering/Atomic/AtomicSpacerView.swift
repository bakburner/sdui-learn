import SwiftUI

/// Renders a Spacer atomic element.
struct AtomicSpacerView: View {
    let element: AtomicElement

    var body: some View {
        Spacer()
            .frame(
                width: element.width.map { LayoutTokenResolver.cgFloat($0) },
                height: element.height.map { LayoutTokenResolver.cgFloat($0) }
            )
    }
}
