import XCTest
@testable import SduiCore

final class CalendarStripViewMathTests: XCTestCase {

    private func loadFixture(_ name: String) throws -> Data {
        let url = try XCTUnwrap(
            Bundle.module.url(forResource: name, withExtension: "json", subdirectory: "Fixtures"),
            "Missing fixture Fixtures/\(name).json"
        )
        return try Data(contentsOf: url)
    }

    func testGenerateCells_clampsToMinAndMax() {
        let cells = CalendarStripView.generateCells(
            minISO: "2026-05-25",
            maxISO: "2026-05-27",
            fallbackCenter: "2026-05-26"
        )

        XCTAssertEqual(cells.map(\.isoString), ["2026-05-25", "2026-05-26", "2026-05-27"])
    }

    func testGenerateCells_includesLeapDay() {
        let cells = CalendarStripView.generateCells(
            minISO: "2024-02-28",
            maxISO: "2024-03-01",
            fallbackCenter: "2024-02-29"
        )

        XCTAssertEqual(cells.map(\.isoString), ["2024-02-28", "2024-02-29", "2024-03-01"])
    }

    func testCellAccessibilityLabel_includesDefaultAndSelectedState() {
        let cell = CalendarCellData(year: 2026, month: 5, day: 26)

        let label = CalendarStripView.cellAccessibilityLabel(
            cell: cell,
            isDefault: true,
            isSelected: true
        )

        XCTAssertTrue(label.contains("default date"))
        XCTAssertTrue(label.contains("selected"))
    }

    func testIsoSelectedDateIdentity_fromFixture() throws {
        let data = try loadFixture("calendar-strip")
        let section = try newJSONDecoder().decode(Section.self, from: data)
        let selected = try XCTUnwrap(section.data?.selectedDate)
        let minISO = section.data?.minDate
        let maxISO = section.data?.maxDate

        let cells = CalendarStripView.generateCells(
            minISO: minISO,
            maxISO: maxISO,
            fallbackCenter: selected
        )

        XCTAssertTrue(cells.contains(where: { $0.isoString == selected }))
    }
}
