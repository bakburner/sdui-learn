import Foundation
import Observation

/// Observable key-value store for screen-level mutable state (tab selection,
/// toggle switches, form field values, etc.).
///
/// Storage is `[String: Any]` holding JSON-primitive values (`String`, `Bool`,
/// `Int64`, `Double`, `[Any]`, `[String: Any]`, `JSONNull`) — matching the
/// underlying type that `Screen.state`'s generated `JSONAny` wrapper already
/// carries. This avoids the lossy string coercion of the previous
/// implementation while still surviving serialization round-trips.
///
/// Mirrors Android's `StateManager` in semantics; the `@Observable` macro
/// provides the SwiftUI reactivity that `MutableStateFlow` gives Compose.
@Observable
public final class ScreenState {

    /// Raw underlying key-value storage. Views observing this property
    /// re-render when any mutation occurs.
    public private(set) var values: [String: Any] = [:]

    public init() {}

    // MARK: - Initialization

    /// Seed screen state from the server-provided `Screen.state` map.
    /// Unwraps each `JSONAny` to its underlying primitive/collection value.
    /// Access level is `internal` because the `JSONAny` parameter is a
    /// generated (internal) type; only `SduiCore` callers consume this.
    func initializeFrom(_ state: [String: JSONAny]?) {
        guard let state else {
            values = [:]
            return
        }
        values = state.mapValues { $0.value }
    }

    /// Wipe all keys. Called on screen re-entry.
    public func reset() {
        values = [:]
    }

    // MARK: - Getters

    public func get(_ key: String) -> Any? { values[key] }

    public func getString(_ key: String) -> String? { values[key] as? String }

    public func getBool(_ key: String) -> Bool? { values[key] as? Bool }

    public func getInt(_ key: String) -> Int? {
        if let i64 = values[key] as? Int64 { return Int(i64) }
        return values[key] as? Int
    }

    public func getDouble(_ key: String) -> Double? { values[key] as? Double }

    // MARK: - Setters

    public func set(_ key: String, value: Any?) {
        if let value {
            values[key] = value
        } else {
            values.removeValue(forKey: key)
        }
    }

    public func set(_ key: String, value: String) { values[key] = value }
    public func set(_ key: String, value: Bool) { values[key] = value }
    public func set(_ key: String, value: Int) { values[key] = Int64(value) }
    public func set(_ key: String, value: Double) { values[key] = value }

    public func remove(_ key: String) {
        values.removeValue(forKey: key)
    }

    // MARK: - Mutate operation

    /// Apply a schema-declared `MutateOperation` at `key` using `incoming` as
    /// the operand. `nil` operation behaves as `.set` for parity with Android.
    /// `internal` because `MutateOperation` is a generated (internal) type.
    func apply(operation: MutateOperation?, key: String, value incoming: Any?) {
        switch operation {
        case .none, .some(.mutateOperationSet):
            set(key, value: incoming)

        case .some(.toggle):
            let current = (values[key] as? Bool) ?? false
            values[key] = !current

        case .some(.increment):
            let delta = Self.asDouble(incoming) ?? 1
            let existing = Self.asDouble(values[key]) ?? 0
            let next = existing + delta
            // Preserve integer typing when both operands are integer-valued.
            if floor(next) == next, abs(next) < Double(Int64.max) {
                values[key] = Int64(next)
            } else {
                values[key] = next
            }

        case .some(.append):
            if var array = values[key] as? [Any] {
                if let incoming { array.append(incoming) }
                values[key] = array
            } else if let current = values[key] as? String, let appended = incoming as? String {
                values[key] = current + appended
            } else if let incoming {
                values[key] = [incoming]
            }
        }
    }

    private static func asDouble(_ any: Any?) -> Double? {
        switch any {
        case let v as Double: return v
        case let v as Int64: return Double(v)
        case let v as Int: return Double(v)
        case let v as Bool: return v ? 1 : 0
        case let s as String: return Double(s)
        default: return nil
        }
    }
}
