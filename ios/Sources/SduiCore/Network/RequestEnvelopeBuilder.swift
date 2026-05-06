import Foundation
#if canImport(UIKit)
import UIKit
#endif

/// Builds the SDUI request envelope as bracket-notation query parameters
/// (ADR-003 / plan-request-transport §D1, D3).
///
/// All composition context travels as query params. When the resulting query
/// exceeds ``RequestEnvelope/maxQueryLength`` (8192), the caller should
/// switch to a POST request whose JSON body has the same shape.
///
/// Mirror of Android's
/// [`RequestEnvelopeBuilder`](../../android/sdui-core/src/main/java/com/nba/sdui/core/request/RequestEnvelopeBuilder.kt).
///
/// Example query output:
/// ```text
/// locale=en&schemaVersion=1.0&platform[deviceClass]=phone
/// &platform[capabilities][sse]=true&market[cohort]=US_NY_METRO
/// &experiments[gd_tab_order_v2]=variant_b
/// ```
public struct RequestEnvelope: Sendable, Equatable {

    /// Safe GET threshold; caller falls back to POST above this length.
    public static let maxQueryLength = 8192

    // MARK: Top-level scalars

    public var locale: String
    public var schemaVersion: String

    // MARK: Platform

    public var platformName: String
    public var appVersion: String?
    public var osVersion: String
    public var deviceClass: String
    // TODO(platform-tier): replace per-boolean capability flags with a single
    // server-defined platform tier string (e.g. "tier:full") to reduce CDN
    // cache-key fragmentation. Tier resolution at edge or in client.
    public var sseCapable: Bool
    public var onFocusCapable: Bool

    // MARK: Device

    public var deviceID: String?

    // MARK: Market

    public var marketCohort: String

    // MARK: Experiments

    public var experiments: [String: String]

    public init(
        locale: String = "en",
        schemaVersion: String = "1.0",
        platformName: String = "ios",
        appVersion: String? = nil,
        osVersion: String = Self.currentOSVersion,
        deviceClass: String = Self.currentDeviceClass,
        sseCapable: Bool = true,
        onFocusCapable: Bool = false,
        deviceID: String? = nil,
        marketCohort: String = "MARKET_UNKNOWN",
        experiments: [String: String] = [:]
    ) {
        self.locale = locale
        self.schemaVersion = schemaVersion
        self.platformName = platformName
        self.appVersion = appVersion
        self.osVersion = osVersion
        self.deviceClass = deviceClass
        self.sseCapable = sseCapable
        self.onFocusCapable = onFocusCapable
        self.deviceID = deviceID
        self.marketCohort = marketCohort
        self.experiments = experiments
    }

    // MARK: Derived

    /// Build the bracket-notation query string. Does NOT include a leading `?`.
    ///
    /// Fixed envelope ordering: locale, schemaVersion, platform, market, experiments.
    /// All platforms must emit in this order for byte-identical CDN cache keys.
    public func buildQueryString() -> String {
        var params: [(String, String)] = []

        params.append(("locale", locale))
        params.append(("schemaVersion", schemaVersion))

        params.append(("platform[deviceClass]", deviceClass))
        params.append(("platform[capabilities][sse]", String(sseCapable)))
        if onFocusCapable {
            params.append(("platform[capabilities][onFocus]", "true"))
        }

        // Market
        params.append(("market[cohort]", marketCohort))

        // deviceID travels as X-Device-Id header, not in the envelope query.

        for (id, variant) in experiments.sorted(by: { $0.key < $1.key }) {
            params.append(("experiments[\(id)]", variant))
        }

        return params
            .map { "\(Self.percentEncode($0.0))=\(Self.percentEncode($0.1))" }
            .joined(separator: "&")
    }

    /// Whether the query exceeds the GET threshold; caller should POST instead.
    public var exceedsGetThreshold: Bool {
        buildQueryString().count > Self.maxQueryLength
    }

    /// Encode the envelope as the JSON body used for POST fallback.
    /// Shape matches the server's ``SduiRequestContext`` field layout.
    public func jsonBody() throws -> Data {
        var platform: [String: Any] = [
            "deviceClass": deviceClass
        ]
        var capabilities: [String: Any] = ["sse": sseCapable]
        if onFocusCapable { capabilities["onFocus"] = true }
        platform["capabilities"] = capabilities

        // deviceID travels as X-Device-Id header, not in the envelope body.

        let body: [String: Any] = [
            "locale": locale,
            "schemaVersion": schemaVersion,
            "platform": platform,
            "market": ["cohort": marketCohort],
            "experiments": experiments
        ]

        return try JSONSerialization.data(
            withJSONObject: body,
            options: [.sortedKeys]
        )
    }

    // MARK: Defaults

    /// Full OS version, e.g. `17.5` or `17.5.1`. Reads from `ProcessInfo`.
    public static var currentOSVersion: String {
        let v = ProcessInfo.processInfo.operatingSystemVersion
        return v.patchVersion == 0
            ? "\(v.majorVersion).\(v.minorVersion)"
            : "\(v.majorVersion).\(v.minorVersion).\(v.patchVersion)"
    }

    /// Best-effort device class. Uses `UIDevice.userInterfaceIdiom` when
    /// available; falls back to `"phone"`.
    public static var currentDeviceClass: String {
        #if canImport(UIKit) && !os(macOS)
        switch UIDevice.current.userInterfaceIdiom {
        case .pad: return "tablet"
        case .tv: return "tv"
        case .phone: return "phone"
        default: return "phone"
        }
        #else
        return "phone"
        #endif
    }

    /// Layout / token axis for this process (best-effort; matches `deviceClass` and orientation).
    public static var currentFormFactor: String {
        #if canImport(UIKit) && !os(macOS)
        switch UIDevice.current.userInterfaceIdiom {
        case .pad: return "tablet"
        case .tv: return "tv"
        case .phone:
            let s = UIScreen.main.bounds.size
            return s.width > s.height ? "phone.landscape" : "phone"
        default: return "phone"
        }
        #else
        return "phone"
        #endif
    }

    /// Current color scheme from OS. Returns "dark" or "light".
    public static var currentTheme: String {
        #if canImport(UIKit) && !os(macOS)
        if UITraitCollection.current.userInterfaceStyle == .dark {
            return "dark"
        }
        return "light"
        #else
        return "light"
        #endif
    }

    // MARK: Percent encoding

    /// Percent-encodes per RFC 3986 unreserved set, matching
    /// `URLEncoder.encode(..., "UTF-8")` on Android for byte parity.
    static func percentEncode(_ value: String) -> String {
        // Allow only the RFC 3986 unreserved character set. Bracket chars,
        // spaces, `=`, `&`, etc. are all encoded so the server sees them as
        // literal query-param key/value bytes.
        let unreserved = CharacterSet(charactersIn:
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
        )
        return value.addingPercentEncoding(withAllowedCharacters: unreserved) ?? value
    }
}
