package com.nba.sdui.core.renderer

import android.util.Log

/**
 * Resolves cross-platform SDUI icon tokens (e.g. `sdui:play`) to the
 * platform-native asset. On Android the resolved value is a Material
 * icon class name (e.g. `PlayArrow`, `SportsBasketball`) — the same
 * string that appears in the `material` column of the canonical
 * `schema/icon-tokens.json`.
 *
 * Consumers map that name to an `androidx.compose.material.icons.Icons`
 * field via a platform lookup (typically a `when` expression over the
 * names they expect to receive).
 *
 * The token vocabulary lives in `schema/icon-tokens.json`; unknown
 * tokens fall back to `sdui:warning`. Non-token strings pass through
 * unchanged so renderers can keep accepting raw Material icon names
 * for back-compat while the server migrates to tokens.
 */
object IconTokenResolver {

    private const val TAG = "IconTokenResolver"
    private const val TOKEN_PREFIX = "sdui:"
    private const val FALLBACK_TOKEN = "sdui:warning"

    /**
     * Snapshot of `schema/icon-tokens.json` — the `material` column
     * keyed by token. Kept inline so the library carries no external
     * resource dependency; regenerate whenever the canonical icon list
     * changes.
     */
    private val tokens: Map<String, String> = mapOf(
        "sdui:play"        to "PlayArrow",
        "sdui:pause"       to "Pause",
        "sdui:back"        to "ArrowBack",
        "sdui:forward"     to "ArrowForward",
        "sdui:settings"    to "Settings",
        "sdui:expand"      to "KeyboardArrowDown",
        "sdui:collapse"    to "KeyboardArrowUp",
        "sdui:check"       to "Check",
        "sdui:warning"     to "Warning",
        "sdui:live"        to "Sensors",
        "sdui:person"      to "AccountCircle",
        "sdui:close"       to "Close",
        "sdui:search"      to "Search",
        "sdui:share"       to "Share",
        "sdui:favorite"    to "FavoriteBorder",
        "sdui:favorited"   to "Favorite",
        "sdui:fullscreen"  to "Fullscreen",
        "sdui:pip"         to "PictureInPicture",
        "sdui:cast"        to "Cast",
        "sdui:info"        to "Info",
        "sdui:calendar"    to "CalendarToday",
        "sdui:refresh"     to "Refresh",
        "sdui:home"        to "Home",
        "sdui:basketball"  to "SportsBasketball",
        "sdui:video"       to "PlayCircle",
        "sdui:leaderboard" to "Leaderboard",
        "sdui:grid"        to "Widgets"
    )

    /**
     * Convert a server icon string into a Material icon class name.
     *
     * @return the Material name when [token] is a recognised
     *   `sdui:` token; the fallback token's Material name when the
     *   token is unknown; the input unchanged when it is not an
     *   `sdui:` token at all; `null` when [token] is null/blank.
     */
    fun resolve(token: String?): String? {
        if (token.isNullOrBlank()) return null
        if (!token.startsWith(TOKEN_PREFIX)) return token

        tokens[token]?.let { return it }
        Log.w(TAG, "unknown icon token $token; falling back to $FALLBACK_TOKEN")
        return tokens[FALLBACK_TOKEN]
    }
}
