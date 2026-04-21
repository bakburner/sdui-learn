import Foundation
import Observation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "SectionVisibilityTracker")

/// Tracks which sections are currently visible on-screen.
///
/// Mirrors Android's
/// [`SectionVisibilityTracker`](../../../../../android/sdui-core/src/main/java/com/nba/sdui/core/state/SectionVisibilityTracker.kt)
/// but implemented with SwiftUI's `.onScrollVisibilityChange` instead of
/// LazyList layoutInfo probing — on iOS we get per-view visibility callbacks
/// from the scroll system itself, so we just book-keep the reported state
/// and debounce exits.
///
/// Entry is immediate; exit is debounced by ``exitDebounceMS`` (default 500ms)
/// to absorb scroll bounce and brief off-screen blips during section reflow.
/// Consumers observe ``visibleSections`` (for SSE channel gating) or await
/// change events via ``events()``.
@Observable
@MainActor
public final class SectionVisibilityTracker {

    /// Set of sections currently considered "visible enough" to drive
    /// polling / SSE processing. Published via `@Observable`.
    public private(set) var visibleSections: Set<String> = []

    private let exitDebounceMS: UInt64
    private var exitTasks: [String: Task<Void, Never>] = [:]
    private var continuations: [UUID: AsyncStream<VisibilityEvent>.Continuation] = [:]

    public init(exitDebounceMS: UInt64 = 500) {
        self.exitDebounceMS = exitDebounceMS
    }

    /// Returns true when the section is currently in the visibility set.
    public func isVisible(_ sectionID: String) -> Bool {
        visibleSections.contains(sectionID)
    }

    /// Report a visibility change for a section. Typically called from
    /// `.onScrollVisibilityChange(threshold:)` at the `SectionLayout` level.
    public func report(sectionID: String, visible: Bool) {
        if visible {
            // Entry is always immediate; cancel any pending exit.
            exitTasks[sectionID]?.cancel()
            exitTasks[sectionID] = nil

            if !visibleSections.contains(sectionID) {
                visibleSections.insert(sectionID)
                emit(.entered(sectionID: sectionID))
                logger.debug("section visible: \(sectionID, privacy: .public)")
            }
        } else {
            // Exit is debounced; subsequent `true` calls cancel the task.
            exitTasks[sectionID]?.cancel()
            exitTasks[sectionID] = Task { @MainActor [weak self, exitDebounceMS] in
                try? await Task.sleep(nanoseconds: exitDebounceMS * 1_000_000)
                guard let self, !Task.isCancelled else { return }
                if self.visibleSections.remove(sectionID) != nil {
                    self.emit(.exited(sectionID: sectionID))
                    logger.debug("section offscreen: \(sectionID, privacy: .public)")
                }
                self.exitTasks[sectionID] = nil
            }
        }
    }

    /// Clear tracked state. Call when the screen tears down or reloads so
    /// stale IDs don't leak across navigations.
    public func reset() {
        for (_, task) in exitTasks { task.cancel() }
        exitTasks.removeAll()
        visibleSections.removeAll()
    }

    /// Async stream of entry / exit events. Consumers use this to forward
    /// visibility updates to ``PollingDriver`` and ``AblyChannelManager``.
    public func events() -> AsyncStream<VisibilityEvent> {
        let id = UUID()
        return AsyncStream { continuation in
            Task { @MainActor [weak self] in
                self?.continuations[id] = continuation
            }
            continuation.onTermination = { [weak self] _ in
                Task { @MainActor [weak self] in
                    self?.continuations[id] = nil
                }
            }
        }
    }

    private func emit(_ event: VisibilityEvent) {
        for (_, cont) in continuations {
            cont.yield(event)
        }
    }

    public enum VisibilityEvent: Sendable, Equatable {
        case entered(sectionID: String)
        case exited(sectionID: String)
    }
}
