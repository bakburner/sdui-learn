import Foundation

/// Converts nba:// URIs to SDUI server paths via a simple prefix swap —
/// no special-case branching for individual screens.
public enum UriResolver {
    private static let scheme = "nba://"
    private static let sduiPrefix = "/v1/sdui/"

    /// Convert `nba://for-you` → `/v1/sdui/for-you`
    public static func resolveEndpoint(uri: String) -> String {
        if uri.hasPrefix(scheme) {
            return sduiPrefix + uri.dropFirst(scheme.count)
        }
        // Already a server path — pass through
        return uri
    }
}
