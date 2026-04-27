import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "PollingDriver")

/// Per-section polling driver. Mirrors Android's VM-embedded poll logic (see
/// `SduiScreenViewModel.setupPolling`) extracted into its own component for
/// test isolation.
///
/// Semantics:
/// - One `Task` per polling section, keyed by `sectionID`
/// - Honours `refreshPolicy.intervalMs` as the base cadence
/// - Exponential backoff on failure, capped at ``maxBackoff``
/// - Resets cadence + failure count on first success
/// - Gate: polling suspends while `isForegroundActive == false`
/// - Hook: `pauseWhenOffScreen` pauses when the section scrolls offscreen
///
/// `actor` isolation serialises start/stop to avoid duplicate tasks per
/// section across rapid lifecycle flips.
actor PollingDriver {

    // `@unchecked Sendable`: `payload` is held as `Any` because the runtime
    // value is a JSON tree (`[String: Any]` / `[Any]` / primitives) produced
    // by `JSONSerialization`. Each instance is constructed once and only read
    // downstream, so there is no shared mutable state to race on.
    struct PollSuccess: @unchecked Sendable {
        let sectionID: String
        /// When the poll hits a direct URL, the JSON payload after
        /// applying `dataPath`. When polling the full SDUI endpoint, the
        /// whole `SduiModels` JSON as `[String: Any]`.
        let payload: Any
        /// `true` when this was a direct-URL poll (payload is section data);
        /// `false` when the VM should re-apply the full screen response.
        let isDirect: Bool
    }

    struct PollFailure: Sendable {
        let sectionID: String
        let consecutiveFailures: Int
        let reason: String
    }

    enum PollEvent: Sendable {
        case success(PollSuccess)
        case failure(PollFailure)
    }

    static let maxBackoff: Duration = .seconds(60)
    static let failureThreshold = 3

    private let repository: SduiRepository
    private var tasks: [String: Task<Void, Never>] = [:]
    private var failureCounts: [String: Int] = [:]
    private var eventContinuations: [UUID: AsyncStream<PollEvent>.Continuation] = [:]

    // Gate state pushed in by the VM. The gate inside `start(...)` consults
    // these instead of taking unsafe-isolated closures.
    private var foregroundActive: Bool = true
    private var visibleSections: Set<String> = []

    init(repository: SduiRepository) {
        self.repository = repository
    }

    // MARK: - Gate state

    func setForegroundActive(_ active: Bool) {
        foregroundActive = active
    }

    func setVisible(_ sectionID: String, visible: Bool) {
        if visible {
            visibleSections.insert(sectionID)
        } else {
            visibleSections.remove(sectionID)
        }
    }

    /// Output stream of poll events for the VM to consume. One stream per
    /// consumer; the VM typically takes one.
    func events() -> AsyncStream<PollEvent> {
        AsyncStream { continuation in
            let id = UUID()
            // `events()` is actor-isolated; the Task inherits that isolation,
            // so `register` runs on this actor without crossing a suspension
            // point. The `await` was a no-op and the compiler flagged it.
            Task { [weak self] in await self?.register(id: id, continuation: continuation) }
            continuation.onTermination = { [weak self] _ in
                Task { [weak self] in await self?.unregister(id: id) }
            }
        }
    }

    private func register(id: UUID, continuation: AsyncStream<PollEvent>.Continuation) {
        eventContinuations[id] = continuation
    }

    private func unregister(id: UUID) {
        eventContinuations.removeValue(forKey: id)
    }

    // MARK: - Start / stop

    /// Start polling `sectionID`. Safe to call repeatedly — any existing task
    /// for this section is cancelled and replaced.
    ///
    /// - Parameters:
    ///   - intervalMs: base cadence from `RefreshPolicy.intervalMs`.
    ///   - directURL: when non-nil, poll this URL and emit raw payload
    ///     (applying `dataPath` if provided). Otherwise, re-fetch the full
    ///     screen endpoint.
    ///   - screenEndpoint: SDUI endpoint for full-screen re-fetch.
    ///   - dataPath: JSONPath applied to `directURL` responses.
    ///   - pauseWhenOffScreen: when `true`, skips ticks while the section is
    ///     not in ``visibleSections``. Foreground gating always applies.
    func start(
        sectionID: String,
        intervalMs: Int,
        directURL: URL?,
        screenEndpoint: String?,
        dataPath: String?,
        pauseWhenOffScreen: Bool,
        traceID: String? = nil
    ) {
        tasks[sectionID]?.cancel()
        failureCounts[sectionID] = 0

        let baseInterval = Duration.milliseconds(intervalMs)
        let repository = self.repository

        tasks[sectionID] = Task { [weak self] in
            var currentInterval = baseInterval
            while !Task.isCancelled {
                await self?.awaitGate(sectionID: sectionID, pauseWhenOffScreen: pauseWhenOffScreen)
                if Task.isCancelled { return }

                try? await Task.sleep(for: currentInterval)
                if Task.isCancelled { return }

                do {
                    let event: PollEvent
                    if let directURL {
                        let raw = try await repository.fetchRawJson(url: directURL, traceID: traceID)
                        let trimmed = Self.extract(dataPath: dataPath, from: raw)
                        event = .success(PollSuccess(sectionID: sectionID, payload: trimmed, isDirect: true))
                    } else if let screenEndpoint {
                        let screen = try await repository.fetchScreen(endpoint: screenEndpoint)
                        event = .success(PollSuccess(sectionID: sectionID, payload: screen, isDirect: false))
                    } else {
                        throw NSError(domain: "PollingDriver", code: 0, userInfo: [
                            NSLocalizedDescriptionKey: "no directURL and no screenEndpoint for section \(sectionID)"
                        ])
                    }
                    await self?.emitSuccess(event)
                    currentInterval = baseInterval
                } catch {
                    let failures = await self?.recordFailure(sectionID: sectionID) ?? 1
                    currentInterval = Self.doubled(currentInterval, cap: Self.maxBackoff)
                    let failure = PollFailure(
                        sectionID: sectionID,
                        consecutiveFailures: failures,
                        reason: error.localizedDescription
                    )
                    await self?.emit(.failure(failure))
                    logger.warning("poll failed section=\(sectionID, privacy: .public) attempt=\(failures) reason=\(error.localizedDescription, privacy: .public)")
                }
            }
        }
    }

    /// Suspends until `foregroundActive == true` and, when
    /// `pauseWhenOffScreen == true`, until the section is in
    /// ``visibleSections``. Polls every 250ms.
    private func awaitGate(sectionID: String, pauseWhenOffScreen: Bool) async {
        while !Task.isCancelled {
            let ok = foregroundActive && (!pauseWhenOffScreen || visibleSections.contains(sectionID))
            if ok { return }
            try? await Task.sleep(for: .milliseconds(250))
        }
    }

    /// Stop polling a specific section.
    func stop(sectionID: String) {
        tasks[sectionID]?.cancel()
        tasks.removeValue(forKey: sectionID)
        failureCounts.removeValue(forKey: sectionID)
    }

    /// Stop all active polls.
    func stopAll() {
        for task in tasks.values { task.cancel() }
        tasks.removeAll()
        failureCounts.removeAll()
    }

    // MARK: - Helpers

    private func recordFailure(sectionID: String) -> Int {
        let next = (failureCounts[sectionID] ?? 0) + 1
        failureCounts[sectionID] = next
        return next
    }

    private func emitSuccess(_ event: PollEvent) {
        if case let .success(success) = event {
            failureCounts[success.sectionID] = 0
        }
        emit(event)
    }

    private func emit(_ event: PollEvent) {
        for (_, cont) in eventContinuations {
            cont.yield(event)
        }
    }

    private static func doubled(_ d: Duration, cap: Duration) -> Duration {
        let doubled = d + d
        return doubled > cap ? cap : doubled
    }

    /// Apply a JSONPath-style `dataPath` (e.g. `$.game` or `$.sections[0]`)
    /// to a raw JSON value. Falls back to the raw value when extraction fails.
    static func extract(dataPath: String?, from raw: Any) -> Any {
        guard let dataPath, !dataPath.isEmpty else { return raw }
        let clean = dataPath.hasPrefix("$.") ? String(dataPath.dropFirst(2)) : dataPath

        var current: Any? = raw
        for part in clean.split(separator: ".") {
            guard let node = current, !(node is NSNull) else { return raw }
            let segment = String(part)
            if let (field, idx) = parseArrayIndex(segment) {
                guard let dict = node as? [String: Any],
                      let array = dict[field] as? [Any],
                      idx < array.count else { return raw }
                current = array[idx]
            } else if let dict = node as? [String: Any] {
                current = dict[segment]
            } else {
                return raw
            }
        }
        return current ?? raw
    }

    private static func parseArrayIndex(_ part: String) -> (String, Int)? {
        guard let open = part.firstIndex(of: "["), part.last == "]" else { return nil }
        let field = String(part[..<open])
        let idxStr = part[part.index(after: open)..<part.index(before: part.endIndex)]
        guard let idx = Int(idxStr) else { return nil }
        return (field, idx)
    }
}
