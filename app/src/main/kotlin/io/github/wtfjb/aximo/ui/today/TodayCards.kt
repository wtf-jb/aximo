package io.github.wtfjb.aximo.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.today.PreviewItem
import io.github.wtfjb.aximo.domain.today.WeekDay
import io.github.wtfjb.aximo.domain.today.WeekOverview
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.LocalWeightUnit
import io.github.wtfjb.aximo.ui.format.formatLongDate
import io.github.wtfjb.aximo.ui.format.formatShortWeekday
import io.github.wtfjb.aximo.ui.format.formatWeight
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors

/**
 * Hero card "Nächstes Workout" (mockup Heute.html): routine, exercise count and
 * estimated length, the progression changes waiting in it, and the start button.
 */
@Composable
fun NextWorkoutCard(next: NextWorkoutUi, onStart: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s20), verticalArrangement = Arrangement.spacedBy(Spacing.s16)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(
                    text = stringResource(R.string.today_next_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.onInverseMuted,
                )
                Text(text = next.routine.name, style = MaterialTheme.typography.displaySmall)
                Text(
                    text = nextMeta(next),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.extendedColors.onInverseMuted,
                )
            }
            if (next.preview.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s10)) {
                    next.preview.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.extendedColors.inverseRaised)
                        PreviewRow(item)
                    }
                }
            }
            PrimaryButton(
                text = stringResource(R.string.today_next_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "6 Übungen · ca. 55 min"; without targets only the exercise count. */
@Composable
private fun nextMeta(next: NextWorkoutUi): String {
    val exercises = pluralStringResource(R.plurals.today_next_exercises, next.exerciseCount, next.exerciseCount)
    if (next.estimatedMinutes == 0) return exercises
    return stringResource(R.string.workout_meta, exercises, stringResource(R.string.today_next_minutes, next.estimatedMinutes))
}

/** "Bankdrücken    82,5 kg  [+2,5]" or "Klimmzug    [9 Wdh.]". */
@Composable
private fun PreviewRow(item: PreviewItem) {
    val unit = LocalWeightUnit.current
    val unitLabel = stringResource(unit.label())
    val repsOnly = item.reason == ProgressionReason.INCREASE_REPS && item.nextWeightKg == 0.0
    val delta = item.deltaKg
    val chip = when (item.reason) {
        ProgressionReason.INCREASE_REPS -> pluralStringResource(R.plurals.today_next_reps, item.nextRepTarget, item.nextRepTarget)
        else -> delta?.let { (if (it >= 0) "+" else "−") + formatWeight(kotlin.math.abs(it), unit) }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Text(text = item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
        if (!repsOnly) {
            Text(text = "${formatWeight(item.nextWeightKg, unit)} $unitLabel", style = MaterialTheme.typography.labelLarge)
        }
        if (chip != null) {
            Text(
                text = chip,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.extendedColors.onInverseAccentContainer,
                modifier = Modifier
                    .clip(Radii.full)
                    .background(MaterialTheme.extendedColors.inverseAccentContainer)
                    .padding(horizontal = Spacing.s8, vertical = Spacing.s4),
            )
        }
    }
}

/** "Diese Woche": Monday to Sunday with strength and cardio days and the session count. */
@Composable
fun WeekCard(week: WeekOverview) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s18), verticalArrangement = Arrangement.spacedBy(Spacing.s14)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.today_week_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    text = pluralStringResource(R.plurals.today_week_sessions, week.sessions, week.sessions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                week.days.forEach { day -> DayCell(day, Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(day: WeekDay, modifier: Modifier = Modifier) {
    val weekday = formatShortWeekday(day.date)
    val description = when (day.kind) {
        DayKind.STRENGTH -> stringResource(R.string.today_week_day_strength, formatLongDate(day.date))
        DayKind.CARDIO -> stringResource(R.string.today_week_day_cardio, formatLongDate(day.date))
        else -> formatLongDate(day.date)
    }
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.s6),
    ) {
        Text(
            text = weekday,
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val circle = Modifier
            .size(Sizes.weekDay)
            .clip(Radii.full)
        val ring = if (day.isToday) Modifier.border(Sizes.todayRing, MaterialTheme.colorScheme.primary, Radii.full) else Modifier
        when (day.kind) {
            DayKind.STRENGTH -> DayIcon(circle.background(MaterialTheme.colorScheme.inverseSurface).then(ring)) {
                Icon(AppIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.size(Sizes.iconSmall))
            }
            DayKind.CARDIO -> DayIcon(circle.background(MaterialTheme.colorScheme.primaryContainer).then(ring)) {
                Icon(AppIcons.Cardio, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(Sizes.iconSmall))
            }
            else -> if (day.isToday) {
                DayIcon(circle.then(ring)) {
                    Text(text = day.date.day.toString(), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Box(circle.background(MaterialTheme.extendedColors.track))
            }
        }
    }
}

@Composable
private fun DayIcon(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) { content() }
}
