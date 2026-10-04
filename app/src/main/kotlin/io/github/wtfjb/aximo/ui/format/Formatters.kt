package io.github.wtfjb.aximo.ui.format

import io.github.wtfjb.aximo.domain.exercise.ExerciseDraft
import io.github.wtfjb.aximo.domain.units.WeightUnit
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Weight in the display unit with the locale's decimal separator: 82.5 kg → "82,5" in German. */
fun formatWeight(kg: Double, unit: WeightUnit, locale: Locale = Locale.getDefault()): String =
    localizeDecimal(ExerciseDraft.formatNumber(unit.fromKg(kg)), locale)

/** Replaces the "." of a plain number with the locale's decimal separator. */
fun localizeDecimal(plain: String, locale: Locale = Locale.getDefault()): String =
    plain.replace('.', DecimalFormatSymbols.getInstance(locale).decimalSeparator)
