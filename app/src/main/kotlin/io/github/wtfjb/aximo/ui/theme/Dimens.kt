package io.github.wtfjb.aximo.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing tokens (space-4 … space-28). */
object Spacing {
    val s4 = 4.dp
    val s6 = 6.dp
    val s8 = 8.dp
    val s10 = 10.dp
    val s12 = 12.dp
    val s14 = 14.dp
    val s16 = 16.dp
    val s18 = 18.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s28 = 28.dp
}

/** Size tokens. */
object Sizes {
    val touch = 44.dp
    val input = 46.dp
    val cta = 54.dp
    val nav = 80.dp
    val icon = 22.dp

    /** Small icons inside buttons (design: 18–20). */
    val iconSmall = 18.dp

    /** Navigation indicator pill, from the mockup (60 × 32). */
    val navIndicatorWidth = 60.dp
    val navIndicatorHeight = 32.dp

    /** Hairline for separators such as the top edge of the navigation bar. */
    val hairline = 1.dp

    /** Filter chip height (tap area is still at least [touch]). */
    val chip = 36.dp

    /** Search field height, from the mockup. */
    val search = 48.dp

    /** Icon tile in list rows. */
    val listTile = 48.dp

    /** Selection check circle in list rows. */
    val check = 28.dp

    /** Set row columns (mockup grid 40 | 1fr | 1fr | 56 | 44). */
    val setNumberColumn = 40.dp
    val setRirColumn = 56.dp

    /** Small round badge (W/D/F) and the check buttons of done and active sets. */
    val badge = 28.dp
    val setCheckDone = 36.dp
    val setCheckActive = 40.dp

    /** Borders: active input (accent) and normal control (line-control). */
    val borderActive = 2.dp
    val borderControl = 1.5.dp

    /** Progress track of the rest timer bar (6, radius 3). */
    val restProgress = 6.dp

    /** Dashed "add" button: corner radius (= radius-xl) and dash length. */
    val dashRadius = 22.dp
    val dashLength = 6.dp

    /** Mood tile on the finish screen (mockup: 64). */
    val moodTile = 64.dp

    /** Line chart (mockup Statistik.html: 140 high, line 2.5, points r 3.5 / last r 6). */
    val chartHeight = 140.dp
    val chartLine = 2.5.dp
    val chartPoint = 7.dp
    val chartPointLast = 12.dp
    val chartPointStroke = 2.dp

    /** Sets per muscle group: bar height and the label / value columns (82 | 1fr | 28). */
    val volumeBar = 12.dp
    val volumeLabelColumn = 82.dp
    val volumeValueColumn = 28.dp

    /** Heatmap cell height and legend swatches (16 × 10 bar, 10 × 10 dot). */
    val heatCell = 18.dp
    val legendBarWidth = 16.dp
    val legendBarHeight = 10.dp
    val legendDot = 10.dp

    /** Set chip in the exercise history ("82,5 × 8 · R2"). */
    val setChip = 34.dp

    /** Day circle in the week bar on "Heute" (mockup: 36, today ring 2.5). */
    val weekDay = 36.dp
    val todayRing = 2.5.dp

    /** Color swatch in the theme showcase. */
    val swatch = 40.dp

    /** Icon tile at the start of a settings row (KI-Coach). */
    val iconTile = 40.dp
}

/** Elevation tokens. Surfaces stand out by color, not by shadow or tint. */
object Elevation {
    val none = 0.dp

    /** shadow-float: floating bars (timer, selection bar). */
    val float = 12.dp
}
