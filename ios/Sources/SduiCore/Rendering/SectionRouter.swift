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
        // `section.surface`. `SectionContainer` is a no-op when
        // `surface` is nil, so AtomicComposites whose root Container
        // already carries its own padding/background/shadow are
        // unaffected — composers opt into outer margin/chrome by
        // emitting a `surface` block on the section envelope.
        switch section.type {
        case "AtomicComposite":
            if let ui = section.data?.ui {
                SectionContainer(surface: section.surface) {
                    AtomicRouter(element: ui, screenState: screenState, onAction: onAction, depth: 0)
                        .environment(\.compositeContent, section.data?.content)
                }
            }

        case "BoxscoreTable":
            SectionContainer(surface: section.surface) {
                BoxscoreTableView(section: section, onAction: onAction)
            }

        case "SeasonLeadersTable":
            SectionContainer(surface: section.surface) {
                SeasonLeadersTableView(section: section, onAction: onAction)
            }

        case "TabGroup":
            SectionContainer(surface: section.surface) {
                TabGroupView(section: section, screenState: screenState, onAction: onAction)
            }

        case "Form":
            SectionContainer(surface: section.surface) {
                FormSectionView(section: section, screenState: screenState, onAction: onAction)
            }

        case "SubscribeHero":
            SectionContainer(surface: section.surface) {
                SubscribeHeroView(section: section, screenState: screenState, onAction: onAction)
            }

        case "SubscribeBanner":
            SectionContainer(surface: section.surface) {
                SubscribeBannerView(section: section, screenState: screenState, onAction: onAction)
            }

        case "AdSlot":
            SectionContainer(surface: section.surface) {
                AdSlotView(section: section, onAction: onAction)
            }

        case "VideoPlayer":
            SectionContainer(surface: section.surface) {
                VideoPlayerStubView(section: section, screenState: screenState, onAction: onAction)
            }

        default:
            EmptyView()
                .onAppear {
                    logger.warning("skipping unknown section type=\(section.type, privacy: .public) id=\(section.id, privacy: .public)")
                }
        }
    }
}
