package io.github.wtfjb.aximo.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.exercise.ExerciseFilter
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.model.Exercise
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the exercise list shows. */
data class ExerciseListUiState(
    val query: String = "",
    val region: BodyRegion? = null,
    val showArchived: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    /** False until the first exercise exists: then the screen shows the empty state. */
    val hasAnyExercise: Boolean = false,
    val loading: Boolean = true,
)

class ExerciseListViewModel(repository: ExerciseRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    private val region = MutableStateFlow<BodyRegion?>(null)
    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<ExerciseListUiState> = combine(
        repository.observeExercises(includeArchived = true),
        query,
        region,
        showArchived,
    ) { all, query, region, showArchived ->
        val pool = all.filter { it.archived == showArchived }
        ExerciseListUiState(
            query = query,
            region = region,
            showArchived = showArchived,
            exercises = ExerciseFilter(query, region).apply(pool),
            hasAnyExercise = all.isNotEmpty(),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseListUiState())

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
}
