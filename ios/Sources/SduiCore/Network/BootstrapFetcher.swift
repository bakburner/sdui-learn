import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "BootstrapFetcher")

/// Public entry point for the very first composition request a host makes
/// (typically `/sdui/init`, which returns `{ "bootstrapUri": "nba://..." }`).
///
/// Routes through the same envelope + GET/POST + `X-Trace-Id` transport as
/// every other composition request so the bootstrap call is not a parallel,
/// hand-rolled `URLSession.shared.data(from:)` path. Hosts that need to fetch
/// non-screen JSON during bootstrap (e.g. config blobs, splash content) should
/// use this rather than building URLs by hand.
public enum BootstrapFetcher {

    /// Fetch the bootstrap JSON document from `endpoint` and extract the
    /// `bootstrapUri` field. Returns `nil` when the endpoint is unreachable,
    /// returns non-2xx, or the response does not contain a `bootstrapUri`
    /// string — the caller is expected to fall back to a hard-coded URI.
    public static func fetchBootstrapUri(
        config: SduiConfig,
        endpoint: String = "/sdui/init"
    ) async -> String? {
        let repository = SduiRepository(config: config)
        do {
            let payload = try await repository.fetchRawJson(endpoint: endpoint)
            guard let dict = payload as? [String: Any],
                  let uri = dict["bootstrapUri"] as? String else {
                logger.warning("bootstrap response missing bootstrapUri")
                return nil
            }
            return uri
        } catch {
            logger.warning("bootstrap fetch failed: \(error.localizedDescription, privacy: .public)")
            return nil
        }
    }
}
