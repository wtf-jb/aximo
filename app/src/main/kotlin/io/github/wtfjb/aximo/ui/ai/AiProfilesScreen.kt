package io.github.wtfjb.aximo.ui.ai

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.SecondaryButton
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.settings.Hint
import io.github.wtfjb.aximo.ui.settings.SettingsCard
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/** All provider profiles (B-01); the radio button picks the active one. */
@Composable
fun AiProfilesScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: AiProfilesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.s16),
        verticalArrangement = Arrangement.spacedBy(Spacing.s12),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack)
            Text(text = stringResource(R.string.ai_profiles_title), style = MaterialTheme.typography.headlineSmall)
        }

        if (!state.loading && state.profiles.isEmpty()) {
            Hint(stringResource(R.string.ai_profiles_empty))
        }
        if (state.profiles.isNotEmpty()) {
            SettingsCard {
                state.profiles.forEachIndexed { index, profile ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ProfileRow(
                        profile = profile,
                        active = profile.id == state.activeId,
                        onSelect = { viewModel.setActive(profile.id) },
                        onEdit = { onEdit(profile.id) },
                    )
                }
            }
        }
        SecondaryButton(text = stringResource(R.string.ai_profiles_add), onClick = { onEdit(0L) }, modifier = Modifier.fillMaxWidth())
        Hint(stringResource(R.string.ai_profiles_hint))
    }
}

@Composable
private fun ProfileRow(profile: AiProviderProfile, active: Boolean, onSelect: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.cta),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Radio on the left selects; the rest of the row opens the form.
        Row(
            modifier = Modifier
                .heightIn(min = Sizes.touch)
                .selectable(selected = active, role = Role.RadioButton, onClick = onSelect)
                .padding(end = Spacing.s8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = active, onClick = null)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(vertical = Spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.ai_profile_summary, stringResource(profile.kind.label()), profile.model),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = stringResource(R.string.ai_profile_edit_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconSmall),
            )
        }
    }
}
