package io.github.wtfjb.aximo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.wtfjb.aximo.R

// Both fonts are variable fonts (one file each). Weights are set with
// FontVariation instead of separate static files.

/**
 * Bricolage Grotesque for one text size. The font also has an optical-size
 * axis ("opsz"); browsers set it to the font size automatically, so we do the
 * same here to match the mockups.
 */
@OptIn(ExperimentalTextApi::class)
private fun displayFamily(weight: FontWeight, sizeSp: Int) = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.Setting("opsz", sizeSp.toFloat()),
        ),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: FontWeight) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Manrope in all weights the design uses. */
val Body = FontFamily(
    manrope(FontWeight.Medium),
    manrope(FontWeight.SemiBold),
    manrope(FontWeight.Bold),
    manrope(FontWeight.ExtraBold),
)

private fun displayStyle(size: Int, line: Float, weight: FontWeight, tracking: Float = 0f) =
    style(displayFamily(weight, size), size, line, weight, tracking)

private fun bodyStyle(size: Int, line: Float, weight: FontWeight, tracking: Float = 0f) =
    style(Body, size, line, weight, tracking)

private fun style(family: FontFamily, size: Int, line: Float, weight: FontWeight, tracking: Float) =
    TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = (size * line).sp,
        fontWeight = weight,
        letterSpacing = tracking.em,
        // Tabular numbers everywhere, so columns of numbers don't jump.
        fontFeatureSettings = "tnum",
    )

val AppTypography = Typography(
    displayLarge = displayStyle(44, 1.0f, FontWeight.ExtraBold, -0.02f), // display-xl
    displayMedium = displayStyle(36, 1.05f, FontWeight.ExtraBold, -0.02f), // display-l
    displaySmall = displayStyle(32, 1.05f, FontWeight.ExtraBold, -0.02f), // display-m
    headlineSmall = displayStyle(24, 1.15f, FontWeight.ExtraBold, -0.01f), // title-l
    titleLarge = displayStyle(20, 1.2f, FontWeight.ExtraBold, -0.01f), // title-m
    titleMedium = displayStyle(18, 1.25f, FontWeight.ExtraBold), // title-s
    titleSmall = bodyStyle(16, 1.375f, FontWeight.ExtraBold), // label-l
    bodyLarge = bodyStyle(15, 1.53f, FontWeight.Medium), // body
    bodyMedium = bodyStyle(15, 1.4f, FontWeight.Bold), // body-strong
    bodySmall = bodyStyle(13, 1.38f, FontWeight.Medium), // caption
    labelLarge = bodyStyle(14, 1.43f, FontWeight.ExtraBold), // label-m
    labelMedium = bodyStyle(12, 1.33f, FontWeight.ExtraBold, 0.08f), // overline (uppercase in the text)
    labelSmall = bodyStyle(11, 1.27f, FontWeight.ExtraBold, 0.06f), // micro
)

/** Text styles outside the Material 3 scale. */
object AppTextStyles {
    /** nav-label: 12/16, SemiBold. */
    val navLabel = bodyStyle(12, 1.33f, FontWeight.SemiBold)

    /** nav-label for the selected tab: ExtraBold. */
    val navLabelActive = bodyStyle(12, 1.33f, FontWeight.ExtraBold)

    /** Rest countdown: display-m in the 34px variant, line height 1. */
    val restCountdown = displayStyle(34, 1.0f, FontWeight.ExtraBold, -0.02f)
}
