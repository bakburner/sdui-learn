import SwiftUI

/// Stub renderer for VideoPlayer sections.
/// Displays a placeholder showing playerType + contentId.
/// Will be replaced with actual platform video SDK integration.
struct VideoPlayerStubView: View {
    let section: Section
    let onAction: (Action) -> Void

    private var playerType: String {
        section.data?.playerType?.rawValue ?? "unknown"
    }

    private var contentId: String {
        section.data?.contentID ?? "unknown"
    }

    var body: some View {
        ZStack {
            color(from: "#1A1F2E") ?? Color.black
            VStack(spacing: 8) {
                Image(systemName: "play.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.white)
                Text("Video Player")
                    .font(.headline)
                    .foregroundColor(.white)
                Text("\(playerType) • \(contentId)")
                    .font(.caption)
                    .foregroundColor(color(from: "#888888") ?? .gray)
            }
        }
        .aspectRatio(16 / 9, contentMode: .fit)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}
