import XCTest
@testable import SduiCore

final class DataBindingApplierTests: XCTestCase {

    func testDotPathTargetReplacesValue() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.homeTeam.score", targetPath: "homeTeam.score", transform: nil)],
            stringKeys: nil
        )
        let current: [String: Any] = ["homeTeam": ["score": 100, "name": "BOS"]]
        let incoming: [String: Any] = ["homeTeam": ["score": 102]]

        let result = applier.applyBindings(
            currentData: current,
            incomingMessage: incoming,
            dataBinding: binding,
            sectionID: "game_1",
            correlationId: nil,
            stringTable: nil
        )

        let homeTeam = result["homeTeam"] as? [String: Any]
        XCTAssertEqual(homeTeam?["score"] as? Int, 102)
        XCTAssertEqual(homeTeam?["name"] as? String, "BOS", "Unbound fields should persist")
    }

    func testArrayIndexSourcePath() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.teams[1].score", targetPath: "away.score", transform: nil)],
            stringKeys: nil
        )
        let current: [String: Any] = ["away": ["score": 0]]
        let incoming: [String: Any] = [
            "teams": [
                ["score": 44],
                ["score": 50]
            ]
        ]

        let result = applier.applyBindings(
            currentData: current,
            incomingMessage: incoming,
            dataBinding: binding,
            sectionID: "s",
            correlationId: nil,
            stringTable: nil
        )

        XCTAssertEqual((result["away"] as? [String: Any])?["score"] as? Int, 50)
    }

    func testMissingPathPreservesCurrentValue() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.nope", targetPath: "homeTeam.score", transform: nil)],
            stringKeys: nil
        )
        let current: [String: Any] = ["homeTeam": ["score": 99]]

        let result = applier.applyBindings(
            currentData: current,
            incomingMessage: [:],
            dataBinding: binding,
            sectionID: "s",
            correlationId: nil,
            stringTable: nil
        )

        XCTAssertEqual((result["homeTeam"] as? [String: Any])?["score"] as? Int, 99)
        XCTAssertEqual(applier.missCount(sectionID: "s", sourcePath: "$.nope"), 1)
    }

    func testMissCounterIncrementsUntilThreshold() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.nope", targetPath: "x", transform: nil)],
            stringKeys: nil
        )

        for i in 1...DataBindingApplier.missThreshold {
            _ = applier.applyBindings(
                currentData: [:],
                incomingMessage: [:],
                dataBinding: binding,
                sectionID: "s",
                correlationId: nil,
                stringTable: nil
            )
            XCTAssertEqual(applier.missCount(sectionID: "s", sourcePath: "$.nope"), i)
        }
    }

    func testSuccessResetsMissCounter() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.score", targetPath: "score", transform: nil)],
            stringKeys: nil
        )

        _ = applier.applyBindings(
            currentData: [:],
            incomingMessage: [:],
            dataBinding: binding,
            sectionID: "s",
            correlationId: nil,
            stringTable: nil
        )
        XCTAssertEqual(applier.missCount(sectionID: "s", sourcePath: "$.score"), 1)

        _ = applier.applyBindings(
            currentData: [:],
            incomingMessage: ["score": 7],
            dataBinding: binding,
            sectionID: "s",
            correlationId: nil,
            stringTable: nil
        )
        XCTAssertEqual(applier.missCount(sectionID: "s", sourcePath: "$.score"), 0)
    }

    func testStringKeyOverridesBoundValue() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.statusKey", targetPath: "statusText", transform: nil)],
            stringKeys: ["statusText": "game.status.live"]
        )
        let incoming: [String: Any] = ["statusKey": "raw"]
        let table = ["game.status.live": "LIVE"]

        let result = applier.applyBindings(
            currentData: ["statusText": "SCHEDULED"],
            incomingMessage: incoming,
            dataBinding: binding,
            sectionID: "s",
            correlationId: nil,
            stringTable: table
        )

        XCTAssertEqual(result["statusText"] as? String, "LIVE")
    }

    func testLiveClockSnapshotTransformAnchorsIncomingClock() {
        let applier = DataBindingApplier()
        let binding = DataBinding(
            bindings: [DataBindingPath(sourcePath: "$.clock", targetPath: "content.clock", transform: .liveClockSnapshot)],
            stringKeys: nil
        )
        // Mirrors the production NBA Ably linescore wire format:
        // scalar ISO-8601 duration string at $.clock plus sibling
        // `clockRunning` at the message root.
        let incoming: [String: Any] = [
            "clock": "PT04M32.00S",
            "clockRunning": "1"
        ]

        let result = applier.applyBindings(
            currentData: ["content": ["clock": ["snapshotSeconds": 300, "isRunning": false]]],
            incomingMessage: incoming,
            dataBinding: binding,
            sectionID: "s",
            correlationId: nil,
            stringTable: nil
        )

        let clock = (result["content"] as? [String: Any])?["clock"] as? [String: Any]
        XCTAssertEqual(clock?["snapshotSeconds"] as? Int, 272)
        XCTAssertEqual(clock?["isRunning"] as? Bool, true)
        XCTAssertNotNil(clock?["snapshotAt"] as? String)
    }
}
