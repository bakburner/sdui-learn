import Foundation
import Observation
import SwiftUI

/// Observable owner of the SDUI `NavigationPath`. Decouples `ActionDispatcher`
/// (which emits navigation intents) from the SwiftUI view tree (which renders
/// the stack via `NavigationStack(path: $nav.path)`).
///
/// Mirrors Android where `ActionHandler` returns a `NavigateResult` and the
/// view/VM layer performs the push — rather than the handler itself coupling
/// to the navigation host.
@Observable
public final class NavCoordinator {

    /// Navigation stack path. Bind to `NavigationStack(path: $nav.path)` in
    /// the hosting view tree.
    public var path: NavigationPath = NavigationPath()

    /// Flag set when an external URL must be opened via `UIApplication.open`.
    /// Set by `ActionDispatcher`; consumed by the app entry point (since
    /// `UIApplication.shared` isn't available inside the package by default
    /// under strict concurrency).
    public var pendingExternalURL: URL?

    public init() {}

    /// Push an SDUI endpoint onto the stack. The root `NavigationStack`'s
    /// `navigationDestination(for: String.self)` resolves the endpoint to
    /// another `ScreenShell`.
    public func push(endpoint: String) {
        withAnimation(.easeInOut(duration: 0.25)) {
            path.append(endpoint)
        }
    }

    public func pop() {
        guard !path.isEmpty else { return }
        path.removeLast()
    }

    public func popToRoot() {
        path = NavigationPath()
    }

    /// Request external URL open. The hosting app (which has access to
    /// `UIApplication.shared`) observes `pendingExternalURL` and handles it.
    public func openExternal(_ url: URL) {
        pendingExternalURL = url
    }
}

// MARK: - Environment

private struct NavCoordinatorKey: EnvironmentKey {
    static let defaultValue: NavCoordinator = NavCoordinator()
}

public extension EnvironmentValues {
    var navCoordinator: NavCoordinator {
        get { self[NavCoordinatorKey.self] }
        set { self[NavCoordinatorKey.self] = newValue }
    }
}
