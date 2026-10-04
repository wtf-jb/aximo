package io.github.wtfjb.aximo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.SetEntry
import io.github.wtfjb.aximo.domain.model.SetType
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.ui.format.formatSetValue
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing

/** A logged set in a history ("82,5 × 8 · R2"); warm-ups in accent-container with "W". */
@Composable
fun SetChip(set: SetEntry, bodyweight: Boolean, unit: WeightUnit, modifier: Modifier = Modifier) {
    val warmUp = set.setType == SetType.WARM_UP
    var text = formatSetValue(set.weightKg, set.reps, bodyweight, unit)
    text = when (set.setType) {
        SetType.WARM_UP -> "${stringResource(R.string.workout_set_badge_warmup)} $text"
        SetType.DROP -> "${stringResource(R.string.workout_set_badge_drop)} $text"
        SetType.FAILURE -> "${stringResource(R.string.workout_set_badge_failure)} $text"
        SetType.WORKING -> text
    }
    set.rir?.let { text = stringResource(R.string.exercise_detail_set_rir, text, it) }
    Surface(
        shape = Radii.sm,
        color = if (warmUp) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (warmUp) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = Sizes.setChip)
                .padding(horizontal = Spacing.s10),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = MaterialTheme.typography.labelLarge.fontWeight))
        }
    }
}
