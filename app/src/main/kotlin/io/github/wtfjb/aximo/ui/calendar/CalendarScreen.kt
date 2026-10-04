package io.github.wtfjb.aximo.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import io.github.wtfjb.aximo.domain.calendar.CalendarMonth
import io.github.wtfjb.aximo.domain.calendar.TrainingCalendar
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.format.formatLongDate
import io.github.wtfjb.aximo.ui.format.formatMonthYear
import io.github.wtfjb.aximo.ui.format.formatShortWeekday
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.today.RecentCard
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.androidx.compose.koinViewModel

/**
 * Calendar of training days. Strength days are dark, cardio days tinted, today has a
 * ring. A tap on a day lists its workouts and cardio below; a tap on a row opens it.
 */
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenCardio: (Long) -> Unit,
    onOpenWorkout: (Long) -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack)
            Text(text = stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineSmall)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.s20, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s18),
        ) {
            MonthPager(state, onShowMonth = viewModel::showMonth, onSelect = viewModel::select)
            Legend()
            Text(text = formatLongDate(state.selected), style = MaterialTheme.typography.titleSmall)
            if (state.selectedItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.calendar_rest_day),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RecentCard(items = state.selectedItems, onOpenCardio = onOpenCardio, onOpenWorkout = onOpenWorkout)
            }
        }
    }
}

/**
 * One page per month, oldest left. Swipe sideways or use the arrows. The pager is
 * rebuilt when the list of months changes (data loaded), so it always starts on [CalendarUiState.month].
 */
@Composable
private fun MonthPager(state: CalendarUiState, onShowMonth: (CalendarMonth) -> Unit, onSelect: (LocalDate) -> Unit) {
    val months = state.months
    key(months.size) {
        val pagerState = rememberPagerState(initialPage = months.indexOf(state.month).coerceAtLeast(0)) { months.size }
        val scope = rememberCoroutineScope()
        val currentOnShowMonth by rememberUpdatedState(onShowMonth)
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { page -> currentOnShowMonth(months[page]) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s18)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMonthYear(months[pagerState.currentPage]),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    if (pagerState.currentPage > 0) {
                        CircleIconButton(AppIcons.ChevronLeft, stringResource(R.string.calendar_previous), {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        })
                    }
                    if (pagerState.currentPage < months.lastIndex) {
                        CircleIconButton(AppIcons.ChevronRight, stringResource(R.string.calendar_next), {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        })
                    }
                }
            }
            HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top) { page ->
                MonthGrid(months[page], state, onSelect)
            }
        }
    }
}

/** Weekday header and the weeks of [month], always six rows so the pages have the same height. */
@Composable
private fun MonthGrid(month: CalendarMonth, state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
    val weeks = TrainingCalendar.weeks(month)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        Row {
            // Any week gives the localized weekday labels, Monday first.
            val monday = StatsCalendar.weekStart(state.today)
            for (offset in 0 until 7) {
                Text(
                    text = formatShortWeekday(monday.plus(offset, DateTimeUnit.DAY)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (row in 0 until MAX_WEEKS) {
            Row {
                val week = weeks.getOrNull(row) ?: List<LocalDate?>(7) { null }
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f).height(Sizes.touch), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                kind = state.days[date] ?: DayKind.NONE,
                                isToday = date == state.today,
                                isSelected = date == state.selected,
                                isFuture = date > state.today,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_WEEKS = 6

@Composable
private fun DayCell(
    date: LocalDate,
    kind: DayKind,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val (background, content) = when (kind) {
        DayKind.STRENGTH -> colors.inverseSurface to colors.inverseOnSurface
        DayKind.CARDIO -> colors.primaryContainer to colors.onPrimaryContainer
        else -> Color.Transparent to if (isFuture) colors.onSurfaceVariant else colors.onSurface
    }
    val ring = when {
        isSelected -> Modifier.border(Sizes.todayRing, colors.onSurface, Radii.full)
        isToday -> Modifier.border(Sizes.todayRing, colors.primary, Radii.full)
        else -> Modifier
    }
    val kindLabel = when (kind) {
        DayKind.STRENGTH -> stringResource(R.string.stats_strength)
        DayKind.CARDIO -> stringResource(R.string.stats_cardio)
        else -> null
    }
    val description = listOfNotNull(formatLongDate(date), kindLabel).joinToString(", ")
    Box(
        modifier = Modifier
            .size(Sizes.touch)
            .clip(Radii.full)
            .background(background)
            .then(ring)
            .semantics {
                contentDescription = description
                selected = isSelected
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = date.day.toString(), style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun Legend() {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s16), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(colors.inverseSurface, stringResource(R.string.stats_strength))
        LegendItem(colors.primaryContainer, stringResource(R.string.stats_cardio))
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        Box(Modifier.size(Sizes.legendDot).clip(Radii.full).background(color))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
