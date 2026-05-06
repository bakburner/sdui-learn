// This file was generated from JSON Schema using quicktype, do not modify it directly.
// To parse the JSON, add this file to your project and do:
//
//   let sduiModels = try SduiModels(json)

import Foundation

/// Server-Driven UI schema for NBA Game Detail screens
// MARK: - SduiModels
struct SduiModels: Codable {
    /// Screen-level actions (e.g. analytics beacons, lifecycle hooks)
    let actions: [Action]?
    let analyticsID: String?
    let defaultRefreshPolicy: RefreshPolicy?
    let id: String
    let navigation: Navigation?
    /// Named overlay sections the client shows when a trigger condition arises. Keys are
    /// developer-defined state names (e.g. 'couchRightsWarning'). Values are server-composed
    /// sections (typically AtomicComposite). Client controls trigger timing and presentation
    /// style; server controls display content.
    let overlays: [String: Section]?
    /// URI the back button should navigate to.  Clients always show a back button; this field
    /// tells them the target.  Omit for root screens (e.g. scoreboard).
    let parentURI: String?
    let schemaVersion: String
    let sections: [Section]
    let state: [String: JSONAny]?
    let title, traceID: String?
    /// Server-exposed A/B / experiment variants available for this screen. Clients read
    /// `options` to render a variant picker (dev UI, QA tooling) and pass the selected id back
    /// to the server on subsequent requests. Omit for screens without active experiments.
    let variants: ExperimentVariants?

    enum CodingKeys: String, CodingKey {
        case actions
        case analyticsID = "analyticsId"
        case defaultRefreshPolicy, id, navigation, overlays
        case parentURI = "parentUri"
        case schemaVersion, sections, state, title
        case traceID = "traceId"
        case variants
    }
}

// MARK: SduiModels convenience initializers and mutators

extension SduiModels {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(SduiModels.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        actions: [Action]?? = nil,
        analyticsID: String?? = nil,
        defaultRefreshPolicy: RefreshPolicy?? = nil,
        id: String? = nil,
        navigation: Navigation?? = nil,
        overlays: [String: Section]?? = nil,
        parentURI: String?? = nil,
        schemaVersion: String? = nil,
        sections: [Section]? = nil,
        state: [String: JSONAny]?? = nil,
        title: String?? = nil,
        traceID: String?? = nil,
        variants: ExperimentVariants?? = nil
    ) -> SduiModels {
        return SduiModels(
            actions: actions ?? self.actions,
            analyticsID: analyticsID ?? self.analyticsID,
            defaultRefreshPolicy: defaultRefreshPolicy ?? self.defaultRefreshPolicy,
            id: id ?? self.id,
            navigation: navigation ?? self.navigation,
            overlays: overlays ?? self.overlays,
            parentURI: parentURI ?? self.parentURI,
            schemaVersion: schemaVersion ?? self.schemaVersion,
            sections: sections ?? self.sections,
            state: state ?? self.state,
            title: title ?? self.title,
            traceID: traceID ?? self.traceID,
            variants: variants ?? self.variants
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Action fired when the form is submitted
///
/// Top-level fallback action invoked when the IAP SDK is not mounted (today, always).
///
/// Optional action to trigger on retry tap (typically a refresh action)
// MARK: - Action
struct Action: Codable {
    /// For fireAndForget actions: where to send the beacon
    let destinations: [Destination]?
    /// For refresh actions: target URL (defaults to current screen endpoint if omitted)
    let endpoint: String?
    /// For fireAndForget actions: event name
    let event: String?
    /// Optional server-provided error message and presentation style. Client falls back to
    /// generic localized string when absent
    let failureFeedback: FailureFeedback?
    /// For fireAndForget actions with onVisible trigger: impression tracking policy
    let impression: ImpressionPolicy?
    /// For toast actions: text message to display in the toast
    let message: String?
    /// For navigate actions with modal presentation: sheet height
    let modalHeight: ModalHeight?
    /// Sequence behavior when this action fails. Client applies per-type default when absent
    /// (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue)
    let onFailure: FailurePolicy?
    /// For mutate actions: operation to perform on the state key
    let operation: MutateOperation?
    /// For refresh actions: map of query param name to screen state key, resolved at action time
    let paramBindings: [String: String]?
    /// For fireAndForget actions: event parameters
    let params: [String: JSONAny]?
    /// For navigate actions: how the destination is presented
    let presentation: NavigationPresentation?
    /// For mutate actions: state key to update. For dismiss actions: what to dismiss
    /// (modal/overlay/screen). For refresh actions: section ID to refresh (omit for full screen)
    let target: String?
    /// For navigate actions: native deeplink URI
    let targetURI: String?
    let trigger: ActionTrigger
    let type: ActionType
    /// For mutate actions: the value to apply with the operation
    let value: JSONAny?
    /// For navigate actions: web-equivalent URL (first-class, not a fallback)
    let webURL: String?

    enum CodingKeys: String, CodingKey {
        case destinations, endpoint, event, failureFeedback, impression, message, modalHeight, onFailure, operation, paramBindings, params, presentation, target
        case targetURI = "targetUri"
        case trigger, type, value
        case webURL = "webUrl"
    }
}

// MARK: Action convenience initializers and mutators

extension Action {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Action.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        destinations: [Destination]?? = nil,
        endpoint: String?? = nil,
        event: String?? = nil,
        failureFeedback: FailureFeedback?? = nil,
        impression: ImpressionPolicy?? = nil,
        message: String?? = nil,
        modalHeight: ModalHeight?? = nil,
        onFailure: FailurePolicy?? = nil,
        operation: MutateOperation?? = nil,
        paramBindings: [String: String]?? = nil,
        params: [String: JSONAny]?? = nil,
        presentation: NavigationPresentation?? = nil,
        target: String?? = nil,
        targetURI: String?? = nil,
        trigger: ActionTrigger? = nil,
        type: ActionType? = nil,
        value: JSONAny?? = nil,
        webURL: String?? = nil
    ) -> Action {
        return Action(
            destinations: destinations ?? self.destinations,
            endpoint: endpoint ?? self.endpoint,
            event: event ?? self.event,
            failureFeedback: failureFeedback ?? self.failureFeedback,
            impression: impression ?? self.impression,
            message: message ?? self.message,
            modalHeight: modalHeight ?? self.modalHeight,
            onFailure: onFailure ?? self.onFailure,
            operation: operation ?? self.operation,
            paramBindings: paramBindings ?? self.paramBindings,
            params: params ?? self.params,
            presentation: presentation ?? self.presentation,
            target: target ?? self.target,
            targetURI: targetURI ?? self.targetURI,
            trigger: trigger ?? self.trigger,
            type: type ?? self.type,
            value: value ?? self.value,
            webURL: webURL ?? self.webURL
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum Destination: String, Codable {
    case adobe = "adobe"
    case all = "all"
    case destinationInternal = "internal"
    case firebase = "firebase"
}

/// Optional server-provided error message and presentation style. Client falls back to
/// generic localized string when absent
///
/// Optional server-provided error message and presentation style for action failures. Client
/// falls back to generic localized string when absent.
// MARK: - FailureFeedback
struct FailureFeedback: Codable {
    /// Localized error message to display on failure
    let message: String?
    /// Presentation hint — clients map to closest platform-native mechanism
    let style: FailureFeedbackStyle?
}

// MARK: FailureFeedback convenience initializers and mutators

extension FailureFeedback {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(FailureFeedback.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        message: String?? = nil,
        style: FailureFeedbackStyle?? = nil
    ) -> FailureFeedback {
        return FailureFeedback(
            message: message ?? self.message,
            style: style ?? self.style
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Presentation hint — clients map to closest platform-native mechanism
///
/// Presentation hint for failure feedback. Clients map to closest platform-native mechanism.
enum FailureFeedbackStyle: String, Codable {
    case inline = "inline"
    case snackbar = "snackbar"
    case toast = "toast"
}

/// For fireAndForget actions with onVisible trigger: impression tracking policy
///
/// Impression tracking policy for analytics actions with onVisible trigger
// MARK: - ImpressionPolicy
struct ImpressionPolicy: Codable {
    let dedup: ImpressionDedup?
    /// Reset interval for once-per-interval strategy (milliseconds)
    let intervalMS: Int?
    let threshold: ImpressionThreshold?

    enum CodingKeys: String, CodingKey {
        case dedup
        case intervalMS = "intervalMs"
        case threshold
    }
}

// MARK: ImpressionPolicy convenience initializers and mutators

extension ImpressionPolicy {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(ImpressionPolicy.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        dedup: ImpressionDedup?? = nil,
        intervalMS: Int?? = nil,
        threshold: ImpressionThreshold?? = nil
    ) -> ImpressionPolicy {
        return ImpressionPolicy(
            dedup: dedup ?? self.dedup,
            intervalMS: intervalMS ?? self.intervalMS,
            threshold: threshold ?? self.threshold
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum ImpressionDedup: String, Codable {
    case none = "none"
    case oncePerInterval = "once-per-interval"
    case oncePerScreen = "once-per-screen"
    case oncePerSession = "once-per-session"
}

// MARK: - ImpressionThreshold
struct ImpressionThreshold: Codable {
    /// Milliseconds section must remain visible before impression fires
    let dwellMS: Int?
    /// Fraction of section area that must be visible (0.5 = 50%)
    let visibility: Double?

    enum CodingKeys: String, CodingKey {
        case dwellMS = "dwellMs"
        case visibility
    }
}

// MARK: ImpressionThreshold convenience initializers and mutators

extension ImpressionThreshold {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(ImpressionThreshold.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        dwellMS: Int?? = nil,
        visibility: Double?? = nil
    ) -> ImpressionThreshold {
        return ImpressionThreshold(
            dwellMS: dwellMS ?? self.dwellMS,
            visibility: visibility ?? self.visibility
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// For navigate actions with modal presentation: sheet height
enum ModalHeight: String, Codable {
    case compact = "compact"
    case full = "full"
    case half = "half"
}

/// Sequence behavior when this action fails. Client applies per-type default when absent
/// (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue)
///
/// Sequence behavior when an action fails. Clients apply per-type defaults when absent:
/// navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue.
enum FailurePolicy: String, Codable {
    case failurePolicyContinue = "continue"
    case halt = "halt"
    case silent = "silent"
}

/// For mutate actions: operation to perform on the state key
enum MutateOperation: String, Codable {
    case append = "append"
    case increment = "increment"
    case mutateOperationSet = "set"
    case toggle = "toggle"
}

/// For navigate actions: how the destination is presented
enum NavigationPresentation: String, Codable {
    case external = "external"
    case fullscreen = "fullscreen"
    case modal = "modal"
    case push = "push"
    case replace = "replace"
}

/// Event that fires the action. Prefer onActivate for primary activation (tap, keyboard
/// Enter/Space, accessibility activate). onTap is a deprecated alias for onActivate.
enum ActionTrigger: String, Codable {
    case onActivate = "onActivate"
    case onBlur = "onBlur"
    case onFocus = "onFocus"
    case onLongPress = "onLongPress"
    case onSubmit = "onSubmit"
    case onSwipe = "onSwipe"
    case onTap = "onTap"
    case onVisible = "onVisible"
}

enum ActionType: String, Codable {
    case dismiss = "dismiss"
    case fireAndForget = "fireAndForget"
    case mutate = "mutate"
    case navigate = "navigate"
    case refresh = "refresh"
    case toast = "toast"
}

// MARK: - RefreshPolicy
struct RefreshPolicy: Codable {
    /// For sse type: Ably channel name pattern (e.g., '{gameId}:linescore')
    let channel: String?
    /// JSONPath to extract section data from response (e.g., '$.game' or '$.sections[0].data')
    let dataPath: String?
    /// For poll type: interval in milliseconds
    let intervalMS: Int?
    /// Whether the client should pause this section's refresh when it scrolls out of the
    /// viewport. Default true. Set false for critical live sections (e.g., live-score panels)
    /// that should refresh continuously.
    let pauseWhenOffScreen: Bool?
    let type: RefreshType
    /// For poll/sse type: URL to poll or connect to. If omitted, polls the SDUI endpoint.
    let url: String?

    enum CodingKeys: String, CodingKey {
        case channel, dataPath
        case intervalMS = "intervalMs"
        case pauseWhenOffScreen, type, url
    }
}

// MARK: RefreshPolicy convenience initializers and mutators

extension RefreshPolicy {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(RefreshPolicy.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        channel: String?? = nil,
        dataPath: String?? = nil,
        intervalMS: Int?? = nil,
        pauseWhenOffScreen: Bool?? = nil,
        type: RefreshType? = nil,
        url: String?? = nil
    ) -> RefreshPolicy {
        return RefreshPolicy(
            channel: channel ?? self.channel,
            dataPath: dataPath ?? self.dataPath,
            intervalMS: intervalMS ?? self.intervalMS,
            pauseWhenOffScreen: pauseWhenOffScreen ?? self.pauseWhenOffScreen,
            type: type ?? self.type,
            url: url ?? self.url
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum RefreshType: String, Codable {
    case poll = "poll"
    case refreshTypeStatic = "static"
    case sse = "sse"
}

// MARK: - Navigation
struct Navigation: Codable {
    let items: [NavigationItem]?
}

// MARK: Navigation convenience initializers and mutators

extension Navigation {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Navigation.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        items: [NavigationItem]?? = nil
    ) -> Navigation {
        return Navigation(
            items: items ?? self.items
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - NavigationItem
struct NavigationItem: Codable {
    let children: [NavigationItem]?
    let icon: String?
    let id, label: String
    let selected: Bool?
    let targetURI: String?

    enum CodingKeys: String, CodingKey {
        case children, icon, id, label, selected
        case targetURI = "targetUri"
    }
}

// MARK: NavigationItem convenience initializers and mutators

extension NavigationItem {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(NavigationItem.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        children: [NavigationItem]?? = nil,
        icon: String?? = nil,
        id: String? = nil,
        label: String? = nil,
        selected: Bool?? = nil,
        targetURI: String?? = nil
    ) -> NavigationItem {
        return NavigationItem(
            children: children ?? self.children,
            icon: icon ?? self.icon,
            id: id ?? self.id,
            label: label ?? self.label,
            selected: selected ?? self.selected,
            targetURI: targetURI ?? self.targetURI
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// One server-composed overlay layer positioned over an OverlayContainer base element.
// MARK: - AtomicOverlay
struct AtomicOverlay: Codable {
    /// Position of the overlay within the base element bounds.
    let alignment: BadgeAlignment?
    /// Atomic element to render in this overlay layer.
    let element: AtomicElement
    /// Optional edge offsets from the aligned base bounds.
    let inset: Spacing?
}

// MARK: AtomicOverlay convenience initializers and mutators

extension AtomicOverlay {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(AtomicOverlay.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        alignment: BadgeAlignment?? = nil,
        element: AtomicElement? = nil,
        inset: Spacing?? = nil
    ) -> AtomicOverlay {
        return AtomicOverlay(
            alignment: alignment ?? self.alignment,
            element: element ?? self.element,
            inset: inset ?? self.inset
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
///
/// Z-positioned child element overlaid on a parent (e.g. 'LIVE' pill at bottom-right of a
/// thumbnail, duration label). Named Badge (not Overlay) to avoid collision with the
/// screen-level overlays map.
// MARK: - Badge
struct Badge: Codable {
    /// Position of the badge within the parent bounds
    let alignment: BadgeAlignment?
    /// The element to render as a badge
    let element: AtomicElement
}

// MARK: Badge convenience initializers and mutators

extension Badge {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Badge.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        alignment: BadgeAlignment?? = nil,
        element: AtomicElement? = nil
    ) -> Badge {
        return Badge(
            alignment: alignment ?? self.alignment,
            element: element ?? self.element
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Atomic tree describing the banner's full visible surface. Renderer walks this tree
/// exactly as an AtomicComposite would; no client-side chrome defaults are permitted. See
/// AGENTS.md §15.1.
///
/// Atomic UI primitive — server-composed building block for the atomic rendering layer
///
/// The element to render as a badge
///
/// OverlayContainer base element. Rendered first and sized by its own atomic box model.
///
/// Atomic element to render in this overlay layer.
///
/// Atomic tree describing the hero's full visible surface — logo, title, subtitle, feature
/// list, tier cards, CTAs. Renderer walks this tree exactly as an AtomicComposite would.
///
/// Root node of the atomic element tree — the rendering instructions
///
/// Atomic tree describing the pre-SDK placeholder surface (e.g. play-glyph column framed at
/// the player's aspectRatio). Renderer walks this tree exactly as an AtomicComposite would;
/// no client-side chrome defaults are permitted. Once the video SDK lands this tree becomes
/// the loading/error placeholder the SDK overlays.
// MARK: - AtomicElement
class AtomicElement: Codable {
    /// Server-provided accessibility metadata for this atomic element
    let accessibility: AccessibilityProperties?
    let actions: [Action]?
    let alignment: Alignment?
    /// Per-child cross-axis alignment override. When set, wins over parent crossAlignment for
    /// this child (matches Figma and CSS align-self semantics).
    let alignSelf: CrossAlignment?
    /// Deprecated: use accessibility.label instead. Retained for backward compatibility; clients
    /// prefer accessibility.label when present.
    let alt: String?
    /// Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
    let aspectRatio: AspectRatioUnion?
    /// DEPRECATED — use backgrounds (array) for new payloads. Single background. If both
    /// background and backgrounds are present, backgrounds wins.
    let background: BackgroundUnion?
    /// Ordered array of background layers. Index 0 is the bottommost layer (Figma convention);
    /// higher indices paint on top. Web renderers must reverse the array when mapping to CSS
    /// background shorthand (CSS is top-to-bottom). When absent, falls back to singular
    /// background field.
    let backgrounds: [BackgroundUnion]?
    /// Z-positioned child element (e.g. 'LIVE' pill, duration label) overlaid on this element.
    let badge: Badge?
    /// OverlayContainer base element. Rendered first and sized by its own atomic box model.
    let base: AtomicElement?
    /// Dot-path into the enclosing AtomicComposite's `data.content` object. When set, renderers
    /// resolve the leaf's canonical live field from `data.content[bindRef]` at render time and
    /// fall back to the inline value when the path is absent. Canonical field per type: Text →
    /// content, Button → label, Image → src, LiveClock → an object with {snapshotSeconds,
    /// snapshotAt, isRunning}. Placing the binding identifier on the consuming node (rather than
    /// in a centrally-declared path-into-tree) lets composers reshape the ui tree without
    /// breaking real-time updates; data-bindings on the section envelope continue to write into
    /// `content.*`.
    let bindRef: String?
    /// Responsive breakpoint in dp/px. For Container: below this screen width, direction flips
    /// from row to column. Enables responsive layouts without client logic.
    let breakpoint: Int?
    let children: [AtomicElement]?
    let color: String?
    /// DisplayGrid column definitions — display-only, non-interactive, server-ordered
    let columns: [Column]?
    let condition, content: String?
    /// Per-corner cornerRadius override. When present, takes precedence over the single-value
    /// cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also
    /// absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and
    /// square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to
    /// UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner
    /// constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius /
    /// borderBottomRightRadius.
    let cornerRadii: CornerRadii?
    /// Corner radius: dp/px or layout token. Applied to Container (with overflow clip) and Image
    /// elements.
    let cornerRadius: LayoutScalar?
    let crossAlignment: CrossAlignment?
    /// Gap between wrapped lines when layoutWrap is true. Falls back to gap when absent. Ignored
    /// when layoutWrap is false.
    let crossAxisGap: LayoutScalar?
    let direction: UIDirection?
    let disabled: Bool?
    let falseChild: AtomicElement?
    let fit: ImageFit?
    /// Flex grow factor. When set on a child of a Container, the child claims proportional space
    /// along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
    let flex: Double?
    /// LiveClock display format. Clients realize using their platform's tabular-numerals
    /// typography (equivalent to TextVariant.score).
    let format: Format?
    /// Gap between flex children (row/column), or grid gap where applicable.
    let gap: LayoutScalar?
    /// Fixed height in dp/px or layout token.
    let height: LayoutScalar?
    /// Sizing behavior along the height axis. 'hug' = intrinsic, 'fill' = stretch to parent,
    /// 'fixed' = use explicit height value.
    let heightMode: SizingMode?
    let icon, id: String?
    /// LiveClock: whether the clock is actively ticking. When true, clients run a local tick
    /// loop at their platform-native refresh cadence (~10Hz) and update the displayed value.
    /// When false, clients render snapshotSeconds verbatim.
    let isRunning: Bool?
    let label: String?
    /// When true, enables flex-wrap on a Container. Children that overflow the main axis wrap to
    /// the next line. Only meaningful on Container elements.
    let layoutWrap: Bool?
    /// Outer space between the element and its siblings or parent edges. Applied outside the
    /// element's background, border, corner radius, and shadow — use this for sibling-to-sibling
    /// spacing instead of Spacer siblings when inhomogeneous gaps are needed.
    let margin: Spacing?
    /// Maximum height constraint in dp/px or layout token.
    let maxHeight: LayoutScalar?
    let maxLines: Int?
    /// Maximum width constraint in dp/px or layout token.
    let maxWidth: LayoutScalar?
    /// Minimum height constraint in dp/px or layout token.
    let minHeight: LayoutScalar?
    /// Minimum width constraint in dp/px or layout token.
    let minWidth: LayoutScalar?
    /// Use tabular/monospaced digit rendering to prevent layout shift on numeric text changes
    /// (scores, clocks).
    let monospacedDigits: Bool?
    /// Element opacity (0=transparent, 1=opaque). Enables duration badge overlays and faded
    /// states.
    let opacity: Double?
    let orientation: Orientation?
    /// OverlayContainer layers rendered over the base element in server order.
    let overlays: [AtomicOverlay]?
    /// Inner space between the element's own background/border and its content.
    let padding: Spacing?
    /// Optional ScrollContainer page indicator. Clients render it only when declared.
    let pageIndicator: PageIndicator?
    let paging: Bool?
    let placeholder: String?
    /// DisplayGrid row data — each object maps column keys to pre-formatted display values
    let rows: [[String: String]]?
    /// Full section object to render via SectionRouter. Only used when type is SectionSlot.
    let section: Section?
    /// DEPRECATED — use shadows (array) for new payloads. Single shadow. If both shadow and
    /// shadows are present, shadows wins.
    let shadow: Shadow?
    /// Ordered array of shadow layers. Index 0 is the outermost shadow (Figma convention);
    /// higher indices are closer to the element. Maps directly to CSS box-shadow list order.
    /// When absent, falls back to singular shadow field.
    let shadows: [Shadow]?
    /// Whether to show scroll indicators on ScrollContainer. Default false for clean carousel
    /// presentation.
    let showIndicators: Bool?
    let size: Int?
    let snapAlignment: Align?
    /// LiveClock: wall-clock instant (ISO-8601) at which snapshotSeconds was captured. Clients
    /// compute elapsed = now - snapshotAt and derive the displayed value. Required when type ==
    /// 'LiveClock'.
    let snapshotAt: Date?
    /// LiveClock: clock value in seconds at the moment captured by snapshotAt. Clients
    /// interpolate from this anchor while isRunning == true. Required when type == 'LiveClock'.
    let snapshotSeconds: Int?
    let src: String?
    /// LiveClock: optional clamp. For direction 'down', clock holds at this value once reached.
    /// For direction 'up', clock holds once reached. Omit to disable the clamp.
    let stopAtSeconds: Int?
    /// Alternate row background for readability
    let striped: Bool?
    /// Text alignment within the element. Used for centered headings, right-aligned numeric
    /// values.
    let textAlign: Align?
    let thickness: Int?
    /// LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds
    /// (default 0); 'up' increments from snapshotSeconds with no upper bound unless
    /// stopAtSeconds is set.
    let tickDirection: TickDirection?
    let trueChild: AtomicElement?
    let type: String
    /// Named variant preset. The vocabulary depends on the element's type: TextVariant for Text,
    /// ButtonVariant for Button, ContainerVariant for Container, ImageVariant for Image.
    /// Renderers parse this string against the primitive's enum and log a diagnostic on
    /// unrecognized values.
    let variant: String?
    let weight: TextWeight?
    /// Fixed width in dp/px or layout token.
    let width: LayoutScalar?
    /// Sizing behavior along the width axis. 'hug' = intrinsic, 'fill' = stretch to parent,
    /// 'fixed' = use explicit width value.
    let widthMode: SizingMode?

    init(accessibility: AccessibilityProperties?, actions: [Action]?, alignment: Alignment?, alignSelf: CrossAlignment?, alt: String?, aspectRatio: AspectRatioUnion?, background: BackgroundUnion?, backgrounds: [BackgroundUnion]?, badge: Badge?, base: AtomicElement?, bindRef: String?, breakpoint: Int?, children: [AtomicElement]?, color: String?, columns: [Column]?, condition: String?, content: String?, cornerRadii: CornerRadii?, cornerRadius: LayoutScalar?, crossAlignment: CrossAlignment?, crossAxisGap: LayoutScalar?, direction: UIDirection?, disabled: Bool?, falseChild: AtomicElement?, fit: ImageFit?, flex: Double?, format: Format?, gap: LayoutScalar?, height: LayoutScalar?, heightMode: SizingMode?, icon: String?, id: String?, isRunning: Bool?, label: String?, layoutWrap: Bool?, margin: Spacing?, maxHeight: LayoutScalar?, maxLines: Int?, maxWidth: LayoutScalar?, minHeight: LayoutScalar?, minWidth: LayoutScalar?, monospacedDigits: Bool?, opacity: Double?, orientation: Orientation?, overlays: [AtomicOverlay]?, padding: Spacing?, pageIndicator: PageIndicator?, paging: Bool?, placeholder: String?, rows: [[String: String]]?, section: Section?, shadow: Shadow?, shadows: [Shadow]?, showIndicators: Bool?, size: Int?, snapAlignment: Align?, snapshotAt: Date?, snapshotSeconds: Int?, src: String?, stopAtSeconds: Int?, striped: Bool?, textAlign: Align?, thickness: Int?, tickDirection: TickDirection?, trueChild: AtomicElement?, type: String, variant: String?, weight: TextWeight?, width: LayoutScalar?, widthMode: SizingMode?) {
        self.accessibility = accessibility
        self.actions = actions
        self.alignment = alignment
        self.alignSelf = alignSelf
        self.alt = alt
        self.aspectRatio = aspectRatio
        self.background = background
        self.backgrounds = backgrounds
        self.badge = badge
        self.base = base
        self.bindRef = bindRef
        self.breakpoint = breakpoint
        self.children = children
        self.color = color
        self.columns = columns
        self.condition = condition
        self.content = content
        self.cornerRadii = cornerRadii
        self.cornerRadius = cornerRadius
        self.crossAlignment = crossAlignment
        self.crossAxisGap = crossAxisGap
        self.direction = direction
        self.disabled = disabled
        self.falseChild = falseChild
        self.fit = fit
        self.flex = flex
        self.format = format
        self.gap = gap
        self.height = height
        self.heightMode = heightMode
        self.icon = icon
        self.id = id
        self.isRunning = isRunning
        self.label = label
        self.layoutWrap = layoutWrap
        self.margin = margin
        self.maxHeight = maxHeight
        self.maxLines = maxLines
        self.maxWidth = maxWidth
        self.minHeight = minHeight
        self.minWidth = minWidth
        self.monospacedDigits = monospacedDigits
        self.opacity = opacity
        self.orientation = orientation
        self.overlays = overlays
        self.padding = padding
        self.pageIndicator = pageIndicator
        self.paging = paging
        self.placeholder = placeholder
        self.rows = rows
        self.section = section
        self.shadow = shadow
        self.shadows = shadows
        self.showIndicators = showIndicators
        self.size = size
        self.snapAlignment = snapAlignment
        self.snapshotAt = snapshotAt
        self.snapshotSeconds = snapshotSeconds
        self.src = src
        self.stopAtSeconds = stopAtSeconds
        self.striped = striped
        self.textAlign = textAlign
        self.thickness = thickness
        self.tickDirection = tickDirection
        self.trueChild = trueChild
        self.type = type
        self.variant = variant
        self.weight = weight
        self.width = width
        self.widthMode = widthMode
    }
}

// MARK: AtomicElement convenience initializers and mutators

extension AtomicElement {
    convenience init(data: Data) throws {
        let me = try newJSONDecoder().decode(AtomicElement.self, from: data)
        self.init(accessibility: me.accessibility, actions: me.actions, alignment: me.alignment, alignSelf: me.alignSelf, alt: me.alt, aspectRatio: me.aspectRatio, background: me.background, backgrounds: me.backgrounds, badge: me.badge, base: me.base, bindRef: me.bindRef, breakpoint: me.breakpoint, children: me.children, color: me.color, columns: me.columns, condition: me.condition, content: me.content, cornerRadii: me.cornerRadii, cornerRadius: me.cornerRadius, crossAlignment: me.crossAlignment, crossAxisGap: me.crossAxisGap, direction: me.direction, disabled: me.disabled, falseChild: me.falseChild, fit: me.fit, flex: me.flex, format: me.format, gap: me.gap, height: me.height, heightMode: me.heightMode, icon: me.icon, id: me.id, isRunning: me.isRunning, label: me.label, layoutWrap: me.layoutWrap, margin: me.margin, maxHeight: me.maxHeight, maxLines: me.maxLines, maxWidth: me.maxWidth, minHeight: me.minHeight, minWidth: me.minWidth, monospacedDigits: me.monospacedDigits, opacity: me.opacity, orientation: me.orientation, overlays: me.overlays, padding: me.padding, pageIndicator: me.pageIndicator, paging: me.paging, placeholder: me.placeholder, rows: me.rows, section: me.section, shadow: me.shadow, shadows: me.shadows, showIndicators: me.showIndicators, size: me.size, snapAlignment: me.snapAlignment, snapshotAt: me.snapshotAt, snapshotSeconds: me.snapshotSeconds, src: me.src, stopAtSeconds: me.stopAtSeconds, striped: me.striped, textAlign: me.textAlign, thickness: me.thickness, tickDirection: me.tickDirection, trueChild: me.trueChild, type: me.type, variant: me.variant, weight: me.weight, width: me.width, widthMode: me.widthMode)
    }

    convenience init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    convenience init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        accessibility: AccessibilityProperties?? = nil,
        actions: [Action]?? = nil,
        alignment: Alignment?? = nil,
        alignSelf: CrossAlignment?? = nil,
        alt: String?? = nil,
        aspectRatio: AspectRatioUnion?? = nil,
        background: BackgroundUnion?? = nil,
        backgrounds: [BackgroundUnion]?? = nil,
        badge: Badge?? = nil,
        base: AtomicElement?? = nil,
        bindRef: String?? = nil,
        breakpoint: Int?? = nil,
        children: [AtomicElement]?? = nil,
        color: String?? = nil,
        columns: [Column]?? = nil,
        condition: String?? = nil,
        content: String?? = nil,
        cornerRadii: CornerRadii?? = nil,
        cornerRadius: LayoutScalar?? = nil,
        crossAlignment: CrossAlignment?? = nil,
        crossAxisGap: LayoutScalar?? = nil,
        direction: UIDirection?? = nil,
        disabled: Bool?? = nil,
        falseChild: AtomicElement?? = nil,
        fit: ImageFit?? = nil,
        flex: Double?? = nil,
        format: Format?? = nil,
        gap: LayoutScalar?? = nil,
        height: LayoutScalar?? = nil,
        heightMode: SizingMode?? = nil,
        icon: String?? = nil,
        id: String?? = nil,
        isRunning: Bool?? = nil,
        label: String?? = nil,
        layoutWrap: Bool?? = nil,
        margin: Spacing?? = nil,
        maxHeight: LayoutScalar?? = nil,
        maxLines: Int?? = nil,
        maxWidth: LayoutScalar?? = nil,
        minHeight: LayoutScalar?? = nil,
        minWidth: LayoutScalar?? = nil,
        monospacedDigits: Bool?? = nil,
        opacity: Double?? = nil,
        orientation: Orientation?? = nil,
        overlays: [AtomicOverlay]?? = nil,
        padding: Spacing?? = nil,
        pageIndicator: PageIndicator?? = nil,
        paging: Bool?? = nil,
        placeholder: String?? = nil,
        rows: [[String: String]]?? = nil,
        section: Section?? = nil,
        shadow: Shadow?? = nil,
        shadows: [Shadow]?? = nil,
        showIndicators: Bool?? = nil,
        size: Int?? = nil,
        snapAlignment: Align?? = nil,
        snapshotAt: Date?? = nil,
        snapshotSeconds: Int?? = nil,
        src: String?? = nil,
        stopAtSeconds: Int?? = nil,
        striped: Bool?? = nil,
        textAlign: Align?? = nil,
        thickness: Int?? = nil,
        tickDirection: TickDirection?? = nil,
        trueChild: AtomicElement?? = nil,
        type: String? = nil,
        variant: String?? = nil,
        weight: TextWeight?? = nil,
        width: LayoutScalar?? = nil,
        widthMode: SizingMode?? = nil
    ) -> AtomicElement {
        return AtomicElement(
            accessibility: accessibility ?? self.accessibility,
            actions: actions ?? self.actions,
            alignment: alignment ?? self.alignment,
            alignSelf: alignSelf ?? self.alignSelf,
            alt: alt ?? self.alt,
            aspectRatio: aspectRatio ?? self.aspectRatio,
            background: background ?? self.background,
            backgrounds: backgrounds ?? self.backgrounds,
            badge: badge ?? self.badge,
            base: base ?? self.base,
            bindRef: bindRef ?? self.bindRef,
            breakpoint: breakpoint ?? self.breakpoint,
            children: children ?? self.children,
            color: color ?? self.color,
            columns: columns ?? self.columns,
            condition: condition ?? self.condition,
            content: content ?? self.content,
            cornerRadii: cornerRadii ?? self.cornerRadii,
            cornerRadius: cornerRadius ?? self.cornerRadius,
            crossAlignment: crossAlignment ?? self.crossAlignment,
            crossAxisGap: crossAxisGap ?? self.crossAxisGap,
            direction: direction ?? self.direction,
            disabled: disabled ?? self.disabled,
            falseChild: falseChild ?? self.falseChild,
            fit: fit ?? self.fit,
            flex: flex ?? self.flex,
            format: format ?? self.format,
            gap: gap ?? self.gap,
            height: height ?? self.height,
            heightMode: heightMode ?? self.heightMode,
            icon: icon ?? self.icon,
            id: id ?? self.id,
            isRunning: isRunning ?? self.isRunning,
            label: label ?? self.label,
            layoutWrap: layoutWrap ?? self.layoutWrap,
            margin: margin ?? self.margin,
            maxHeight: maxHeight ?? self.maxHeight,
            maxLines: maxLines ?? self.maxLines,
            maxWidth: maxWidth ?? self.maxWidth,
            minHeight: minHeight ?? self.minHeight,
            minWidth: minWidth ?? self.minWidth,
            monospacedDigits: monospacedDigits ?? self.monospacedDigits,
            opacity: opacity ?? self.opacity,
            orientation: orientation ?? self.orientation,
            overlays: overlays ?? self.overlays,
            padding: padding ?? self.padding,
            pageIndicator: pageIndicator ?? self.pageIndicator,
            paging: paging ?? self.paging,
            placeholder: placeholder ?? self.placeholder,
            rows: rows ?? self.rows,
            section: section ?? self.section,
            shadow: shadow ?? self.shadow,
            shadows: shadows ?? self.shadows,
            showIndicators: showIndicators ?? self.showIndicators,
            size: size ?? self.size,
            snapAlignment: snapAlignment ?? self.snapAlignment,
            snapshotAt: snapshotAt ?? self.snapshotAt,
            snapshotSeconds: snapshotSeconds ?? self.snapshotSeconds,
            src: src ?? self.src,
            stopAtSeconds: stopAtSeconds ?? self.stopAtSeconds,
            striped: striped ?? self.striped,
            textAlign: textAlign ?? self.textAlign,
            thickness: thickness ?? self.thickness,
            tickDirection: tickDirection ?? self.tickDirection,
            trueChild: trueChild ?? self.trueChild,
            type: type ?? self.type,
            variant: variant ?? self.variant,
            weight: weight ?? self.weight,
            width: width ?? self.width,
            widthMode: widthMode ?? self.widthMode
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Section-specific data payload
///
/// Tabbed navigation with dynamic content sections per tab
///
/// Typed tabular data for an NBA-style boxscore (one per team)
///
/// Server-driven form section with typed fields bound to screen state
///
/// Ad placement primitive — carries placement semantics while delegating auction/targeting
/// to ad-platform SDKs (see ADR-007)
///
/// Sortable, paginated table of season statistical leaders (league-wide)
///
/// Inline subscription upsell banner. Reserved SDK integration point: the banner's visible
/// chrome is entirely server-composed via `ui` until the platform IAP SDK (StoreKit / Play
/// Billing) lands and starts mounting the purchase flow on CTA tap. `tiers` carries the IAP
/// product identifiers the SDK will later bind to; `ctaAction` is the (pre-SDK) fallback
/// action.
///
/// Full-screen subscription upsell hero. Reserved SDK integration point — same contract as
/// SubscribeBannerData: `ui` carries the full visible composition; `tiers` carries IAP
/// product identifiers the SDK will bind to post-landing.
///
/// Data payload for AtomicComposite sections — ui contains rendering instructions, content
/// carries domain data
///
/// Video player section — reserved SDK integration point for DRM / HLS / ad insertion.
/// `playerType`, `contentId`, `autoplay`, and `capabilities` are SDK inputs (the video SDK
/// reads them and mounts); `ui` carries the pre-SDK placeholder composition that renders
/// before the SDK is integrated and will serve as the loading/error placeholder afterwards.
// MARK: - DataClass
struct DataClass: Codable {
    let defaultTab, stateKey: String?
    let tabContents: [String: [Section]]?
    let tabs: [TabData]?
    /// Ordered list of column definitions; clients render left-to-right
    ///
    /// Ordered column definitions; clients render left-to-right
    let columns: [BoxscoreColumnDefinition]?
    /// Text shown when no player rows are available
    let emptyMessage: String?
    /// Player rows ordered by server (starters first, then bench)
    ///
    /// Player rows, pre-sorted by the server
    let players: [PlayerRow]?
    /// Screen-state key holding the current sort direction (asc/desc)
    let sortDirectionStateKey: String?
    /// Screen-state key holding the current sort column key
    let sortStateKey: String?
    /// Hex colour for team accent
    let teamColor: String?
    let teamLogoURL, teamName: String?
    /// Aggregate row shown at the bottom of the table
    let teamTotals: [String: JSONAny]?
    /// Three-letter team code, e.g. 'BOS'
    let teamTricode: String?
    let fields: [FormField]?
    /// Layout hint for field arrangement
    let layout: Layout?
    /// Action fired when the form is submitted
    let submitAction: Action?
    let submitLabel: String?
    /// Ad unit path used by the ad SDK
    let adUnitPath: String?
    /// Whether to collapse the slot when no fill is returned
    let collapseOnEmpty: Bool?
    /// Disclosure label displayed above/below the ad
    let label: String?
    /// Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses
    /// when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own
    /// width/height. Required so the stub renderer has no client-side chrome defaults.
    let placeholder: Placeholder?
    /// Ad network identifier, e.g. 'gam', 'amazon'
    let provider: String?
    /// Optional auto-refresh interval in seconds
    let refreshIntervalSEC: Int?
    /// Accepted creative sizes as [width, height] pairs
    let sizes: [[Int]]?
    /// Key-value targeting hints passed to ad SDK
    let targeting: [String: String]?
    /// Current page (1-based)
    let page: Int?
    /// Number of rows per page
    let pageSize: Int?
    /// Key of the column the table is currently sorted by
    let sortColumn: String?
    let sortDirection: SortDirection?
    /// Secondary text, e.g. '2025-26 Regular Season – Per Game'
    let subtitle: String?
    /// Table heading, e.g. 'Season Leaders'
    let title: String?
    /// Total number of rows available server-side (for pagination display)
    let totalRows: Int?
    /// Top-level fallback action invoked when the IAP SDK is not mounted (today, always).
    let ctaAction: Action?
    /// IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands;
    /// not used by the renderer, which reads the visible price copy out of `ui`.
    ///
    /// IAP product identifiers + server-emitted prices. Consumed by the IAP SDK when it lands;
    /// not used by the renderer.
    let tiers: [SubscriptionTier]?
    /// Atomic tree describing the banner's full visible surface. Renderer walks this tree
    /// exactly as an AtomicComposite would; no client-side chrome defaults are permitted. See
    /// AGENTS.md §15.1.
    ///
    /// Atomic tree describing the hero's full visible surface — logo, title, subtitle, feature
    /// list, tier cards, CTAs. Renderer walks this tree exactly as an AtomicComposite would.
    ///
    /// Root node of the atomic element tree — the rendering instructions
    ///
    /// Atomic tree describing the pre-SDK placeholder surface (e.g. play-glyph column framed at
    /// the player's aspectRatio). Renderer walks this tree exactly as an AtomicComposite would;
    /// no client-side chrome defaults are permitted. Once the video SDK lands this tree becomes
    /// the loading/error placeholder the SDK overlays.
    let ui: AtomicElement?
    /// Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
    /// data-binding support.
    let content: [String: JSONAny]?
    let autoplay: Bool?
    /// Platform capabilities the player should enable. Server includes only capabilities
    /// relevant to the requesting platform (via X-Platform header).
    let capabilities: [Capability]?
    /// Content identifier — interpreted by playerType (gameId for game, mediaId for vod, eventId
    /// for event, streamUrl for stream). Single field avoids mutually exclusive optional IDs.
    let contentID: String?
    let displayConfig: DisplayConfig?
    /// Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
    let playerType: PlayerType?

    enum CodingKeys: String, CodingKey {
        case defaultTab, stateKey, tabContents, tabs, columns, emptyMessage, players, sortDirectionStateKey, sortStateKey, teamColor
        case teamLogoURL = "teamLogoUrl"
        case teamName, teamTotals, teamTricode, fields, layout, submitAction, submitLabel, adUnitPath, collapseOnEmpty, label, placeholder, provider
        case refreshIntervalSEC = "refreshIntervalSec"
        case sizes, targeting, page, pageSize, sortColumn, sortDirection, subtitle, title, totalRows, ctaAction, tiers, ui, content, autoplay, capabilities
        case contentID = "contentId"
        case displayConfig, playerType
    }
}

// MARK: DataClass convenience initializers and mutators

extension DataClass {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(DataClass.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        defaultTab: String?? = nil,
        stateKey: String?? = nil,
        tabContents: [String: [Section]]?? = nil,
        tabs: [TabData]?? = nil,
        columns: [BoxscoreColumnDefinition]?? = nil,
        emptyMessage: String?? = nil,
        players: [PlayerRow]?? = nil,
        sortDirectionStateKey: String?? = nil,
        sortStateKey: String?? = nil,
        teamColor: String?? = nil,
        teamLogoURL: String?? = nil,
        teamName: String?? = nil,
        teamTotals: [String: JSONAny]?? = nil,
        teamTricode: String?? = nil,
        fields: [FormField]?? = nil,
        layout: Layout?? = nil,
        submitAction: Action?? = nil,
        submitLabel: String?? = nil,
        adUnitPath: String?? = nil,
        collapseOnEmpty: Bool?? = nil,
        label: String?? = nil,
        placeholder: Placeholder?? = nil,
        provider: String?? = nil,
        refreshIntervalSEC: Int?? = nil,
        sizes: [[Int]]?? = nil,
        targeting: [String: String]?? = nil,
        page: Int?? = nil,
        pageSize: Int?? = nil,
        sortColumn: String?? = nil,
        sortDirection: SortDirection?? = nil,
        subtitle: String?? = nil,
        title: String?? = nil,
        totalRows: Int?? = nil,
        ctaAction: Action?? = nil,
        tiers: [SubscriptionTier]?? = nil,
        ui: AtomicElement?? = nil,
        content: [String: JSONAny]?? = nil,
        autoplay: Bool?? = nil,
        capabilities: [Capability]?? = nil,
        contentID: String?? = nil,
        displayConfig: DisplayConfig?? = nil,
        playerType: PlayerType?? = nil
    ) -> DataClass {
        return DataClass(
            defaultTab: defaultTab ?? self.defaultTab,
            stateKey: stateKey ?? self.stateKey,
            tabContents: tabContents ?? self.tabContents,
            tabs: tabs ?? self.tabs,
            columns: columns ?? self.columns,
            emptyMessage: emptyMessage ?? self.emptyMessage,
            players: players ?? self.players,
            sortDirectionStateKey: sortDirectionStateKey ?? self.sortDirectionStateKey,
            sortStateKey: sortStateKey ?? self.sortStateKey,
            teamColor: teamColor ?? self.teamColor,
            teamLogoURL: teamLogoURL ?? self.teamLogoURL,
            teamName: teamName ?? self.teamName,
            teamTotals: teamTotals ?? self.teamTotals,
            teamTricode: teamTricode ?? self.teamTricode,
            fields: fields ?? self.fields,
            layout: layout ?? self.layout,
            submitAction: submitAction ?? self.submitAction,
            submitLabel: submitLabel ?? self.submitLabel,
            adUnitPath: adUnitPath ?? self.adUnitPath,
            collapseOnEmpty: collapseOnEmpty ?? self.collapseOnEmpty,
            label: label ?? self.label,
            placeholder: placeholder ?? self.placeholder,
            provider: provider ?? self.provider,
            refreshIntervalSEC: refreshIntervalSEC ?? self.refreshIntervalSEC,
            sizes: sizes ?? self.sizes,
            targeting: targeting ?? self.targeting,
            page: page ?? self.page,
            pageSize: pageSize ?? self.pageSize,
            sortColumn: sortColumn ?? self.sortColumn,
            sortDirection: sortDirection ?? self.sortDirection,
            subtitle: subtitle ?? self.subtitle,
            title: title ?? self.title,
            totalRows: totalRows ?? self.totalRows,
            ctaAction: ctaAction ?? self.ctaAction,
            tiers: tiers ?? self.tiers,
            ui: ui ?? self.ui,
            content: content ?? self.content,
            autoplay: autoplay ?? self.autoplay,
            capabilities: capabilities ?? self.capabilities,
            contentID: contentID ?? self.contentID,
            displayConfig: displayConfig ?? self.displayConfig,
            playerType: playerType ?? self.playerType
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Full section object to render via SectionRouter. Only used when type is SectionSlot.
// MARK: - Section
class Section: Codable {
    /// Section-level accessibility metadata (landmark role, live region, heading)
    let accessibility: AccessibilityProperties?
    /// Section-level interaction actions
    let actions: [Action]?
    let analyticsID, backgroundColor: String?
    /// Origin identifier for the content backing this section (e.g. 'cms:article-42',
    /// 'stats-api:leaders-2025'). Carried through to analytics for two-tier attribution.
    let contentSourceID: String?
    /// Section-specific data payload
    let data: DataClass?
    let dataBinding: DataBinding?
    let id: String
    let layoutHints: SectionLayoutHints?
    let padding: Spacing?
    let refreshPolicy: RefreshPolicy?
    let sectionStates: SectionStates?
    /// Section-level map of translation key to localized string. Used by DataBindingResolver to
    /// resolve stringKeys on real-time updates.
    let stringTable: [String: String]?
    /// Nested interaction targets within the section
    let subsections: [Subsection]?
    let surface: SectionSurface?
    let type: String

    enum CodingKeys: String, CodingKey {
        case accessibility, actions
        case analyticsID = "analyticsId"
        case backgroundColor
        case contentSourceID = "contentSourceId"
        case data, dataBinding, id, layoutHints, padding, refreshPolicy, sectionStates, stringTable, subsections, surface, type
    }

    init(accessibility: AccessibilityProperties?, actions: [Action]?, analyticsID: String?, backgroundColor: String?, contentSourceID: String?, data: DataClass?, dataBinding: DataBinding?, id: String, layoutHints: SectionLayoutHints?, padding: Spacing?, refreshPolicy: RefreshPolicy?, sectionStates: SectionStates?, stringTable: [String: String]?, subsections: [Subsection]?, surface: SectionSurface?, type: String) {
        self.accessibility = accessibility
        self.actions = actions
        self.analyticsID = analyticsID
        self.backgroundColor = backgroundColor
        self.contentSourceID = contentSourceID
        self.data = data
        self.dataBinding = dataBinding
        self.id = id
        self.layoutHints = layoutHints
        self.padding = padding
        self.refreshPolicy = refreshPolicy
        self.sectionStates = sectionStates
        self.stringTable = stringTable
        self.subsections = subsections
        self.surface = surface
        self.type = type
    }
}

// MARK: Section convenience initializers and mutators

extension Section {
    convenience init(data: Data) throws {
        let me = try newJSONDecoder().decode(Section.self, from: data)
        self.init(accessibility: me.accessibility, actions: me.actions, analyticsID: me.analyticsID, backgroundColor: me.backgroundColor, contentSourceID: me.contentSourceID, data: me.data, dataBinding: me.dataBinding, id: me.id, layoutHints: me.layoutHints, padding: me.padding, refreshPolicy: me.refreshPolicy, sectionStates: me.sectionStates, stringTable: me.stringTable, subsections: me.subsections, surface: me.surface, type: me.type)
    }

    convenience init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    convenience init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        accessibility: AccessibilityProperties?? = nil,
        actions: [Action]?? = nil,
        analyticsID: String?? = nil,
        backgroundColor: String?? = nil,
        contentSourceID: String?? = nil,
        data: DataClass?? = nil,
        dataBinding: DataBinding?? = nil,
        id: String? = nil,
        layoutHints: SectionLayoutHints?? = nil,
        padding: Spacing?? = nil,
        refreshPolicy: RefreshPolicy?? = nil,
        sectionStates: SectionStates?? = nil,
        stringTable: [String: String]?? = nil,
        subsections: [Subsection]?? = nil,
        surface: SectionSurface?? = nil,
        type: String? = nil
    ) -> Section {
        return Section(
            accessibility: accessibility ?? self.accessibility,
            actions: actions ?? self.actions,
            analyticsID: analyticsID ?? self.analyticsID,
            backgroundColor: backgroundColor ?? self.backgroundColor,
            contentSourceID: contentSourceID ?? self.contentSourceID,
            data: data ?? self.data,
            dataBinding: dataBinding ?? self.dataBinding,
            id: id ?? self.id,
            layoutHints: layoutHints ?? self.layoutHints,
            padding: padding ?? self.padding,
            refreshPolicy: refreshPolicy ?? self.refreshPolicy,
            sectionStates: sectionStates ?? self.sectionStates,
            stringTable: stringTable ?? self.stringTable,
            subsections: subsections ?? self.subsections,
            surface: surface ?? self.surface,
            type: type ?? self.type
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Position of the badge within the parent bounds
///
/// Position of the overlay within the base element bounds.
///
/// Position of the indicator within the ScrollContainer bounds.
enum BadgeAlignment: String, Codable {
    case bottomCenter = "bottomCenter"
    case bottomEnd = "bottomEnd"
    case bottomStart = "bottomStart"
    case center = "center"
    case centerEnd = "centerEnd"
    case centerStart = "centerStart"
    case topCenter = "topCenter"
    case topEnd = "topEnd"
    case topStart = "topStart"
}

/// Outer space between the element and its siblings or parent edges. Applied outside the
/// element's background, border, corner radius, and shadow — use this for sibling-to-sibling
/// spacing instead of Spacer siblings when inhomogeneous gaps are needed.
///
/// Optional edge offsets from the aligned base bounds.
///
/// Inner space between the element's own background/border and its content.
///
/// Outer margin (space between the surface and its siblings / screen edge).
///
/// Inner padding (space between the surface edge and the content it wraps).
// MARK: - Spacing
struct Spacing: Codable {
    let bottom, end, start, top: LayoutScalar?
}

// MARK: Spacing convenience initializers and mutators

extension Spacing {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Spacing.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        bottom: LayoutScalar?? = nil,
        end: LayoutScalar?? = nil,
        start: LayoutScalar?? = nil,
        top: LayoutScalar?? = nil
    ) -> Spacing {
        return Spacing(
            bottom: bottom ?? self.bottom,
            end: end ?? self.end,
            start: start ?? self.start,
            top: top ?? self.top
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Bottom-trailing corner.
///
/// Absolute layout value: raw dp/px integer, or a semantic layout token reference
/// token:<path> (e.g. token:nba.spacing.md, token:nba.radius.lg) resolved per
/// platform.formFactor against bundled spacing/corner/size/typography/shadow registries.
/// Unknown tokens log token_resolver_missing and fall back to 0 (or caller default).
///
/// Bottom-leading corner.
///
/// Top-trailing corner (top-right in LTR, top-left in RTL).
///
/// Top-leading corner (top-left in LTR, top-right in RTL).
///
/// Corner radius: dp/px or layout token. Applied to Container (with overflow clip) and Image
/// elements.
///
/// Gap between wrapped lines when layoutWrap is true. Falls back to gap when absent. Ignored
/// when layoutWrap is false.
///
/// Gap between flex children (row/column), or grid gap where applicable.
///
/// Fixed height in dp/px or layout token.
///
/// Maximum height constraint in dp/px or layout token.
///
/// Maximum width constraint in dp/px or layout token.
///
/// Minimum height constraint in dp/px or layout token.
///
/// Minimum width constraint in dp/px or layout token.
///
/// Fixed width in dp/px or layout token.
///
/// Corner radius: dp/px or layout token, applied to the surface (with overflow clip).
enum LayoutScalar: Codable {
    case integer(Int)
    case string(String)

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let x = try? container.decode(Int.self) {
            self = .integer(x)
            return
        }
        if let x = try? container.decode(String.self) {
            self = .string(x)
            return
        }
        throw DecodingError.typeMismatch(LayoutScalar.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for LayoutScalar"))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .integer(let x):
            try container.encode(x)
        case .string(let x):
            try container.encode(x)
        }
    }
}

/// Section-level accessibility metadata (landmark role, live region, heading)
///
/// Server-provided accessibility metadata applied natively per platform
///
/// Server-provided accessibility metadata for this atomic element
///
/// Subsection-level accessibility metadata
// MARK: - AccessibilityProperties
struct AccessibilityProperties: Codable {
    /// Heading level (1-6) for role=heading elements. Maps to aria-level (Web),
    /// accessibilityAddTraits .isHeader (iOS), semantics { heading() } (Android).
    let headingLevel: Int?
    /// When true, element and its descendants are hidden from the accessibility tree (decorative
    /// content).
    let hidden: Bool?
    /// Additional context announced after the label. Maps to accessibilityHint (iOS),
    /// contentDescription suffix (Android), aria-describedby text (Web).
    let hint: String?
    /// Human-readable label announced by screen readers. Omit for elements whose text content is
    /// self-describing.
    let label: String?
    /// Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to
    /// aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
    let liveRegion: LiveRegion?
    /// Semantic role override. 'none' suppresses the element's intrinsic role.
    let role: Role?
    /// Override default accessibility traversal order. Lower values are visited first. Omit to
    /// use natural DOM/view order.
    let sortOrder: Int?
}

// MARK: AccessibilityProperties convenience initializers and mutators

extension AccessibilityProperties {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(AccessibilityProperties.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        headingLevel: Int?? = nil,
        hidden: Bool?? = nil,
        hint: String?? = nil,
        label: String?? = nil,
        liveRegion: LiveRegion?? = nil,
        role: Role?? = nil,
        sortOrder: Int?? = nil
    ) -> AccessibilityProperties {
        return AccessibilityProperties(
            headingLevel: headingLevel ?? self.headingLevel,
            hidden: hidden ?? self.hidden,
            hint: hint ?? self.hint,
            label: label ?? self.label,
            liveRegion: liveRegion ?? self.liveRegion,
            role: role ?? self.role,
            sortOrder: sortOrder ?? self.sortOrder
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Announces content changes. 'polite' waits for idle; 'assertive' interrupts. Maps to
/// aria-live (Web), accessibilityLiveRegion (Android), .accessibilityAddTraits (iOS).
enum LiveRegion: String, Codable {
    case assertive = "assertive"
    case off = "off"
    case polite = "polite"
}

/// Semantic role override. 'none' suppresses the element's intrinsic role.
enum Role: String, Codable {
    case button = "button"
    case cell = "cell"
    case heading = "heading"
    case image = "image"
    case link = "link"
    case list = "list"
    case listitem = "listitem"
    case none = "none"
    case row = "row"
    case tab = "tab"
    case table = "table"
    case tabpanel = "tabpanel"
}

/// Per-child cross-axis alignment override. When set, wins over parent crossAlignment for
/// this child (matches Figma and CSS align-self semantics).
enum CrossAlignment: String, Codable {
    case center = "center"
    case end = "end"
    case start = "start"
    case stretch = "stretch"
}

enum Alignment: String, Codable {
    case center = "center"
    case end = "end"
    case spaceAround = "spaceAround"
    case spaceBetween = "spaceBetween"
    case spaceEvenly = "spaceEvenly"
    case start = "start"
}

/// Aspect ratio: legacy numeric (w/h), or named ratio string for semantic layout.
enum AspectRatioUnion: Codable {
    case double(Double)
    case enumeration(AspectRatioEnum)

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let x = try? container.decode(Double.self) {
            self = .double(x)
            return
        }
        if let x = try? container.decode(AspectRatioEnum.self) {
            self = .enumeration(x)
            return
        }
        throw DecodingError.typeMismatch(AspectRatioUnion.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for AspectRatioUnion"))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .double(let x):
            try container.encode(x)
        case .enumeration(let x):
            try container.encode(x)
        }
    }
}

enum AspectRatioEnum: String, Codable {
    case the11 = "1:1"
    case the169 = "16:9"
    case the219 = "21:9"
    case the32 = "3:2"
    case the43 = "4:3"
}

/// DEPRECATED — use backgrounds (array) for new payloads. Single background. If both
/// background and backgrounds are present, backgrounds wins.
///
/// Shared background type — solid color, gradient, or image with overlay
///
/// Surface background (solid, gradient, or image).
enum BackgroundUnion: Codable {
    case background(Background)
    case string(String)

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let x = try? container.decode(String.self) {
            self = .string(x)
            return
        }
        if let x = try? container.decode(Background.self) {
            self = .background(x)
            return
        }
        throw DecodingError.typeMismatch(BackgroundUnion.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for BackgroundUnion"))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .background(let x):
            try container.encode(x)
        case .string(let x):
            try container.encode(x)
        }
    }
}

/// Gradient background with ordered color stops
///
/// Image background with optional scale and overlay
// MARK: - Background
struct Background: Codable {
    /// Ordered list of color stops (hex or semantic token)
    let colors: [String]?
    let direction: Direction?
    /// URL of the background image
    let imageURL: String?
    /// Optional overlay applied on top of the image
    let overlay: Overlay?
    let scaleType: ScaleType?

    enum CodingKeys: String, CodingKey {
        case colors, direction
        case imageURL = "imageUrl"
        case overlay, scaleType
    }
}

// MARK: Background convenience initializers and mutators

extension Background {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Background.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        colors: [String]?? = nil,
        direction: Direction?? = nil,
        imageURL: String?? = nil,
        overlay: Overlay?? = nil,
        scaleType: ScaleType?? = nil
    ) -> Background {
        return Background(
            colors: colors ?? self.colors,
            direction: direction ?? self.direction,
            imageURL: imageURL ?? self.imageURL,
            overlay: overlay ?? self.overlay,
            scaleType: scaleType ?? self.scaleType
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum Direction: String, Codable {
    case diagonal = "diagonal"
    case horizontal = "horizontal"
    case vertical = "vertical"
}

/// Optional overlay applied on top of the image
enum Overlay: Codable {
    case backgroundGradient(BackgroundGradient)
    case string(String)

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let x = try? container.decode(String.self) {
            self = .string(x)
            return
        }
        if let x = try? container.decode(BackgroundGradient.self) {
            self = .backgroundGradient(x)
            return
        }
        throw DecodingError.typeMismatch(Overlay.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for Overlay"))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .backgroundGradient(let x):
            try container.encode(x)
        case .string(let x):
            try container.encode(x)
        }
    }
}

/// Gradient background with ordered color stops
// MARK: - BackgroundGradient
struct BackgroundGradient: Codable {
    /// Ordered list of color stops (hex or semantic token)
    let colors: [String]
    let direction: Direction?
}

// MARK: BackgroundGradient convenience initializers and mutators

extension BackgroundGradient {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(BackgroundGradient.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        colors: [String]? = nil,
        direction: Direction?? = nil
    ) -> BackgroundGradient {
        return BackgroundGradient(
            colors: colors ?? self.colors,
            direction: direction ?? self.direction
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum ScaleType: String, Codable {
    case contain = "contain"
    case cover = "cover"
    case fill = "fill"
}

// MARK: - Column
struct Column: Codable {
    let align: Align?
    /// Row data key
    let key: String
    /// Header label
    let label: String
    /// Fixed width (integer) or 'flex'
    let width: WidthUnion?
}

// MARK: Column convenience initializers and mutators

extension Column {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Column.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        align: Align?? = nil,
        key: String? = nil,
        label: String? = nil,
        width: WidthUnion?? = nil
    ) -> Column {
        return Column(
            align: align ?? self.align,
            key: key ?? self.key,
            label: label ?? self.label,
            width: width ?? self.width
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Text alignment within the element. Used for centered headings, right-aligned numeric
/// values.
enum Align: String, Codable {
    case center = "center"
    case end = "end"
    case start = "start"
}

/// Fixed width (integer) or 'flex'
enum WidthUnion: Codable {
    case enumeration(WidthEnum)
    case integer(Int)

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let x = try? container.decode(Int.self) {
            self = .integer(x)
            return
        }
        if let x = try? container.decode(WidthEnum.self) {
            self = .enumeration(x)
            return
        }
        throw DecodingError.typeMismatch(WidthUnion.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for WidthUnion"))
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .enumeration(let x):
            try container.encode(x)
        case .integer(let x):
            try container.encode(x)
        }
    }
}

enum WidthEnum: String, Codable {
    case flex = "flex"
}

/// Per-corner cornerRadius override. When present, takes precedence over the single-value
/// cornerRadius; any corner key omitted falls back to cornerRadius (or 0 if that is also
/// absent). Used for asymmetric card shapes — e.g. content-rail cards with rounded tops and
/// square bottoms so headline text does not collide with a bottom-corner curve. iOS maps to
/// UnevenRoundedRectangle (iOS 16+); Android to RoundedCornerShape's four-corner
/// constructor; web to borderTopLeftRadius / borderTopRightRadius / borderBottomLeftRadius /
/// borderBottomRightRadius.
// MARK: - CornerRadii
struct CornerRadii: Codable {
    /// Bottom-trailing corner.
    let bottomEnd: LayoutScalar?
    /// Bottom-leading corner.
    let bottomStart: LayoutScalar?
    /// Top-trailing corner (top-right in LTR, top-left in RTL).
    let topEnd: LayoutScalar?
    /// Top-leading corner (top-left in LTR, top-right in RTL).
    let topStart: LayoutScalar?
}

// MARK: CornerRadii convenience initializers and mutators

extension CornerRadii {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(CornerRadii.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        bottomEnd: LayoutScalar?? = nil,
        bottomStart: LayoutScalar?? = nil,
        topEnd: LayoutScalar?? = nil,
        topStart: LayoutScalar?? = nil
    ) -> CornerRadii {
        return CornerRadii(
            bottomEnd: bottomEnd ?? self.bottomEnd,
            bottomStart: bottomStart ?? self.bottomStart,
            topEnd: topEnd ?? self.topEnd,
            topStart: topStart ?? self.topStart
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum UIDirection: String, Codable {
    case column = "column"
    case row = "row"
}

enum ImageFit: String, Codable {
    case contain = "contain"
    case cover = "cover"
    case fill = "fill"
    case none = "none"
}

/// LiveClock display format. Clients realize using their platform's tabular-numerals
/// typography (equivalent to TextVariant.score).
enum Format: String, Codable {
    case hMmSs = "h:mm:ss"
    case mSs = "m:ss"
    case mmSs = "mm:ss"
}

/// Sizing behavior along the height axis. 'hug' = intrinsic, 'fill' = stretch to parent,
/// 'fixed' = use explicit height value.
///
/// Sizing behavior along one axis. 'hug' sizes to content (default). 'fill' stretches to
/// parent available space. 'fixed' uses the explicit width/height value.
///
/// Sizing behavior along the width axis. 'hug' = intrinsic, 'fill' = stretch to parent,
/// 'fixed' = use explicit width value.
enum SizingMode: String, Codable {
    case fill = "fill"
    case fixed = "fixed"
    case hug = "hug"
}

enum Orientation: String, Codable {
    case horizontal = "horizontal"
    case vertical = "vertical"
}

/// Optional ScrollContainer page indicator. Clients render it only when declared.
///
/// Server-declared scroll page indicator presentation for ScrollContainer. Clients own local
/// scroll state only to realize the declared affordance.
// MARK: - PageIndicator
struct PageIndicator: Codable {
    /// Active indicator color.
    let activeColor: String?
    /// Position of the indicator within the ScrollContainer bounds.
    let alignment: BadgeAlignment
    /// Inactive indicator color.
    let color: String?
    /// Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal
    /// bar segments.
    let style: Style
}

// MARK: PageIndicator convenience initializers and mutators

extension PageIndicator {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(PageIndicator.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        activeColor: String?? = nil,
        alignment: BadgeAlignment? = nil,
        color: String?? = nil,
        style: Style? = nil
    ) -> PageIndicator {
        return PageIndicator(
            activeColor: activeColor ?? self.activeColor,
            alignment: alignment ?? self.alignment,
            color: color ?? self.color,
            style: style ?? self.style
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Indicator visualization style. 'dots' renders circular dots; 'dashes' renders horizontal
/// bar segments.
enum Style: String, Codable {
    case dashes = "dashes"
    case dots = "dots"
}

/// DEPRECATED — use shadows (array) for new payloads. Single shadow. If both shadow and
/// shadows are present, shadows wins.
///
/// Shadow effect with CSS/SwiftUI semantics (radius + offset). Compose approximates via
/// elevation. Use 'type' to distinguish drop vs inner shadows.
///
/// Drop shadow applied to the surface.
// MARK: - Shadow
struct Shadow: Codable {
    /// Shadow color (hex with alpha, or token reference)
    let color: String?
    /// Horizontal offset in dp/px
    let offsetX: Double?
    /// Vertical offset in dp/px
    let offsetY: Double?
    /// Blur radius in dp/px
    let radius: Double?
    /// Shadow type. 'drop' is an outer shadow (default, backward-compatible). 'inner' is an
    /// inset shadow. Platforms without first-class inner shadow support fall back to drop with a
    /// diagnostic.
    let type: ShadowType?
}

// MARK: Shadow convenience initializers and mutators

extension Shadow {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Shadow.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        color: String?? = nil,
        offsetX: Double?? = nil,
        offsetY: Double?? = nil,
        radius: Double?? = nil,
        type: ShadowType?? = nil
    ) -> Shadow {
        return Shadow(
            color: color ?? self.color,
            offsetX: offsetX ?? self.offsetX,
            offsetY: offsetY ?? self.offsetY,
            radius: radius ?? self.radius,
            type: type ?? self.type
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Shadow type. 'drop' is an outer shadow (default, backward-compatible). 'inner' is an
/// inset shadow. Platforms without first-class inner shadow support fall back to drop with a
/// diagnostic.
enum ShadowType: String, Codable {
    case drop = "drop"
    case inner = "inner"
}

/// LiveClock tick direction. 'down' decrements from snapshotSeconds toward stopAtSeconds
/// (default 0); 'up' increments from snapshotSeconds with no upper bound unless
/// stopAtSeconds is set.
enum TickDirection: String, Codable {
    case down = "down"
    case up = "up"
}


/// Font weight tokens for atomic Text elements.
enum TextWeight: String, Codable {
    case bold = "bold"
    case medium = "medium"
    case regular = "regular"
    case semiBold = "semiBold"
}

enum Capability: String, Codable {
    case airplay = "airplay"
    case backgroundAudio = "backgroundAudio"
    case chromecast = "chromecast"
    case fullscreenRotation = "fullscreenRotation"
    case pip = "pip"
}

/// Defines a single column in the boxscore table
// MARK: - BoxscoreColumnDefinition
struct BoxscoreColumnDefinition: Codable {
    /// Whether this column should be visually emphasised (e.g., bold)
    let highlighted: Bool?
    /// Property key on each player's stats object that supplies this column's value
    let key: String
    /// Column header text displayed to the user
    let label: String
    /// Whether this column supports client-side sorting
    let sortable: Bool?
    /// Optional hint for column width (e.g. 'auto', '64px', '1fr')
    let width: String?
}

// MARK: BoxscoreColumnDefinition convenience initializers and mutators

extension BoxscoreColumnDefinition {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(BoxscoreColumnDefinition.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        highlighted: Bool?? = nil,
        key: String? = nil,
        label: String? = nil,
        sortable: Bool?? = nil,
        width: String?? = nil
    ) -> BoxscoreColumnDefinition {
        return BoxscoreColumnDefinition(
            highlighted: highlighted ?? self.highlighted,
            key: key ?? self.key,
            label: label ?? self.label,
            sortable: sortable ?? self.sortable,
            width: width ?? self.width
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - DisplayConfig
struct DisplayConfig: Codable {
    let aspectRatio: String?
    let height: Int?
}

// MARK: DisplayConfig convenience initializers and mutators

extension DisplayConfig {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(DisplayConfig.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        aspectRatio: String?? = nil,
        height: Int?? = nil
    ) -> DisplayConfig {
        return DisplayConfig(
            aspectRatio: aspectRatio ?? self.aspectRatio,
            height: height ?? self.height
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// One input field inside a form section
// MARK: - FormField
struct FormField: Codable {
    let disabled: Bool?
    let fieldID: String
    /// Input type; clients map to platform-native controls
    let fieldType: FieldType
    let label: String
    /// For select/radio/checkbox field types: the available choices
    let options: [FormOption]?
    let placeholder: String?
    let formFieldRequired: Bool?
    /// Screen-state key that holds this field's current value
    let stateKey: String
    /// Message to show when validation fails
    let validationMessage: String?
    /// Optional regex pattern for client-side validation
    let validationPattern: String?
    /// How to realize the control. Applies only when fieldType == 'select'. Missing value is
    /// treated as 'dropdown' at render time.
    let variant: SelectVariant?

    enum CodingKeys: String, CodingKey {
        case disabled
        case fieldID = "fieldId"
        case fieldType, label, options, placeholder
        case formFieldRequired = "required"
        case stateKey, validationMessage, validationPattern, variant
    }
}

// MARK: FormField convenience initializers and mutators

extension FormField {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(FormField.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        disabled: Bool?? = nil,
        fieldID: String? = nil,
        fieldType: FieldType? = nil,
        label: String? = nil,
        options: [FormOption]?? = nil,
        placeholder: String?? = nil,
        formFieldRequired: Bool?? = nil,
        stateKey: String? = nil,
        validationMessage: String?? = nil,
        validationPattern: String?? = nil,
        variant: SelectVariant?? = nil
    ) -> FormField {
        return FormField(
            disabled: disabled ?? self.disabled,
            fieldID: fieldID ?? self.fieldID,
            fieldType: fieldType ?? self.fieldType,
            label: label ?? self.label,
            options: options ?? self.options,
            placeholder: placeholder ?? self.placeholder,
            formFieldRequired: formFieldRequired ?? self.formFieldRequired,
            stateKey: stateKey ?? self.stateKey,
            validationMessage: validationMessage ?? self.validationMessage,
            validationPattern: validationPattern ?? self.validationPattern,
            variant: variant ?? self.variant
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Input type; clients map to platform-native controls
enum FieldType: String, Codable {
    case checkbox = "checkbox"
    case date = "date"
    case number = "number"
    case radio = "radio"
    case select = "select"
    case text = "text"
    case textarea = "textarea"
    case toggle = "toggle"
}

/// One selectable option within a select/radio/checkbox form field
// MARK: - FormOption
struct FormOption: Codable {
    let label, value: String
}

// MARK: FormOption convenience initializers and mutators

extension FormOption {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(FormOption.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        label: String? = nil,
        value: String? = nil
    ) -> FormOption {
        return FormOption(
            label: label ?? self.label,
            value: value ?? self.value
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// How to realize the control. Applies only when fieldType == 'select'. Missing value is
/// treated as 'dropdown' at render time.
///
/// How a Form single-select field is realized by the client. 'dropdown' maps to the platform
/// menu (default). 'chips' is a horizontally-scrollable row of tappable capsules. Applies
/// only when FormField.fieldType == 'select'.
enum SelectVariant: String, Codable {
    case chips = "chips"
    case dropdown = "dropdown"
}

/// Layout hint for field arrangement
enum Layout: String, Codable {
    case grid = "grid"
    case horizontal = "horizontal"
    case vertical = "vertical"
}

/// Visual treatment for the reserved rectangle before the ad SDK mounts (and after misses
/// when collapseOnEmpty is false). Shares dimensions with sizes[0] — never carries its own
/// width/height. Required so the stub renderer has no client-side chrome defaults.
// MARK: - Placeholder
struct Placeholder: Codable {
    /// Fill color for the empty rectangle.
    let backgroundColor: String?
    /// Caption rendered inside the empty rectangle (e.g. 'Advertisement').
    let text: String?
}

// MARK: Placeholder convenience initializers and mutators

extension Placeholder {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Placeholder.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        backgroundColor: String?? = nil,
        text: String?? = nil
    ) -> Placeholder {
        return Placeholder(
            backgroundColor: backgroundColor ?? self.backgroundColor,
            text: text ?? self.text
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Discriminator for SDK player variant. Client passes contentId to the matching SDK method.
enum PlayerType: String, Codable {
    case event = "event"
    case game = "game"
    case nbaTv = "nbaTv"
    case stream = "stream"
    case vod = "vod"
}

/// One player row inside a boxscore table
///
/// One ranked player row in a season leaders table
// MARK: - PlayerRow
struct PlayerRow: Codable {
    let actions: [Action]?
    let imageURL, jerseyNumber: String?
    /// Display name (short form, e.g. 'J. Tatum')
    ///
    /// Display name, e.g. 'Luka Dončić'
    let name: String
    let playerID: String
    let position: String?
    /// Whether this player was in the starting lineup
    let starter: Bool?
    /// Stat values keyed by column key (gp, min, pts, reb, ast, etc.)
    let stats: [String: JSONAny]
    /// Ranking position (1-based)
    let rank: Int?
    /// Team tricode, e.g. 'LAL'
    let team: String?

    enum CodingKeys: String, CodingKey {
        case actions
        case imageURL = "imageUrl"
        case jerseyNumber, name
        case playerID = "playerId"
        case position, starter, stats, rank, team
    }
}

// MARK: PlayerRow convenience initializers and mutators

extension PlayerRow {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(PlayerRow.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        actions: [Action]?? = nil,
        imageURL: String?? = nil,
        jerseyNumber: String?? = nil,
        name: String? = nil,
        playerID: String? = nil,
        position: String?? = nil,
        starter: Bool?? = nil,
        stats: [String: JSONAny]? = nil,
        rank: Int?? = nil,
        team: String?? = nil
    ) -> PlayerRow {
        return PlayerRow(
            actions: actions ?? self.actions,
            imageURL: imageURL ?? self.imageURL,
            jerseyNumber: jerseyNumber ?? self.jerseyNumber,
            name: name ?? self.name,
            playerID: playerID ?? self.playerID,
            position: position ?? self.position,
            starter: starter ?? self.starter,
            stats: stats ?? self.stats,
            rank: rank ?? self.rank,
            team: team ?? self.team
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum SortDirection: String, Codable {
    case asc = "asc"
    case desc = "desc"
}

// MARK: - TabData
struct TabData: Codable {
    let id, label: String
    let stateKey, stateValue: String?
}

// MARK: TabData convenience initializers and mutators

extension TabData {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(TabData.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        id: String? = nil,
        label: String? = nil,
        stateKey: String?? = nil,
        stateValue: String?? = nil
    ) -> TabData {
        return TabData(
            id: id ?? self.id,
            label: label ?? self.label,
            stateKey: stateKey ?? self.stateKey,
            stateValue: stateValue ?? self.stateValue
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - SubscriptionTier
struct SubscriptionTier: Codable {
    /// Badge label, e.g. 'BEST VALUE', 'MOST POPULAR'
    let badgeText: String?
    let ctaAction: Action?
    let ctaLabel: String?
    let features: [String]?
    let id: String
    /// Tier name, e.g. 'League Pass', 'League Pass Premium'
    let name: String
    /// Strikethrough price if on sale
    let originalPrice: String?
    /// Display price, e.g. '$14.99/mo'
    let price: String
}

// MARK: SubscriptionTier convenience initializers and mutators

extension SubscriptionTier {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(SubscriptionTier.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        badgeText: String?? = nil,
        ctaAction: Action?? = nil,
        ctaLabel: String?? = nil,
        features: [String]?? = nil,
        id: String? = nil,
        name: String? = nil,
        originalPrice: String?? = nil,
        price: String? = nil
    ) -> SubscriptionTier {
        return SubscriptionTier(
            badgeText: badgeText ?? self.badgeText,
            ctaAction: ctaAction ?? self.ctaAction,
            ctaLabel: ctaLabel ?? self.ctaLabel,
            features: features ?? self.features,
            id: id ?? self.id,
            name: name ?? self.name,
            originalPrice: originalPrice ?? self.originalPrice,
            price: price ?? self.price
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - DataBinding
struct DataBinding: Codable {
    let bindings: [DataBindingPath]?
    /// Optional map of targetPath to translation key for client-side i18n resolution on bound
    /// fields
    let stringKeys: [String: String]?
}

// MARK: DataBinding convenience initializers and mutators

extension DataBinding {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(DataBinding.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        bindings: [DataBindingPath]?? = nil,
        stringKeys: [String: String]?? = nil
    ) -> DataBinding {
        return DataBinding(
            bindings: bindings ?? self.bindings,
            stringKeys: stringKeys ?? self.stringKeys
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - DataBindingPath
struct DataBindingPath: Codable {
    /// JSONPath in incoming message (e.g., '$.homeTeam.score')
    let sourcePath: String
    /// Dot-path to component property (e.g., 'homeScore.content')
    let targetPath: String
    /// Optional server-declared transform applied by shared client binding infrastructure before
    /// writing the target value. liveClockSnapshot normalizes clock payload values into {
    /// snapshotSeconds, snapshotAt, isRunning }.
    let transform: Transform?
}

// MARK: DataBindingPath convenience initializers and mutators

extension DataBindingPath {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(DataBindingPath.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        sourcePath: String? = nil,
        targetPath: String? = nil,
        transform: Transform?? = nil
    ) -> DataBindingPath {
        return DataBindingPath(
            sourcePath: sourcePath ?? self.sourcePath,
            targetPath: targetPath ?? self.targetPath,
            transform: transform ?? self.transform
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Optional server-declared transform applied by shared client binding infrastructure before
/// writing the target value. liveClockSnapshot normalizes clock payload values into {
/// snapshotSeconds, snapshotAt, isRunning }.
enum Transform: String, Codable {
    case liveClockSnapshot = "liveClockSnapshot"
}

/// Optional layout hints for section placement. Clients apply best-effort; unknown hints are
/// ignored.
// MARK: - SectionLayoutHints
struct SectionLayoutHints: Codable {
    /// Render a divider line above this section
    let dividerAbove: Bool?
    /// Render a divider line below this section
    let dividerBelow: Bool?
    /// Bottom margin in dp/points
    let marginBottom: Int?
    /// Top margin in dp/points (0 = flush)
    let marginTop: Int?
    /// Rendering priority hint — clients may use for lazy loading or viewport priority
    let priority: Priority?
}

// MARK: SectionLayoutHints convenience initializers and mutators

extension SectionLayoutHints {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(SectionLayoutHints.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        dividerAbove: Bool?? = nil,
        dividerBelow: Bool?? = nil,
        marginBottom: Int?? = nil,
        marginTop: Int?? = nil,
        priority: Priority?? = nil
    ) -> SectionLayoutHints {
        return SectionLayoutHints(
            dividerAbove: dividerAbove ?? self.dividerAbove,
            dividerBelow: dividerBelow ?? self.dividerBelow,
            marginBottom: marginBottom ?? self.marginBottom,
            marginTop: marginTop ?? self.marginTop,
            priority: priority ?? self.priority
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Rendering priority hint — clients may use for lazy loading or viewport priority
enum Priority: String, Codable {
    case high = "high"
    case low = "low"
    case normal = "normal"
}

/// Server-declared loading and error presentation for a section. Clients render these states
/// when applicable.
// MARK: - SectionStates
struct SectionStates: Codable {
    /// Server-declared error presentation for this section.
    let error: ErrorState?
    let loading: Loading?
}

// MARK: SectionStates convenience initializers and mutators

extension SectionStates {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(SectionStates.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        error: ErrorState?? = nil,
        loading: Loading?? = nil
    ) -> SectionStates {
        return SectionStates(
            error: error ?? self.error,
            loading: loading ?? self.loading
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Server-declared error presentation for this section.
///
/// Server-declared error-state shape rendered by section error boundaries. Named
/// `ErrorState` (not `Error`) so the generated client type does not shadow each runtime's
/// native error protocol (e.g. `Swift.Error`).
// MARK: - ErrorState
struct ErrorState: Codable {
    /// If true, collapse the section entirely on error instead of showing error UI
    let hideOnError: Bool?
    /// Error message to display (e.g., 'Unable to load scores')
    let message: String?
    /// Optional action to trigger on retry tap (typically a refresh action)
    let retryAction: Action?
    /// Button label for the retry CTA (e.g. 'Try Again', 'Reload'). Clients use 'Retry' as a
    /// neutral default when omitted.
    let retryLabel: String?
}

// MARK: ErrorState convenience initializers and mutators

extension ErrorState {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(ErrorState.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        hideOnError: Bool?? = nil,
        message: String?? = nil,
        retryAction: Action?? = nil,
        retryLabel: String?? = nil
    ) -> ErrorState {
        return ErrorState(
            hideOnError: hideOnError ?? self.hideOnError,
            message: message ?? self.message,
            retryAction: retryAction ?? self.retryAction,
            retryLabel: retryLabel ?? self.retryLabel
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - Loading
struct Loading: Codable {
    /// Minimum height to reserve during loading (prevents layout shift)
    let minHeightDP: Int?
    /// Which loading skeleton style to use
    let skeleton: Skeleton?

    enum CodingKeys: String, CodingKey {
        case minHeightDP = "minHeightDp"
        case skeleton
    }
}

// MARK: Loading convenience initializers and mutators

extension Loading {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Loading.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        minHeightDP: Int?? = nil,
        skeleton: Skeleton?? = nil
    ) -> Loading {
        return Loading(
            minHeightDP: minHeightDP ?? self.minHeightDP,
            skeleton: skeleton ?? self.skeleton
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Which loading skeleton style to use
enum Skeleton: String, Codable {
    case none = "none"
    case placeholder = "placeholder"
    case shimmer = "shimmer"
    case spinner = "spinner"
}

/// Nested interaction target within a section (e.g., tappable team area inside a scoreboard)
// MARK: - Subsection
struct Subsection: Codable {
    /// Subsection-level accessibility metadata
    let accessibility: AccessibilityProperties?
    let actions: [Action]?
    let id: String
}

// MARK: Subsection convenience initializers and mutators

extension Subsection {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Subsection.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        accessibility: AccessibilityProperties?? = nil,
        actions: [Action]?? = nil,
        id: String? = nil
    ) -> Subsection {
        return Subsection(
            accessibility: accessibility ?? self.accessibility,
            actions: actions ?? self.actions,
            id: id ?? self.id
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Server-driven surface spec applied by the client's SectionRouter to every semantic
/// section — the visual wrapper beneath the section's content. Mirrors the inline-chrome
/// vocabulary on AtomicContainer so semantic sections have schema parity with composed
/// sections. Every client's shared SectionContainer wrapper reads these fields;
/// semantic-section renderers do not set outer padding, margin, corner radius, shadow,
/// border, or background themselves. The sibling `data` field carries content (including the
/// atomic UI tree); `surface` carries the frame that sits beneath it.
// MARK: - SectionSurface
struct SectionSurface: Codable {
    /// Surface background (solid, gradient, or image).
    let background: BackgroundUnion?
    /// Outer stroke applied around the surface.
    let border: Border?
    /// Corner radius: dp/px or layout token, applied to the surface (with overflow clip).
    let cornerRadius: LayoutScalar?
    /// Outer margin (space between the surface and its siblings / screen edge).
    let margin: Spacing?
    /// Inner padding (space between the surface edge and the content it wraps).
    let padding: Spacing?
    /// Drop shadow applied to the surface.
    let shadow: Shadow?
}

// MARK: SectionSurface convenience initializers and mutators

extension SectionSurface {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(SectionSurface.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        background: BackgroundUnion?? = nil,
        border: Border?? = nil,
        cornerRadius: LayoutScalar?? = nil,
        margin: Spacing?? = nil,
        padding: Spacing?? = nil,
        shadow: Shadow?? = nil
    ) -> SectionSurface {
        return SectionSurface(
            background: background ?? self.background,
            border: border ?? self.border,
            cornerRadius: cornerRadius ?? self.cornerRadius,
            margin: margin ?? self.margin,
            padding: padding ?? self.padding,
            shadow: shadow ?? self.shadow
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Outer stroke applied around the surface.
///
/// Outer stroke applied around a container or section.
// MARK: - Border
struct Border: Codable {
    /// Stroke color (hex or token)
    let color: String?
    /// Stroke width in dp/px
    let width: Double?
}

// MARK: Border convenience initializers and mutators

extension Border {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Border.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        color: String?? = nil,
        width: Double?? = nil
    ) -> Border {
        return Border(
            color: color ?? self.color,
            width: width ?? self.width
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}


/// Server-exposed A/B / experiment variants available for this screen. Clients read
/// `options` to render a variant picker (dev UI, QA tooling) and pass the selected id back
/// to the server on subsequent requests. Omit for screens without active experiments.
///
/// Wrapper for the set of A/B variants the server is willing to serve for this screen. Lets
/// clients expose variant selection without hardcoding experiment ids or option vocabularies.
// MARK: - ExperimentVariants
struct ExperimentVariants: Codable {
    /// Stable identifier for the experiment (e.g. `game_detail_variant`). Clients echo this key
    /// back to the server as part of the experiments map on subsequent requests.
    let experimentID: String
    /// Ordered list of variants the client may choose from.
    let options: [ExperimentVariantOption]

    enum CodingKeys: String, CodingKey {
        case experimentID = "experimentId"
        case options
    }
}

// MARK: ExperimentVariants convenience initializers and mutators

extension ExperimentVariants {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(ExperimentVariants.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        experimentID: String? = nil,
        options: [ExperimentVariantOption]? = nil
    ) -> ExperimentVariants {
        return ExperimentVariants(
            experimentID: experimentID ?? self.experimentID,
            options: options ?? self.options
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// One variant within an experiment.
// MARK: - ExperimentVariantOption
struct ExperimentVariantOption: Codable {
    /// Optional longer description shown alongside the label.
    let description: String?
    /// Variant identifier (e.g. `A`, `B`). Opaque to clients.
    let id: String
    /// Human-readable label rendered in variant pickers.
    let label: String
}

// MARK: ExperimentVariantOption convenience initializers and mutators

extension ExperimentVariantOption {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(ExperimentVariantOption.self, from: data)
    }

    init(_ json: String, using encoding: String.Encoding = .utf8) throws {
        guard let data = json.data(using: encoding) else {
            throw NSError(domain: "JSONDecoding", code: 0, userInfo: nil)
        }
        try self.init(data: data)
    }

    init(fromURL url: URL) throws {
        try self.init(data: try Data(contentsOf: url))
    }

    func with(
        description: String?? = nil,
        id: String? = nil,
        label: String? = nil
    ) -> ExperimentVariantOption {
        return ExperimentVariantOption(
            description: description ?? self.description,
            id: id ?? self.id,
            label: label ?? self.label
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - Helper functions for creating encoders and decoders

func newJSONDecoder() -> JSONDecoder {
    let decoder = JSONDecoder()
    if #available(iOS 10.0, OSX 10.12, tvOS 10.0, watchOS 3.0, *) {
        decoder.dateDecodingStrategy = .iso8601
    }
    return decoder
}

func newJSONEncoder() -> JSONEncoder {
    let encoder = JSONEncoder()
    if #available(iOS 10.0, OSX 10.12, tvOS 10.0, watchOS 3.0, *) {
        encoder.dateEncodingStrategy = .iso8601
    }
    return encoder
}

// MARK: - Encode/decode helpers

class JSONNull: Codable, Hashable {

    public static func == (lhs: JSONNull, rhs: JSONNull) -> Bool {
            return true
    }

    public var hashValue: Int {
            return 0
    }

    public func hash(into hasher: inout Hasher) {
            // No-op
    }

    public init() {}

    public required init(from decoder: Decoder) throws {
            let container = try decoder.singleValueContainer()
            if !container.decodeNil() {
                    throw DecodingError.typeMismatch(JSONNull.self, DecodingError.Context(codingPath: decoder.codingPath, debugDescription: "Wrong type for JSONNull"))
            }
    }

    public func encode(to encoder: Encoder) throws {
            var container = encoder.singleValueContainer()
            try container.encodeNil()
    }
}

class JSONCodingKey: CodingKey {
    let key: String

    required init?(intValue: Int) {
            return nil
    }

    required init?(stringValue: String) {
            key = stringValue
    }

    var intValue: Int? {
            return nil
    }

    var stringValue: String {
            return key
    }
}

class JSONAny: Codable {

    let value: Any

    static func decodingError(forCodingPath codingPath: [CodingKey]) -> DecodingError {
            let context = DecodingError.Context(codingPath: codingPath, debugDescription: "Cannot decode JSONAny")
            return DecodingError.typeMismatch(JSONAny.self, context)
    }

    static func encodingError(forValue value: Any, codingPath: [CodingKey]) -> EncodingError {
            let context = EncodingError.Context(codingPath: codingPath, debugDescription: "Cannot encode JSONAny")
            return EncodingError.invalidValue(value, context)
    }

    static func decode(from container: SingleValueDecodingContainer) throws -> Any {
            if let value = try? container.decode(Bool.self) {
                    return value
            }
            if let value = try? container.decode(Int64.self) {
                    return value
            }
            if let value = try? container.decode(Double.self) {
                    return value
            }
            if let value = try? container.decode(String.self) {
                    return value
            }
            if container.decodeNil() {
                    return JSONNull()
            }
            throw decodingError(forCodingPath: container.codingPath)
    }

    static func decode(from container: inout UnkeyedDecodingContainer) throws -> Any {
            if let value = try? container.decode(Bool.self) {
                    return value
            }
            if let value = try? container.decode(Int64.self) {
                    return value
            }
            if let value = try? container.decode(Double.self) {
                    return value
            }
            if let value = try? container.decode(String.self) {
                    return value
            }
            if let value = try? container.decodeNil() {
                    if value {
                            return JSONNull()
                    }
            }
            if var container = try? container.nestedUnkeyedContainer() {
                    return try decodeArray(from: &container)
            }
            if var container = try? container.nestedContainer(keyedBy: JSONCodingKey.self) {
                    return try decodeDictionary(from: &container)
            }
            throw decodingError(forCodingPath: container.codingPath)
    }

    static func decode(from container: inout KeyedDecodingContainer<JSONCodingKey>, forKey key: JSONCodingKey) throws -> Any {
            if let value = try? container.decode(Bool.self, forKey: key) {
                    return value
            }
            if let value = try? container.decode(Int64.self, forKey: key) {
                    return value
            }
            if let value = try? container.decode(Double.self, forKey: key) {
                    return value
            }
            if let value = try? container.decode(String.self, forKey: key) {
                    return value
            }
            if let value = try? container.decodeNil(forKey: key) {
                    if value {
                            return JSONNull()
                    }
            }
            if var container = try? container.nestedUnkeyedContainer(forKey: key) {
                    return try decodeArray(from: &container)
            }
            if var container = try? container.nestedContainer(keyedBy: JSONCodingKey.self, forKey: key) {
                    return try decodeDictionary(from: &container)
            }
            throw decodingError(forCodingPath: container.codingPath)
    }

    static func decodeArray(from container: inout UnkeyedDecodingContainer) throws -> [Any] {
            var arr: [Any] = []
            while !container.isAtEnd {
                    let value = try decode(from: &container)
                    arr.append(value)
            }
            return arr
    }

    static func decodeDictionary(from container: inout KeyedDecodingContainer<JSONCodingKey>) throws -> [String: Any] {
            var dict = [String: Any]()
            for key in container.allKeys {
                    let value = try decode(from: &container, forKey: key)
                    dict[key.stringValue] = value
            }
            return dict
    }

    static func encode(to container: inout UnkeyedEncodingContainer, array: [Any]) throws {
            for value in array {
                    if let value = value as? Bool {
                            try container.encode(value)
                    } else if let value = value as? Int64 {
                            try container.encode(value)
                    } else if let value = value as? Double {
                            try container.encode(value)
                    } else if let value = value as? String {
                            try container.encode(value)
                    } else if value is JSONNull {
                            try container.encodeNil()
                    } else if let value = value as? [Any] {
                            var container = container.nestedUnkeyedContainer()
                            try encode(to: &container, array: value)
                    } else if let value = value as? [String: Any] {
                            var container = container.nestedContainer(keyedBy: JSONCodingKey.self)
                            try encode(to: &container, dictionary: value)
                    } else {
                            throw encodingError(forValue: value, codingPath: container.codingPath)
                    }
            }
    }

    static func encode(to container: inout KeyedEncodingContainer<JSONCodingKey>, dictionary: [String: Any]) throws {
            for (key, value) in dictionary {
                    let key = JSONCodingKey(stringValue: key)!
                    if let value = value as? Bool {
                            try container.encode(value, forKey: key)
                    } else if let value = value as? Int64 {
                            try container.encode(value, forKey: key)
                    } else if let value = value as? Double {
                            try container.encode(value, forKey: key)
                    } else if let value = value as? String {
                            try container.encode(value, forKey: key)
                    } else if value is JSONNull {
                            try container.encodeNil(forKey: key)
                    } else if let value = value as? [Any] {
                            var container = container.nestedUnkeyedContainer(forKey: key)
                            try encode(to: &container, array: value)
                    } else if let value = value as? [String: Any] {
                            var container = container.nestedContainer(keyedBy: JSONCodingKey.self, forKey: key)
                            try encode(to: &container, dictionary: value)
                    } else {
                            throw encodingError(forValue: value, codingPath: container.codingPath)
                    }
            }
    }

    static func encode(to container: inout SingleValueEncodingContainer, value: Any) throws {
            if let value = value as? Bool {
                    try container.encode(value)
            } else if let value = value as? Int64 {
                    try container.encode(value)
            } else if let value = value as? Double {
                    try container.encode(value)
            } else if let value = value as? String {
                    try container.encode(value)
            } else if value is JSONNull {
                    try container.encodeNil()
            } else {
                    throw encodingError(forValue: value, codingPath: container.codingPath)
            }
    }

    public required init(from decoder: Decoder) throws {
            if var arrayContainer = try? decoder.unkeyedContainer() {
                    self.value = try JSONAny.decodeArray(from: &arrayContainer)
            } else if var container = try? decoder.container(keyedBy: JSONCodingKey.self) {
                    self.value = try JSONAny.decodeDictionary(from: &container)
            } else {
                    let container = try decoder.singleValueContainer()
                    self.value = try JSONAny.decode(from: container)
            }
    }

    public func encode(to encoder: Encoder) throws {
            if let arr = self.value as? [Any] {
                    var container = encoder.unkeyedContainer()
                    try JSONAny.encode(to: &container, array: arr)
            } else if let dict = self.value as? [String: Any] {
                    var container = encoder.container(keyedBy: JSONCodingKey.self)
                    try JSONAny.encode(to: &container, dictionary: dict)
            } else {
                    var container = encoder.singleValueContainer()
                    try JSONAny.encode(to: &container, value: self.value)
            }
    }
}

// MARK: - ActionTrigger convenience (codegen post-process)
extension ActionTrigger {
    /// Primary user activation: `onActivate` (preferred) or legacy `onTap`.
    var isPrimaryActivation: Bool {
        self == .onActivate || self == .onTap
    }
}
