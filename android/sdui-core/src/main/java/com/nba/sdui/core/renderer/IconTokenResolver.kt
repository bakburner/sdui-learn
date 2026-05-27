package com.nba.sdui.core.renderer

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

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
        "sdui:grid"        to "Widgets",
        "sdui:lock"        to "Lock",
        "sdui:more"        to "MoreVert"
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

    /**
     * Resolve a wire icon token to a Compose [ImageVector], or null when
     * the token cannot be mapped (caller supplies a fallback).
     *
     * Each branch uses [Icons.Filled] with explicit `filled.*` imports so
     * extended Material icons are in scope at compile time.
     */
    fun imageVector(token: String?): ImageVector? {
        val materialName = resolve(token) ?: return null
        return materialNameToImageVector(materialName)
    }

    private fun materialNameToImageVector(name: String): ImageVector? = when (name) {
        "PlayArrow"         -> Icons.Filled.PlayArrow
        "Pause"             -> Icons.Filled.Pause
        "ArrowBack"         -> Icons.Filled.ArrowBack
        "ArrowForward"      -> Icons.Filled.ArrowForward
        "Settings"          -> Icons.Filled.Settings
        "KeyboardArrowDown" -> Icons.Filled.KeyboardArrowDown
        "KeyboardArrowUp"   -> Icons.Filled.KeyboardArrowUp
        "Check"             -> Icons.Filled.Check
        "Warning"           -> Icons.Filled.Warning
        "Sensors"           -> Icons.Filled.Sensors
        "AccountCircle"     -> Icons.Filled.AccountCircle
        "Close"             -> Icons.Filled.Close
        "Search"            -> Icons.Filled.Search
        "Share"             -> Icons.Filled.Share
        "FavoriteBorder"    -> Icons.Filled.FavoriteBorder
        "Favorite"          -> Icons.Filled.Favorite
        "Fullscreen"        -> Icons.Filled.Fullscreen
        "PictureInPicture"  -> Icons.Filled.PictureInPicture
        "Cast"              -> Icons.Filled.Cast
        "Info"              -> Icons.Filled.Info
        "CalendarToday"     -> Icons.Filled.CalendarToday
        "Refresh"           -> Icons.Filled.Refresh
        "Home"              -> Icons.Filled.Home
        "SportsBasketball"  -> Icons.Filled.SportsBasketball
        "PlayCircle"        -> Icons.Filled.PlayCircle
        "Leaderboard"       -> Icons.Filled.Leaderboard
        "Widgets"           -> Icons.Filled.Widgets
        "Lock"              -> Icons.Filled.Lock
        "MoreVert"          -> Icons.Filled.MoreVert
        "List"              -> Icons.AutoMirrored.Filled.List
        else                -> null
    }
}
