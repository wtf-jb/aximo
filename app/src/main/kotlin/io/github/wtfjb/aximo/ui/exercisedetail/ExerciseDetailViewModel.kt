package io.github.wtfjb.aximo.ui.exercisedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import io.github.wtfjb.aximo.domain.catalog.CatalogSearch
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.stats.ExerciseBests
import io.github.wtfjb.aximo.domain.stats.ExerciseSession
import io.github.wtfjb.aximo.domain.stats.ExerciseStats
import io.github.wtfjb.aximo.domain.stats.ProgressMetric
import io.github.wtfjb.aximo.domain.stats.ProgressPoint
import io.github.wtfjb.aximo.domain.stats.RepRecord
import io.github.wtfjb.aximo.domain.progression.ProgressionRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Tabs of the exercise detail (mockup UebungDetail.html). */
enum class DetailTab { HISTORY, CHART, RECORDS, INFO }

data class ExerciseDetailUiState(
    val loading: Boolean = true,
    val exercise: Exercise? = null,
    val metric: ProgressMetric = ProgressMetric.E1RM,
    /** Newest first, for the history list. */
    val sessions: List<ExerciseSession> = emptyList(),
    /** Routine name per routine id, for the session meta line. */
    val routineNames: Map<Long, String> = emptyMap(),
    /** Oldest first, for the chart. */
    val points: List<ProgressPoint> = emptyList(),
    /** Workout ids of sessions with a new record. */
    val recordWorkouts: Set<Long> = emptySet(),
    val bests: ExerciseBests? = null,
    val repRecords: List<RepRecord> = emptyList(),
    val suggestion: ProgressionState? = null,
    /** Working sets of the last session: how many sets the suggestion is for. */
    val lastWorkingSets: Int = 0,
    /** Steps from the exercise library, if the exercise has an entry there (B-06). */
    val instructions: List<String> = emptyList(),
    val tab: DetailTab = DetailTab.HISTORY,
)

/** Exercise detail: key figures, next suggestion, history, chart, records, info (A-07). */
class ExerciseDetailViewModel(
    exercises: ExerciseRepository,
    workouts: WorkoutRepository,
    routines: RoutineRepository,
    private val progression: ProgressionRepository,
    catalog: CatalogRepository,
    private val exerciseId: Long,
) : ViewModel() {

    private val tab = MutableStateFlow(DetailTab.HISTORY)
    private val suggestion = MutableStateFlow<ProgressionState?>(null)
    private val instructions = MutableStateFlow<List<String>>(emptyList())

    init {
        viewModelScope.launch { suggestion.value = progression.get(exerciseId) }
        viewModelScope.launch {
            val catalogId = exercises.observeExercises(includeArchived = true).first().firstOrNull { it.id == exerciseId }?.catalogId
            instructions.value = CatalogSearch.entryFor(catalog.entries(), catalogId)?.instructions.orEmpty()
        }
    }

    val uiState: StateFlow<ExerciseDetailUiState> = combine(
        exercises.observeExercises(includeArchived = true),
        workouts.observeFinished(),
        routines.observeRoutines(),
        tab,
        combine(suggestion, instructions) { s, steps -> s to steps },
    ) { allExercises, finished, routineList, tab, (suggestion, steps) ->
        val exercise = allExercises.firstOrNull { it.id == exerciseId }
        val metric = exercise?.let { ExerciseStats.metricFor(it.type) } ?: ProgressMetric.E1RM
        val sessions = ExerciseStats.sessions(finished, exerciseId)
        val points = ExerciseStats.points(sessions, metric)
        ExerciseDetailUiState(
            loading = false,
            exercise = exercise,
            metric = metric,
            sessions = sessions.reversed(),
            routineNames = routineList.associate { it.id to it.name },
            points = points,
            recordWorkouts = ExerciseStats.recordSessions(sessions, metric),
            bests = if (sessions.isEmpty()) null else ExerciseStats.bests(sessions, metric),
            repRecords = ExerciseStats.repRecords(sessions),
            suggestion = suggestion,
            lastWorkingSets = sessions.lastOrNull()?.let { ProgressionRules.workingSets(it.sets).size } ?: 0,
            instructions = steps,
            tab = tab,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())

    fun onTabSelect(value: DetailTab) {
        tab.value = value
    }
}
