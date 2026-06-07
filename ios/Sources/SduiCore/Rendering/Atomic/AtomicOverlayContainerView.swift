import SwiftUI
import os

private let overlayLogger = Logger(subsystem: "com.nba.sdui", category: "AtomicOverlayContainer")

struct AtomicOverlayContainerView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        Group {
            if let base = element.base {
                AtomicRouter(element: base, screenState: screenState, onAction: onAction, depth: depth)
                    .overlay {
                        ZStack {
                            ForEach(Array((element.overlays ?? []).enumerated()), id: \.offset) { _, overlay in
                                overlayLayer(overlay)
                                    .frame(
                                        maxWidth: .infinity,
                                        maxHeight: .infinity,
                                        alignment: swiftUIAlignment(for: overlay.alignment)
                                    )
                            }
                        }
                    }
                .applyActionTriggers(element.actions, onAction: onAction)
                .sduiAccessibility(element.accessibility)
                .atomicBox(element, screenState: screenState, onAction: onAction)
            } else {
                EmptyView()
                    .onAppear {
                        overlayLogger.warning("missing OverlayContainer base id=\(element.id ?? "nil", privacy: .public)")
                    }
            }
        }
    }

    @ViewBuilder
    private func overlayLayer(_ overlay: AtomicOverlay) -> some View {
        let el = overlay.element
        let fillWidth = el.widthMode == .fill
        let fillHeight = el.heightMode == .fill

        AtomicRouter(element: el, screenState: screenState, onAction: onAction, depth: depth)
            .frame(
                maxWidth: fillWidth ? .infinity : nil,
                maxHeight: fillHeight ? .infinity : nil
            )
            .padding(edgeInsets(from: overlay.inset))
    }

    private func swiftUIAlignment(for alignment: BadgeAlignment?) -> SwiftUI.Alignment {
        switch alignment {
        case .topStart: return .topLeading
        case .topCenter: return .top
        case .topEnd: return .topTrailing
        case .centerStart: return .leading
        case .center: return .center
        case .centerEnd: return .trailing
        case .bottomStart: return .bottomLeading
        case .bottomEnd: return .bottomTrailing
        case .bottomCenter, .none: return .bottom
        }
    }
}
