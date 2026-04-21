import XCTest
@testable import SduiCore

// `@MainActor` sits on each method rather than the class because
// `XCTestCase` is nonisolated. `SectionVisibilityTracker` is `@MainActor`,
// so every test interaction must stay on the main actor.
final class SectionVisibilityTrackerTests: XCTestCase {

    @MainActor
    func testReportingEntersImmediately() {
        let tracker = SectionVisibilityTracker()
        tracker.report(sectionID: "scoreboard", visible: true)
        XCTAssertTrue(tracker.isVisible("scoreboard"))
    }

    @MainActor
    func testExitIsDebounced() async throws {
        let tracker = SectionVisibilityTracker(exitDebounceMS: 50)
        tracker.report(sectionID: "scoreboard", visible: true)
        tracker.report(sectionID: "scoreboard", visible: false)

        XCTAssertTrue(tracker.isVisible("scoreboard"), "Exit must be debounced")

        try await Task.sleep(nanoseconds: 120 * 1_000_000)
        XCTAssertFalse(tracker.isVisible("scoreboard"))
    }

    @MainActor
    func testBouncingBackDuringDebounceKeepsVisible() async throws {
        let tracker = SectionVisibilityTracker(exitDebounceMS: 100)
        tracker.report(sectionID: "hero", visible: true)
        tracker.report(sectionID: "hero", visible: false)

        try await Task.sleep(nanoseconds: 40 * 1_000_000)
        tracker.report(sectionID: "hero", visible: true)

        try await Task.sleep(nanoseconds: 120 * 1_000_000)
        XCTAssertTrue(tracker.isVisible("hero"))
    }

    @MainActor
    func testResetClearsEverything() async throws {
        let tracker = SectionVisibilityTracker()
        tracker.report(sectionID: "hero", visible: true)
        tracker.report(sectionID: "scoreboard", visible: true)

        tracker.reset()

        XCTAssertFalse(tracker.isVisible("hero"))
        XCTAssertFalse(tracker.isVisible("scoreboard"))
    }

    @MainActor
    func testEventStreamEmitsEntryAndExit() async throws {
        let tracker = SectionVisibilityTracker(exitDebounceMS: 25)
        let stream = tracker.events()

        let task = Task { () -> [SectionVisibilityTracker.VisibilityEvent] in
            var collected: [SectionVisibilityTracker.VisibilityEvent] = []
            for await event in stream {
                collected.append(event)
                if collected.count == 2 { break }
            }
            return collected
        }

        try await Task.sleep(nanoseconds: 10 * 1_000_000)
        tracker.report(sectionID: "hero", visible: true)
        tracker.report(sectionID: "hero", visible: false)

        let events = await task.value
        XCTAssertEqual(events, [.entered(sectionID: "hero"), .exited(sectionID: "hero")])
    }
}
