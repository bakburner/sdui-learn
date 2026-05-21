import Foundation

/// Resolves server-emitted relative asset paths (e.g. `/sdui-demo/team.svg`)
/// against the SDUI composition service base URL used by ``SduiRepository``.
///
/// Web serves the same paths same-origin via the dev proxy; iOS must
/// absolutize before handing URLs to Kingfisher.
enum WireUrlResolver {
    static func resolve(_ url: String?, baseURL: String?) -> String? {
        guard let url else { return nil }
        let trimmed = url.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return trimmed }
        let lower = trimmed.lowercased()
        if lower.hasPrefix("http://") || lower.hasPrefix("https://") {
            return trimmed
        }
        var base = baseURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        while base.hasSuffix("/") { base.removeLast() }
        guard !base.isEmpty else { return trimmed }
        let path = trimmed.hasPrefix("/") ? trimmed : "/\(trimmed)"
        return base + path
    }
}
