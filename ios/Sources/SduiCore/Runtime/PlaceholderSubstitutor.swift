import Foundation

/// Replaces `{{stateKey}}` placeholders inside server-emitted strings with
/// the current screen-state value for that key.
///
/// The server emits actions like
/// `targetUri = "nba://games?date={{calendar_selected_date}}"` so a single
/// composed payload can carry parameterised navigation / refresh intent
/// without the server having to know the client's current state. The
/// substitution happens on the client at action-dispatch time, after any
/// preceding `mutate` (or renderer-driven `screenState.set`) has updated
/// the state map.
///
/// - Unknown keys are left as-is (`{{foo}}` → `{{foo}}`), which preserves
///   the error signal at the network layer rather than silently producing
///   a half-substituted URL.
/// - Keys follow `[A-Za-z_][A-Za-z0-9_.]*` so dotted state paths like
///   `filters.team` are recognised; arbitrary characters are not, keeping
///   the regex narrow enough to skip `{{ … }}` patterns embedded in
///   unrelated text.
///
/// Mirrors Android's `PlaceholderSubstitutor`.
enum PlaceholderSubstitutor {

    private static let pattern: NSRegularExpression = {
        // swiftlint:disable:next force_try
        try! NSRegularExpression(pattern: #"\{\{([A-Za-z_][A-Za-z0-9_.]*)\}\}"#)
    }()

    /// Replace every `{{key}}` in `template` using values from `state`.
    /// When `keepUnknown` is true (URI/endpoint fields), unknown keys are
    /// left intact so the failure surfaces at the network layer instead of
    /// producing a half-substituted URL. When false (`paramBindings`
    /// values), unknown keys resolve to the empty string so the caller can
    /// drop them — the wire contract for optional filter params.
    static func substitute(
        _ template: String,
        state: ScreenState,
        keepUnknown: Bool = true
    ) -> String {
        guard template.contains("{{") else { return template }
        let ns = template as NSString
        let range = NSRange(location: 0, length: ns.length)
        let matches = pattern.matches(in: template, options: [], range: range)
        guard !matches.isEmpty else { return template }

        var result = ""
        var cursor = 0
        for match in matches {
            let matchRange = match.range
            if matchRange.location > cursor {
                result += ns.substring(with: NSRange(
                    location: cursor,
                    length: matchRange.location - cursor
                ))
            }
            let keyRange = match.range(at: 1)
            let key = ns.substring(with: keyRange)
            if let value = state.get(key) {
                result += stringValue(of: value)
            } else if keepUnknown {
                result += ns.substring(with: matchRange)
            }
            cursor = matchRange.location + matchRange.length
        }
        if cursor < ns.length {
            result += ns.substring(from: cursor)
        }
        return result
    }

    static func substitute(_ template: String?, state: ScreenState) -> String? {
        guard let template else { return nil }
        return substitute(template, state: state)
    }

    /// Build a copy of `action` with every placeholder-bearing string field
    /// resolved against `state`. Returns the input unchanged when no field
    /// carries `{{`.
    static func resolve(_ action: Action, state: ScreenState) -> Action {
        let newTargetURI = substitute(action.targetURI, state: state)
        let newWebURL = substitute(action.webURL, state: state)
        let newEndpoint = substitute(action.endpoint, state: state)
        let newParamBindings: [String: String]?
        if let bindings = action.paramBindings {
            newParamBindings = bindings.mapValues {
                substitute($0, state: state, keepUnknown: false)
            }
        } else {
            newParamBindings = nil
        }

        if newTargetURI == action.targetURI,
           newWebURL == action.webURL,
           newEndpoint == action.endpoint,
           newParamBindings == action.paramBindings {
            return action
        }

        // `Action.with(...)` uses Swift's double-optional convention so a
        // `nil` argument means "leave unchanged" and `.some(nil)` clears
        // the field. We always want to overwrite, so wrap in `.some(...)`.
        return action.with(
            endpoint: .some(newEndpoint),
            paramBindings: .some(newParamBindings),
            targetURI: .some(newTargetURI),
            webURL: .some(newWebURL)
        )
    }

    private static func stringValue(of value: Any) -> String {
        if let s = value as? String { return s }
        if let b = value as? Bool { return b ? "true" : "false" }
        if let i = value as? Int { return String(i) }
        if let i = value as? Int64 { return String(i) }
        if let d = value as? Double {
            if d.rounded() == d, abs(d) < Double(Int64.max) {
                return String(Int64(d))
            }
            return String(d)
        }
        return String(describing: value)
    }
}
