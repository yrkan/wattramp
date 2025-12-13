package io.github.wattramp.util

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import io.github.wattramp.data.PreferencesRepository.AppLanguage
import java.util.Locale

/**
 * Helper for managing app locale/language settings.
 * Karoo 3 runs Android 13+ (TIRAMISU), so we use the modern LocaleManager API.
 */
object LocaleHelper {

    /**
     * Apply the selected language to the app.
     */
    fun applyLanguage(context: Context, language: AppLanguage) {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return

        if (language == AppLanguage.SYSTEM) {
            // Reset to system default
            localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
        } else {
            // Set specific locale
            val locale = when (language) {
                AppLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
                else -> Locale(language.code)
            }
            localeManager.applicationLocales = LocaleList(locale)
        }
    }

    /**
     * Get current app locale.
     */
    fun getCurrentLocale(context: Context): Locale {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val locales = localeManager?.applicationLocales
        return if (locales != null && !locales.isEmpty) {
            locales.get(0) ?: Locale.getDefault()
        } else {
            Locale.getDefault()
        }
    }
}
