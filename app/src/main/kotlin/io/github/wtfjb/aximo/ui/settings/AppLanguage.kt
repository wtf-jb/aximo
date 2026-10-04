package io.github.wtfjb.aximo.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi

/** App language (A-09). SYSTEM follows the device language. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    GERMAN("de"),
    ENGLISH("en"),
}

/**
 * Per-app language via Android's LocaleManager (Android 13+). Android stores the
 * choice and restarts the screen in the new language. Older versions only follow
 * the device language, so the setting is hidden there.
 */
object AppLanguages {
    val isSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun current(context: Context): AppLanguage {
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val language = locales[0].language
        return AppLanguage.entries.firstOrNull { it != AppLanguage.SYSTEM && it.tag == language } ?: AppLanguage.SYSTEM
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun set(context: Context, language: AppLanguage) {
        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language.tag)
    }
}
