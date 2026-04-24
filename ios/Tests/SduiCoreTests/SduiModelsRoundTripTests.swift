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
            screen.sections.contains(where: isLiveGamePanelComposite),
            "live game detail must include a linescore-bound AtomicComposite"
        )
    }

    /// A live GamePanel card is now a server-composed AtomicComposite whose
    /// refreshPolicy is SSE on a `{gameId}:linescore` channel. That channel
    /// suffix is part of the wire contract and is a stable marker for the
    /// migrated section shape.
    private func isLiveGamePanelComposite(_ section: Section) -> Bool {
        guard section.type == "AtomicComposite" else { return false }
        guard let channel = section.refreshPolicy?.channel else { return false }
        return channel.hasSuffix(":linescore")
    }

    func testGameDetailFinalDecodes() throws {
        let data = try loadFixture("game-detail-final")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty)
    }

    func testGameDetailPreDecodes() throws {
        let data = try loadFixture("game-detail-pre")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty, "pre-game screen should contain sections")
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

    /// Locks the rebuilt For You shape — carousel of game-card composites,
    /// featured lead card, VOD playlist grouped-list — against the generated
    /// Swift models. Tripping this test means the composer is emitting
    /// something the decoder can't parse.
    func testForYouDecodes() throws {
        let data = try loadFixture("for-you")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty, "For You screen should contain sections")
        XCTAssertTrue(
            screen.sections.contains(where: { $0.type == "AtomicComposite" }),
            "For You must include AtomicComposite sections (carousel, featured game, VOD playlist)"
        )
        XCTAssertTrue(
            screen.sections.contains(where: { ($0.id ?? "").hasPrefix("featured-game-") }),
            "For You must include a featured-game composite"
        )
    }

    func testRefreshPolicyFieldsSurviveRoundTrip() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        let gamePanel = try XCTUnwrap(screen.sections.first(where: isLiveGamePanelComposite))
        XCTAssertNotNil(gamePanel.refreshPolicy, "Live game panel must declare a refresh policy")
        XCTAssertNotNil(gamePanel.dataBinding, "Live game panel must declare data bindings")
    }

    func testPauseWhenOffScreenDecodesFromFixture() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        let gamePanel = try XCTUnwrap(screen.sections.first(where: isLiveGamePanelComposite))
        let policy = try XCTUnwrap(gamePanel.refreshPolicy)
        XCTAssertEqual(policy.pauseWhenOffScreen, false,
                       "Live game panel SSE section should have pauseWhenOffScreen=false")
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

    // MARK: - Variant decode coverage

    func testSelectVariantValuesDecode() throws {
        for raw in ["dropdown", "chips"] {
            let json = """
            {
              "fieldId": "season",
              "fieldType": "select",
              "label": "Season",
              "stateKey": "scheduleSeason",
              "variant": "\(raw)"
            }
            """.data(using: .utf8)!
            let field = try newJSONDecoder().decode(FormField.self, from: json)
            XCTAssertEqual(field.variant?.rawValue, raw,
                           "SelectVariant '\(raw)' must decode as the matching enum case")
        }
    }

    func testLiveClockLeafDecodes() throws {
        let data = try loadFixture("live-clock-leaf")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty)
        let section = try XCTUnwrap(screen.sections.first)
        XCTAssertEqual(section.type, "AtomicComposite")
        let root = try XCTUnwrap(section.data?.ui)
        let kids = try XCTUnwrap(root.children)
        XCTAssertEqual(kids.count, 3)
        XCTAssertTrue(kids.allSatisfy { $0.type == "LiveClock" },
                      "every child in the fixture should be a LiveClock leaf")
        let running = try XCTUnwrap(kids.first(where: { $0.id == "clock-running-down" }))
        XCTAssertEqual(running.snapshotSeconds, 142)
        XCTAssertEqual(running.isRunning, true)
        XCTAssertEqual(running.tickDirection, .down)
        XCTAssertEqual(running.stopAtSeconds, 0)
        XCTAssertEqual(running.format, .mSs)
        XCTAssertNotNil(running.snapshotAt, "snapshotAt must decode as a Date")
        let up = try XCTUnwrap(kids.first(where: { $0.id == "clock-running-up" }))
        XCTAssertEqual(up.tickDirection, .up)
        XCTAssertEqual(up.format, .mmSs)
    }

    func testSelectVariantMissingDecodesAsNil() throws {
        let json = """
        {
          "fieldId": "season",
          "fieldType": "select",
          "label": "Season",
          "stateKey": "scheduleSeason"
        }
        """.data(using: .utf8)!
        let field = try newJSONDecoder().decode(FormField.self, from: json)
        XCTAssertNil(field.variant,
                     "Absent variant must decode as nil; renderer is responsible for the 'dropdown' fallback")
    }
}
