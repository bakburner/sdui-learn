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
    /// URI the back button should navigate to.  Clients always show a back button; this field
    /// tells them the target.  Omit for root screens (e.g. scoreboard).
    let parentURI: String?
    let schemaVersion: String
    let sections: [Section]
    let state: [String: JSONAny]?
    let title, traceID: String?

    enum CodingKeys: String, CodingKey {
        case actions
        case analyticsID = "analyticsId"
        case defaultRefreshPolicy, id, navigation
        case parentURI = "parentUri"
        case schemaVersion, sections, state, title
        case traceID = "traceId"
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
        parentURI: String?? = nil,
        schemaVersion: String? = nil,
        sections: [Section]? = nil,
        state: [String: JSONAny]?? = nil,
        title: String?? = nil,
        traceID: String?? = nil
    ) -> SduiModels {
        return SduiModels(
            actions: actions ?? self.actions,
            analyticsID: analyticsID ?? self.analyticsID,
            defaultRefreshPolicy: defaultRefreshPolicy ?? self.defaultRefreshPolicy,
            id: id ?? self.id,
            navigation: navigation ?? self.navigation,
            parentURI: parentURI ?? self.parentURI,
            schemaVersion: schemaVersion ?? self.schemaVersion,
            sections: sections ?? self.sections,
            state: state ?? self.state,
            title: title ?? self.title,
            traceID: traceID ?? self.traceID
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

enum ActionTrigger: String, Codable {
    case onBlur = "onBlur"
    case onFocus = "onFocus"
    case onLongPress = "onLongPress"
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
    let type: RefreshType
    /// For poll/sse type: URL to poll or connect to. If omitted, polls the SDUI endpoint.
    let url: String?

    enum CodingKeys: String, CodingKey {
        case channel, dataPath
        case intervalMS = "intervalMs"
        case type, url
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
        type: RefreshType? = nil,
        url: String?? = nil
    ) -> RefreshPolicy {
        return RefreshPolicy(
            channel: channel ?? self.channel,
            dataPath: dataPath ?? self.dataPath,
            intervalMS: intervalMS ?? self.intervalMS,
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

/// Root node of the atomic element tree — the rendering instructions
///
/// Atomic UI primitive — server-composed building block for the atomic rendering layer
// MARK: - AtomicElement
class AtomicElement: Codable {
    let actions: [Action]?
    let alignment: Alignment?
    let alt: String?
    let aspectRatio: Double?
    let backgroundColor: String?
    /// Gradient background for Container elements
    let backgroundGradient: BackgroundGradient?
    /// Responsive breakpoint in dp/px. For Container: below this screen width, direction flips
    /// from row to column. Enables responsive layouts without client logic.
    let breakpoint: Int?
    let buttonVariant: ButtonVariant?
    /// Typography variant for data cells
    let cellVariant: TextVariant?
    let children: [AtomicElement]?
    let color: String?
    /// DisplayGrid column definitions — display-only, non-interactive, server-ordered
    let columns: [Column]?
    let condition, content: String?
    /// Corner radius in dp/px. Applied to Container (with overflow clip) and Image elements.
    let cornerRadius: Int?
    let crossAlignment: CrossAlignment?
    let direction: UIDirection?
    let disabled: Bool?
    let falseChild: AtomicElement?
    let fit: ImageFit?
    /// Flex grow factor. When set on a child of a Container, the child claims proportional space
    /// along the main axis (like CSS flex or Compose weight). Default 0 (size to content).
    let flex: Double?
    let gap: Int?
    /// Typography variant for header cells
    let headerVariant: TextVariant?
    let height: Int?
    let icon, id, label: String?
    let maxLines: Int?
    let orientation: Orientation?
    let padding: Spacing?
    let paging: Bool?
    let placeholder: String?
    /// DisplayGrid row data — each object maps column keys to pre-formatted display values
    let rows: [[String: String]]?
    /// Full section object to render via SectionRouter. Only used when type is SectionSlot.
    let section: Section?
    let size: Int?
    let snapAlignment: Align?
    let src: String?
    /// Alternate row background for readability
    let striped: Bool?
    let thickness: Int?
    let trueChild: AtomicElement?
    let type: UIType
    let variant: TextVariant?
    let weight: TextWeight?
    let width: Int?

    init(actions: [Action]?, alignment: Alignment?, alt: String?, aspectRatio: Double?, backgroundColor: String?, backgroundGradient: BackgroundGradient?, breakpoint: Int?, buttonVariant: ButtonVariant?, cellVariant: TextVariant?, children: [AtomicElement]?, color: String?, columns: [Column]?, condition: String?, content: String?, cornerRadius: Int?, crossAlignment: CrossAlignment?, direction: UIDirection?, disabled: Bool?, falseChild: AtomicElement?, fit: ImageFit?, flex: Double?, gap: Int?, headerVariant: TextVariant?, height: Int?, icon: String?, id: String?, label: String?, maxLines: Int?, orientation: Orientation?, padding: Spacing?, paging: Bool?, placeholder: String?, rows: [[String: String]]?, section: Section?, size: Int?, snapAlignment: Align?, src: String?, striped: Bool?, thickness: Int?, trueChild: AtomicElement?, type: UIType, variant: TextVariant?, weight: TextWeight?, width: Int?) {
        self.actions = actions
        self.alignment = alignment
        self.alt = alt
        self.aspectRatio = aspectRatio
        self.backgroundColor = backgroundColor
        self.backgroundGradient = backgroundGradient
        self.breakpoint = breakpoint
        self.buttonVariant = buttonVariant
        self.cellVariant = cellVariant
        self.children = children
        self.color = color
        self.columns = columns
        self.condition = condition
        self.content = content
        self.cornerRadius = cornerRadius
        self.crossAlignment = crossAlignment
        self.direction = direction
        self.disabled = disabled
        self.falseChild = falseChild
        self.fit = fit
        self.flex = flex
        self.gap = gap
        self.headerVariant = headerVariant
        self.height = height
        self.icon = icon
        self.id = id
        self.label = label
        self.maxLines = maxLines
        self.orientation = orientation
        self.padding = padding
        self.paging = paging
        self.placeholder = placeholder
        self.rows = rows
        self.section = section
        self.size = size
        self.snapAlignment = snapAlignment
        self.src = src
        self.striped = striped
        self.thickness = thickness
        self.trueChild = trueChild
        self.type = type
        self.variant = variant
        self.weight = weight
        self.width = width
    }
}

// MARK: AtomicElement convenience initializers and mutators

extension AtomicElement {
    convenience init(data: Data) throws {
        let me = try newJSONDecoder().decode(AtomicElement.self, from: data)
        self.init(actions: me.actions, alignment: me.alignment, alt: me.alt, aspectRatio: me.aspectRatio, backgroundColor: me.backgroundColor, backgroundGradient: me.backgroundGradient, breakpoint: me.breakpoint, buttonVariant: me.buttonVariant, cellVariant: me.cellVariant, children: me.children, color: me.color, columns: me.columns, condition: me.condition, content: me.content, cornerRadius: me.cornerRadius, crossAlignment: me.crossAlignment, direction: me.direction, disabled: me.disabled, falseChild: me.falseChild, fit: me.fit, flex: me.flex, gap: me.gap, headerVariant: me.headerVariant, height: me.height, icon: me.icon, id: me.id, label: me.label, maxLines: me.maxLines, orientation: me.orientation, padding: me.padding, paging: me.paging, placeholder: me.placeholder, rows: me.rows, section: me.section, size: me.size, snapAlignment: me.snapAlignment, src: me.src, striped: me.striped, thickness: me.thickness, trueChild: me.trueChild, type: me.type, variant: me.variant, weight: me.weight, width: me.width)
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
        actions: [Action]?? = nil,
        alignment: Alignment?? = nil,
        alt: String?? = nil,
        aspectRatio: Double?? = nil,
        backgroundColor: String?? = nil,
        backgroundGradient: BackgroundGradient?? = nil,
        breakpoint: Int?? = nil,
        buttonVariant: ButtonVariant?? = nil,
        cellVariant: TextVariant?? = nil,
        children: [AtomicElement]?? = nil,
        color: String?? = nil,
        columns: [Column]?? = nil,
        condition: String?? = nil,
        content: String?? = nil,
        cornerRadius: Int?? = nil,
        crossAlignment: CrossAlignment?? = nil,
        direction: UIDirection?? = nil,
        disabled: Bool?? = nil,
        falseChild: AtomicElement?? = nil,
        fit: ImageFit?? = nil,
        flex: Double?? = nil,
        gap: Int?? = nil,
        headerVariant: TextVariant?? = nil,
        height: Int?? = nil,
        icon: String?? = nil,
        id: String?? = nil,
        label: String?? = nil,
        maxLines: Int?? = nil,
        orientation: Orientation?? = nil,
        padding: Spacing?? = nil,
        paging: Bool?? = nil,
        placeholder: String?? = nil,
        rows: [[String: String]]?? = nil,
        section: Section?? = nil,
        size: Int?? = nil,
        snapAlignment: Align?? = nil,
        src: String?? = nil,
        striped: Bool?? = nil,
        thickness: Int?? = nil,
        trueChild: AtomicElement?? = nil,
        type: UIType? = nil,
        variant: TextVariant?? = nil,
        weight: TextWeight?? = nil,
        width: Int?? = nil
    ) -> AtomicElement {
        return AtomicElement(
            actions: actions ?? self.actions,
            alignment: alignment ?? self.alignment,
            alt: alt ?? self.alt,
            aspectRatio: aspectRatio ?? self.aspectRatio,
            backgroundColor: backgroundColor ?? self.backgroundColor,
            backgroundGradient: backgroundGradient ?? self.backgroundGradient,
            breakpoint: breakpoint ?? self.breakpoint,
            buttonVariant: buttonVariant ?? self.buttonVariant,
            cellVariant: cellVariant ?? self.cellVariant,
            children: children ?? self.children,
            color: color ?? self.color,
            columns: columns ?? self.columns,
            condition: condition ?? self.condition,
            content: content ?? self.content,
            cornerRadius: cornerRadius ?? self.cornerRadius,
            crossAlignment: crossAlignment ?? self.crossAlignment,
            direction: direction ?? self.direction,
            disabled: disabled ?? self.disabled,
            falseChild: falseChild ?? self.falseChild,
            fit: fit ?? self.fit,
            flex: flex ?? self.flex,
            gap: gap ?? self.gap,
            headerVariant: headerVariant ?? self.headerVariant,
            height: height ?? self.height,
            icon: icon ?? self.icon,
            id: id ?? self.id,
            label: label ?? self.label,
            maxLines: maxLines ?? self.maxLines,
            orientation: orientation ?? self.orientation,
            padding: padding ?? self.padding,
            paging: paging ?? self.paging,
            placeholder: placeholder ?? self.placeholder,
            rows: rows ?? self.rows,
            section: section ?? self.section,
            size: size ?? self.size,
            snapAlignment: snapAlignment ?? self.snapAlignment,
            src: src ?? self.src,
            striped: striped ?? self.striped,
            thickness: thickness ?? self.thickness,
            trueChild: trueChild ?? self.trueChild,
            type: type ?? self.type,
            variant: variant ?? self.variant,
            weight: weight ?? self.weight,
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
/// Inline subscription upsell banner with headline, body copy, and CTA
///
/// Full-screen subscription upsell hero with multi-tier pricing and feature list
///
/// Data payload for AtomicComposite sections — ui contains rendering instructions, content
/// carries domain data
// MARK: - DataClass
struct DataClass: Codable {
    let defaultTab, stateKey: String?
    let tabContents: [String: [Section]]?
    let tabs: [TabData]?
    let actions: [Action]?
    let awayTeam: TeamData?
    /// Background image URL for featured variant hero card
    let backgroundImageURL: String?
    /// Badge/chip label, e.g. 'LIVE', 'FEATURED'
    let badgeText: String?
    /// Game clock string (e.g. 'PT05M32.00S' or '5:32')
    let gameClock: String?
    let gameID: String?
    let gameLeaders: GameLeadersData?
    let gameStatus: Int?
    let gameStatusText, gameTimeEt: String?
    let homeTeam: TeamData?
    /// Current game period (quarter number)
    let period: Int?
    /// Visual treatment: 'standard' for compact feed cards, 'featured' for hero-sized cards with
    /// gradient/background, 'scoreboard' for compact scoreboard rows
    let variant: Variant?
    /// Secondary label shown above the matchup (e.g. team name, 'Recommended')
    let visualLabel: String?
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
    let ctaAction: Action?
    let ctaLabel, logoURL: String?
    /// Optional pricing tier highlights
    let tiers: [SubscriptionTier]?
    /// Bullet-point feature list
    let features: [String]?
    /// Optional domain data (strings, URLs, flags) to populate the ui tree. Reserved for future
    /// data-binding support.
    let content: [String: JSONAny]?
    /// Root node of the atomic element tree — the rendering instructions
    let ui: AtomicElement?

    enum CodingKeys: String, CodingKey {
        case defaultTab, stateKey, tabContents, tabs, actions, awayTeam
        case backgroundImageURL = "backgroundImageUrl"
        case badgeText, gameClock
        case gameID = "gameId"
        case gameLeaders, gameStatus, gameStatusText, gameTimeEt, homeTeam, period, variant, visualLabel, columns, emptyMessage, players, sortDirectionStateKey, sortStateKey, teamColor
        case teamLogoURL = "teamLogoUrl"
        case teamName, teamTotals, teamTricode, fields, layout, submitAction, submitLabel, adUnitPath, collapseOnEmpty, label, provider
        case refreshIntervalSEC = "refreshIntervalSec"
        case sizes, targeting, page, pageSize, sortColumn, sortDirection, subtitle, title, totalRows, ctaAction, ctaLabel
        case logoURL = "logoUrl"
        case tiers, features, content, ui
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
        actions: [Action]?? = nil,
        awayTeam: TeamData?? = nil,
        backgroundImageURL: String?? = nil,
        badgeText: String?? = nil,
        gameClock: String?? = nil,
        gameID: String?? = nil,
        gameLeaders: GameLeadersData?? = nil,
        gameStatus: Int?? = nil,
        gameStatusText: String?? = nil,
        gameTimeEt: String?? = nil,
        homeTeam: TeamData?? = nil,
        period: Int?? = nil,
        variant: Variant?? = nil,
        visualLabel: String?? = nil,
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
        ctaLabel: String?? = nil,
        logoURL: String?? = nil,
        tiers: [SubscriptionTier]?? = nil,
        features: [String]?? = nil,
        content: [String: JSONAny]?? = nil,
        ui: AtomicElement?? = nil
    ) -> DataClass {
        return DataClass(
            defaultTab: defaultTab ?? self.defaultTab,
            stateKey: stateKey ?? self.stateKey,
            tabContents: tabContents ?? self.tabContents,
            tabs: tabs ?? self.tabs,
            actions: actions ?? self.actions,
            awayTeam: awayTeam ?? self.awayTeam,
            backgroundImageURL: backgroundImageURL ?? self.backgroundImageURL,
            badgeText: badgeText ?? self.badgeText,
            gameClock: gameClock ?? self.gameClock,
            gameID: gameID ?? self.gameID,
            gameLeaders: gameLeaders ?? self.gameLeaders,
            gameStatus: gameStatus ?? self.gameStatus,
            gameStatusText: gameStatusText ?? self.gameStatusText,
            gameTimeEt: gameTimeEt ?? self.gameTimeEt,
            homeTeam: homeTeam ?? self.homeTeam,
            period: period ?? self.period,
            variant: variant ?? self.variant,
            visualLabel: visualLabel ?? self.visualLabel,
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
            ctaLabel: ctaLabel ?? self.ctaLabel,
            logoURL: logoURL ?? self.logoURL,
            tiers: tiers ?? self.tiers,
            features: features ?? self.features,
            content: content ?? self.content,
            ui: ui ?? self.ui
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
    /// Section-level interaction actions
    let actions: [Action]?
    let analyticsID, backgroundColor: String?
    /// Section-specific data payload
    let data: DataClass?
    let dataBindings: DataBinding?
    let id: String
    let layoutHints: SectionLayoutHints?
    let padding: Spacing?
    let refreshPolicy: RefreshPolicy?
    let sectionStates: SectionStates?
    /// Nested interaction targets within the section
    let subsections: [Subsection]?
    let type: SectionType

    enum CodingKeys: String, CodingKey {
        case actions
        case analyticsID = "analyticsId"
        case backgroundColor, data, dataBindings, id, layoutHints, padding, refreshPolicy, sectionStates, subsections, type
    }

    init(actions: [Action]?, analyticsID: String?, backgroundColor: String?, data: DataClass?, dataBindings: DataBinding?, id: String, layoutHints: SectionLayoutHints?, padding: Spacing?, refreshPolicy: RefreshPolicy?, sectionStates: SectionStates?, subsections: [Subsection]?, type: SectionType) {
        self.actions = actions
        self.analyticsID = analyticsID
        self.backgroundColor = backgroundColor
        self.data = data
        self.dataBindings = dataBindings
        self.id = id
        self.layoutHints = layoutHints
        self.padding = padding
        self.refreshPolicy = refreshPolicy
        self.sectionStates = sectionStates
        self.subsections = subsections
        self.type = type
    }
}

// MARK: Section convenience initializers and mutators

extension Section {
    convenience init(data: Data) throws {
        let me = try newJSONDecoder().decode(Section.self, from: data)
        self.init(actions: me.actions, analyticsID: me.analyticsID, backgroundColor: me.backgroundColor, data: me.data, dataBindings: me.dataBindings, id: me.id, layoutHints: me.layoutHints, padding: me.padding, refreshPolicy: me.refreshPolicy, sectionStates: me.sectionStates, subsections: me.subsections, type: me.type)
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
        actions: [Action]?? = nil,
        analyticsID: String?? = nil,
        backgroundColor: String?? = nil,
        data: DataClass?? = nil,
        dataBindings: DataBinding?? = nil,
        id: String? = nil,
        layoutHints: SectionLayoutHints?? = nil,
        padding: Spacing?? = nil,
        refreshPolicy: RefreshPolicy?? = nil,
        sectionStates: SectionStates?? = nil,
        subsections: [Subsection]?? = nil,
        type: SectionType? = nil
    ) -> Section {
        return Section(
            actions: actions ?? self.actions,
            analyticsID: analyticsID ?? self.analyticsID,
            backgroundColor: backgroundColor ?? self.backgroundColor,
            data: data ?? self.data,
            dataBindings: dataBindings ?? self.dataBindings,
            id: id ?? self.id,
            layoutHints: layoutHints ?? self.layoutHints,
            padding: padding ?? self.padding,
            refreshPolicy: refreshPolicy ?? self.refreshPolicy,
            sectionStates: sectionStates ?? self.sectionStates,
            subsections: subsections ?? self.subsections,
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

enum Alignment: String, Codable {
    case center = "center"
    case end = "end"
    case spaceAround = "spaceAround"
    case spaceBetween = "spaceBetween"
    case spaceEvenly = "spaceEvenly"
    case start = "start"
}

/// Gradient background for Container elements
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

enum Direction: String, Codable {
    case diagonal = "diagonal"
    case horizontal = "horizontal"
    case vertical = "vertical"
}

enum ButtonVariant: String, Codable {
    case primary = "primary"
    case secondary = "secondary"
    case tertiary = "tertiary"
    case text = "text"
}

/// Typography variant for data cells
///
/// Typography variant for header cells
enum TextVariant: String, Codable {
    case body = "body"
    case bodySmall = "bodySmall"
    case caption = "caption"
    case heading1 = "heading1"
    case heading2 = "heading2"
    case heading3 = "heading3"
    case label = "label"
    case score = "score"
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

enum CrossAlignment: String, Codable {
    case center = "center"
    case end = "end"
    case start = "start"
    case stretch = "stretch"
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

enum Orientation: String, Codable {
    case horizontal = "horizontal"
    case vertical = "vertical"
}

// MARK: - Spacing
struct Spacing: Codable {
    let bottom, end, start, top: Int?
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
        bottom: Int?? = nil,
        end: Int?? = nil,
        start: Int?? = nil,
        top: Int?? = nil
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

enum UIType: String, Codable {
    case button = "Button"
    case conditional = "Conditional"
    case container = "Container"
    case displayGrid = "DisplayGrid"
    case divider = "Divider"
    case image = "Image"
    case scrollContainer = "ScrollContainer"
    case sectionSlot = "SectionSlot"
    case spacer = "Spacer"
    case text = "Text"
}

enum TextWeight: String, Codable {
    case bold = "bold"
    case medium = "medium"
    case regular = "regular"
    case semibold = "semibold"
}

// MARK: - TeamData
struct TeamData: Codable {
    let logoURL: String?
    let score: Int
    let teamCity: String
    let teamID: Int
    let teamName, teamTricode: String

    enum CodingKeys: String, CodingKey {
        case logoURL = "logoUrl"
        case score, teamCity
        case teamID = "teamId"
        case teamName, teamTricode
    }
}

// MARK: TeamData convenience initializers and mutators

extension TeamData {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(TeamData.self, from: data)
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
        logoURL: String?? = nil,
        score: Int? = nil,
        teamCity: String? = nil,
        teamID: Int? = nil,
        teamName: String? = nil,
        teamTricode: String? = nil
    ) -> TeamData {
        return TeamData(
            logoURL: logoURL ?? self.logoURL,
            score: score ?? self.score,
            teamCity: teamCity ?? self.teamCity,
            teamID: teamID ?? self.teamID,
            teamName: teamName ?? self.teamName,
            teamTricode: teamTricode ?? self.teamTricode
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
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

    enum CodingKeys: String, CodingKey {
        case disabled
        case fieldID = "fieldId"
        case fieldType, label, options, placeholder
        case formFieldRequired = "required"
        case stateKey, validationMessage, validationPattern
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
        validationPattern: String?? = nil
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
            validationPattern: validationPattern ?? self.validationPattern
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

// MARK: - GameLeadersData
struct GameLeadersData: Codable {
    let awayLeader, homeLeader: GameLeaderData?
}

// MARK: GameLeadersData convenience initializers and mutators

extension GameLeadersData {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(GameLeadersData.self, from: data)
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
        awayLeader: GameLeaderData?? = nil,
        homeLeader: GameLeaderData?? = nil
    ) -> GameLeadersData {
        return GameLeadersData(
            awayLeader: awayLeader ?? self.awayLeader,
            homeLeader: homeLeader ?? self.homeLeader
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - GameLeaderData
struct GameLeaderData: Codable {
    let assists: Int?
    let name: String?
    let points, rebounds: Int?
}

// MARK: GameLeaderData convenience initializers and mutators

extension GameLeaderData {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(GameLeaderData.self, from: data)
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
        assists: Int?? = nil,
        name: String?? = nil,
        points: Int?? = nil,
        rebounds: Int?? = nil
    ) -> GameLeaderData {
        return GameLeaderData(
            assists: assists ?? self.assists,
            name: name ?? self.name,
            points: points ?? self.points,
            rebounds: rebounds ?? self.rebounds
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

/// Layout hint for field arrangement
enum Layout: String, Codable {
    case grid = "grid"
    case horizontal = "horizontal"
    case vertical = "vertical"
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

/// Visual treatment: 'standard' for compact feed cards, 'featured' for hero-sized cards with
/// gradient/background, 'scoreboard' for compact scoreboard rows
enum Variant: String, Codable {
    case featured = "featured"
    case scoreboard = "scoreboard"
    case standard = "standard"
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
        targetPath: String? = nil
    ) -> DataBindingPath {
        return DataBindingPath(
            sourcePath: sourcePath ?? self.sourcePath,
            targetPath: targetPath ?? self.targetPath
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
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
    let error: Error?
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
        error: Error?? = nil,
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

// MARK: - Error
struct Error: Codable {
    /// If true, collapse the section entirely on error instead of showing error UI
    let hideOnError: Bool?
    /// Error message to display (e.g., 'Unable to load scores')
    let message: String?
    /// Optional action to trigger on retry tap (typically a refresh action)
    let retryAction: Action?
}

// MARK: Error convenience initializers and mutators

extension Error {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Error.self, from: data)
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
        retryAction: Action?? = nil
    ) -> Error {
        return Error(
            hideOnError: hideOnError ?? self.hideOnError,
            message: message ?? self.message,
            retryAction: retryAction ?? self.retryAction
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
        actions: [Action]?? = nil,
        id: String? = nil
    ) -> Subsection {
        return Subsection(
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

enum SectionType: String, Codable {
    case adSlot = "AdSlot"
    case atomicComposite = "AtomicComposite"
    case boxscoreTable = "BoxscoreTable"
    case form = "Form"
    case gamePanel = "GamePanel"
    case seasonLeadersTable = "SeasonLeadersTable"
    case subscribeBanner = "SubscribeBanner"
    case subscribeHero = "SubscribeHero"
    case tabGroup = "TabGroup"
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
