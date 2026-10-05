package io.github.wtfjb.aximo.ui.logging

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.logging.ExerciseChoice
import io.github.wtfjb.aximo.domain.logging.LoggingException
import io.github.wtfjb.aximo.domain.logging.LoggingProposal
import io.github.wtfjb.aximo.domain.logging.ProposedLogEntry
import io.github.wtfjb.aximo.domain.logging.SpeechError
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.ui.ai.label
import io.github.wtfjb.aximo.ui.components.AppSwitch
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.components.SetChip
import io.github.wtfjb.aximo.ui.format.LocalWeightUnit
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.settings.CancelButton
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import org.koin.androidx.compose.koinViewModel

/**
 * "Per Text oder Sprache erfassen" (B-04): text field with microphone in, preview of
 * the sets out. Nothing is saved before the user confirms the preview.
 */
@Composable
fun LoggingScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: LoggingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val proposal = state.proposal

    LaunchedEffect(state.saved) {
        if (state.saved != null) onDone()
    }
    // From the preview, back goes to the text first.
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
            Text(text = stringResource(R.string.logging_title), style = MaterialTheme.typography.headlineSmall)
        }

        if (proposal == null) {
            Input(state, viewModel)
        } else {
            Preview(proposal, viewModel)
        }
    }

    if (state.showNotice) {
        AlertDialog(
            onDismissRequest = viewModel::dismissNotice,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.coach_notice_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = stringResource(R.string.logging_notice_body), style = MaterialTheme.typography.bodyMedium) },
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
private fun ColumnScope.Input(state: LoggingUiState, viewModel: LoggingViewModel) {
    // The microphone permission is only asked for at the first tap on the microphone.
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startListening() else viewModel.speechPermissionDenied()
    }
    val onMic = {
        when {
            state.listening -> viewModel.stopListening()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ->
                viewModel.startListening()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Text(
        text = stringResource(R.string.logging_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalAlignment = Alignment.Bottom) {
        LabeledTextField(
            label = stringResource(R.string.logging_field),
            value = state.text,
            onValueChange = viewModel::setText,
            containerColor = MaterialTheme.colorScheme.surface,
            singleLine = false,
            placeholder = stringResource(R.string.logging_placeholder),
            modifier = Modifier.weight(1f),
        )
        if (state.speechAvailable) {
            CircleIconButton(
                icon = if (state.listening) AppIcons.Close else AppIcons.Mic,
                contentDescription = stringResource(if (state.listening) R.string.logging_mic_stop else R.string.logging_mic),
                onClick = onMic,
            )
        }
    }
    if (state.listening) {
        Text(
            text = stringResource(R.string.logging_listening),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    state.speechError?.let { SpeechErrorText(it) }

    ErrorText(state.error)
    if (state.running) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircularProgressIndicator(modifier = Modifier.size(Sizes.icon), strokeWidth = Sizes.borderActive)
            Text(
                text = stringResource(R.string.logging_running),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    } else {
        PrimaryButton(text = stringResource(R.string.logging_parse), onClick = viewModel::requestParse, modifier = Modifier.fillMaxWidth())
    }
    TextButton(onClick = viewModel::showSentData, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(text = stringResource(R.string.coach_sent_data), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Preview(proposal: LoggingProposal, viewModel: LoggingViewModel) {
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
                    text = stringResource(R.string.logging_preview),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.onInverseAccentContainer,
                )
            }
            Text(text = stringResource(R.string.logging_preview_hint), style = MaterialTheme.typography.bodyLarge)
        }
    }
    proposal.entries.forEachIndexed { index, entry ->
        EntryCard(entry, index, viewModel)
    }
    if (proposal.dropped > 0) {
        Text(
            text = pluralStringResource(R.plurals.logging_dropped, proposal.dropped, proposal.dropped),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (proposal.canSave) {
        val sets = proposal.confirmed.sumOf { it.sets.size }
        PrimaryButton(
            text = pluralStringResource(R.plurals.logging_save, sets, sets),
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.logging_save_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Text(
            text = stringResource(if (proposal.needsChoice) R.string.logging_choose_hint else R.string.logging_nothing_selected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
    SecondaryButton(text = stringResource(R.string.logging_edit_text), onClick = viewModel::discard, modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surface)
}

@Composable
private fun EntryCard(entry: ProposedLogEntry, index: Int, viewModel: LoggingViewModel) {
    val choice = entry.choice
    val isNew = choice is ExerciseChoice.Create
    val title = when (choice) {
        is ExerciseChoice.Existing -> choice.exercise.name
        is ExerciseChoice.Create -> choice.exercise.name
        null -> entry.spoken
    }
    val bodyweight = when (choice) {
        is ExerciseChoice.Existing -> choice.exercise.type == ExerciseType.BODYWEIGHT
        is ExerciseChoice.Create -> choice.exercise.type == ExerciseType.BODYWEIGHT
        null -> false
    }
    val titleColor = if (entry.included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s10)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(value = entry.included, role = Role.Switch, onValueChange = { viewModel.setIncluded(index, it) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        Text(text = title, style = MaterialTheme.typography.titleSmall, color = titleColor, modifier = Modifier.weight(1f, fill = false))
                        if (isNew) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            ) {
                                Text(
                                    text = stringResource(R.string.coach_badge_new),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = Spacing.s6, vertical = Spacing.s4),
                                )
                            }
                        }
                    }
                    if (!entry.spoken.equals(title, ignoreCase = true)) {
                        Text(
                            text = stringResource(R.string.logging_said, entry.spoken),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                AppSwitch(checked = entry.included, onCheckedChange = null)
            }
            if (entry.options.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    entry.options.forEachIndexed { optionIndex, option ->
                        ChoiceChip(
                            text = when (option) {
                                is ExerciseChoice.Existing -> option.exercise.name
                                is ExerciseChoice.Create -> stringResource(R.string.logging_option_new, option.exercise.name)
                            },
                            selected = entry.selected == optionIndex,
                            onClick = { viewModel.select(index, optionIndex) },
                            unselectedColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                entry.runs().forEach { run ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                        if (run.count > 1) {
                            Text(text = stringResource(R.string.logging_run_count, run.count), style = MaterialTheme.typography.labelLarge)
                        }
                        val set = run.set
                        SetChip(
                            set = SetEntry(
                                workoutExerciseId = 0,
                                position = 0,
                                weightKg = set.weightKg ?: 0.0,
                                reps = set.reps,
                                rpe = set.rpe,
                                rir = set.rir,
                                setType = set.setType,
                            ),
                            bodyweight = bodyweight,
                            unit = LocalWeightUnit.current,
                        )
                    }
                }
            }
            if (entry.weightMissing) {
                Text(
                    text = stringResource(R.string.logging_weight_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SpeechErrorText(error: SpeechError) {
    Text(
        text = stringResource(
            when (error) {
                SpeechError.PERMISSION -> R.string.logging_speech_permission
                SpeechError.NOTHING_HEARD -> R.string.logging_speech_nothing
                SpeechError.LANGUAGE_UNAVAILABLE -> R.string.logging_speech_language
                SpeechError.FAILED -> R.string.logging_speech_failed
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun ErrorText(error: LoggingError?) {
    val text = when (error) {
        null -> return
        is LoggingError.Ai -> {
            val reason = stringResource(error.reason.label())
            val headline = error.statusCode?.let { stringResource(R.string.ai_error_with_status, reason, it) } ?: reason
            headline + error.detail?.let { "\n$it" }.orEmpty()
        }
        is LoggingError.Logging -> stringResource(
            when (error.reason) {
                LoggingException.Reason.NO_PROFILE -> R.string.coach_error_no_profile
                LoggingException.Reason.EMPTY_TEXT -> R.string.logging_error_empty
                LoggingException.Reason.NOTHING_USABLE -> R.string.logging_error_nothing_usable
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
