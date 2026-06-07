import Foundation

enum AtomicActionTriggerRegistry {
    static let supported: Set<ActionTrigger> = [
        .onActivate,
        .onVisible,
        .onLongPress,
        .onFocus,
        .onBlur,
        .onSubmit,
    ]

    static let notHosted: [ActionTrigger: String] = [
        .onSwipe: "carousel-only at ScrollContainer level"
    ]

    static func actions(for trigger: ActionTrigger, in actions: [Action]?) -> [Action] {
        guard let actions else { return [] }
        return actions.filter { $0.trigger == trigger }
    }
}

extension ActionTrigger {
    static let allCases: [ActionTrigger] = [
        .onActivate,
        .onBlur,
        .onFocus,
        .onLongPress,
        .onSubmit,
        .onSwipe,
        .onVisible,
    ]
}