package io.github.wtfjb.aximo.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.workout.WorkoutLogic
import io.github.wtfjb.aximo.rest.nextSetText
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.AppTextStyles
import io.github.wtfjb.aximo.ui.theme.Elevation
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors

/**
 * Floating rest timer at the bottom of the workout screen (A-03, design system
 * "RestTimer"): countdown, what comes next, "+15 s" and skip. The progress runs
 * from full to empty.
 */
@Composable
fun RestTimerBar(rest: RestUi, onExtend: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val muted = MaterialTheme.extendedColors.onInverseMuted
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.xxl,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = Elevation.float,
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.s18, end = Spacing.s14, top = Spacing.s14, bottom = Spacing.s14),
            verticalArrangement = Arrangement.spacedBy(Spacing.s12),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                val countdown = WorkoutLogic.formatElapsed(rest.remainingSeconds)
                val title = stringResource(R.string.rest_title)
                // No live region: TalkBack would read the countdown every second.
                Column(modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$title $countdown" }) {
                    Text(text = title, style = MaterialTheme.typography.labelMedium, color = muted)
                    Text(text = countdown, style = AppTextStyles.restCountdown)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.rest_next_label), style = MaterialTheme.typography.bodySmall, color = muted)
                    Text(
                        text = nextSetText(LocalResources.current, rest.next),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val extendDescription = stringResource(R.string.rest_extend_description)
                Surface(
                    onClick = onExtend,
                    modifier = Modifier
                        .heightIn(min = Sizes.touch)
                        .semantics { contentDescription = extendDescription },
                    shape = Radii.full,
                    color = MaterialTheme.extendedColors.inverseRaised,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Box(modifier = Modifier.padding(horizontal = Spacing.s14), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.rest_extend), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Surface(
                    onClick = onSkip,
                    modifier = Modifier.size(Sizes.touch),
                    shape = Radii.full,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = AppIcons.SkipForward,
                            contentDescription = stringResource(R.string.rest_skip),
                            modifier = Modifier.size(Sizes.iconSmall),
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.restProgress)
                    .background(MaterialTheme.extendedColors.inverseRaised, Radii.full),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(rest.remainingFraction)
                        .height(Sizes.restProgress)
                        .background(MaterialTheme.colorScheme.primary, Radii.full),
                )
            }
        }
    }
}
