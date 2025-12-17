package io.github.wattramp.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import io.github.wattramp.data.PreferencesRepository.AppLanguage
import io.github.wattramp.data.PreferencesRepository.AppTheme
import java.util.Locale

/**
 * Helper for managing app locale/language and theme settings.
 * Uses SharedPreferences for instant access on app startup.
 */
object LocaleHelper {

    private const val PREF_NAME = "wattramp_locale"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_THEME = "selected_theme"

    /**
     * Save the selected language code to SharedPreferences.
     */
    fun saveLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    /**
     * Get the saved language from SharedPreferences.
     */
    fun getSavedLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val languageName = prefs.getString(KEY_LANGUAGE, AppLanguage.SYSTEM.name)
        return try {
            AppLanguage.valueOf(languageName ?: AppLanguage.SYSTEM.name)
        } catch (e: Exception) {
            AppLanguage.SYSTEM
        }
    }

    /**
     * Wrap the context with the selected locale configuration.
     * Call this from Activity.attachBaseContext()
     */
    fun wrapContext(context: Context): Context {
        val language = getSavedLanguage(context)
        return if (language == AppLanguage.SYSTEM) {
            context
        } else {
            val locale = getLocaleForLanguage(language)
            wrapContextWithLocale(context, locale)
        }
    }

    /**
     * Apply the selected language - saves preference and returns whether activity needs recreation.
     */
    fun applyLanguage(context: Context, language: AppLanguage): Boolean {
        val currentLanguage = getSavedLanguage(context)
        if (currentLanguage == language) {
            return false
        }
        saveLanguage(context, language)
        return true
    }

    /**
     * Get Locale for the given AppLanguage.
     */
    private fun getLocaleForLanguage(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.SYSTEM -> Locale.getDefault()
            AppLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.ENGLISH -> Locale.ENGLISH
            AppLanguage.GERMAN -> Locale.GERMAN
            AppLanguage.FRENCH -> Locale.FRENCH
            AppLanguage.ITALIAN -> Locale.ITALIAN
            AppLanguage.JAPANESE -> Locale.JAPANESE
            else -> Locale(language.code)
        }
    }

    /**
     * Wrap context with the specified locale.
     */
    private fun wrapContextWithLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }

    /**
     * Get current app locale.
     */
    fun getCurrentLocale(context: Context): Locale {
        val language = getSavedLanguage(context)
        return if (language == AppLanguage.SYSTEM) {
            Locale.getDefault()
        } else {
            getLocaleForLanguage(language)
        }
    }

    // ==================== Theme ====================

    /**
     * Save the selected theme to SharedPreferences.
     */
    fun saveTheme(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.name)
            .apply()
    }

    /**
     * Get the saved theme from SharedPreferences.
     */
    fun getSavedTheme(context: Context): AppTheme {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val themeName = prefs.getString(KEY_THEME, AppTheme.ORANGE.name)
        return try {
            AppTheme.valueOf(themeName ?: AppTheme.ORANGE.name)
        } catch (e: Exception) {
            AppTheme.ORANGE
        }
    }
}
