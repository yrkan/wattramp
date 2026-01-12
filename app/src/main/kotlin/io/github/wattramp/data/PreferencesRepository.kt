package io.github.wattramp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wattramp_settings")

/**
 * Repository for storing and retrieving user preferences and test history.
 */
class PreferencesRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        // Settings keys
        private val KEY_CURRENT_FTP = intPreferencesKey("current_ftp")
        private val KEY_USER_WEIGHT = floatPreferencesKey("user_weight")
        private val KEY_RAMP_START_POWER = intPreferencesKey("ramp_start_power")
        private val KEY_RAMP_STEP = intPreferencesKey("ramp_step")
        private val KEY_SOUND_ALERTS = booleanPreferencesKey("sound_alerts")
        private val KEY_SCREEN_WAKE = booleanPreferencesKey("screen_wake")
        private val KEY_SHOW_MOTIVATION = booleanPreferencesKey("show_motivation")
        private val KEY_FTP_CALC_METHOD = stringPreferencesKey("ftp_calc_method")
        private val KEY_ZONE_TOLERANCE = intPreferencesKey("zone_tolerance")
        private val KEY_WARMUP_DURATION = intPreferencesKey("warmup_duration")
        private val KEY_COOLDOWN_DURATION = intPreferencesKey("cooldown_duration")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_SHOW_CHECKLIST = booleanPreferencesKey("show_checklist")

        // History key
        private val KEY_TEST_HISTORY = stringPreferencesKey("test_history")

        // Active test session key
        private val KEY_ACTIVE_SESSION = stringPreferencesKey("active_session")

        // Default values
        const val DEFAULT_FTP = 200
        const val DEFAULT_USER_WEIGHT = 70f // kg
        const val DEFAULT_RAMP_START = 150
        const val DEFAULT_RAMP_STEP = 20
        const val DEFAULT_ZONE_TOLERANCE = 5
        const val DEFAULT_WARMUP_DURATION = 5
        const val DEFAULT_COOLDOWN_DURATION = 5

        // Validation bounds
        const val MIN_FTP = 50
        const val MAX_FTP = 999
        const val MIN_RAMP_START = 50
        const val MAX_RAMP_START = 300
        const val MIN_RAMP_STEP = 5
        const val MAX_RAMP_STEP = 50
        const val MIN_ZONE_TOLERANCE = 0
        const val MAX_ZONE_TOLERANCE = 20
        const val MIN_WARMUP_DURATION = 0
        const val MAX_WARMUP_DURATION = 30
        const val MIN_COOLDOWN_DURATION = 0
        const val MAX_COOLDOWN_DURATION = 30
    }

    // Settings data class
    data class Settings(
        val currentFtp: Int = DEFAULT_FTP,
        val userWeight: Float = DEFAULT_USER_WEIGHT,
        val rampStartPower: Int = DEFAULT_RAMP_START,
        val rampStep: Int = DEFAULT_RAMP_STEP,
        val soundAlerts: Boolean = true,
        val screenWake: Boolean = true,
        val showMotivation: Boolean = true,
        val ftpCalcMethod: FtpCalcMethod = FtpCalcMethod.STANDARD,
        val zoneTolerance: Int = DEFAULT_ZONE_TOLERANCE,
        val warmupDuration: Int = DEFAULT_WARMUP_DURATION,
        val cooldownDuration: Int = DEFAULT_COOLDOWN_DURATION,
        val language: AppLanguage = AppLanguage.SYSTEM,
        val theme: AppTheme = AppTheme.ORANGE,
        val showChecklist: Boolean = true
    ) {
        /** Calculate W/kg based on current FTP and weight */
        val wattsPerKg: Double
            get() = if (userWeight > 0) currentFtp / userWeight.toDouble() else 0.0
    }

    enum class AppTheme(val displayName: String) {
        ORANGE("Orange"),
        BLUE("Blue")
    }

    enum class AppLanguage(val code: String, val displayName: String) {
        SYSTEM("", "System"),
        ENGLISH("en", "English"),
        SPANISH("es", "Español"),
        GERMAN("de", "Deutsch"),
        FRENCH("fr", "Français"),
        ITALIAN("it", "Italiano"),
        PORTUGUESE("pt", "Português"),
        DUTCH("nl", "Nederlands"),
        JAPANESE("ja", "日本語"),
        CHINESE("zh", "中文"),
        RUSSIAN("ru", "Русский")
    }

    enum class FtpCalcMethod {
        CONSERVATIVE,
        STANDARD,
        AGGRESSIVE
    }

    // Flow of settings
    val settingsFlow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            currentFtp = prefs[KEY_CURRENT_FTP] ?: DEFAULT_FTP,
            userWeight = prefs[KEY_USER_WEIGHT] ?: DEFAULT_USER_WEIGHT,
            rampStartPower = prefs[KEY_RAMP_START_POWER] ?: DEFAULT_RAMP_START,
            rampStep = prefs[KEY_RAMP_STEP] ?: DEFAULT_RAMP_STEP,
            soundAlerts = prefs[KEY_SOUND_ALERTS] ?: true,
            screenWake = prefs[KEY_SCREEN_WAKE] ?: true,
            showMotivation = prefs[KEY_SHOW_MOTIVATION] ?: true,
            ftpCalcMethod = prefs[KEY_FTP_CALC_METHOD]?.let {
                FtpCalcMethod.valueOf(it)
            } ?: FtpCalcMethod.STANDARD,
            zoneTolerance = prefs[KEY_ZONE_TOLERANCE] ?: DEFAULT_ZONE_TOLERANCE,
            warmupDuration = prefs[KEY_WARMUP_DURATION] ?: DEFAULT_WARMUP_DURATION,
            cooldownDuration = prefs[KEY_COOLDOWN_DURATION] ?: DEFAULT_COOLDOWN_DURATION,
            language = prefs[KEY_LANGUAGE]?.let {
                try { AppLanguage.valueOf(it) } catch (e: Exception) { AppLanguage.SYSTEM }
            } ?: AppLanguage.SYSTEM,
            theme = prefs[KEY_THEME]?.let {
                try { AppTheme.valueOf(it) } catch (e: Exception) { AppTheme.ORANGE }
            } ?: AppTheme.ORANGE,
            showChecklist = prefs[KEY_SHOW_CHECKLIST] ?: true
        )
    }

    // Flow of test history
    val testHistoryFlow: Flow<TestHistoryData> = context.dataStore.data.map { prefs ->
        prefs[KEY_TEST_HISTORY]?.let { jsonString ->
            try {
                json.decodeFromString<TestHistoryData>(jsonString)
            } catch (e: Exception) {
                TestHistoryData()
            }
        } ?: TestHistoryData()
    }

    // Update individual settings with validation
    suspend fun updateCurrentFtp(ftp: Int) {
        val validFtp = ftp.coerceIn(MIN_FTP, MAX_FTP)
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENT_FTP] = validFtp
        }
    }

    suspend fun updateUserWeight(weight: Float) {
        val validWeight = weight.coerceIn(30f, 200f) // Reasonable bounds
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_WEIGHT] = validWeight
        }
    }

    suspend fun updateRampStartPower(power: Int) {
        val validPower = power.coerceIn(MIN_RAMP_START, MAX_RAMP_START)
        context.dataStore.edit { prefs ->
            prefs[KEY_RAMP_START_POWER] = validPower
        }
    }

    suspend fun updateRampStep(step: Int) {
        val validStep = step.coerceIn(MIN_RAMP_STEP, MAX_RAMP_STEP)
        context.dataStore.edit { prefs ->
            prefs[KEY_RAMP_STEP] = validStep
        }
    }

    suspend fun updateSoundAlerts(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ALERTS] = enabled
        }
    }

    suspend fun updateScreenWake(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SCREEN_WAKE] = enabled
        }
    }

    suspend fun updateShowMotivation(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_MOTIVATION] = enabled
        }
    }

    suspend fun updateFtpCalcMethod(method: FtpCalcMethod) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FTP_CALC_METHOD] = method.name
        }
    }

    suspend fun updateZoneTolerance(tolerance: Int) {
        val validTolerance = tolerance.coerceIn(MIN_ZONE_TOLERANCE, MAX_ZONE_TOLERANCE)
        context.dataStore.edit { prefs ->
            prefs[KEY_ZONE_TOLERANCE] = validTolerance
        }
    }

    suspend fun updateWarmupDuration(minutes: Int) {
        val validMinutes = minutes.coerceIn(MIN_WARMUP_DURATION, MAX_WARMUP_DURATION)
        context.dataStore.edit { prefs ->
            prefs[KEY_WARMUP_DURATION] = validMinutes
        }
    }

    suspend fun updateCooldownDuration(minutes: Int) {
        val validMinutes = minutes.coerceIn(MIN_COOLDOWN_DURATION, MAX_COOLDOWN_DURATION)
        context.dataStore.edit { prefs ->
            prefs[KEY_COOLDOWN_DURATION] = validMinutes
        }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.name
        }
    }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun updateShowChecklist(show: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_CHECKLIST] = show
        }
    }

    // Save test result to history
    suspend fun saveTestResult(result: TestResult) {
        context.dataStore.edit { prefs ->
            val currentHistory = prefs[KEY_TEST_HISTORY]?.let {
                try {
                    json.decodeFromString<TestHistoryData>(it)
                } catch (e: Exception) {
                    TestHistoryData()
                }
            } ?: TestHistoryData()

            val updatedHistory = currentHistory.addResult(result)
            prefs[KEY_TEST_HISTORY] = json.encodeToString(updatedHistory)
        }
    }

    // Clear test history and reset FTP to default
    suspend fun clearTestHistory() {
        context.dataStore.edit { prefs ->
            prefs[KEY_TEST_HISTORY] = json.encodeToString(TestHistoryData())
            prefs[KEY_CURRENT_FTP] = DEFAULT_FTP
        }
    }

    // Save/restore active session
    suspend fun saveActiveSession(session: TestSession?) {
        context.dataStore.edit { prefs ->
            if (session != null) {
                prefs[KEY_ACTIVE_SESSION] = json.encodeToString(session)
            } else {
                prefs.remove(KEY_ACTIVE_SESSION)
            }
        }
    }

    val activeSessionFlow: Flow<TestSession?> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_SESSION]?.let {
            try {
                json.decodeFromString<TestSession>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
}
