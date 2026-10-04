package io.github.wtfjb.aximo.ui.summary

import io.github.wtfjb.aximo.ui.format.LocalWeightUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.stats.RecordType
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatShortDate
import io.github.wtfjb.aximo.ui.format.formatTime
import io.github.wtfjb.aximo.ui.format.formatWeight
import io.github.wtfjb.aximo.ui.format.formatInteger
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Summary after finishing a workout (mockup Abschluss.html). */
@Composable
fun SummaryScreen(
    workoutId: Long,
    onDone: () -> Unit,
    viewModel: SummaryViewModel = koinViewModel { parametersOf(workoutId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unit = LocalWeightUnit.current

    LaunchedEffect(state.done, state.loading, state.workout) {
        if (state.done || (!state.loading && state.workout == null)) onDone()
    }
    val workout = state.workout ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = Spacing.s16, end = Spacing.s16, top = Spacing.s16, bottom = Spacing.s24),
        verticalArrangement = Arrangement.spacedBy(Spacing.s18),
    ) {
        item { HeroCard(state = state, unit = unit) }

        if (state.records.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.summary_new_records)) }
            item {
                ListCard {
                    state.records.forEach { item ->
                        val name = item.exercise.name
                        val record = item.record
                        val title = when (record.type) {
                            RecordType.E1RM -> stringResource(R.string.summary_record_e1rm, name)
                            RecordType.WEIGHT -> stringResource(R.string.summary_record_weight, name)
                            RecordType.REPS_AT_WEIGHT -> stringResource(R.string.summary_record_reps, name)
                            RecordType.VOLUME -> stringResource(R.string.summary_record_volume, name)
                        }
                        val unitLabel = stringResource(unit.label())
                        val (now, before) = if (record.type == RecordType.REPS_AT_WEIGHT) {
                            val weight = formatWeight(record.weightKg, unit) + " " + unitLabel
                            stringResource(R.string.summary_reps_at_weight, record.value.toInt().toString(), weight) to
                                stringResource(R.string.summary_reps_at_weight, record.previous.toInt().toString(), weight)
                        } else {
                            "${formatWeight(record.value, unit)} $unitLabel" to "${formatWeight(record.previous, unit)} $unitLabel"
                        }
                        ChangeRow(title = title, before = before, after = now)
                    }
                }
            }
        }

        if (state.nextTime.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.summary_next_time)) }
            item {
                ListCard {
                    state.nextTime.forEach { item ->
                        val unitLabel = stringResource(unit.label())
                        val current = "${formatWeight(item.currentWeightKg, unit)} $unitLabel"
                        val suggestion = item.suggestion
                        when (suggestion.reason) {
                            ProgressionReason.INCREASE_WEIGHT, ProgressionReason.DECREASE_WEIGHT -> ChangeRow(
                                title = item.exercise.name,
                                before = current,
                                after = "${formatWeight(suggestion.nextWeightKg, unit)} $unitLabel",
                            )
                            ProgressionReason.INCREASE_REPS -> ChangeRow(
                                title = item.exercise.name,
                                before = current,
                                after = pluralStringResource(R.plurals.summary_reps_target, suggestion.nextRepTarget, suggestion.nextRepTarget),
                                keepBefore = true,
                            )
                            ProgressionReason.HOLD -> TextRow(item.exercise.name, stringResource(R.string.summary_stays, current))
                        }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.summary_how)) }
        item { MoodRow(selected = state.rating, onSelect = viewModel::onRatingChange) }
        item {
            LabeledTextField(
                label = stringResource(R.string.workout_finish_note),
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                containerColor = MaterialTheme.colorScheme.surface,
                singleLine = false,
            )
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.summary_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HeroCard(state: SummaryUiState, unit: WeightUnit) {
    val workout = state.workout ?: return
    val extended = MaterialTheme.extendedColors
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s18), verticalArrangement = Arrangement.spacedBy(Spacing.s14)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(
                    text = state.routineName?.let { stringResource(R.string.summary_done, it) } ?: stringResource(R.string.summary_done_free),
                    style = MaterialTheme.typography.displaySmall,
                )
                val start = workout.workout.startedAt
                val end = workout.workout.endedAt ?: start
                Text(
                    text = stringResource(R.string.summary_when, formatShortDate(start), formatTime(start), formatTime(end)),
                    style = MaterialTheme.typography.bodySmall,
                    color = extended.onInverseMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                StatTile(WorkoutLogic.formatElapsed(state.durationSeconds), stringResource(R.string.summary_duration), Modifier.weight(1f))
                StatTile(
                    formatInteger(unit.fromKg(state.volumeKg)),
                    volumeLabel(state, stringResource(R.string.summary_volume, stringResource(unit.label()))),
                    Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                StatTile(state.workingSets.toString(), stringResource(R.string.summary_working_sets), Modifier.weight(1f))
                StatTile(state.records.size.toString(), stringResource(R.string.summary_records), Modifier.weight(1f))
            }
            StatTile(
                value = state.kcal?.let { stringResource(R.string.calories_value, formatInteger(it.toDouble())) } ?: stringResource(R.string.cardio_empty_value),
                label = stringResource(if (state.kcal != null) R.string.calories_estimated else R.string.calories_no_body_weight),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** "Volumen (kg)" plus "· +4 % ggü. letzter Push A" when there is a previous workout of the routine. */
@Composable
private fun volumeLabel(state: SummaryUiState, base: String): String {
    val change = state.volumeChange ?: return base
    val name = state.routineName ?: return base
    val percent = String.format(java.util.Locale.ROOT, "%+d", Math.round(change * 100))
    return stringResource(R.string.workout_meta, base, stringResource(R.string.summary_volume_change, percent, name))
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.extendedColors.inverseRaised, modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.s12), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.extendedColors.onInverseMuted)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun ListCard(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            content()
        }
    }
}

/** "Bankdrücken · e1RM   101,3 kg → 104,5 kg": old value struck through in ink-muted, new one bold. */
@Composable
private fun ChangeRow(title: String, before: String, after: String, keepBefore: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = before,
            style = MaterialTheme.typography.bodySmall.copy(textDecoration = if (keepBefore) null else TextDecoration.LineThrough),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = AppIcons.ArrowRight,
            contentDescription = stringResource(R.string.summary_arrow_description),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconSmall),
        )
        Text(text = after, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TextRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** "Wie lief's?": five tiles, the chosen one inverse. */
@Composable
private fun MoodRow(selected: Int?, onSelect: (Int) -> Unit) {
    val labels = listOf(
        R.string.summary_mood_1,
        R.string.summary_mood_2,
        R.string.summary_mood_3,
        R.string.summary_mood_4,
        R.string.summary_mood_5,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        labels.forEachIndexed { index, label ->
            val value = index + 1
            val isSelected = selected == value
            Surface(
                onClick = { onSelect(value) },
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
                contentColor = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Sizes.moodTile)
                    .semantics {
                        this.selected = isSelected
                        role = Role.RadioButton
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
                        Text(text = stringResource(label), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    }
}
