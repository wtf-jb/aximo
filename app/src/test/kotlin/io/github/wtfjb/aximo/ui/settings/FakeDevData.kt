package io.github.wtfjb.aximo.ui.settings

import io.github.wtfjb.aximo.domain.devdata.DevDataRepository

class FakeDevData : DevDataRepository {
    var loaded = 0
    var cleared = 0
    var failure: Exception? = null

    override suspend fun loadTestData() {
        failure?.let { throw it }
        loaded++
    }

    override suspend fun clearAll() {
        failure?.let { throw it }
        cleared++
    }
}
