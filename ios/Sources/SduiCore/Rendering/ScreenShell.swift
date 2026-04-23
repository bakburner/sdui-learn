import SwiftUI

/// Hosts a single SDUI screen. Delegates all data + action orchestration to
/// ``SduiScreenViewModel``; this view's job is layout + lifecycle glue.
///
/// `ScreenShell` never knows which screen type it is hosting — the endpoint
/// string is the only screen-specific input.
public struct ScreenShell: View {
    let endpoint: String

    @Environment(\.sduiConfig) private var config
    @Environment(\.navCoordinator) private var navCoordinator
    @Environment(\.scenePhase) private var scenePhase

    @State private var vm: SduiScreenViewModel?

    public init(endpoint: String) {
        self.endpoint = endpoint
    }

    public var body: some View {
        Group {
            if let vm {
                ScreenBody(vm: vm)
            } else {
                ProgressView()
                    .task { bootstrap() }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            vm?.handleScenePhase(phase)
        }
    }

    @MainActor
    private func bootstrap() {
        guard vm == nil else { return }
        vm = SduiScreenViewModel(
            endpoint: endpoint,
            config: config,
            nav: navCoordinator
        )
    }
}

/// Inner body rendered once the VM is constructed. Split so the VM is
/// guaranteed non-nil inside the closure-heavy `.task`/`.refreshable` hooks.
private struct ScreenBody: View {
    @Bindable var vm: SduiScreenViewModel
    @Environment(\.navCoordinator) private var navCoordinator

    var body: some View {
        ZStack(alignment: .top) {
            Group {
                switch vm.loadState {
                case .idle, .loading:
                    ProgressView("Loading…")
                case .loaded:
                    if let screen = vm.screen {
                        SduiNavigationShell(
                            navigation: screen.navigation,
                            onNavigate: { uri in
                                navCoordinator.push(endpoint: UriResolver.resolveEndpoint(uri: uri))
                            }
                        ) {
                            ScrollView {
                                LazyVStack(spacing: 0) {
                                    ForEach(Array(screen.sections.enumerated()), id: \.offset) { _, section in
                                        SectionLayout(
                                            section: section,
                                            screenState: vm.screenState,
                                            staleness: vm.stalenessPublisher,
                                            dispatcher: vm.makeActionDispatcher().scoped(to: section.id),
                                            onVisibilityChange: { visible in
                                                vm.reportVisibility(sectionID: section.id, visible: visible)
                                            }
                                        )
                                    }
                                }
                            }
                            .refreshable { await vm.load() }
                        }
                        .navigationTitle(screen.title ?? "")
                    } else {
                        ProgressView("Loading…")
                    }
                case .failed(let message):
                    ErrorView(message: message) {
                        Task { await vm.load() }
                    }
                }
            }
            ToastOverlay(host: vm.toasts)
        }
        .task { await vm.load() }
    }
}

/// Wraps a single section with its layout hints (margins, dividers,
/// background, padding). Renderer-internal logic lives here so
/// `ScreenBody` stays a pure list.
private struct SectionLayout: View {
    let section: Section
    let screenState: ScreenState
    let staleness: SectionStalenessTracker
    let dispatcher: ActionDispatcher
    let onVisibilityChange: (Bool) -> Void

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let hints = section.layoutHints
        let marginTop = CGFloat(hints?.marginTop ?? 0)
        let marginBottom = CGFloat(hints?.marginBottom ?? 0)

        VStack(spacing: 0) {
            if hints?.dividerAbove == true { Divider() }

            ZStack(alignment: .topTrailing) {
                SectionErrorBoundary(
                    sectionID: section.id,
                    sectionType: section.type,
                    sectionStates: section.sectionStates,
                    data: sectionDataDict(section),
                    onAction: { dispatcher.dispatch($0) }
                ) {
                    SectionRouter(
                        section: section,
                        screenState: screenState,
                        onAction: { dispatcher.dispatch($0) }
                    )
                }
                .padding(edgeInsets(from: section.padding))
                .background(ColorTokenResolver.resolve(section.backgroundColor, colorScheme: colorScheme) ?? .clear)
                .sduiAccessibility(section.accessibility)

                StalenessBadge(sectionID: section.id, tracker: staleness)
            }
            .padding(.top, marginTop)
            .padding(.bottom, marginBottom)

            if hints?.dividerBelow == true { Divider() }
        }
        // ADR-009: 50% visibility threshold. Dwell + dedup live in
        // `ImpressionTracker`, triggered from `onVisible` fireAndForget
        // actions inside the router. iOS 17 lacks a first-class visibility
        // API, so we approximate with `.onAppear`/`.onDisappear` inside a
        // `LazyVStack` — good enough for entry/exit signalling. iOS 18+
        // can upgrade to `.onScrollVisibilityChange(threshold:)`.
        .onAppear {
            onVisibilityChange(true)
            fireOnVisibleActions()
        }
        .onDisappear {
            onVisibilityChange(false)
        }
    }

    /// Server-declared `onVisible` actions for this section. Dispatched
    /// when the section crosses the visibility threshold; `ImpressionTracker`
    /// inside the dispatcher applies dedup policy.
    @MainActor
    private func fireOnVisibleActions() {
        let actions = (section.actions ?? []).filter { $0.trigger == .onVisible }
        for action in actions { dispatcher.dispatch(action) }
    }

    /// Extracts the section's `data` as a `[String: Any]` for validation
    /// purposes — the error boundary reads `data["ui"]` to detect malformed
    /// AtomicComposite payloads before we attempt to render them.
    private func sectionDataDict(_ section: Section) -> [String: Any]? {
        guard let data = section.data,
              let encoded = try? newJSONEncoder().encode(data),
              let dict = try? JSONSerialization.jsonObject(with: encoded) as? [String: Any]
        else { return nil }
        return dict
    }
}

private struct ErrorView: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Retry", action: retry)
        }
        .padding()
    }
}
