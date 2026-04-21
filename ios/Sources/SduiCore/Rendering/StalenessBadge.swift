import SwiftUI

/// Small overlay chip shown when a section's real-time feed is degraded
/// (SSE disconnect, repeated polling failures). Observes
/// ``SectionStalenessTracker`` directly and fades itself in/out.
struct StalenessBadge: View {
    let sectionID: String
    let tracker: SectionStalenessTracker

    var body: some View {
        Group {
            if tracker.isStale(sectionID) {
                HStack(spacing: 4) {
                    Image(systemName: "wifi.exclamationmark")
                        .font(.caption2)
                    Text("Stale")
                        .font(.caption2.weight(.semibold))
                }
                .foregroundStyle(.white)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(
                    Capsule().fill(Color.orange)
                )
                .padding(8)
                .accessibilityLabel("Live updates unavailable for this section")
                .transition(.opacity.combined(with: .scale))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: tracker.isStale(sectionID))
    }
}
