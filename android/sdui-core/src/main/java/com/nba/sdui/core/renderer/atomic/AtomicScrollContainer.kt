package com.nba.sdui.core.renderer.atomic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.AtomicElement
import com.nba.sdui.core.state.SduiAction

/**
 * AtomicScrollContainer — renders a scrollable list (LazyRow / LazyColumn)
 * or a paged HorizontalPager when paging is enabled.
 */
@Composable
fun AtomicScrollContainer(
    element: AtomicElement,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    modifier: Modifier = Modifier,
    depth: Int = 0
) {
    val children = element.children.orEmpty()
    val gap = element.gap?.dp ?: 0.dp
    val isHorizontal = element.direction != "column"

    if (element.paging == true && isHorizontal) {
        val pagerState = rememberPagerState(pageCount = { children.size })
        HorizontalPager(
            state = pagerState,
            modifier = modifier,
            pageSpacing = gap
        ) { page ->
            AtomicRouter(children[page], screenState, onAction, depth = depth + 1)
        }
    } else if (isHorizontal) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            itemsIndexed(children) { _, child ->
                AtomicRouter(child, screenState, onAction, depth = depth + 1)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(gap),
            contentPadding = PaddingValues(vertical = 0.dp)
        ) {
            itemsIndexed(children) { _, child ->
                AtomicRouter(child, screenState, onAction, depth = depth + 1)
            }
        }
    }
}
