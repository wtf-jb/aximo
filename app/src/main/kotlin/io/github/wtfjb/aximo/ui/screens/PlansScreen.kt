package io.github.wtfjb.aximo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatShortDate
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.plans.PlansViewModel
import io.github.wtfjb.aximo.ui.plans.RoutineItem
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/**
 * "Pläne" tab (A-05, mockup Plaene.html): free training, cardio, the routines
 * with "Als Nächstes" and the exercise list. Block card and "Mit KI erstellen"
 * are Prio B and left out; the exercise list takes the free spot next to "Routinen".
 */
@Composable
fun PlansScreen(
    onOpenWorkout: () -> Unit,
    onOpenExercises: () -> Unit,
    onLogCardio: () -> Unit,
    onOpenRoutine: (Long) -> Unit,
    onNewRoutine: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlansViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.s20, end = Spacing.s20, top = Spacing.s28, bottom = Spacing.s24),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.nav_plans),
                    style = MaterialTheme.typography.displayMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onNewRoutine,
                    shape = Radii.full,
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(Sizes.touch),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(AppIcons.Plus, stringResource(R.string.plans_new_routine), Modifier.size(Sizes.icon))
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                QuickAction(AppIcons.Play, stringResource(R.string.plans_free_training), Modifier.weight(1f)) {
                    viewModel.start(null, onOpenWorkout)
                }
                QuickAction(AppIcons.Cardio, stringResource(R.string.plans_cardio), Modifier.weight(1f), onLogCardio)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.plans_routines),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onOpenExercises,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                ) {
                    Icon(AppIcons.Dumbbell, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
                    Spacer(Modifier.size(Spacing.s6))
                    Text(text = stringResource(R.string.exercises_open), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (!state.loading && state.routines.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                    Text(
                        text = stringResource(R.string.plans_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SecondaryButton(text = stringResource(R.string.plans_create_routine), onClick = onNewRoutine)
                }
            }
        }
        items(state.routines, key = { it.routine.id }) { item ->
            RoutineCard(
                item = item,
                onOpen = { onOpenRoutine(item.routine.id) },
                onStart = { viewModel.start(item.routine.id, onOpenWorkout) },
            )
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.heightIn(min = Sizes.cta),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.s12),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun RoutineCard(item: RoutineItem, onOpen: () -> Unit, onStart: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s14, top = Spacing.s14, bottom = Spacing.s14),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    Text(text = item.routine.name, style = MaterialTheme.typography.titleSmall)
                    if (item.isNext) {
                        Surface(
                            shape = Radii.full,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ) {
                            Text(
                                text = stringResource(R.string.plans_next),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = Spacing.s8, vertical = Spacing.s4),
                            )
                        }
                    }
                }
                Text(
                    text = routineMeta(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Surface(
                onClick = onStart,
                shape = Radii.full,
                color = if (item.isNext) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = if (item.isNext) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Sizes.touch),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AppIcons.Play,
                        contentDescription = stringResource(R.string.plans_start_routine, item.routine.name),
                        modifier = Modifier.size(Sizes.iconSmall),
                    )
                }
            }
        }
    }
}

/** "6 Übungen · zuletzt Do., 1. Okt." or, if never trained, "6 Übungen · Brust, Schultern, Arme". */
@Composable
private fun routineMeta(item: RoutineItem): String {
    val count = pluralStringResource(R.plurals.plans_exercise_count, item.exerciseCount, item.exerciseCount)
    val detail = item.lastTrained?.let { stringResource(R.string.plans_last_trained, formatShortDate(it)) }
        ?: item.regions.map { stringResource(it.label()) }.joinToString(", ").takeIf { it.isNotEmpty() }
    return if (detail != null) stringResource(R.string.workout_meta, count, detail) else count
}
