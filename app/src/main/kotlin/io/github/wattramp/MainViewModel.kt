package io.github.wattramp

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.wattramp.data.*
import io.github.wattramp.engine.TestEngine
import io.github.wattramp.engine.TestState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Main ViewModel that manages test state and service connection.
 * Survives configuration changes and properly handles lifecycle.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    // Service connection state
    private val _extensionBound = MutableStateFlow(false)
    val extensionBound: StateFlow<Boolean> = _extensionBound.asStateFlow()

    private var boundExtension: WattRampExtension? = null

    // Demo mode state (when Karoo extension is not available)
    private val _demoTestState = MutableStateFlow<TestState>(TestState.Idle)

    // Session state - survives configuration changes
    private val _sessionTestStarted = MutableStateFlow(false)
    val sessionTestStarted: StateFlow<Boolean> = _sessionTestStarted.asStateFlow()

    private val _sessionHasNavigated = MutableStateFlow(false)
    val sessionHasNavigated: StateFlow<Boolean> = _sessionHasNavigated.asStateFlow()

    // Whether we're in demo mode (no Karoo extension)
    val isDemoMode: Boolean
        get() = boundExtension == null && !_extensionBound.value

    // Combined test state - from extension or demo
    val testState: StateFlow<TestState> = combine(
        _extensionBound,
        _demoTestState
    ) { bound, demoState ->
        if (bound && boundExtension != null) {
            boundExtension?.testEngine?.state?.value ?: demoState
        } else {
            demoState
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TestState.Idle)

    // Delegate test state to extension when available
    val activeTestState: Flow<TestState>
        get() = if (boundExtension != null) {
            boundExtension!!.testEngine.state
        } else {
            _demoTestState
        }

    // Settings flow
    val settings = preferencesRepository.settingsFlow

    // History flow
    val testHistory = preferencesRepository.testHistoryFlow

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // The extension is a KarooExtension which doesn't use standard binding
            // We rely on the singleton pattern instead
            checkExtensionAvailability()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundExtension = null
            _extensionBound.value = false
        }
    }

    init {
        // Check for extension availability periodically at startup
        viewModelScope.launch {
            // Initial check
            checkExtensionAvailability()

            // Retry a few times if not available (extension might still be starting)
            repeat(5) {
                if (!_extensionBound.value) {
                    delay(500)
                    checkExtensionAvailability()
                }
            }
        }

        // Observe test state changes for auto-save active session
        viewModelScope.launch {
            activeTestState.collect { state ->
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
            }
        }
    }

    /**
     * Check if WattRampExtension singleton is available.
     */
    private fun checkExtensionAvailability() {
        val extension = WattRampExtension.instance
        if (extension != null) {
            boundExtension = extension
            _extensionBound.value = true
        }
    }

    /**
     * Start a new test with the specified protocol.
     */
    fun startTest(protocol: ProtocolType) {
        _sessionTestStarted.value = true
        _sessionHasNavigated.value = false

        val extension = boundExtension
        if (extension != null) {
            // Real Karoo mode
            extension.testEngine.startTest(protocol)
        } else {
            // Demo mode - simulate test
            startDemoTest(protocol)
        }
    }

    /**
     * Simulate a test in demo mode.
     */
    private fun startDemoTest(protocol: ProtocolType) {
        viewModelScope.launch {
            val currentFtp = preferencesRepository.settingsFlow.first().currentFtp

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
                val progressState = demoRunningState.copy(
                    elapsedMs = i * 1000L,
                    timeRemainingInInterval = (5 * 60 * 1000L) - (i * 1000L),
                    currentStep = if (protocol == ProtocolType.RAMP) i + 1 else null
                )
                _demoTestState.value = progressState
            }

            // Generate demo result
            val newFtp = (currentFtp * (0.95 + Math.random() * 0.15)).toInt()

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

        val extension = boundExtension
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
        viewModelScope.launch {
            preferencesRepository.updateCurrentFtp(ftp)
        }
    }

    /**
     * Save test result to history.
     */
    fun saveTestResult(result: TestResult) {
        viewModelScope.launch {
            preferencesRepository.saveTestResult(result)
        }
    }

    /**
     * Update ramp start power.
     */
    fun updateRampStartPower(power: Int) {
        viewModelScope.launch {
            preferencesRepository.updateRampStartPower(power)
        }
    }

    /**
     * Update ramp step.
     */
    fun updateRampStep(step: Int) {
        viewModelScope.launch {
            preferencesRepository.updateRampStep(step)
        }
    }

    /**
     * Update sound alerts setting.
     */
    fun updateSoundAlerts(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateSoundAlerts(enabled)
        }
    }

    /**
     * Update screen wake setting.
     */
    fun updateScreenWake(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateScreenWake(enabled)
        }
    }

    /**
     * Update show motivation setting.
     */
    fun updateShowMotivation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowMotivation(enabled)
        }
    }

    /**
     * Update warmup duration.
     */
    fun updateWarmupDuration(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateWarmupDuration(minutes)
        }
    }

    /**
     * Update cooldown duration.
     */
    fun updateCooldownDuration(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateCooldownDuration(minutes)
        }
    }

    /**
     * Update language setting.
     */
    fun updateLanguage(language: PreferencesRepository.AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.updateLanguage(language)
        }
    }

    /**
     * Update theme setting.
     */
    fun updateTheme(theme: PreferencesRepository.AppTheme) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(theme)
        }
    }

    /**
     * Clear test history.
     */
    fun clearTestHistory() {
        viewModelScope.launch {
            preferencesRepository.clearTestHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        boundExtension = null
    }
}
