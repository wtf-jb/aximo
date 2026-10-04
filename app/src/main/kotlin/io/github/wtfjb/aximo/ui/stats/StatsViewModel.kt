package io.github.wtfjb.aximo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.cardio.CardioActivities
import io.github.wtfjb.aximo.domain.cardio.PaceStyle
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.CardioEntry
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.CardioRepository
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.stats.CardioMetric
import io.github.wtfjb.aximo.domain.stats.CardioPoint
import io.github.wtfjb.aximo.domain.stats.CardioSummary
import io.github.wtfjb.aximo.domain.stats.CardioTrends
import io.github.wtfjb.aximo.domain.stats.Consistency
import io.github.wtfjb.aximo.domain.stats.DayKind
import io.github.wtfjb.aximo.domain.stats.ExerciseBests
import io.github.wtfjb.aximo.domain.stats.ExerciseStats
import io.github.wtfjb.aximo.domain.stats.MuscleVolume
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import io.github.wtfjb.aximo.domain.stats.ProgressPoint
import io.github.wtfjb.aximo.domain.stats.StatsCalendar
import io.github.wtfjb.aximo.domain.stats.StatsPeriod
import io.github.wtfjb.aximo.domain.time.TimeSource
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone

/** e1RM (or reps) progress of one exercise in the chosen period. */
data class ExerciseProgress(
    val exercise: Exercise,
    val metric: ProgressMetric,
    val points: List<ProgressPoint>,
    /** Indices into [points] of sessions that set a new record. */
    val recordIndices: Set<Int>,
    val change: Double?,
    val bests: ExerciseBests,
)

data class RegionVolume(val region: BodyRegion, val setsPerWeek: Double)

data class ConsistencyStats(
    /** Weeks oldest first, each Monday to Sunday. */
    val heatmap: List<List<DayKind>>,
    val sessions: Int,
    val perWeek: Double,
    val streakWeeks: Int,
)

data class CardioTrend(
    val activity: Exercise,
    val paceStyle: PaceStyle,
    val metric: CardioMetric,
    val points: List<CardioPoint>,
    val summary: CardioSummary,
)

data class StatsUiState(
    val loading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.THREE_MONTHS,
    /** Exercises with history, most recently trained first. */
    val exercises: List<Exercise> = emptyList(),
    val progress: ExerciseProgress? = null,
    val regions: List<RegionVolume> = emptyList(),
    val consistency: ConsistencyStats? = null,
    val cardioActivities: List<Exercise> = emptyList(),
    val cardio: CardioTrend? = null,
) {
    /** Nothing logged yet at all. */
    val isEmpty: Boolean get() = !loading && exercises.isEmpty() && cardioActivities.isEmpty()
}

/** What the user picked on the screen. Null ids mean "the most recent one". */
private data class Selection(
    val period: StatsPeriod = StatsPeriod.THREE_MONTHS,
    val exerciseId: Long? = null,
    val cardioActivityId: Long? = null,
    val cardioMetric: CardioMetric = CardioMetric.PACE,
)

/** Statistics tab (A-07, mockup Statistik.html). */
class StatsViewModel(
    workouts: WorkoutRepository,
    cardio: CardioRepository,
    exercises: ExerciseRepository,
    private val time: TimeSource,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val selection = MutableStateFlow(Selection())

    val uiState: StateFlow<StatsUiState> = combine(
        workouts.observeFinished(),
        cardio.observeAll(),
        exercises.observeExercises(includeArchived = true),
        selection,
    ) { finished, entries, allExercises, picked ->
        build(finished, entries, allExercises, picked)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun onPeriodChange(period: StatsPeriod) = selection.update { it.copy(period = period) }

    fun onExerciseChange(id: Long) = selection.update { it.copy(exerciseId = id) }

    fun onCardioActivityChange(id: Long) = selection.update { it.copy(cardioActivityId = id) }

    fun onCardioMetricChange(metric: CardioMetric) = selection.update { it.copy(cardioMetric = metric) }

    private fun build(
        finished: List<WorkoutDetail>,
        entries: List<CardioEntry>,
        allExercises: List<Exercise>,
        picked: Selection,
    ): StatsUiState {
        val today = StatsCalendar.localDate(time.now(), zone)
        val period = picked.period
        val inPeriod = { instant: kotlin.time.Instant -> StatsCalendar.inPeriod(instant, period, today, zone) }
        val workoutsInPeriod = finished.filter { inPeriod(it.workout.startedAt) }
        // Cardio inside a workout is part of that workout's session.
        val standaloneCardio = entries.filter { it.workoutId == null }
        val cardioInPeriod = entries.filter { inPeriod(it.startedAt) }

        val first = (finished.map { it.workout.startedAt } + standaloneCardio.map { it.startedAt }).minOrNull()
        val weeks = StatsCalendar.weekCount(period, today, first?.let { StatsCalendar.localDate(it, zone) })

        // Exercise progress
        val trained = ExerciseStats.trainedExercises(finished)
        val exercise = trained.firstOrNull { it.id == picked.exerciseId } ?: trained.firstOrNull()
        val progress = exercise?.let { ex ->
            val metric = ExerciseStats.metricFor(ex.type)
            val sessions = ExerciseStats.sessions(workoutsInPeriod, ex.id)
            val points = ExerciseStats.points(sessions, metric)
            // Records compare with all earlier sessions, not just those in the period.
            val records = ExerciseStats.recordSessions(ExerciseStats.sessions(finished, ex.id), metric)
            ExerciseProgress(
                exercise = ex,
                metric = metric,
                points = points,
                recordIndices = points.indices.filter { points[it].workoutId in records }.toSet(),
                change = ExerciseStats.change(points),
                bests = ExerciseStats.bests(sessions, metric),
            )
        }

        // Sets per region and week
        val perWeek = MuscleVolume.perWeek(MuscleVolume.setsPerRegion(workoutsInPeriod), weeks)
        val regions = MuscleVolume.regions.map { RegionVolume(it, perWeek.getValue(it)) }

        // Consistency: the heatmap always shows the last weeks, the numbers follow the period.
        val days = Consistency.activeDays(finished.map { it.workout.startedAt }, standaloneCardio.map { it.startedAt }, zone)
        val sessions = workoutsInPeriod.size + standaloneCardio.count { inPeriod(it.startedAt) }
        val consistency = ConsistencyStats(
            heatmap = Consistency.heatmap(days, today),
            sessions = sessions,
            perWeek = Consistency.perWeek(sessions, weeks),
            streakWeeks = Consistency.streakWeeks(days.keys, today),
        )

        // Cardio trends of one activity
        val activities = CardioTrends.activities(entries, allExercises)
        val activity = activities.firstOrNull { it.id == picked.cardioActivityId } ?: activities.firstOrNull()
        val cardioTrend = activity?.let { act ->
            val style = CardioActivities.paceStyle(act)
            val ofActivity = cardioInPeriod.filter { it.exerciseId == act.id }
            CardioTrend(
                activity = act,
                paceStyle = style,
                metric = picked.cardioMetric,
                points = CardioTrends.points(ofActivity, picked.cardioMetric, style),
                summary = CardioTrends.summary(ofActivity),
            )
        }

        return StatsUiState(
            loading = false,
            period = period,
            exercises = trained,
            progress = progress,
            regions = regions,
            consistency = consistency,
            cardioActivities = activities,
            cardio = cardioTrend,
        )
    }
}
