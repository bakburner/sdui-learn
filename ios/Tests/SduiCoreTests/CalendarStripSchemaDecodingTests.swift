import XCTest
@testable import SduiCore

final class CalendarStripSchemaDecodingTests: XCTestCase {

    private func loadFixture(_ name: String) throws -> Data {
        let url = try XCTUnwrap(
            Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures"),
            "Missing fixture Fixtures/\(name).json"
        )
        return try Data(contentsOf: url)
    }

    func testCalendarStripFixtureDecodes() throws {
        let data = try loadFixture("calendar-strip")
        let section = try newJSONDecoder().decode(Section.self, from: data)

        XCTAssertEqual(section.id, "server-games-calendar__type-CalendarStrip")
        XCTAssertEqual(section.type, "CalendarStrip")
        XCTAssertEqual(section.contentSourceID, "server:games-calendar")
        XCTAssertEqual(section.analyticsID, "games_calendar_strip")
        XCTAssertEqual(section.accessibility?.label, "Games date picker")
        XCTAssertEqual(section.data?.stateKey, "games_selected_date")
        XCTAssertEqual(section.data?.selectedDate, "2026-05-25")
        XCTAssertEqual(section.data?.defaultDate, "2026-05-25")
        XCTAssertEqual(section.data?.minDate, "2025-10-01")
        XCTAssertEqual(section.data?.maxDate, "2026-06-30")
        XCTAssertEqual(section.data?.onDateSelected?.trigger, .onActivate)
        XCTAssertEqual(section.data?.onDateSelected?.type, .refresh)
        XCTAssertEqual(section.data?.onDateSelected?.endpoint, "/v1/sdui/screen/games")
        XCTAssertEqual(section.data?.onDateSelected?.paramBindings?["date"], "{{games_selected_date}}")
    }

    /// quicktype collapses `Section.data` `oneOf` into a single `SectionData` whose
    /// fields are all optional, so the typed decoder cannot enforce per-branch
    /// `required`. Required-field absence is detectable downstream — `CalendarStripView`
    /// renders nothing when `data.defaultDate == nil`. This test pins the codegen
    /// reality so a future change (e.g. switching to a sum type) doesn't silently
    /// alter the contract surface.
    func testCalendarStripDecodeDoesNotThrowWhenDefaultDateMissing_unionCollapse() throws {
        let payload = """
        {
          "id": "server-games-calendar__type-CalendarStrip",
          "type": "CalendarStrip",
          "data": {
            "stateKey": "games_selected_date",
            "selectedDate": "2026-05-25",
            "onDateSelected": {
              "trigger": "onActivate",
              "type": "refresh",
              "endpoint": "/v1/sdui/screen/games",
              "paramBindings": { "date": "{{games_selected_date}}" }
            }
          }
        }
        """.data(using: .utf8)!

        let section = try newJSONDecoder().decode(Section.self, from: payload)
        XCTAssertNil(section.data?.defaultDate, "defaultDate must be detectable as absent for downstream validation")
    }

    func testCalendarStripDecodeSucceedsWithoutMinAndMax() throws {
        let payload = """
        {
          "id": "server-games-calendar__type-CalendarStrip",
          "type": "CalendarStrip",
          "data": {
            "stateKey": "games_selected_date",
            "selectedDate": "2026-05-25",
            "defaultDate": "2026-05-25",
            "onDateSelected": {
              "trigger": "onActivate",
              "type": "refresh",
              "endpoint": "/v1/sdui/screen/games",
              "paramBindings": { "date": "{{games_selected_date}}" }
            }
          }
        }
        """.data(using: .utf8)!

        let section = try newJSONDecoder().decode(Section.self, from: payload)
        XCTAssertNil(section.data?.minDate)
        XCTAssertNil(section.data?.maxDate)
    }

    func testCalendarStripDecodeFailsWhenOnDateSelectedIsArray() {
        let payload = """
        {
          "id": "server-games-calendar__type-CalendarStrip",
          "type": "CalendarStrip",
          "data": {
            "stateKey": "games_selected_date",
            "selectedDate": "2026-05-25",
            "defaultDate": "2026-05-25",
            "onDateSelected": [
              {
                "trigger": "onActivate",
                "type": "refresh",
                "endpoint": "/v1/sdui/screen/games",
                "paramBindings": { "date": "{{games_selected_date}}" }
              }
            ]
          }
        }
        """.data(using: .utf8)!

        XCTAssertThrowsError(try newJSONDecoder().decode(Section.self, from: payload))
    }
}
