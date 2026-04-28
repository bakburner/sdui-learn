import SwiftUI

/// Stub renderer for `AdSlot`. Reserves the ad SDK's eventual mount
/// rectangle — `data.sizes[0]` is the single source of truth for
/// dimensions, shared by the placeholder and (when it lands) the
/// ad SDK itself. Inner placeholder chrome (background color, label
/// text) comes from `data.placeholder`. Outer chrome is `section.surface`
/// (server-composed, typically `SduiUtils.adSlotSurface()`). The creative
/// **fills the column width** inside that padding; height follows the
/// **aspect ratio** of `data.sizes[0]`.
///
/// This renderer carries no client-side chrome defaults. A payload
/// missing required `sizes` is a decoder-level failure, not a
/// render-time one — reservation dimensions and placeholder content
/// come from the server payload.
struct AdSlotView: View {
    let section: Section
    let onAction: (Action) -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        guard
            let data = section.data,
            let first = data.sizes?.first,
            first.count >= 2
        else {
            return AnyView(EmptyView())
        }
        let w = CGFloat(first[0])
        let h = CGFloat(first[1])
        guard w > 0, h > 0 else { return AnyView(EmptyView()) }
        let aspect = w / h
        let label = data.label
        let placeholder = data.placeholder

        return AnyView(
            VStack(alignment: .center, spacing: 8) {
                if let label = label {
                    Text(label)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                Rectangle()
                    .fill(ColorTokenResolver.resolve(placeholder?.backgroundColor, colorScheme: colorScheme) ?? .clear)
                    .aspectRatio(aspect, contentMode: .fit)
                    .frame(maxWidth: .infinity)
                    .overlay(
                        Text(placeholder?.text ?? "")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    )
            }
            .frame(maxWidth: .infinity, alignment: .center)
        )
    }
}
