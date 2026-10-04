package io.github.wtfjb.aximo.ui.routine

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.RoutineExercise
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.theme.Spacing

/** Targets of one routine exercise: sets, rep range, target RIR (empty = none). */
@Composable
fun RoutineTargetsDialog(
    exerciseName: String,
    entry: RoutineExercise,
    onConfirm: (sets: Int, repMin: Int, repMax: Int, rir: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var sets by remember { mutableStateOf(entry.targetSets.toString()) }
    var repMin by remember { mutableStateOf(entry.repMin.toString()) }
    var repMax by remember { mutableStateOf(entry.repMax.toString()) }
    var rir by remember { mutableStateOf(entry.targetRir?.toString() ?: "") }
    var showError by remember { mutableStateOf(false) }

    fun tryConfirm() {
        val s = sets.trim().toIntOrNull()
        val min = repMin.trim().toIntOrNull()
        val max = repMax.trim().toIntOrNull()
        val r = if (rir.isBlank()) null else rir.trim().toIntOrNull() ?: -1
        val valid = s != null && s >= 1 && min != null && max != null && min in 1..max && (r == null || r >= 0)
        if (valid) onConfirm(s!!, min!!, max!!, r) else showError = true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.routine_targets_title, exerciseName), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    LabeledTextField(
                        label = stringResource(R.string.routine_field_sets),
                        value = sets,
                        onValueChange = { sets = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    LabeledTextField(
                        label = stringResource(R.string.routine_field_rir),
                        value = rir,
                        onValueChange = { rir = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    LabeledTextField(
                        label = stringResource(R.string.routine_field_rep_min),
                        value = repMin,
                        onValueChange = { repMin = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    LabeledTextField(
                        label = stringResource(R.string.routine_field_rep_max),
                        value = repMax,
                        onValueChange = { repMax = it },
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (showError) {
                    Text(
                        text = stringResource(R.string.routine_targets_invalid),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        confirmButton = { InverseButton(text = stringResource(R.string.routine_apply), onClick = ::tryConfirm) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.routine_cancel), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
