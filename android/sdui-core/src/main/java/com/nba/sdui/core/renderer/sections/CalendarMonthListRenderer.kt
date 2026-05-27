package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.adapters.mapCalendarMonthList
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.state.SduiAction
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch
import java.time.format.TextStyle as JavaTextStyle

private const val TAG = "CalendarMonthListRenderer"

private const val MONTH_HEADER_TEXT = "token:nba.label.primary"
private const val WEEKDAY_TEXT = "token:nba.label.secondary"
private const val DAY_TEXT = "token:nba.label.primary"
private const val DAY_TEXT_MUTED = "token:nba.label.tertiary"
private const val DAY_TEXT_SELECTION = "token:nba.label.selection"
private const val DAY_TEXT_TODAY = "token:nba.label.interactive"
private const val TODAY_BG = "token:nba.label.interactive"
private const val SELECTED_BG = "token:nba.bg.disabled"
private const val DOT_DEFAULT = "token:nba.label.secondary"
private const val DOT_ACCENT = "token:nba.label.accent.brand"
private const val HEADER_BG = "token:nba.bg.secondary"
private const val FAB_BG = "token:nba.button.primary.bg"
private const val FAB_TEXT = "token:nba.button.primary.label"

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarMonthListRenderer(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapCalendarMonthList(section)
    if (model == null) {
        Log.w(TAG, "Unable to parse calendar month list data for section ${section.id}")
        return
    }

    val selectedIso = (screenState[model.stateKey] as? String) ?: model.selectedDate
    val selectedDate = parseIsoDate(selectedIso)
    val defaultDate = parseIsoDate(model.defaultDate)
    if (selectedDate == null || defaultDate == null) {
        Log.w(TAG, "Failed to parse selected/default date for section ${section.id}")
        return
    }

    val minDate = model.minDate?.let(::parseIsoDate) ?: selectedDate.minusMonths(6)
    val maxDate = model.maxDate?.let(::parseIsoDate) ?: selectedDate.plusMonths(6)
    if (maxDate.isBefore(minDate)) {
        Log.w(TAG, "Calendar month range invalid for section ${section.id}: $minDate > $maxDate")
        return
    }

    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val monthStarts = remember(minDate, maxDate) { monthsInRange(minDate, maxDate) }
    val weekdayLabels = remember(locale, firstDayOfWeek) {
        rotatedWeekdayLabels(locale, firstDayOfWeek)
    }

    val selectedMonth = selectedDate.withDayOfMonth(1)
    val defaultMonth = defaultDate.withDayOfMonth(1)
    val initialMonthIndex = remember(monthStarts, selectedMonth) {
        monthStarts.indexOf(selectedMonth).takeIf { it >= 0 } ?: 0
    }
    val defaultMonthIndex = remember(monthStarts, defaultMonth) {
        monthStarts.indexOf(defaultMonth).takeIf { it >= 0 } ?: 0
    }

    val colors = rememberMonthListColors()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var didInitialScroll by remember(section.id) { mutableStateOf(false) }

    LaunchedEffect(monthStarts, initialMonthIndex, didInitialScroll) {
        if (!didInitialScroll && monthStarts.isNotEmpty()) {
            listState.scrollToItem(initialMonthIndex)
            didInitialScroll = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .applyAccessibility(section.accessibility)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState
        ) {
            monthStarts.forEach { monthStart ->
                val monthLabel = formatMonthYear(monthStart, locale)
                stickyHeader(key = "header-${monthStart}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.headerBackground)
                            .padding(top = 10.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.monthHeader,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            weekdayLabels.forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.weekdayLabel,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                item(key = "month-${monthStart}") {
                    MonthGrid(
                        monthStart = monthStart,
                        selectedDate = selectedDate,
                        defaultDate = defaultDate,
                        firstDayOfWeek = firstDayOfWeek,
                        colors = colors,
                        hasGames = { isoDate ->
                            (model.dateMetadata[isoDate]?.gameCount ?: 0) > 0
                        },
                        hasTeamGame = { isoDate ->
                            model.dateMetadata[isoDate]?.hasTeamGame == true
                        },
                        onDateSelected = { date ->
                            val isoDate = date.toString()
                            onStateChange(model.stateKey, isoDate)
                            onAction(model.onDateSelected)
                        }
                    )
                }
            }

            item(key = "bottom-padding") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                scope.launch {
                    listState.animateScrollToItem(defaultMonthIndex)
                }
            },
            containerColor = colors.fabBackground,
            contentColor = colors.fabText,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text(
                text = "Today",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthGrid(
    monthStart: LocalDate,
    selectedDate: LocalDate,
    defaultDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
    colors: MonthListColors,
    hasGames: (String) -> Boolean,
    hasTeamGame: (String) -> Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val cells = remember(monthStart, firstDayOfWeek) {
        monthCells(monthStart, firstDayOfWeek)
    }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        maxItemsInEachRow = 7
    ) {
        cells.forEach { cellDate ->
            if (cellDate == null) {
                Spacer(
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                )
            } else {
                val isoDate = cellDate.toString()
                val isSelected = selectedDate == cellDate
                val isDefaultDate = defaultDate == cellDate
                val showDot = hasGames(isoDate)
                val dotColor = if (hasTeamGame(isoDate)) colors.dotAccent else colors.dotDefault

                DateCell(
                    date = cellDate,
                    isSelected = isSelected,
                    isDefaultDate = isDefaultDate,
                    showDot = showDot,
                    dotColor = dotColor,
                    colors = colors,
                    onDateSelected = { onDateSelected(cellDate) },
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isDefaultDate: Boolean,
    showDot: Boolean,
    dotColor: Color,
    colors: MonthListColors,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && isDefaultDate -> colors.defaultBackground
        isSelected -> colors.selectedBackground
        isDefaultDate -> colors.defaultBackground
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected && isDefaultDate -> colors.selectionText
        isSelected -> colors.dayText
        isDefaultDate -> colors.selectionText
        showDot -> colors.dayText
        else -> colors.dayTextMuted
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (backgroundColor != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(backgroundColor, CircleShape)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = textColor
            )
            if (showDot) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(6.dp)
                        .background(dotColor, CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onDateSelected
                )
        )
    }
}

@Composable
private fun rememberMonthListColors(): MonthListColors {
    val scheme = MaterialTheme.colorScheme
    val monthHeader = monthListColor(MONTH_HEADER_TEXT, scheme.onSurface)
    val weekdayLabel = monthListColor(WEEKDAY_TEXT, scheme.onSurfaceVariant)
    val dayText = monthListColor(DAY_TEXT, scheme.onSurface)
    val dayTextMuted = monthListColor(DAY_TEXT_MUTED, scheme.onSurfaceVariant)
    val selectionText = monthListColor(DAY_TEXT_SELECTION, scheme.onPrimary)
    val todayText = monthListColor(DAY_TEXT_TODAY, scheme.primary)
    val defaultBackground = monthListColor(TODAY_BG, scheme.primary)
    val selectedBackground = monthListColor(SELECTED_BG, scheme.surfaceVariant)
    val dotDefault = monthListColor(DOT_DEFAULT, scheme.onSurfaceVariant)
    val dotAccent = monthListColor(DOT_ACCENT, scheme.primary)
    val headerBackground = monthListColor(HEADER_BG, scheme.surface)
    val fabBackground = monthListColor(FAB_BG, scheme.primary)
    val fabText = monthListColor(FAB_TEXT, scheme.onPrimary)

    return remember(
        monthHeader,
        weekdayLabel,
        dayText,
        dayTextMuted,
        selectionText,
        todayText,
        defaultBackground,
        selectedBackground,
        dotDefault,
        dotAccent,
        headerBackground,
        fabBackground,
        fabText
    ) {
        MonthListColors(
            monthHeader = monthHeader,
            weekdayLabel = weekdayLabel,
            dayText = dayText,
            dayTextMuted = dayTextMuted,
            selectionText = selectionText,
            todayText = todayText,
            defaultBackground = defaultBackground,
            selectedBackground = selectedBackground,
            dotDefault = dotDefault,
            dotAccent = dotAccent,
            headerBackground = headerBackground,
            fabBackground = fabBackground,
            fabText = fabText
        )
    }
}

@Composable
private fun monthListColor(token: String, fallback: Color): Color {
    val resolved = ColorTokenResolver.resolve(token)
    return if (resolved != Color.Unspecified) resolved else fallback
}

private data class MonthListColors(
    val monthHeader: Color,
    val weekdayLabel: Color,
    val dayText: Color,
    val dayTextMuted: Color,
    val selectionText: Color,
    val todayText: Color,
    val defaultBackground: Color,
    val selectedBackground: Color,
    val dotDefault: Color,
    val dotAccent: Color,
    val headerBackground: Color,
    val fabBackground: Color,
    val fabText: Color
)

private fun monthsInRange(minDate: LocalDate, maxDate: LocalDate): List<LocalDate> {
    val start = minDate.withDayOfMonth(1)
    val end = maxDate.withDayOfMonth(1)
    val months = mutableListOf<LocalDate>()
    var current = start
    while (!current.isAfter(end)) {
        months += current
        current = current.plusMonths(1)
    }
    return months
}

private fun monthCells(monthStart: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
    val firstDate = monthStart.withDayOfMonth(1)
    val leadingEmpty =
        (firstDate.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val daysInMonth = firstDate.lengthOfMonth()

    val cells = buildList<LocalDate?> {
        repeat(leadingEmpty) { add(null) }
        for (day in 1..daysInMonth) {
            add(firstDate.withDayOfMonth(day))
        }
    }.toMutableList()

    val trailing = (7 - (cells.size % 7)) % 7
    repeat(trailing) {
        cells += null
    }
    return cells
}

private fun rotatedWeekdayLabels(locale: Locale, firstDayOfWeek: DayOfWeek): List<String> {
    return List(7) { offset ->
        firstDayOfWeek
            .plus(offset.toLong())
            .getDisplayName(JavaTextStyle.SHORT, locale)
            .replaceFirstChar { it.uppercase(locale) }
    }
}

private fun formatMonthYear(date: LocalDate, locale: Locale): String {
    val month = date.month
        .getDisplayName(JavaTextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) }
    return "$month ${date.year}"
}
