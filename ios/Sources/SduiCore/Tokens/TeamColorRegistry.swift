import Foundation
import os

/// Resolves `nba.team.*` color tokens by walking the team section of the bundled
/// `color-tokens.json`. Resolution is fully local — no network calls.
///
/// Token resolution:
///   1. Look up the token in `semantic` to find the mode name. Theme-split tokens
///      (`nba.team.accent`, `nba.team.accent-label`) pick the mode for the given theme.
///   2. Look up the team's value in that mode; fall back to `_default`.
///   3. If the value is a role string, look up `palettes[teamId][role]`.
///   4. If the value is `{ "ref": "..." }`, resolve via `ColorTokenResolver`.
///   5. If the value is `{ "value": "#HEX" }`, return the literal hex.
public enum TeamColorRegistry {

    private static let logger = Logger(subsystem: "com.nba.sdui", category: "TeamColorRegistry")

    // MARK: - Data types

    private struct TeamPalette {
        let primary: String
        let secondary: String
        let tertiary: String?
    }

    private enum ModeValue {
        case role(String)
        case ref(String)
        case literal(String)
    }

    private struct SemanticEntry {
        let mode: String?
        let dark: String?
        let light: String?
    }

    private struct TeamData {
        let palettes: [String: TeamPalette]
        let modes: [String: [String: ModeValue]]
        let semantic: [String: SemanticEntry]
    }

    // MARK: - Loaded data

    private static let teamData: TeamData = loadTeamData()

    // MARK: - Public API

    /// Resolve an `nba.team.*` token for a given team and theme.
    /// Returns a hex string (`#RRGGBB`) or `nil` for unknown tokens/teams.
    public static func resolveTeamColor(token: String, teamId: String, theme: String) -> String? {
        let teamId = teamId.lowercased()

        guard let semanticEntry = teamData.semantic[token] else {
            logger.debug("team_color_missing: unknown token \(token, privacy: .public)")
            return nil
        }

        guard teamData.palettes[teamId] != nil else {
            logger.debug("team_color_missing: unknown teamId \(teamId, privacy: .public) for token \(token, privacy: .public)")
            return nil
        }

        let modeName: String
        if let singleMode = semanticEntry.mode {
            modeName = singleMode
        } else {
            let themeKey = theme.lowercased()
            if themeKey == "dark", let darkMode = semanticEntry.dark {
                modeName = darkMode
            } else if let lightMode = semanticEntry.light {
                modeName = lightMode
            } else {
                logger.debug("team_color_missing: no mode for theme \(theme, privacy: .public) in token \(token, privacy: .public)")
                return nil
            }
        }

        guard let mode = teamData.modes[modeName] else {
            logger.debug("team_color_missing: unknown mode \(modeName, privacy: .public)")
            return nil
        }

        let modeValue = mode[teamId] ?? mode["_default"]
        guard let modeValue else {
            logger.debug("team_color_missing: no value for team \(teamId, privacy: .public) in mode \(modeName, privacy: .public)")
            return nil
        }

        return resolve(modeValue: modeValue, teamId: teamId)
    }

    // MARK: - Resolution helpers

    private static func resolve(modeValue: ModeValue, teamId: String) -> String? {
        switch modeValue {
        case .role(let role):
            guard let palette = teamData.palettes[teamId] else { return nil }
            switch role {
            case "primary":   return palette.primary
            case "secondary": return palette.secondary
            case "tertiary":  return palette.tertiary
            default:
                logger.debug("team_color_missing: unknown role \(role, privacy: .public)")
                return nil
            }
        case .ref(let tokenName):
            return ColorTokenResolver.resolveTokenNameToHex(tokenName)
        case .literal(let hex):
            return hex
        }
    }

    // MARK: - JSON loading

    private static func loadTeamData() -> TeamData {
        guard let url = Bundle.module.url(forResource: "color-tokens", withExtension: "json", subdirectory: "Tokens")
            ?? Bundle.module.url(forResource: "color-tokens", withExtension: "json")
        else {
            fatalError("TeamColorRegistry: missing bundled resource color-tokens.json")
        }

        let data: Data
        do {
            data = try Data(contentsOf: url)
        } catch {
            fatalError("TeamColorRegistry: failed reading color-tokens.json: \(error)")
        }

        let json: Any
        do {
            json = try JSONSerialization.jsonObject(with: data, options: [])
        } catch {
            fatalError("TeamColorRegistry: failed parsing color-tokens.json: \(error)")
        }

        guard let root = json as? [String: Any],
              let team = root["team"] as? [String: Any] else {
            fatalError("TeamColorRegistry: missing team section in color-tokens.json")
        }

        return TeamData(
            palettes: parsePalettes(team),
            modes: parseModes(team),
            semantic: parseSemantic(team)
        )
    }

    private static func parsePalettes(_ team: [String: Any]) -> [String: TeamPalette] {
        guard let obj = team["palettes"] as? [String: Any] else {
            fatalError("TeamColorRegistry: missing palettes in team section")
        }

        var result: [String: TeamPalette] = [:]
        result.reserveCapacity(obj.count)
        for (teamId, value) in obj {
            guard let colors = value as? [String: String] else {
                fatalError("TeamColorRegistry: invalid palette for team \(teamId)")
            }
            guard let primary = colors["primary"],
                  let secondary = colors["secondary"] else {
                fatalError("TeamColorRegistry: missing primary/secondary for team \(teamId)")
            }
            result[teamId] = TeamPalette(
                primary: primary,
                secondary: secondary,
                tertiary: colors["tertiary"]
            )
        }
        return result
    }

    private static func parseModes(_ team: [String: Any]) -> [String: [String: ModeValue]] {
        guard let obj = team["modes"] as? [String: Any] else {
            fatalError("TeamColorRegistry: missing modes in team section")
        }

        var result: [String: [String: ModeValue]] = [:]
        result.reserveCapacity(obj.count)
        for (modeName, value) in obj {
            guard let entries = value as? [String: Any] else {
                fatalError("TeamColorRegistry: invalid mode \(modeName)")
            }
            var modeMap: [String: ModeValue] = [:]
            modeMap.reserveCapacity(entries.count)
            for (key, entryValue) in entries {
                if let role = entryValue as? String {
                    modeMap[key] = .role(role)
                } else if let dict = entryValue as? [String: String] {
                    if let ref = dict["ref"] {
                        modeMap[key] = .ref(ref)
                    } else if let hex = dict["value"] {
                        modeMap[key] = .literal(hex)
                    }
                }
            }
            result[modeName] = modeMap
        }
        return result
    }

    private static func parseSemantic(_ team: [String: Any]) -> [String: SemanticEntry] {
        guard let obj = team["semantic"] as? [String: Any] else {
            fatalError("TeamColorRegistry: missing semantic in team section")
        }

        var result: [String: SemanticEntry] = [:]
        result.reserveCapacity(obj.count)
        for (token, value) in obj {
            guard let entry = value as? [String: String] else {
                fatalError("TeamColorRegistry: invalid semantic entry for \(token)")
            }
            if let mode = entry["mode"] {
                result[token] = SemanticEntry(mode: mode, dark: nil, light: nil)
            } else {
                result[token] = SemanticEntry(mode: nil, dark: entry["dark"], light: entry["light"])
            }
        }
        return result
    }
}
