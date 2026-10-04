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
            MonthHeader(
                title = formatMonthYear(state.month),
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
            )
            MonthGrid(state, onSelect = viewModel::select)
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

@Composable
private fun MonthHeader(
    title: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            if (canGoBack) CircleIconButton(AppIcons.ChevronLeft, stringResource(R.string.calendar_previous), onPrevious)
            if (canGoForward) CircleIconButton(AppIcons.ChevronRight, stringResource(R.string.calendar_next), onNext)
        }
    }
}

@Composable
private fun MonthGrid(state: CalendarUiState, onSelect: (LocalDate) -> Unit) {
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
        state.weeks.forEach { week ->
            Row {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
