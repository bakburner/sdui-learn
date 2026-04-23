import SwiftUI

/// Shared outer-chrome wrapper applied by `SectionRouter` to every
/// permanent section. Reads `section.display` (margin, padding,
/// background, cornerRadius, shadow, border) and applies it
/// platform-natively, so permanent-section renderers never set
/// their own outer chrome.
///
/// Supports three `background` shapes — matching the schema's
/// `Background` union:
///   • string  → token or hex, resolved to a solid `Color`
///   • object with `colors`    → `LinearGradient` with direction
///   • object with `imageUrl`  → remote `AsyncImage` (chrome layer
///                                sits below `content`)
///
/// See `AGENTS.md` §15.3 for the governance rule this wrapper
/// enforces, and `SduiUtils.defaultSectionDisplay()` on the
/// server for the default chrome values composers emit.
struct SectionContainer<Content: View>: View {
    let display: SectionDisplay?
    let content: () -> Content

    @Environment(\.colorScheme) private var colorScheme

    init(display: SectionDisplay?, @ViewBuilder content: @escaping () -> Content) {
        self.display = display
        self.content = content
    }

    var body: some View {
        let padding = edgeInsets(from: display?.padding)
        let margin = edgeInsets(from: display?.margin)
        let radius = CGFloat(display?.cornerRadius ?? 0)

        return content()
            .padding(padding)
            .background(backgroundLayer())
            .clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay(borderOverlay(radius: radius))
            .applyShadow(display?.shadow)
            .padding(margin)
    }

    @ViewBuilder
    private func backgroundLayer() -> some View {
        switch display?.background {
        case .none:
            Color.clear
        case .some(.string(let value)):
            ColorTokenResolver.resolve(value, colorScheme: colorScheme) ?? Color.clear
        case .some(.background(let bg)):
            if let imageUrl = bg.imageURL, let url = URL(string: imageUrl) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    Color.clear
                }
            } else if let colors = bg.colors, colors.count > 1 {
                LinearGradient(
                    gradient: Gradient(colors: colors.map {
                        ColorTokenResolver.resolve($0, colorScheme: colorScheme) ?? .clear
                    }),
                    startPoint: gradientStart(bg.direction),
                    endPoint: gradientEnd(bg.direction)
                )
            } else if let first = bg.colors?.first {
                ColorTokenResolver.resolve(first, colorScheme: colorScheme) ?? Color.clear
            } else {
                Color.clear
            }
        }
    }

    @ViewBuilder
    private func borderOverlay(radius: CGFloat) -> some View {
        if let border = display?.border,
           let width = border.width, width > 0,
           let color = ColorTokenResolver.resolve(border.color, colorScheme: colorScheme) {
            RoundedRectangle(cornerRadius: radius, style: .continuous)
                .stroke(color, lineWidth: CGFloat(width))
        } else {
            EmptyView()
        }
    }

    private func gradientStart(_ direction: Direction?) -> UnitPoint {
        switch direction {
        case .horizontal: return .leading
        case .diagonal: return .topLeading
        case .vertical, .none: return .top
        }
    }

    private func gradientEnd(_ direction: Direction?) -> UnitPoint {
        switch direction {
        case .horizontal: return .trailing
        case .diagonal: return .bottomTrailing
        case .vertical, .none: return .bottom
        }
    }
}
