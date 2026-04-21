import SwiftUI

/// Renders a Spacer atomic element.
struct AtomicSpacerView: View {
    let element: AtomicElement

    var body: some View {
        Spacer()
            .frame(
                width: element.width.map { CGFloat($0) },
                height: element.height.map { CGFloat($0) }
            )
    }
}
