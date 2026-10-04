package io.github.wtfjb.aximo.domain.settings

/** Which color scheme the app uses. SYSTEM follows the Android setting. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    /** Resolves the mode to "dark or not", given the current system setting. */
    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }
}
