package io.github.wtfjb.aximo.ui.routine

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.format.formatRest
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import io.github.wtfjb.aximo.ui.workout.PickedExercises
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Edit or create a routine (A-05, mockup Routine.html). */
@Composable
fun RoutineEditScreen(
    routineId: Long,
    picked: PickedExercises?,
    onPickedConsumed: () -> Unit,
    onAddExercise: () -> Unit,
    onDone: () -> Unit,
    viewModel: RoutineEditViewModel = koinViewModel { parametersOf(routineId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var targetsFor by remember { mutableStateOf<Int?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }
    LaunchedEffect(picked, state.loading) {
        if (picked != null && !state.loading) {
            viewModel.addExercises(picked.ids, picked.superset)
            onPickedConsumed()
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), onDone)
            Text(
                text = stringResource(if (state.isNew) R.string.routine_new_title else R.string.routine_edit_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            InverseButton(text = stringResource(R.string.routine_save), onClick = viewModel::save)
        }
        if (state.loading) return@Column

        val entries = state.draft.entries
        LazyColumn(
            contentPadding = PaddingValues(start = Spacing.s16, end = Spacing.s16, top = Spacing.s10, bottom = Spacing.s24),
            verticalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.s4, vertical = Spacing.s6),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s6),
                ) {
                    NameField(value = state.draft.name, onValueChange = viewModel::onNameChange)
                    if (state.showErrors && state.draft.nameMissing) {
                        Text(
                            text = stringResource(R.string.routine_name_missing),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (entries.isNotEmpty()) {
                        val sets = entries.sumOf { it.targetSets }
                        InfoChip(pluralStringResource(R.plurals.routine_working_sets, sets, sets))
                    }
                }
            }
            items(entryGroups(entries), key = { group -> "${group.first().index}-${group.first().entry.exerciseId}" }) { group ->
                val letter = group.first().entry.supersetGroup
                EntryCard(supersetLetter = letter) {
                    group.forEachIndexed { member, item ->
                        if (member > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        EntryRow(
                            entry = item.entry,
                            exercise = state.exercises[item.entry.exerciseId],
                            supersetLabel = letter?.let { "$it${member + 1}" },
                            canMoveUp = item.index > 0,
                            canMoveDown = item.index < entries.lastIndex,
                            onOpenTargets = { targetsFor = item.index },
                            onMoveUp = { viewModel.move(item.index, -1) },
                            onMoveDown = { viewModel.move(item.index, +1) },
                            onUngroup = { viewModel.ungroup(item.index) },
                            onRemove = { viewModel.remove(item.index) },
                        )
                    }
                }
            }
            item { AddExerciseButton(onClick = onAddExercise) }
            if (!state.isNew) {
                item {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(
                            text = stringResource(R.string.routine_delete),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }

    targetsFor?.let { index ->
        val entry = state.draft.entries.getOrNull(index)
        if (entry == null) {
            targetsFor = null
        } else {
            RoutineTargetsDialog(
                exerciseName = state.exercises[entry.exerciseId]?.name.orEmpty(),
                entry = entry,
                onConfirm = { sets, min, max, rir ->
                    viewModel.updateTargets(index, sets, min, max, rir)
                    targetsFor = null
                },
                onDismiss = { targetsFor = null },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.routine_delete_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(stringResource(R.string.routine_delete_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                InverseButton(text = stringResource(R.string.routine_delete), onClick = {
                    confirmDelete = false
                    viewModel.delete()
                })
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.routine_cancel), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

/** Routine name as a large title that can be typed into directly. */
@Composable
private fun NameField(value: String, onValueChange: (String) -> Unit) {
    val placeholder = stringResource(R.string.routine_name_placeholder)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.displaySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder },
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.extendedColors.inkPlaceholder)
                }
                inner()
            }
        },
    )
}

@Composable
private fun InfoChip(text: String) {
    Surface(shape = Radii.full, color = MaterialTheme.colorScheme.surface) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = MaterialTheme.typography.bodySmall.letterSpacing),
            modifier = Modifier.padding(horizontal = Spacing.s10, vertical = Spacing.s6),
        )
    }
}

/** An entry together with its index in the routine. */
private data class IndexedEntry(val index: Int, val entry: RoutineExercise)

/** Consecutive entries with the same superset letter form one card, all others stand alone. */
private fun entryGroups(entries: List<RoutineExercise>): List<List<IndexedEntry>> {
    val groups = mutableListOf<MutableList<IndexedEntry>>()
    entries.forEachIndexed { index, entry ->
        val last = groups.lastOrNull()
        if (entry.supersetGroup != null && last != null && last.first().entry.supersetGroup == entry.supersetGroup) {
            last += IndexedEntry(index, entry)
        } else {
            groups += mutableListOf(IndexedEntry(index, entry))
        }
    }
    return groups
}

/** White card; a superset gets a dark outline and the "SUPERSATZ A" header. */
@Composable
private fun EntryCard(supersetLetter: String?, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (supersetLetter != null) BorderStroke(Sizes.borderActive, MaterialTheme.colorScheme.inverseSurface) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.s6)) {
            if (supersetLetter != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
                    modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s6, bottom = Spacing.s4),
                ) {
                    Surface(
                        shape = Radii.full,
                        color = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ) {
                        Text(
                            text = stringResource(R.string.workout_superset, supersetLetter),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = Spacing.s10, vertical = Spacing.s4),
                        )
                    }
                    Text(
                        text = stringResource(R.string.routine_superset_rest),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

/** One routine exercise inside a card. Tap opens the targets, ⋯ the menu. */
@Composable
private fun EntryRow(
    entry: RoutineExercise,
    exercise: Exercise?,
    supersetLabel: String?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpenTargets: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUngroup: () -> Unit,
    onRemove: () -> Unit,
) {
    val name = exercise?.name.orEmpty()
    Surface(onClick = onOpenTargets, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s8, top = Spacing.s6, bottom = Spacing.s6),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(
                    text = if (supersetLabel != null) stringResource(R.string.routine_superset_entry, supersetLabel, name) else name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = entryMeta(entry, exercise, showRest = supersetLabel == null),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EntryMenu(
                name = name,
                grouped = supersetLabel != null,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onOpenTargets = onOpenTargets,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onUngroup = onUngroup,
                onRemove = onRemove,
            )
        }
    }
}

/** "3 × 6–8 · RIR 2 · Pause 2:30" */
@Composable
private fun entryMeta(entry: RoutineExercise, exercise: Exercise?, showRest: Boolean): String {
    val parts = mutableListOf(stringResource(R.string.routine_target, entry.targetSets, entry.repMin, entry.repMax))
    entry.targetRir?.let { parts += stringResource(R.string.routine_target_rir, it) }
    if (showRest && exercise != null && exercise.restSeconds > 0) {
        parts += stringResource(R.string.routine_rest, formatRest(exercise.restSeconds))
    }
    return parts.reduce { acc, part -> stringResource(R.string.workout_meta, acc, part) }
}

@Composable
private fun EntryMenu(
    name: String,
    grouped: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpenTargets: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUngroup: () -> Unit,
    onRemove: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(Sizes.touch)) {
            Icon(
                imageVector = AppIcons.More,
                contentDescription = stringResource(R.string.routine_entry_options, name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.icon),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItem(R.string.routine_edit_targets) { open = false; onOpenTargets() }
            if (canMoveUp) MenuItem(R.string.routine_move_up) { open = false; onMoveUp() }
            if (canMoveDown) MenuItem(R.string.routine_move_down) { open = false; onMoveDown() }
            if (grouped) MenuItem(R.string.routine_ungroup) { open = false; onUngroup() }
            MenuItem(R.string.routine_remove) { open = false; onRemove() }
        }
    }
}

@Composable
private fun MenuItem(label: Int, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label), style = MaterialTheme.typography.bodyMedium) }, onClick = onClick)
}

/** Dashed outline button at the end of the list (mockup). */
@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.outline
    val shapeRadius = Sizes.dashRadius
    val strokeWidth = Sizes.borderActive
    val dash = Sizes.dashLength
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.cta)
            .drawBehind {
                drawRoundRect(
                    color = color,
                    cornerRadius = CornerRadius(shapeRadius.toPx()),
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())),
                    ),
                )
            },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Plus, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
            Text(stringResource(R.string.routine_add_exercise), style = MaterialTheme.typography.labelLarge)
        }
    }
}
