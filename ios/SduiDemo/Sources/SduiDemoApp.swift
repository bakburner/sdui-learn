import SwiftUI
import SduiCore

@main
struct SduiDemoApp: App {
    // TODO: Bootstrap URI should come from a /sdui/init endpoint.
    //       Hardcoded here only as a temporary prototype bootstrap.
    private static let BOOTSTRAP_URI = "nba://for-you"

    @State private var nav = NavCoordinator()

    private let config = SduiConfig(
        baseURL: URL(string: "http://localhost:8080")!,
        ablyTokenURL: URL(string: "http://localhost:8080/rttoken")!
    )

    var body: some Scene {
        WindowGroup {
            NavigationStack(path: $nav.path) {
                ScreenShell(endpoint: UriResolver.resolveEndpoint(uri: Self.BOOTSTRAP_URI))
                    .navigationDestination(for: String.self) { endpoint in
                        ScreenShell(endpoint: endpoint)
                    }
            }
            .sduiConfig(config)
            .environment(\.navCoordinator, nav)
            .onChange(of: nav.pendingExternalURL) { _, newValue in
                if let url = newValue {
                    UIApplication.shared.open(url)
                    nav.pendingExternalURL = nil
                }
            }
        }
    }
}
