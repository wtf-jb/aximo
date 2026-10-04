package io.github.wtfjb.aximo.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.workout.Effort
import io.github.wtfjb.aximo.domain.workout.SetRating
import io.github.wtfjb.aximo.ui.format.LocalSetRating
import io.github.wtfjb.aximo.ui.format.formatRpe
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.ui.components.semanticsDescription
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors

/** Visual state of a set row (design-system.md, SetRow). */
enum class SetRowState { DONE, ACTIVE, OPEN }

/** Column headers above the set rows: SATZ · KG · WDH · RIR (or RPE). */
@Composable
fun SetHeader(weightLabel: String) {
    val style = MaterialTheme.typography.labelSmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), modifier = Modifier.padding(horizontal = Spacing.s4)) {
        Text(stringResource(R.string.workout_col_set), Modifier.width(Sizes.setNumberColumn), color, style = style, textAlign = TextAlign.Center)
        Text(weightLabel, Modifier.weight(1f), color, style = style, textAlign = TextAlign.Center)
        Text(stringResource(R.string.workout_col_reps), Modifier.weight(1f), color, style = style, textAlign = TextAlign.Center)
        val effort = if (LocalSetRating.current == SetRating.RPE) R.string.workout_col_rpe else R.string.workout_col_rir
        Text(stringResource(effort), Modifier.width(Sizes.setRirColumn), color, style = style, textAlign = TextAlign.Center)
        Box(Modifier.width(Sizes.touch))
    }
}

/**
 * One set: number or type badge, weight, reps, RIR or RPE (from the settings; the
 * other scale is converted), check button. Values open the number entry, the
 * number opens the set menu.
 */
@Composable
fun SetRow(
    set: SetEntry,
    number: Int,
    state: SetRowState,
    weightText: String,
    onEdit: (SetField) -> Unit,
    onToggleDone: () -> Unit,
    onSetType: (SetType) -> Unit,
    onDelete: () -> Unit,
) {
    val label = if (number > 0) number.toString() else stringResource(R.string.workout_set_badge_warmup)
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = Sizes.touch)
        .let {
            if (state == SetRowState.DONE) {
                it.clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerLow)
            } else {
                it
            }
        }
        .padding(horizontal = Spacing.s4, vertical = Spacing.s4)

    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SetNumberCell(set = set, label = label, state = state, onSetType = onSetType, onDelete = onDelete)
        ValueCell(weightText, state, highlight = true, onClick = { onEdit(SetField.WEIGHT) }, modifier = Modifier.weight(1f))
        ValueCell(set.reps.toString(), state, highlight = true, onClick = { onEdit(SetField.REPS) }, modifier = Modifier.weight(1f))
        val rpeScale = LocalSetRating.current == SetRating.RPE
        val effort = if (rpeScale) Effort.rpe(set)?.let { formatRpe(it) } else Effort.rir(set)?.toString()
        ValueCell(
            text = effort ?: "–",
            state = state,
            highlight = false,
            onClick = { onEdit(if (rpeScale) SetField.RPE else SetField.RIR) },
            modifier = Modifier.width(Sizes.setRirColumn),
        )
        Box(modifier = Modifier.width(Sizes.touch), contentAlignment = Alignment.Center) {
            CheckButton(state = state, label = label, onClick = onToggleDone)
        }
    }
}

@Composable
private fun SetNumberCell(
    set: SetEntry,
    label: String,
    state: SetRowState,
    onSetType: (SetType) -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val menuDescription = stringResource(R.string.workout_set_menu, label)
    val badge = when (set.setType) {
        SetType.WARM_UP -> stringResource(R.string.workout_set_badge_warmup)
        SetType.DROP -> stringResource(R.string.workout_set_badge_drop)
        SetType.FAILURE -> stringResource(R.string.workout_set_badge_failure)
        SetType.WORKING -> null
    }
    Box(modifier = Modifier.width(Sizes.setNumberColumn), contentAlignment = Alignment.Center) {
        Surface(
            onClick = { menuOpen = true },
            shape = Radii.full,
            color = if (badge != null) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
            contentColor = if (badge != null) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else if (state == SetRowState.OPEN) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier
                .size(Sizes.touch)
                .padding((Sizes.touch - Sizes.badge) / 2)
                .semanticsDescription(menuDescription),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = badge ?: label,
                    style = if (badge != null) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
                )
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.label()), style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuOpen = false
                        onSetType(type)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.workout_set_delete), style = MaterialTheme.typography.bodyMedium) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ValueCell(
    text: String,
    state: SetRowState,
    highlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        SetRowState.DONE -> Surface(onClick = onClick, color = Color.Transparent, modifier = modifier) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = Spacing.s8),
            )
        }
        SetRowState.ACTIVE, SetRowState.OPEN -> {
            val active = state == SetRowState.ACTIVE
            val border = if (active && highlight) {
                BorderStroke(Sizes.borderActive, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(Sizes.borderControl, MaterialTheme.colorScheme.outline)
            }
            Surface(
                onClick = onClick,
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = border,
                modifier = modifier.heightIn(min = Sizes.input),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Bold,
                        ),
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.extendedColors.inkPlaceholder,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckButton(state: SetRowState, label: String, onClick: () -> Unit) {
    val description = if (state == SetRowState.DONE) {
        stringResource(R.string.workout_set_reopen, label)
    } else {
        stringResource(R.string.workout_set_complete, label)
    }
    val (size, color, content, border) = when (state) {
        SetRowState.DONE -> CheckStyle(
            Sizes.setCheckDone,
            MaterialTheme.colorScheme.inverseSurface,
            MaterialTheme.colorScheme.inverseOnSurface,
            null,
        )
        SetRowState.ACTIVE -> CheckStyle(
            Sizes.setCheckActive,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            null,
        )
        SetRowState.OPEN -> CheckStyle(
            Sizes.setCheckActive,
            Color.Transparent,
            MaterialTheme.extendedColors.inkPlaceholder,
            BorderStroke(Sizes.borderActive, MaterialTheme.colorScheme.outline),
        )
    }
    Surface(
        onClick = onClick,
        shape = Radii.full,
        color = color,
        contentColor = content,
        border = border,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = AppIcons.Check, contentDescription = description, modifier = Modifier.size(Sizes.iconSmall))
        }
    }
}

private data class CheckStyle(
    val size: androidx.compose.ui.unit.Dp,
    val color: Color,
    val content: Color,
    val border: BorderStroke?,
)
