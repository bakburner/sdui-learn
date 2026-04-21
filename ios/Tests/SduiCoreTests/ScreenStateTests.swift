import XCTest
@testable import SduiCore

final class ScreenStateTests: XCTestCase {

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
        state.apply(operation: .toggle, key: "flag", value: nil)
        XCTAssertEqual(state.getBool("flag"), true)

        state.apply(operation: .toggle, key: "flag", value: nil)
        XCTAssertEqual(state.getBool("flag"), false)
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
}
