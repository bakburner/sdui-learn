import SwiftUI

/// Stub renderer for VideoPlayer sections. Will be replaced with the
/// platform video SDK in a later phase; until then renders a
/// placeholder play icon. Outer chrome (background, corner radius)
/// comes from `section.display` via `SectionContainer`. The renderer
/// only owns the 16:9 content frame and the placeholder glyph —
/// see AGENTS.md §15.1(2) and §15.3.
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
        Color.clear
            .aspectRatio(16 / 9, contentMode: .fit)
            .frame(maxWidth: .infinity)
            .overlay(
                VStack(spacing: 8) {
                    Image(systemName: "play.fill")
                        .font(.system(size: 48))
                        .foregroundColor(.white)
                    Text("Video Player")
                        .font(.headline)
                        .foregroundColor(.white)
                    Text("\(playerType) • \(contentId)")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.6))
                }
            )
    }
}
