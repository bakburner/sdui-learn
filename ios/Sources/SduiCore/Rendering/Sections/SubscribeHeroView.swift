import SwiftUI

/// SubscribeHeroView — reserved SDK integration point for the full-
/// screen subscription upsell.
///
/// The entire visible surface (logo, title, subtitle, feature list,
/// tier cards, CTAs) is expressed as an atomic tree under
/// `section.data.ui`; this view is a thin walker over that tree via
/// ``AtomicRouter``, identical in behaviour to an AtomicComposite
/// section.
///
/// Outer chrome (margin, radius, gradient background, inner padding)
/// comes from `section.surface` via ``SectionContainer`` — this view
/// only walks the inner atomic tree. See AGENTS.md §15.3.
///
/// `section.data.tiers` carries IAP product identifiers reserved for
/// the future StoreKit integration; the renderer reads nothing from it
/// today.
struct SubscribeHeroView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        if let ui = section.data?.ui {
            AtomicRouter(element: ui, screenState: screenState, onAction: onAction, depth: 0)
        }
    }
}
