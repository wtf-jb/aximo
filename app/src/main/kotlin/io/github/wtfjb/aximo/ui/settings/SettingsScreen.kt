package io.github.wtfjb.aximo.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.domain.workout.SetRating
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatRest
import io.github.wtfjb.aximo.ui.format.formatWeight
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

/** Which dialog is open. */
private enum class SettingsDialog { LANGUAGE, REST, STEPS, WEEKLY_GOAL, IMPORT }

/**
 * Settings (A-09, mockup Einstellungen.html) with export and import (A-08).
 * AI coach (B) and Health Connect (C) are left out.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val training = state.training
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
    val context = LocalContext.current
    val language = remember { if (AppLanguages.isSupported) AppLanguages.current(context) else AppLanguage.SYSTEM }

    // Android's file dialogs: create a file for the exports, pick one for the import.
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_JSON)) { uri ->
        uri?.let { viewModel.exportJson(it.toString()) }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_CSV)) { uri ->
        uri?.let { viewModel.exportCsv(it.toString()) }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restore(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack)
            Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        }

        SectionLabel(stringResource(R.string.settings_section_general))
        SettingsCard {
            if (AppLanguages.isSupported) {
                ValueRow(stringResource(R.string.settings_language), languageLabel(language), onClick = { dialog = SettingsDialog.LANGUAGE })
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            SegmentRow(
                label = stringResource(R.string.settings_unit),
                options = WeightUnit.entries.map { stringResource(it.label()) },
                selectedIndex = training.unit.ordinal,
                onSelect = { viewModel.setUnit(WeightUnit.entries[it]) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SegmentRow(
                label = stringResource(R.string.settings_theme),
                options = ThemeMode.entries.map { stringResource(it.label()) },
                selectedIndex = state.themeMode.ordinal,
                onSelect = { viewModel.setThemeMode(ThemeMode.entries[it]) },
            )
        }

        SectionLabel(stringResource(R.string.settings_section_training))
        SettingsCard {
            ValueRow(stringResource(R.string.settings_weekly_goal), weeklyGoalLabel(training.weeklyGoal), onClick = { dialog = SettingsDialog.WEEKLY_GOAL })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueRow(stringResource(R.string.settings_rest), formatRest(training.restSeconds), onClick = { dialog = SettingsDialog.REST })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueRow(stringResource(R.string.settings_steps), stepsLabel(training), onClick = { dialog = SettingsDialog.STEPS })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SegmentRow(
                label = stringResource(R.string.settings_rating),
                options = listOf(stringResource(R.string.workout_col_rir), stringResource(R.string.workout_col_rpe)),
                selectedIndex = training.rating.ordinal,
                onSelect = { viewModel.setRating(SetRating.entries[it]) },
            )
        }
        Hint(stringResource(R.string.settings_training_hint))

        SettingsCard {
            ValueRow(stringResource(R.string.settings_add_catalog), "", onClick = viewModel::addStandardExercises)
            state.catalogAdded?.let { added ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = pluralStringResource(R.plurals.settings_catalog_added, added, added),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(vertical = Spacing.s12)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
        Hint(stringResource(R.string.settings_add_catalog_hint))

        SectionLabel(stringResource(R.string.settings_section_data))
        BackupCard(
            state = state.backup,
            onExportJson = { exportJson.launch(fileName("backup", "json")) },
            onExportCsv = { exportCsv.launch(fileName("sets", "csv")) },
            onImport = { dialog = SettingsDialog.IMPORT },
        )
        Hint(stringResource(R.string.settings_backup_hint))
    }

    when (dialog) {
        SettingsDialog.LANGUAGE -> ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries.map { languageLabel(it) },
            selectedIndex = language.ordinal,
            onSelect = { index ->
                dialog = null
                if (AppLanguages.isSupported) AppLanguages.set(context, AppLanguage.entries[index])
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.REST -> ChoiceDialog(
            title = stringResource(R.string.settings_rest),
            options = TrainingSettings.REST_CHOICES.map { formatRest(it) },
            selectedIndex = TrainingSettings.REST_CHOICES.indexOf(training.restSeconds),
            onSelect = { index ->
                dialog = null
                viewModel.setRestSeconds(TrainingSettings.REST_CHOICES[index])
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.WEEKLY_GOAL -> {
            // First option "no goal", then 1 to 7 sessions.
            val goals = listOf<Int?>(null) + TrainingSettings.WEEKLY_GOAL_CHOICES.toList()
            ChoiceDialog(
                title = stringResource(R.string.settings_weekly_goal),
                options = goals.map { weeklyGoalLabel(it) },
                selectedIndex = goals.indexOf(training.weeklyGoal),
                onSelect = { index ->
                    dialog = null
                    viewModel.setWeeklyGoal(goals[index])
                },
                onDismiss = { dialog = null },
            )
        }
        SettingsDialog.STEPS -> StepsDialog(
            training = training,
            onConfirm = { barbell, dumbbell -> if (viewModel.setSteps(barbell, dumbbell)) dialog = null },
            onDismiss = { dialog = null },
        )
        SettingsDialog.IMPORT -> ImportDialog(
            onConfirm = {
                dialog = null
                importJson.launch(IMPORT_TYPES)
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private const val MIME_JSON = "application/json"
private const val MIME_CSV = "text/csv"

/** Some file managers don't know .json and report it as binary or text. */
private val IMPORT_TYPES = arrayOf(MIME_JSON, "application/octet-stream", "text/plain")

/** "aximo-backup-2026-10-04.json". */
private fun fileName(kind: String, extension: String): String = "aximo-$kind-${LocalDate.now()}.$extension"

@Composable
private fun BackupCard(state: BackupState, onExportJson: () -> Unit, onExportCsv: () -> Unit, onImport: () -> Unit) {
    SettingsCard {
        // Three rows instead of the mockup's button row: the labels fit without wrapping.
        ValueRow(stringResource(R.string.settings_export_json), "", onClick = onExportJson)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ValueRow(stringResource(R.string.settings_export_csv), "", onClick = onExportCsv)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ValueRow(stringResource(R.string.settings_import), "", onClick = onImport)
        val message = when {
            state.busy -> stringResource(R.string.settings_backup_busy)
            state.message != null -> stringResource(state.message.text())
            else -> null
        }
        if (message != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (state.message?.isError() == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(vertical = Spacing.s12)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

private fun BackupMessage.text(): Int = when (this) {
    BackupMessage.EXPORTED -> R.string.settings_backup_exported
    BackupMessage.CSV_EXPORTED -> R.string.settings_backup_csv_exported
    BackupMessage.RESTORED -> R.string.settings_backup_restored
    BackupMessage.INVALID_FILE -> R.string.settings_backup_invalid
    BackupMessage.NEWER_VERSION -> R.string.settings_backup_newer
    BackupMessage.FAILED -> R.string.settings_backup_failed
}

private fun BackupMessage.isError(): Boolean =
    this == BackupMessage.INVALID_FILE || this == BackupMessage.NEWER_VERSION || this == BackupMessage.FAILED

/** Import replaces everything, so it is confirmed before the file is picked. */
@Composable
private fun ImportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.settings_import_title), style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = stringResource(R.string.settings_import_body), style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { InverseButton(text = stringResource(R.string.settings_import_confirm), onClick = onConfirm) },
        dismissButton = { CancelButton(onDismiss) },
    )
}

@Composable
private fun Hint(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun weeklyGoalLabel(goal: Int?): String =
    if (goal == null) stringResource(R.string.settings_weekly_goal_none) else pluralStringResource(R.plurals.today_week_sessions, goal, goal)

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.GERMAN -> stringResource(R.string.settings_language_german)
    AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
}

private fun ThemeMode.label(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

/** "2,5 kg · DB 2 kg". */
@Composable
private fun stepsLabel(training: TrainingSettings): String = stringResource(
    R.string.settings_steps_value,
    formatWeight(training.steps.barbellKg, training.unit),
    formatWeight(training.steps.dumbbellKg, training.unit),
    stringResource(training.unit.label()),
)

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.s8),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.s16, vertical = Spacing.s4)) {
            content()
        }
    }
}

/** Label on the left, current value and a chevron on the right; opens a dialog. */
@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.cta)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.s12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconSmall),
        )
    }
}

@Composable
private fun SegmentRow(label: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = Spacing.s12),
        verticalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        SegmentedControl(options = options, selectedIndex = selectedIndex, onSelect = onSelect)
    }
}

/** Single choice from a short list; a tap applies and closes. */
@Composable
private fun ChoiceDialog(title: String, options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Sizes.touch)
                            .selectable(selected = index == selectedIndex, role = Role.RadioButton, onClick = { onSelect(index) }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
                    ) {
                        RadioButton(selected = index == selectedIndex, onClick = null)
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { CancelButton(onDismiss) },
    )
}

/** Default steps for barbell (and everything else) and dumbbells, in the display unit. */
@Composable
private fun StepsDialog(training: TrainingSettings, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var barbell by remember { mutableStateOf(formatWeight(training.steps.barbellKg, training.unit)) }
    var dumbbell by remember { mutableStateOf(formatWeight(training.steps.dumbbellKg, training.unit)) }
    val unitLabel = stringResource(training.unit.label())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.settings_steps), style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                LabeledTextField(
                    label = stringResource(R.string.settings_steps_barbell, unitLabel),
                    value = barbell,
                    onValueChange = { barbell = it },
                    keyboardType = KeyboardType.Decimal,
                )
                LabeledTextField(
                    label = stringResource(R.string.settings_steps_dumbbell, unitLabel),
                    value = dumbbell,
                    onValueChange = { dumbbell = it },
                    keyboardType = KeyboardType.Decimal,
                )
            }
        },
        confirmButton = { InverseButton(text = stringResource(R.string.workout_apply), onClick = { onConfirm(barbell, dumbbell) }) },
        dismissButton = { CancelButton(onDismiss) },
    )
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = stringResource(R.string.workout_cancel), style = MaterialTheme.typography.labelLarge)
    }
}
