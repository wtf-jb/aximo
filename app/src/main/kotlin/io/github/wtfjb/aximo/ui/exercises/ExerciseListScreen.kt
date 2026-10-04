package io.github.wtfjb.aximo.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.PillTextButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SearchField
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Elevation
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * List of all exercises with search and region filter (A-01, mockup Uebungen.html).
 * With [selectionMode] it is the picker for a workout: tapping selects, and the
 * floating bar returns the selection via [onPicked] (ids in tap order, superset or not).
 */
@Composable
fun ExerciseListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    selectionMode: Boolean = false,
    onPicked: (ids: List<Long>, superset: Boolean) -> Unit = { _, _ -> },
    viewModel: ExerciseListViewModel = koinViewModel(key = "exercises-$selectionMode") { parametersOf(selectionMode) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        ExerciseListContent(
            state = state,
            viewModel = viewModel,
            onBack = onBack,
            onCreate = onCreate,
            onRowClick = { id -> if (state.selectionMode) viewModel.onToggleSelected(id) else onOpen(id) },
        )
        if (state.selectionMode && state.selectedIds.isNotEmpty()) {
            SelectionBar(
                count = state.selectedIds.size,
                onSuperset = { onPicked(state.selectedIds, true) },
                onAdd = { onPicked(state.selectedIds, false) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(Spacing.s12),
            )
        }
    }
}

@Composable
private fun ExerciseListContent(
    state: ExerciseListUiState,
    viewModel: ExerciseListViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onRowClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                CircleIconButton(
                    icon = if (state.selectionMode) AppIcons.Close else AppIcons.Back,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                )
                Text(
                    text = stringResource(if (state.selectionMode) R.string.picker_title else R.string.exercises_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                PillTextButton(
                    text = stringResource(R.string.exercises_new),
                    onClick = onCreate,
                    contentDescription = stringResource(R.string.exercises_new_description),
                )
            }
            if (state.hasAnyExercise) {
                SearchField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = stringResource(R.string.exercises_search),
                )
            }
        }
        if (state.hasAnyExercise) {
            FilterRow(
                region = state.region,
                showArchived = state.showArchived,
                showArchiveChip = !state.selectionMode,
                onRegionSelect = viewModel::onRegionSelect,
                onArchivedToggle = viewModel::onShowArchivedToggle,
            )
            ExerciseList(
                exercises = state.exercises,
                selectionMode = state.selectionMode,
                selectedIds = state.selectedIds,
                onRowClick = onRowClick,
            )
        } else if (!state.loading) {
            EmptyState(onCreate = onCreate)
        }
    }
}

@Composable
private fun FilterRow(
    region: BodyRegion?,
    showArchived: Boolean,
    showArchiveChip: Boolean,
    onRegionSelect: (BodyRegion?) -> Unit,
    onArchivedToggle: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.s16, vertical = Spacing.s4),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s6),
    ) {
        item {
            ChoiceChip(stringResource(R.string.exercises_filter_all), region == null, { onRegionSelect(null) })
        }
        items(BodyRegion.entries) { r ->
            ChoiceChip(stringResource(r.label()), region == r, { onRegionSelect(r) })
        }
        if (showArchiveChip) {
            item {
                ChoiceChip(stringResource(R.string.exercises_filter_archived), showArchived, onArchivedToggle)
            }
        }
    }
}

@Composable
private fun ExerciseList(
    exercises: List<Exercise>,
    selectionMode: Boolean,
    selectedIds: List<Long>,
    onRowClick: (Long) -> Unit,
) {
    if (exercises.isEmpty()) {
        Text(
            text = stringResource(R.string.exercises_no_results),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.s20),
        )
        return
    }
    LazyColumn(
        // Extra space at the bottom so the floating selection bar doesn't cover the last row.
        contentPadding = PaddingValues(
            start = Spacing.s16,
            end = Spacing.s16,
            top = Spacing.s8,
            bottom = if (selectionMode) Sizes.cta * 2 else Spacing.s24,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.s6),
    ) {
        items(exercises, key = { it.id }) { exercise ->
            ExerciseRow(
                exercise = exercise,
                selectionMode = selectionMode,
                selected = exercise.id in selectedIds,
                onClick = { onRowClick(exercise.id) },
            )
        }
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, selectionMode: Boolean, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(Sizes.borderActive, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { if (selectionMode) this.selected = selected },
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.s10, end = Spacing.s12, top = Spacing.s10, bottom = Spacing.s10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.listTile)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = exercise.type.icon(),
                    contentDescription = stringResource(exercise.type.label()),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.icon),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = exerciseMeta(exercise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (selectionMode) {
                SelectionCheck(selected)
            }
        }
    }
}

@Composable
private fun SelectionCheck(selected: Boolean) {
    val modifier = Modifier
        .size(Sizes.check)
        .clip(Radii.full)
    Box(
        modifier = if (selected) {
            modifier.background(MaterialTheme.colorScheme.primary)
        } else {
            modifier.border(Sizes.borderActive, MaterialTheme.colorScheme.outline, Radii.full)
        },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Sizes.iconSmall),
            )
        }
    }
}

/** Floating inverse bar of the picker: "Als Supersatz" and "N Übungen hinzufügen". */
@Composable
private fun SelectionBar(count: Int, onSuperset: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.xxl,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = Elevation.float,
    ) {
        Row(modifier = Modifier.padding(Spacing.s10), horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            Button(
                onClick = onSuperset,
                enabled = count >= 2,
                modifier = Modifier.weight(1f).heightIn(min = Sizes.touch),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.extendedColors.inverseRaised,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    disabledContainerColor = MaterialTheme.extendedColors.inverseRaised,
                    disabledContentColor = MaterialTheme.extendedColors.onInverseMuted,
                ),
            ) {
                Text(text = stringResource(R.string.picker_superset), style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.weight(1.2f).heightIn(min = Sizes.touch),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = pluralStringResource(R.plurals.picker_add, count, count),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

/** "Brust, Trizeps · Langhantel" */
@Composable
private fun exerciseMeta(exercise: Exercise): String {
    val muscles = exercise.primaryMuscles.sortedBy { it.ordinal }.map { stringResource(it.label()) }
    val equipment = stringResource(exercise.equipment.label())
    return (listOf(muscles.joinToString(", ")).filter { it.isNotEmpty() } + equipment).joinToString(" · ")
}

fun ExerciseType.icon(): ImageVector = when (this) {
    ExerciseType.STRENGTH -> AppIcons.Dumbbell
    ExerciseType.BODYWEIGHT -> AppIcons.Bodyweight
    ExerciseType.CARDIO -> AppIcons.Cardio
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.s20),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Text(text = stringResource(R.string.exercises_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.exercises_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = stringResource(R.string.exercises_create),
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
