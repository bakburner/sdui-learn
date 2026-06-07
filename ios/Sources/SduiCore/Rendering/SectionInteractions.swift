import Foundation

/// Shared helpers for resolving actions declared on a ``Section`` or its
/// ``Subsection`` entries. Mirrors Android's
/// [`SectionInteractions`](../../../../../android/sdui-core/src/main/java/com/nba/sdui/core/renderer/interactions/SectionInteractions.kt).
///
/// Section renderers call these helpers instead of reaching into
/// `section.actions` directly so the resolution rules (legacy fallback
/// via `data.actions`, trigger matching, etc.) stay consistent.
enum SectionInteractions {

    /// All actions declared for this section. Falls back to legacy
    /// `data.actions` (a `[Action]` array inside the section payload)
    /// when `section.actions` is nil — preserves compatibility with
    /// pre-schema-v3 server composers that emitted actions under `data`.
    static func actions(for section: Section) -> [Action] {
        if let sectionLevel = section.actions, !sectionLevel.isEmpty {
            return sectionLevel
        }
        return legacyActions(in: section)
    }

    /// First action matching `trigger`. Matches Android's `primaryAction`.
    static func primaryAction(
        for section: Section,
        trigger: ActionTrigger = .onActivate
    ) -> Action? {
        let all = actions(for: section)
        return all.first(where: { $0.trigger == trigger })
    }

    /// Actions declared on a subsection (interactive nested element).
    static func subsectionActions(for section: Section, subsectionID: String) -> [Action] {
        guard let sub = section.subsections?.first(where: { $0.id == subsectionID }) else {
            return []
        }
        return sub.actions ?? []
    }

    static func subsectionPrimaryAction(
        for section: Section,
        subsectionID: String,
        trigger: ActionTrigger = .onActivate
    ) -> Action? {
        let sub = subsectionActions(for: section, subsectionID: subsectionID)
        return sub.first(where: { $0.trigger == trigger })
    }

    /// Extract a legacy `data.actions` array by round-tripping through
    /// JSON — avoids adding a typed field to the generated model just to
    /// support the compatibility path.
    private static func legacyActions(in section: Section) -> [Action] {
        guard let data = section.data,
              let encoded = try? newJSONEncoder().encode(data),
              let dict = try? JSONSerialization.jsonObject(with: encoded) as? [String: Any],
              let array = dict["actions"] as? [Any]
        else { return [] }

        let json = try? JSONSerialization.data(withJSONObject: array)
        guard let json else { return [] }
        return (try? newJSONDecoder().decode([Action].self, from: json)) ?? []
    }
}
