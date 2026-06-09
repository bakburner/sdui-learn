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
        let pollID: String
        /// When the poll hits a direct URL, the JSON payload after
        /// applying `dataPath`.
        let payload: Any
        /// `true` when this was a direct-URL poll.
        let isDirect: Bool
    }

    /// Categorical kind of a poll failure. Consumers should switch on `kind`
    /// rather than substring-match against `reason`, which is a free-form
    /// localized string.
    enum PollFailureKind: Sendable {
        case sectionNotFound
        case upgradeRequired
        case other
    }

    struct PollFailure: Sendable {
        let sectionID: String
        let pollID: String
        let consecutiveFailures: Int
        let reason: String
        let kind: PollFailureKind
    }

    enum PollEvent: @unchecked Sendable {
        case success(PollSuccess)
        case sectionSuccess(sectionID: String, pollID: String, section: Section)
        case failure(PollFailure)
    }

    static let maxBackoff: Duration = .seconds(60)
    static let failureThreshold = 3

    private let repository: SduiRepository
    private var tasks: [String: Task<Void, Never>] = [:]
    private var sectionPollIDs: [String: Set<String>] = [:]
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
    ///     (applying `dataPath` if provided). Otherwise, re-fetch the section
    ///     endpoint.
    ///   - sectionEndpoint: SDUI endpoint for single-section re-fetch.
    ///   - dataPath: JSONPath applied to `directURL` responses.
    ///   - pauseWhenOffScreen: when `true`, skips ticks while the section is
    ///     not in ``visibleSections``. Foreground gating always applies.
    func start(
        pollID: String,
        sectionID: String,
        intervalMs: Int,
        directURL: URL?,
        sectionEndpoint: String?,
        dataPath: String?,
        pauseWhenOffScreen: Bool,
        correlationId: String? = nil
    ) {
        tasks[pollID]?.cancel()
        failureCounts[pollID] = 0
        var ids = sectionPollIDs[sectionID] ?? Set<String>()
        ids.insert(pollID)
        sectionPollIDs[sectionID] = ids

        let baseInterval = Duration.milliseconds(intervalMs)
        let repository = self.repository

        tasks[pollID] = Task { [weak self] in
            var currentInterval = baseInterval
            while !Task.isCancelled {
                await self?.awaitGate(sectionID: sectionID, pauseWhenOffScreen: pauseWhenOffScreen)
                if Task.isCancelled { return }

                try? await Task.sleep(for: currentInterval)
                if Task.isCancelled { return }

                do {
                    let event: PollEvent
                    // sectionEndpoint takes precedence over directURL when both are set (schema §RefreshPolicy).
                    if let sectionEndpoint {
                        let result = try await repository.fetchSection(endpoint: sectionEndpoint, correlationId: correlationId)
                        event = .sectionSuccess(sectionID: sectionID, pollID: pollID, section: result.value)
                    } else if let directURL {
                        let raw = try await repository.fetchRawJson(url: directURL, correlationId: correlationId)
                        let trimmed = Self.extract(dataPath: dataPath, from: raw)
                        event = .success(PollSuccess(sectionID: sectionID, pollID: pollID, payload: trimmed, isDirect: true))
                    } else {
                        throw NSError(domain: "PollingDriver", code: 0, userInfo: [
                            NSLocalizedDescriptionKey: "no directURL and no sectionEndpoint for section \(sectionID)"
                        ])
                    }
                    await self?.emitSuccess(event)
                    currentInterval = baseInterval
                } catch {
                    let failures = await self?.recordFailure(pollID: pollID) ?? 1
                    currentInterval = Self.doubled(currentInterval, cap: Self.maxBackoff)
                    let kind: PollFailureKind
                    if let sduiError = error as? SduiError {
                        switch sduiError {
                        case .sectionNotFound: kind = .sectionNotFound
                        case .upgradeRequired: kind = .upgradeRequired
                        default: kind = .other
                        }
                    } else {
                        kind = .other
                    }
                    let failure = PollFailure(
                        sectionID: sectionID,
                        pollID: pollID,
                        consecutiveFailures: failures,
                        reason: error.localizedDescription,
                        kind: kind
                    )
                    await self?.emit(.failure(failure))
                    logger.warning("poll failed section=\(sectionID, privacy: .public) pollID=\(pollID, privacy: .public) attempt=\(failures) reason=\(error.localizedDescription, privacy: .public)")
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
        guard let pollIDs = sectionPollIDs.removeValue(forKey: sectionID) else { return }
        for pollID in pollIDs {
            tasks[pollID]?.cancel()
            tasks.removeValue(forKey: pollID)
            failureCounts.removeValue(forKey: pollID)
        }
    }

    /// Stop all active polls.
    func stopAll() {
        for task in tasks.values { task.cancel() }
        tasks.removeAll()
        sectionPollIDs.removeAll()
        failureCounts.removeAll()
    }

    // MARK: - Helpers

    private func recordFailure(pollID: String) -> Int {
        let next = (failureCounts[pollID] ?? 0) + 1
        failureCounts[pollID] = next
        return next
    }

    private func emitSuccess(_ event: PollEvent) {
        switch event {
        case .success(let success):
            failureCounts[success.pollID] = 0
        case .sectionSuccess(_, let pollID, _):
            failureCounts[pollID] = 0
        case .failure:
            break
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
