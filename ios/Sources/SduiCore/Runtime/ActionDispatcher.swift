import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "ActionDispatcher")

/// Executes a sequence of SDUI `Action`s in declared order per
/// `docs/sdui-requirements-summary.md`.
///
/// Mirrors Android's
/// [`ActionHandler.executeSequence`](../../android/sdui-core/src/main/java/com/nba/sdui/core/state/ActionHandler.kt):
/// - Default per-type failure policy when `onFailure` is absent
///   (navigate=halt, fireAndForget/dismiss/toast=silent, mutate/refresh=continue).
/// - `halt` presents `failureFeedback.message` via ``ToastHost`` (falling back
///   to a generic localized string).
/// - `navigate` success halts the sequence regardless of onFailure.
///
/// `@MainActor` isolation guarantees ordering without explicit locking and
/// lets mutate/dispatch touch SwiftUI-observable state directly.
@MainActor
final class ActionDispatcher {

    /// Hook invoked when a `refresh` action fires. The VM owns this and
    /// scopes refresh to the affected `sectionId` (full-screen when nil).
    typealias RefreshHandler = (
        _ sectionID: String?,
        _ endpoint: String?,
        _ resolvedParams: [String: String]
    ) -> Void

    /// Hook invoked for `dismiss` actions. Typically wires to
    /// `@Environment(\.dismiss)` in a presenting view.
    typealias DismissHandler = () -> Void

    private let screenState: ScreenState
    private let nav: NavCoordinator
    private let toasts: ToastHost
    private let analytics: AnalyticsDispatcher
    private let impressions: ImpressionTracker
    private let refreshHandler: RefreshHandler
    private let dismissHandler: DismissHandler
    /// Identifies the section a given action originates from, used for
    /// impression dedup keying (`sectionID:event`). Falls back to the
    /// empty string for screen-level actions.
    let sectionContext: String

    init(
        screenState: ScreenState,
        nav: NavCoordinator,
        toasts: ToastHost,
        analytics: AnalyticsDispatcher,
        impressions: ImpressionTracker,
        refreshHandler: @escaping RefreshHandler,
        dismissHandler: @escaping DismissHandler = {},
        sectionContext: String = ""
    ) {
        self.screenState = screenState
        self.nav = nav
        self.toasts = toasts
        self.analytics = analytics
        self.impressions = impressions
        self.refreshHandler = refreshHandler
        self.dismissHandler = dismissHandler
        self.sectionContext = sectionContext
    }

    /// Returns a dispatcher that shares all dependencies but scopes
    /// impression dedup to the supplied `sectionID`. Used by
    /// ``SectionRouter`` so per-section `onVisible` actions dedup on
    /// that section's key rather than the screen default.
    func scoped(to sectionID: String) -> ActionDispatcher {
        ActionDispatcher(
            screenState: screenState,
            nav: nav,
            toasts: toasts,
            analytics: analytics,
            impressions: impressions,
            refreshHandler: refreshHandler,
            dismissHandler: dismissHandler,
            sectionContext: sectionID
        )
    }

    // MARK: - Sequence entry points

    /// Dispatch a single action. Most call sites hand in one action at a
    /// time (a tap, an onVisible fire). Convenience over `execute(_:)`.
    func dispatch(_ action: Action) {
        execute([action])
    }

    /// Execute actions in order, honouring failure policy + navigate halt
    /// rules. Returns `true` when the sequence ran to completion, `false`
    /// when it halted mid-sequence (navigate success or halt-failure).
    @discardableResult
    func execute(_ actions: [Action]) -> Bool {
        for action in actions {
            let result = handle(action)
            switch result {
            case .navigateSuccess:
                return false
            case .success:
                continue
            case .failure(let feedback):
                switch resolveFailurePolicy(action) {
                case .halt:
                    logger.warning("action \(action.type.rawValue) failed; halt policy stopping sequence")
                    presentFailure(feedback, from: action)
                    return false
                case .failurePolicyContinue:
                    logger.warning("action \(action.type.rawValue) failed; continue policy proceeding")
                    continue
                case .silent, .none:
                    continue
                }
            }
        }
        return true
    }

    // MARK: - Per-type handlers

    private enum HandleResult {
        case success
        case navigateSuccess
        case failure(FailureFeedback?)
    }

    private func handle(_ action: Action) -> HandleResult {
        logger.debug("handling type=\(action.type.rawValue) trigger=\(action.trigger.rawValue)")
        switch action.type {
        case .navigate: return handleNavigate(action)
        case .fireAndForget: return handleFireAndForget(action)
        case .mutate: return handleMutate(action)
        case .refresh: return handleRefresh(action)
        case .dismiss: return handleDismiss(action)
        case .toast: return handleToast(action)
        }
    }

    private func handleNavigate(_ action: Action) -> HandleResult {
        guard let targetURI = action.targetURI ?? action.webURL else {
            return .failure(action.failureFeedback)
        }
        if action.presentation == .external, let url = URL(string: targetURI) {
            nav.openExternal(url)
        } else {
            let endpoint = UriResolver.resolveEndpoint(uri: targetURI)
            switch action.presentation {
            case .replace:
                nav.popToRoot()
                nav.push(endpoint: endpoint)
            case .modal, .fullscreen:
                logger.warning("navigation presentation \(action.presentation?.rawValue ?? "nil", privacy: .public) is decoded but no native host is registered; falling back to push modalHeight=\(action.modalHeight?.rawValue ?? "nil", privacy: .public)")
                nav.push(endpoint: endpoint)
            case .external, .push, .none:
                nav.push(endpoint: endpoint)
            }
        }
        return .navigateSuccess
    }

    private func handleFireAndForget(_ action: Action) -> HandleResult {
        let event = action.event ?? "unnamed_event"
        let params = (action.params ?? [:]).mapValues { $0.value }
        let destinations = (action.destinations ?? []).map(\.rawValue)

        // ADR-009: `onVisible` beacons run through the impression tracker
        // for dedup. Non-visibility triggers (onTap etc.) always fire.
        guard action.trigger == .onVisible else {
            analytics.send(event: event, params: params, destinations: destinations)
            return .success
        }

        let sectionID = sectionContext.isEmpty ? "screen" : sectionContext
        let policy = action.impression
        let analytics = self.analytics
        let impressions = self.impressions
        Task {
            let shouldFire = await impressions.shouldFire(
                sectionID: sectionID,
                event: event,
                policy: policy
            )
            guard shouldFire else { return }
            analytics.send(event: event, params: params, destinations: destinations)
        }

        return .success
    }

    private func handleMutate(_ action: Action) -> HandleResult {
        guard let key = action.target else {
            return .failure(action.failureFeedback)
        }
        screenState.apply(
            operation: action.operation,
            key: key,
            value: action.value?.value
        )
        return .success
    }

    private func handleRefresh(_ action: Action) -> HandleResult {
        let resolved = resolveParamBindings(action.paramBindings)
        refreshHandler(action.target, action.endpoint, resolved)
        return .success
    }

    private func handleDismiss(_ action: Action) -> HandleResult {
        dismissHandler()
        return .success
    }

    private func handleToast(_ action: Action) -> HandleResult {
        guard let message = action.message, !message.isEmpty else {
            return .failure(action.failureFeedback)
        }
        toasts.show(message, style: .info)
        return .success
    }

    // MARK: - Helpers

    private func resolveFailurePolicy(_ action: Action) -> FailurePolicy? {
        if let explicit = action.onFailure { return explicit }
        // Per-type defaults mirror Android's DEFAULT_FAILURE_POLICY table.
        switch action.type {
        case .navigate: return .halt
        case .fireAndForget, .dismiss, .toast: return .silent
        case .mutate, .refresh: return .failurePolicyContinue
        }
    }

    private func presentFailure(_ feedback: FailureFeedback?, from action: Action) {
        let message = feedback?.message ?? defaultFailureMessage(for: action)
        if feedback?.style == .inline {
            logger.warning("failureFeedback.style=inline is decoded but not hosted inline on iOS; presenting error toast")
        }
        toasts.show(message, style: .error)
    }

    private func defaultFailureMessage(for action: Action) -> String {
        // Generic localized fallback matching the requirements doc.
        switch action.type {
        case .navigate: return String(localized: "Couldn't open that destination.")
        case .refresh: return String(localized: "Couldn't refresh right now.")
        case .mutate: return String(localized: "Couldn't update.")
        case .fireAndForget: return String(localized: "Couldn't send.")
        case .dismiss: return String(localized: "Couldn't close.")
        case .toast: return String(localized: "Something went wrong.")
        }
    }

    /// Resolve `paramBindings` map entries from screen state, matching
    /// Android's mustache-template stripping (`{{form_season}}` → lookup
    /// `form_season` in `ScreenState`).
    private func resolveParamBindings(_ bindings: [String: String]?) -> [String: String] {
        guard let bindings, !bindings.isEmpty else { return [:] }
        var resolved: [String: String] = [:]
        for (queryKey, template) in bindings {
            let stateKey = template
                .trimmingCharacters(in: .whitespaces)
                .replacingOccurrences(of: "{{", with: "")
                .replacingOccurrences(of: "}}", with: "")
                .trimmingCharacters(in: .whitespaces)
            if let value = screenState.get(stateKey) {
                resolved[queryKey] = String(describing: value)
            }
        }
        return resolved
    }
}
