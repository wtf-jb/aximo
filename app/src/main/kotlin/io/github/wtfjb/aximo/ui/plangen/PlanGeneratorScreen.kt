package io.github.wtfjb.aximo.ui.plangen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.plan.PlanException
import io.github.wtfjb.aximo.domain.plan.PlanGoal
import io.github.wtfjb.aximo.domain.plan.PlanOptions
import io.github.wtfjb.aximo.domain.plan.PlanProposal
import io.github.wtfjb.aximo.domain.plan.ProposedExercise
import io.github.wtfjb.aximo.domain.plan.ProposedRoutine
import io.github.wtfjb.aximo.ui.ai.label
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.routine.targetEffort
import io.github.wtfjb.aximo.ui.settings.CancelButton
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import org.koin.androidx.compose.koinViewModel

/**
 * "Mit KI erstellen" (B-03): wishes in, routine draft out. The draft is only
 * saved after the user confirms it.
 */
@Composable
fun PlanGeneratorScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: PlanGeneratorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val proposal = state.proposal

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }
    // From the draft, back goes to the form first.
    BackHandler(enabled = proposal != null) { viewModel.discard() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), if (proposal != null) viewModel::discard else onBack)
            Text(text = stringResource(R.string.plangen_title), style = MaterialTheme.typography.headlineSmall)
        }

        if (proposal == null) {
            Form(state, viewModel)
        } else {
            Draft(proposal, viewModel)
        }
    }

    if (state.showNotice) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.coach_notice_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = stringResource(R.string.plangen_notice_body), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { InverseButton(text = stringResource(R.string.coach_notice_confirm), onClick = viewModel::acceptNotice) },
            dismissButton = {
                Row {
                    TextButton(onClick = viewModel::showSentData) {
                        Text(text = stringResource(R.string.coach_sent_data_short), style = MaterialTheme.typography.labelLarge)
                    }
                    CancelButton(viewModel::dismissNotice)
                }
            },
        )
    }
    state.sentData?.let { data ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSentData,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.coach_sent_data), style = MaterialTheme.typography.titleMedium) },
            text = {
                SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = data, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSentData) {
                    Text(text = stringResource(R.string.coach_close), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun ColumnScope.Form(state: PlanGeneratorUiState, viewModel: PlanGeneratorViewModel) {
    val request = state.request

    Text(
        text = stringResource(R.string.plangen_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Section(R.string.plangen_goal) {
        PlanGoal.entries.forEach { goal ->
            ChoiceChip(text = stringResource(goal.label()), selected = request.goal == goal, onClick = { viewModel.setGoal(goal) })
        }
    }
    Section(R.string.plangen_days) {
        PlanOptions.days.forEach { days ->
            ChoiceChip(text = "$days", selected = request.daysPerWeek == days, onClick = { viewModel.setDays(days) })
        }
    }
    Section(R.string.plangen_minutes) {
        PlanOptions.minutes.forEach { minutes ->
            ChoiceChip(
                text = stringResource(R.string.plangen_minutes_value, minutes),
                selected = request.minutes == minutes,
                onClick = { viewModel.setMinutes(minutes) },
            )
        }
    }
    Section(R.string.plangen_equipment) {
        PlanOptions.equipment.forEach { equipment ->
            ChoiceChip(
                text = stringResource(equipment.label()),
                selected = equipment in request.equipment,
                onClick = { viewModel.toggleEquipment(equipment) },
            )
        }
    }
    LabeledTextField(
        label = stringResource(R.string.plangen_restrictions),
        value = request.restrictions,
        onValueChange = viewModel::setRestrictions,
        singleLine = false,
        placeholder = stringResource(R.string.plangen_restrictions_placeholder),
        modifier = Modifier.fillMaxWidth(),
    )

    ErrorText(state.error)
    if (state.running) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircularProgressIndicator(modifier = Modifier.size(Sizes.icon), strokeWidth = Sizes.borderActive)
            Text(
                text = stringResource(R.string.plangen_running),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    } else {
        PrimaryButton(text = stringResource(R.string.plangen_create), onClick = viewModel::requestPlan, modifier = Modifier.fillMaxWidth())
    }
    TextButton(onClick = viewModel::showSentData, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(text = stringResource(R.string.coach_sent_data), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Section(title: Int, chips: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text(text = stringResource(title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            chips()
        }
    }
}

@Composable
private fun Draft(proposal: PlanProposal, viewModel: PlanGeneratorViewModel) {
    Surface(
        shape = Radii.xxl,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s18), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Icon(AppIcons.Sparkle, contentDescription = null, tint = MaterialTheme.extendedColors.onInverseAccentContainer, modifier = Modifier.size(Sizes.iconSmall))
                Text(
                    text = stringResource(R.string.plangen_draft),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.onInverseAccentContainer,
                )
            }
            Text(text = proposal.summary, style = MaterialTheme.typography.bodyLarge)
        }
    }
    proposal.routines.forEachIndexed { index, routine ->
        RoutineCard(routine, onRemove = { viewModel.removeRoutine(index) })
    }
    if (proposal.dropped > 0) {
        Text(
            text = pluralStringResource(R.plurals.plangen_dropped, proposal.dropped, proposal.dropped),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(
        text = stringResource(R.string.plangen_save_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (proposal.routines.isNotEmpty()) {
        PrimaryButton(
            text = pluralStringResource(R.plurals.plangen_save, proposal.routines.size, proposal.routines.size),
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    SecondaryButton(text = stringResource(R.string.plangen_again), onClick = viewModel::discard, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun RoutineCard(routine: ProposedRoutine, onRemove: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = routine.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onRemove) {
                    Text(
                        text = stringResource(R.string.plangen_remove_routine),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            routine.exercises.forEach { ExerciseRow(it) }
        }
    }
}

@Composable
private fun ExerciseRow(exercise: ProposedExercise) {
    val reps = if (exercise.repMin == exercise.repMax) "${exercise.repMin}" else "${exercise.repMin}–${exercise.repMax}"
    val target = stringResource(R.string.coach_sets_reps, exercise.sets, reps)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = exercise.targetRir?.let { stringResource(R.string.workout_meta, target, targetEffort(it)) } ?: target,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun PlanGoal.label(): Int = when (this) {
    PlanGoal.STRENGTH -> R.string.plangen_goal_strength
    PlanGoal.HYPERTROPHY -> R.string.plangen_goal_hypertrophy
    PlanGoal.GENERAL -> R.string.plangen_goal_general
}

@Composable
private fun ErrorText(error: PlanError?) {
    val text = when (error) {
        null -> return
        is PlanError.Ai -> {
            val reason = stringResource(error.reason.label())
            val headline = error.statusCode?.let { stringResource(R.string.ai_error_with_status, reason, it) } ?: reason
            headline + error.detail?.let { "\n$it" }.orEmpty()
        }
        is PlanError.Plan -> stringResource(
            when (error.reason) {
                PlanException.Reason.NO_PROFILE -> R.string.coach_error_no_profile
                PlanException.Reason.NOT_ENOUGH_EXERCISES -> R.string.plangen_error_few_exercises
                PlanException.Reason.NOTHING_USABLE -> R.string.plangen_error_nothing_usable
            },
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
