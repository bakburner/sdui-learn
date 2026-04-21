import SwiftUI

/// Stub renderer for AdSlot — Phase 4 will add platform ad SDK lifecycle.
struct AdSlotView: View {
    let section: Section
    let onAction: (Action) -> Void

    var body: some View {
        if let data = section.data {
            VStack(spacing: 4) {
                if let label = data.label {
                    Text(label)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                Rectangle()
                    .fill(Color.gray.opacity(0.1))
                    .frame(height: adHeight(from: data.sizes))
                    .overlay(
                        Text("Ad Placeholder")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    )
            }
        }
    }

    private func adHeight(from sizes: [[Int]]?) -> CGFloat {
        guard let sizes = sizes, let first = sizes.first, first.count >= 2 else {
            return 90
        }
        return CGFloat(first[1])
    }
}
