package io.github.wtfjb.aximo.ui.cardio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.Instant

/** Duration as hours, minutes and seconds. Apply is ignored while the total is 0 or a field is invalid. */
@Composable
fun DurationDialog(initialSec: Int?, onConfirm: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    val (h, m, s) = CardioMath.split(initialSec ?: 0)
    var hours by remember { mutableStateOf(if (h > 0) h.toString() else "") }
    var minutes by remember { mutableStateOf(if (initialSec != null) m.toString() else "") }
    var seconds by remember { mutableStateOf(if (initialSec != null) s.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.cardio_duration), style = MaterialTheme.typography.titleMedium) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                LabeledTextField(
                    label = stringResource(R.string.cardio_hours),
                    value = hours,
                    onValueChange = { hours = it },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = stringResource(R.string.cardio_minutes),
                    value = minutes,
                    onValueChange = { minutes = it },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                LabeledTextField(
                    label = stringResource(R.string.cardio_seconds),
                    value = seconds,
                    onValueChange = { seconds = it },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            InverseButton(
                text = stringResource(R.string.workout_apply),
                onClick = {
                    if (CardioMath.durationOf(hours, minutes, seconds) != null) onConfirm(hours, minutes, seconds)
                },
            )
        },
        dismissButton = { CancelButton(onDismiss) },
    )
}

/**
 * Start date and time: first the date picker, then the time picker. The
 * result is in the device time zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartDateTimeDialog(initial: Instant, onConfirm: (Instant) -> Unit, onDismiss: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val start = java.time.Instant.ofEpochSecond(initial.epochSeconds).atZone(zone)
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    val date = pickedDate
    if (date == null) {
        // The date picker works with UTC midnight.
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                InverseButton(
                    text = stringResource(R.string.cardio_next),
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            pickedDate = java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                    },
                )
            },
            dismissButton = { CancelButton(onDismiss) },
        ) {
            DatePicker(state = dateState)
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = start.hour,
            initialMinute = start.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.cardio_start_time), style = MaterialTheme.typography.titleMedium) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                InverseButton(
                    text = stringResource(R.string.workout_apply),
                    onClick = {
                        val local = date.atTime(LocalTime.of(timeState.hour, timeState.minute)).atZone(zone)
                        onConfirm(Instant.fromEpochSeconds(local.toEpochSecond()))
                    },
                )
            },
            dismissButton = { CancelButton(onDismiss) },
        )
    }
}

/** Asks before deleting an entry. */
@Composable
fun DeleteCardioDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.cardio_delete_title), style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = stringResource(R.string.cardio_delete_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { InverseButton(text = stringResource(R.string.cardio_delete), onClick = onConfirm) },
        dismissButton = { CancelButton(onDismiss) },
    )
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = stringResource(R.string.workout_cancel), style = MaterialTheme.typography.labelLarge)
    }
}
