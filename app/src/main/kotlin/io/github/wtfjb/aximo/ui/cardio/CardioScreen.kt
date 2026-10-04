package io.github.wtfjb.aximo.ui.cardio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.cardio.CardioDraft
import io.github.wtfjb.aximo.domain.cardio.CardioFieldError
import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.format.formatInteger
import io.github.wtfjb.aximo.ui.format.formatShortDate
import io.github.wtfjb.aximo.ui.format.formatTime
import io.github.wtfjb.aximo.ui.format.localizeDecimal
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import io.github.wtfjb.aximo.ui.workout.NoteDialog
import io.github.wtfjb.aximo.ui.workout.NumberEntryDialog
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Which entry dialog is open. */
private enum class CardioDialog { START, DURATION, DISTANCE, HEART_RATE, ELEVATION, NOTE, DELETE }

/** "Cardio erfassen" (A-04, mockup Cardio.html). Health Connect import (C-01) is left out. */
@Composable
fun CardioScreen(
    entryId: Long,
    onDone: () -> Unit,
    viewModel: CardioViewModel = koinViewModel { parametersOf(entryId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<CardioDialog?>(null) }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            CircleIconButton(AppIcons.Close, stringResource(R.string.action_back), onDone)
            Text(
                text = stringResource(if (state.isNew) R.string.cardio_title else R.string.cardio_title_edit),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (!state.isNew && !state.loading) {
                TextButton(onClick = { dialog = CardioDialog.DELETE }) {
                    Text(
                        text = stringResource(R.string.cardio_delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        if (!state.loading) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s10),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12),
            ) {
                ActivityGrid(
                    activities = state.activities,
                    selectedId = state.draft.exerciseId,
                    onSelect = viewModel::onActivityChange,
                )
                if (CardioFieldError.ACTIVITY_MISSING in state.errors) {
                    ErrorText(stringResource(R.string.cardio_error_activity))
                }
                StartButton(startedAt = state.draft.startedAt, onClick = { dialog = CardioDialog.START })
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    MetricTile(
                        label = stringResource(R.string.cardio_duration),
                        value = state.draft.durationSec?.let { CardioMath.formatDuration(it.toLong()) },
                        unit = stringResource(R.string.cardio_duration_unit),
                        highlighted = CardioFieldError.DURATION_MISSING in state.errors,
                        onClick = { dialog = CardioDialog.DURATION },
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        label = stringResource(R.string.cardio_distance),
                        value = state.draft.distanceM?.let { formatKm(it) },
                        unit = stringResource(R.string.cardio_km),
                        highlighted = false,
                        onClick = { dialog = CardioDialog.DISTANCE },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (CardioFieldError.DURATION_MISSING in state.errors) {
                    ErrorText(stringResource(R.string.cardio_error_duration))
                }
                PaceCard(state)
                OptionalFields(state.draft, onOpen = { dialog = it })
                Spacer(Modifier.size(Spacing.s4))
            }
            PrimaryButton(
                text = stringResource(R.string.cardio_save),
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s12, bottom = Spacing.s20),
            )
        }
    }

    val draft = state.draft
    when (dialog) {
        CardioDialog.START -> StartDateTimeDialog(
            initial = draft.startedAt,
            onConfirm = { viewModel.onStartChange(it); dialog = null },
            onDismiss = { dialog = null },
        )
        CardioDialog.DURATION -> DurationDialog(
            initialSec = draft.durationSec,
            onConfirm = { h, m, s -> viewModel.onDurationChange(h, m, s); dialog = null },
            onDismiss = { dialog = null },
        )
        CardioDialog.DISTANCE -> NumberEntryDialog(
            title = stringResource(R.string.cardio_distance_km),
            initial = draft.distanceM?.let { localizeDecimal(CardioDraft.formatKm(it)) } ?: "",
            step = DISTANCE_STEP_KM,
            decimal = true,
            allowEmpty = true,
            onConfirm = { viewModel.onDistanceChange(it); dialog = null },
            onDismiss = { dialog = null },
        )
        CardioDialog.HEART_RATE -> NumberEntryDialog(
            title = stringResource(R.string.cardio_heart_rate_bpm),
            initial = draft.avgHeartRate?.toString() ?: "",
            step = 1.0,
            decimal = false,
            allowEmpty = true,
            onConfirm = { viewModel.onHeartRateChange(it); dialog = null },
            onDismiss = { dialog = null },
        )
        CardioDialog.ELEVATION -> NumberEntryDialog(
            title = stringResource(R.string.cardio_elevation_m),
            initial = draft.elevationM?.let { ExerciseDraft.formatNumber(it) } ?: "",
            step = ELEVATION_STEP_M,
            decimal = false,
            allowEmpty = true,
            onConfirm = { viewModel.onElevationChange(it); dialog = null },
            onDismiss = { dialog = null },
        )
        CardioDialog.NOTE -> NoteDialog(
            title = stringResource(R.string.cardio_note),
            initial = draft.note,
            onConfirm = { viewModel.onNoteChange(it); dialog = null },
            onDismiss = { dialog = null },
        )
        CardioDialog.DELETE -> DeleteCardioDialog(
            onConfirm = { viewModel.delete(); dialog = null },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private const val DISTANCE_STEP_KM = 0.5
private const val ELEVATION_STEP_M = 10.0
private const val ACTIVITY_COLUMNS = 4

/** Activities as a grid of four equal buttons per row, the selected one inverse. */
@Composable
private fun ActivityGrid(activities: List<Exercise>, selectedId: Long?, onSelect: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        activities.chunked(ACTIVITY_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                row.forEach { activity ->
                    val selected = activity.id == selectedId
                    Surface(
                        onClick = { onSelect(activity.id) },
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Sizes.touch)
                            .semantics {
                                this.selected = selected
                                role = Role.RadioButton
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = Spacing.s4)) {
                            Text(text = activity.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        }
                    }
                }
                // Keep the columns aligned in an incomplete last row.
                repeat(ACTIVITY_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** "Heute · 08:15" or "Sa., 3. Okt. · 08:15", opens date and time pickers. */
@Composable
private fun StartButton(startedAt: Instant, onClick: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val date = java.time.Instant.ofEpochSecond(startedAt.epochSeconds).atZone(ZoneId.systemDefault()).toLocalDate()
    val day = if (date == LocalDate.now()) stringResource(R.string.cardio_today) else formatShortDate(startedAt)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.search),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s10),
        ) {
            Icon(AppIcons.Calendar, contentDescription = null, tint = muted, modifier = Modifier.size(Sizes.icon))
            Text(
                text = stringResource(R.string.cardio_when, day, formatTime(startedAt)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            Icon(
                AppIcons.Minimize,
                contentDescription = stringResource(R.string.cardio_change_start),
                tint = muted,
                modifier = Modifier.size(Sizes.iconSmall),
            )
        }
    }
}

/** Big value tile for duration and distance. Empty shows a placeholder, [highlighted] an accent ring. */
@Composable
private fun MetricTile(
    label: String,
    value: String?,
    unit: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (highlighted) BorderStroke(Sizes.borderActive, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value ?: stringResource(R.string.cardio_empty_value),
                style = MaterialTheme.typography.displaySmall,
                color = if (value == null) MaterialTheme.extendedColors.inkPlaceholder else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(text = unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Calculated pace (or speed) on the inverse card; the other value on the right. */
@Composable
private fun PaceCard(state: CardioUiState) {
    val extended = MaterialTheme.extendedColors
    val empty = stringResource(R.string.cardio_empty_value)
    val pace = state.paceSeconds?.let { CardioMath.formatPace(it) }
    val paceText = when (state.paceStyle) {
        PaceStyle.PER_500M -> stringResource(R.string.cardio_pace_per_500m, pace ?: empty)
        else -> stringResource(R.string.cardio_pace_per_km, pace ?: empty)
    }
    val speedText = stringResource(R.string.cardio_speed_kmh, state.speedKmh?.let { formatSpeed(it) } ?: empty)
    val speedFirst = state.paceStyle == PaceStyle.SPEED

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s18, vertical = Spacing.s16),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.s16),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                    Text(
                        text = stringResource(if (speedFirst) R.string.cardio_speed_calculated else R.string.cardio_pace_calculated),
                        style = MaterialTheme.typography.labelMedium,
                        color = extended.onInverseMuted,
                    )
                    Text(text = if (speedFirst) speedText else paceText, style = MaterialTheme.typography.displaySmall, maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                    Text(
                        text = stringResource(if (speedFirst) R.string.cardio_pace else R.string.cardio_avg_speed),
                        style = MaterialTheme.typography.bodySmall,
                        color = extended.onInverseMuted,
                    )
                    Text(text = if (speedFirst) paceText else speedText, style = MaterialTheme.typography.titleSmall)
                }
            }
            CaloriesRow(state)
        }
    }
}

/** Calorie estimate under the pace; asks for the body weight if it is missing. */
@Composable
private fun CaloriesRow(state: CardioUiState) {
    val muted = MaterialTheme.extendedColors.onInverseMuted
    if (state.bodyWeightKg == null) {
        Text(text = stringResource(R.string.calories_no_body_weight), style = MaterialTheme.typography.bodySmall, color = muted)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s16)) {
        Text(
            text = stringResource(R.string.calories_estimated),
            style = MaterialTheme.typography.bodySmall,
            color = muted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = state.kcal?.let { stringResource(R.string.calories_value, formatInteger(it.toDouble())) } ?: stringResource(R.string.cardio_empty_value),
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

/** Heart rate, elevation and note; empty ones say "optional". */
@Composable
private fun OptionalFields(draft: CardioDraft, onOpen: (CardioDialog) -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s4)) {
            OptionalRow(
                icon = AppIcons.Heart,
                label = stringResource(R.string.cardio_heart_rate),
                value = draft.avgHeartRate?.let { stringResource(R.string.cardio_bpm, it) },
                onClick = { onOpen(CardioDialog.HEART_RATE) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OptionalRow(
                icon = AppIcons.Mountain,
                label = stringResource(R.string.cardio_elevation),
                value = draft.elevationM?.let { stringResource(R.string.cardio_meters, ExerciseDraft.formatNumber(it)) },
                onClick = { onOpen(CardioDialog.ELEVATION) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            OptionalRow(
                icon = AppIcons.Note,
                label = stringResource(R.string.cardio_note),
                value = draft.note.takeIf { it.isNotBlank() },
                onClick = { onOpen(CardioDialog.NOTE) },
            )
        }
    }
}

@Composable
private fun OptionalRow(icon: ImageVector, label: String, value: String?, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.heightIn(min = Sizes.cta),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(Sizes.icon))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (value != null) {
                Text(text = value, style = MaterialTheme.typography.labelLarge, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            } else {
                Text(
                    text = stringResource(R.string.cardio_optional),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
}
