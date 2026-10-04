package io.github.wtfjb.aximo.ui.workout

import androidx.annotation.StringRes
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.model.SetType

@StringRes
fun SetType.label(): Int = when (this) {
    SetType.WARM_UP -> R.string.workout_set_type_warmup
    SetType.WORKING -> R.string.workout_set_type_working
    SetType.DROP -> R.string.workout_set_type_drop
    SetType.FAILURE -> R.string.workout_set_type_failure
}
