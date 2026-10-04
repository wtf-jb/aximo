package io.github.wtfjb.aximo.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.ai.AiProfileRepository
import io.github.wtfjb.aximo.domain.ai.AiProfiles
import io.github.wtfjb.aximo.domain.ai.AiProviderProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AiProfilesUiState(
    val profiles: List<AiProviderProfile> = emptyList(),
    /** The profile the AI features use. */
    val activeId: Long? = null,
    val loading: Boolean = true,
)

/** List of provider profiles (B-01): pick the active one, open one to edit. */
class AiProfilesViewModel(private val repository: AiProfileRepository) : ViewModel() {

    val uiState: StateFlow<AiProfilesUiState> = repository.observeProfiles()
        .map { profiles -> AiProfilesUiState(profiles, AiProfiles.active(profiles)?.id, loading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiProfilesUiState())

    fun setActive(id: Long) {
        viewModelScope.launch { repository.setActive(id) }
    }
}
