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
        case upgradeRequired(message: String)
    }

    // MARK: - Observable surface

    public private(set) var loadState: LoadState = .idle
    public private(set) var title: String?
    public private(set) var navigationItems: Int = 0

    /// Most recent full-screen response. Views read `sections` from this.
    /// Internal access because `SduiModels` is a generated (internal) type;
    /// external consumers observe `loadState` / `title` instead.
    private(set) var screen: SduiModels?

    /// Last successfully loaded screen; retained when a later fetch fails so
    /// shell navigation and ``SduiModels/parentURI`` stay available for escape.
    private(set) var shellScreen: SduiModels?

    // These are owned here so views (including sheet/overlay hosts) can
    // re-use them across re-renders.
    public let screenState: ScreenState
    public let toasts: ToastHost
    public let visibility: SectionVisibilityTracker

    /// Origin for absolutizing relative demo/static paths from the server payload.
    public var wireAssetBaseURL: String { config.baseURL.absoluteString }

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

    /// Sections that should never have their refresh paused on scroll-out.
    /// Set from `refreshPolicy.pauseWhenOffScreen == false`.
    private var alwaysRefreshSections: Set<String> = []

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
                    if let channel = self.sectionAblyChannels[sectionID],
                       !self.alwaysRefreshSections.contains(sectionID) {
                        await self.ably.setChannelVisible(channel, visible: true)
                    }
                case .exited(let sectionID):
                    await self.polling.setVisible(sectionID, visible: false)
                    if let channel = self.sectionAblyChannels[sectionID],
                       !self.alwaysRefreshSections.contains(sectionID) {
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
        } catch let err as SduiError where err == .upgradeRequired {
            logger.warning("Schema version mismatch — upgrade required for \(self.endpoint, privacy: .public)")
            loadState = .upgradeRequired(message: err.errorDescription ?? "Please update the app to continue.")
        } catch let err as SduiError {
            logger.error("load failed for \(self.endpoint, privacy: .public): \(err.localizedDescription, privacy: .public)")
            loadState = .failed(message: err.errorDescription ?? "Load failed")
        } catch {
            logger.error("unexpected load error: \(error.localizedDescription, privacy: .public)")
            loadState = .failed(message: error.localizedDescription)
        }
    }

    /// Called by `ActionDispatcher` when a `refresh` action fires.
    ///
    /// Parameterized refresh routes through the same `fetchScreen` transport as
    /// the initial load so the request envelope, bracket-notation encoding,
    /// length-based POST fallback, and `X-Trace-Id` correlation all carry over
    /// uniformly. The response is always a full screen — section merging is
    /// handled client-side by id (Android matches).
    func refresh(sectionID: String?, endpoint explicitEndpoint: String?, resolvedParams: [String: String]) {
        Task {
            if let explicitEndpoint {
                do {
                    let refreshScreen = try await repository.fetchScreen(
                        endpoint: explicitEndpoint,
                        userParams: resolvedParams,
                        traceID: screen?.traceID
                    )
                    await mergeRefreshedScreen(refreshScreen, targetSectionID: sectionID)
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

    /// Apply a parameterized-refresh response: surgical replace when the
    /// target section is in the response, fall back to wholesale apply
    /// otherwise. Mirrors Android's `refreshSections` merge semantics.
    private func mergeRefreshedScreen(_ refreshed: SduiModels, targetSectionID: String?) async {
        for (key, value) in refreshed.state ?? [:] {
            screenState.set(key, value: value.value)
        }

        guard let current = screen else {
            applyScreen(refreshed)
            return
        }

        if let targetSectionID,
           let updated = refreshed.sections.first(where: { $0.id == targetSectionID }),
           let idx = current.sections.firstIndex(where: { $0.id == targetSectionID }) {
            var merged = current.sections
            merged[idx] = updated
            screen = current.with(sections: merged)
            return
        }

        applyScreen(refreshed)
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
        self.shellScreen = newScreen
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
        let shouldPause = policy.pauseWhenOffScreen ?? true
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
                pauseWhenOffScreen: shouldPause,
                traceID: self.screen?.traceID
            )
        }
    }

    private func startSSE(section: Section, policy: RefreshPolicy) {
        guard let channelName = policy.channel else { return }
        let sectionID = section.id
        let shouldPause = policy.pauseWhenOffScreen ?? true
        sectionAblyChannels[sectionID] = channelName

        if !shouldPause {
            alwaysRefreshSections.insert(sectionID)
        }

        // Respect the pre-existing visibility state when (re)attaching.
        // Sections with pauseWhenOffScreen=false are always "visible" for
        // SSE purposes — they never gate on scroll position.
        let isVisible = shouldPause ? visibility.isVisible(sectionID) : true
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
                await applyPolledData(sectionID: success.sectionID, incoming: dict)
            } else if let newScreen = success.payload as? SduiModels {
                applyScreen(newScreen)
            }
        case .failure(let failure):
            if failure.consecutiveFailures >= PollingDriver.failureThreshold {
                stalenessTracker.markStale(failure.sectionID)
            }
        }
    }

    /// Apply a direct-URL poll payload to a section's data. Mirrors
    /// ``handleAblyMessage`` so the poll and real-time paths are
    /// symmetrical, and matches the web `LiveSectionWrapper` two-step
    /// behaviour:
    ///   1. If the section declares a `dataBinding`, map incoming fields
    ///      through it (preserves every key the binding does not touch,
    ///      including `ui` for AtomicComposite sections).
    ///   2. Otherwise shallow-merge the incoming payload over the current
    ///      data. Keys not present in the incoming payload survive —
    ///      which means `data.ui` is preserved even for unconfigured
    ///      AtomicComposite polls, instead of being wiped out like the
    ///      old wholesale-replace path did.
    private func applyPolledData(sectionID: String, incoming: [String: Any]) async {
        guard let screen,
              let idx = screen.sections.firstIndex(where: { $0.id == sectionID }) else { return }
        let section = screen.sections[idx]
        let currentData = await Self.dataDictOnBackground(from: section)
        let updated: [String: Any]
        if let dataBinding = section.dataBinding {
            updated = bindingApplier.applyBindings(
                currentData: currentData,
                incomingMessage: incoming,
                dataBinding: dataBinding,
                sectionID: sectionID,
                traceID: screen.traceID,
                stringTable: section.stringTable
            )
        } else {
            var merged = currentData
            for (k, v) in incoming { merged[k] = v }
            updated = merged
        }
        await updateSectionData(sectionID: sectionID, newData: updated)
    }

    private func handleAblyMessage(sectionID: String, message: [String: Any]) async {
        stalenessTracker.clear(sectionID)
        guard let screen,
              let idx = screen.sections.firstIndex(where: { $0.id == sectionID }),
              let dataBinding = screen.sections[idx].dataBinding else { return }
        let section = screen.sections[idx]
        let currentData = await Self.dataDictOnBackground(from: section)
        let updated = bindingApplier.applyBindings(
            currentData: currentData,
            incomingMessage: message,
            dataBinding: dataBinding,
            sectionID: sectionID,
            traceID: screen.traceID,
            stringTable: section.stringTable
        )
        await updateSectionData(sectionID: sectionID, newData: updated)
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
    private func updateSectionData(sectionID: String, newData: [String: Any]) async {
        guard let screen = self.screen,
              let idx = screen.sections.firstIndex(where: { $0.id == sectionID }) else { return }
        let section = screen.sections[idx]
        let newDataData = try? JSONSerialization.data(withJSONObject: newData)
        let newScreen: SduiModels? = await Task.detached(priority: .userInitiated) {
            if let newDataData,
               let newDataClass = try? newJSONDecoder().decode(DataClass.self, from: newDataData) {
                let newSection = section.with(data: newDataClass)
                var newSections = screen.sections
                newSections[idx] = newSection
                return screen.with(sections: newSections)
            }
            guard let replaced = Self.rebuildSectionMergingData(section, newData: newData) else { return nil }
            var newSections = screen.sections
            newSections[idx] = replaced
            return Self.replaceSectionsOnScreen(screen, with: newSections)
        }.value
        if let newScreen {
            self.screen = newScreen
        } else {
            logger.warning("failed to rebuild section \(sectionID, privacy: .public) data")
        }
    }

    // MARK: - Static JSON helpers

    private nonisolated static func rebuildSectionMergingData(_ section: Section, newData: [String: Any]) -> Section? {
        guard let encoded = try? newJSONEncoder().encode(section),
              var sectionDict = try? JSONSerialization.jsonObject(with: encoded) as? [String: Any]
        else { return nil }
        sectionDict["data"] = newData
        guard let merged = try? JSONSerialization.data(withJSONObject: sectionDict),
              let rebuilt = try? newJSONDecoder().decode(Section.self, from: merged)
        else { return nil }
        return rebuilt
    }

    private nonisolated static func replaceSectionsOnScreen(_ screen: SduiModels, with sections: [Section]) -> SduiModels? {
        guard let data = try? newJSONEncoder().encode(screen),
              var dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return nil }
        let sectionsJSON = sections.compactMap { section -> Any? in
            guard let data = try? newJSONEncoder().encode(section) else { return nil }
            return try? JSONSerialization.jsonObject(with: data)
        }
        dict["sections"] = sectionsJSON
        guard let merged = try? JSONSerialization.data(withJSONObject: dict),
              let rebuilt = try? newJSONDecoder().decode(SduiModels.self, from: merged)
        else { return nil }
        return rebuilt
    }

    private static func dataDictOnBackground(from section: Section) async -> [String: Any] {
        await Task.detached(priority: .userInitiated) {
            guard let data = section.data,
                  let raw = try? newJSONEncoder().encode(data),
                  let dict = try? JSONSerialization.jsonObject(with: raw) as? [String: Any]
            else { return [:] }
            return dict
        }.value
    }

}
