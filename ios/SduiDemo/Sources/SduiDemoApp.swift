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
        do {
            let url = config.baseURL.appendingPathComponent("sdui/init")
            let (data, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse, http.statusCode == 200,
                  let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let uri = json["bootstrapUri"] as? String else {
                bootstrapEndpoint = UriResolver.resolveEndpoint(uri: Self.FALLBACK_BOOTSTRAP_URI)
                return
            }
            bootstrapEndpoint = UriResolver.resolveEndpoint(uri: uri)
        } catch {
            bootstrapEndpoint = UriResolver.resolveEndpoint(uri: Self.FALLBACK_BOOTSTRAP_URI)
        }
    }
}
