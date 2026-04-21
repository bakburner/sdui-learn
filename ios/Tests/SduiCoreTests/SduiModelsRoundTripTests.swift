import XCTest
@testable import SduiCore

/// Phase A end-of-phase gate (per `docs/sdui-refapp-implementation-plan.md` §0.7).
///
/// Round-trip decodes representative server responses against the generated
/// `SduiModels.swift`. Catches schema/union decoding breakage before any
/// downstream phase lands on top.
final class SduiModelsRoundTripTests: XCTestCase {

    private func loadFixture(_ name: String) throws -> Data {
        let url = try XCTUnwrap(
            Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures"),
            "Missing fixture Fixtures/\(name).json"
        )
        return try Data(contentsOf: url)
    }

    func testGameDetailLiveDecodes() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)

        XCTAssertEqual(screen.id, "game-detail-0042300102")
        XCTAssertEqual(screen.schemaVersion, "1.0")
        XCTAssertFalse(screen.sections.isEmpty, "live game detail should have at least one section")
        XCTAssertTrue(
            screen.sections.contains(where: { $0.type == .gamePanel }),
            "live game detail must include a GamePanel section"
        )
    }

    func testGameDetailFinalDecodes() throws {
        let data = try loadFixture("game-detail-final")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty)
    }

    func testScoreboardLiveDecodes() throws {
        let data = try loadFixture("scoreboard-live")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty, "scoreboard should contain sections")
    }

    func testBoxscoreDecodes() throws {
        let data = try loadFixture("boxscore")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty, "boxscore screen should contain sections")
    }

    func testRefreshPolicyFieldsSurviveRoundTrip() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        let gamePanel = try XCTUnwrap(screen.sections.first(where: { $0.type == .gamePanel }))
        XCTAssertNotNil(gamePanel.refreshPolicy, "GamePanel must declare a refresh policy")
        XCTAssertNotNil(gamePanel.dataBinding, "GamePanel must declare data bindings")
    }

    func testPauseWhenOffScreenDecodesFromFixture() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        let gamePanel = try XCTUnwrap(screen.sections.first(where: { $0.type == .gamePanel }))
        let policy = try XCTUnwrap(gamePanel.refreshPolicy)
        XCTAssertEqual(policy.pauseWhenOffScreen, false,
                       "GamePanel SSE section should have pauseWhenOffScreen=false")
    }

    func testPauseWhenOffScreenDefaultsToNilWhenAbsent() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        // Find a section whose refreshPolicy does NOT include pauseWhenOffScreen
        let pollSection = screen.sections.first(where: {
            $0.refreshPolicy?.type == .poll
        })
        if let pollPolicy = pollSection?.refreshPolicy {
            XCTAssertNil(pollPolicy.pauseWhenOffScreen,
                         "Sections without explicit pauseWhenOffScreen should decode as nil (client defaults to true)")
        }
    }
