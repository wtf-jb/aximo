package io.github.wtfjb.aximo.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.workout.WorkoutExerciseDetail
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.format.localizeDecimal
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/** Picked exercises from the picker, handed over by the navigation. */
data class PickedExercises(val ids: List<Long>, val superset: Boolean)

/** The running workout (A-02, mockup Workout.html). */
@Composable
fun WorkoutScreen(
    picked: PickedExercises?,
    onPickedConsumed: () -> Unit,
    onAddExercise: () -> Unit,
    onClose: () -> Unit,
    viewModel: WorkoutViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<SetEditing?>(null) }
    var noteFor by remember { mutableStateOf<WorkoutExerciseDetail?>(null) }
    var finishing by remember { mutableStateOf(false) }

    // Workout finished or discarded (or none running): leave the screen.
    LaunchedEffect(state.loading, state.workout) {
        if (!state.loading && state.workout == null) onClose()
    }
    LaunchedEffect(picked, state.workout?.workout?.id) {
        if (picked != null && state.workout != null) {
            viewModel.addExercises(picked.ids, picked.superset)
            onPickedConsumed()
        }
    }

    val workout = state.workout ?: return
    val total = workout.exercises.size
    val done = workout.exercises.count { it.sets.isNotEmpty() && it.sets.all { s -> s.completedAt != null } }

    val actions = WorkoutActions(
        onEditSet = { detail, set, field -> editing = SetEditing(detail, set, field) },
        onToggleDone = viewModel::toggleSetDone,
        onSetType = viewModel::setType,
        onDeleteSet = viewModel::deleteSet,
        onAddSet = viewModel::addSet,
        onNote = { noteFor = it },
        onRemove = { viewModel.removeExercise(it.entry) },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            CircleIconButton(AppIcons.Minimize, stringResource(R.string.workout_minimize), onClose)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.workout_progress, stringResource(R.string.workout_free), done, total),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = WorkoutLogic.formatElapsed(state.elapsedSeconds), style = MaterialTheme.typography.headlineSmall)
            }
            InverseButton(text = stringResource(R.string.workout_finish), onClick = { finishing = true })
        }

        if (workout.exercises.isEmpty()) {
            Column(
                modifier = Modifier.padding(Spacing.s20),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12),
            ) {
                Text(
                    text = stringResource(R.string.workout_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrimaryButton(
                    text = stringResource(R.string.workout_add_exercise),
                    onClick = onAddExercise,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.s12, end = Spacing.s12, top = Spacing.s6, bottom = Spacing.s24),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12),
            ) {
                items(state.groups, key = { group -> group.exercises.first().entry.id }) { group ->
                    WorkoutGroupCard(group = group, lastPerformance = state.lastPerformance, unit = state.unit, actions = actions)
                }
                item {
                    SecondaryButton(
                        text = stringResource(R.string.workout_add_exercise),
                        onClick = onAddExercise,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    editing?.let { edit ->
        val label = WorkoutLogic.setNumbers(edit.detail.sets)[edit.detail.sets.indexOf(edit.set).coerceAtLeast(0)]
            .let { if (it > 0) it.toString() else stringResource(R.string.workout_set_badge_warmup) }
        val (title, initial) = when (edit.field) {
            SetField.WEIGHT -> stringResource(R.string.workout_field_weight, label) to
                localizeDecimal(ExerciseDraft.formatNumber(state.unit.fromKg(edit.set.weightKg)))
            SetField.REPS -> stringResource(R.string.workout_field_reps, label) to edit.set.reps.toString()
            SetField.RIR -> stringResource(R.string.workout_field_rir, label) to (edit.set.rir?.toString() ?: "")
        }
        NumberEntryDialog(
            title = title,
            initial = initial,
            step = if (edit.field == SetField.WEIGHT) state.unit.fromKg(edit.detail.exercise.incrementKg).takeIf { it > 0 } ?: 1.0 else 1.0,
            decimal = edit.field == SetField.WEIGHT,
            allowEmpty = edit.field == SetField.RIR,
            onConfirm = { text ->
                viewModel.editSet(edit.set, edit.field, text)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    noteFor?.let { detail ->
        NoteDialog(
            title = stringResource(R.string.workout_exercise_note),
            initial = detail.entry.note,
            onConfirm = { note ->
                viewModel.setExerciseNote(detail.entry, note)
                noteFor = null
            },
            onDismiss = { noteFor = null },
        )
    }

    if (finishing) {
        FinishDialog(
            initialNote = workout.workout.note,
            onFinish = { note ->
                finishing = false
                viewModel.finish(note)
            },
            onDiscard = {
                finishing = false
                viewModel.discard()
            },
            onDismiss = { finishing = false },
        )
    }
}

/** A set value being edited in the number dialog. */
private data class SetEditing(val detail: WorkoutExerciseDetail, val set: SetEntry, val field: SetField)
