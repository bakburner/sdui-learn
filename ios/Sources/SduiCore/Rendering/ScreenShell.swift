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
    @Environment(\.sduiNavigateHome) private var navigateHome
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("sdui_color_scheme") private var colorSchemePreference = "system"

    var body: some View {
        ZStack(alignment: .top) {
            Group {
                switch vm.loadState {
                case .idle, .loading:
                    shellWrapped {
                        ProgressView("Loading…")
                    }
                case .loaded:
                    if let screen = vm.screen {
                        shellWrapped {
                            ScrollView {
                                let insets = edgeInsets(from: screen.contentInsets)
                                LazyVStack(alignment: .leading, spacing: 0) {
                                    ForEach(screen.sections, id: \.id) { section in
                                        let sectionDispatcher = vm.makeActionDispatcher().scoped(to: section.id)
                                        SectionLayout(
                                            section: section,
                                            horizontalInsets: EdgeInsets(top: 0, leading: insets.leading, bottom: 0, trailing: insets.trailing),
                                            screenState: vm.screenState,
                                            staleness: vm.stalenessPublisher,
                                            dispatcher: sectionDispatcher,
                                            onVisibilityChange: { visible in
                                                vm.reportVisibility(sectionID: section.id, visible: visible)
                                            }
                                        )
                                        .environment(\.batchActionExecutor, { actions in
                                            sectionDispatcher.execute(actions)
                                        })
                                    }
                                }
                                .padding(.top, insets.top)
                                .padding(.bottom, insets.bottom)
                            }
                            .refreshable { await vm.load() }
                            .environment(\.wireAssetBaseURL, vm.wireAssetBaseURL)
                        }
                    } else {
                        shellWrapped {
                            ProgressView("Loading…")
                        }
                    }
                case .failed(let message):
                    shellWrapped {
                        ErrorView(
                            message: message,
                            canNavigateBack: !navCoordinator.path.isEmpty
                                || (vm.shellScreen?.parentURI?.isEmpty == false),
                            onNavigateBack: { navigateBack() },
                            onRetry: { Task { await vm.load() } }
                        )
                    }
                case .upgradeRequired(let message):
                    shellWrapped {
                        UpgradeRequiredView(
                            message: message,
                            onNavigateHome: { navigateBack() }
                        )
                    }
                }
            }
            .animation(.easeInOut(duration: 0.2), value: vm.loadState)
            .transition(.opacity)
            ToastOverlay(host: vm.toasts)
        }
        .preferredColorScheme(preferredColorScheme)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(themeToggleTitle) {
                    toggleColorScheme()
                }
            }
        }
        .task { await vm.load() }
    }

    private var effectiveColorScheme: ColorScheme {
        switch colorSchemePreference {
        case "light": return .light
        case "dark": return .dark
        default: return colorScheme
        }
    }

    private var preferredColorScheme: ColorScheme? {
        switch colorSchemePreference {
        case "light": return .light
        case "dark": return .dark
        default: return nil
        }
    }

    private var themeToggleTitle: String {
        effectiveColorScheme == .dark ? "Light" : "Dark"
    }

    private func toggleColorScheme() {
        colorSchemePreference = effectiveColorScheme == .dark ? "light" : "dark"
    }

    @ViewBuilder
    private func shellWrapped<Content: View>(@ViewBuilder content: @escaping () -> Content) -> some View {
        SduiNavigationShell(
            navigation: vm.shellScreen?.navigation,
            onNavigate: { uri in
                navCoordinator.push(endpoint: UriResolver.resolveEndpoint(uri: uri))
            },
            content: content
        )
    }

    private func navigateBack() {
        if !navCoordinator.path.isEmpty {
            navCoordinator.pop()
        } else if let parent = vm.shellScreen?.parentURI, !parent.isEmpty {
            navCoordinator.push(endpoint: UriResolver.resolveEndpoint(uri: parent))
        } else if let navigateHome {
            navigateHome()
        } else {
            navCoordinator.popToRoot()
        }
    }
}

/// Wraps a single section with the screen's horizontal insets and dispatches
/// `onVisible` actions on appear/disappear. Section outer chrome (margin,
/// padding, background, cornerRadius, shadow, border) is owned by
/// `SectionContainer` reading `section.surface`.
private struct SectionLayout: View {
    let section: Section
    let horizontalInsets: EdgeInsets
    let screenState: ScreenState
    let staleness: SectionStalenessTracker
    let dispatcher: ActionDispatcher
    let onVisibilityChange: (Bool) -> Void

    var body: some View {
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
            .frame(maxWidth: .infinity, alignment: .leading)
            .sduiAccessibility(section.accessibility)

            StalenessBadge(sectionID: section.id, tracker: staleness)
        }
        .padding(.leading, horizontalInsets.leading)
        .padding(.trailing, horizontalInsets.trailing)
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
        if !actions.isEmpty { dispatcher.execute(actions) }
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

private enum ClientChromeSpacing {
    static let lg = LayoutTokenResolver.cgFloat(.string("token:nba.spacing.lg"))
    static let md = LayoutTokenResolver.cgFloat(.string("token:nba.spacing.md"))
    static let sm = LayoutTokenResolver.cgFloat(.string("token:nba.spacing.sm"))
}

private struct ErrorView: View {
    let message: String
    let canNavigateBack: Bool
    let onNavigateBack: () -> Void
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: ClientChromeSpacing.lg) {
            Image(systemName: "exclamationmark.triangle")
                .font(.largeTitle)
                .foregroundStyle(.secondary)
            Text("Failed to load screen")
                .font(.headline)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            HStack(spacing: ClientChromeSpacing.md) {
                Button(canNavigateBack ? "Go back" : "Home", action: onNavigateBack)
                    .buttonStyle(.bordered)
                Button("Retry", action: onRetry)
                    .buttonStyle(.borderedProminent)
            }
        }
        .padding(ClientChromeSpacing.lg)
    }
}

private struct UpgradeRequiredView: View {
    let message: String
    let onNavigateHome: () -> Void

    var body: some View {
        VStack(spacing: ClientChromeSpacing.md) {
            Text("Update Required")
                .font(.title2)
                .fontWeight(.bold)
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Home", action: onNavigateHome)
                .buttonStyle(.bordered)
        }
        .padding(ClientChromeSpacing.lg)
    }
}

