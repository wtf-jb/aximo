package io.github.wtfjb.aximo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.ProgressionReason
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import io.github.wtfjb.aximo.ui.components.HintChip
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.exercises.icon
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatWeight
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing

/** Everything a card can ask the screen to do. */
class WorkoutActions(
    val onEditSet: (WorkoutExerciseDetail, SetEntry, SetField) -> Unit,
    val onToggleDone: (SetEntry) -> Unit,
    val onSetType: (SetEntry, SetType) -> Unit,
    val onDeleteSet: (SetEntry) -> Unit,
    val onAddSet: (WorkoutExerciseDetail) -> Unit,
    val onNote: (WorkoutExerciseDetail) -> Unit,
    val onRemove: (WorkoutExerciseDetail) -> Unit,
)

/** One card: a single exercise, or a superset with its exercises one below the other. */
@Composable
fun WorkoutGroupCard(
    group: WorkoutGroupUi,
    lastPerformance: Map<Long, List<SetEntry>>,
    targets: Map<Long, RoutineExercise>,
    progression: Map<Long, ProgressionState>,
    unit: WeightUnit,
    actions: WorkoutActions,
) {
    Surface(shape = Radii.xxl, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(start = Spacing.s14, end = Spacing.s14, top = Spacing.s16, bottom = Spacing.s14),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            if (group.supersetGroup != null) {
                SupersetChip(group.supersetGroup)
            }
            group.exercises.forEachIndexed { index, detail ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ExerciseBlock(
                    detail = detail,
                    tileLabel = group.supersetGroup?.let { "$it${index + 1}" },
                    activeSetId = group.activeSetId,
                    last = lastPerformance[detail.entry.exerciseId].orEmpty(),
                    target = targets[detail.entry.exerciseId],
                    progression = progression[detail.entry.exerciseId],
                    unit = unit,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun SupersetChip(letter: String) {
    Surface(
        shape = Radii.full,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
    ) {
        Text(
            text = stringResource(R.string.workout_superset, letter),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = Spacing.s10, vertical = Spacing.s4),
        )
    }
}

@Composable
private fun ExerciseBlock(
    detail: WorkoutExerciseDetail,
    tileLabel: String?,
    activeSetId: Long?,
    last: List<SetEntry>,
    target: RoutineExercise?,
    progression: ProgressionState?,
    unit: WeightUnit,
    actions: WorkoutActions,
) {
    val exercise = detail.exercise
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s10)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Box(
                modifier = Modifier
                    .size(Sizes.touch)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (tileLabel != null) {
                    Text(text = tileLabel, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(
                        imageVector = exercise.type.icon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Sizes.icon),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(text = exercise.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(
                        R.string.workout_meta,
                        stringResource(exercise.equipment.label()),
                        if (target != null) {
                            targetText(target)
                        } else {
                            stringResource(R.string.workout_meta_reps, exercise.repRangeMin, exercise.repRangeMax)
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ExerciseMenu(detail = detail, actions = actions)
        }

        progressionHint(progression, last, unit)?.let { HintChip(text = it) }
        if (last.isNotEmpty()) {
            Text(
                text = stringResource(R.string.workout_last, lastSummary(last, unit)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (detail.entry.note.isNotBlank()) {
            Text(text = detail.entry.note, style = MaterialTheme.typography.bodySmall)
        }

        val weightLabel = stringResource(
            when {
                exercise.type == ExerciseType.BODYWEIGHT && unit == WeightUnit.KG -> R.string.workout_col_added_kg
                exercise.type == ExerciseType.BODYWEIGHT -> R.string.workout_col_added_lbs
                unit == WeightUnit.KG -> R.string.workout_col_kg
                else -> R.string.workout_col_lbs
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            SetHeader(weightLabel)
            val numbers = WorkoutLogic.setNumbers(detail.sets)
            detail.sets.forEachIndexed { index, set ->
                SetRow(
                    set = set,
                    number = numbers[index],
                    state = when {
                        set.completedAt != null -> SetRowState.DONE
                        set.id == activeSetId -> SetRowState.ACTIVE
                        else -> SetRowState.OPEN
                    },
                    weightText = formatWeight(set.weightKg, unit),
                    onEdit = { field -> actions.onEditSet(detail, set, field) },
                    onToggleDone = { actions.onToggleDone(set) },
                    onSetType = { type -> actions.onSetType(set, type) },
                    onDelete = { actions.onDeleteSet(set) },
                )
            }
        }

        SecondaryButton(
            text = stringResource(R.string.workout_add_set),
            onClick = { actions.onAddSet(detail) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExerciseMenu(detail: WorkoutExerciseDetail, actions: WorkoutActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(Sizes.touch)) {
            Icon(
                imageVector = AppIcons.More,
                contentDescription = stringResource(R.string.workout_exercise_options, detail.exercise.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.icon),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_exercise_note), style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    open = false
                    actions.onNote(detail)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_exercise_remove), style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    open = false
                    actions.onRemove(detail)
                },
            )
        }
    }
}

/**
 * Hint chip text for the suggestion of the progression rules (A-06):
 * "Progression: 82,5 kg (+2,5)" or "Progression: 9 Wdh."; nothing when the weight just stays.
 */
@Composable
private fun progressionHint(progression: ProgressionState?, last: List<SetEntry>, unit: WeightUnit): String? {
    progression ?: return null
    val unitLabel = stringResource(unit.label())
    return when (progression.reason) {
        ProgressionReason.INCREASE_WEIGHT, ProgressionReason.DECREASE_WEIGHT -> {
            val weight = formatWeight(progression.nextWeightKg, unit)
            val lastTop = last.maxOfOrNull { it.weightKg }
            if (lastTop == null) {
                stringResource(R.string.workout_progression_weight, weight, unitLabel)
            } else {
                val delta = progression.nextWeightKg - lastTop
                val sign = if (delta >= 0) "+" else "−"
                stringResource(R.string.workout_progression_weight_delta, weight, unitLabel, sign + formatWeight(kotlin.math.abs(delta), unit))
            }
        }
        ProgressionReason.INCREASE_REPS -> pluralStringResource(R.plurals.workout_progression_reps, progression.nextRepTarget, progression.nextRepTarget)
        ProgressionReason.HOLD -> null
    }
}

/** "Ziel 3 × 6–8 · RIR 2" */
@Composable
private fun targetText(target: RoutineExercise): String {
    val base = stringResource(R.string.workout_target, stringResource(R.string.routine_target, target.targetSets, target.repMin, target.repMax))
    return target.targetRir?.let { stringResource(R.string.workout_meta, base, stringResource(R.string.routine_target_rir, it)) } ?: base
}

/** "80 × 8 · 8 · 8" when the weight stays the same, else "80 × 8 · 82,5 × 6". */
private fun lastSummary(sets: List<SetEntry>, unit: WeightUnit): String {
    val sameWeight = sets.all { it.weightKg == sets.first().weightKg }
    return if (sameWeight) {
        formatWeight(sets.first().weightKg, unit) + " × " + sets.joinToString(" · ") { it.reps.toString() }
    } else {
        sets.joinToString(" · ") { formatWeight(it.weightKg, unit) + " × " + it.reps }
    }
}
