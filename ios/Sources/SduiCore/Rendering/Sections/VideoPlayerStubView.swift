import SwiftUI

/// VideoPlayerStubView — reserved SDK integration point for the video player.
///
/// Today the visible surface is the pre-SDK placeholder composed by the
/// server as an atomic tree under `section.data.ui`; this view is a thin
/// walker over that tree via ``AtomicRouter``. Once the platform video
/// SDK (HLS/DASH, PiP, AirPlay) lands it mounts here using the SDK
/// inputs at the top of `section.data` (`playerType`, `contentID`,
/// `autoplay`, `capabilities`, `displayConfig`) and the atomic tree
/// becomes the SDK's loading / error placeholder.
///
/// Outer chrome comes from `section.surface` via ``SectionContainer``.
struct VideoPlayerStubView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        if let ui = section.data?.ui {
            AtomicRouter(element: ui, screenState: screenState, onAction: onAction, depth: 0)
        }
    }
}
