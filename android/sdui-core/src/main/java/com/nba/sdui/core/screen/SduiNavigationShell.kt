package com.nba.sdui.core.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.nba.sdui.core.models.Navigation
import com.nba.sdui.core.models.NavigationItem

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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = item.selected,
                        onClick = { item.targetUri?.let(onNavigate) },
                        icon = { Icon(imageVector = navIcon(item), contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}

private fun navIcon(item: NavigationItem): ImageVector {
    return when (item.icon?.lowercase()) {
        "teams", "groups" -> Icons.AutoMirrored.Filled.List
        "standings", "table_chart" -> Icons.Default.Info
        else -> Icons.Default.Home
    }
}
