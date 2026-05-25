package com.nba.sdui.core.tokens

import android.util.Log
import com.nba.sdui.core.renderer.ColorTokenResolver
import org.json.JSONObject

/**
 * Resolves `nba.team.*` color tokens for a given team and theme from the
 * bundled `tokens/color-tokens.json` team section.
 *
 * Resolution path:
 *   semantic[token] → mode name → mode[teamId] (or _default) → palette role / ref / literal
 */
object TeamColorRegistry {

    private const val TAG = "TeamColorRegistry"

    private data class TeamPalette(
        val primary: String,
        val secondary: String,
        val tertiary: String?,
    )

    private sealed class ModeValue {
        data class Role(val role: String) : ModeValue()
        data class Ref(val ref: String) : ModeValue()
        data class Literal(val hex: String) : ModeValue()
    }

    private data class SemanticEntry(
        val mode: String?,
        val dark: String?,
        val light: String?,
    )

    private val palettes: Map<String, TeamPalette> by lazy { loadPalettes() }
    private val modes: Map<String, Map<String, ModeValue>> by lazy { loadModes() }
    private val semantic: Map<String, SemanticEntry> by lazy { loadSemantic() }

    /**
     * Resolve a team color token to a hex string.
     *
     * @param token Full token name (e.g. `nba.team.bg`)
     * @param teamId Three-letter lowercase team abbreviation (e.g. `atl`, `bkn`)
     * @param theme `"dark"` or `"light"`
     * @return Resolved hex color string, or null if the team/token is unknown.
     */
    fun resolveTeamColor(token: String, teamId: String, theme: String): String? {
        val entry = semantic[token]
        if (entry == null) {
            Log.w(TAG, "team_token_unknown: $token")
            return null
        }

        val modeName = if (entry.mode != null) {
            entry.mode
        } else {
            if (theme == "dark") entry.dark else entry.light
        }

        if (modeName == null) {
            Log.w(TAG, "team_token_no_mode: $token theme=$theme")
            return null
        }

        val mode = modes[modeName]
        if (mode == null) {
            Log.w(TAG, "team_mode_missing: $modeName")
            return null
        }

        val palette = palettes[teamId]
        if (palette == null) {
            Log.w(TAG, "team_unknown: $teamId")
            return null
        }

        val modeValue = mode[teamId] ?: mode["_default"]
        if (modeValue == null) {
            Log.w(TAG, "team_mode_no_default: $modeName teamId=$teamId")
            return null
        }

        return when (modeValue) {
            is ModeValue.Role -> paletteColorForRole(palette, modeValue.role, teamId)
            is ModeValue.Ref -> resolveRef(modeValue.ref)
            is ModeValue.Literal -> modeValue.hex
        }
    }

    private fun paletteColorForRole(palette: TeamPalette, role: String, teamId: String): String? {
        return when (role) {
            "primary" -> palette.primary
            "secondary" -> palette.secondary
            "tertiary" -> palette.tertiary ?: run {
                Log.w(TAG, "team_palette_no_tertiary: $teamId")
                null
            }
            else -> {
                Log.w(TAG, "team_palette_unknown_role: $role teamId=$teamId")
                null
            }
        }
    }

    /**
     * Resolve a `{ "ref": "nba.color.primary.N" }` value through the existing
     * ColorTokenResolver semantic/palette chain. Returns the hex string for the
     * mode-independent palette entry (team refs always point to grey-scale primitives).
     */
    private fun resolveRef(ref: String): String? {
        return ColorTokenResolver.resolveTokenToHex(ref)
    }

    // ── JSON loading ──────────────────────────────────────────────────────

    private fun loadPalettes(): Map<String, TeamPalette> {
        val team = loadTeamSection()
        val palettesJson = team.getJSONObject("palettes")
        val result = linkedMapOf<String, TeamPalette>()
        val iter = palettesJson.keys()
        while (iter.hasNext()) {
            val id = iter.next()
            val obj = palettesJson.getJSONObject(id)
            result[id] = TeamPalette(
                primary = obj.getString("primary"),
                secondary = obj.getString("secondary"),
                tertiary = obj.optString("tertiary", null),
            )
        }
        return result
    }

    private fun loadModes(): Map<String, Map<String, ModeValue>> {
        val team = loadTeamSection()
        val modesJson = team.getJSONObject("modes")
        val result = linkedMapOf<String, Map<String, ModeValue>>()
        val modeIter = modesJson.keys()
        while (modeIter.hasNext()) {
            val modeName = modeIter.next()
            val modeObj = modesJson.getJSONObject(modeName)
            val entries = linkedMapOf<String, ModeValue>()
            val entryIter = modeObj.keys()
            while (entryIter.hasNext()) {
                val key = entryIter.next()
                val raw = modeObj.get(key)
                entries[key] = when (raw) {
                    is String -> ModeValue.Role(raw)
                    is JSONObject -> {
                        when {
                            raw.has("ref") -> ModeValue.Ref(raw.getString("ref"))
                            raw.has("value") -> ModeValue.Literal(raw.getString("value"))
                            else -> ModeValue.Role("primary")
                        }
                    }
                    else -> ModeValue.Role("primary")
                }
            }
            result[modeName] = entries
        }
        return result
    }

    private fun loadSemantic(): Map<String, SemanticEntry> {
        val team = loadTeamSection()
        val semanticJson = team.getJSONObject("semantic")
        val result = linkedMapOf<String, SemanticEntry>()
        val iter = semanticJson.keys()
        while (iter.hasNext()) {
            val token = iter.next()
            val obj = semanticJson.getJSONObject(token)
            result[token] = SemanticEntry(
                mode = obj.optString("mode", null),
                dark = obj.optString("dark", null),
                light = obj.optString("light", null),
            )
        }
        return result
    }

    private fun loadTeamSection(): JSONObject {
        val classLoader = TeamColorRegistry::class.java.classLoader
            ?: throw IllegalStateException("No class loader available for TeamColorRegistry")
        val stream = classLoader.getResourceAsStream("tokens/color-tokens.json")
            ?: throw IllegalStateException("Missing token resource: tokens/color-tokens.json")
        val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        return JSONObject(text).getJSONObject("team")
    }
}
