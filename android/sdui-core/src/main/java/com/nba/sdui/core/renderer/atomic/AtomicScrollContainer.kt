package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Alignment
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.CrossAlignment
import com.nba.sdui.core.models.generated.UIDirection
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicScrollContainer — renders a scrollable list (LazyRow / LazyColumn)
 * or a paged HorizontalPager when paging is enabled. Box-model chrome
 * comes from [AtomicBox]; this renderer only owns the scroll layout.
 */
@Composable
fun AtomicScrollContainer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    depth: Int = 0,
    onStateChange: (String, Any) -> Unit = { _, _ -> },
    sectionSlotDepth: Int = 0
) {
    val children = element.children.orEmpty()
    val gap = element.gap?.toInt()?.dp ?: 0.dp
    val isHorizontal = element.direction != UIDirection.Column
    val crossAxis = when (element.crossAlignment) {
        CrossAlignment.Center -> if (isHorizontal) ComposeAlignment.CenterVertically else ComposeAlignment.CenterHorizontally
        CrossAlignment.End -> if (isHorizontal) ComposeAlignment.Bottom else ComposeAlignment.End
        else -> if (isHorizontal) ComposeAlignment.Top else ComposeAlignment.Start
    }
    if (element.snapAlignment != null && element.paging != true) {
        Log.w(TAG, "snapAlignment=${element.snapAlignment} requires paging or future snap support; rendering normal scroll")
    }
    if (element.showIndicators == true) {
        Log.w(TAG, "showIndicators=true is decoded but Compose Lazy containers do not expose scrollbars here; scrolling remains enabled")
    }

    AtomicBox(element, screenState, onAction) { boxModifier ->
        val a11yModifier = boxModifier.applyAccessibility(element.accessibility)
        if (element.paging == true) {
            val pagerState = rememberPagerState(pageCount = { children.size })
            if (isHorizontal) {
                HorizontalPager(
                    state = pagerState,
                    modifier = a11yModifier,
                    pageSpacing = gap
                ) { page ->
                    Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                        AtomicRouter(children[page], screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                }
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = a11yModifier,
                    pageSpacing = gap
                ) { page ->
                    Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                        AtomicRouter(children[page], screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                }
            }
        } else if (isHorizontal) {
            LazyRow(
                modifier = a11yModifier,
                horizontalArrangement = horizontalArrangement(element.alignment, gap),
                verticalAlignment = crossAxis as ComposeAlignment.Vertical
            ) {
                itemsIndexed(children) { _, child ->
                    Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                        AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = a11yModifier,
                verticalArrangement = verticalArrangement(element.alignment, gap),
                horizontalAlignment = crossAxis as ComposeAlignment.Horizontal
            ) {
                itemsIndexed(children) { _, child ->
                    Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                        AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                    }
                }
            }
        }
    }
}

private fun childCrossAxisModifier(element: AtomicElement, isHorizontal: Boolean): Modifier {
    if (element.crossAlignment != CrossAlignment.Stretch) return Modifier
    return if (isHorizontal) Modifier.fillMaxHeight() else Modifier.fillMaxWidth()
}

private fun horizontalArrangement(alignment: Alignment?, gap: androidx.compose.ui.unit.Dp): Arrangement.Horizontal {
    return when (alignment) {
        Alignment.Center -> Arrangement.spacedBy(gap, ComposeAlignment.CenterHorizontally)
        Alignment.End -> Arrangement.spacedBy(gap, ComposeAlignment.End)
        Alignment.SpaceBetween -> Arrangement.SpaceBetween
        Alignment.SpaceAround -> Arrangement.SpaceAround
        Alignment.SpaceEvenly -> Arrangement.SpaceEvenly
        else -> Arrangement.spacedBy(gap, ComposeAlignment.Start)
    }
}

private fun verticalArrangement(alignment: Alignment?, gap: androidx.compose.ui.unit.Dp): Arrangement.Vertical {
    return when (alignment) {
        Alignment.Center -> Arrangement.spacedBy(gap, ComposeAlignment.CenterVertically)
        Alignment.End -> Arrangement.spacedBy(gap, ComposeAlignment.Bottom)
        Alignment.SpaceBetween -> Arrangement.SpaceBetween
        Alignment.SpaceAround -> Arrangement.SpaceAround
        Alignment.SpaceEvenly -> Arrangement.SpaceEvenly
        else -> Arrangement.spacedBy(gap, ComposeAlignment.Top)
    }
}

private const val TAG = "AtomicScrollContainer"
