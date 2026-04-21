import Foundation
import os

#if !SDUI_DISABLE_ABLY
import Ably
#endif

private let logger = Logger(subsystem: "com.nba.sdui", category: "AblyChannelManager")

#if SDUI_DISABLE_ABLY

// MARK: - No-op stub (SDUI_DISABLE_ABLY)
//
// Compiled when the package is built without the `ably-cocoa` dependency —
// e.g. on Intel Macs where Ably's SPM module map is broken on the x86_64
// simulator slice, or on any host that just wants polling-only runtime.
//
// The stub keeps the full public API so `SduiScreenViewModel` compiles
// unchanged. All calls are inert: subscriptions produce an empty
// `AsyncStream` that finishes immediately, and `connectionStates()`
// emits a single `.other` state before finishing so consumers that
// await a first event don't hang forever.

actor AblyChannelManager {

    enum ConnectionState: Sendable {
        case connected
        case disconnected
        case failed(message: String?)
        case other
    }

    init(tokenURL: URL) {
        logger.notice("AblyChannelManager stub active (SDUI_DISABLE_ABLY). Real-time via Ably is disabled; polling still works.")
    }

    func setChannelVisible(_ channelName: String, visible: Bool) {}

    func isChannelVisible(_ channelName: String) -> Bool { true }

    func initialize() {}

    func disconnect() {}

    func subscribe(channelName: String) -> AsyncStream<[String: Any]> {
        AsyncStream { $0.finish() }
    }

    func connectionStates() -> AsyncStream<ConnectionState> {
        AsyncStream { continuation in
            continuation.yield(.other)
            continuation.finish()
        }
    }
}

#else

/// Manages the SDUI app's real-time Ably connection. Mirrors Android's
/// [`AblyChannelManager`](../../android/sdui-core/src/main/java/com/nba/sdui/core/data/AblyChannelManager.kt).
///
/// Ably messages are opaque JSON — they surface as `[String: Any]`
/// dictionaries with no typed decoding. Callers route each payload through
/// ``DataBindingApplier`` to project it onto a section's current data.
///
/// Concurrency: `actor` isolation serialises channel registration /
/// teardown. Subscriptions expose `AsyncStream<[String: Any]>` so consumers
/// use plain `for await` loops inside `Task`s.
actor AblyChannelManager {

    enum ConnectionState: Sendable {
        case connected
        case disconnected
        case failed(message: String?)
        case other
    }

    private let tokenURL: URL
    private var client: ARTRealtime?
    private var channels: [String: ARTRealtimeChannel] = [:]
    private var subscriptions: [String: ARTEventListener] = [:]

    /// Channels marked invisible drop inbound messages rather than yielding
    /// them to consumers. Re-entering visibility starts yielding again.
    /// Missing entry = visible (default), so that the pre-visibility API
    /// remains backward-compatible for callers that don't bother reporting.
    private var channelVisibility: [String: Bool] = [:]

    /// Exposed as an AsyncStream for consumers that want to react to
    /// connection-state transitions (the VM flips `SectionStalenessTracker`
    /// on sustained disconnects).
    private var connectionContinuations: [UUID: AsyncStream<ConnectionState>.Continuation] = [:]

    init(tokenURL: URL) {
        self.tokenURL = tokenURL
    }

    /// Mark a channel visible / invisible. While invisible the manager
    /// reports `isChannelVisible == false`; consumers (the VM) decide
    /// whether to skip message processing. The actual Ably subscription
    /// stays attached to avoid the re-handshake cost when the section
    /// scrolls back into view.
    func setChannelVisible(_ channelName: String, visible: Bool) {
        channelVisibility[channelName] = visible
        logger.debug("channel \(channelName, privacy: .public) visible=\(visible)")
    }

    /// Query the current visibility state for a channel. Missing
    /// entries default to `true` (visible) so callers that never report
    /// visibility retain pre-Phase-F behaviour.
    func isChannelVisible(_ channelName: String) -> Bool {
        channelVisibility[channelName] ?? true
    }

    // MARK: - Lifecycle

    /// Idempotently create the underlying `ARTRealtime` client. Token
    /// retrieval happens lazily via the auth callback.
    func initialize() {
        guard client == nil else { return }

        let options = ARTClientOptions()
        options.autoConnect = true
        options.logLevel = .warn
        options.authCallback = { [tokenURL] _, callback in
            Task {
                await Self.fetchToken(from: tokenURL, callback: callback)
            }
        }

        let realtime = ARTRealtime(options: options)
        realtime.connection.on { [weak self] stateChange in
            guard let self else { return }
            let mapped: ConnectionState
            switch stateChange.current {
            case .connected: mapped = .connected
            case .disconnected, .suspended: mapped = .disconnected
            case .failed: mapped = .failed(message: stateChange.reason?.message)
            default: mapped = .other
            }
            Task { await self.broadcastConnection(mapped) }
        }
        client = realtime
        logger.info("Ably client created tokenURL=\(self.tokenURL.absoluteString, privacy: .public)")
    }

    func disconnect() {
        for (name, listener) in subscriptions {
            if let channel = channels[name] {
                channel.unsubscribe(listener)
            }
        }
        subscriptions.removeAll()
        channels.removeAll()
        client?.close()
        client = nil
        for (_, cont) in connectionContinuations {
            cont.finish()
        }
        connectionContinuations.removeAll()
    }

    // MARK: - Subscriptions

    /// Subscribe to a channel by name. Returns an `AsyncStream` that emits
    /// each message payload as an opaque `[String: Any]`.
    func subscribe(channelName: String) -> AsyncStream<[String: Any]> {
        initialize()
        guard let client else {
            return AsyncStream { $0.finish() }
        }

        let channel = client.channels.get(channelName)
        channels[channelName] = channel

        channel.on { stateChange in
            switch stateChange.current {
            case .attached: logger.info("channel attached \(channelName, privacy: .public)")
            case .detached: logger.warning("channel detached \(channelName, privacy: .public)")
            case .failed: logger.error("channel failed \(channelName, privacy: .public) reason=\(stateChange.reason?.message ?? "?", privacy: .public)")
            default: break
            }
        }

        return AsyncStream { continuation in
            let listener = channel.subscribe { [channelName] message in
                guard let payload = Self.parseMessage(message) else {
                    logger.warning("dropping non-dictionary payload on \(channelName, privacy: .public)")
                    return
                }
                continuation.yield(payload)
            }

            if let listener {
                Task { [weak self] in
                    await self?.registerListener(listener, for: channelName)
                }
            }

            continuation.onTermination = { [weak self] _ in
                Task { [weak self] in await self?.tearDown(channelName: channelName) }
            }
        }
    }

    /// Connection-state stream; a fresh stream per consumer (VM + tests).
    func connectionStates() -> AsyncStream<ConnectionState> {
        AsyncStream { continuation in
            let id = UUID()
            Task { [weak self] in
                await self?.registerConnectionContinuation(id: id, continuation: continuation)
            }
            continuation.onTermination = { [weak self] _ in
                Task { [weak self] in await self?.unregisterConnectionContinuation(id: id) }
            }
        }
    }

    // MARK: - Private

    private func registerListener(_ listener: ARTEventListener, for channelName: String) {
        subscriptions[channelName] = listener
    }

    private func tearDown(channelName: String) {
        if let listener = subscriptions.removeValue(forKey: channelName),
           let channel = channels[channelName] {
            channel.unsubscribe(listener)
        }
        channels.removeValue(forKey: channelName)
    }

    private func registerConnectionContinuation(id: UUID, continuation: AsyncStream<ConnectionState>.Continuation) {
        connectionContinuations[id] = continuation
    }

    private func unregisterConnectionContinuation(id: UUID) {
        connectionContinuations.removeValue(forKey: id)
    }

    private func broadcastConnection(_ state: ConnectionState) {
        for (_, cont) in connectionContinuations {
            cont.yield(state)
        }
    }

    private static func parseMessage(_ message: ARTMessage) -> [String: Any]? {
        switch message.data {
        case let dict as [String: Any]:
            return dict
        case let str as String:
            guard let data = str.data(using: .utf8) else { return nil }
            return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
        case let data as Data:
            return (try? JSONSerialization.jsonObject(with: data)) as? [String: Any]
        default:
            return nil
        }
    }

    /// The NBA identity endpoint wraps tokens as
    /// `{"status":"success","data":{"accessToken":"<JWT>"}}` — Ably expects
    /// the raw JWT so we unwrap before handing it back.
    private static func fetchToken(
        from url: URL,
        callback: @escaping (ARTTokenDetailsCompatible?, (any Error)?) -> Void
    ) async {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 10
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                let statusCode = (response as? HTTPURLResponse)?.statusCode ?? -1
                throw NSError(domain: "AblyAuth", code: statusCode, userInfo: [
                    NSLocalizedDescriptionKey: "Token endpoint returned HTTP \(statusCode)"
                ])
            }
            let jwt = extractJWT(from: data) ?? String(data: data, encoding: .utf8) ?? ""
            callback(jwt as NSString, nil)
        } catch {
            logger.error("Ably token fetch failed: \(error.localizedDescription, privacy: .public)")
            callback(nil, error)
        }
    }

    private static func extractJWT(from data: Data) -> String? {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let innerData = json["data"] as? [String: Any],
              let token = innerData["accessToken"] as? String,
              !token.isEmpty else { return nil }
        return token
    }
}

#endif
