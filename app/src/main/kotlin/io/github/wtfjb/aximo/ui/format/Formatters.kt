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

/** Seconds as "2:30". */
fun formatRest(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

/** Short weekday and date in the device locale, e.g. "Do., 1. Okt." / "Thu, Oct 1". */
fun formatShortDate(instant: kotlin.time.Instant, locale: Locale = Locale.getDefault()): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM")
    val date = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(java.time.ZoneId.systemDefault())
    return java.time.format.DateTimeFormatter.ofPattern(pattern, locale).format(date)
}

/** Time of day in the device format, e.g. "09:02" / "9:02 AM". */
fun formatTime(instant: kotlin.time.Instant, locale: Locale = Locale.getDefault()): String {
    val time = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()).atZone(java.time.ZoneId.systemDefault())
    return java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT).withLocale(locale).format(time)
}

/** Whole number with the locale's grouping, e.g. 9420 → "9.420" in German. */
fun formatInteger(value: Double, locale: Locale = Locale.getDefault()): String =
    java.text.NumberFormat.getIntegerInstance(locale).format(Math.round(value))
