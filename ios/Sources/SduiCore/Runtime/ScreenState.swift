import Foundation
import Observation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "ScreenState")

enum ScreenStateLogProbe {
    static var warningSink: ((String) -> Void)?

    static func warning(_ message: String) {
        logger.warning("\(message, privacy: .public)")
        warningSink?(message)
    }
}

/// Per-key value holder so the observation system can avoid treating the
/// entire screen map as a single invalidation surface when a single key
/// changes.
@Observable
fileprivate final class KeySlot {
    var value: Any?
}

/// Observable key-value store for screen-level mutable state (tab selection,
/// toggle switches, form field values, etc.).
///
/// Storage is JSON-primitive values (`String`, `Bool`,
/// `Int64`, `Double`, `[Any]`, `[String: Any]`, `JSONNull`) — matching the
/// underlying type that `Screen.state`'s generated `JSONAny` wrapper already
/// carries. This avoids the lossy string coercion of the previous
/// implementation while still surviving serialization round-trips.
///
/// Mirrors Android's `StateManager` in semantics; the `@Observable` macro
/// provides the SwiftUI reactivity that `MutableStateFlow` gives Compose.
@Observable
public final class ScreenState {

    fileprivate var slots: [String: KeySlot] = [:]

    public init() {}

    // MARK: - Initialization

    /// Seed screen state from the server-provided `Screen.state` map.
    /// Unwraps each `JSONAny` to its underlying primitive/collection value.
    /// Access level is `internal` because the `JSONAny` parameter is a
    /// generated (internal) type; only `SduiCore` callers consume this.
    func initializeFrom(_ state: [String: JSONAny]?) {
        slots.removeAll(keepingCapacity: true)
        guard let state else { return }
        for (key, v) in state {
            let slot = KeySlot()
            slot.value = v.value
            slots[key] = slot
        }
    }

    /// Wipe all keys. Called on screen re-entry.
    public func reset() {
        slots = [:]
    }

    // MARK: - Getters

    public func get(_ key: String) -> Any? { slots[key]?.value }

    public func getString(_ key: String) -> String? { slots[key]?.value as? String }

    public func getBool(_ key: String) -> Bool? { slots[key]?.value as? Bool }

    public func getInt(_ key: String) -> Int? {
        if let i64 = slots[key]?.value as? Int64 { return Int(i64) }
        return slots[key]?.value as? Int
    }

    public func getDouble(_ key: String) -> Double? { slots[key]?.value as? Double }

    // MARK: - Setters

    public func set(_ key: String, value: Any?) {
        if let value {
            let slot: KeySlot
            if let existing = slots[key] {
                slot = existing
            } else {
                let s = KeySlot()
                slots[key] = s
                slot = s
            }
            slot.value = value
        } else {
            slots.removeValue(forKey: key)
        }
    }

    // Typed convenience setters route through the `Any?` overload via an
    // explicit cast — without the cast Swift's overload resolution would
    // re-pick the typed overload (most specific match) and infinitely
    // recurse on the next call.
    public func set(_ key: String, value: String) { set(key, value: value as Any?) }
    public func set(_ key: String, value: Bool) { set(key, value: value as Any?) }
    public func set(_ key: String, value: Int) { set(key, value: Int64(value) as Any?) }
    public func set(_ key: String, value: Double) { set(key, value: value as Any?) }

    public func remove(_ key: String) {
        slots.removeValue(forKey: key)
    }

    // MARK: - Mutate operation

    /// Apply a schema-declared `MutateOperation` at `key` using `incoming` as
    /// the operand. `nil` operation behaves as `.set` for parity with Android.
    /// `internal` because `MutateOperation` is a generated (internal) type.
    func apply(operation: MutateOperation?, key: String, value incoming: Any?) {
        let current = slots[key]?.value
        switch operation {
        case .none, .some(.mutateOperationSet):
            set(key, value: incoming)

        case .some(.toggle):
            guard let current = current as? Bool else {
                // TODO(action-mutate): review type-mismatch semantics across platforms
                ScreenStateLogProbe.warning("mutate toggle noop: key=\(key) currentType=\(Self.typeName(for: current))")
                return
            }
            set(key, value: !current)

        case .some(.increment):
            guard let existing = Self.asStoredNumber(current) else {
                // TODO(action-mutate): review type-mismatch semantics across platforms
                ScreenStateLogProbe.warning("mutate increment noop: key=\(key) currentType=\(Self.typeName(for: current))")
                return
            }
            let delta = Self.asDouble(incoming) ?? 1
            let next = existing + delta
            if floor(next) == next, abs(next) < Double(Int64.max) {
                set(key, value: Int64(next))
            } else {
                set(key, value: next)
            }

        case .some(.append):
            if var array = current as? [Any] {
                if let incoming { array.append(incoming) }
                set(key, value: array)
            } else if let current = current as? String, let appended = incoming as? String {
                set(key, value: current + appended)
            } else if current == nil, let incoming {
                set(key, value: [incoming])
            } else {
                // TODO(action-mutate): review type-mismatch semantics across platforms
                ScreenStateLogProbe.warning("mutate append noop: key=\(key) currentType=\(Self.typeName(for: current))")
            }

        default:
            // TODO(action-mutate): review type-mismatch semantics across platforms
            ScreenStateLogProbe.warning("mutate noop: unknown operation key=\(key) op=\(operation?.rawValue ?? "nil") currentType=\(Self.typeName(for: current))")
        }
    }

    private static func asStoredNumber(_ any: Any?) -> Double? {
        switch any {
        case let v as Double where v.isFinite: return v
        case let v as Int64: return Double(v)
        case let v as Int: return Double(v)
        default: return nil
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

    private static func typeName(for value: Any?) -> String {
        guard let value else { return "nil" }
        return String(describing: type(of: value))
    }
}
