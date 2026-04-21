import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "SectionRouter")

/// Dispatches each section to the appropriate renderer. Unknown types are
/// silently skipped with a warning log.
struct SectionRouter: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        switch section.type {
        case .atomicComposite:
            if let ui = section.data?.ui {
                AtomicRouter(element: ui, screenState: screenState, onAction: onAction, depth: 0)
            }

        case .gamePanel:
            GamePanelView(section: section, onAction: onAction)

        case .boxscoreTable:
            BoxscoreTableView(section: section, onAction: onAction)

        case .seasonLeadersTable:
            SeasonLeadersTableView(section: section, onAction: onAction)

        case .tabGroup:
            TabGroupView(section: section, screenState: screenState, onAction: onAction)

        case .form:
            FormSectionView(section: section, screenState: screenState, onAction: onAction)

        case .subscribeHero:
            SubscribeHeroView(section: section, onAction: onAction)

        case .subscribeBanner:
            SubscribeBannerView(section: section, onAction: onAction)

        case .adSlot:
            AdSlotView(section: section, onAction: onAction)

        case .videoPlayer:
            VideoPlayerStubView(section: section, onAction: onAction)

        case .unknown(let rawType):
            EmptyView()
                .onAppear {
                    logger.warning("skipping unknown section type=\(rawType, privacy: .public) id=\(section.id, privacy: .public)")
                }
        }
    }
}
