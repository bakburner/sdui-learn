import XCTest
@testable import SduiCore

final class ImpressionTrackerTests: XCTestCase {

    func testOncePerScreenDedupesRepeats() async {
        let tracker = ImpressionTracker()
        let policy = ImpressionPolicy(dedup: .oncePerScreen, intervalMS: nil, threshold: nil)

        let first = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        let second = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        let third = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)

        XCTAssertTrue(first)
        XCTAssertFalse(second)
        XCTAssertFalse(third)
    }

    func testNoneDedupFiresEveryTime() async {
        let tracker = ImpressionTracker()
        let policy = ImpressionPolicy(dedup: ImpressionDedup.none, intervalMS: nil, threshold: nil)

        for _ in 0..<5 {
            let fire = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
            XCTAssertTrue(fire)
        }
    }

    func testOncePerIntervalRespectsWindow() async {
        let fakeClock = FakeClock(Date(timeIntervalSince1970: 1_000))
        let tracker = ImpressionTracker(clock: { fakeClock.now })
        let policy = ImpressionPolicy(dedup: .oncePerInterval, intervalMS: 1000, threshold: nil)

        let first = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        XCTAssertTrue(first)

        fakeClock.advance(by: 0.5)
        let inside = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        XCTAssertFalse(inside)

        fakeClock.advance(by: 0.6) // total 1.1s after first
        let outside = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        XCTAssertTrue(outside)
    }

    func testKeyScopingByEventName() async {
        let tracker = ImpressionTracker()
        let policy = ImpressionPolicy(dedup: .oncePerScreen, intervalMS: nil, threshold: nil)

        let a = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        let b = await tracker.shouldFire(sectionID: "hero", event: "click", policy: policy)

        XCTAssertTrue(a)
        XCTAssertTrue(b, "Different events under same section should each fire once")
    }

    func testResetScreenClearsDedup() async {
        let tracker = ImpressionTracker()
        let policy = ImpressionPolicy(dedup: .oncePerScreen, intervalMS: nil, threshold: nil)

        _ = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)
        await tracker.resetScreen()
        let second = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: policy)

        XCTAssertTrue(second)
    }

    func testDefaultDedupIsOncePerScreen() async {
        let tracker = ImpressionTracker()

        let first = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: nil)
        let second = await tracker.shouldFire(sectionID: "hero", event: "impression", policy: nil)

        XCTAssertTrue(first)
        XCTAssertFalse(second)
    }
}

/// Thread-safe mutable clock for tests. `@Sendable` closures can't capture
/// a local `var` by reference, so we wrap the mutable time in a reference
/// type whose pointer is captured as an immutable `let`. Test code only
/// touches `now`/`advance(by:)` from the test's executor, so `@unchecked`
/// Sendable is safe here.
private final class FakeClock: @unchecked Sendable {
    var now: Date
    init(_ start: Date) { self.now = start }
    func advance(by seconds: TimeInterval) { now = now.addingTimeInterval(seconds) }
}
