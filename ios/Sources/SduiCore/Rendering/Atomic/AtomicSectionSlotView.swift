import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "AtomicSectionSlot")
private let maxSlotDepth = 2

/// Bridges an atomic element back to SectionRouter for embedded sections.
struct AtomicSectionSlotView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    var slotDepth: Int = 0

    var body: some View {
        if slotDepth >= maxSlotDepth {
            let _ = logger.warning("SectionSlot recursion limit reached at depth \(slotDepth) — skipping")
            EmptyView()
        } else if let section = element.section {
            SectionRouter(section: section, screenState: screenState, onAction: onAction)
        }
    }
}
