package io.github.wtfjb.aximo.ui.stats

import io.github.wtfjb.aximo.ui.format.LocalWeightUnit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.cardio.CardioMath
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.stats.CardioMetric
import io.github.wtfjb.aximo.domain.stats.Consistency
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.stats.MuscleVolume
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import io.github.wtfjb.aximo.domain.stats.StatsPeriod
import io.github.wtfjb.aximo.domain.units.WeightUnit
import io.github.wtfjb.aximo.ui.components.ChoiceChip
import io.github.wtfjb.aximo.ui.components.SegmentedControl
import io.github.wtfjb.aximo.ui.components.StatTile
import io.github.wtfjb.aximo.ui.exercises.label
import io.github.wtfjb.aximo.ui.format.formatInteger
import io.github.wtfjb.aximo.ui.format.formatOneDecimal
import io.github.wtfjb.aximo.ui.format.formatPercentChange
import io.github.wtfjb.aximo.ui.format.formatSetValue
import io.github.wtfjb.aximo.ui.format.formatShortDate
import io.github.wtfjb.aximo.ui.format.formatWeight
import io.github.wtfjb.aximo.ui.icons.AppIcons
import io.github.wtfjb.aximo.ui.theme.Radii
import io.github.wtfjb.aximo.ui.theme.Sizes
import io.github.wtfjb.aximo.ui.theme.Spacing
import io.github.wtfjb.aximo.ui.theme.extendedColors
import org.koin.androidx.compose.koinViewModel

/** Statistics tab (A-07, mockup Statistik.html) plus a cardio card. */
@Composable
fun StatsScreen(
    onOpenExercise: (Long) -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unit = LocalWeightUnit.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.s20, end = Spacing.s20, top = Spacing.s28, bottom = Spacing.s24),
        verticalArrangement = Arrangement.spacedBy(Spacing.s16),
    ) {
        item { Text(text = stringResource(R.string.nav_stats), style = MaterialTheme.typography.displayMedium) }
        item {
            SegmentedControl(
                options = StatsPeriod.entries.map { stringResource(it.label()) },
                selectedIndex = state.period.ordinal,
                onSelect = { viewModel.onPeriodChange(StatsPeriod.entries[it]) },
            )
        }
        if (state.isEmpty) {
            item {
                Text(
                    text = stringResource(R.string.stats_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        state.progress?.let { progress ->
            item {
                ProgressCard(
                    progress = progress,
                    period = state.period,
                    choices = state.exercises,
                    unit = unit,
                    onPick = viewModel::onExerciseChange,
                    onOpen = { onOpenExercise(progress.exercise.id) },
                )
            }
            item { VolumeCard(state.regions) }
        }
        state.consistency?.takeIf { !state.isEmpty }?.let { item { ConsistencyCard(it) } }
        state.cardio?.let { trend ->
            item {
                CardioCard(
                    trend = trend,
                    activities = state.cardioActivities,
                    onPickActivity = viewModel::onCardioActivityChange,
                    onPickMetric = viewModel::onCardioMetricChange,
                )
            }
        }
    }
}

private fun StatsPeriod.label(): Int = when (this) {
    StatsPeriod.FOUR_WEEKS -> R.string.stats_period_4w
    StatsPeriod.THREE_MONTHS -> R.string.stats_period_3m
    StatsPeriod.ONE_YEAR -> R.string.stats_period_1y
    StatsPeriod.ALL -> R.string.stats_period_all
}

/** White card with radius-2xl, as all cards on this screen. */
@Composable
private fun StatsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = Radii.xxl, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.s18), verticalArrangement = Arrangement.spacedBy(Spacing.s14), content = content)
    }
}

@Composable
private fun CardHeader(title: String, trailing: String? = null) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Pill on a white card (surface-container-low), e.g. the exercise picker. */
@Composable
private fun CardPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    description: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = Radii.full,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .heightIn(min = Sizes.touch)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(start = Spacing.s14, end = Spacing.s12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s6),
        ) {
            Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (trailingIcon != null) Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
        }
    }
}

@Composable
private fun ProgressCard(
    progress: ExerciseProgress,
    period: StatsPeriod,
    choices: List<io.github.wtfjb.aximo.domain.model.Exercise>,
    unit: WeightUnit,
    onPick: (Long) -> Unit,
    onOpen: () -> Unit,
) {
    val unitLabel = stringResource(unit.label())
    val isReps = progress.metric == ProgressMetric.REPS
    val format: (Double) -> String = if (isReps) { v -> formatInteger(v) } else { v -> formatWeight(Math.round(v * 2) / 2.0, unit) }
    StatsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            var menuOpen by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                CardPill(
                    text = progress.exercise.name,
                    onClick = { menuOpen = true },
                    trailingIcon = AppIcons.ChevronDown,
                    description = stringResource(R.string.stats_pick_exercise, progress.exercise.name),
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    choices.forEach { exercise ->
                        DropdownMenuItem(
                            text = { Text(exercise.name, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                menuOpen = false
                                onPick(exercise.id)
                            },
                        )
                    }
                }
            }
            CardPill(text = stringResource(R.string.stats_details), onClick = onOpen, trailingIcon = AppIcons.ArrowRight)
        }

        val points = progress.points
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(
                    text = stringResource(if (isReps) R.string.stats_reps_label else R.string.stats_e1rm_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = points.lastOrNull()?.let { format(it.value) } ?: stringResource(R.string.exercise_detail_none),
                        style = MaterialTheme.typography.displayLarge,
                    )
                    if (!isReps && points.isNotEmpty()) {
                        Text(text = " $unitLabel", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            progress.change?.let { change ->
                ChangeChip(stringResource(R.string.stats_change, formatPercentChange(change), stringResource(period.label())), rising = change > 0)
            }
        }

        when {
            points.isEmpty() -> MutedText(stringResource(R.string.stats_no_sessions))
            points.size < 2 -> MutedText(stringResource(R.string.stats_too_few))
            else -> {
                val from = formatShortDate(points.first().startedAt)
                val to = formatShortDate(points.last().startedAt)
                TrendChart(
                    values = points.map { it.value },
                    highlighted = progress.recordIndices,
                    formatValue = { formatInteger(it) },
                    contentDescription = stringResource(
                        if (isReps) R.string.stats_chart_reps else R.string.stats_chart_e1rm,
                        progress.exercise.name,
                        from,
                        to,
                    ),
                )
                DateRow(from, to)
            }
        }

        val bests = progress.bests
        val none = stringResource(R.string.exercise_detail_none)
        val bodyweight = isReps
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            StatTile(
                value = bests.maxWeightKg?.let { "${formatWeight(it, unit)} $unitLabel" } ?: none,
                label = stringResource(if (isReps) R.string.stats_max_extra else R.string.stats_max_weight),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = bests.bestSet?.let { formatSetValue(it.weightKg, it.reps, bodyweight, unit) } ?: none,
                label = stringResource(R.string.stats_best_set),
                modifier = Modifier.weight(1f),
            )
            if (isReps) {
                StatTile(formatInteger(bests.totalReps.toDouble()), stringResource(R.string.stats_total_reps), Modifier.weight(1f))
            } else {
                StatTile(formatInteger(unit.fromKg(bests.volumeKg)), stringResource(R.string.stats_volume, unitLabel), Modifier.weight(1f))
            }
        }
    }
}

/** "+9,1 % in 3 M" in accent-container; the arrow only for a rise. */
@Composable
private fun ChangeChip(text: String, rising: Boolean) {
    Surface(
        shape = Radii.full,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.padding(bottom = Spacing.s4),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s10, vertical = Spacing.s6),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            if (rising) Icon(AppIcons.TrendUp, contentDescription = null, modifier = Modifier.size(Sizes.iconSmall))
            Text(text = text, style = MaterialTheme.typography.bodySmall.copy(fontWeight = MaterialTheme.typography.labelLarge.fontWeight))
        }
    }
}

@Composable
private fun MutedText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** First and last date under a chart. */
@Composable
fun DateRow(from: String, to: String) {
    Row(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
        Text(from, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(to, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VolumeCard(regions: List<RegionVolume>) {
    StatsCard {
        CardHeader(stringResource(R.string.stats_sets_title), stringResource(R.string.stats_per_week))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s14), modifier = Modifier.clearAndSetSemantics { }) {
            LegendItem(
                color = MaterialTheme.extendedColors.chartBand,
                text = stringResource(R.string.stats_target_band, MuscleVolume.TARGET_MIN.toInt(), MuscleVolume.TARGET_MAX.toInt()),
                bar = true,
            )
            LegendItem(color = MaterialTheme.extendedColors.accentChart, text = stringResource(R.string.stats_below_target), bar = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s12)) {
            regions.forEach { VolumeRow(it) }
        }
    }
}

@Composable
private fun VolumeRow(item: RegionVolume) {
    val name = stringResource(item.region.label())
    val value = formatOneDecimal(item.setsPerWeek)
    val description = stringResource(R.string.stats_region_sets, name, value)
    val below = MuscleVolume.belowTarget(item.setsPerWeek)
    val scale = MuscleVolume.SCALE_MAX
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.s10),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(Sizes.volumeLabelColumn),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(Sizes.volumeBar)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            // Target band 10–20 behind the bar
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight((MuscleVolume.TARGET_MIN / scale).toFloat()))
                Box(
                    Modifier
                        .weight(((MuscleVolume.TARGET_MAX - MuscleVolume.TARGET_MIN) / scale).toFloat())
                        .fillMaxHeight()
                        .background(MaterialTheme.extendedColors.chartBand),
                )
                Spacer(Modifier.weight(((scale - MuscleVolume.TARGET_MAX) / scale).toFloat()))
            }
            val fraction = (item.setsPerWeek / scale).coerceIn(0.0, 1.0).toFloat()
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (below) MaterialTheme.extendedColors.accentChart else MaterialTheme.colorScheme.onSurface),
                )
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(Sizes.volumeValueColumn),
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String, bar: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s6)) {
        Box(
            Modifier
                .size(
                    width = if (bar) Sizes.legendBarWidth else Sizes.legendDot,
                    height = if (bar) Sizes.legendBarHeight else Sizes.legendDot,
                )
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color),
        )
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConsistencyCard(stats: ConsistencyStats) {
    StatsCard {
        CardHeader(
            stringResource(R.string.stats_consistency),
            stringResource(R.string.stats_sessions_per_week, formatOneDecimal(stats.perWeek)),
        )
        val strengthDays = stats.heatmap.flatten().count { it == DayKind.STRENGTH }
        val cardioDays = stats.heatmap.flatten().count { it == DayKind.CARDIO }
        val description = pluralStringResource(R.plurals.stats_heatmap_description, Consistency.HEATMAP_WEEKS, Consistency.HEATMAP_WEEKS, strengthDays, cardioDays)
        Column(
            modifier = Modifier.clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(Spacing.s4),
        ) {
            // Rows are weekdays (Monday on top), columns are weeks (oldest left).
            for (day in 0 until 7) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                    stats.heatmap.forEach { week ->
                        val kind = week[day]
                        Box(
                            Modifier
                                .weight(1f)
                                .height(Sizes.heatCell)
                                .clip(Radii.smHalf)
                                .background(
                                    when (kind) {
                                        DayKind.STRENGTH -> MaterialTheme.extendedColors.accentChart
                                        DayKind.CARDIO -> MaterialTheme.colorScheme.inverseSurface
                                        DayKind.NONE -> MaterialTheme.extendedColors.chartGrid
                                        DayKind.FUTURE -> Color.Transparent
                                    },
                                ),
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clearAndSetSemantics { }) {
            Text(
                text = pluralStringResource(R.plurals.stats_heatmap_caption, Consistency.HEATMAP_WEEKS, Consistency.HEATMAP_WEEKS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s12)) {
                LegendItem(MaterialTheme.extendedColors.accentChart, stringResource(R.string.stats_strength), bar = false)
                LegendItem(MaterialTheme.colorScheme.inverseSurface, stringResource(R.string.stats_cardio), bar = false)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            StatTile(
                value = pluralStringResource(R.plurals.stats_streak_weeks, stats.streakWeeks, stats.streakWeeks),
                label = stringResource(R.string.stats_streak_label),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = stats.sessions.toString(),
                label = stringResource(R.string.stats_sessions_label),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CardioCard(
    trend: CardioTrend,
    activities: List<io.github.wtfjb.aximo.domain.model.Exercise>,
    onPickActivity: (Long) -> Unit,
    onPickMetric: (CardioMetric) -> Unit,
) {
    StatsCard {
        CardHeader(stringResource(R.string.stats_cardio))
        if (activities.size > 1) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.s8),
            ) {
                activities.forEach { activity ->
                    ChoiceChip(
                        text = activity.name,
                        selected = activity.id == trend.activity.id,
                        onClick = { onPickActivity(activity.id) },
                        unselectedColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                }
            }
        }
        SegmentedControl(
            options = CardioMetric.entries.map { stringResource(it.label(trend.paceStyle)) },
            selectedIndex = trend.metric.ordinal,
            onSelect = { onPickMetric(CardioMetric.entries[it]) },
        )
        val format: (Double) -> String = { cardioValue(it, trend.metric, trend.paceStyle) }
        val points = trend.points
        if (points.isEmpty()) {
            MutedText(stringResource(R.string.stats_cardio_no_values))
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s4)) {
                Text(
                    text = stringResource(R.string.stats_cardio_latest),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = cardioValueWithUnit(points.last().value, trend.metric, trend.paceStyle), style = MaterialTheme.typography.displaySmall)
            }
            if (points.size >= 2) {
                val from = formatShortDate(points.first().startedAt)
                val to = formatShortDate(points.last().startedAt)
                TrendChart(
                    values = points.map { it.value },
                    highlighted = emptySet(),
                    formatValue = format,
                    contentDescription = stringResource(
                        R.string.stats_cardio_chart,
                        stringResource(trend.metric.label(trend.paceStyle)),
                        trend.activity.name,
                        from,
                        to,
                    ),
                )
                DateRow(from, to)
            } else {
                MutedText(stringResource(R.string.stats_too_few))
            }
        }
        val summary = trend.summary
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            StatTile(summary.sessions.toString(), stringResource(R.string.stats_sessions_label), Modifier.weight(1f))
            StatTile(
                io.github.wtfjb.aximo.ui.cardio.formatKm(summary.distanceM, decimals = 1),
                stringResource(R.string.stats_distance_km),
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
            StatTile(CardioMath.formatDuration(summary.durationSec), stringResource(R.string.stats_duration_total), Modifier.weight(1f))
            StatTile(
                summary.avgHeartRate?.let { stringResource(R.string.stats_bpm, it) } ?: stringResource(R.string.exercise_detail_none),
                stringResource(R.string.stats_avg_heart_rate),
                Modifier.weight(1f),
            )
        }
    }
}

private fun CardioMetric.label(style: PaceStyle): Int = when (this) {
    CardioMetric.PACE -> if (style == PaceStyle.SPEED) R.string.stats_metric_speed else R.string.stats_metric_pace
    CardioMetric.DISTANCE -> R.string.stats_metric_distance
    CardioMetric.DURATION -> R.string.stats_metric_duration
    CardioMetric.HEART_RATE -> R.string.stats_metric_heart_rate
}

/** Axis label: pace "5:31", speed "10,9", distance "5,2", duration in minutes, heart rate. */
private fun cardioValue(value: Double, metric: CardioMetric, style: PaceStyle): String = when (metric) {
    CardioMetric.PACE -> if (style == PaceStyle.SPEED) formatOneDecimal(value) else CardioMath.formatPace(value)
    CardioMetric.DISTANCE -> formatOneDecimal(value / METERS_PER_KM)
    CardioMetric.DURATION -> formatInteger(value / SECONDS_PER_MINUTE)
    CardioMetric.HEART_RATE -> formatInteger(value)
}

@Composable
private fun cardioValueWithUnit(value: Double, metric: CardioMetric, style: PaceStyle): String = when (metric) {
    CardioMetric.PACE -> when (style) {
        PaceStyle.PER_KM -> stringResource(R.string.cardio_pace_per_km, CardioMath.formatPace(value))
        PaceStyle.PER_500M -> stringResource(R.string.cardio_pace_per_500m, CardioMath.formatPace(value))
        PaceStyle.SPEED -> stringResource(R.string.cardio_speed_kmh, io.github.wtfjb.aximo.ui.cardio.formatSpeed(value))
    }
    CardioMetric.DISTANCE -> "${io.github.wtfjb.aximo.ui.cardio.formatKm(value, decimals = 2)} ${stringResource(R.string.cardio_km)}"
    CardioMetric.DURATION -> CardioMath.formatDuration(value.toLong())
    CardioMetric.HEART_RATE -> stringResource(R.string.stats_bpm, value.toInt())
}

private const val METERS_PER_KM = 1000.0
private const val SECONDS_PER_MINUTE = 60.0
