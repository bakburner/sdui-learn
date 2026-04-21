import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "IconTokenResolver")

/// Resolves cross-platform SDUI icon tokens (e.g. `sdui:play`) to the
/// platform-native asset. On iOS the resolved value is a
/// [SF Symbol](https://developer.apple.com/sf-symbols/) name suitable
/// for `Image(systemName:)`.
///
/// The token vocabulary lives in `schema/icon-tokens.json`; unknown
/// tokens fall back to `sdui:warning`. Non-token strings pass through
/// unchanged so renderers can keep accepting raw SF Symbol names for
/// back-compat while the server migrates to tokens.
public struct IconTokenResolver {

    /// Shared resolver, lazily initialised from the bundled snapshot.
    public static let shared: IconTokenResolver = {
        IconTokenResolver(tokens: defaultTokens)
    }()

    private let tokens: [String: String]

    public init(tokens: [String: String]) {
        self.tokens = tokens
    }

    /// Convert a server icon string into a platform-native symbol name.
    ///
    /// - Returns: the SF Symbol name when `token` is an SDUI token we
    ///   recognise; the fallback token's symbol when the token is unknown;
    ///   the input unchanged when it isn't an `sdui:` token at all.
    public func resolve(_ token: String?) -> String? {
        guard let token, !token.isEmpty else { return nil }
        guard token.hasPrefix("sdui:") else { return token }

        if let resolved = tokens[token] { return resolved }
        logger.warning("unknown icon token \(token, privacy: .public); falling back to sdui:warning")
        return tokens["sdui:warning"]
    }
}

/// Snapshot of `schema/icon-tokens.json`. Kept inline so the library
/// carries no external resource dependency; regenerate this map from
/// the schema whenever the canonical icon list changes.
private let defaultTokens: [String: String] = [
    "sdui:play":       "play.fill",
    "sdui:pause":      "pause.fill",
    "sdui:back":       "chevron.left",
    "sdui:forward":    "chevron.right",
    "sdui:settings":   "gearshape",
    "sdui:expand":     "chevron.down",
    "sdui:collapse":   "chevron.up",
    "sdui:check":      "checkmark",
    "sdui:warning":    "exclamationmark.triangle",
    "sdui:live":       "antenna.radiowaves.left.and.right",
    "sdui:person":     "person.circle",
    "sdui:close":      "xmark",
    "sdui:search":     "magnifyingglass",
    "sdui:share":      "square.and.arrow.up",
    "sdui:favorite":   "heart",
    "sdui:favorited":  "heart.fill",
    "sdui:fullscreen": "arrow.up.left.and.arrow.down.right",
    "sdui:pip":        "pip",
    "sdui:cast":       "airplayvideo",
    "sdui:info":       "info.circle",
    "sdui:calendar":   "calendar",
    "sdui:refresh":    "arrow.clockwise",
    "sdui:home":        "house",
    "sdui:basketball":  "basketball.fill",
    "sdui:video":       "play.rectangle",
    "sdui:leaderboard": "list.number",
    "sdui:grid":        "square.grid.2x2"
]
