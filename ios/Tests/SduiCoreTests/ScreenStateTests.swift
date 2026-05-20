import XCTest
@testable import SduiCore

final class ScreenStateTests: XCTestCase {

    override func tearDown() {
        ScreenStateLogProbe.warningSink = nil
        super.tearDown()
    }

    private func captureWarnings(_ body: () -> Void) -> [String] {
        var warnings: [String] = []
        ScreenStateLogProbe.warningSink = { warnings.append($0) }
        body()
        ScreenStateLogProbe.warningSink = nil
        return warnings
    }

    func testSetAndGetRoundTrip() {
        let state = ScreenState()
        state.set("name", value: "LeBron")
        XCTAssertEqual(state.getString("name"), "LeBron")

        state.set("score", value: 42)
        XCTAssertEqual(state.getInt("score"), 42)

        state.set("visible", value: true)
        XCTAssertEqual(state.getBool("visible"), true)
    }

    func testSetNilRemovesKey() {
        let state = ScreenState()
        state.set("k", value: "v")
        state.set("k", value: nil as Any?)
        XCTAssertNil(state.get("k"))
    }

    func testMutateToggle() {
        let state = ScreenState()
        state.set("flag", value: true)
        state.apply(operation: .toggle, key: "flag", value: nil)
        XCTAssertEqual(state.getBool("flag"), false)

        state.apply(operation: .toggle, key: "flag", value: nil)
        XCTAssertEqual(state.getBool("flag"), true)
    }

    func testMutateIncrementPreservesIntegerType() {
        let state = ScreenState()
        state.set("counter", value: 5)
        state.apply(operation: .increment, key: "counter", value: 3)

        XCTAssertEqual(state.getInt("counter"), 8)
    }

    func testMutateIncrementPromotesToDouble() throws {
        let state = ScreenState()
        state.set("meter", value: 1.5)
        state.apply(operation: .increment, key: "meter", value: 0.25)

        XCTAssertEqual(try XCTUnwrap(state.getDouble("meter")), 1.75, accuracy: 0.0001)
    }

    func testMutateAppendBuildsArray() {
        let state = ScreenState()
        state.apply(operation: .append, key: "items", value: "a")
        state.apply(operation: .append, key: "items", value: "b")

        let items = state.get("items") as? [Any]
        XCTAssertEqual(items?.count, 2)
        XCTAssertEqual(items?.first as? String, "a")
    }

    func testMutateAppendConcatenatesStrings() {
        let state = ScreenState()
        state.set("note", value: "hello")
        state.apply(operation: .append, key: "note", value: " world")

        XCTAssertEqual(state.getString("note"), "hello world")
    }

    func testMutateNilOperationBehavesAsSet() {
        let state = ScreenState()
        state.apply(operation: nil, key: "x", value: "first")
        XCTAssertEqual(state.getString("x"), "first")

        state.apply(operation: nil, key: "x", value: "second")
        XCTAssertEqual(state.getString("x"), "second")
    }

    func testResetClearsAllKeys() {
        let state = ScreenState()
        state.set("a", value: "1")
        state.set("b", value: "2")

        state.reset()

        XCTAssertNil(state.get("a"))
        XCTAssertNil(state.get("b"))
    }

    func testMutateToggleNonBoolWarnsAndNoOps() {
        let state = ScreenState()
        state.set("flag", value: "yes")

        let warnings = captureWarnings {
            state.apply(operation: .toggle, key: "flag", value: nil)
        }

        XCTAssertEqual(state.getString("flag"), "yes")
        XCTAssertEqual(warnings.count, 1)
        XCTAssertTrue(warnings[0].contains("mutate toggle noop"))
    }

    func testMutateToggleMissingWarnsAndNoOps() {
        let state = ScreenState()

        let warnings = captureWarnings {
            state.apply(operation: .toggle, key: "flag", value: nil)
        }

        XCTAssertNil(state.get("flag"))
        XCTAssertEqual(warnings.count, 1)
        XCTAssertTrue(warnings[0].contains("mutate toggle noop"))
    }

    func testMutateIncrementNonNumericWarnsAndNoOps() {
        let state = ScreenState()
        state.set("counter", value: "two")

        let warnings = captureWarnings {
            state.apply(operation: .increment, key: "counter", value: 3)
        }

        XCTAssertEqual(state.getString("counter"), "two")
        XCTAssertEqual(warnings.count, 1)
        XCTAssertTrue(warnings[0].contains("mutate increment noop"))
    }

    func testMutateIncrementMissingWarnsAndNoOps() {
        let state = ScreenState()

        let warnings = captureWarnings {
            state.apply(operation: .increment, key: "counter", value: 3)
        }

        XCTAssertNil(state.get("counter"))
        XCTAssertEqual(warnings.count, 1)
        XCTAssertTrue(warnings[0].contains("mutate increment noop"))
    }

    func testMutateAppendIncompatibleWarnsAndNoOps() {
        let state = ScreenState()
        state.set("items", value: true)

        let warnings = captureWarnings {
            state.apply(operation: .append, key: "items", value: "next")
        }

        XCTAssertEqual(state.getBool("items"), true)
        XCTAssertEqual(warnings.count, 1)
        XCTAssertTrue(warnings[0].contains("mutate append noop"))
    }

    func testMutateAppendNoOpsWhenStringCurrentReceivesNonStringIncoming() {
        let state = ScreenState()
        state.set("note", value: "hello")

        state.apply(operation: .append, key: "note", value: 2)

        XCTAssertEqual(state.getString("note"), "hello")
    }
}
