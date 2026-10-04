package io.github.wtfjb.aximo.domain

import io.github.wtfjb.aximo.domain.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun systemFollowsTheSystemSetting() {
        assertEquals(true, ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertEquals(false, ThemeMode.SYSTEM.isDark(systemIsDark = false))
    }

    @Test
    fun lightAndDarkIgnoreTheSystemSetting() {
        assertEquals(false, ThemeMode.LIGHT.isDark(systemIsDark = true))
        assertEquals(true, ThemeMode.DARK.isDark(systemIsDark = false))
    }
}
