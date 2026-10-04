package io.github.wtfjb.aximo.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ProgressionState
import io.github.wtfjb.aximo.domain.progression.ProgressionRules
import io.github.wtfjb.aximo.domain.repository.ProgressionRepository
import io.github.wtfjb.aximo.domain.repository.RoutineRepository
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.stats.PersonalRecord
import io.github.wtfjb.aximo.domain.stats.Records
import io.github.wtfjb.aximo.domain.stats.WorkoutComparison
import io.github.wtfjb.aximo.domain.workout.WorkoutDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecordItem(val exercise: Exercise, val record: PersonalRecord)

/** One line of "Fürs nächste Mal": current top weight and the suggestion. */
data class NextTimeItem(val exercise: Exercise, val currentWeightKg: Double, val suggestion: ProgressionState)

data class SummaryUiState(
    val loading: Boolean = true,
    val workout: WorkoutDetail? = null,
    val routineName: String? = null,
    val durationSeconds: Long = 0,
    val volumeKg: Double = 0.0,
    val workingSets: Int = 0,
    /** Volume change against the previous workout of the same routine, e.g. 0.04 = +4 %. */
    val volumeChange: Double? = null,
    val records: List<RecordItem> = emptyList(),
    val nextTime: List<NextTimeItem> = emptyList(),
    /** 1–5, null = not chosen. */
    val rating: Int? = null,
    val note: String = "",
    /** True once saved: the screen closes. */
    val done: Boolean = false,
)

/** Summary after finishing a workout (mockup Abschluss.html). */
class SummaryViewModel(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val progression: ProgressionRepository,
    private val workoutId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SummaryUiState())
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val detail = workouts.getWorkout(workoutId)
        if (detail == null) {
            _uiState.update { it.copy(loading = false) }
            return
        }
        val start = detail.workout.startedAt
        val allSets = detail.exercises.flatMap { it.sets }
        val byExercise = detail.exercises.groupBy { it.entry.exerciseId }

        val records = byExercise.mapNotNull { (exerciseId, entries) ->
            val record = Records.newRecord(entries.flatMap { it.sets }, workouts.setsBefore(exerciseId, start))
            record?.let { RecordItem(entries.first().exercise, it) }
        }
        val nextTime = byExercise.mapNotNull { (exerciseId, entries) ->
            val suggestion = progression.get(exerciseId) ?: return@mapNotNull null
            val working = ProgressionRules.workingSets(entries.flatMap { it.sets })
            if (working.isEmpty()) return@mapNotNull null
            NextTimeItem(entries.first().exercise, working.maxOf { it.weightKg }, suggestion)
        }

        _uiState.update {
            it.copy(
                loading = false,
                workout = detail,
                routineName = detail.workout.routineId?.let { id -> routines.getRoutine(id)?.routine?.name },
                durationSeconds = detail.workout.endedAt?.let { end -> (end - start).inWholeSeconds } ?: 0,
                volumeKg = Records.volume(allSets),
                volumeChange = detail.workout.routineId?.let { routineId ->
                    WorkoutComparison.volumeChange(detail, workouts.previousOfRoutine(routineId, start))
                },
                workingSets = Records.countingSets(allSets).size,
                records = records,
                nextTime = nextTime,
                rating = detail.workout.rating,
                note = detail.workout.note,
            )
        }
    }

    fun onRatingChange(rating: Int) = _uiState.update { it.copy(rating = if (it.rating == rating) null else rating) }

    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            workouts.rateWorkout(workoutId, state.rating, state.note)
            _uiState.update { it.copy(done = true) }
        }
    }
}
