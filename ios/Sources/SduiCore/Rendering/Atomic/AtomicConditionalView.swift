import SwiftUI

/// Evaluates a condition against screen state and renders thenElement or elseElement.
struct AtomicConditionalView: View {
    let element: AtomicElement
    let screenState: ScreenState
    let onAction: (Action) -> Void
    let depth: Int

    var body: some View {
        if evaluateCondition() {
            if let trueChild = element.trueChild {
                AtomicRouter(element: trueChild, screenState: screenState, onAction: onAction, depth: depth)
            }
        } else {
            if let falseChild = element.falseChild {
                AtomicRouter(element: falseChild, screenState: screenState, onAction: onAction, depth: depth)
            }
        }
    }

    /// Evaluate the condition string against screen state.
    /// Simple equality check: "stateKey == value"
    private func evaluateCondition() -> Bool {
        guard let condition = element.condition else { return false }

        // Support "key == value" and "key != value" patterns
        if let range = condition.range(of: "==") {
            let key = condition[condition.startIndex..<range.lowerBound].trimmingCharacters(in: .whitespaces)
            let expected = condition[range.upperBound...].trimmingCharacters(in: .whitespaces).trimmingCharacters(in: CharacterSet(charactersIn: "'\""))
            return screenState.getString(key) == expected
        }

        if let range = condition.range(of: "!=") {
            let key = condition[condition.startIndex..<range.lowerBound].trimmingCharacters(in: .whitespaces)
            let expected = condition[range.upperBound...].trimmingCharacters(in: .whitespaces).trimmingCharacters(in: CharacterSet(charactersIn: "'\""))
            return screenState.getString(key) != expected
        }

        // Boolean key check: truthy if key exists and is "true"
        return screenState.getString(condition) == "true"
    }
}
