package com.nba.sdui.core.renderer.sections

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nba.sdui.core.models.generated.Section
import com.nba.sdui.core.renderer.ColorTokenResolver
import com.nba.sdui.core.renderer.applyAccessibility
import com.nba.sdui.core.renderer.adapters.mapCalendarStrip
import com.nba.sdui.core.state.SduiAction
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

private const val TAG = "CalendarStripRenderer"

private const val CELL_TEXT_PRIMARY = "token:nba.label.primary"
private const val CELL_TEXT_SECONDARY = "token:nba.label.secondary"
private const val CELL_TEXT_INTERACTIVE = "token:nba.label.interactive"
private const val CELL_TEXT_SELECTION = "token:nba.label.selection"
private const val CELL_INDICATOR_INTERACTIVE = "token:nba.label.interactive"
private const val CELL_INDICATOR_DISABLED = "token:nba.bg.disabled"
private const val MONTH_LABEL_PRIMARY = "token:nba.label.primary"

@Composable
fun CalendarStripRenderer(
    section: Section,
    screenState: Map<String, Any>,
    onAction: (SduiAction) -> Unit,
    onStateChange: (String, Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val model = mapCalendarStrip(section, screenState)
    if (model == null) {
        Log.w(TAG, "Unable to parse calendar strip data for section ${section.id}")
        return
    }

    val locale = Locale.getDefault()
    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek

    val selectedDate = parseIsoDate(model.selectedDate)
    val defaultDate = parseIsoDate(model.defaultDate)
    if (selectedDate == null || defaultDate == null) {
        Log.w(TAG, "Failed to parse selectedDate or defaultDate for section ${section.id}")
        return
    }

    val minDate = model.minDate?.let { parseIsoDate(it) }
    val maxDate = model.maxDate?.let { parseIsoDate(it) }

    val anchorDate = effectiveMinDate(minDate, selectedDate, firstDayOfWeek)
    val endDate = effectiveMaxDate(maxDate, selectedDate)

    val totalWeeks = weeksBetween(anchorDate, endDate, firstDayOfWeek)
        .coerceAtLeast(1)
    val selectedWeekIndex = weekIndexOf(anchorDate, selectedDate, firstDayOfWeek)
        .coerceIn(0, totalWeeks - 1)

    val pagerState = rememberPagerState(initialPage = selectedWeekIndex) { totalWeeks }

    val centeredWeekStartDate = remember(pagerState.currentPage) {
        weekStartDate(anchorDate, pagerState.currentPage, firstDayOfWeek)
    }
    val monthLabel = remember(centeredWeekStartDate) {
        formatMonthYearLabel(centeredWeekStartDate, locale)
    }

    val weekdayLabels = remember(locale, firstDayOfWeek) {
        rotatedWeekdayLabels(locale, firstDayOfWeek)
    }

    val colors = rememberStripColors()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .applyAccessibility(section.accessibility),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Month/year label — styled like a button affordance but not tappable in PR 1
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleSmall,
            color = colors.monthLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Day-of-week header row
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.dayOfWeekLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Week pager
        HorizontalPager(
            state = pagerState
        ) { page ->
            val weekStart = remember(page) {
                weekStartDate(anchorDate, page, firstDayOfWeek)
            }
            val datesForWeek = remember(weekStart, minDate, maxDate) {
                generateWeekDates(weekStart, minDate, maxDate)
            }

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                datesForWeek.forEach { cellDate ->
                    if (cellDate != null) {
                        val isSelected = cellDate == selectedDate
                        val isDefault = cellDate.toString() == model.defaultDate
                        DateCell(
                            date = cellDate,
                            isSelected = isSelected,
                            isDefaultDate = isDefault,
                            colors = colors,
                            onDateSelected = {
                                onStateChange(model.stateKey, cellDate.toString())
                                onAction(model.onDateSelected)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isDefaultDate: Boolean,
    colors: StripColors,
    onDateSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presentation = when {
        isSelected && isDefaultDate -> CellStyle(
            textColor = colors.selectedDefaultText,
            indicatorColor = colors.defaultIndicator,
            textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        isSelected -> CellStyle(
            textColor = colors.primaryText,
            indicatorColor = colors.selectedIndicator,
            textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
        isDefaultDate -> CellStyle(
            textColor = colors.defaultText,
            indicatorColor = Color.Transparent,
            textStyle = MaterialTheme.typography.labelMedium
        )
        else -> CellStyle(
            textColor = colors.primaryText,
            indicatorColor = Color.Transparent,
            textStyle = MaterialTheme.typography.labelMedium
        )
    }

    val density = LocalDensity.current
    var indicatorSizeDp by remember { mutableFloatStateOf(32f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
    ) {
        AnimatedVisibility(
            modifier = Modifier.matchParentSize(),
            visible = isSelected,
            enter = scaleIn(),
            exit = scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(indicatorSizeDp.dp)
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .align(Alignment.Center)
                    .background(presentation.indicatorColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {}
        }

        Text(
            text = date.dayOfMonth.toString(),
            style = presentation.textStyle,
            color = animateColorAsState(
                targetValue = presentation.textColor,
                label = "dateColor"
            ).value,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(8.dp)
                .onSizeChanged { size ->
                    with(density) {
                        indicatorSizeDp = max(size.height.toDp().value, size.width.toDp().value)
                    }
                }
        )

        Box(
            Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    role = Role.Button,
                    onClick = onDateSelected
                )
                .fillMaxHeight()
        )
    }
}

// ── Color resolution ────────────────────────────────────────────────

@Composable
private fun rememberStripColors(): StripColors {
    val scheme = MaterialTheme.colorScheme
    val primaryText = stripColor(CELL_TEXT_PRIMARY, scheme.onSurface)
    val secondaryText = stripColor(CELL_TEXT_SECONDARY, scheme.onSurfaceVariant)
    val interactive = stripColor(CELL_TEXT_INTERACTIVE, scheme.primary)
    val selectionText = stripColor(CELL_TEXT_SELECTION, scheme.onPrimary)
    val disabledIndicator = stripColor(CELL_INDICATOR_DISABLED, scheme.surfaceVariant)
    val monthLabel = stripColor(MONTH_LABEL_PRIMARY, scheme.onSurface)

    return remember(
        primaryText, secondaryText, interactive, selectionText, disabledIndicator, monthLabel
    ) {
        StripColors(
            primaryText = primaryText,
            dayOfWeekLabel = secondaryText,
            monthLabel = monthLabel,
            defaultText = interactive,
            selectedDefaultText = selectionText,
            defaultIndicator = interactive,
            selectedIndicator = disabledIndicator
        )
    }
}

@Composable
private fun stripColor(token: String, fallback: Color): Color {
    val resolved = ColorTokenResolver.resolve(token)
    return if (resolved != Color.Unspecified) resolved else fallback
}

@Immutable
private data class StripColors(
    val primaryText: Color,
    val dayOfWeekLabel: Color,
    val monthLabel: Color,
    val defaultText: Color,
    val selectedDefaultText: Color,
    val defaultIndicator: Color,
    val selectedIndicator: Color
)

@Immutable
private data class CellStyle(
    val textColor: Color,
    val indicatorColor: Color,
    val textStyle: TextStyle
)

// ── Calendar math (LocalDate, no timezone) ──────────────────────────

internal fun parseIsoDate(iso: String): LocalDate? {
    return try {
        LocalDate.parse(iso)
    } catch (e: Exception) {
        Log.w(TAG, "Failed to parse ISO date: $iso", e)
        null
    }
}

internal fun effectiveMinDate(
    minDate: LocalDate?,
    selectedDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): LocalDate {
    val anchor = minDate ?: selectedDate.minusMonths(6)
    return anchor.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
}

internal fun effectiveMaxDate(maxDate: LocalDate?, selectedDate: LocalDate): LocalDate {
    return maxDate ?: selectedDate.plusMonths(6)
}

/**
 * Number of weeks between {@code anchorDate} and {@code targetDate}, inclusive
 * of both endpoints — i.e. the **count** of weeks. Same-week returns 1.
 *
 * Use this for total-page counts. For a 0-based page index, see [weekIndexOf].
 */
internal fun weeksBetween(
    anchorDate: LocalDate,
    targetDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): Int {
    return weekIndexOf(anchorDate, targetDate, firstDayOfWeek) + 1
}

/**
 * 0-based index of {@code targetDate}'s week measured from {@code anchorDate}'s
 * week. Same-week returns 0.
 */
internal fun weekIndexOf(
    anchorDate: LocalDate,
    targetDate: LocalDate,
    firstDayOfWeek: DayOfWeek
): Int {
    val anchorWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val targetWeekStart = targetDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return ChronoUnit.WEEKS.between(anchorWeekStart, targetWeekStart).toInt()
}

internal fun weekStartDate(
    anchorDate: LocalDate,
    weekIndex: Int,
    firstDayOfWeek: DayOfWeek
): LocalDate {
    val anchorWeekStart = anchorDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return anchorWeekStart.plusWeeks(weekIndex.toLong())
}

internal fun generateWeekDates(
    weekStart: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?
): List<LocalDate?> {
    return List(7) { i ->
        val date = weekStart.plusDays(i.toLong())
        if ((minDate != null && date.isBefore(minDate)) ||
            (maxDate != null && date.isAfter(maxDate))
        ) {
            null
        } else {
            date
        }
    }
}

// ── Locale helpers ──────────────────────────────────────────────────

private fun rotatedWeekdayLabels(locale: Locale, firstDayOfWeek: DayOfWeek): List<String> {
    return List(7) { i ->
        val day = firstDayOfWeek.plus(i.toLong())
        day.getDisplayName(JavaTextStyle.SHORT, locale)
            .take(3)
            .replaceFirstChar { it.uppercase(locale) }
    }
}

private fun formatMonthYearLabel(dateInWeek: LocalDate, locale: Locale): String {
    val mid = dateInWeek.plusDays(3)
    val month = mid.month.getDisplayName(JavaTextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase(locale) }
    return "$month ${mid.year}"
}
