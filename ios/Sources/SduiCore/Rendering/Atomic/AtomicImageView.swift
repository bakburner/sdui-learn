import SwiftUI

/// Renders an Image atomic element. Image URLs always come from the server
/// response — never constructed client-side.
struct AtomicImageView: View {
    let element: AtomicElement
    var screenState: ScreenState = ScreenState()
    var onAction: (Action) -> Void = { _ in }

    var body: some View {
        if let src = element.src, let url = URL(string: src) {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(
                            element.aspectRatio.map { CGFloat($0) },
                            contentMode: contentMode
                        )
                case .failure:
                    placeholder
                case .empty:
                    ProgressView()
                @unknown default:
                    placeholder
                }
            }
            .frame(
                width: element.width.map { CGFloat($0) },
                height: element.height.map { CGFloat($0) }
            )
            .cornerRadius(CGFloat(element.cornerRadius ?? 0))
            .applyBadge(element.badge, screenState: screenState, onAction: onAction)
            .applyActionTriggers(element.actions, onAction: onAction)
            .sduiAccessibility(element.accessibility, fallbackLabel: element.alt)
        }
    }

    private var contentMode: ContentMode {
        switch element.fit {
        case .some(.cover): return .fill
        case .some(.contain): return .fit
        case .some(.fill): return .fill
        case .some(.none), nil: return .fit
        }
    }

    @ViewBuilder
    private var placeholder: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.2))
            .frame(
                width: element.width.map { CGFloat($0) },
                height: element.height.map { CGFloat($0) }
            )
    }
}
