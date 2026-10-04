package io.github.wtfjb.aximo.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.cardio.CardioActivities
import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.recent.RecentItem
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.ui.cardio.formatKm
import io.github.wtfjb.aximo.ui.cardio.formatSpeed
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatInteger
import io.github.wtfjb.aximo.ui.format.formatShortDate
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing

/**
 * "Zuletzt" on the Today tab (mockup Heute.html): workouts with duration and
 * volume, cardio with distance, duration and pace. Cardio rows open the entry.
 */
@Composable
fun RecentList(items: List<RecentItem>, onOpenCardio: (Long) -> Unit, onOpenWorkout: (Long) -> Unit) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s10)) {
        Text(text = stringResource(R.string.today_recent), style = MaterialTheme.typography.titleSmall)
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s6)) {
                items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    when (item) {
                        is RecentItem.WorkoutItem -> WorkoutRow(item, onClick = { onOpenWorkout(item.workoutId) })
                        is RecentItem.CardioItem -> CardioRow(item, onClick = { onOpenCardio(item.entry.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutRow(item: RecentItem.WorkoutItem, onClick: () -> Unit) {
    val minutes = (item.durationSec / 60).toInt()
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        RecentRow(
            title = item.routineName ?: stringResource(R.string.today_recent_free),
            meta = stringResource(
                R.string.workout_meta,
                formatShortDate(item.startedAt),
                pluralStringResource(R.plurals.today_recent_minutes, minutes, minutes),
            ),
            value = "${formatInteger(item.volumeKg)} ${stringResource(WeightUnit.KG.label())}",
        )
    }
}

@Composable
private fun CardioRow(item: RecentItem.CardioItem, onClick: () -> Unit) {
    val entry = item.entry
    val distance = entry.distanceM
    val value = when (CardioActivities.paceStyle(item.activity)) {
        PaceStyle.PER_KM -> CardioMath.paceSecondsPerKm(entry.durationSec, distance)
            ?.let { stringResource(R.string.cardio_pace_per_km, CardioMath.formatPace(it)) }
        PaceStyle.PER_500M -> CardioMath.paceSecondsPer500m(entry.durationSec, distance)
            ?.let { stringResource(R.string.cardio_pace_per_500m, CardioMath.formatPace(it)) }
        PaceStyle.SPEED -> CardioMath.speedKmh(entry.durationSec, distance)
            ?.let { stringResource(R.string.cardio_speed_kmh, formatSpeed(it)) }
    }
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        RecentRow(
            title = distance?.let { stringResource(R.string.cardio_recent_title, item.activity.name, formatKm(it, decimals = 1)) }
                ?: item.activity.name,
            meta = stringResource(
                R.string.workout_meta,
                formatShortDate(entry.startedAt),
                CardioMath.formatDuration(entry.durationSec.toLong()),
            ),
            value = value,
        )
    }
}

@Composable
private fun RecentRow(title: String, meta: String, value: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.cta)
            .padding(vertical = Spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(text = meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (value != null) {
            Text(text = value, style = MaterialTheme.typography.labelLarge)
        }
    }
}
