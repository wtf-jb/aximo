package io.github.wtfjb.aximo.ui.coach

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.review.AiReview
import io.github.wtfjb.aximo.domain.review.ReviewException
import io.github.wtfjb.aximo.domain.review.SuggestionChange
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import io.github.wtfjb.aximo.ui.ai.label
import io.github.wtfjb.aximo.ui.components.InverseButton
import io.github.wtfjb.aximo.ui.components.PillTextButton
import io.github.wtfjb.aximo.ui.components.PrimaryButton
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.format.formatDayMonth
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.screens.TabScreen
import io.github.wtfjb.aximo.ui.settings.CancelButton
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import org.koin.androidx.compose.koinViewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Coach tab: weekly review (B-02, mockup Review.html) and chat (B-05), switched
 * with a segmented control. The review is the default, so "Wochen-Review bereit"
 * on Heute lands on it.
 */
@Composable
fun CoachScreen(
    viewModel: CoachViewModel = koinViewModel(),
    chatViewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val review = state.review
    var tab by rememberSaveable { mutableIntStateOf(TAB_REVIEW) }

    TabScreen(
        title = stringResource(R.string.nav_coach),
        overline = if (tab == TAB_REVIEW) review?.let { weekLabel(it) } else null,
        action = if (tab == TAB_CHAT && !chatState.isEmpty) {
            { PillTextButton(text = stringResource(R.string.chat_new), onClick = chatViewModel::requestClear) }
        } else {
            null
        },
    ) {
        SegmentedControl(
            options = listOf(stringResource(R.string.coach_title), stringResource(R.string.chat_tab)),
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        if (tab == TAB_REVIEW) {
            ReviewPane(state, viewModel, Modifier.weight(1f))
        } else {
            ChatPane(chatState, chatViewModel, Modifier.weight(1f))
        }
    }

    if (state.showNotice) {
        NoticeDialog(
            body = stringResource(R.string.coach_notice_body),
            onAccept = viewModel::acceptNotice,
            onShowData = viewModel::showSentData,
            onDismiss = viewModel::dismissNotice,
        )
    }
    state.sentData?.let { SentDataDialog(it, viewModel::dismissSentData) }
}

private const val TAB_REVIEW = 0
private const val TAB_CHAT = 1

@Composable
private fun ReviewPane(state: CoachUiState, viewModel: CoachViewModel, modifier: Modifier = Modifier) {
    val review = state.review
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.s18),
    ) {
        if (review == null && !state.loading) {
            Text(
                text = stringResource(R.string.coach_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (review != null) {
            SummaryCard(review)
            if (state.items.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pluralStringResource(R.plurals.coach_suggestions, state.items.size, state.items.size),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.openApplicable.size > 1) {
                        TextButton(onClick = viewModel::applyAll) {
                            Text(
                                text = stringResource(R.string.coach_apply_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.coach_no_suggestions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.items.forEach { item ->
                SuggestionCard(item, onApply = { viewModel.apply(item.suggestion.id) }, onDiscard = { viewModel.discard(item.suggestion.id) })
            }
        }

        ErrorText(state.error)
        if (state.running) {
            ThinkingRow()
        } else {
            PrimaryButton(
                text = stringResource(if (review == null) R.string.coach_create else R.string.coach_create_again),
                onClick = viewModel::requestReview,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextButton(onClick = viewModel::showSentData, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(text = stringResource(R.string.coach_sent_data), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(modifier = Modifier.height(Spacing.s16))
    }
}

/** Spinner with "Coach denkt …". */
@Composable
internal fun ThinkingRow() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
        CircularProgressIndicator(modifier = Modifier.size(Sizes.icon), strokeWidth = Sizes.borderActive)
        Text(
            text = stringResource(R.string.coach_running),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** What is sent and why, before the first AI call. */
@Composable
internal fun NoticeDialog(body: String, onAccept: () -> Unit, onShowData: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.coach_notice_title), style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(text = body, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.verticalScroll(rememberScrollState()))
        },
        confirmButton = { InverseButton(text = stringResource(R.string.coach_notice_confirm), onClick = onAccept) },
        dismissButton = {
            Row {
                TextButton(onClick = onShowData) {
                    Text(text = stringResource(R.string.coach_sent_data_short), style = MaterialTheme.typography.labelLarge)
                }
                CancelButton(onDismiss)
            }
        },
    )
}

/** "Gesendete Daten ansehen": the JSON exactly as sent, selectable. */
@Composable
internal fun SentDataDialog(data: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(text = stringResource(R.string.coach_sent_data), style = MaterialTheme.typography.titleMedium) },
        text = {
            SelectionContainer(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = data, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.coach_close), style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

/** "KW 40 · 28. Sep. – 4. Okt." for the week the review was made in. */
@Composable
private fun weekLabel(review: AiReview): String {
    val date = review.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val start = date.plus(-(date.dayOfWeek.ordinal), DateTimeUnit.DAY)
    val end = start.plus(6, DateTimeUnit.DAY)
    val week = java.time.LocalDate.of(date.year, date.month.ordinal + 1, date.day).get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    return stringResource(R.string.coach_week, week, formatDayMonth(start), formatDayMonth(end))
}

@Composable
private fun SummaryCard(review: AiReview) {
    Surface(
        shape = Radii.xxl,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.s18), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Icon(AppIcons.Sparkle, contentDescription = null, tint = MaterialTheme.extendedColors.onInverseAccentContainer, modifier = Modifier.size(Sizes.iconSmall))
                Text(
                    text = stringResource(R.string.coach_summary),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.extendedColors.onInverseAccentContainer,
                )
            }
            Text(text = review.summary, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pluralStringResource(R.plurals.coach_basis, review.weeks, review.weeks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extendedColors.onInverseMuted,
            )
        }
    }
}

@Composable
internal fun SuggestionCard(item: SuggestionItem, onApply: () -> Unit, onDiscard: () -> Unit) {
    val change = item.suggestion.change
    val status = item.suggestion.status
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s16), verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                Surface(shape = Radii.full, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Text(
                        text = stringResource(change.category()),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = Spacing.s8, vertical = Spacing.s4),
                    )
                }
                Text(
                    text = item.routineName?.let { stringResource(R.string.coach_routine, it) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusLabel(status)
            }
            Text(text = stringResource(change.title()), style = MaterialTheme.typography.titleLarge)
            DiffBox(change, item.exerciseName ?: "–")
            Text(text = item.suggestion.rationale, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (status == SuggestionStatus.OPEN) {
                if (!item.applicable) {
                    Text(
                        text = stringResource(R.string.coach_not_applicable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                    OutlinedButton(
                        onClick = onDiscard,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(Sizes.borderControl, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f).heightIn(min = Sizes.input),
                    ) {
                        Text(text = stringResource(R.string.coach_discard), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (item.applicable) {
                        Button(
                            onClick = onApply,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            ),
                            modifier = Modifier.weight(1f).heightIn(min = Sizes.input),
                        ) {
                            Text(text = stringResource(R.string.coach_apply), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLabel(status: SuggestionStatus) {
    val text = when (status) {
        SuggestionStatus.OPEN -> return
        SuggestionStatus.APPLIED -> R.string.coach_status_applied
        SuggestionStatus.DISCARDED -> R.string.coach_status_discarded
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
        if (status == SuggestionStatus.APPLIED) {
            Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
        }
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.labelLarge,
            color = if (status == SuggestionStatus.APPLIED) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Grey box with "what → what", as in the mockup. */
@Composable
private fun DiffBox(change: SuggestionChange, exercise: String) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = Spacing.s12, vertical = Spacing.s4)) {
            when (change) {
                is SuggestionChange.SetCount ->
                    DiffRow(stringResource(R.string.coach_diff_sets, exercise), change.from.toString(), change.to.toString())
                is SuggestionChange.RepRange ->
                    DiffRow(stringResource(R.string.coach_diff_reps, exercise), range(change.fromMin, change.fromMax), range(change.toMin, change.toMax))
                is SuggestionChange.TargetRir ->
                    DiffRow(stringResource(R.string.coach_diff_rir, exercise), change.from?.toString() ?: "–", change.to?.toString() ?: "–")
                is SuggestionChange.AddExercise ->
                    BadgeRow(exercise, stringResource(R.string.coach_badge_new), stringResource(R.string.coach_sets_reps, change.sets, range(change.repMin, change.repMax)))
                is SuggestionChange.RemoveExercise ->
                    BadgeRow(exercise, stringResource(R.string.coach_badge_removed), null)
            }
        }
    }
}

private fun range(min: Int, max: Int): String = if (min == max) "$min" else "$min–$max"

@Composable
private fun DiffRow(label: String, from: String, to: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s10),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            text = from,
            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(AppIcons.ArrowRight, contentDescription = stringResource(R.string.coach_diff_to), modifier = Modifier.size(Sizes.iconSmall))
        Text(text = to, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun BadgeRow(label: String, badge: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s10),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
            Text(text = badge, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = Spacing.s6, vertical = Spacing.s4))
        }
        if (value != null) Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
internal fun ErrorText(error: CoachError?) {
    val text = when (error) {
        null -> return
        is CoachError.Ai -> {
            val reason = stringResource(error.reason.label())
            val headline = error.statusCode?.let { stringResource(R.string.ai_error_with_status, reason, it) } ?: reason
            headline + error.detail?.let { "\n$it" }.orEmpty()
        }
        is CoachError.Review -> stringResource(
            when (error.reason) {
                ReviewException.Reason.NO_PROFILE -> R.string.coach_error_no_profile
                ReviewException.Reason.NO_DATA -> R.string.coach_error_no_data
            },
        )
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

private fun SuggestionChange.category(): Int = when (this) {
    is SuggestionChange.SetCount, is SuggestionChange.AddExercise, is SuggestionChange.RemoveExercise -> R.string.coach_category_volume
    is SuggestionChange.RepRange, is SuggestionChange.TargetRir -> R.string.coach_category_exercise
}

private fun SuggestionChange.title(): Int = when (this) {
    is SuggestionChange.SetCount -> if (to > from) R.string.coach_title_more_sets else R.string.coach_title_fewer_sets
    is SuggestionChange.RepRange -> R.string.coach_title_reps
    is SuggestionChange.TargetRir -> R.string.coach_title_rir
    is SuggestionChange.AddExercise -> R.string.coach_title_add
    is SuggestionChange.RemoveExercise -> R.string.coach_title_remove
}
