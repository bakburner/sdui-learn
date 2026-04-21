import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "ImpressionTracker")

/// Dedupes and fires impression analytics beacons per ADR-009.
///
/// Mirrors the web `AnalyticsProvider` + `useImpressionTracking` pair:
/// - `once-per-screen` (default): fire at most one beacon per dedup key for
///   the lifetime of the screen.
/// - `once-per-session`: fire at most one beacon per dedup key per app
///   session (tracker lives as long as the host does).
/// - `once-per-interval`: fire no more than once per `intervalMs` per dedup
///   key.
/// - `none`: fire every time.
///
/// The tracker is an `actor` because state reads/writes happen from both
/// the main actor (ActionDispatcher) and background visibility tasks; the
/// actor isolation gives us a single critical section for registry updates.
actor ImpressionTracker {

    private struct Record {
        var lastFiredAt: Date
    }

    private var registry: [String: Record] = [:]
    private let clock: @Sendable () -> Date

    init(clock: @Sendable @escaping () -> Date = { Date() }) {
        self.clock = clock
    }

    /// Decide whether the action should fire given its impression policy,
    /// mark it fired if so, and return the decision. The action is keyed
    /// by `sectionID:event` to match the web implementation.
    ///
    /// Returns `true` when the caller should dispatch the beacon.
    func shouldFire(
        sectionID: String,
        event: String,
        policy: ImpressionPolicy?
    ) -> Bool {
        let dedup = policy?.dedup ?? .oncePerScreen
        let key = "\(sectionID):\(event)"
        let now = clock()

        switch dedup {
        case .none:
            registry[key] = Record(lastFiredAt: now)
            return true

        case .oncePerScreen, .oncePerSession:
            if registry[key] != nil {
                logger.debug("impression suppressed (\(dedup.rawValue, privacy: .public)): \(key, privacy: .public)")
                return false
            }
            registry[key] = Record(lastFiredAt: now)
            return true

        case .oncePerInterval:
            let intervalMs = policy?.intervalMS ?? 30_000
            if let last = registry[key]?.lastFiredAt,
               now.timeIntervalSince(last) * 1000 < Double(intervalMs) {
                logger.debug("impression suppressed (interval): \(key, privacy: .public)")
                return false
            }
            registry[key] = Record(lastFiredAt: now)
            return true
        }
    }

    /// Clear all tracked impressions. Called when the screen tears down
    /// so `once-per-screen` really is per screen, not per app lifetime.
    func resetScreen() {
        registry.removeAll()
    }

    /// Inspect the registry size (useful for tests / debugging).
    func count() -> Int {
        registry.count
    }
}
