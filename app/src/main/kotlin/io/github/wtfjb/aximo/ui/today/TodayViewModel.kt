package io.github.wtfjb.aximo.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.wtfjb.aximo.domain.repository.WorkoutRepository
import io.github.wtfjb.aximo.domain.time.TimeSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** "Heute" tab. For now only: start a free workout or resume the running one. */
class TodayViewModel(
    private val workouts: WorkoutRepository,
    private val time: TimeSource,
) : ViewModel() {

    val hasActiveWorkout: StateFlow<Boolean> = workouts.observeActiveWorkout()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Starts a free workout unless one is running, then calls [onReady] to open it. */
    fun startOrResume(onReady: () -> Unit) {
        viewModelScope.launch {
            if (workouts.observeActiveWorkout().first() == null) {
                workouts.startWorkout(time.now())
            }
            onReady()
        }
    }
}
