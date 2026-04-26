package com.nba.sdui.core.screen

import android.util.Log
import androidx.compose.foundation.layout.padding
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.nba.sdui.core.models.generated.Navigation
import com.nba.sdui.core.models.generated.NavigationItem
import com.nba.sdui.core.renderer.IconTokenResolver

private const val TAG = "SduiNavigationShell"

@Composable
fun SduiNavigationShell(
    navigation: Navigation?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val items = navigation?.items.orEmpty()
    if (items.isEmpty()) {
        content(modifier)
        return
    }

    val selectedIndex = items.indexOfFirst { it.selected == true }.let { if (it >= 0) it else 0 }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = item.selected == true,
                        onClick = { item.targetURI?.let(onNavigate) },
                        icon = { Icon(imageVector = navIcon(item), contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = selectedIndex,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()).togetherWith(
                    slideOutHorizontally { -it } + fadeOut()
                )
            },
            label = "navShellContent"
        ) { _ ->
            content(Modifier.padding(innerPadding))
        }
    }
}

private fun navIcon(item: NavigationItem): ImageVector {
    val resolved = IconTokenResolver.resolve(item.icon) ?: return Icons.Default.Info
    return materialNameToImageVector(resolved) ?: run {
        Log.w(TAG, "no ImageVector mapping for material name '$resolved'; falling back to Info")
        Icons.Default.Info
    }
}

/**
 * Reflection-free lookup from the Material icon class name emitted by
 * [IconTokenResolver] (mirroring the `material` column of
 * `schema/icon-tokens.json`) to the corresponding
 * `androidx.compose.material.icons.Icons` field.
 *
 * New tokens added to `icon-tokens.json` must be mirrored here; unknown
 * names return `null` and the caller falls back to a placeholder icon.
 */
private fun materialNameToImageVector(name: String): ImageVector? = when (name) {
    "PlayArrow"         -> Icons.Default.PlayArrow
    "Pause"             -> Icons.Default.Pause
    "ArrowBack"         -> Icons.Filled.ArrowBack
    "ArrowForward"      -> Icons.Filled.ArrowForward
    "Settings"          -> Icons.Default.Settings
    "KeyboardArrowDown" -> Icons.Default.KeyboardArrowDown
    "KeyboardArrowUp"   -> Icons.Default.KeyboardArrowUp
    "Check"             -> Icons.Default.Check
    "Warning"           -> Icons.Default.Warning
    "Sensors"           -> Icons.Default.Sensors
    "AccountCircle"     -> Icons.Default.AccountCircle
    "Close"             -> Icons.Default.Close
    "Search"            -> Icons.Default.Search
    "Share"             -> Icons.Default.Share
    "FavoriteBorder"    -> Icons.Default.FavoriteBorder
    "Favorite"          -> Icons.Default.Favorite
    "Fullscreen"        -> Icons.Default.Fullscreen
    "PictureInPicture"  -> Icons.Default.PictureInPicture
    "Cast"              -> Icons.Default.Cast
    "Info"              -> Icons.Default.Info
    "CalendarToday"     -> Icons.Default.CalendarToday
    "Refresh"           -> Icons.Default.Refresh
    "Home"              -> Icons.Default.Home
    "SportsBasketball"  -> Icons.Default.SportsBasketball
    "PlayCircle"        -> Icons.Default.PlayCircle
    "Leaderboard"       -> Icons.Default.Leaderboard
    "Widgets"           -> Icons.Default.Widgets
    "List"              -> Icons.AutoMirrored.Filled.List
    else                -> null
}
