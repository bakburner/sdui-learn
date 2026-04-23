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
            screen.sections.contains(where: { $0.type == "GamePanel" }),
            "live game detail must include a GamePanel section"
        )
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
        XCTAssertTrue(
            screen.sections.contains(where: { $0.type == "GamePanel" }),
            "pre-game screen must include a GamePanel section"
        )
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

    /// Locks the rebuilt For You shape — carousel of GamePanel SectionSlots,
    /// featured lead card, VOD playlist grouped-list — against the generated
    /// Swift models. Tripping this test means the composer is emitting
    /// something the decoder can't parse.
    func testForYouDecodes() throws {
        let data = try loadFixture("for-you")
        let screen = try SduiModels(data: data)
        XCTAssertFalse(screen.sections.isEmpty, "For You screen should contain sections")
        XCTAssertTrue(
            screen.sections.contains(where: { $0.type == "GamePanel" }),
            "For You must include at least one featured GamePanel"
        )
        XCTAssertTrue(
            screen.sections.contains(where: { $0.type == "AtomicComposite" }),
            "For You must include AtomicComposite sections (carousel + VOD playlist)"
        )
    }

    func testRefreshPolicyFieldsSurviveRoundTrip() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        let gamePanel = try XCTUnwrap(screen.sections.first(where: { $0.type == "GamePanel" }))
        XCTAssertNotNil(gamePanel.refreshPolicy, "GamePanel must declare a refresh policy")
        XCTAssertNotNil(gamePanel.dataBinding, "GamePanel must declare data bindings")
    }

    func testPauseWhenOffScreenDecodesFromFixture() throws {
        let data = try loadFixture("game-detail-live")
        let screen = try SduiModels(data: data)
        let gamePanel = try XCTUnwrap(screen.sections.first(where: { $0.type == "GamePanel" }))
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

    // MARK: - Variant decode coverage

    // GamePanel data decodes through the union `DataClass` type that
    // quicktype synthesizes for `Section.data` across every section
    // shape — there is no dedicated `GamePanelData` type in the
    // generated models.

    // `TeamData` in the generated models requires teamId (Int), teamCity,
    // teamName, teamTricode, and score. These inline JSON blobs supply
    // the full required shape even though only `variant` is under test.

    private static let bosTeam = """
    { "teamId": 1610612738, "teamCity": "Boston", "teamName": "Celtics", "teamTricode": "BOS", "score": 0 }
    """

    private static let lalTeam = """
    { "teamId": 1610612747, "teamCity": "Los Angeles", "teamName": "Lakers", "teamTricode": "LAL", "score": 0 }
    """

    func testGamePanelVariantFeaturedDecodes() throws {
        let json = """
        {
          "gameId": "0042300102",
          "gameStatus": 1,
          "homeTeam": \(Self.bosTeam),
          "awayTeam": \(Self.lalTeam),
          "variant": "featured"
        }
        """.data(using: .utf8)!
        let data = try newJSONDecoder().decode(DataClass.self, from: json)
        XCTAssertEqual(data.variant, .featured, "featured GamePanelVariant must decode")
    }

    func testGamePanelVariantStandardDecodes() throws {
        let json = """
        {
          "gameId": "0042300102",
          "gameStatus": 3,
          "homeTeam": \(Self.bosTeam),
          "awayTeam": \(Self.lalTeam),
          "variant": "standard"
        }
        """.data(using: .utf8)!
        let data = try newJSONDecoder().decode(DataClass.self, from: json)
        XCTAssertEqual(data.variant, .standard)
    }

    func testGamePanelVariantMissingDecodesAsNil() throws {
        let json = """
        {
          "gameId": "0042300102",
          "gameStatus": 1,
          "homeTeam": \(Self.bosTeam),
          "awayTeam": \(Self.lalTeam)
        }
        """.data(using: .utf8)!
        let data = try newJSONDecoder().decode(DataClass.self, from: json)
        XCTAssertNil(data.variant,
                     "Absent variant must decode as nil; renderer is responsible for the 'standard' fallback")
    }

    func testSelectVariantValuesDecode() throws {
        for raw in ["dropdown", "chips", "segmented"] {
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
