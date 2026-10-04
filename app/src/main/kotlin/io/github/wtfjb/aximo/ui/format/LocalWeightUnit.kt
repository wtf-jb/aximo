package io.github.wtfjb.aximo.ui.format

import androidx.compose.runtime.compositionLocalOf
import io.github.wtfjb.aximo.domain.units.WeightUnit

/** Display unit for weights from the settings (A-09), provided once in MainActivity. */
val LocalWeightUnit = compositionLocalOf { WeightUnit.KG }
