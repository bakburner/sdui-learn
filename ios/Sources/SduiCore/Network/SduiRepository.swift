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
    func fetchScreen(
        endpoint: String,
        variant: String? = nil
    ) async throws -> SduiModels {
        var envelope = envelopeProvider()
        if let variant {
            envelope.gameState = variant
        }

        let traceID = config.traceIDProvider()
        let request = try buildRequest(endpoint: endpoint, envelope: envelope, traceID: traceID)
        logger.debug("Fetching screen \(request.httpMethod ?? "GET"): \(request.url?.absoluteString ?? endpoint) [trace=\(traceID, privacy: .public)]")

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
            activeScreenFetchTraceID = traceID
        }
        defer {
            fetchStateLock.withLock {
                if activeScreenFetchTraceID == traceID {
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

    private func buildRequest(
        endpoint: String,
        envelope: RequestEnvelope,
        traceID: String
    ) throws -> URLRequest {
        let path = endpoint.hasPrefix("/") ? endpoint : "/\(endpoint)"
        let baseWithPath = config.baseURL.appendingPathComponent(
            String(path.dropFirst())
        )

        if envelope.exceedsGetThreshold {
            var request = URLRequest(url: baseWithPath)
            request.httpMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = try envelope.jsonBody()
            attachAuthHeaders(&request, traceID: traceID)
            return request
        }

        let query = envelope.buildQueryString()
        let urlString = baseWithPath.absoluteString + "?" + query
        guard let url = URL(string: urlString) else {
            throw SduiError.invalidURL(endpoint)
        }
        var request = URLRequest(url: url)
        attachAuthHeaders(&request, traceID: traceID)
        return request
    }

    private func attachAuthHeaders(_ request: inout URLRequest, traceID: String) {
        if let token = config.authorizationToken {
            request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }
        request.setValue(traceID, forHTTPHeaderField: "X-Trace-Id")
    }
}

enum SduiError: LocalizedError, Equatable {
    case invalidURL(String)
    case invalidResponse
    case httpError(statusCode: Int)
    case decodingFailed(String)

    var errorDescription: String? {
        switch self {
        case .invalidURL(let path): return "Invalid URL for endpoint: \(path)"
        case .invalidResponse: return "Invalid HTTP response"
        case .httpError(let code): return "HTTP error: \(code)"
        case .decodingFailed(let message): return "Decoding failed: \(message)"
        }
    }
}
