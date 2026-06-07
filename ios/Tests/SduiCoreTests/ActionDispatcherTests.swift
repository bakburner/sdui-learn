import XCTest
@testable import SduiCore

// `@MainActor` sits on each method rather than the class because
// `XCTestCase` is nonisolated. Tests reach into `@MainActor` state
// (`ActionDispatcher`, `ToastHost`, `ScreenState`) and must stay on
// the main actor.
final class ActionDispatcherTests: XCTestCase {

    @MainActor
    private func makeDispatcher(
        refresh: @escaping ActionDispatcher.RefreshHandler = { _, _, _ in },
        dismiss: @escaping ActionDispatcher.DismissHandler = {},
        analytics: AnalyticsDispatcher = InMemoryAnalyticsDispatcher(),
        impressions: ImpressionTracker = ImpressionTracker(),
        sectionContext: String = "hero"
    ) -> (ActionDispatcher, ScreenState, NavCoordinator, ToastHost) {
        let state = ScreenState()
        let nav = NavCoordinator()
        let toasts = ToastHost()
        let dispatcher = ActionDispatcher(
            screenState: state,
            nav: nav,
            toasts: toasts,
            analytics: analytics,
            impressions: impressions,
            refreshHandler: refresh,
            dismissHandler: dismiss,
            sectionContext: sectionContext
        )
        return (dispatcher, state, nav, toasts)
    }

    private func action(
        type: ActionType,
        trigger: ActionTrigger = .onActivate,
        event: String? = nil,
        target: String? = nil,
        operation: MutateOperation? = nil,
        value: JSONAny? = nil,
        message: String? = nil,
        onFailure: FailurePolicy? = nil,
        failureFeedback: FailureFeedback? = nil,
        impression: ImpressionPolicy? = nil,
        destinations: [Destination]? = nil,
        params: [String: JSONAny]? = nil,
        endpoint: String? = nil,
        paramBindings: [String: String]? = nil,
        targetURI: String? = nil,
        webURL: String? = nil,
        presentation: NavigationPresentation? = nil
    ) -> Action {
        Action(
            destinations: destinations,
            endpoint: endpoint,
            event: event,
            failureFeedback: failureFeedback,
            impression: impression,
            message: message,
            modalHeight: nil,
            onFailure: onFailure,
            operation: operation,
            paramBindings: paramBindings,
            params: params,
            presentation: presentation,
            target: target,
            targetURI: targetURI,
            trigger: trigger,
            type: type,
            value: value,
            webURL: webURL
        )
    }

    @MainActor
    func testMutateSetUpdatesScreenState() throws {
        let (dispatcher, state, _, _) = makeDispatcher()
        let value = try JSONDecoder().decode(JSONAny.self, from: Data("\"hello\"".utf8))
        dispatcher.dispatch(action(type: .mutate, target: "greeting", operation: .mutateOperationSet, value: value))

        XCTAssertEqual(state.getString("greeting"), "hello")
    }

    @MainActor
    func testToastWithMessageShowsToast() {
        let (dispatcher, _, _, toasts) = makeDispatcher()
        dispatcher.dispatch(action(type: .toast, message: "Saved"))

        XCTAssertEqual(toasts.current?.message, "Saved")
    }

    @MainActor
    func testRefreshInvokesHandler() {
        var captured: (String?, String?, [String: String])?
        let (dispatcher, _, _, _) = makeDispatcher { id, endpoint, params in
            captured = (id, endpoint, params)
        }

        dispatcher.dispatch(action(
            type: .refresh,
            target: "scoreboard",
            endpoint: "/v1/sdui/scoreboard",
            paramBindings: nil
        ))

        XCTAssertEqual(captured?.0, "scoreboard")
        XCTAssertEqual(captured?.1, "/v1/sdui/scoreboard")
    }

    @MainActor
    func testDismissInvokesHandler() {
        var dismissed = false
        let (dispatcher, _, _, _) = makeDispatcher(dismiss: { dismissed = true })

        dispatcher.dispatch(action(type: .dismiss))

        XCTAssertTrue(dismissed)
    }

    @MainActor
    func testFireAndForgetOnActivateDispatchesImmediately() {
        let analytics = InMemoryAnalyticsDispatcher()
        let (dispatcher, _, _, _) = makeDispatcher(analytics: analytics)
        dispatcher.dispatch(action(type: .fireAndForget, trigger: .onActivate, event: "tap_event"))

        XCTAssertEqual(analytics.events.first?.name, "tap_event")
    }

    @MainActor
    func testFireAndForgetOnVisibleDedupsPerScreen() async throws {
        let analytics = InMemoryAnalyticsDispatcher()
        let impressions = ImpressionTracker()
        let (dispatcher, _, _, _) = makeDispatcher(analytics: analytics, impressions: impressions)

        for _ in 0..<3 {
            dispatcher.dispatch(action(
                type: .fireAndForget,
                trigger: .onVisible,
                event: "hero_impression"
            ))
        }

        // Let the Task queue flush.
        try await Task.sleep(nanoseconds: 50 * 1_000_000)

        XCTAssertEqual(analytics.events.filter { $0.name == "hero_impression" }.count, 1)
    }

    @MainActor
    func testNavigateHaltStopsSequence() {
        let analytics = InMemoryAnalyticsDispatcher()
        let (dispatcher, _, _, _) = makeDispatcher(analytics: analytics)

        let navigate = action(type: .navigate, targetURI: "nba://for-you")
        let followUp = action(type: .fireAndForget, trigger: .onActivate, event: "after_nav")

        let ran = dispatcher.execute([navigate, followUp])

        XCTAssertFalse(ran, "Navigate success halts the sequence")
        XCTAssertTrue(analytics.events.isEmpty, "Follow-up must not run")
    }

    @MainActor
    func testNavigateWithWebUrlOnly() {
        let (dispatcher, _, nav, _) = makeDispatcher()

        dispatcher.dispatch(action(type: .navigate, webURL: "nba://scoreboard"))

        XCTAssertEqual(nav.path.count, 1)
    }

    @MainActor
    func testNavigateUsesTargetURIWhenWebURLIsMissing() {
        let (dispatcher, _, nav, _) = makeDispatcher()

        dispatcher.dispatch(action(type: .navigate, targetURI: "nba://game/0042300102"))

        XCTAssertEqual(nav.path.count, 1)
    }

    @MainActor
    func testNavigateWithTargetUriAndWebUrlPrefersTargetUriForNbaScheme() {
        let (dispatcher, _, nav, _) = makeDispatcher()

        dispatcher.dispatch(action(
            type: .navigate,
            targetURI: "nba://boxscore/0042300102",
            webURL: "https://www.nba.com/game/0042300102"
        ))

        XCTAssertEqual(nav.path.count, 1)
    }

    @MainActor
    func testNavigateWithNonNbaTargetUriAndWebUrlPrefersWebUrl() {
        let (dispatcher, _, nav, _) = makeDispatcher()

        dispatcher.dispatch(action(
            type: .navigate,
            targetURI: "https://example.com/internal",
            webURL: "nba://schedule"
        ))

        XCTAssertEqual(nav.path.count, 1)
    }

    @MainActor
    func testToastFailureShowsFallbackMessage() {
        let (dispatcher, _, _, toasts) = makeDispatcher()
        let bad = action(type: .toast, message: nil, onFailure: .halt)

        dispatcher.dispatch(bad)

        XCTAssertNotNil(toasts.current, "Halt should surface failure feedback as a toast")
    }
}
