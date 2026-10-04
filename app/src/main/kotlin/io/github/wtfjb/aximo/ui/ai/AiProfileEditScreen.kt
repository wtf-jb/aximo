package io.github.wtfjb.aximo.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.ai.AiProfileFieldError
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderKind
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.LabeledTextField
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.settings.CancelButton
import io.github.wtfjb.aximo.ui.settings.Hint
import io.github.wtfjb.aximo.ui.settings.SettingsCard
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** Form for one provider profile (B-01): name, kind, URL, model, key, connection test. */
@Composable
fun AiProfileEditScreen(
    profileId: Long,
    onDone: () -> Unit,
    viewModel: AiProfileEditViewModel = koinViewModel { parametersOf(profileId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = state.draft
    val errors = state.errors
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircleIconButton(AppIcons.Close, stringResource(R.string.action_back), onDone)
            Text(
                text = stringResource(if (draft.isNew) R.string.ai_profile_new_title else R.string.ai_profile_edit_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (!draft.isNew && !state.loading) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(
                        text = stringResource(R.string.ai_profile_delete),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        SettingsCard {
            Column(
                modifier = Modifier.padding(vertical = Spacing.s12),
                verticalArrangement = Arrangement.spacedBy(Spacing.s12),
            ) {
                LabeledTextField(
                    label = stringResource(R.string.ai_profile_name),
                    value = draft.name,
                    onValueChange = viewModel::setName,
                    placeholder = stringResource(R.string.ai_profile_name_placeholder),
                    error = stringResource(R.string.ai_profile_error_name).takeIf { AiProfileFieldError.NAME_MISSING in errors },
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                    Text(
                        text = stringResource(R.string.ai_profile_kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SegmentedControl(
                        options = AiProviderKind.entries.map { stringResource(it.label()) },
                        selectedIndex = draft.kind.ordinal,
                        onSelect = { viewModel.setKind(AiProviderKind.entries[it]) },
                    )
                }
                LabeledTextField(
                    label = stringResource(R.string.ai_profile_url),
                    value = draft.baseUrl,
                    onValueChange = viewModel::setBaseUrl,
                    keyboardType = KeyboardType.Uri,
                    placeholder = stringResource(
                        if (draft.kind == AiProviderKind.ANTHROPIC) R.string.ai_profile_url_placeholder_anthropic else R.string.ai_profile_url_placeholder_openai,
                    ),
                    error = stringResource(R.string.ai_profile_error_url).takeIf { AiProfileFieldError.URL_INVALID in errors },
                )
                ModelField(
                    value = draft.model,
                    onValueChange = viewModel::setModel,
                    models = state.models,
                    onReload = viewModel::reloadModels,
                    placeholder = if (draft.kind == AiProviderKind.ANTHROPIC) {
                        stringResource(R.string.ai_profile_model_placeholder_anthropic, AiProfiles.ANTHROPIC_MODEL_EXAMPLE)
                    } else {
                        stringResource(R.string.ai_profile_model_placeholder_openai)
                    },
                    error = stringResource(R.string.ai_profile_error_model).takeIf { AiProfileFieldError.MODEL_MISSING in errors },
                )
                LabeledTextField(
                    label = stringResource(R.string.ai_profile_key),
                    value = draft.apiKey,
                    onValueChange = viewModel::setApiKey,
                    secret = true,
                    placeholder = stringResource(
                        when {
                            draft.hasStoredKey && !draft.removeKey -> R.string.ai_profile_key_stored
                            draft.kind == AiProviderKind.OPENAI_COMPATIBLE -> R.string.ai_profile_key_optional
                            else -> R.string.ai_profile_key_placeholder
                        },
                    ),
                    error = stringResource(R.string.ai_profile_error_key).takeIf { AiProfileFieldError.KEY_MISSING in errors },
                )
                if (draft.hasStoredKey) {
                    TextButton(onClick = viewModel::toggleRemoveKey) {
                        Text(
                            text = stringResource(if (draft.removeKey) R.string.ai_profile_key_keep else R.string.ai_profile_key_remove),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
        if (draft.isCleartext) {
            Text(
                text = stringResource(R.string.ai_profile_cleartext),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        SecondaryButton(
            text = stringResource(R.string.ai_profile_test),
            onClick = viewModel::testConnection,
            modifier = Modifier.fillMaxWidth(),
        )
        TestResult(state.test)

        PrimaryButton(text = stringResource(R.string.ai_profile_save), onClick = viewModel::save, modifier = Modifier.fillMaxWidth())
        Hint(stringResource(R.string.ai_profile_hint))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.ai_profile_delete_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = stringResource(R.string.ai_profile_delete_body), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                InverseButton(text = stringResource(R.string.ai_profile_delete), onClick = {
                    confirmDelete = false
                    viewModel.delete()
                })
            },
            dismissButton = { CancelButton { confirmDelete = false } },
        )
    }
}

/**
 * Model as free text plus a dropdown with the models the provider reports.
 * The list loads by itself once URL (and for Anthropic a key) are filled in;
 * typing stays possible for providers without a model list.
 */
@Composable
private fun ModelField(
    value: String,
    onValueChange: (String) -> Unit,
    models: ModelListState,
    onReload: () -> Unit,
    placeholder: String,
    error: String?,
) {
    var expanded by remember { mutableStateOf(false) }
    val list = (models as? ModelListState.Loaded)?.models.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        Box {
            LabeledTextField(
                label = stringResource(R.string.ai_profile_model),
                value = value,
                onValueChange = onValueChange,
                keyboardType = KeyboardType.Uri, // no auto-capitalization, no spaces
                placeholder = placeholder,
                error = error,
                trailing = if (list.isNotEmpty()) {
                    {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                AppIcons.ChevronDown,
                                contentDescription = stringResource(R.string.ai_models_choose),
                                modifier = Modifier.size(Sizes.icon),
                            )
                        }
                    }
                } else {
                    null
                },
            )
            DropdownMenu(
                expanded = expanded && list.isNotEmpty(),
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                list.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(text = model, style = MaterialTheme.typography.bodyLarge) },
                        trailingIcon = if (model == value.trim()) {
                            { Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall)) }
                        } else {
                            null
                        },
                        onClick = {
                            onValueChange(model)
                            expanded = false
                        },
                    )
                }
            }
        }
        ModelListStatus(models, onReload)
    }
}

@Composable
private fun ModelListStatus(models: ModelListState, onReload: () -> Unit) {
    when (models) {
        ModelListState.Idle -> Unit
        ModelListState.Loading -> StatusText(stringResource(R.string.ai_models_loading), isError = false)
        is ModelListState.Loaded -> StatusText(
            if (models.models.isEmpty()) {
                stringResource(R.string.ai_models_none)
            } else {
                pluralStringResource(R.plurals.ai_models_found, models.models.size, models.models.size)
            },
            isError = false,
        )
        is ModelListState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
            val reason = stringResource(models.reason.label())
            val headline = models.statusCode?.let { stringResource(R.string.ai_error_with_status, reason, it) } ?: reason
            StatusText(stringResource(R.string.ai_models_failed, headline), isError = true, modifier = Modifier.weight(1f))
            TextButton(onClick = onReload) {
                Text(text = stringResource(R.string.ai_models_reload), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StatusText(text: String, isError: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

@Composable
private fun TestResult(test: ConnectionTestState) {
    val (text, isError) = when (test) {
        ConnectionTestState.Idle -> return
        ConnectionTestState.Running -> stringResource(R.string.ai_test_running) to false
        ConnectionTestState.Success -> stringResource(R.string.ai_test_success) to false
        is ConnectionTestState.Failed -> {
            val reason = stringResource(test.reason.label())
            val headline = test.statusCode?.let { stringResource(R.string.ai_error_with_status, reason, it) } ?: reason
            // The provider's own message (English, unformatted) helps find the cause.
            val detail = test.detail?.let { "\n$it" }.orEmpty()
            "$headline$detail" to true
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}
