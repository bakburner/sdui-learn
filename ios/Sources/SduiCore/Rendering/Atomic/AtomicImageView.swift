import SwiftUI

/// Renders an Image atomic element. Image URLs always come from the server
/// response — never constructed client-side.
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
            // Priority: inline fillWidth > inline width > variant fillWidth.
            // A thumbnail inside a fixed-width card emits `fillWidth: true` +
            // `aspectRatio: 16/9` to stretch to the card edge while the card
            // owns the sole width anchor.
            let inlineFillWidth = element.fillWidth ?? false
            let shouldFillWidth = (inlineFillWidth || (spec?.fillWidth ?? false))
                && element.width == nil

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
            .modifier(ImageFrameModifier(
                width: element.width.map { CGFloat($0) },
                height: element.height.map { CGFloat($0) },
                fillWidth: shouldFillWidth
            ))
            .modifier(ImageAspectRatioModifier(
                // Apply aspectRatio at the outer level only when fillWidth
                // is on and no explicit height was set — in that case the
                // loading/failure placeholders would otherwise collapse to
                // zero height (the inner `.aspectRatio` on `.success` only
                // runs once the image loads). When width+height are fixed,
                // the inner `.aspectRatio` is sufficient.
                aspectRatio: (shouldFillWidth && element.height == nil) ? resolvedAspectRatio : nil,
                contentMode: resolvedContentMode
            ))
            .modifier(ImageCornerRadiusModifier(
                radius: shouldClip ? resolvedRadius : nil,
                radii: shouldClip ? element.cornerRadii : nil
            ))
            .applyBadge(element.badge, screenState: screenState, onAction: onAction)
            .applyActionTriggers(element.actions, onAction: onAction)
            .padding(edgeInsets(from: element.padding))
            .sduiAccessibility(element.accessibility, fallbackLabel: element.alt)
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
            .frame(
                width: element.width.map { CGFloat($0) },
                height: element.height.map { CGFloat($0) }
            )
    }
}

private struct ImageFrameModifier: ViewModifier {
    let width: CGFloat?
    let height: CGFloat?
    let fillWidth: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if fillWidth {
            content
                .frame(maxWidth: .infinity)
                .frame(height: height)
        } else {
            content.frame(width: width, height: height)
        }
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
            // Asymmetric per-corner rounding (e.g. content-card thumbnail:
            // rounded top, square bottom so the text area below can sit flush
            // against the image without a curved inset above it). `cornerRadius`
            // (single value) is used as the fallback for any corner omitted
            // from cornerRadii.
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
