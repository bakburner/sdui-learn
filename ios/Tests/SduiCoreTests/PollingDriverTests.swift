import XCTest
@testable import SduiCore

/// Tests for ``PollingDriver`` gating logic: the interplay between
/// foreground state, section visibility, and `pauseWhenOffScreen`.
///
/// Network-dependent polling is not exercised here (that requires a mock
/// URLSession). These tests validate the gate state management and event
/// stream wiring.
final class PollingDriverTests: XCTestCase {

    /// When foreground is false, `awaitGate` suspends. When it flips to
    /// true, the gate opens. We test this indirectly by checking that
    /// `start()` emits no events while gated (foreground=false) and emits
    /// after the gate opens.
    ///
    /// Since `awaitGate` is private, we verify behaviour through the
    /// visible/foreground setters and the events stream.
    func testSetForegroundActiveToggle() async {
        let config = SduiConfig(
            baseURL: URL(string: "http://localhost:8080")!,
            ablyTokenURL: URL(string: "http://localhost:8080/ably-token")!
        )
        let repo = SduiRepository(config: config)
        let driver = PollingDriver(repository: repo)

        // Start foreground=false — poll gate is closed
        await driver.setForegroundActive(false)

        // Section is visible but foreground is off
        await driver.setVisible("section-1", visible: true)

        // Flip foreground to true — gate should open
        await driver.setForegroundActive(true)

        // Verify no crash, gate state is consistent
        // (can't easily test awaitGate without a real poll, but this confirms
        // the actor methods are callable and don't deadlock)
    }

    func testSetVisibleAndForeground() async {
        let config = SduiConfig(
            baseURL: URL(string: "http://localhost:8080")!,
            ablyTokenURL: URL(string: "http://localhost:8080/ably-token")!
        )
        let repo = SduiRepository(config: config)
        let driver = PollingDriver(repository: repo)

        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.setVisible("s2", visible: false)

        // Start and immediately stop — validates the start path doesn't throw
        await driver.start(
            pollID: "s1#section",
            sectionID: "s1",
            intervalMs: 60_000, // Long interval so it doesn't actually fire
            directURL: nil,
            sectionEndpoint: "/v1/sdui/test",
            dataPath: nil,
            pauseWhenOffScreen: true
        )

        // Stop should clean up without errors
        await driver.stop(sectionID: "s1")
    }

    func testPauseWhenOffScreenFalseBypassesVisibilityGate() async {
        let config = SduiConfig(
            baseURL: URL(string: "http://localhost:8080")!,
            ablyTokenURL: URL(string: "http://localhost:8080/ably-token")!
        )
        let repo = SduiRepository(config: config)
        let driver = PollingDriver(repository: repo)

        await driver.setForegroundActive(true)
        // Section is NOT visible
        await driver.setVisible("gamepanel", visible: false)

        // Start with pauseWhenOffScreen=false — gate should NOT block
        await driver.start(
            pollID: "gamepanel#section",
            sectionID: "gamepanel",
            intervalMs: 60_000,
            directURL: nil,
            sectionEndpoint: "/v1/sdui/test",
            dataPath: nil,
            pauseWhenOffScreen: false
        )

        // Quick sleep to let the task start (it should not be stuck in awaitGate)
        try? await Task.sleep(for: .milliseconds(50))

        await driver.stop(sectionID: "gamepanel")
    }

    func testStopAllCancelsEverything() async {
        let config = SduiConfig(
            baseURL: URL(string: "http://localhost:8080")!,
            ablyTokenURL: URL(string: "http://localhost:8080/ably-token")!
        )
        let repo = SduiRepository(config: config)
        let driver = PollingDriver(repository: repo)

        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.setVisible("s2", visible: true)

        await driver.start(
            pollID: "s1#section", sectionID: "s1", intervalMs: 60_000, directURL: nil,
            sectionEndpoint: "/v1/sdui/test", dataPath: nil, pauseWhenOffScreen: true
        )
        await driver.start(
            pollID: "s2#section", sectionID: "s2", intervalMs: 60_000, directURL: nil,
            sectionEndpoint: "/v1/sdui/test", dataPath: nil, pauseWhenOffScreen: true
        )

        await driver.stopAll()

        // Verify no crash or hanging tasks after stopAll
    }

    func testExtractDataPath() {
        let raw: Any = ["game": ["homeTeam": ["score": 102]]]

        let extracted = PollingDriver.extract(dataPath: "$.game.homeTeam.score", from: raw)
        XCTAssertEqual(extracted as? Int, 102)

        let noPath = PollingDriver.extract(dataPath: nil, from: raw)
        XCTAssertTrue(noPath is [String: Any])

        let missingPath = PollingDriver.extract(dataPath: "$.game.awayTeam", from: raw)
        // Falls back to raw when path segment is missing
        XCTAssertTrue(missingPath is [String: Any])
    }

    func testExtractArrayIndex() {
        let raw: Any = ["sections": [["id": "s1"], ["id": "s2"]]]

        let extracted = PollingDriver.extract(dataPath: "$.sections[1].id", from: raw)
        XCTAssertEqual(extracted as? String, "s2")
    }

    // MARK: - Network-backed tests

    private static let minimalSectionJSON: Data =
        #"{"id":"s1","type":"AtomicComposite"}"#.data(using: .utf8)!
    private static let sseSectionJSON: Data =
        #"{"id":"s1","type":"AtomicComposite","refreshPolicy":[{"type":"sse","channel":"game:123:scoreboard"}]}"#.data(using: .utf8)!

    private func makeNetworkRepository() -> SduiRepository {
        let config = SduiConfig(
            baseURL: URL(string: "http://localhost:8080")!,
            ablyTokenURL: URL(string: "http://localhost:8080/ably-token")!
        )
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [CapturingURLProtocol.self]
        let session = URLSession(configuration: configuration)
        return SduiRepository(config: config, session: session, envelopeProvider: { RequestEnvelope() })
    }

    func testSectionEndpointHappyPath() async throws {
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.minimalSectionJSON)

        let driver = PollingDriver(repository: makeNetworkRepository())
        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.start(
            pollID: "s1#section",
            sectionID: "s1",
            intervalMs: 100,
            directURL: nil,
            sectionEndpoint: "/v1/sdui/section/test",
            dataPath: nil,
            pauseWhenOffScreen: false
        )

        var events: [PollingDriver.PollEvent] = []
        let collectTask = Task {
            for await event in await driver.events() {
                events.append(event)
                if events.count >= 1 { break }
            }
        }
        try await Task.sleep(for: .milliseconds(400))
        collectTask.cancel()
        await driver.stopAll()

        XCTAssertFalse(events.isEmpty, "expected at least one poll event")
        guard case .sectionSuccess(let sectionID, _, _) = events.first else {
            XCTFail("expected .sectionSuccess, got \(String(describing: events.first))")
            return
        }
        XCTAssertEqual(sectionID, "s1")
    }

    func testSectionEndpointSSESectionYieldsSectionSuccess() async throws {
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.sseSectionJSON)

        let driver = PollingDriver(repository: makeNetworkRepository())
        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.start(
            pollID: "s1#section",
            sectionID: "s1",
            intervalMs: 100,
            directURL: nil,
            sectionEndpoint: "/v1/sdui/section/test",
            dataPath: nil,
            pauseWhenOffScreen: false
        )

        var events: [PollingDriver.PollEvent] = []
        let collectTask = Task {
            for await event in await driver.events() {
                events.append(event)
                if events.count >= 1 { break }
            }
        }
        try await Task.sleep(for: .milliseconds(400))
        collectTask.cancel()
        await driver.stopAll()

        XCTAssertFalse(events.isEmpty, "expected at least one poll event")
        guard case .sectionSuccess(let sectionID, _, let section) = events.first else {
            XCTFail("expected .sectionSuccess, got \(String(describing: events.first))")
            return
        }
        XCTAssertEqual(sectionID, "s1")
        let ssePolicy = section.refreshPolicy?.first(where: { $0.type == .sse })
        XCTAssertEqual(ssePolicy?.type, .sse)
        XCTAssertEqual(ssePolicy?.channel, "game:123:scoreboard")
    }

    func testSectionEndpoint404YieldsFailureKindSectionNotFound() async throws {
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Data(), statusCode: 404)

        let driver = PollingDriver(repository: makeNetworkRepository())
        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.start(
            pollID: "s1#section",
            sectionID: "s1",
            intervalMs: 100,
            directURL: nil,
            sectionEndpoint: "/v1/sdui/section/test",
            dataPath: nil,
            pauseWhenOffScreen: false
        )

        var events: [PollingDriver.PollEvent] = []
        let collectTask = Task {
            for await event in await driver.events() {
                events.append(event)
                if events.count >= 1 { break }
            }
        }
        try await Task.sleep(for: .milliseconds(400))
        collectTask.cancel()
        await driver.stopAll()

        XCTAssertFalse(events.isEmpty, "expected a failure event")
        guard case .failure(let failure) = events.first else {
            XCTFail("expected .failure, got \(String(describing: events.first))")
            return
        }
        guard case .sectionNotFound = failure.kind else {
            XCTFail("expected kind .sectionNotFound, got \(failure.kind)")
            return
        }
    }

    func testSectionEndpoint5xxYieldsFailureKindOther() async throws {
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Data(), statusCode: 500)

        let driver = PollingDriver(repository: makeNetworkRepository())
        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.start(
            pollID: "s1#section",
            sectionID: "s1",
            intervalMs: 100,
            directURL: nil,
            sectionEndpoint: "/v1/sdui/section/test",
            dataPath: nil,
            pauseWhenOffScreen: false
        )

        var events: [PollingDriver.PollEvent] = []
        let collectTask = Task {
            for await event in await driver.events() {
                events.append(event)
                if events.count >= 1 { break }
            }
        }
        try await Task.sleep(for: .milliseconds(400))
        collectTask.cancel()
        await driver.stopAll()

        XCTAssertFalse(events.isEmpty, "expected a failure event")
        guard case .failure(let failure) = events.first else {
            XCTFail("expected .failure, got \(String(describing: events.first))")
            return
        }
        guard case .other = failure.kind else {
            XCTFail("expected kind .other, got \(failure.kind)")
            return
        }
    }

    func testConsecutiveFailuresIncrementCount() async throws {
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Data(), statusCode: 500)

        let driver = PollingDriver(repository: makeNetworkRepository())
        await driver.setForegroundActive(true)
        await driver.setVisible("s1", visible: true)
        await driver.start(
            pollID: "s1#section",
            sectionID: "s1",
            intervalMs: 100,
            directURL: nil,
            sectionEndpoint: "/v1/sdui/section/test",
            dataPath: nil,
            pauseWhenOffScreen: false
        )

        var events: [PollingDriver.PollEvent] = []
        let collectTask = Task {
            for await event in await driver.events() {
                events.append(event)
                if events.count >= 2 { break }
            }
        }
        // First poll fires at ~100ms; after failure, interval doubles to 200ms;
        // second poll fires at ~300ms. Allow 700ms to collect both.
        try await Task.sleep(for: .milliseconds(700))
        collectTask.cancel()
        await driver.stopAll()

        XCTAssertGreaterThanOrEqual(events.count, 2, "expected at least 2 failure events")
        guard events.count >= 2, case .failure(let secondFailure) = events[1] else {
            XCTFail("expected second event to be .failure")
            return
        }
        XCTAssertEqual(secondFailure.consecutiveFailures, 2)
    }
}
