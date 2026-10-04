package io.github.wtfjb.aximo.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.catalog.CatalogEntry
import io.github.wtfjb.aximo.domain.catalog.CatalogRepository
import io.github.wtfjb.aximo.domain.catalog.CatalogSearch
import io.github.wtfjb.aximo.domain.model.BodyRegion
import io.github.wtfjb.aximo.domain.repository.ExerciseRepository
import io.github.wtfjb.aximo.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CatalogUiState(
    val loading: Boolean = true,
    val query: String = "",
    val region: BodyRegion? = null,
    val entries: List<CatalogEntry> = emptyList(),
    /** `catalogId`s of exercises already in the list (archived ones count). */
    val addedIds: Set<String> = emptySet(),
    /** The entry whose details are open. */
    val selected: CatalogEntry? = null,
)

/** The exercise library (B-06): search, read the instructions, add to the own list. */
class CatalogViewModel(
    private val catalog: CatalogRepository,
    private val exercises: ExerciseRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val all = MutableStateFlow<List<CatalogEntry>?>(null)
    private val query = MutableStateFlow("")
    private val region = MutableStateFlow<BodyRegion?>(null)
    private val selected = MutableStateFlow<CatalogEntry?>(null)

    init {
        viewModelScope.launch { all.value = catalog.entries() }
    }

    private val filters = combine(query, region, selected) { q, r, s -> Triple(q, r, s) }

    val uiState: StateFlow<CatalogUiState> = combine(
        all,
        exercises.observeExercises(includeArchived = true).map { list -> list.mapNotNull { it.catalogId }.toSet() },
        filters,
    ) { entries, added, (q, r, s) ->
        CatalogUiState(
            loading = entries == null,
            query = q,
            region = r,
            entries = entries?.let { CatalogSearch.filter(it, q, r) }.orEmpty(),
            addedIds = added,
            selected = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** null = all regions. */
    fun onRegionSelect(value: BodyRegion?) {
        region.value = value
    }

    fun open(entry: CatalogEntry) {
        selected.value = entry
    }

    fun close() {
        selected.value = null
    }

    /** Adds the entry to the own exercises unless it is already there. */
    fun add(entry: CatalogEntry) {
        viewModelScope.launch {
            val existing = exercises.observeExercises(includeArchived = true).first()
            if (existing.none { it.catalogId == entry.catalogId }) {
                exercises.saveExercise(CatalogSearch.exerciseFor(entry, settings.training.first()))
            }
            selected.value = null
        }
    }
}
