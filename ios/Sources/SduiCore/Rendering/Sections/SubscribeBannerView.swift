import SwiftUI

/// SubscribeBannerView — reserved SDK integration point for inline
/// subscription upsell.
///
/// The entire visible surface is expressed as an atomic tree under
/// `section.data.ui`; this view is a thin walker over that tree via
/// ``AtomicRouter``, identical in behaviour to an AtomicComposite
/// section.
///
/// Outer chrome (margin, radius, gradient background, inner padding)
/// comes from `section.surface` via ``SectionContainer`` — this view
/// only walks the inner atomic tree. See AGENTS.md §15.3.
///
/// `section.data.ctaAction` is the pre-SDK fallback action; once the
/// StoreKit IAP integration lands it will take over the CTA button's
/// tap, reading product identifiers from `section.data.tiers`.
struct SubscribeBannerView: View {
    let section: Section
    let screenState: ScreenState
    let onAction: (Action) -> Void

    var body: some View {
        if let ui = section.data?.ui {
            AtomicRouter(element: ui, screenState: screenState, onAction: onAction, depth: 0)
        }
    }
}
