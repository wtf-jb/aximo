package io.github.wtfjb.aximo.ui.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.settings.ThemeMode
import io.github.wtfjb.aximo.domain.settings.TrainingSettings
import io.github.wtfjb.aximo.domain.units.WeightUnit
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

/** Which dialog is open. */
private enum class SettingsDialog { LANGUAGE, REST, STEPS }

/**
 * Settings (A-09, mockup Einstellungen.html). AI coach (B) and Health Connect (C)
 * are left out; export and import follow with A-08.
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
            ValueRow(stringResource(R.string.settings_rest), formatRest(training.restSeconds), onClick = { dialog = SettingsDialog.REST })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueRow(stringResource(R.string.settings_steps), stepsLabel(training), onClick = { dialog = SettingsDialog.STEPS })
        }
        Text(
            text = stringResource(R.string.settings_training_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        SettingsDialog.STEPS -> StepsDialog(
            training = training,
            onConfirm = { barbell, dumbbell -> if (viewModel.setSteps(barbell, dumbbell)) dialog = null },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

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
