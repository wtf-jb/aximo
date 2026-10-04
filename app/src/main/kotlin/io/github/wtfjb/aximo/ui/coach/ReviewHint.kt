package io.github.wtfjb.aximo.ui.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.review.AiReviewRepository
import io.github.wtfjb.aximo.domain.review.SuggestionStatus
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.androidx.compose.koinViewModel

/** Open suggestions of the latest review, 0 = no card. Without an AI profile always 0. */
class ReviewHintViewModel(reviews: AiReviewRepository, profiles: AiProfileRepository) : ViewModel() {
    val openSuggestions: StateFlow<Int> = combine(reviews.observeLatest(), profiles.observeProfiles()) { review, list ->
        if (!AiProfiles.isAvailable(list)) 0 else review?.suggestions?.count { it.status == SuggestionStatus.OPEN } ?: 0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

/** Card "Wochen-Review bereit" on Heute (mockup Heute.html); opens the Coach tab. */
@Composable
fun ReviewHintCard(onOpen: () -> Unit, viewModel: ReviewHintViewModel = koinViewModel()) {
    val open by viewModel.openSuggestions.collectAsStateWithLifecycle()
    if (open == 0) return
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.s16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s14),
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(Sizes.listTile),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Coach, contentDescription = null, modifier = Modifier.size(Sizes.icon))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.today_review_ready), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = pluralStringResource(R.plurals.today_review_open, open, open),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.iconSmall),
            )
        }
    }
}
