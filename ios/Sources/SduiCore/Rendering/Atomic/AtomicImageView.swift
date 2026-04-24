import SwiftUI

/// Renders an Image atomic element. Image URLs always come from the server
/// response — never constructed client-side. Image-specific concerns
/// (aspectRatio, objectFit, cornerRadii-on-the-image) live here; margin /
/// padding / bg / shadow / opacity / width / fillWidth / badge live on
/// `AtomicBoxModifier` via `.atomicBox(...)`.
///
/// Note on sizing: explicit `width` / `height` on the wire go to the
/// AtomicBox's outer frame (matching leaves of every other primitive).
/// The <img>-equivalent inside fills that frame so cornerRadius/clip on
/// the box applies correctly. The image's own `.aspectRatio` sits on
/// the inner view so loading / failure placeholders keep the intended
/// aspect box even before bytes land.
struct AtomicImageView: View {
    let element: AtomicElement
    var screenState: ScreenState = ScreenState()
    var onAction: (Action) -> Void = { _ in }

    var body: some View {
        if let src = element.src, let url = URL(string: src) {
            let spec = ImageVariantResolver.resolve(element.variant)
            let resolvedAspectRatio = element.aspectRatio.map { CGFloat($0) } ?? spec?.aspectRatio
            let resolvedContentMode = contentMode(spec: spec)
            let resolvedRadius = element.cornerRadius.map { CGFloat($0) } ?? spec?.cornerRadius
            let shouldClip = spec?.clip ?? true

            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .aspectRatio(resolvedAspectRatio, contentMode: resolvedContentMode)
                case .failure:
                    placeholder
                case .empty:
                    ProgressView()
                @unknown default:
                    placeholder
                }
            }
            // Aspect ratio on the outer frame covers loading/failure
            // placeholders, which otherwise collapse before the image
            // lands. A fixed `height` on the element disables this so
            // the outer frame is a fixed box, not an aspect-derived one.
            .modifier(ImageAspectRatioModifier(
                aspectRatio: (element.height == nil) ? resolvedAspectRatio : nil,
                contentMode: resolvedContentMode
            ))
            // Image-specific corner clip (the AtomicBox also clips when
            // cornerRadius is set, so this is redundant when the image
            // is already sized to fill the box; kept for the legacy
            // case of a variant like `logo` that sets `clip: false` and
            // relies on its own clipping disabled).
            .modifier(ImageCornerRadiusModifier(
                radius: shouldClip ? nil : resolvedRadius,
                radii: shouldClip ? nil : element.cornerRadii
            ))
            .applyActionTriggers(element.actions, onAction: onAction)
            .sduiAccessibility(element.accessibility, fallbackLabel: element.alt)
            .atomicBox(element, screenState: screenState, onAction: onAction)
        }
    }

    private func contentMode(spec: ImageVariantSpec?) -> ContentMode {
        if let fit = element.fit {
            switch fit {
            case .cover: return .fill
            case .contain: return .fit
            case .fill: return .fill
            case .none: return .fit
            }
        }
        return spec?.contentMode ?? .fit
    }

    @ViewBuilder
    private var placeholder: some View {
        Rectangle()
            .fill(Color.gray.opacity(0.2))
    }
}

private struct ImageAspectRatioModifier: ViewModifier {
    let aspectRatio: CGFloat?
    let contentMode: ContentMode

    @ViewBuilder
    func body(content: Content) -> some View {
        if let aspectRatio = aspectRatio {
            content.aspectRatio(aspectRatio, contentMode: contentMode)
        } else {
            content
        }
    }
}

private struct ImageCornerRadiusModifier: ViewModifier {
    let radius: CGFloat?
    let radii: CornerRadii?

    @ViewBuilder
    func body(content: Content) -> some View {
        if let radii = radii, hasAnyCorner(radii) {
            content.clipShape(unevenShape(radii: radii, fallback: radius ?? 0))
        } else if let radius = radius {
            content.cornerRadius(radius)
        } else {
            content
        }
    }

    private func hasAnyCorner(_ r: CornerRadii) -> Bool {
        (r.topStart ?? 0) != 0 || (r.topEnd ?? 0) != 0
            || (r.bottomStart ?? 0) != 0 || (r.bottomEnd ?? 0) != 0
    }

    private func unevenShape(radii: CornerRadii, fallback: CGFloat) -> some Shape {
        let tl = CGFloat(radii.topStart ?? Int(fallback))
        let tr = CGFloat(radii.topEnd ?? Int(fallback))
        let bl = CGFloat(radii.bottomStart ?? Int(fallback))
        let br = CGFloat(radii.bottomEnd ?? Int(fallback))
        return UnevenRoundedRectangle(
            topLeadingRadius: tl,
            bottomLeadingRadius: bl,
            bottomTrailingRadius: br,
            topTrailingRadius: tr,
            style: .continuous
        )
    }
}
