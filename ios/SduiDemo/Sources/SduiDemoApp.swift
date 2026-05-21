import SwiftUI
import SduiCore

@main
struct SduiDemoApp: App {
    // Degraded-connectivity fallback only — primary bootstrap URI comes from /sdui/init.
    private static let FALLBACK_BOOTSTRAP_URI = "nba://for-you"

    @State private var nav = NavCoordinator()
    @State private var bootstrapEndpoint: String?

    private let config = SduiConfig(
        baseURL: URL(string: "http://localhost:8080")!,
        ablyTokenURL: URL(string: "http://localhost:8080/rttoken")!
    )

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
