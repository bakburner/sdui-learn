import Foundation
import os

/// Receives `fireAndForget` analytics events emitted by the SDUI
/// `ActionDispatcher`. The default implementation logs via `Logger`, matching
/// the placeholder on Android at
/// `android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt`
/// (comment "In a real implementation, this would forward to registered
/// backends"). Production apps inject a concrete implementation that forwards
/// to Adobe/Firebase/etc.
public protocol AnalyticsDispatcher: Sendable {
    /// Dispatch an analytics beacon.
    ///
    /// - Parameter destinations: Raw-string destinations from the schema
    ///   (`"adobe"`, `"firebase"`, `"internal"`, `"all"`). An empty array
    ///   means "all" by convention.
    func send(
        event: String,
        params: [String: Any],
        destinations: [String]
    )
}

/// Default dispatcher: logs each event via `Logger`. Identical semantics to
/// Android's prototype `handleFireAndForget` which does `Log.i(...)`.
public struct LoggerAnalyticsDispatcher: AnalyticsDispatcher {

    // `os.Logger` is documented thread-safe but is not marked `Sendable`, so we
    // hold it in an `@unchecked Sendable` holder to keep `LoggerAnalyticsDispatcher`
    // a first-class `Sendable` struct under strict concurrency.
    private let holder = LoggerHolder()

    public init() {}

    private var logger: Logger { holder.logger }

    public func send(
        event: String,
        params: [String: Any],
        destinations: [String]
    ) {
        let destinationsStr = destinations.isEmpty
            ? "all"
            : destinations.joined(separator: ",")
        logger.info(
            "analytics event=\(event, privacy: .public) destinations=\(destinationsStr, privacy: .public) params=\(String(describing: params), privacy: .public)"
        )
    }
}

private final class LoggerHolder: @unchecked Sendable {
    let logger = Logger(subsystem: "com.nba.sdui", category: "Analytics")
}

/// Test/demo helper that records every dispatched event in memory for
/// inspection.
public final class InMemoryAnalyticsDispatcher: AnalyticsDispatcher, @unchecked Sendable {
    public struct Event: Sendable, Equatable {
        public let name: String
        public let paramKeys: [String]
        public let destinations: [String]
    }

    private let lock = NSLock()
    private var _events: [Event] = []

    public init() {}

    public var events: [Event] {
        lock.withLock { _events }
    }

    public func reset() {
        lock.withLock { _events = [] }
    }

    public func send(
        event: String,
        params: [String: Any],
        destinations: [String]
    ) {
        let record = Event(
            name: event,
            paramKeys: params.keys.sorted(),
            destinations: destinations
        )
        lock.withLock { _events.append(record) }
    }
}
