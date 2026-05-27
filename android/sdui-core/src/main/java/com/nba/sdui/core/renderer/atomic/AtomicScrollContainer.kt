package com.nba.sdui.core.renderer.atomic

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Alignment
import com.nba.sdui.core.models.generated.AtomicElement
import com.nba.sdui.core.models.generated.BadgeAlignment
import com.nba.sdui.core.models.generated.CrossAlignment
import com.nba.sdui.core.models.generated.Style
import com.nba.sdui.core.models.generated.UIDirection
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.LayoutTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.request.RequestEnvelopeBuilder
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
    // TODO(phase3): swap for `LocalSduiFormFactor.current` once the
    // form-factor classifier is plumbed end-to-end.
    val formFactor = RequestEnvelopeBuilder.defaultFormFactor()
    val gap = LayoutTokenResolver.dp(element.gap, formFactor)
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
    val indicatorStyle = element.pageIndicator?.style
    val showPageDots = element.paging == true && indicatorStyle == Style.Dots && children.size > 1
    val showPageDashes = element.paging == true && indicatorStyle == Style.Dashes && children.size > 1

    AtomicBox(element, screenState, onAction) { boxModifier ->
        val a11yModifier = boxModifier.applyAccessibility(element.accessibility)
        if (element.paging == true) {
            val pagerState = rememberPagerState(pageCount = { children.size })
            val dotsOnTop = when (element.pageIndicator?.alignment) {
                BadgeAlignment.TopStart, BadgeAlignment.TopCenter, BadgeAlignment.TopEnd -> true
                else -> false
            }
            val dotRowArrangement = pageIndicatorHorizontalArrangement(element.pageIndicator?.alignment)

            if (showPageDashes) {
                Box(modifier = a11yModifier) {
                    if (isHorizontal) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier,
                            pageSpacing = gap
                        ) { page ->
                            Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                                AtomicRouter(children[page], screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                            }
                        }
                    } else {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier,
                            pageSpacing = gap
                        ) { page ->
                            Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                                AtomicRouter(children[page], screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                            }
                        }
                    }
                    PageIndicatorDashes(
                        count = children.size,
                        activePage = pagerState.currentPage,
                        inactiveColor = resolvedIndicatorColor(element.pageIndicator?.color, Color.White.copy(alpha = 0.4f)),
                        activeColor = resolvedIndicatorColor(element.pageIndicator?.activeColor, Color.White),
                        modifier = Modifier
                            .align(ComposeAlignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                Column(modifier = a11yModifier) {
                    if (showPageDots && dotsOnTop) {
                        PageIndicatorDots(
                            count = children.size,
                            activePage = pagerState.currentPage,
                            inactiveColor = resolvedIndicatorColor(element.pageIndicator?.color, Color.White.copy(alpha = 0.45f)),
                            activeColor = resolvedIndicatorColor(element.pageIndicator?.activeColor, Color.White),
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            groupHorizontalArrangement = dotRowArrangement
                        )
                    }
                    if (isHorizontal) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier,
                            pageSpacing = gap
                        ) { page ->
                            Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                                AtomicRouter(children[page], screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                            }
                        }
                    } else {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier,
                            pageSpacing = gap
                        ) { page ->
                            Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                                AtomicRouter(children[page], screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                            }
                        }
                    }
                    if (showPageDots && !dotsOnTop) {
                        PageIndicatorDots(
                            count = children.size,
                            activePage = pagerState.currentPage,
                            inactiveColor = resolvedIndicatorColor(element.pageIndicator?.color, Color.White.copy(alpha = 0.45f)),
                            activeColor = resolvedIndicatorColor(element.pageIndicator?.activeColor, Color.White),
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            groupHorizontalArrangement = dotRowArrangement
                        )
                    }
                }
            }
        } else if (isHorizontal) {
            val listState = rememberLazyListState()
            if (showPageDots) {
                Box(modifier = a11yModifier) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier,
                        horizontalArrangement = horizontalArrangement(element.alignment, gap),
                        verticalAlignment = crossAxis as ComposeAlignment.Vertical
                    ) {
                        itemsIndexed(
                            children,
                            key = { i, child -> child.id ?: "scroll_child_$i" }
                        ) { _, child ->
                            Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                                AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                            }
                        }
                    }
                    PageIndicatorDots(
                        count = children.size,
                        activePage = listState.firstVisibleItemIndex,
                        inactiveColor = resolvedIndicatorColor(element.pageIndicator?.color, Color.White.copy(alpha = 0.45f)),
                        activeColor = resolvedIndicatorColor(element.pageIndicator?.activeColor, Color.White),
                        modifier = Modifier
                            .align(pageIndicatorAlignment(element.pageIndicator?.alignment))
                            .padding(8.dp)
                    )
                }
            } else {
                LazyRow(
                    state = listState,
                    modifier = a11yModifier,
                    horizontalArrangement = horizontalArrangement(element.alignment, gap),
                    verticalAlignment = crossAxis as ComposeAlignment.Vertical
                ) {
                    itemsIndexed(
                        children,
                        key = { i, child -> child.id ?: "scroll_child_$i" }
                    ) { _, child ->
                        Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                            AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                        }
                    }
                }
            }
        } else {
            val listState = rememberLazyListState()
            if (showPageDots) {
                Box(modifier = a11yModifier) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier,
                        verticalArrangement = verticalArrangement(element.alignment, gap),
                        horizontalAlignment = crossAxis as ComposeAlignment.Horizontal
                    ) {
                        itemsIndexed(
                            children,
                            key = { i, child -> child.id ?: "scroll_child_$i" }
                        ) { _, child ->
                            Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                                AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                            }
                        }
                    }
                    PageIndicatorDots(
                        count = children.size,
                        activePage = listState.firstVisibleItemIndex,
                        inactiveColor = resolvedIndicatorColor(element.pageIndicator?.color, Color.White.copy(alpha = 0.45f)),
                        activeColor = resolvedIndicatorColor(element.pageIndicator?.activeColor, Color.White),
                        modifier = Modifier
                            .align(pageIndicatorAlignment(element.pageIndicator?.alignment))
                            .padding(8.dp)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = a11yModifier,
                    verticalArrangement = verticalArrangement(element.alignment, gap),
                    horizontalAlignment = crossAxis as ComposeAlignment.Horizontal
                ) {
                    itemsIndexed(
                        children,
                        key = { i, child -> child.id ?: "scroll_child_$i" }
                    ) { _, child ->
                        Box(modifier = childCrossAxisModifier(element, isHorizontal)) {
                            AtomicRouter(child, screenState, onAction, depth = depth + 1, onStateChange = onStateChange, sectionSlotDepth = sectionSlotDepth)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicatorDots(
    count: Int,
    activePage: Int,
    inactiveColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier,
    groupHorizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    Row(
        modifier = modifier,
        horizontalArrangement = groupHorizontalArrangement
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(count) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (index == activePage) activeColor else inactiveColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PageIndicatorDashes(
    count: Int,
    activePage: Int,
    inactiveColor: Color,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        if (index == activePage) activeColor else inactiveColor,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun resolvedIndicatorColor(value: String?, fallback: Color): Color {
    val resolved = ColorTokenResolver.resolve(value)
    return if (resolved == Color.Unspecified) fallback else resolved
}

private fun pageIndicatorHorizontalArrangement(alignment: BadgeAlignment?): Arrangement.Horizontal {
    return when (alignment) {
        BadgeAlignment.TopStart, BadgeAlignment.BottomStart, BadgeAlignment.CenterStart -> Arrangement.Start
        BadgeAlignment.TopEnd, BadgeAlignment.BottomEnd, BadgeAlignment.CenterEnd -> Arrangement.End
        BadgeAlignment.TopCenter, BadgeAlignment.BottomCenter, BadgeAlignment.Center, null -> Arrangement.Center
    }
}

/** Used when page dots overlay a non-paging LazyRow/LazyColumn (legacy path). */
private fun pageIndicatorAlignment(alignment: BadgeAlignment?): ComposeAlignment {
    return when (alignment) {
        BadgeAlignment.TopStart -> ComposeAlignment.TopStart
        BadgeAlignment.TopCenter -> ComposeAlignment.TopCenter
        BadgeAlignment.TopEnd -> ComposeAlignment.TopEnd
        BadgeAlignment.CenterStart -> ComposeAlignment.CenterStart
        BadgeAlignment.Center -> ComposeAlignment.Center
        BadgeAlignment.CenterEnd -> ComposeAlignment.CenterEnd
        BadgeAlignment.BottomStart -> ComposeAlignment.BottomStart
        BadgeAlignment.BottomEnd -> ComposeAlignment.BottomEnd
        BadgeAlignment.BottomCenter, null -> ComposeAlignment.BottomCenter
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
