package io.github.wtfjb.aximo.ui.format

import androidx.compose.runtime.compositionLocalOf
import io.github.wtfjb.aximo.domain.workout.SetRating

/** Scale for set effort from the settings (RIR or RPE, A-02), provided once in MainActivity. */
val LocalSetRating = compositionLocalOf { SetRating.RIR }
