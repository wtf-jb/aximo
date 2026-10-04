package io.github.wtfjb.aximo.domain.devdata

/** Developer tools for testing: fill the app with sample data, or wipe it. Implemented in :data. */
interface DevDataRepository {
    /**
     * Replaces all training data with generated sample data (about three years of
     * workouts and cardio). Settings and AI provider profiles stay as they are.
     */
    suspend fun loadTestData()

    /**
     * Deletes everything the user entered: exercises, routines, workouts, cardio,
     * progression and the AI reviews and chat. The standard exercises are created
     * again. Settings and AI provider profiles stay.
     */
    suspend fun clearAll()
}
