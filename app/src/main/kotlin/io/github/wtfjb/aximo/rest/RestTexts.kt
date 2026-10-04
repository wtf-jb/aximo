package io.github.wtfjb.aximo.rest

import android.content.res.Resources
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.rest.NextSet

/** "Satz 3 · Bankdrücken", "Warm-up · Bankdrücken" or "Alle Sätze erledigt". Used by the bar and the notification. */
fun nextSetText(resources: Resources, next: NextSet?): String = when {
    next == null -> resources.getString(R.string.rest_next_none)
    next.setNumber == 0 -> resources.getString(R.string.rest_next_warmup, next.exerciseName)
    else -> resources.getString(R.string.rest_next_set, next.setNumber, next.exerciseName)
}
