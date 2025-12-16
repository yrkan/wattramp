package io.github.wattramp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wattramp.data.*
import io.github.wattramp.engine.TestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.random.Random

/**
 * Main ViewModel that manages test state and service connection.
 * Survives configuration changes and properly handles lifecycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    // Extension reference with thread-safe access using synchronized block
    private val extensionLock = Any()
    @Volatile
    private var _boundExtension: WattRampExtension? = null

    private fun getExtensionSafely(): WattRampExtension? {
        // Fast path - already bound
        _boundExtension?.let { return it }

        // Slow path - try to bind
        return synchronized(extensionLock) {
            // Double-check after acquiring lock
            _boundExtension ?: WattRampExtension.instance?.also {
                _boundExtension = it
                _extensionBound.value = true
            }
        }
    }

    // Service connection state
    private val _extensionBound = MutableStateFlow(false)
    val extensionBound: StateFlow<Boolean> = _extensionBound.asStateFlow()

    // Demo mode state (when Karoo extension is not available)
    private val _demoTestState = MutableStateFlow<TestState>(TestState.Idle)

    // Session state - survives configuration changes
    private val _sessionTestStarted = MutableStateFlow(false)
    val sessionTestStarted: StateFlow<Boolean> = _sessionTestStarted.asStateFlow()

    private val _sessionHasNavigated = MutableStateFlow(false)
    val sessionHasNavigated: StateFlow<Boolean> = _sessionHasNavigated.asStateFlow()

    // Whether we're in demo mode (no Karoo extension)
    val isDemoMode: Boolean
        get() = getExtensionSafely() == null

    /**
     * Active test state - properly switches between extension and demo state.
     * Uses flatMapLatest to correctly observe the right flow.
     */
    val activeTestState: StateFlow<TestState> = _extensionBound
        .flatMapLatest { bound ->
            if (bound) {
                // Get extension safely
                val ext = getExtensionSafely()
                if (ext != null) {
                    ext.testEngine.state
                } else {
                    _demoTestState
                }
            } else {
                _demoTestState
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TestState.Idle
        )

    // Settings flow - cached to avoid multiple subscriptions
    val settings: StateFlow<PreferencesRepository.Settings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PreferencesRepository.Settings()
        )

    // History flow
    val testHistory: StateFlow<TestHistoryData> = preferencesRepository.testHistoryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TestHistoryData()
        )

    init {
        // Check for extension availability - non-blocking approach
        viewModelScope.launch(Dispatchers.Default) {
            // Initial check
            checkExtensionAvailability()

            // Quick retry with shorter delays (total max 1.5 sec instead of 3 sec)
            repeat(5) {
                if (!_extensionBound.value) {
                    delay(100)
                    checkExtensionAvailability()
                }
            }

            // If still not available, do a final slower retry
            if (!_extensionBound.value) {
                delay(500)
                checkExtensionAvailability()
            }
        }

        // Observe test state changes for auto-save active session
        viewModelScope.launch(Dispatchers.Default) {
            activeTestState.collect { state ->
                withContext(Dispatchers.IO) {
                    try {
                        when (state) {
                            is TestState.Running -> {
                                // Save active session for recovery
                                preferencesRepository.saveActiveSession(
                                    TestSession(
                                        protocol = state.protocol,
                                        startTimeMs = System.currentTimeMillis() - state.elapsedMs,
                                        currentPhase = state.phase
                                    )
                                )
                            }
                            is TestState.Completed, is TestState.Failed, is TestState.Idle -> {
                                // Clear active session
                                preferencesRepository.saveActiveSession(null)
                            }
                            is TestState.Paused -> {
                                // Keep session active but mark as paused
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore save errors - not critical
                    }
                }
            }
        }
    }

    /**
     * Check if WattRampExtension singleton is available.
     */
    private fun checkExtensionAvailability() {
        synchronized(extensionLock) {
            val extension = WattRampExtension.instance
            if (extension != null && _boundExtension == null) {
                _boundExtension = extension
                _extensionBound.value = true
            }
        }
    }

    /**
     * Start a new test with the specified protocol.
     */
    fun startTest(protocol: ProtocolType) {
        val extension = getExtensionSafely()
        if (extension != null) {
            // Real Karoo mode - check if test actually started
            val started = extension.testEngine.startTest(protocol)
            if (started) {
                _sessionTestStarted.value = true
                _sessionHasNavigated.value = false
            }
            // If not started, TestEngine already set state to Failed
            // which will be observed via activeTestState
        } else {
            // Demo mode - simulate test
            _sessionTestStarted.value = true
            _sessionHasNavigated.value = false
            startDemoTest(protocol)
        }
    }

    /**
     * Simulate a test in demo mode.
     */
    private fun startDemoTest(protocol: ProtocolType) {
        viewModelScope.launch {
            val currentFtp = settings.value.currentFtp

            // Create a running state to show progress
            val demoRunningState = TestState.Running(
                protocol = protocol,
                phase = TestPhase.WARMUP,
                currentInterval = Interval(
                    name = "Warmup",
                    phase = TestPhase.WARMUP,
                    durationMs = 5 * 60 * 1000L,
                    targetPowerPercent = 0.5
                ),
                intervalIndex = 0,
                elapsedMs = 0,
                timeRemainingInInterval = 5 * 60 * 1000L,
                currentPower = (currentFtp * 0.5).toInt(),
                targetPower = (currentFtp * 0.5).toInt(),
                currentStep = if (protocol == ProtocolType.RAMP) 1 else null,
                estimatedTotalSteps = if (protocol == ProtocolType.RAMP) 15 else null
            )

            // Show running state
            _demoTestState.value = demoRunningState

            // Simulate progress updates
            for (i in 1..3) {
                delay(1000)
                _demoTestState.value = demoRunningState.copy(
                    elapsedMs = i * 1000L,
                    timeRemainingInInterval = (5 * 60 * 1000L) - (i * 1000L),
                    currentStep = if (protocol == ProtocolType.RAMP) i + 1 else null
                )
            }

            // Generate demo result using Kotlin's Random
            val newFtp = (currentFtp * (0.95 + Random.nextDouble() * 0.15)).toInt()

            val demoResult = TestResult(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                protocol = protocol,
                calculatedFtp = newFtp,
                previousFtp = currentFtp,
                testDurationMs = when (protocol) {
                    ProtocolType.RAMP -> 18 * 60 * 1000L
                    ProtocolType.TWENTY_MINUTE -> 55 * 60 * 1000L
                    ProtocolType.EIGHT_MINUTE -> 45 * 60 * 1000L
                },
                maxOneMinutePower = if (protocol == ProtocolType.RAMP) (newFtp / 0.75).toInt() else null,
                averagePower = if (protocol != ProtocolType.RAMP) (newFtp / 0.95).toInt() else null,
                stepsCompleted = if (protocol == ProtocolType.RAMP) ((newFtp - 100) / 20) else null,
                formula = when (protocol) {
                    ProtocolType.RAMP -> "Max 1-min × 0.75"
                    ProtocolType.TWENTY_MINUTE -> "20-min avg × 0.95"
                    ProtocolType.EIGHT_MINUTE -> "8-min avg × 0.90"
                },
                saved = false
            )

            _demoTestState.value = TestState.Completed(demoResult)
        }
    }

    /**
     * Dismiss results and return to idle state.
     */
    fun dismissResults() {
        _sessionTestStarted.value = false
        _sessionHasNavigated.value = false

        val extension = getExtensionSafely()
        if (extension != null) {
            extension.testEngine.dismissResults()
        } else {
            _demoTestState.value = TestState.Idle
        }
    }

    /**
     * Mark that navigation to result screen has occurred.
     */
    fun setHasNavigated(value: Boolean) {
        _sessionHasNavigated.value = value
    }

    /**
     * Update current FTP in preferences.
     */
    fun updateCurrentFtp(ftp: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateCurrentFtp(ftp)
        }
    }

    /**
     * Save test result to history.
     */
    fun saveTestResult(result: TestResult) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.saveTestResult(result)
        }
    }

    /**
     * Update ramp start power.
     */
    fun updateRampStartPower(power: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateRampStartPower(power)
        }
    }

    /**
     * Update ramp step.
     */
    fun updateRampStep(step: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateRampStep(step)
        }
    }

    /**
     * Update sound alerts setting.
     */
    fun updateSoundAlerts(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateSoundAlerts(enabled)
        }
    }

    /**
     * Update screen wake setting.
     */
    fun updateScreenWake(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateScreenWake(enabled)
        }
    }

    /**
     * Update show motivation setting.
     */
    fun updateShowMotivation(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateShowMotivation(enabled)
        }
    }

    /**
     * Update warmup duration.
     */
    fun updateWarmupDuration(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateWarmupDuration(minutes)
        }
    }

    /**
     * Update cooldown duration.
     */
    fun updateCooldownDuration(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateCooldownDuration(minutes)
        }
    }

    /**
     * Update language setting.
     */
    fun updateLanguage(language: PreferencesRepository.AppLanguage) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateLanguage(language)
        }
    }

    /**
     * Update theme setting.
     */
    fun updateTheme(theme: PreferencesRepository.AppTheme) {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.updateTheme(theme)
        }
    }

    /**
     * Clear test history.
     */
    fun clearTestHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesRepository.clearTestHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        synchronized(extensionLock) {
            _boundExtension = null
        }
    }
}
