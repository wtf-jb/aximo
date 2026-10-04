package io.github.wtfjb.aximo.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.exercise.ExerciseFieldError
import io.github.wtfjb.aximo.domain.model.Equipment
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.MuscleGroup
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Form to create or edit an exercise (A-01). */
@Composable
fun ExerciseEditScreen(
    exerciseId: Long,
    onDone: () -> Unit,
    viewModel: ExerciseEditViewModel = koinViewModel { parametersOf(exerciseId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircleIconButton(AppIcons.Close, stringResource(R.string.action_back), onDone)
            Text(
                text = stringResource(if (state.isNew) R.string.exercise_edit_title_new else R.string.exercise_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (!state.isNew && !state.loading) {
                TextButton(onClick = viewModel::toggleArchived) {
                    Text(
                        text = stringResource(if (state.draft.archived) R.string.exercise_unarchive else R.string.exercise_archive),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (!state.loading) {
            ExerciseForm(
                draft = state.draft,
                errors = state.errors,
                onChange = viewModel::onDraftChange,
                onEquipmentChange = viewModel::onEquipmentChange,
            )
            PrimaryButton(
                text = stringResource(R.string.exercise_save),
                onClick = viewModel::save,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseForm(
    draft: ExerciseDraft,
    errors: Set<ExerciseFieldError>,
    onChange: ((ExerciseDraft) -> ExerciseDraft) -> Unit,
    onEquipmentChange: (Equipment) -> Unit,
) {
    val sunken = MaterialTheme.colorScheme.surfaceContainerLow

    FormCard {
        LabeledTextField(
            label = stringResource(R.string.exercise_field_name),
            value = draft.name,
            onValueChange = { text -> onChange { it.copy(name = text) } },
            error = if (ExerciseFieldError.NAME_MISSING in errors) stringResource(R.string.exercise_error_name) else null,
        )
    }

    FormCard(title = stringResource(R.string.exercise_field_type)) {
        val types = ExerciseType.entries
        SegmentedControl(
            options = types.map { stringResource(it.label()) },
            selectedIndex = types.indexOf(draft.type),
            onSelect = { index -> onChange { it.copy(type = types[index]) } },
        )
    }

    FormCard(title = stringResource(R.string.exercise_field_equipment)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s6), verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
            Equipment.entries.forEach { equipment ->
                ChoiceChip(
                    text = stringResource(equipment.label()),
                    selected = draft.equipment == equipment,
                    onClick = { onEquipmentChange(equipment) },
                    unselectedColor = sunken,
                )
            }
        }
    }

    FormCard(title = stringResource(R.string.exercise_field_primary)) {
        MuscleChips(selected = draft.primaryMuscles, onToggle = { muscle -> onChange { it.togglePrimary(muscle) } })
    }

    FormCard(title = stringResource(R.string.exercise_field_secondary)) {
        MuscleChips(selected = draft.secondaryMuscles, onToggle = { muscle -> onChange { it.toggleSecondary(muscle) } })
    }

    // Cardio is logged by time and distance; progression fields don't apply.
    if (draft.type != ExerciseType.CARDIO) {
        FormCard(title = stringResource(R.string.exercise_field_progression)) {
            val repError = if (ExerciseFieldError.REP_RANGE_INVALID in errors) stringResource(R.string.exercise_error_reps) else null
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                LabeledTextField(
                    label = stringResource(R.string.exercise_field_rep_min),
                    value = draft.repRangeMin,
                    onValueChange = { text -> onChange { it.copy(repRangeMin = text) } },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = stringResource(R.string.exercise_field_rep_max),
                    value = draft.repRangeMax,
                    onValueChange = { text -> onChange { it.copy(repRangeMax = text) } },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            if (repError != null) {
                Text(text = repError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                LabeledTextField(
                    label = stringResource(R.string.exercise_field_increment, stringResource(draft.unit.label())),
                    value = draft.increment,
                    onValueChange = { text -> onChange { it.copy(increment = text) } },
                    keyboardType = KeyboardType.Decimal,
                    error = if (ExerciseFieldError.INCREMENT_INVALID in errors) stringResource(R.string.exercise_error_increment) else null,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = stringResource(R.string.exercise_field_rest),
                    value = draft.restSeconds,
                    onValueChange = { text -> onChange { it.copy(restSeconds = text) } },
                    keyboardType = KeyboardType.Number,
                    error = if (ExerciseFieldError.REST_INVALID in errors) stringResource(R.string.exercise_error_rest) else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    FormCard {
        LabeledTextField(
            label = stringResource(R.string.exercise_field_note),
            value = draft.note,
            onValueChange = { text -> onChange { it.copy(note = text) } },
            singleLine = false,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MuscleChips(selected: Set<MuscleGroup>, onToggle: (MuscleGroup) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s6), verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        MuscleGroup.entries.forEach { muscle ->
            ChoiceChip(
                text = stringResource(muscle.label()),
                selected = muscle in selected,
                onClick = { onToggle(muscle) },
                unselectedColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        }
    }
}

/** White card with an optional section title. */
@Composable
private fun FormCard(title: String? = null, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
            }
            content()
        }
    }
}
