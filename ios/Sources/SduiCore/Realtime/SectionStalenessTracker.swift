import Foundation
import Observation

/// Observable set of section IDs whose real-time feed is currently degraded
/// (SSE disconnect, polling repeatedly failing, sustained binding misses).
///
/// Consumed by `SectionRouter` to overlay a staleness badge (Phase G) and by
/// accessibility descriptors (Phase H) so VoiceOver announces "stale data".
@Observable
@MainActor
public final class SectionStalenessTracker {

    /// Current set of stale section IDs.
    public private(set) var staleSections: Set<String> = []

    public init() {}

    public func markStale(_ sectionID: String) {
        staleSections.insert(sectionID)
    }

    public func clear(_ sectionID: String) {
        staleSections.remove(sectionID)
    }

    public func isStale(_ sectionID: String) -> Bool {
        staleSections.contains(sectionID)
    }
}
