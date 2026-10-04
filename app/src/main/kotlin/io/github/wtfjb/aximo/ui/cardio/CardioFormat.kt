package io.github.wtfjb.aximo.ui.cardio

import java.util.Locale

/** Distance with the locale's decimal separator: 5200 m → "5,20" ([decimals] = 2) or "5,2" (1). */
fun formatKm(distanceM: Double, decimals: Int = 2, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%.${decimals}f", distanceM / 1000)

/** Speed with one decimal: 10.88 → "10,9". */
fun formatSpeed(kmh: Double, locale: Locale = Locale.getDefault()): String = String.format(locale, "%.1f", kmh)
