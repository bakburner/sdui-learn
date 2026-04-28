import SwiftUI

/// Shared section-surface wrapper applied by `SectionRouter` to every
/// permanent section. Reads `section.surface` (margin, padding,
/// background, cornerRadius, shadow, border) and applies it
/// platform-natively, so permanent-section renderers never set
/// their own outer chrome.
///
/// Supports three `background` shapes — matching the schema's
/// `Background` union:
///   • string  → token or hex, resolved to a solid `Color`
///   • object with `colors`    → `LinearGradient` with direction
///   • object with `imageUrl`  → cached remote image (surface layer
///                                sits below `content`)
///
/// Shared wrapper enforcing server-driven outer chrome for every section.
/// See `SduiUtils.defaultSurface()` on the server for the default
/// surface values composers emit.
struct SectionContainer<Content: View>: View {
    let surface: SectionSurface?
    let content: () -> Content

    @Environment(\.colorScheme) private var colorScheme

    init(surface: SectionSurface?, @ViewBuilder content: @escaping () -> Content) {
        self.surface = surface
        self.content = content
    }

    var body: some View {
        let padding = edgeInsets(from: surface?.padding)
        let margin = edgeInsets(from: surface?.margin)
        let radius = LayoutTokenResolver.cgFloat(surface?.cornerRadius)

        return content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(padding)
            .background { backgroundView(for: surface?.background, colorScheme: colorScheme) }
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(borderOverlay(radius: radius))
            .applyShadow(surface?.shadow)
            .padding(margin)
    }

    @ViewBuilder
    private func borderOverlay(radius: CGFloat) -> some View {
        if let border = surface?.border,
           let width = border.width, width > 0,
           let color = ColorTokenResolver.resolve(border.color, colorScheme: colorScheme) {
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .stroke(color, lineWidth: CGFloat(width))
        } else {
            EmptyView()
        }
    }
}
