import SwiftUI

/// Content blob flowing through an `AtomicComposite`'s subtree. Set by
/// `SectionRouter` when dispatching `AtomicComposite`, read by leaf
/// primitives that carry a `bindRef`. Non-composite contexts leave
/// this `nil`.
private struct CompositeContentKey: EnvironmentKey {
    static let defaultValue: [String: JSONAny]? = nil
}

extension EnvironmentValues {
    var compositeContent: [String: JSONAny]? {
        get { self[CompositeContentKey.self] }
        set { self[CompositeContentKey.self] = newValue }
    }
}

/// Resolver for the `bindRef` property on atomic leaf elements.
///
/// `bindRef` is a dot-path into the enclosing `AtomicComposite`'s
/// `data.content` object. Each primitive has a canonical live field
/// the resolver targets — `content` for `Text`, `src` for `Image`,
/// `label` for `Button`, and an object-shaped `{snapshotSeconds,
/// snapshotAt, isRunning}` for `LiveClock`. Placing the reference on
/// the consuming node (rather than declaring a central path-into-tree
/// binding) lets the composer reshape the ui tree without breaking
/// real-time updates.
enum BindRefResolver {

    /// Resolve a bindRef to a string value (Text.content, Button.label, Image.src).
    static func resolveString(bindRef: String?, in content: [String: JSONAny]?) -> String? {
        guard let value = resolveValue(bindRef: bindRef, in: content) else { return nil }
        if let s = value as? String { return s }
        if let i = value as? Int { return String(i) }
        if let d = value as? Double { return stringForDouble(d) }
        if let b = value as? Bool { return b ? "true" : "false" }
        return nil
    }

    /// Resolve a bindRef to a dictionary (LiveClock composite value).
    static func resolveDictionary(bindRef: String?, in content: [String: JSONAny]?) -> [String: Any]? {
        guard let value = resolveValue(bindRef: bindRef, in: content) else { return nil }
        return value as? [String: Any]
    }

    /// Resolve a bindRef to any underlying value, unwrapping `JSONAny`.
    static func resolveValue(bindRef: String?, in content: [String: JSONAny]?) -> Any? {
        guard let path = bindRef, !path.isEmpty, let root = content else { return nil }
        let parts = path.split(separator: ".", omittingEmptySubsequences: false).map(String.init)

        // First hop operates on the [String: JSONAny] root. Subsequent hops
        // traverse the plain `Any` value surfaced by `JSONAny.value`.
        guard let head = parts.first else { return nil }
        guard let firstNode = root[head]?.value else { return nil }
        var current: Any = firstNode
        for part in parts.dropFirst() {
            if let dict = current as? [String: Any], let next = dict[part] {
                current = next
            } else {
                return nil
            }
        }
        return current
    }

    private static func stringForDouble(_ value: Double) -> String {
        if value.rounded() == value, abs(value) < 1e16 {
            return String(Int(value))
        }
        return String(value)
    }
}
