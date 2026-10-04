package io.github.wtfjb.aximo.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.CircleIconButton
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.SearchField
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import org.koin.androidx.compose.koinViewModel

/** Exercise library (B-06): search, filter by region, details with instructions, add to the own list. */
@Composable
fun CatalogScreen(
    onBack: () -> Unit,
    viewModel: CatalogViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(start = Spacing.s16, end = Spacing.s16, top = Spacing.s14, bottom = Spacing.s8),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                CircleIconButton(AppIcons.Back, stringResource(R.string.action_back), onBack)
                Text(text = stringResource(R.string.catalog_title), style = MaterialTheme.typography.headlineSmall)
            }
            SearchField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.catalog_search),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.s16, vertical = Spacing.s4),
            horizontalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            item { ChoiceChip(stringResource(R.string.exercises_filter_all), state.region == null, { viewModel.onRegionSelect(null) }) }
            items(BodyRegion.entries) { r ->
                ChoiceChip(stringResource(r.label()), state.region == r, { viewModel.onRegionSelect(r) })
            }
        }
        if (state.entries.isEmpty() && !state.loading) {
            Text(
                text = stringResource(R.string.exercises_no_results),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(Spacing.s20),
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = Spacing.s16, end = Spacing.s16, top = Spacing.s8, bottom = Spacing.s24),
            verticalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            if (state.entries.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.catalog_english_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.s4),
                    )
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                CatalogRow(entry, added = entry.catalogId in state.addedIds, onClick = { viewModel.open(entry) })
            }
        }
    }

    state.selected?.let { entry ->
        val added = entry.catalogId in state.addedIds
        AlertDialog(
            onDismissRequest = viewModel::close,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = entry.name, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                    Text(
                        text = entryMeta(entry),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Instructions(entry.instructions)
                }
            },
            confirmButton = {
                if (added) {
                    TextButton(onClick = viewModel::close) {
                        Text(text = stringResource(R.string.catalog_in_list), style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    InverseButton(text = stringResource(R.string.catalog_add), onClick = { viewModel.add(entry) })
                }
            },
            dismissButton = {
                if (!added) {
                    TextButton(onClick = viewModel::close) {
                        Text(text = stringResource(R.string.catalog_close), style = MaterialTheme.typography.labelLarge)
                    }
                }
            },
        )
    }
}

@Composable
private fun CatalogRow(entry: CatalogEntry, added: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.s14, end = Spacing.s12, top = Spacing.s10, bottom = Spacing.s10),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(text = entry.name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = entryMeta(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (added) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = stringResource(R.string.catalog_in_list),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.iconSmall),
                )
            }
        }
    }
}

/** "Brust, Trizeps · Langhantel" */
@Composable
private fun entryMeta(entry: CatalogEntry): String {
    val muscles = entry.primary.sortedBy { it.ordinal }.map { stringResource(it.label()) }.joinToString(", ")
    return "$muscles · ${stringResource(entry.equipment.label())}"
}

/** The numbered steps of an exercise; also used on the exercise detail. */
@Composable
fun Instructions(steps: List<String>) {
    if (steps.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
        Text(text = stringResource(R.string.catalog_instructions), style = MaterialTheme.typography.titleSmall)
        steps.forEachIndexed { index, step ->
            Text(text = stringResource(R.string.catalog_step, index + 1, step), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
