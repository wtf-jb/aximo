package io.github.wtfjb.aximo.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.exercise.ExerciseFilter
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.model.ExerciseType
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** What the exercise list shows. */
data class ExerciseListUiState(
    val query: String = "",
    val region: BodyRegion? = null,
    val showArchived: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    /** False until the first exercise exists: then the screen shows the empty state. */
    val hasAnyExercise: Boolean = false,
    val loading: Boolean = true,
    /** Picker for a workout: tap selects instead of opening the form. */
    val selectionMode: Boolean = false,
    /** Selected exercise ids in the order they were tapped. */
    val selectedIds: List<Long> = emptyList(),
)

/** The exercise list, either to manage exercises or ([selectionMode]) to pick some for a workout. */
class ExerciseListViewModel(
    repository: ExerciseRepository,
    private val selectionMode: Boolean = false,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val region = MutableStateFlow<BodyRegion?>(null)
    private val showArchived = MutableStateFlow(false)
    private val selected = MutableStateFlow<List<Long>>(emptyList())

    private val filters = combine(query, region, showArchived, selected) { q, r, a, s -> Filters(q, r, a, s) }

    val uiState: StateFlow<ExerciseListUiState> = combine(
        repository.observeExercises(includeArchived = true),
        filters,
    ) { all, f ->
        val pool = all.filter { exercise ->
            if (selectionMode) {
                // Cardio is logged separately (A-04), archived exercises can't be added.
                !exercise.archived && exercise.type != ExerciseType.CARDIO
            } else {
                exercise.archived == f.showArchived
            }
        }
        ExerciseListUiState(
            query = f.query,
            region = f.region,
            showArchived = f.showArchived,
            exercises = ExerciseFilter(f.query, f.region).apply(pool),
            hasAnyExercise = all.isNotEmpty(),
            loading = false,
            selectionMode = selectionMode,
            selectedIds = f.selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseListUiState(selectionMode = selectionMode))

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** null = all regions. */
    fun onRegionSelect(value: BodyRegion?) {
        region.value = value
    }

    fun onShowArchivedToggle() {
        showArchived.value = !showArchived.value
    }

    fun onToggleSelected(id: Long) {
        selected.update { if (id in it) it - id else it + id }
    }

    private data class Filters(
        val query: String,
        val region: BodyRegion?,
        val showArchived: Boolean,
        val selected: List<Long>,
    )
}
