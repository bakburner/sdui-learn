import XCTest
@testable import SduiCore

final class CalendarMonthListSchemaDecodingTests: XCTestCase {

    private func loadFixture(_ name: String) throws -> Data {
        let url = try XCTUnwrap(
            Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures"),
            "Missing fixture Fixtures/\(name).json"
        )
        return try Data(contentsOf: url)
    }

    func testCalendarMonthListFixtureDecodesWithDateMetadata() throws {
        let data = try loadFixture("calendar-month-list")
        let section = try newJSONDecoder().decode(Section.self, from: data)

        XCTAssertEqual(section.id, "server-games-calendar__type-CalendarMonthList")
        XCTAssertEqual(section.type, "CalendarMonthList")
        XCTAssertEqual(section.contentSourceID, "server:games-calendar")
        XCTAssertEqual(section.analyticsID, "games_calendar_month_list")
        XCTAssertEqual(section.data?.stateKey, "calendar_selected_date")
        XCTAssertEqual(section.data?.selectedDate, "2026-05-26")
        XCTAssertEqual(section.data?.defaultDate, "2026-05-26")
        XCTAssertEqual(section.data?.minDate, "2025-10-01")
        XCTAssertEqual(section.data?.maxDate, "2026-06-30")

        let metadata = try XCTUnwrap(section.data?.dateMetadata)
        XCTAssertEqual(metadata["2026-05-26"]?.gameCount, 3)
        XCTAssertEqual(metadata["2026-05-26"]?.hasTeamGame, false)

        XCTAssertEqual(section.data?.onDateSelected?.trigger, .onActivate)
        XCTAssertEqual(section.data?.onDateSelected?.type, .navigate)
        XCTAssertEqual(section.data?.onDateSelected?.targetURI, "nba://games?date={{calendar_selected_date}}")
    }

    func testCalendarStripExpandedActionDecodesWhenPresent() throws {
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
            },
            "expandedAction": {
              "trigger": "onActivate",
              "type": "navigate",
              "targetUri": "nba://calendar"
            }
          }
        }
        """.data(using: .utf8)!

        let section = try newJSONDecoder().decode(Section.self, from: payload)
        XCTAssertEqual(section.data?.expandedAction?.trigger, .onActivate)
        XCTAssertEqual(section.data?.expandedAction?.type, .navigate)
        XCTAssertEqual(section.data?.expandedAction?.targetURI, "nba://calendar")
    }

    func testCalendarStripExpandedActionDecodesWhenAbsent() throws {
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
        XCTAssertNil(section.data?.expandedAction)
    }
}
