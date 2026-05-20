import SwiftUI

/// Environment key providing batch action execution to the view tree.
/// When an element's primary activation trigger fires, `ActionTapModifier`
/// collects ALL matching actions and passes the ordered array to this
/// executor so cross-action failure policies (halt/continue/silent) are
/// honored across the batch.
///
/// Set by `ScreenShell` which owns the `ActionDispatcher`.
private struct BatchActionExecutorKey: EnvironmentKey {
    static let defaultValue: (([Action]) -> Void)? = nil
}

private struct FormActionContextKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    var batchActionExecutor: (([Action]) -> Void)? {
        get { self[BatchActionExecutorKey.self] }
        set { self[BatchActionExecutorKey.self] = newValue }
    }

    var isInFormActionContext: Bool {
        get { self[FormActionContextKey.self] }
        set { self[FormActionContextKey.self] = newValue }
    }
}
