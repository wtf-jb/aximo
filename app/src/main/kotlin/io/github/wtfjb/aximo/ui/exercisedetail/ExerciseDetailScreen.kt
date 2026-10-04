package io.github.wtfjb.aximo.ui.exercisedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.stats.ExerciseSession
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import io.github.wtfjb.aximo.domain.stats.Records
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.PillTextButton
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatInteger
import io.github.wtfjb.aximo.ui.format.formatRest
import io.github.wtfjb.aximo.ui.format.formatShortDate
import io.github.wtfjb.aximo.ui.format.formatWeight
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.stats.TrendChart
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Exercise detail (A-07, mockup UebungDetail.html). */
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = koinViewModel { parametersOf(exerciseId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exercise = state.exercise
    val unit = WeightUnit.KG
    val unitLabel = stringResource(unit.label())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s24),
        verticalArrangement = Arrangement.spacedBy(Spacing.s14),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack)
                Row(modifier = Modifier.weight(1f)) {}
                if (exercise != null) PillTextButton(text = stringResource(R.string.detail_edit), onClick = { onEdit(exercise.id) })
            }
        }
        if (exercise == null) return@LazyColumn

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4), modifier = Modifier.padding(horizontal = Spacing.s4)) {
                Text(text = exercise.name, style = MaterialTheme.typography.displaySmall)
                Text(
                    text = metaLine(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            val bests = state.bests
            val isReps = state.metric == ProgressMetric.REPS
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                if (isReps) {
                    Tile(bests?.bestSet?.reps?.toString() ?: stringResource(R.string.detail_none), stringResource(R.string.detail_best_reps), Modifier.weight(1f))
                } else {
                    Tile(bests?.bestE1rm?.let { formatWeight(it, unit) } ?: stringResource(R.string.detail_none), stringResource(R.string.detail_e1rm, unitLabel), Modifier.weight(1f))
                }
                Tile(bests?.maxWeightKg?.let { formatWeight(it, unit) } ?: stringResource(R.string.detail_none), stringResource(R.string.detail_max, unitLabel), Modifier.weight(1f))
                Tile((bests?.sessions ?: 0).toString(), stringResource(R.string.detail_sessions), Modifier.weight(1f))
            }
        }
        state.suggestion?.let { suggestion ->
            item {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Column(modifier = Modifier.fillMaxWidth().padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                        Text(text = stringResource(R.string.detail_next_time), style = MaterialTheme.typography.labelMedium)
                        Text(
                            text = stringResource(
                                R.string.detail_next_sets,
                                state.lastWorkingSets.coerceAtLeast(1),
                                "${formatWeight(suggestion.nextWeightKg, unit)} $unitLabel",
                                suggestion.nextRepTarget,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(
                                when (suggestion.reason) {
                                    ProgressionReason.INCREASE_WEIGHT -> R.string.detail_next_reason_increase
                                    ProgressionReason.INCREASE_REPS -> R.string.detail_next_reason_reps
                                    ProgressionReason.HOLD -> R.string.detail_next_reason_hold
                                    ProgressionReason.DECREASE_WEIGHT -> R.string.detail_next_reason_decrease
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item {
            val tabs = DetailTab.entries
            SegmentedControl(
                options = tabs.map { stringResource(it.label()) },
                selectedIndex = tabs.indexOf(state.tab),
                onSelect = { viewModel.onTabSelect(tabs[it]) },
            )
        }
        when (state.tab) {
            DetailTab.HISTORY -> if (state.sessions.isEmpty()) {
                item { Muted(stringResource(R.string.detail_no_history)) }
            } else {
                items(state.sessions, key = { it.workoutId }) { session ->
                    SessionCard(
                        session = session,
                        routineName = session.routineId?.let { state.routineNames[it] },
                        isRecord = session.workoutId in state.recordWorkouts,
                        unit = unit,
                    )
                }
            }
            DetailTab.CHART -> item {
                Card {
                    if (state.points.size < 2) {
                        Muted(stringResource(R.string.stats_too_few))
                    } else {
                        val isReps = state.metric == ProgressMetric.REPS
                        val first = formatShortDate(state.points.first().startedAt)
                        val last = formatShortDate(state.points.last().startedAt)
                        TrendChart(
                            values = state.points.map { it.value },
                            highlighted = state.points.indices.filter { state.points[it].workoutId in state.recordWorkouts }.toSet(),
                            formatValue = { if (isReps) it.toInt().toString() else formatWeight(it, unit) },
                            contentDescription = stringResource(
                                if (isReps) R.string.stats_chart_reps else R.string.stats_chart_e1rm,
                                exercise.name,
                                first,
                                last,
                            ),
                        )
                    }
                }
            }
            DetailTab.RECORDS -> item { RecordsCard(state, unit, unitLabel) }
            DetailTab.INFO -> item { InfoCard(exercise, unit, unitLabel) }
        }
    }
}

private fun DetailTab.label(): Int = when (this) {
    DetailTab.HISTORY -> R.string.detail_tab_history
    DetailTab.CHART -> R.string.detail_tab_chart
    DetailTab.RECORDS -> R.string.detail_tab_records
    DetailTab.INFO -> R.string.detail_tab_info
}

/** "Langhantel · Brust, Trizeps" */
@Composable
private fun metaLine(exercise: Exercise): String {
    val muscles = (exercise.primaryMuscles.sortedBy { it.ordinal } + exercise.secondaryMuscles.sortedBy { it.ordinal })
        .map { stringResource(it.label()) }
    val equipment = stringResource(exercise.equipment.label())
    return if (muscles.isEmpty()) equipment else stringResource(R.string.workout_meta, equipment, muscles.joinToString(", "))
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.s12), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s10)) { content() }
    }
}

@Composable
private fun Muted(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(session: ExerciseSession, routineName: String?, isRecord: Boolean, unit: WeightUnit) {
    val unitLabel = stringResource(unit.label())
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(text = formatShortDate(session.startedAt), style = MaterialTheme.typography.bodyMedium)
                val volume = stringResource(R.string.detail_session_meta_volume, formatInteger(unit.fromKg(Records.volume(session.sets))), unitLabel)
                Text(
                    text = routineName?.let { stringResource(R.string.workout_meta, it, volume) } ?: volume,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isRecord) {
                Surface(shape = Radii.full, color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
                    Text(
                        text = stringResource(R.string.detail_pr),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = Spacing.s8, vertical = Spacing.s4),
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s6), verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            session.sets.forEach { SetChip(it, unit) }
        }
    }
}

/** "82,5 × 8 · R2"; warm-ups tinted "W 40 × 10" (design system: set chip). */
@Composable
private fun SetChip(set: SetEntry, unit: WeightUnit) {
    val weight = formatWeight(set.weightKg, unit)
    val warm = set.setType == SetType.WARM_UP
    val rir = set.rir
    val text = when {
        warm -> stringResource(R.string.detail_set_chip_warmup, weight, set.reps)
        rir != null -> stringResource(R.string.detail_set_chip_rir, weight, set.reps, rir)
        else -> stringResource(R.string.detail_set_chip, weight, set.reps)
    }
    Surface(
        shape = Radii.sm,
        color = if (warm) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (warm) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = MaterialTheme.typography.labelLarge.fontWeight),
            modifier = Modifier.padding(horizontal = Spacing.s10, vertical = Spacing.s8),
        )
    }
}

@Composable
private fun RecordsCard(state: ExerciseDetailUiState, unit: WeightUnit, unitLabel: String) {
    val bests = state.bests
    Card {
        if (bests == null) {
            Muted(stringResource(R.string.detail_no_history))
            return@Card
        }
        val none = stringResource(R.string.detail_none)
        bests.bestE1rm?.let { InfoRow(stringResource(R.string.detail_best_e1rm), "${formatWeight(it, unit)} $unitLabel") }
        InfoRow(
            stringResource(if (state.exercise?.type == ExerciseType.BODYWEIGHT) R.string.stats_max_extra else R.string.stats_max_weight),
            bests.maxWeightKg?.let { "${formatWeight(it, unit)} $unitLabel" } ?: none,
        )
        bests.bestSet?.let { InfoRow(stringResource(R.string.stats_best_set), stringResource(R.string.detail_set_chip, formatWeight(it.weightKg, unit), it.reps)) }
        InfoRow(stringResource(R.string.stats_volume, unitLabel), formatInteger(unit.fromKg(bests.volumeKg)))
        InfoRow(stringResource(R.string.stats_total_reps), bests.totalReps.toString())
        InfoRow(stringResource(R.string.detail_sessions), bests.sessions.toString())
        if (state.repRecords.isNotEmpty()) {
            Text(text = stringResource(R.string.detail_rep_records), style = MaterialTheme.typography.titleSmall)
            state.repRecords.forEach { record ->
                InfoRow(
                    "${formatWeight(record.weightKg, unit)} $unitLabel",
                    stringResource(R.string.detail_rep_record, record.reps, "${formatWeight(record.weightKg, unit)} $unitLabel"),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(exercise: Exercise, unit: WeightUnit, unitLabel: String) {
    val none = stringResource(R.string.detail_none)
    Card {
        InfoRow(stringResource(R.string.detail_info_type), stringResource(exercise.type.label()))
        InfoRow(stringResource(R.string.detail_info_equipment), stringResource(exercise.equipment.label()))
        InfoRow(
            stringResource(R.string.detail_info_primary),
            exercise.primaryMuscles.sortedBy { it.ordinal }.map { stringResource(it.label()) }.joinToString(", ").ifEmpty { none },
        )
        InfoRow(
            stringResource(R.string.detail_info_secondary),
            exercise.secondaryMuscles.sortedBy { it.ordinal }.map { stringResource(it.label()) }.joinToString(", ").ifEmpty { none },
        )
        if (exercise.type != ExerciseType.CARDIO) {
            InfoRow(stringResource(R.string.detail_info_reps), stringResource(R.string.workout_meta_reps, exercise.repRangeMin, exercise.repRangeMax))
            InfoRow(stringResource(R.string.detail_info_increment), "${formatWeight(exercise.incrementKg, unit)} $unitLabel")
            InfoRow(stringResource(R.string.detail_info_rest), formatRest(exercise.restSeconds))
        }
        if (exercise.note.isNotBlank()) InfoRow(stringResource(R.string.detail_info_note), exercise.note)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.touch),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
