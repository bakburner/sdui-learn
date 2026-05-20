import XCTest
@testable import SduiCore

final class ActionTriggerCoverageTests: XCTestCase {

    func testEveryActionTriggerIsRegisteredExactlyOnce() {
        let supported = AtomicActionTriggerRegistry.supported
        let notHosted = Set(AtomicActionTriggerRegistry.notHosted.keys)
        let allTriggers = Set(ActionTrigger.allCases)

        XCTAssertEqual(supported.intersection(notHosted), [])
        XCTAssertEqual(supported.union(notHosted), allTriggers)
    }

    func testNotHostedReasonDocumentsAtomicSwipeGap() {
        XCTAssertEqual(
            AtomicActionTriggerRegistry.notHosted[.onSwipe],
            "carousel-only at ScrollContainer level"
        )
    }
}