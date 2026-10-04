package io.github.wtfjb.aximo.ui.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import io.github.wtfjb.aximo.ui.components.AppSwitch
import io.github.wtfjb.aximo.ui.routine.targetEffort
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing

/**
 * A new plan from the chat (B-05): all routines with exercises and targets,
 * each one can be switched off; "N Routinen speichern" adds the rest to Pläne.
 */
@Composable
internal fun PlanSuggestionCard(
    item: SuggestionItem,
    excluded: Set<Int>,
    onToggle: (Int) -> Unit,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
) {
    val routines = item.plan.orEmpty()
    val status = item.suggestion.status
    val open = status == SuggestionStatus.OPEN
    val selected = routines.indices.count { it !in excluded }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Surface(shape = Radii.full, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Text(
                        text = stringResource(R.string.coach_category_plan),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = Spacing.s8, vertical = Spacing.s4),
                    )
                }
                Text(
                    text = pluralStringResource(R.plurals.chat_plan_routines, routines.size, routines.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusLabel(status)
            }
            Text(text = stringResource(R.string.coach_title_plan), style = MaterialTheme.typography.titleLarge)
            Text(text = item.suggestion.rationale, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            routines.forEachIndexed { index, routine ->
                RoutineBox(routine, included = index !in excluded, onToggle = { onToggle(index) }.takeIf { open })
            }

            if (open) {
                if (routines.any { r -> r.exercises.any { it.isNew } }) {
                    Text(
                        text = stringResource(R.string.chat_plan_new_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!item.applicable) {
                    Text(
                        text = stringResource(R.string.chat_plan_not_applicable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    OutlinedButton(
                        onClick = onDiscard,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(Sizes.borderControl, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f).heightIn(min = Sizes.input),
                    ) {
                        Text(text = stringResource(R.string.coach_discard), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (item.applicable) {
                        Button(
                            onClick = onApply,
                            enabled = selected > 0,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            ),
                            modifier = Modifier.weight(1f).heightIn(min = Sizes.input),
                        ) {
                            Text(text = pluralStringResource(R.plurals.chat_plan_save, selected, selected), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else if (status == SuggestionStatus.APPLIED) {
                Text(
                    text = stringResource(R.string.chat_plan_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One routine of the plan in the grey box, with its switch while the plan is open. */
@Composable
private fun RoutineBox(routine: PlanRoutineItem, included: Boolean, onToggle: (() -> Unit)?) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.s12, vertical = Spacing.s10),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onToggle != null) AppSwitch(checked = included, onCheckedChange = { onToggle() })
            }
            if (included) {
                routine.exercises.forEach { ExerciseLine(it) }
            }
        }
    }
}

@Composable
private fun ExerciseLine(exercise: PlanExerciseItem) {
    val target = stringResource(R.string.coach_sets_reps, exercise.sets, range(exercise.repMin, exercise.repMax))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text(text = exercise.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (exercise.isNew) {
            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
                Text(
                    text = stringResource(R.string.coach_badge_new),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = Spacing.s6, vertical = Spacing.s4),
                )
            }
        }
        Text(
            text = exercise.targetRir?.let { stringResource(R.string.workout_meta, target, targetEffort(it)) } ?: target,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
