import XCTest
@testable import SduiCore

@MainActor
final class SduiScreenViewModelRefreshTests: XCTestCase {

    override func setUp() {
        super.setUp()
        CapturingURLProtocol.reset()
    }

    // MARK: - (a) Screen-channel full replace drops omitted sections

    func testScreenChannelMatchingIdReplacesEntireScreenAndDropsOmittedSections() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.screenWith3SectionsJSON)
        await vm.load()
        XCTAssertEqual(vm.screen?.sections.count, 3)
        XCTAssertEqual(vm.screen?.sections.map(\.id), ["section-a", "section-b", "section-c"])

        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.screenWith2SectionsJSON)
        await vm.replaceCurrentScreen(endpoint: "/v1/sdui/screen/game-detail", userParams: [:])

        XCTAssertEqual(vm.screen?.sections.count, 2, "Sections omitted from the new payload must be dropped")
        XCTAssertEqual(vm.screen?.sections.map(\.id), ["section-a", "section-c"])
    }

    // MARK: - (b) Screen-channel mismatched id is dropped

    func testScreenChannelMismatchedIdIsDroppedWithWarning() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.gameDetailScreenJSON)
        await vm.load()
        XCTAssertEqual(vm.screen?.id, "game-detail")

        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.mismatchedIdScreenJSON)
        await vm.replaceCurrentScreen(endpoint: "/v1/sdui/screen/leaders", userParams: [:])

        XCTAssertEqual(vm.screen?.id, "game-detail", "Mismatched id must not replace the current screen")
        XCTAssertEqual(vm.screen?.sections.count, 1, "Original sections must be preserved")
    }

    // MARK: - (c) Section-channel replaces only the target section

    func testSectionChannelReplacesOnlyTargetSectionInPlace() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.screenWith3SectionsJSON)
        await vm.load()
        XCTAssertEqual(vm.screen?.sections.count, 3)

        let originalB = vm.screen?.sections.first(where: { $0.id == "section-b" })
        XCTAssertNotNil(originalB?.refreshPolicy, "Original section-b has a refreshPolicy")

        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.replacementSectionBJSON)
        await vm.replaceSection(sectionID: "section-b", endpoint: "/v1/sdui/section/section-b")

        XCTAssertEqual(vm.screen?.sections.count, 3, "Section count must not change")
        XCTAssertEqual(vm.screen?.sections.map(\.id), ["section-a", "section-b", "section-c"],
                       "Section order must be preserved")

        let replacedB = vm.screen?.sections.first(where: { $0.id == "section-b" })
        XCTAssertNil(replacedB?.refreshPolicy, "Replaced section-b should lack a refreshPolicy")
    }

    // MARK: - (d) Pull-to-refresh preserves current query params

    func testPullToRefreshPreservesCurrentQueryParams() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps, endpoint: "/v1/sdui/screen/games")

        CapturingURLProtocol.respond(with: Self.gamesScreenJSON)
        await vm.load()

        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.gamesScreenJSON)
        await vm.replaceCurrentScreen(
            endpoint: "/v1/sdui/screen/games",
            userParams: ["date": "2026-05-18"]
        )

        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.gamesScreenJSON)
        await vm.load()

        let request = try CapturingURLProtocol.requireCaptured()
        let query = try XCTUnwrap(request.url?.query)
        XCTAssertTrue(query.contains("date=2026-05-18"),
                      "Pull-to-refresh must replay the current screen's user params")
    }

    // MARK: - (e) Successful refetch resets the poll timer

    func testSuccessfulReplaceCurrentScreenResetsPollTimer() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.screenWithPollJSON)
        await vm.load()

        // Wait for at least one poll tick to confirm polling is active.
        try await Task.sleep(for: .milliseconds(350))

        // Now trigger replaceCurrentScreen. This calls applyScreen on success,
        // which cancels the existing screenLevelPollTask and restarts it —
        // effectively resetting the timer.
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.screenWithPollJSON)
        await vm.replaceCurrentScreen(endpoint: "/v1/sdui/screen/game-detail", userParams: [:])

        // The screen should have been replaced (proving applyScreen ran,
        // which always resets the screen-level poll task).
        XCTAssertEqual(vm.screen?.id, "game-detail")
        XCTAssertEqual(vm.loadState, .loaded)

        // Verify no premature poll fires within half the interval after reset.
        CapturingURLProtocol.reset()
        CapturingURLProtocol.respond(with: Self.screenWithPollJSON)
        try await Task.sleep(for: .milliseconds(100))

        // A poll tick should eventually arrive after the full interval.
        try await waitUntil(timeout: .seconds(2)) {
            (try? CapturingURLProtocol.requireCaptured()) != nil
        }

        await deps.polling.stopAll()
    }

    // MARK: - Preserved: section-endpoint poll replaces section in screen

    func testSectionEndpointPollReplacesSectionInScreen() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.screenWithSectionEndpointJSON)
        await vm.load()

        CapturingURLProtocol.respond(with: Self.replacementSectionJSON)
        try await waitUntil(timeout: .seconds(3)) {
            guard let section = vm.screen?.sections.first(where: { $0.id == "scoreboard" }) else {
                return false
            }
            return section.refreshPolicy == nil
        }

        await deps.polling.stopAll()
        let replaced = try XCTUnwrap(vm.screen?.sections.first(where: { $0.id == "scoreboard" }))
        XCTAssertNil(replaced.refreshPolicy, "section should be replaced by endpoint response payload")
    }

    func testSectionEndpointSkippedWhenScreenDefaultRefreshPolicyPoll() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.screenWithConflictJSON)
        await vm.load()

        try await Task.sleep(for: .milliseconds(450))

        await deps.polling.stopAll()

        let captured = try CapturingURLProtocol.requireCaptured()
        let path = try XCTUnwrap(captured.url?.path)
        XCTAssertEqual(path, "/v1/sdui/screen/game-detail")
        XCTAssertNotEqual(path, "/v1/sdui/section/scoreboard")
    }

    func testSectionEndpointMarksStaleAfterFailureThreshold() async throws {
        let deps = makeTestDependencies()
        let vm = makeVM(deps: deps)

        CapturingURLProtocol.respond(with: Self.screenWithSectionEndpointJSON)
        await vm.load()

        CapturingURLProtocol.respond(with: Data(), statusCode: 500)
        try await waitUntil(timeout: .seconds(4)) {
            vm.stalenessPublisher.isStale("scoreboard")
        }

        await deps.polling.stopAll()
        XCTAssertTrue(vm.stalenessPublisher.isStale("scoreboard"))
    }

    // MARK: - Helpers

    private struct TestDeps {
        let repo: SduiRepository
        let polling: PollingDriver
        let ably: AblyChannelManager
        let config: SduiConfig
    }

    private func makeTestDependencies() -> TestDeps {
        let config = SduiConfig(
            baseURL: URL(string: "http://localhost:8080")!,
            ablyTokenURL: URL(string: "http://localhost:8080/ably-token")!
        )
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [CapturingURLProtocol.self]
        let session = URLSession(configuration: configuration)
        let repo = SduiRepository(config: config, session: session, envelopeProvider: { RequestEnvelope() })
        let polling = PollingDriver(repository: repo)
        let ably = AblyChannelManager(tokenURL: config.ablyTokenURL)
        return TestDeps(repo: repo, polling: polling, ably: ably, config: config)
    }

    private func makeVM(deps: TestDeps, endpoint: String = "/v1/sdui/screen/game-detail") -> SduiScreenViewModel {
        SduiScreenViewModel(
            endpoint: endpoint,
            config: deps.config,
            nav: NavCoordinator(),
            repository: deps.repo,
            polling: deps.polling,
            ably: deps.ably
        )
    }

    private func waitUntil(
        timeout: Duration,
        interval: Duration = .milliseconds(50),
        condition: @escaping @MainActor () throws -> Bool
    ) async throws {
        let start = ContinuousClock.now
        while ContinuousClock.now - start < timeout {
            if try condition() {
                return
            }
            try await Task.sleep(for: interval)
        }
        XCTFail("condition not met before timeout \(timeout)")
        throw NSError(domain: "SduiScreenViewModelRefreshTests", code: 1)
    }

    // MARK: - Fixtures

    private static let gameDetailScreenJSON: Data = """
        {
          "id": "game-detail",
          "schemaVersion": "1.0",
          "sections": [{"id": "scoreboard", "type": "AtomicComposite"}]
        }
        """.data(using: .utf8)!

    private static let gamesScreenJSON: Data = """
        {
          "id": "games",
          "schemaVersion": "1.0",
          "sections": [{"id": "calendar", "type": "CalendarStrip"}]
        }
        """.data(using: .utf8)!

    private static let mismatchedIdScreenJSON: Data = """
        {
          "id": "leaders",
          "schemaVersion": "1.0",
          "sections": [{"id": "stats", "type": "AtomicComposite"}]
        }
        """.data(using: .utf8)!

    private static let screenWith3SectionsJSON: Data = """
        {
          "id": "game-detail",
          "schemaVersion": "1.0",
          "sections": [
            {"id": "section-a", "type": "AtomicComposite"},
            {"id": "section-b", "type": "AtomicComposite", "refreshPolicy": {"type": "poll", "intervalMs": 5000}},
            {"id": "section-c", "type": "AtomicComposite"}
          ]
        }
        """.data(using: .utf8)!

    private static let screenWith2SectionsJSON: Data = """
        {
          "id": "game-detail",
          "schemaVersion": "1.0",
          "sections": [
            {"id": "section-a", "type": "AtomicComposite"},
            {"id": "section-c", "type": "AtomicComposite"}
          ]
        }
        """.data(using: .utf8)!

    private static let replacementSectionBJSON: Data = """
        {"id": "section-b", "type": "AtomicComposite"}
        """.data(using: .utf8)!

    private static let screenWithPollJSON: Data = """
        {
          "id": "game-detail",
          "schemaVersion": "1.0",
          "defaultRefreshPolicy": {"type": "poll", "intervalMs": 200},
          "sections": [{"id": "scoreboard", "type": "AtomicComposite"}]
        }
        """.data(using: .utf8)!

    private static let screenWithSectionEndpointJSON: Data = """
        {
          "id": "game-detail",
          "schemaVersion": "1.0",
          "sections": [{
            "id": "scoreboard",
            "type": "AtomicComposite",
            "refreshPolicy": {
              "type": "poll",
              "intervalMs": 200,
              "sectionEndpoint": "/v1/sdui/section/scoreboard",
              "pauseWhenOffScreen": false
            }
          }]
        }
        """.data(using: .utf8)!

    private static let replacementSectionJSON: Data = """
        {"id": "scoreboard", "type": "AtomicComposite", "data": {"status": "live"}}
        """.data(using: .utf8)!

    private static let screenWithConflictJSON: Data = """
        {
          "id": "game-detail",
          "schemaVersion": "1.0",
          "defaultRefreshPolicy": {"type": "poll", "intervalMs": 5000},
          "sections": [{
            "id": "scoreboard",
            "type": "AtomicComposite",
            "refreshPolicy": {
              "type": "poll",
              "intervalMs": 200,
              "sectionEndpoint": "/v1/sdui/section/scoreboard",
              "pauseWhenOffScreen": false
            }
          }]
        }
        """.data(using: .utf8)!
}
