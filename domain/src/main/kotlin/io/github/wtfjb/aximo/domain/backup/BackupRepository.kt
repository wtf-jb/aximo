package io.github.wtfjb.aximo.domain.backup

/** Full export and restore of all data as versioned JSON (A-08). Implemented in :data. */
interface BackupRepository {
    /** Everything (exercises, routines, workouts, cardio, progression, training settings) as JSON. */
    suspend fun exportJson(): String

    /**
     * Replaces all data with the content of [json]. Either everything is restored or
     * nothing changes. Throws [BackupException] if the file can't be used.
     */
    suspend fun restoreJson(json: String)
}

class BackupException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** Not an Aximo backup, damaged, or inconsistent. */
        INVALID_FILE,

        /** Written by a newer app version with a higher schemaVersion. */
        NEWER_VERSION,
    }
}
