import Foundation
import Observation
import SwiftUI
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "SduiScreenViewModel")

/// Owns the full lifecycle of a single SDUI screen. Mirrors Android's
/// `SduiScreenViewModel` (one-to-one field mapping; see
/// `android/sdui-core/src/main/java/com/nba/sdui/core/screen/SduiScreenViewModel.kt`).
///
/// Responsibilities:
/// - Fetch screen via ``SduiRepository`` (one generic fetch path).
/// - Own ``ScreenState`` (mutable state) and ``SectionStalenessTracker``.
/// - Subscribe SSE-backed sections through ``AblyChannelManager`` and apply
///   opaque messages to section data via ``DataBindingApplier``.
/// - Drive per-section polling through ``PollingDriver`` with exponential
///   backoff and `pauseWhenOffScreen` gating.
/// - Wire ``ActionDispatcher`` so actions mutate the VM's refresh hooks.
/// - React to `ScenePhase` transitions: suspend polling + Ably on
///   `.background`, resume on `.active`.
///
/// `@Observable` + `@MainActor` so SwiftUI views observe property changes
/// without manual wiring.
@MainActor
@Observable
public final class SduiScreenViewModel {

    public enum LoadState: Sendable, Equatable {
        case idle
        case loading
        case loaded
        case failed(message: String)
    }

    // MARK: - Observable surface

    public private(set) var loadState: LoadState = .idle
    public private(set) var title: String?
    public private(set) var navigationItems: Int = 0

    /// Most recent full-screen response. Views read `sections` from this.
    /// Internal access because `SduiModels` is a generated (internal) type;
    /// external consumers observe `loadState` / `title` instead.
    private(set) var screen: SduiModels?

    // These are owned here so views (including sheet/overlay hosts) can
    // re-use them across re-renders.
    public let screenState: ScreenState
    public let toasts: ToastHost
    public let visibility: SectionVisibilityTracker

    // MARK: - Dependencies

    private let endpoint: String
    private let config: SduiConfig
    private let nav: NavCoordinator
    private let analytics: AnalyticsDispatcher
    private let repository: SduiRepository
    private let stalenessTracker: SectionStalenessTracker
    private let impressions: ImpressionTracker
    private let bindingApplier = DataBindingApplier()
    private let polling: PollingDriver
    private let ably: AblyChannelManager

    // MARK: - Internal bookkeeping

    private var pollEventsTask: Task<Void, Never>?
    private var ablyTasks: [String: Task<Void, Never>] = [:]
    private var ablyConnectionTask: Task<Void, Never>?
    private var visibilityEventsTask: Task<Void, Never>?
    private var isForegroundActive: Bool = true

    /// Maps sectionID → Ably channel names opened for that section.
    /// Used to forward visibility events to `AblyChannelManager`.
    private var sectionAblyChannels: [String: String] = [:]

    public init(
        endpoint: String,
        config: SduiConfig,
        nav: NavCoordinator,
        analytics: AnalyticsDispatcher = LoggerAnalyticsDispatcher()
    ) {
        self.endpoint = endpoint
        self.config = config
        self.nav = nav
        self.analytics = analytics
        self.repository = SduiRepository(config: config)
        self.screenState = ScreenState()
        self.toasts = ToastHost()
        self.stalenessTracker = SectionStalenessTracker()
        self.visibility = SectionVisibilityTracker()
        self.impressions = ImpressionTracker()
        self.polling = PollingDriver(repository: self.repository)
        self.ably = AblyChannelManager(tokenURL: config.ablyTokenURL)
        self.startVisibilityForwarder()
    }

    // MARK: - Publicly accessible components

    public var stalenessPublisher: SectionStalenessTracker { stalenessTracker }

    /// Report per-section visibility. Views call this from
    /// `.onScrollVisibilityChange(threshold:)` at the `SectionLayout`
    /// level; the tracker debounces exits and forwards events to the
    /// polling driver + Ably channel manager through
    /// ``startVisibilityForwarder()``.
    public func reportVisibility(sectionID: String, visible: Bool) {
        visibility.report(sectionID: sectionID, visible: visible)
    }

    private func startVisibilityForwarder() {
        visibilityEventsTask?.cancel()
        visibilityEventsTask = Task { [weak self] in
            guard let self else { return }
            for await event in self.visibility.events() {
                switch event {
                case .entered(let sectionID):
                    await self.polling.setVisible(sectionID, visible: true)
                    if let channel = self.sectionAblyChannels[sectionID] {
                        await self.ably.setChannelVisible(channel, visible: true)
                    }
                case .exited(let sectionID):
                    await self.polling.setVisible(sectionID, visible: false)
                    if let channel = self.sectionAblyChannels[sectionID] {
                        await self.ably.setChannelVisible(channel, visible: false)
                    }
                }
            }
        }
    }

    // MARK: - Loading

    /// Kick off the initial fetch. Safe to call repeatedly (e.g. from
    /// `.refreshable`).
    public func load() async {
        loadState = .loading
        do {
            let result = try await repository.fetchScreen(endpoint: endpoint)
            applyScreen(result)
            loadState = .loaded
        } catch let err as SduiError {
            logger.error("load failed for \(self.endpoint, privacy: .public): \(err.localizedDescription, privacy: .public)")
            loadState = .failed(message: err.errorDescription ?? "Load failed")
        } catch {
            logger.error("unexpected load error: \(error.localizedDescription, privacy: .public)")
            loadState = .failed(message: error.localizedDescription)
        }
    }

    /// Called by `ActionDispatcher` when a `refresh` action fires.
    func refresh(sectionID: String?, endpoint explicitEndpoint: String?, resolvedParams: [String: String]) {
        Task {
            if let explicitEndpoint {
                // Parameterized refresh: caller-supplied endpoint + bindings.
                let url = Self.appendParams(to: explicitEndpoint, params: resolvedParams)
                do {
                    let raw = try await repository.fetchRawJson(url: url)
                    if let sectionID, let dict = raw as? [String: Any] {
                        updateSectionData(sectionID: sectionID, newData: dict)
                    } else if let decoded = try? Self.decodeScreen(from: raw) {
                        applyScreen(decoded)
                    }
                } catch {
                    logger.error("parameterized refresh failed: \(error.localizedDescription, privacy: .public)")
                    toasts.show(String(localized: "Couldn't refresh right now."), style: .error)
                }
            } else {
                // Full-screen re-fetch; sectionID scoping handled on next poll/SSE tick.
                await load()
            }
        }
    }

    // MARK: - Scene phase

    public func handleScenePhase(_ phase: ScenePhase) {
        switch phase {
        case .active:
            isForegroundActive = true
            Task { await polling.setForegroundActive(true) }
            logger.debug("scenePhase active")
        case .inactive:
            isForegroundActive = false
            Task { await polling.setForegroundActive(false) }
            logger.debug("scenePhase inactive")
        case .background:
            isForegroundActive = false
            Task { await polling.setForegroundActive(false) }
            cancelAllAbly()
            logger.debug("scenePhase background — paused polls + disconnected Ably")
            // On re-foreground the view's `.task` re-runs, which restarts
            // Ably subscriptions via `applyScreen`. Polling tasks are still
            // alive; they resume as soon as foregroundActive flips back.
        @unknown default:
            break
        }
    }

    // MARK: - Action dispatcher factory

    /// Build the dispatcher for the current screen. ScreenShell holds the
    /// reference for the lifetime of the view.
    func makeActionDispatcher() -> ActionDispatcher {
        ActionDispatcher(
            screenState: screenState,
            nav: nav,
            toasts: toasts,
            analytics: analytics,
            impressions: impressions,
            refreshHandler: { [weak self] sectionID, endpoint, params in
                self?.refresh(sectionID: sectionID, endpoint: endpoint, resolvedParams: params)
            }
        )
    }

    // MARK: - Screen update

    private func applyScreen(_ newScreen: SduiModels) {
        let firstLoad = self.screen == nil
        self.screen = newScreen
        self.title = newScreen.title
        self.navigationItems = newScreen.navigation?.items?.count ?? 0

        if firstLoad {
            screenState.initializeFrom(newScreen.state)
        } else {
            // Re-initializing state on a pure refresh would clobber user tab
            // selections. Android merges only the keys the server provides.
            for (key, value) in newScreen.state ?? [:] {
                if screenState.get(key) == nil {
                    screenState.set(key, value: value.value)
                }
            }
        }

        restartRealtime(for: newScreen)
    }

    // MARK: - Real-time wiring

    private func restartRealtime(for screen: SduiModels) {
        // Tear down previous subscriptions.
        Task { await polling.stopAll() }
        cancelAllAbly()
        sectionAblyChannels.removeAll()

        if pollEventsTask == nil {
            pollEventsTask = Task { [weak self] in
                guard let self else { return }
                let stream = await self.polling.events()
                for await event in stream {
                    await self.handlePollEvent(event)
                }
            }
        }

        for section in screen.sections {
            guard let policy = section.refreshPolicy else { continue }
            switch policy.type {
            case .poll:
                startPolling(section: section, policy: policy)
            case .sse:
                startSSE(section: section, policy: policy)
            case .refreshTypeStatic:
                break
            }
        }
    }

    private func startPolling(section: Section, policy: RefreshPolicy) {
        guard let intervalMs = policy.intervalMS else { return }
        let directURL = policy.url.flatMap(URL.init(string:))
        let screenEndpoint = self.endpoint
        let sectionID = section.id
        let dataPath = policy.dataPath
        let initiallyVisible = visibility.isVisible(sectionID)

        Task {
            // Seed visibility from the tracker; subsequent changes flow
            // through `startVisibilityForwarder`.
            await polling.setVisible(sectionID, visible: initiallyVisible)
            await polling.setForegroundActive(isForegroundActive)
            await polling.start(
                sectionID: sectionID,
                intervalMs: intervalMs,
                directURL: directURL,
                screenEndpoint: directURL == nil ? screenEndpoint : nil,
                dataPath: dataPath,
                pauseWhenOffScreen: true
            )
        }
    }

    private func startSSE(section: Section, policy: RefreshPolicy) {
        guard let channelName = policy.channel else { return }
        let sectionID = section.id
        sectionAblyChannels[sectionID] = channelName

        // Respect the pre-existing visibility state when (re)attaching.
        let isVisible = visibility.isVisible(sectionID)
        Task { [weak self] in
            await self?.ably.setChannelVisible(channelName, visible: isVisible)
        }

        ablyTasks[sectionID]?.cancel()
        ablyTasks[sectionID] = Task { [weak self, channelName] in
            guard let self else { return }
            let stream = await self.ably.subscribe(channelName: channelName)
            for await message in stream {
                // Drop messages arriving for offscreen sections — polling
                // will resync on re-entry.
                if await self.ably.isChannelVisible(channelName) == false { continue }
                await self.handleAblyMessage(sectionID: sectionID, message: message)
            }
        }

        if ablyConnectionTask == nil {
            ablyConnectionTask = Task { [weak self] in
                guard let self else { return }
                let states = await self.ably.connectionStates()
                for await state in states {
                    await self.handleAblyConnectionState(state)
                }
            }
        }
    }

    private func cancelAllAbly() {
        for (_, task) in ablyTasks { task.cancel() }
        ablyTasks.removeAll()
        ablyConnectionTask?.cancel()
        ablyConnectionTask = nil
        Task { await ably.disconnect() }
    }


    // MARK: - Event handlers

    private func handlePollEvent(_ event: PollingDriver.PollEvent) async {
        switch event {
        case .success(let success):
            stalenessTracker.clear(success.sectionID)
            if success.isDirect, let dict = success.payload as? [String: Any] {
                updateSectionData(sectionID: success.sectionID, newData: dict)
            } else if let newScreen = success.payload as? SduiModels {
                applyScreen(newScreen)
            }
        case .failure(let failure):
            if failure.consecutiveFailures >= PollingDriver.failureThreshold {
                stalenessTracker.markStale(failure.sectionID)
            }
        }
    }

    private func handleAblyMessage(sectionID: String, message: [String: Any]) async {
        stalenessTracker.clear(sectionID)
        guard let screen,
              let idx = screen.sections.firstIndex(where: { $0.id == sectionID }),
              let dataBinding = screen.sections[idx].dataBinding else { return }
        let section = screen.sections[idx]
        let currentData = Self.dataDict(from: section)
        let updated = bindingApplier.applyBindings(
            currentData: currentData,
            incomingMessage: message,
            dataBinding: dataBinding,
            sectionID: sectionID,
            traceID: screen.traceID,
            stringTable: section.stringTable
        )
        updateSectionData(sectionID: sectionID, newData: updated)
    }

    private func handleAblyConnectionState(_ state: AblyChannelManager.ConnectionState) async {
        switch state {
        case .disconnected, .failed:
            for (sectionID, _) in ablyTasks {
                stalenessTracker.markStale(sectionID)
            }
        case .connected, .other:
            break
        }
    }

    // MARK: - Mutation helpers

    /// Replace a section's `data` with the merged payload.
    private func updateSectionData(sectionID: String, newData: [String: Any]) {
        guard var screen,
              let idx = screen.sections.firstIndex(where: { $0.id == sectionID }) else { return }
        let section = screen.sections[idx]
        guard let replaced = Self.rebuildSection(section, withData: newData) else {
            logger.warning("failed to rebuild section \(sectionID, privacy: .public) data")
            return
        }
        var sections = screen.sections
        sections[idx] = replaced
        screen = Self.replaceSections(screen, with: sections)
        self.screen = screen
    }

    // MARK: - Static JSON helpers

    private static func dataDict(from section: Section) -> [String: Any] {
        guard let data = section.data,
              let raw = try? newJSONEncoder().encode(data),
              let dict = try? JSONSerialization.jsonObject(with: raw) as? [String: Any]
        else { return [:] }
        return dict
    }

    private static func rebuildSection(_ section: Section, withData dataDict: [String: Any]) -> Section? {
        guard let encoded = try? newJSONEncoder().encode(section),
              var sectionDict = try? JSONSerialization.jsonObject(with: encoded) as? [String: Any]
        else { return nil }
        sectionDict["data"] = dataDict
        guard let merged = try? JSONSerialization.data(withJSONObject: sectionDict),
              let rebuilt = try? newJSONDecoder().decode(Section.self, from: merged)
        else { return nil }
        return rebuilt
    }

    private static func replaceSections(_ screen: SduiModels, with sections: [Section]) -> SduiModels {
        guard let data = try? newJSONEncoder().encode(screen),
              var dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return screen }
        let sectionsJSON = sections.compactMap { section -> Any? in
            guard let data = try? newJSONEncoder().encode(section) else { return nil }
            return try? JSONSerialization.jsonObject(with: data)
        }
        dict["sections"] = sectionsJSON
        guard let merged = try? JSONSerialization.data(withJSONObject: dict),
              let rebuilt = try? newJSONDecoder().decode(SduiModels.self, from: merged)
        else { return screen }
        return rebuilt
    }

    private static func decodeScreen(from raw: Any) throws -> SduiModels {
        let data = try JSONSerialization.data(withJSONObject: raw)
        return try newJSONDecoder().decode(SduiModels.self, from: data)
    }

    private static func appendParams(to endpoint: String, params: [String: String]) -> URL {
        var components = URLComponents(string: endpoint) ?? URLComponents()
        var query = components.queryItems ?? []
        for (key, value) in params where !value.isEmpty {
            query.append(URLQueryItem(name: key, value: value))
        }
        components.queryItems = query
        return components.url ?? URL(string: endpoint)!
    }
}
