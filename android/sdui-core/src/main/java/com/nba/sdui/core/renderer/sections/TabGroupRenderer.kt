package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.SectionRouter
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.atomic.AtomicRouter
import com.nba.sdui.core.renderer.adapters.mapTabGroup
import com.nba.sdui.core.renderer.interactions.SectionInteractions
import com.nba.sdui.core.state.SduiAction

private const val TAG = "TabGroupRenderer"

/** Native tab-row tokens (aligned with server `secondaryStripSurface`). */
private const val TAB_LABEL_PRIMARY = "token:nba.label.primary"
private const val TAB_LABEL_SECONDARY = "token:nba.label.secondary"
private const val TAB_ACCENT_BRAND = "token:nba.label.accent.brand"
private const val TAB_DIVIDER = "token:nba.divider.moderate"

/**
 * TabGroup — thin host for tabbed section routing.
 *
 * Server-owned: [Section.surface], [Section.subsections] (per-tab mutate),
 * tab metadata and [tabContents]. Optional [Section.data] ui is the tab header only.
 *
 * Platform-native tab row when ui is absent is client-realized presentation;
 * selection still dispatches declared subsection actions.
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

    val activeTabIndex = data.tabs.indexOfFirst { it.stateValue == data.activeTabValue }
        .takeIf { it >= 0 } ?: 0
    val headerUi = section.data?.ui

    val tabColors = rememberTabStripColors()

    Column(modifier = modifier.fillMaxWidth().applyAccessibility(section.accessibility)) {
        if (headerUi != null) {
            AtomicRouter(
                element = headerUi,
                screenState = screenState,
                onAction = onAction,
                onStateChange = onStateChange
            )
        } else {
            ScrollableTabRow(
                selectedTabIndex = activeTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {
                    HorizontalDivider(color = tabColors.divider)
                },
                indicator = { tabPositions ->
                    if (activeTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTabIndex]),
                            height = 2.dp,
                            color = tabColors.indicator
                        )
                    }
                }
            ) {
                data.tabs.forEachIndexed { index, tab ->
                    key(tab.id) {
                        Tab(
                            selected = index == activeTabIndex,
                            onClick = { dispatchTabSelect(section, tab.id, onAction) },
                            selectedContentColor = tabColors.labelPrimary,
                            unselectedContentColor = tabColors.labelSecondary,
                            text = { Text(text = tab.label) }
                        )
                    }
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
            Column(modifier = Modifier.fillMaxWidth()) {
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

@Composable
private fun rememberTabStripColors(): TabStripColors {
    val scheme = MaterialTheme.colorScheme
    val labelPrimary = tabStripColor(TAB_LABEL_PRIMARY, scheme.onSurface)
    val labelSecondary = tabStripColor(TAB_LABEL_SECONDARY, scheme.onSurfaceVariant)
    val indicator = tabStripColor(TAB_ACCENT_BRAND, scheme.primary)
    val divider = tabStripColor(TAB_DIVIDER, scheme.outlineVariant)
    return remember(scheme, labelPrimary, labelSecondary, indicator, divider) {
        TabStripColors(labelPrimary, labelSecondary, indicator, divider)
    }
}

@Composable
private fun tabStripColor(token: String, fallback: Color): Color {
    val resolved = ColorTokenResolver.resolve(token)
    return if (resolved != Color.Unspecified) resolved else fallback
}

private data class TabStripColors(
    val labelPrimary: Color,
    val labelSecondary: Color,
    val indicator: Color,
    val divider: Color
)

private fun dispatchTabSelect(
    section: Section,
    tabId: String,
    onAction: (SduiAction) -> Unit
) {
    val action = SectionInteractions.subsectionPrimaryAction(section, tabId)
    if (action != null) {
        onAction(action)
    } else {
        Log.w(TAG, "missing subsection mutate action sectionId=${section.id} tabId=$tabId")
    }
}
