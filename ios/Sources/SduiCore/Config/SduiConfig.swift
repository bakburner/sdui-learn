import Foundation
import SwiftUI

/// Runtime configuration injected into `SduiCore` from the hosting app.
///
/// Mirrors Android's `SduiScreenConfig(baseUrl, ablyTokenUrl)`. Provided to the
/// view tree via `@Environment(\.sduiConfig)`.
public struct SduiConfig: Sendable, Equatable {
    /// Base URL of the SDUI composition service (e.g. `http://localhost:8080`).
    public let baseURL: URL

    /// Token endpoint the Ably SDK calls to mint short-lived JWTs. Matches
    /// `SduiScreenConfig.ablyTokenUrl` on Android.
    public let ablyTokenURL: URL

    /// Optional bearer token forwarded as the `Authorization` header on every
    /// screen fetch (per ADR-003 request envelope). Nil = unauthenticated.
    public let authorizationToken: String?

    /// Optional correlation-id forwarded as the `X-Correlation-ID` header.
    /// Auto-generated per-request if nil.
    public let correlationIdProvider: @Sendable () -> String

    /// A/B experiment assignments forwarded as `experiments[<id>]=<variant>` in
    /// every request envelope. Mirrors Android's `SduiScreenConfig.experiments`.
    public let experiments: [String: String]

    public init(
        baseURL: URL,
        ablyTokenURL: URL,
        authorizationToken: String? = nil,
        correlationIdProvider: @Sendable @escaping () -> String = { UUID().uuidString },
        experiments: [String: String] = [:]
    ) {
        self.baseURL = baseURL
        self.ablyTokenURL = ablyTokenURL
        self.authorizationToken = authorizationToken
        self.correlationIdProvider = correlationIdProvider
        self.experiments = experiments
    }

    /// Returns a copy with the given experiment assigned, replacing any
    /// previous value for the same id.
    public func withExperiment(_ id: String, variant: String) -> SduiConfig {
        var updated = experiments
        updated[id] = variant
        return SduiConfig(
            baseURL: baseURL,
            ablyTokenURL: ablyTokenURL,
            authorizationToken: authorizationToken,
            correlationIdProvider: correlationIdProvider,
            experiments: updated
        )
    }

    public static func == (lhs: SduiConfig, rhs: SduiConfig) -> Bool {
        lhs.baseURL == rhs.baseURL
            && lhs.ablyTokenURL == rhs.ablyTokenURL
            && lhs.authorizationToken == rhs.authorizationToken
            && lhs.experiments == rhs.experiments
    }
}

// MARK: - Environment

private struct SduiConfigKey: EnvironmentKey {
    static let defaultValue: SduiConfig = SduiConfig(
        baseURL: URL(string: "http://localhost:8080")!,
        ablyTokenURL: URL(string: "http://localhost:8080/rttoken")!
    )
}

public extension EnvironmentValues {
    var sduiConfig: SduiConfig {
        get { self[SduiConfigKey.self] }
        set { self[SduiConfigKey.self] = newValue }
    }
}

public extension View {
    /// Inject an `SduiConfig` into the environment for all descendant SDUI views.
    func sduiConfig(_ config: SduiConfig) -> some View {
        environment(\.sduiConfig, config)
    }
}
