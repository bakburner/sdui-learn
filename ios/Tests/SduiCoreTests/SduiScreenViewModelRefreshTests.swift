import XCTest
@testable import SduiCore

@MainActor
final class SduiScreenViewModelRefreshTests: XCTestCase {

    override func setUp() {
        super.setUp()
        CapturingURLProtocol.reset()
    }

    func testSectionEndpointPollReplacesSectionInScreen() async throws {
        let dependencies = makeTestDependencies()
        let vm = makeVM(
            repo: dependencies.repo,
            polling: dependencies.polling,
            ably: dependencies.ably,
            config: dependencies.config
        )

        CapturingURLProtocol.respond(with: Self.screenWithSectionEndpointJSON)
        await vm.load()

        CapturingURLProtocol.respond(with: Self.replacementSectionJSON)
        try await waitUntil(timeout: .seconds(3)) {
            guard let section = vm.screen?.sections.first(where: { $0.id == "scoreboard" }) else {
                return false
            }
            return section.refreshPolicy == nil
        }

        await dependencies.polling.stopAll()
        let replaced = try XCTUnwrap(vm.screen?.sections.first(where: { $0.id == "scoreboard" }))
        XCTAssertNil(replaced.refreshPolicy, "section should be replaced by endpoint response payload")
    }

    func testSectionEndpointSkippedWhenScreenDefaultRefreshPolicyPoll() async throws {
        let dependencies = makeTestDependencies()
        let vm = makeVM(
            repo: dependencies.repo,
            polling: dependencies.polling,
            ably: dependencies.ably,
            config: dependencies.config
        )

        CapturingURLProtocol.respond(with: Self.screenWithConflictJSON)
        await vm.load()

        // Wait longer than section interval; if sectionEndpoint poll ran,
        // the protocol would capture the section endpoint request.
        try await Task.sleep(for: .milliseconds(450))

        await dependencies.polling.stopAll()

        let captured = try CapturingURLProtocol.requireCaptured()
        let path = try XCTUnwrap(captured.url?.path)
        XCTAssertEqual(path, "/v1/sdui/screen/game-detail")
        XCTAssertNotEqual(path, "/v1/sdui/section/scoreboard")
    }

    func testSectionEndpointMarksStaleAfterFailureThreshold() async throws {
        let dependencies = makeTestDependencies()
        let vm = makeVM(
            repo: dependencies.repo,
            polling: dependencies.polling,
            ably: dependencies.ably,
            config: dependencies.config
        )

        CapturingURLProtocol.respond(with: Self.screenWithSectionEndpointJSON)
        await vm.load()

        CapturingURLProtocol.respond(with: Data(), statusCode: 500)
        try await waitUntil(timeout: .seconds(4)) {
            vm.stalenessPublisher.isStale("scoreboard")
        }

        await dependencies.polling.stopAll()
        XCTAssertTrue(vm.stalenessPublisher.isStale("scoreboard"))
    }

    // MARK: - Helpers

    private func makeTestDependencies() -> (repo: SduiRepository, polling: PollingDriver, ably: AblyChannelManager, config: SduiConfig) {
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
        return (repo, polling, ably, config)
    }

    private func makeVM(repo: SduiRepository, polling: PollingDriver, ably: AblyChannelManager, config: SduiConfig) -> SduiScreenViewModel {
        SduiScreenViewModel(
            endpoint: "/v1/sdui/screen/game-detail",
            config: config,
            nav: NavCoordinator(),
            repository: repo,
            polling: polling,
            ably: ably
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
