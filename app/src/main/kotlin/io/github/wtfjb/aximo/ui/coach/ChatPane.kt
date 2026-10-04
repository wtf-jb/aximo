package io.github.wtfjb.aximo.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.chat.ChatRole
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.MessageField
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.settings.CancelButton
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import androidx.compose.material3.AlertDialog

/** Bubbles take at most this share of the width, so the sides stay readable. */
private const val BUBBLE_WIDTH = 0.85f

/**
 * Coach chat (B-05): conversation with suggestion cards inline, input at the
 * bottom. No mockup; built from the review's cards and the app's tokens.
 */
@Composable
internal fun ChatPane(state: ChatUiState, viewModel: ChatViewModel, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    // New message or "Coach denkt …": scroll to the end.
    LaunchedEffect(state.entries.size, state.pending, state.error) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            if (state.isEmpty && !state.loading) {
                item { Intro(onExample = viewModel::setDraft) }
            }
            items(state.entries, key = { it.message.id }) { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                    Bubble(entry.message.text, entry.message.role)
                    entry.items.forEach { item ->
                        SuggestionCard(
                            item,
                            onApply = { viewModel.apply(item.suggestion.id) },
                            onDiscard = { viewModel.discard(item.suggestion.id) },
                        )
                    }
                }
            }
            state.pending?.let { question ->
                item { Bubble(question, ChatRole.USER) }
                item { ThinkingRow() }
            }
            if (state.error != null) {
                item { ErrorText(state.error) }
            }
        }
        Composer(state, viewModel)
    }

    if (state.showNotice) {
        NoticeDialog(
            body = stringResource(R.string.chat_notice_body),
            onAccept = viewModel::acceptNotice,
            onShowData = viewModel::showSentData,
            onDismiss = viewModel::dismissNotice,
        )
    }
    state.sentData?.let { SentDataDialog(it, viewModel::dismissSentData) }
    if (state.confirmClear) {
        AlertDialog(
            onDismissRequest = viewModel::dismissClear,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = stringResource(R.string.chat_clear_title), style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = stringResource(R.string.chat_clear_body), style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { InverseButton(text = stringResource(R.string.chat_clear_confirm), onClick = viewModel::confirmClear) },
            dismissButton = { CancelButton(viewModel::dismissClear) },
        )
    }
}

@Composable
private fun Intro(onExample: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        Text(
            text = stringResource(R.string.chat_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s8), verticalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            listOf(R.string.chat_example_stall, R.string.chat_example_volume, R.string.chat_example_routines).forEach { res ->
                val text = stringResource(res)
                ChoiceChip(text = text, selected = false, onClick = { onExample(text) })
            }
        }
    }
}

/** Question on the right in the inverse color, answer on the left on the surface. */
@Composable
private fun Bubble(text: String, role: ChatRole) {
    val user = role == ChatRole.USER
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (user) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(modifier = Modifier.fillMaxWidth(BUBBLE_WIDTH), contentAlignment = if (user) Alignment.CenterEnd else Alignment.CenterStart) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = if (user) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface,
                contentColor = if (user) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.s14, vertical = Spacing.s10),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s4),
                ) {
                    if (!user) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
                            Icon(
                                AppIcons.Sparkle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(Sizes.iconSmall),
                            )
                            Text(
                                text = stringResource(R.string.nav_coach),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    SelectionContainer {
                        Text(text = text, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(state: ChatUiState, viewModel: ChatViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.s8, bottom = Spacing.s8),
        verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            MessageField(
                value = state.draft,
                onValueChange = viewModel::setDraft,
                placeholder = stringResource(R.string.chat_placeholder),
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = viewModel::send,
                enabled = state.canSend,
                shape = Radii.full,
                color = if (state.canSend) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.extendedColors.track,
                contentColor = if (state.canSend) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.input),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Send, contentDescription = stringResource(R.string.chat_send), modifier = Modifier.size(Sizes.iconSmall))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.chat_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = viewModel::showSentData) {
                Text(text = stringResource(R.string.coach_sent_data_short), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
