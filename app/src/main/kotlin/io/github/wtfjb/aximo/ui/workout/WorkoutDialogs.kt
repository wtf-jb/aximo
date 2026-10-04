package io.github.wtfjb.aximo.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.format.localizeDecimal
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Spacing

/**
 * Number entry for a set value: −/+ change it by [step], the keyboard works too.
 * [decimal] allows a decimal value (weight); [allowEmpty] allows clearing it (RIR).
 */
@Composable
fun NumberEntryDialog(
    title: String,
    initial: String,
    step: Double,
    decimal: Boolean,
    allowEmpty: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }

    fun stepBy(delta: Double) {
        val current = ExerciseDraft.parseNumber(text) ?: 0.0
        val next = (current + delta).coerceAtLeast(if (decimal) -Double.MAX_VALUE else 0.0)
        text = localizeDecimal(ExerciseDraft.formatNumber(next))
    }

    val valid = (allowEmpty && text.isBlank()) ||
        (if (decimal) ExerciseDraft.parseNumber(text) != null else text.trim().toIntOrNull()?.let { it >= 0 } == true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                CircleIconButton(AppIcons.Minus, stringResource(R.string.workout_value_decrease), { stepBy(-step) })
                LabeledTextField(
                    label = title,
                    value = text,
                    onValueChange = { text = it },
                    keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                CircleIconButton(AppIcons.Plus, stringResource(R.string.workout_value_increase), { stepBy(step) })
            }
        },
        confirmButton = {
            InverseButton(
                text = stringResource(R.string.workout_apply),
                onClick = { if (valid) onConfirm(text) },
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.workout_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** Free-text note for an exercise. */
@Composable
fun NoteDialog(title: String, initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { LabeledTextField(label = title, value = text, onValueChange = { text = it }, singleLine = false) },
        confirmButton = { InverseButton(text = stringResource(R.string.workout_save_note), onClick = { onConfirm(text) }) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.workout_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** "Beenden": optional workout note, finish or discard. Discarding asks once more. */
@Composable
fun FinishDialog(initialNote: String, onFinish: (String) -> Unit, onDiscard: () -> Unit, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf(initialNote) }
    var confirmDiscard by remember { mutableStateOf(false) }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.workout_discard_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = stringResource(R.string.workout_discard_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { InverseButton(text = stringResource(R.string.workout_discard), onClick = onDiscard) },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(text = stringResource(R.string.workout_cancel), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.workout_finish_title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                LabeledTextField(
                    label = stringResource(R.string.workout_finish_note),
                    value = note,
                    onValueChange = { note = it },
                    singleLine = false,
                )
                TextButton(onClick = { confirmDiscard = true }) {
                    Text(
                        text = stringResource(R.string.workout_discard),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        confirmButton = { InverseButton(text = stringResource(R.string.workout_finish), onClick = { onFinish(note) }) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.workout_continue), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
