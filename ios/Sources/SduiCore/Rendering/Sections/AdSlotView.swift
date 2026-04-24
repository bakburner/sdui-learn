import SwiftUI

/// Stub renderer for `AdSlot`. Reserves the ad SDK's eventual mount
/// rectangle — `data.sizes[0]` is the single source of truth for
/// dimensions, shared by the placeholder and (when it lands) the
/// ad SDK itself. Inner placeholder chrome (background color, label
/// text) comes from `data.placeholder`; outer chrome (margin,
/// padding, shadow, radius) comes from `section.surface` via the
/// shared `SectionContainer` wrapper.
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
        let width = CGFloat(first[0])
        let height = CGFloat(first[1])
        let label = data.label
        let placeholder = data.placeholder

        return AnyView(
            VStack(spacing: 4) {
                if let label = label {
                    Text(label)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                Rectangle()
                    .fill(ColorTokenResolver.resolve(placeholder?.backgroundColor, colorScheme: colorScheme) ?? .clear)
                    .frame(width: width, height: height)
                    .overlay(
                        Text(placeholder?.text ?? "")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    )
            }
            .frame(maxWidth: .infinity)
        )
    }
}
