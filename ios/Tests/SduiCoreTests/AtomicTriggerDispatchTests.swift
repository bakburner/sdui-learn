import XCTest
@testable import SduiCore

final class AtomicTriggerDispatchTests: XCTestCase {

    override func tearDown() {
        AtomicActionTriggerLogProbe.debugSink = nil
        super.tearDown()
    }

    private func action(trigger: ActionTrigger, event: String) -> Action {
        Action(
            destinations: nil,
            endpoint: nil,
            event: event,
            failureFeedback: nil,
            impression: nil,
            message: nil,
            modalHeight: nil,
            onFailure: nil,
            operation: nil,
            paramBindings: nil,
            params: nil,
            presentation: nil,
            target: nil,
            targetURI: nil,
            trigger: trigger,
            type: .fireAndForget,
            value: nil,
            webURL: nil
        )
    }

    func testOnLongPressDispatchesMatchingActionsThroughBatchExecutor() {
        let actions = [
            action(trigger: .onActivate, event: "tap"),
            action(trigger: .onLongPress, event: "long-1"),
            action(trigger: .onLongPress, event: "long-2")
        ]
        var dispatchedIndividually: [String] = []
        var dispatchedBatch: [String] = []

        AtomicActionTriggerDispatcher.dispatch(
            trigger: .onLongPress,
            actions: actions,
            onAction: { dispatchedIndividually.append($0.event ?? "") },
            batchExecutor: { dispatchedBatch = $0.compactMap(\.event) }
        )

        XCTAssertEqual(dispatchedBatch, ["long-1", "long-2"])
        XCTAssertTrue(dispatchedIndividually.isEmpty)
    }

    func testOnFocusDispatchesMatchingActionsThroughBatchExecutor() {
        let actions = [
            action(trigger: .onFocus, event: "focus"),
            action(trigger: .onBlur, event: "blur")
        ]
        var dispatchedBatch: [String] = []

        AtomicActionTriggerDispatcher.dispatch(
            trigger: .onFocus,
            actions: actions,
            onAction: { _ in XCTFail("Expected batch executor path") },
            batchExecutor: { dispatchedBatch = $0.compactMap(\.event) }
        )

        XCTAssertEqual(dispatchedBatch, ["focus"])
    }

    func testOnBlurDispatchesMatchingActionsThroughBatchExecutor() {
        let actions = [
            action(trigger: .onFocus, event: "focus"),
            action(trigger: .onBlur, event: "blur")
        ]
        var dispatchedBatch: [String] = []

        AtomicActionTriggerDispatcher.dispatch(
            trigger: .onBlur,
            actions: actions,
            onAction: { _ in XCTFail("Expected batch executor path") },
            batchExecutor: { dispatchedBatch = $0.compactMap(\.event) }
        )

        XCTAssertEqual(dispatchedBatch, ["blur"])
    }

    func testOnSubmitDispatchesMatchingActionsThroughBatchExecutor() {
        let actions = [
            action(trigger: .onSubmit, event: "submit"),
            action(trigger: .onActivate, event: "activate")
        ]
        var dispatchedBatch: [String] = []

        AtomicActionTriggerDispatcher.dispatch(
            trigger: .onSubmit,
            actions: actions,
            onAction: { _ in XCTFail("Expected batch executor path") },
            batchExecutor: { dispatchedBatch = $0.compactMap(\.event) }
        )

        XCTAssertEqual(dispatchedBatch, ["submit"])
    }

    func testOnSwipeLogsNotHostedAndDoesNothing() {
        let actions = [action(trigger: .onSwipe, event: "swipe")]
        var logs: [String] = []
        var dispatchedIndividually = false
        var dispatchedBatch = false
        AtomicActionTriggerLogProbe.debugSink = { logs.append($0) }

        AtomicActionTriggerDispatcher.dispatch(
            trigger: .onSwipe,
            actions: actions,
            onAction: { _ in dispatchedIndividually = true },
            batchExecutor: { _ in dispatchedBatch = true }
        )

        XCTAssertFalse(dispatchedIndividually)
        XCTAssertFalse(dispatchedBatch)
        XCTAssertEqual(logs.count, 1)
        XCTAssertTrue(logs[0].contains("not hosted at the element level"))
        XCTAssertTrue(logs[0].contains("onSwipe"))
    }
}