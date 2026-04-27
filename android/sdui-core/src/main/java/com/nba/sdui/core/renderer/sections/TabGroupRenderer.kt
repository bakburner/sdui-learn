package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.mapTabGroup
import com.nba.sdui.core.renderer.adapters.mapTabMutateAction
import com.nba.sdui.core.renderer.SectionRouter
import com.nba.sdui.core.state.SduiAction

/**
 * TabGroup Renderer - Displays tabbed navigation with dynamic content.
 * 
 * Tab selection is managed by client-side state mutation:
 * - Selecting a tab fires a mutate action updating the screen state
 * - The tab content swaps without a server round-trip
 */
@Composable
fun TabGroupRenderer(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val data = mapTabGroup(section, screenState)
    
    if (data == null) {
        Text(
            text = "Unable to load tabs",
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.error
        )
        return
    }
    
    // Get current active tab from screen state
    val activeTabIndex = data.tabs.indexOfFirst { it.stateValue == data.activeTabValue }
        .takeIf { it >= 0 } ?: 0
    
    Column(modifier = modifier.fillMaxWidth().applyAccessibility(section.accessibility)) {
        // Tab Row
        TabRow(
            selectedTabIndex = activeTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            data.tabs.forEachIndexed { index, tab ->
                key(tab.stateValue) {
                    Tab(
                        selected = index == activeTabIndex,
                        onClick = {
                            Log.d("TabGroupRenderer", "Tab clicked: ${tab.label}, stateValue=${tab.stateValue}")
                            onStateChange(data.stateKey, tab.stateValue)
                            onAction(mapTabMutateAction(data.stateKey, tab.stateValue))
                        },
                        text = {
                            Text(text = tab.label)
                        }
                    )
                }
            }
        }

        AnimatedContent(
            targetState = activeTabIndex,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()).togetherWith(
                    slideOutHorizontally { -it } + fadeOut()
                )
            },
            label = "tabGroupBody"
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                data.activeSections.forEach { contentSection ->
                    key(contentSection.id) {
                        SectionRouter(
                            section = contentSection,
                            screenState = screenState,
                            onAction = onAction,
                            onStateChange = onStateChange
                        )
                    }
                }
            }
        }
    }
}
