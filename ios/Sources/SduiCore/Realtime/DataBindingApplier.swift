import Foundation
import os

private let logger = Logger(subsystem: "com.nba.sdui", category: "DataBindingApplier")

/// Applies server-defined data bindings to a section's current data using an
/// opaque real-time message payload.
///
/// Mirrors Android's
/// [`DataBindingResolver`](../../android/sdui-core/src/main/java/com/nba/sdui/core/data/DataBindingResolver.kt).
/// Ably messages are treated as opaque JSON: the applier has zero knowledge of
/// payload structure — the section's `DataBinding.bindings` list is the only
/// mechanism connecting an incoming message to section data.
///
/// Semantics:
/// - JSONPath-like source paths (`$.homeTeam.score`, `$.teams[0].name`)
/// - Dot-path targets (`homeTeam.score`), with intermediate objects auto-created
/// - Missing source path → preserve previous target value, bump miss counter
/// - Miss counter ≥ `missThreshold` (3) emits a `binding_path_missing` warning
///   per section+sourcePath (MP-1 / MP-2 / MP-3 in the plan)
/// - `stringKeys[targetPath]` overrides the bound value with a localized
///   lookup from the section-scoped string table (i18n layer)
///
/// Instances are `@MainActor`-agnostic. The VM constructs one per screen and
/// calls `applyBindings` synchronously on the data-update hot path.
final class DataBindingApplier {

    static let missThreshold = 3

    private var consecutiveMisses: [String: Int] = [:]

    init() {}

    /// Apply `binding` to `currentData` using `incomingMessage`. Returns a
    /// new data dictionary with bound fields replaced.
    func applyBindings(
        currentData: [String: Any],
        incomingMessage: [String: Any],
        dataBinding: DataBinding,
        sectionID: String?,
        correlationId: String?,
        stringTable: [String: String]?
    ) -> [String: Any] {
        guard let bindings = dataBinding.bindings else { return currentData }

        logger.debug("Applying \(bindings.count) bindings, correlationId=\(correlationId ?? "-", privacy: .public)")

        var output = currentData
        for binding in bindings {
            do {
                try applyOne(
                    output: &output,
                    incoming: incomingMessage,
                    binding: binding,
                    sectionID: sectionID,
                    correlationId: correlationId
                )

                if let key = dataBinding.stringKeys?[binding.targetPath],
                   let table = stringTable,
                   let resolved = table[key] {
                    setValue(resolved, at: binding.targetPath, in: &output)
                    logger.debug("Applied binding: \(binding.sourcePath, privacy: .public) -> \(binding.targetPath, privacy: .public) = \(Self.describe(resolved), privacy: .public) [stringKey=\(key, privacy: .public)], correlationId=\(correlationId ?? "-", privacy: .public)")
                } else if let key = dataBinding.stringKeys?[binding.targetPath], stringTable == nil {
                    logger.warning("stringKey '\(key, privacy: .public)' present but no stringTable for section=\(sectionID ?? "?", privacy: .public)")
                } else {
                    let applied = Self.valueAt(path: binding.targetPath, in: output)
                    logger.debug("Applied binding: \(binding.sourcePath, privacy: .public) -> \(binding.targetPath, privacy: .public) = \(Self.describe(applied), privacy: .public), correlationId=\(correlationId ?? "-", privacy: .public)")
                }
            } catch {
                // `applyOne` already bumps `consecutiveMisses` on a miss before
                // throwing — don't double-count here. This catch just surfaces
                // the failure in the log for post-hoc debugging.
                logger.warning("failed binding \(binding.sourcePath, privacy: .public) -> \(binding.targetPath, privacy: .public) section=\(sectionID ?? "?", privacy: .public): \(error.localizedDescription, privacy: .public)")
            }
        }
        return output
    }

    private static func describe(_ value: Any?) -> String {
        guard let value, !(value is NSNull) else { return "nil" }
        if let string = value as? String { return "\"\(string)\"" }
        if let dict = value as? [String: Any] {
            let pairs = dict.keys.sorted().map { key in "\(key)=\(describe(dict[key] ?? NSNull()))" }
            return "{" + pairs.joined(separator: ", ") + "}"
        }
        return String(describing: value)
    }

    private static func valueAt(path: String, in dict: [String: Any]) -> Any? {
        let parts = path.split(separator: ".").map(String.init)
        var current: Any? = dict
        for part in parts {
            guard let node = current as? [String: Any] else { return nil }
            current = node[part]
        }
        return current
    }

    /// Consecutive-miss counter for `sectionID:sourcePath`. Exposed for the
    /// VM's staleness / analytics emission.
    func missCount(sectionID: String, sourcePath: String) -> Int {
        consecutiveMisses["\(sectionID):\(sourcePath)"] ?? 0
    }

    /// Clear all miss counters for a section (call on section removal to
    /// prevent unbounded growth across long-running screens).
    func resetCounters(sectionID: String) {
        let prefix = "\(sectionID):"
        consecutiveMisses = consecutiveMisses.filter { !$0.key.hasPrefix(prefix) }
    }

    // MARK: - Single binding

    private struct BindingMissError: Error {}

    private func applyOne(
        output: inout [String: Any],
        incoming: [String: Any],
        binding: DataBindingPath,
        sectionID: String?,
        correlationId: String?
    ) throws {
        let sourceValue = Self.resolveSourcePath(binding.sourcePath, in: incoming)

        guard let resolved = sourceValue, !(resolved is NSNull) else {
            if let sectionID {
                let missKey = "\(sectionID):\(binding.sourcePath)"
                let next = (consecutiveMisses[missKey] ?? 0) + 1
                consecutiveMisses[missKey] = next
                if next == Self.missThreshold {
                    logger.warning("binding_path_missing: section=\(sectionID, privacy: .public) sourcePath=\(binding.sourcePath, privacy: .public) correlationId=\(correlationId ?? "-", privacy: .public)")
                    // Analytics beacon emission is the VM's concern; this
                    // module only measures the miss streak.
                }
            }
            throw BindingMissError()
        }

        if let sectionID {
            consecutiveMisses.removeValue(forKey: "\(sectionID):\(binding.sourcePath)")
        }

        let value = transform(resolved, using: binding.transform, root: incoming)
        setValue(value, at: binding.targetPath, in: &output)
    }

    private func transform(_ value: Any, using transform: Transform?, root: [String: Any]) -> Any {
        switch transform {
        case .liveClockSnapshot:
            return Self.normalizeLiveClockSnapshot(value, root: root)
        case .none:
            return value
        }
    }

    private static func normalizeLiveClockSnapshot(_ value: Any, root: [String: Any]) -> [String: Any] {
        let objectValue = value as? [String: Any]
        let rawSeconds = objectValue?["snapshotSeconds"]
            ?? objectValue?["seconds"]
            ?? objectValue?["remainingSeconds"]
            ?? value
        let snapshotAt = stringValue(objectValue?["snapshotAt"])
            ?? stringValue(objectValue?["snapshotAtIso"])
            ?? stringValue(root["snapshotAt"])
            ?? ISO8601DateFormatter().string(from: Date())
        let runningValue = objectValue?["isRunning"]
            ?? objectValue?["clockRunning"]
            ?? objectValue?["gameClockRunning"]
            ?? root["isRunning"]
            ?? root["clockRunning"]
            ?? root["gameClockRunning"]

        return [
            "snapshotSeconds": parseClockSeconds(rawSeconds) ?? 0,
            "snapshotAt": snapshotAt,
            "isRunning": boolValue(runningValue) ?? false
        ]
    }

    private static func parseClockSeconds(_ value: Any?) -> Int? {
        if let intValue = value as? Int {
            return max(0, intValue)
        }
        if let doubleValue = value as? Double, doubleValue.isFinite {
            return max(0, Int(doubleValue.rounded(.down)))
        }
        guard let string = value as? String else { return nil }
        let trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)

        if let seconds = parseISODurationSeconds(trimmed) {
            return seconds
        }

        let clockPattern = #"(?<!\d)(\d{1,2}):([0-5]\d)(?:\.\d+)?(?!\d)"#
        guard let regex = try? NSRegularExpression(pattern: clockPattern),
              let match = regex.firstMatch(in: trimmed, range: NSRange(trimmed.startIndex..<trimmed.endIndex, in: trimmed)),
              let minuteRange = Range(match.range(at: 1), in: trimmed),
              let secondRange = Range(match.range(at: 2), in: trimmed),
              let minutes = Int(trimmed[minuteRange]),
              let seconds = Int(trimmed[secondRange]) else { return nil }
        return minutes * 60 + seconds
    }

    private static func parseISODurationSeconds(_ value: String) -> Int? {
        let pattern = #"^PT(?:(\d+(?:\.\d+)?)H)?(?:(\d+(?:\.\d+)?)M)?(?:(\d+(?:\.\d+)?)S)?$"#
        guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else { return nil }
        let range = NSRange(value.startIndex..<value.endIndex, in: value)
        guard let match = regex.firstMatch(in: value, range: range) else { return nil }

        func component(_ index: Int) -> Double {
            let nsRange = match.range(at: index)
            guard nsRange.location != NSNotFound,
                  let range = Range(nsRange, in: value) else { return 0 }
            return Double(value[range]) ?? 0
        }

        let total = component(1) * 3600 + component(2) * 60 + component(3)
        return max(0, Int(total.rounded(.down)))
    }

    private static func boolValue(_ value: Any?) -> Bool? {
        if let bool = value as? Bool { return bool }
        if let number = value as? NSNumber { return number.intValue != 0 }
        guard let string = value as? String else { return nil }
        switch string.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "true", "1", "yes", "y":
            return true
        case "false", "0", "no", "n":
            return false
        default:
            return nil
        }
    }

    private static func stringValue(_ value: Any?) -> String? {
        guard let string = value as? String,
              !string.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return string
    }

    // MARK: - Path navigation

    /// Navigate a JSONPath-like expression like `$.homeTeam.score` or
    /// `$.teams[0].name` through `root`. Returns `nil` if any segment is
    /// missing.
    static func resolveSourcePath(_ path: String, in root: [String: Any]) -> Any? {
        let clean: String
        if path.hasPrefix("$.") {
            clean = String(path.dropFirst(2))
        } else if path == "$" {
            return root
        } else {
            clean = path
        }
        return navigate(clean, into: root)
    }

    private static func navigate(_ path: String, into value: Any) -> Any? {
        guard !path.isEmpty else { return value }
        let parts = path.split(separator: ".", omittingEmptySubsequences: false).map(String.init)

        var current: Any? = value
        for part in parts {
            guard let node = current, !(node is NSNull) else { return nil }

            if let (field, index) = parseArrayIndex(part) {
                guard let dict = node as? [String: Any],
                      let array = dict[field] as? [Any],
                      index < array.count else { return nil }
                current = array[index]
            } else {
                if let dict = node as? [String: Any] {
                    current = dict[part]
                } else {
                    return nil
                }
            }
        }
        return current
    }

    private static func parseArrayIndex(_ part: String) -> (field: String, index: Int)? {
        guard let openBracket = part.firstIndex(of: "["),
              part.last == "]" else { return nil }
        let field = String(part[..<openBracket])
        let indexStr = part[part.index(after: openBracket)..<part.index(before: part.endIndex)]
        guard let idx = Int(indexStr) else { return nil }
        return (field, idx)
    }

    /// Write `value` into `dict` at dot-separated `path`, auto-creating
    /// intermediate `[String: Any]` objects as needed.
    func setValue(_ value: Any, at path: String, in dict: inout [String: Any]) {
        let parts = path.split(separator: ".").map(String.init)
        if parts.count == 1 {
            dict[parts[0]] = value
            return
        }

        // Walk/create nested dicts. Swift dictionaries are value types, so
        // we rebuild from the inside out.
        func writeNested(_ remaining: ArraySlice<String>, into node: inout [String: Any]) {
            guard let head = remaining.first else { return }
            let tail = remaining.dropFirst()
            if tail.isEmpty {
                node[head] = value
                return
            }
            var child = node[head] as? [String: Any] ?? [:]
            writeNested(tail, into: &child)
            node[head] = child
        }
        writeNested(ArraySlice(parts), into: &dict)
    }
}
