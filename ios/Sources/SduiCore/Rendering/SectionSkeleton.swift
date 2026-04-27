import SwiftUI

/// Server-driven loading placeholder for a section.
///
/// Mirrors Android's
/// [`SectionSkeleton`](../../../../../android/sdui-core/src/main/java/com/nba/sdui/core/renderer/SectionSkeleton.kt).
/// Respects `sectionStates.loading.minHeightDp` to prevent layout shift
/// during section-level refresh and picks the skeleton style from the
/// server's `Loading.skeleton` enum.
struct SectionSkeleton: View {
    let sectionStates: SectionStates?

    var body: some View {
        let style = sectionStates?.loading?.skeleton ?? .shimmer
        let minHeight = CGFloat(sectionStates?.loading?.minHeightDP ?? 80)

        switch style {
        case .none:
            Color.clear.frame(height: 0)
        case .spinner:
            Spinner(minHeight: minHeight)
        case .placeholder:
            PlaceholderRows(minHeight: minHeight, animated: false)
        case .shimmer:
            PlaceholderRows(minHeight: minHeight, animated: true)
        }
    }
}

private struct Spinner: View {
    let minHeight: CGFloat

    var body: some View {
        HStack {
            Spacer()
            ProgressView()
                .progressViewStyle(.circular)
            Spacer()
        }
        .frame(maxWidth: .infinity, minHeight: minHeight)
    }
}

private struct PlaceholderRows: View {
    let minHeight: CGFloat
    let animated: Bool

    @State private var shimmerOffset: CGFloat = -1

    private let rowWidths: [CGFloat] = [1.0, 0.75, 0.5]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(rowWidths.enumerated()), id: \.offset) { _, fraction in
                GeometryReader { geo in
                    RoundedRectangle(cornerRadius: 4, style: .continuous)
                        .fill(Color.primary.opacity(0.08))
                        .frame(width: geo.size.width * fraction, height: 14)
                        .overlay(alignment: .leading) {
                            if animated {
                                shimmerOverlay(width: geo.size.width * fraction)
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                }
                .frame(height: 14)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .leading)
        .onAppear {
            guard animated else { return }
            Task { @MainActor in
                await Task.yield()
                withAnimation(.linear(duration: 1.5).repeatForever(autoreverses: false)) {
                    shimmerOffset = 2
                }
            }
        }
    }

    @ViewBuilder
    private func shimmerOverlay(width: CGFloat) -> some View {
        LinearGradient(
            colors: [
                Color.clear,
                Color.primary.opacity(0.12),
                Color.clear
            ],
            startPoint: .leading,
            endPoint: .trailing
        )
        .frame(width: width * 0.6)
        .offset(x: width * shimmerOffset)
    }
}
