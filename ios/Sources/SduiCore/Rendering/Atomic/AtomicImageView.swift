import Kingfisher
import SwiftUI
#if os(iOS)
import UIKit
#endif

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

    @Environment(\.compositeContent) private var compositeContent
    @State private var activeSrc: String?
    @State private var triedPayloadFallback = false
    @State private var finalImageFailed = false

    var body: some View {
        if let src = currentSrc, let url = URL(string: src) {
            let spec = ImageVariantResolver.resolve(element.variant)
            let resolvedAspectRatio = LayoutTokenResolver.aspectRatio(element.aspectRatio) ?? spec?.aspectRatio
            let resolvedContentMode = contentMode(spec: spec)
            let resolvedRadius = element.cornerRadius.map { LayoutTokenResolver.cgFloat($0) } ?? spec?.cornerRadius
            let shouldClip = spec?.clip ?? true

            Group {
                if finalImageFailed {
                    placeholder(resolvedAspectRatio: resolvedAspectRatio, contentMode: resolvedContentMode)
                } else {
                    KFImage(url)
                        .placeholder {
                            loadingPlaceholder(resolvedAspectRatio: resolvedAspectRatio, contentMode: resolvedContentMode)
                        }
                        .onFailure { _ in
                            handleImageFailure(currentSrc: src)
                        }
                        .resizable()
                        .aspectRatio(resolvedAspectRatio, contentMode: resolvedContentMode)
                }
            }
            .modifier(AtomicImageMaxFrameModifier())
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
            .task(id: resolvedSrc) {
                activeSrc = resolvedSrc
                triedPayloadFallback = false
                finalImageFailed = false
            }
        }
    }

    /// Preserve payload-provided fallback semantics while letting Kingfisher
    /// provide memory/disk caching for successful responses.
    private func handleImageFailure(currentSrc: String) {
        if let fallback = element.placeholder,
           !triedPayloadFallback,
           fallback != currentSrc,
           URL(string: fallback) != nil {
            triedPayloadFallback = true
            activeSrc = fallback
        } else {
            finalImageFailed = true
        }
    }

    /// Resolve `src` from `bindRef` when present (pointing into the
    /// enclosing composite's `data.content`), falling back to the inline
    /// `src` URL. Lets composers rebind image URLs in flight without
    /// touching the ui tree.
    private var resolvedSrc: String? {
        if let bound = BindRefResolver.resolveString(bindRef: element.bindRef, in: compositeContent) {
            return bound
        }
        return element.src
    }

    private var currentSrc: String? {
        activeSrc ?? resolvedSrc
    }

    private func contentMode(spec: ImageVariantSpec?) -> SwiftUI.ContentMode {
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
    private func loadingPlaceholder(
        resolvedAspectRatio: CGFloat?,
        contentMode: SwiftUI.ContentMode
    ) -> some View {
        ZStack {
            Color.gray.opacity(0.2)
            ProgressView()
        }
        .modifier(ImageAspectRatioModifier(aspectRatio: (element.height == nil) ? resolvedAspectRatio : nil, contentMode: contentMode))
    }

    /// Last-resort when `src` and server `placeholder` both fail: bundled league fallback art (not wire-driven).
    @ViewBuilder
    private func placeholder(
        resolvedAspectRatio: CGFloat?,
        contentMode: SwiftUI.ContentMode
    ) -> some View {
        Image("SduiImageLastResortFallback", bundle: .module)
            .resizable()
            .modifier(ImageAspectRatioModifier(aspectRatio: (element.height == nil) ? resolvedAspectRatio : nil, contentMode: contentMode))
            .accessibilityHidden(true)
    }
}

private struct AtomicImageMaxFrameModifier: ViewModifier {
    @ViewBuilder
    func body(content: Content) -> some View {
        #if os(iOS)
        content
            .frame(
                maxWidth: min(UIScreen.main.bounds.width * 2, 8192),
                maxHeight: min(UIScreen.main.bounds.height * 2, 8192)
            )
        #else
        content
        #endif
    }
}

private struct ImageAspectRatioModifier: ViewModifier {
    let aspectRatio: CGFloat?
    let contentMode: SwiftUI.ContentMode

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
        LayoutTokenResolver.cgFloat(r.topStart) != 0 || LayoutTokenResolver.cgFloat(r.topEnd) != 0
            || LayoutTokenResolver.cgFloat(r.bottomStart) != 0 || LayoutTokenResolver.cgFloat(r.bottomEnd) != 0
    }

    private func unevenShape(radii: CornerRadii, fallback: CGFloat) -> some Shape {
        let tl = cornerScalar(radii.topStart, fallback: fallback)
        let tr = cornerScalar(radii.topEnd, fallback: fallback)
        let bl = cornerScalar(radii.bottomStart, fallback: fallback)
        let br = cornerScalar(radii.bottomEnd, fallback: fallback)
        return UnevenRoundedRectangle(
            topLeadingRadius: tl,
            bottomLeadingRadius: bl,
            bottomTrailingRadius: br,
            topTrailingRadius: tr,
            style: .continuous
        )
    }

    private func cornerScalar(_ s: LayoutScalar?, fallback: CGFloat) -> CGFloat {
        guard let s else { return fallback }
        return LayoutTokenResolver.cgFloat(s)
    }
}
