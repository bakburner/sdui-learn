import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "SectionRouter")

/// Dispatches each section to the appropriate renderer. Unknown types
/// are silently skipped with a warning log — same shape as the Android
/// and web routers.
///
/// `section.type` is a plain `String` on the wire. The iOS codegen
/// applies a post-processing step (see `codegen/generate.sh`) that
/// rewrites quicktype's inline-enum output for `Section.type` to
/// `String`, so that this switch can use a `default:` branch for
/// forward compatibility the same way the other clients do.
struct SectionRouter: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        // Every section, permanent or AtomicComposite, is wrapped by
        // SectionContainer so outer chrome is server-driven via
        // `section.display`. `SectionContainer` is a no-op when
        // `display` is nil, so AtomicComposites whose root Container
        // already carries its own padding/background/shadow are
        // unaffected — composers opt into outer margin/chrome by
        // emitting a `display` block on the section envelope.
        switch section.type {
        case "AtomicComposite":
            if let ui = section.data?.ui {
                SectionContainer(display: section.display) {
                    AtomicRouter(element: ui, screenState: screenState, onAction: onAction, depth: 0)
                }
            }

        case "GamePanel":
            SectionContainer(display: section.display) {
                GamePanelView(section: section, onAction: onAction)
            }

        case "BoxscoreTable":
            SectionContainer(display: section.display) {
                BoxscoreTableView(section: section, onAction: onAction)
            }

        case "SeasonLeadersTable":
            SectionContainer(display: section.display) {
                SeasonLeadersTableView(section: section, onAction: onAction)
            }

        case "TabGroup":
            SectionContainer(display: section.display) {
                TabGroupView(section: section, screenState: screenState, onAction: onAction)
            }

        case "Form":
            SectionContainer(display: section.display) {
                FormSectionView(section: section, screenState: screenState, onAction: onAction)
            }

        case "SubscribeHero":
            SectionContainer(display: section.display) {
                SubscribeHeroView(section: section, onAction: onAction)
            }

        case "SubscribeBanner":
            SectionContainer(display: section.display) {
                SubscribeBannerView(section: section, onAction: onAction)
            }

        case "AdSlot":
            SectionContainer(display: section.display) {
                AdSlotView(section: section, onAction: onAction)
            }

        case "VideoPlayer":
            SectionContainer(display: section.display) {
                VideoPlayerStubView(section: section, onAction: onAction)
            }

        default:
            EmptyView()
                .onAppear {
                    logger.warning("skipping unknown section type=\(section.type, privacy: .public) id=\(section.id, privacy: .public)")
                }
        }
    }
}
