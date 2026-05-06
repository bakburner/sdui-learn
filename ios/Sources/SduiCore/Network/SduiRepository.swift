import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "SduiRepository")

/// Single entry point for all SDUI network requests — one generic fetch
/// method, no dedicated `getGameDetail()` helpers.
///
/// Transport contract (ADR-003 / plan-request-transport):
/// - Bracket-notation GET query params below 8192 chars, otherwise POST with
///   the identical envelope as a JSON body.
/// - Only `Authorization` and `X-Trace-Id` headers. Deprecated `X-Platform`
///   and `X-Schema-Version` headers are no longer sent.
final class SduiRepository {
    private let config: SduiConfig
    private let session: URLSession
    private let envelopeProvider: @Sendable () -> RequestEnvelope
    private let fetchStateLock = NSLock()
    private var activeScreenFetch: Task<SduiModels, Error>?
    /// Per-request `X-Trace-Id` of the current in-flight screen fetch. Reused
    /// as the active-fetch identity so the deferred cleanup only nils the slot
    /// when no newer fetch has overwritten it (Task is a struct, so identity
    /// comparison via `===` is unavailable).
    private var activeScreenFetchTraceID: String?

    deinit {
        fetchStateLock.withLock { activeScreenFetch?.cancel() }
    }

    init(
        config: SduiConfig,
        session: URLSession = .shared,
        envelopeProvider: @Sendable @escaping () -> RequestEnvelope = { RequestEnvelope() }
    ) {
        self.config = config
        self.session = session
        self.envelopeProvider = envelopeProvider
    }

    /// Fetch a screen from any SDUI endpoint. The endpoint is always provided
    /// by caller/server — never hardcoded.
    ///
    /// - Parameters:
    ///   - endpoint: server-relative path (e.g. `/sdui/scoreboard`).
    ///   - userParams: optional user-supplied filter params (e.g. Form submit
    ///     bindings like `season=2025-26`). Always travel in the URL query
    ///     string regardless of GET vs POST so the server reads them
    ///     uniformly. They participate in the GET/POST length decision.
    ///   - traceID: optional trace ID to reuse from a parent fetch (e.g.
    ///     parameterized refresh inheriting its screen's trace). Falls back
    ///     to the config's provider when absent.
    func fetchScreen(
        endpoint: String,
        userParams: [String: String] = [:],
        traceID: String? = nil
    ) async throws -> SduiModels {
        let envelope = envelopeProvider()

        let resolvedTraceID = traceID ?? config.traceIDProvider()
        let request = try buildRequest(
            endpoint: endpoint,
            envelope: envelope,
            userParams: userParams,
            traceID: resolvedTraceID
        )
        logger.debug("Fetching screen \(request.httpMethod ?? "GET"): \(request.url?.absoluteString ?? endpoint) [trace=\(resolvedTraceID, privacy: .public)]")

        fetchStateLock.withLock { activeScreenFetch?.cancel() }

        let httpSession = self.session
        let task: Task<SduiModels, Error> = Task {
            let (data, response) = try await httpSession.data(for: request)

            guard let httpResponse = response as? HTTPURLResponse else {
                throw SduiError.invalidResponse
            }

            guard (200...299).contains(httpResponse.statusCode) else {
                logger.error("HTTP \(httpResponse.statusCode) for \(endpoint)")
                throw SduiError.httpError(statusCode: httpResponse.statusCode)
            }

            // Detect server-signaled schema version mismatch
            if httpResponse.value(forHTTPHeaderField: "X-Schema-Version-Mismatch") == "upgrade-required" {
                logger.warning("Server signaled schema version mismatch: upgrade-required")
                throw SduiError.upgradeRequired
            }

            do {
                let screen = try SduiModels(data: data)
                logger.debug("Fetched screen '\(screen.id)' with \(screen.sections.count) sections")
                return screen
            } catch {
                let detail = Self.describeDecodingError(error)
                logger.error("Failed to decode screen from \(endpoint, privacy: .public): \(detail, privacy: .public)")
                throw SduiError.decodingFailed(detail)
            }
        }
        fetchStateLock.withLock {
            activeScreenFetch = task
            activeScreenFetchTraceID = resolvedTraceID
        }
        defer {
            fetchStateLock.withLock {
                if activeScreenFetchTraceID == resolvedTraceID {
                    activeScreenFetch = nil
                    activeScreenFetchTraceID = nil
                }
            }
        }
        return try await task.value
    }

    /// Expand a `DecodingError` into `keyNotFound/typeMismatch/valueNotFound/dataCorrupted`
    /// with the failing JSON path and the underlying reason. `localizedDescription`
    /// hides that detail, which makes on-device decode failures unreadable.
    private static func describeDecodingError(_ error: Error) -> String {
        guard let decodingError = error as? DecodingError else {
            return error.localizedDescription
        }
        let path: (DecodingError.Context) -> String = { ctx in
            ctx.codingPath.map { $0.stringValue }.joined(separator: ".")
        }
        switch decodingError {
        case let .keyNotFound(key, ctx):
            return "keyNotFound '\(key.stringValue)' at '\(path(ctx))' — \(ctx.debugDescription)"
        case let .typeMismatch(type, ctx):
            return "typeMismatch expected=\(type) at '\(path(ctx))' — \(ctx.debugDescription)"
        case let .valueNotFound(type, ctx):
            return "valueNotFound \(type) at '\(path(ctx))' — \(ctx.debugDescription)"
        case let .dataCorrupted(ctx):
            return "dataCorrupted at '\(path(ctx))' — \(ctx.debugDescription)"
        @unknown default:
            return String(describing: decodingError)
        }
    }

    /// Fetch raw JSON from a direct URL (for poll refresh with `dataPath` extraction).
    /// - Parameter traceID: Optional trace ID to reuse from the parent screen fetch.
    ///   Falls back to generating a fresh ID if nil.
    func fetchRawJson(url: URL, traceID: String? = nil) async throws -> Any {
        var request = URLRequest(url: url)
        attachAuthHeaders(&request, traceID: traceID ?? config.traceIDProvider())

        let (data, response) = try await session.data(for: request)

        guard let httpResponse = response as? HTTPURLResponse,
              (200...299).contains(httpResponse.statusCode) else {
            throw SduiError.invalidResponse
        }

        return try JSONSerialization.jsonObject(with: data)
    }

    // MARK: - Request assembly

    /// Build a request for the canonical SDUI transport. The envelope owns the
    /// GET-vs-POST decision (length-based via `exceedsGetThreshold`); user
    /// params always travel in the URL query string regardless of method, so
    /// the server reads them through the same `@RequestParam` path on either
    /// side. Both halves of the query string use RFC-3986 percent-encoding so
    /// values with `&`, `=`, spaces, or non-ASCII bytes survive the round-trip
    /// intact.
    private func buildRequest(
        endpoint: String,
        envelope: RequestEnvelope,
        userParams: [String: String],
        traceID: String
    ) throws -> URLRequest {
        let path = endpoint.hasPrefix("/") ? endpoint : "/\(endpoint)"
        let baseWithPath = config.baseURL.appendingPathComponent(
            String(path.dropFirst())
        )

        let userQuery = Self.encodeUserParams(userParams)
        // Threshold includes both halves so large userParams trigger POST too.
        let envelopeQuery = envelope.buildQueryString()
        let combinedLength = envelopeQuery.count + (userQuery.isEmpty ? 0 : userQuery.count + 1)

        if combinedLength > RequestEnvelope.maxQueryLength {
            let postURL: URL
            if userQuery.isEmpty {
                postURL = baseWithPath
            } else {
                guard let url = URL(string: baseWithPath.absoluteString + "?" + userQuery) else {
                    throw SduiError.invalidURL(endpoint)
                }
                postURL = url
            }
            var request = URLRequest(url: postURL)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try envelope.jsonBody()
            attachAuthHeaders(&request, traceID: traceID, envelope: envelope)
            return request
        }

        let combined = userQuery.isEmpty ? envelopeQuery : userQuery + "&" + envelopeQuery
        let urlString = baseWithPath.absoluteString + "?" + combined
        guard let url = URL(string: urlString) else {
            throw SduiError.invalidURL(endpoint)
        }
        var request = URLRequest(url: url)
        attachAuthHeaders(&request, traceID: traceID, envelope: envelope)
        return request
    }

    /// Percent-encode user params with the same RFC-3986 rule the envelope
    /// uses, preserving deterministic order so the encoded URL is stable
    /// across calls (matters for parity tests and CDN cache keys).
    private static func encodeUserParams(_ params: [String: String]) -> String {
        guard !params.isEmpty else { return "" }
        return params
            .filter { !$0.value.isEmpty }
            .sorted(by: { $0.key < $1.key })
            .map { "\(RequestEnvelope.percentEncode($0.key))=\(RequestEnvelope.percentEncode($0.value))" }
            .joined(separator: "&")
    }

    private func attachAuthHeaders(_ request: inout URLRequest, traceID: String, envelope: RequestEnvelope? = nil) {
        if let token = config.authorizationToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.setValue(traceID, forHTTPHeaderField: "X-Trace-Id")
        request.setValue(UUID().uuidString, forHTTPHeaderField: "X-Request-Id")
        if let deviceID = envelope?.deviceID {
            request.setValue(deviceID, forHTTPHeaderField: "X-Device-Id")
        }
        if let envelope {
            request.setValue(envelope.platformName, forHTTPHeaderField: "X-Platform")
            if let appVersion = envelope.appVersion {
                request.setValue(appVersion, forHTTPHeaderField: "X-App-Version")
            }
            request.setValue(envelope.osVersion, forHTTPHeaderField: "X-OS-Version")
        }
        // TODO(edge): placeholder — edge worker will set these from client IP
        request.setValue("US", forHTTPHeaderField: "X-Resolved-Country")
        request.setValue("MARKET_UNKNOWN", forHTTPHeaderField: "X-Resolved-Market-Cohort")
    }
}

enum SduiError: LocalizedError, Equatable {
    case invalidURL(String)
    case invalidResponse
    case httpError(statusCode: Int)
    case decodingFailed(String)
    case upgradeRequired

    var errorDescription: String? {
        switch self {
        case .invalidURL(let path): return "Invalid URL for endpoint: \(path)"
        case .invalidResponse: return "Invalid HTTP response"
        case .httpError(let code): return "HTTP error: \(code)"
        case .decodingFailed(let message): return "Decoding failed: \(message)"
        case .upgradeRequired: return "This version of the app is no longer supported. Please update to continue."
        }
    }
}
