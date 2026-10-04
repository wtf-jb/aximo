package io.github.wtfjb.aximo.data

import androidx.room.Room
import io.github.wtfjb.aximo.data.db.AximoDatabase
import org.junit.After
import org.junit.Before
import org.robolectric.RuntimeEnvironment

/** Base class: a fresh in-memory database for every test. */
abstract class DatabaseTest {
    protected lateinit var db: AximoDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AximoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }
}
