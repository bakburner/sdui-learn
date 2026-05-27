import SwiftUI
import SduiCore

@main
struct SduiDemoApp: App {
    // Degraded-connectivity fallback only — primary bootstrap URI comes from /sdui/init.
    private static let FALLBACK_BOOTSTRAP_URI = "nba://for-you"

    @State private var nav = NavCoordinator()
    @State private var bootstrapEndpoint: String?

    private let config: SduiConfig = {
        // Precedence (highest wins):
        //   1. `SDUI_IOS_BASE_URL` env var — set by `make dev-ios-{local,remote}`
        //      via `simctl launch --env`. Only present on the Make-launched run.
        //   2. `SduiBaseURL` UserDefault — persisted from step 1 on the most
        //      recent Make-launched run. Survives springboard relaunches and
        //      iOS auto-relaunches after a crash, so the app stays on whichever
        //      server the developer started with instead of silently flipping
        //      to the hardcoded fallback when the env var disappears.
        //   3. `http://localhost:8080` — safe dev default (matches dev-ios-local).
        let defaults = UserDefaults.standard
        let envURL = ProcessInfo.processInfo.environment["SDUI_IOS_BASE_URL"]
        if let envURL, !envURL.isEmpty {
            defaults.set(envURL, forKey: "SduiBaseURL")
        }
        let urlString = envURL
            ?? defaults.string(forKey: "SduiBaseURL")
            ?? "http://localhost:8080"
        return SduiConfig(
            baseURL: URL(string: urlString)!,
            ablyTokenURL: URL(string: "https://identity.nba.com/rttoken")!
        )
    }()

    init() {
        CrashLogger.install()
        CrashLogger.trace("SduiDemoApp init baseURL=\(config.baseURL.absoluteString)")
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if let endpoint = bootstrapEndpoint {
                    NavigationStack(path: $nav.path) {
                        ScreenShell(endpoint: endpoint)
                            .navigationDestination(for: String.self) { ep in
                                ScreenShell(endpoint: ep)
                            }
                    }
                } else {
                    ProgressView()
                }
            }
            .sduiConfig(config)
            .environment(\.navCoordinator, nav)
            .environment(\.sduiNavigateHome) {
                nav.popToRoot()
                bootstrapEndpoint = UriResolver.resolveEndpoint(uri: Self.FALLBACK_BOOTSTRAP_URI)
            }
            .task { await fetchBootstrapUri() }
            .onChange(of: nav.pendingExternalURL) { _, newValue in
                if let url = newValue {
                    UIApplication.shared.open(url)
                    nav.pendingExternalURL = nil
                }
            }
        }
    }

    private func fetchBootstrapUri() async {
        let uri = await BootstrapFetcher.fetchBootstrapUri(config: config)
            ?? Self.FALLBACK_BOOTSTRAP_URI
        bootstrapEndpoint = UriResolver.resolveEndpoint(uri: uri)
    }
}
