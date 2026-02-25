// This file was generated from JSON Schema using quicktype, do not modify it directly.
// To parse the JSON, add this file to your project and do:
//
//   let sduiModels = try SduiModels(json)

import Foundation

/// Server-Driven UI schema for NBA Game Detail screens
// MARK: - SduiModels
struct SduiModels: Codable {
    let analyticsID: String?
    let defaultRefreshPolicy: RefreshPolicy?
    let id: String
    let navigation: Navigation?
    let schemaVersion: String
    let sections: [Section]
    let state: [String: JSONAny]?
    let title, traceID: String?

    enum CodingKeys: String, CodingKey {
        case analyticsID = "analyticsId"
        case defaultRefreshPolicy, id, navigation, schemaVersion, sections, state, title
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
        analyticsID: String?? = nil,
        defaultRefreshPolicy: RefreshPolicy?? = nil,
        id: String? = nil,
        navigation: Navigation?? = nil,
        schemaVersion: String? = nil,
        sections: [Section]? = nil,
        state: [String: JSONAny]?? = nil,
        title: String?? = nil,
        traceID: String?? = nil
    ) -> SduiModels {
        return SduiModels(
            analyticsID: analyticsID ?? self.analyticsID,
            defaultRefreshPolicy: defaultRefreshPolicy ?? self.defaultRefreshPolicy,
            id: id ?? self.id,
            navigation: navigation ?? self.navigation,
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

/// Section-specific data payload
///
/// Container for a titled list of stat lines
///
/// Horizontal scrolling strip of content cards
///
/// Tabbed navigation with dynamic content sections per tab
///
/// Promotional banner with optional image and call-to-action
///
/// Responsive row layout that places children side-by-side above a breakpoint width, and
/// stacks vertically below it
// MARK: - DataClass
struct DataClass: Codable {
    let awayTeam: TeamData?
    let clock: String?
    let gameStatus: Int?
    let gameStatusText: String?
    let homeTeam: TeamData?
    let period: Int?
    let stats: [StatLineData]?
    let title: String?
    let action: Action?
    let contentType: ContentType?
    let duration, headline, id, subhead: String?
    let thumbnailURL: String?
    let cards: [ContentCardData]?
    let defaultTab, stateKey: String?
    let tabContents: [String: [Section]]?
    let tabs: [TabData]?
    let actions: [Action]?
    let description, imageURL, gameID: String?
    let gameLeaders: GameLeadersData?
    let gameTimeEt: String?
    /// Screen width (dp) below which children stack vertically
    let breakpoint: Int?
    /// Child sections rendered in a row (or column when collapsed)
    let children: [Section]?
    /// Gap between children in dp/px
    let spacing: Int?

    enum CodingKeys: String, CodingKey {
        case awayTeam, clock, gameStatus, gameStatusText, homeTeam, period, stats, title, action, contentType, duration, headline, id, subhead
        case thumbnailURL = "thumbnailUrl"
        case cards, defaultTab, stateKey, tabContents, tabs, actions, description
        case imageURL = "imageUrl"
        case gameID = "gameId"
        case gameLeaders, gameTimeEt, breakpoint, children, spacing
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
        awayTeam: TeamData?? = nil,
        clock: String?? = nil,
        gameStatus: Int?? = nil,
        gameStatusText: String?? = nil,
        homeTeam: TeamData?? = nil,
        period: Int?? = nil,
        stats: [StatLineData]?? = nil,
        title: String?? = nil,
        action: Action?? = nil,
        contentType: ContentType?? = nil,
        duration: String?? = nil,
        headline: String?? = nil,
        id: String?? = nil,
        subhead: String?? = nil,
        thumbnailURL: String?? = nil,
        cards: [ContentCardData]?? = nil,
        defaultTab: String?? = nil,
        stateKey: String?? = nil,
        tabContents: [String: [Section]]?? = nil,
        tabs: [TabData]?? = nil,
        actions: [Action]?? = nil,
        description: String?? = nil,
        imageURL: String?? = nil,
        gameID: String?? = nil,
        gameLeaders: GameLeadersData?? = nil,
        gameTimeEt: String?? = nil,
        breakpoint: Int?? = nil,
        children: [Section]?? = nil,
        spacing: Int?? = nil
    ) -> DataClass {
        return DataClass(
            awayTeam: awayTeam ?? self.awayTeam,
            clock: clock ?? self.clock,
            gameStatus: gameStatus ?? self.gameStatus,
            gameStatusText: gameStatusText ?? self.gameStatusText,
            homeTeam: homeTeam ?? self.homeTeam,
            period: period ?? self.period,
            stats: stats ?? self.stats,
            title: title ?? self.title,
            action: action ?? self.action,
            contentType: contentType ?? self.contentType,
            duration: duration ?? self.duration,
            headline: headline ?? self.headline,
            id: id ?? self.id,
            subhead: subhead ?? self.subhead,
            thumbnailURL: thumbnailURL ?? self.thumbnailURL,
            cards: cards ?? self.cards,
            defaultTab: defaultTab ?? self.defaultTab,
            stateKey: stateKey ?? self.stateKey,
            tabContents: tabContents ?? self.tabContents,
            tabs: tabs ?? self.tabs,
            actions: actions ?? self.actions,
            description: description ?? self.description,
            imageURL: imageURL ?? self.imageURL,
            gameID: gameID ?? self.gameID,
            gameLeaders: gameLeaders ?? self.gameLeaders,
            gameTimeEt: gameTimeEt ?? self.gameTimeEt,
            breakpoint: breakpoint ?? self.breakpoint,
            children: children ?? self.children,
            spacing: spacing ?? self.spacing
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

// MARK: - Section
struct Section: Codable {
    /// Section-level interaction actions
    let actions: [Action]?
    let analyticsID, backgroundColor: String?
    /// Section-specific data payload
    let data: DataClass?
    let dataBindings: DataBinding?
    let id: String
    let padding: Spacing?
    let refreshPolicy: RefreshPolicy?
    /// Nested interaction targets within the section
    let subsections: [Subsection]?
    let type: TypeEnum

    enum CodingKeys: String, CodingKey {
        case actions
        case analyticsID = "analyticsId"
        case backgroundColor, data, dataBindings, id, padding, refreshPolicy, subsections, type
    }
}

// MARK: Section convenience initializers and mutators

extension Section {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(Section.self, from: data)
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
        backgroundColor: String?? = nil,
        data: DataClass?? = nil,
        dataBindings: DataBinding?? = nil,
        id: String? = nil,
        padding: Spacing?? = nil,
        refreshPolicy: RefreshPolicy?? = nil,
        subsections: [Subsection]?? = nil,
        type: TypeEnum? = nil
    ) -> Section {
        return Section(
            actions: actions ?? self.actions,
            analyticsID: analyticsID ?? self.analyticsID,
            backgroundColor: backgroundColor ?? self.backgroundColor,
            data: data ?? self.data,
            dataBindings: dataBindings ?? self.dataBindings,
            id: id ?? self.id,
            padding: padding ?? self.padding,
            refreshPolicy: refreshPolicy ?? self.refreshPolicy,
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

// MARK: - Action
struct Action: Codable {
    /// For analytics actions: event name
    let eventName: String?
    /// For analytics actions: event parameters
    let eventParams: [String: JSONAny]?
    /// For navigate actions: web fallback if deeplink fails
    let fallbackURL: String?
    /// For refresh actions: specific section to refresh (null = full screen)
    let sectionID: String?
    /// For mutate actions: state key to update
    let stateKey: String?
    /// For mutate actions: new value for the state key
    let stateValue: JSONAny?
    /// For navigate actions: deeplink URI
    let targetURI: String?
    let trigger: ActionTrigger
    let type: ActionType

    enum CodingKeys: String, CodingKey {
        case eventName, eventParams
        case fallbackURL = "fallbackUrl"
        case sectionID = "sectionId"
        case stateKey, stateValue
        case targetURI = "targetUri"
        case trigger, type
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
        eventName: String?? = nil,
        eventParams: [String: JSONAny]?? = nil,
        fallbackURL: String?? = nil,
        sectionID: String?? = nil,
        stateKey: String?? = nil,
        stateValue: JSONAny?? = nil,
        targetURI: String?? = nil,
        trigger: ActionTrigger? = nil,
        type: ActionType? = nil
    ) -> Action {
        return Action(
            eventName: eventName ?? self.eventName,
            eventParams: eventParams ?? self.eventParams,
            fallbackURL: fallbackURL ?? self.fallbackURL,
            sectionID: sectionID ?? self.sectionID,
            stateKey: stateKey ?? self.stateKey,
            stateValue: stateValue ?? self.stateValue,
            targetURI: targetURI ?? self.targetURI,
            trigger: trigger ?? self.trigger,
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

enum ActionTrigger: String, Codable {
    case onLongPress = "onLongPress"
    case onSwipe = "onSwipe"
    case onTap = "onTap"
    case onVisible = "onVisible"
}

enum ActionType: String, Codable {
    case analytics = "analytics"
    case dismiss = "dismiss"
    case mutate = "mutate"
    case navigate = "navigate"
    case refresh = "refresh"
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

// MARK: - ContentCardData
struct ContentCardData: Codable {
    let action: Action?
    let contentType: ContentType?
    let duration: String?
    let headline, id: String
    let subhead, thumbnailURL: String?

    enum CodingKeys: String, CodingKey {
        case action, contentType, duration, headline, id, subhead
        case thumbnailURL = "thumbnailUrl"
    }
}

// MARK: ContentCardData convenience initializers and mutators

extension ContentCardData {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(ContentCardData.self, from: data)
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
        action: Action?? = nil,
        contentType: ContentType?? = nil,
        duration: String?? = nil,
        headline: String? = nil,
        id: String? = nil,
        subhead: String?? = nil,
        thumbnailURL: String?? = nil
    ) -> ContentCardData {
        return ContentCardData(
            action: action ?? self.action,
            contentType: contentType ?? self.contentType,
            duration: duration ?? self.duration,
            headline: headline ?? self.headline,
            id: id ?? self.id,
            subhead: subhead ?? self.subhead,
            thumbnailURL: thumbnailURL ?? self.thumbnailURL
        )
    }

    func jsonData() throws -> Data {
        return try newJSONEncoder().encode(self)
    }

    func jsonString(encoding: String.Encoding = .utf8) throws -> String? {
        return String(data: try self.jsonData(), encoding: encoding)
    }
}

enum ContentType: String, Codable {
    case article = "article"
    case gallery = "gallery"
    case video = "video"
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

// MARK: - StatLineData
struct StatLineData: Codable {
    let playerID: Int
    let playerImageURL: String?
    let playerName, statCategory: String
    let statLabel: String?
    let statValue: String
    let teamTricode: String?

    enum CodingKeys: String, CodingKey {
        case playerID = "playerId"
        case playerImageURL = "playerImageUrl"
        case playerName, statCategory, statLabel, statValue, teamTricode
    }
}

// MARK: StatLineData convenience initializers and mutators

extension StatLineData {
    init(data: Data) throws {
        self = try newJSONDecoder().decode(StatLineData.self, from: data)
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
        playerID: Int? = nil,
        playerImageURL: String?? = nil,
        playerName: String? = nil,
        statCategory: String? = nil,
        statLabel: String?? = nil,
        statValue: String? = nil,
        teamTricode: String?? = nil
    ) -> StatLineData {
        return StatLineData(
            playerID: playerID ?? self.playerID,
            playerImageURL: playerImageURL ?? self.playerImageURL,
            playerName: playerName ?? self.playerName,
            statCategory: statCategory ?? self.statCategory,
            statLabel: statLabel ?? self.statLabel,
            statValue: statValue ?? self.statValue,
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

enum TypeEnum: String, Codable {
    case contentCard = "ContentCard"
    case contentRail = "ContentRail"
    case gameCard = "GameCard"
    case promoBanner = "PromoBanner"
    case row = "Row"
    case scoreboardHeader = "ScoreboardHeader"
    case statLine = "StatLine"
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
