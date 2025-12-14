package io.github.wattramp.engine

import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.*
import io.github.wattramp.protocols.*
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnStreamState
import io.hammerhead.karooext.models.RideState
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.ArrayDeque

/**
 * Main test engine that manages the FTP test state machine.
 */
class TestEngine(private val extension: WattRampExtension) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferencesRepository by lazy { PreferencesRepository(extension) }
    private val alertManager by lazy { AlertManager(extension) }

    // Current state with thread-safe access
    private val _state = MutableStateFlow<TestState>(TestState.Idle)
    val state: StateFlow<TestState> = _state.asStateFlow()

    // Current protocol
    @Volatile
    private var currentProtocol: TestProtocol? = null

    // Test timing
    @Volatile
    private var testStartTimeMs: Long = 0L
    @Volatile
    private var pausedTimeMs: Long = 0L
    @Volatile
    private var totalPausedMs: Long = 0L

    // Current power, HR, cadence and settings
    @Volatile
    private var currentPower: Int = 0
    @Volatile
    private var currentHeartRate: Int = 0
    @Volatile
    private var currentCadence: Int = 0
    @Volatile
    private var currentFtp: Int = 200
    @Volatile
    private var settings: PreferencesRepository.Settings = PreferencesRepository.Settings()

    // Power tracking for avg/max - use ArrayDeque for efficient removal from front
    private val powerSamples = mutableListOf<Int>()
    private var maxOneMinutePower: Int = 0
    private val lastMinutePowerSamples = ArrayDeque<Int>(65) // Slightly more than 60 for safety

    // Karoo system consumer IDs
    @Volatile
    private var powerConsumerId: String? = null
    @Volatile
    private var heartRateConsumerId: String? = null
    @Volatile
    private var cadenceConsumerId: String? = null
    @Volatile
    private var rideStateConsumerId: String? = null
    @Volatile
    private var userProfileConsumerId: String? = null

    // Update job
    private var updateJob: Job? = null

    // Lock for state updates
    private val stateLock = Any()

    init {
        // Observe settings
        scope.launch {
            preferencesRepository.settingsFlow.collect { newSettings ->
                settings = newSettings
                currentFtp = newSettings.currentFtp
            }
        }
    }

    /**
     * Start a new FTP test with the specified protocol.
     */
    fun startTest(protocolType: ProtocolType) {
        synchronized(stateLock) {
            if (_state.value !is TestState.Idle) {
                stopTestInternal(FailureReason.USER_STOPPED)
            }

            // Create protocol instance
            currentProtocol = when (protocolType) {
                ProtocolType.RAMP -> RampTest(
                    startPower = settings.rampStartPower,
                    stepIncrement = settings.rampStep,
                    warmupDurationMin = settings.warmupDuration,
                    cooldownDurationMin = settings.cooldownDuration
                )
                ProtocolType.TWENTY_MINUTE -> TwentyMinTest(currentFtp)
                ProtocolType.EIGHT_MINUTE -> EightMinTest(currentFtp)
            }

            currentProtocol?.reset()
            alertManager.reset()

            testStartTimeMs = System.currentTimeMillis()
            totalPausedMs = 0L
            pausedTimeMs = 0L

            // Reset power tracking
            powerSamples.clear()
            maxOneMinutePower = 0
            lastMinutePowerSamples.clear()
            currentHeartRate = 0
            currentCadence = 0
            currentPower = 0

            // Connect to Karoo streams
            connectToKarooStreams()

            // Start update loop
            startUpdateLoop()

            // Initial state
            updateState()
        }
    }

    /**
     * Stop the current test.
     */
    fun stopTest(reason: FailureReason = FailureReason.USER_STOPPED) {
        synchronized(stateLock) {
            stopTestInternal(reason)
        }
    }

    private fun stopTestInternal(reason: FailureReason) {
        val protocol = currentProtocol ?: return

        updateJob?.cancel()
        updateJob = null
        disconnectFromKarooStreams()

        val elapsedMs = getElapsedTimeMs()

        // Get partial result if test has been running
        val partialResult = if (elapsedMs > 60_000L) {
            try {
                protocol.getTestResult(
                    startTime = testStartTimeMs,
                    previousFtp = currentFtp,
                    method = settings.ftpCalcMethod
                )
            } catch (e: Exception) {
                null
            }
        } else null

        _state.value = TestState.Failed(
            protocol = protocol.type,
            reason = reason,
            partialResult = partialResult
        )

        currentProtocol = null
    }

    /**
     * Mark test as complete and show results.
     */
    private fun completeTest() {
        synchronized(stateLock) {
            val protocol = currentProtocol ?: return

            updateJob?.cancel()
            updateJob = null
            disconnectFromKarooStreams()

            val result = try {
                protocol.getTestResult(
                    startTime = testStartTimeMs,
                    previousFtp = currentFtp,
                    method = settings.ftpCalcMethod
                )
            } catch (e: Exception) {
                // If result calculation fails, create a minimal result
                TestResult(
                    id = java.util.UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    protocol = protocol.type,
                    calculatedFtp = currentFtp,
                    previousFtp = currentFtp,
                    testDurationMs = getElapsedTimeMs(),
                    maxOneMinutePower = maxOneMinutePower,
                    averagePower = if (powerSamples.isNotEmpty()) powerSamples.average().toInt() else 0,
                    stepsCompleted = null,
                    formula = "Error calculating",
                    saved = false
                )
            }

            _state.value = TestState.Completed(result)

            // Show completion alert
            alertManager.showCompleteAlert(
                calculatedFtp = result.calculatedFtp,
                previousFtp = result.previousFtp,
                soundEnabled = settings.soundAlerts,
                screenWakeEnabled = settings.screenWake
            )

            // Save result to history
            scope.launch(Dispatchers.IO) {
                try {
                    preferencesRepository.saveTestResult(result)
                } catch (e: Exception) {
                    // Log error but don't crash
                }
            }

            currentProtocol = null
        }
    }

    /**
     * Save the FTP result to Karoo and preferences.
     */
    fun saveFtpResult(ftp: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                preferencesRepository.updateCurrentFtp(ftp)
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    /**
     * Dismiss results and return to idle state.
     */
    fun dismissResults() {
        _state.value = TestState.Idle
    }

    private fun connectToKarooStreams() {
        try {
            // Subscribe to power data
            powerConsumerId = extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.POWER)
            ) { event: OnStreamState ->
                when (val streamState = event.state) {
                    is StreamState.Streaming -> {
                        streamState.dataPoint.singleValue?.toInt()?.let { power ->
                            onPowerUpdate(power)
                        }
                    }
                    else -> { /* Ignore other states */ }
                }
            }

            // Subscribe to heart rate data
            heartRateConsumerId = extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.HEART_RATE)
            ) { event: OnStreamState ->
                when (val streamState = event.state) {
                    is StreamState.Streaming -> {
                        streamState.dataPoint.singleValue?.toInt()?.let { hr ->
                            currentHeartRate = hr
                        }
                    }
                    else -> { /* Ignore other states */ }
                }
            }

            // Subscribe to cadence data
            cadenceConsumerId = extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.CADENCE)
            ) { event: OnStreamState ->
                when (val streamState = event.state) {
                    is StreamState.Streaming -> {
                        streamState.dataPoint.singleValue?.toInt()?.let { rpm ->
                            currentCadence = rpm
                        }
                    }
                    else -> { /* Ignore other states */ }
                }
            }

            // Subscribe to ride state
            rideStateConsumerId = extension.karooSystem.addConsumer<RideState> { rideState ->
                onRideStateChange(rideState)
            }

            // Get user profile (initial FTP)
            userProfileConsumerId = extension.karooSystem.addConsumer<UserProfile> { profile ->
                // Safe access with null check
                val profileFtp = try { profile.ftp } catch (e: Exception) { 0 }
                if (profileFtp > 0 && currentFtp == 200) {
                    currentFtp = profileFtp
                }
            }
        } catch (e: Exception) {
            // Log error but continue - some streams might still work
        }
    }

    private fun disconnectFromKarooStreams() {
        // Disconnect each consumer with individual try-catch to ensure all are attempted
        powerConsumerId?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) { /* ignore */ }
        }
        heartRateConsumerId?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) { /* ignore */ }
        }
        cadenceConsumerId?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) { /* ignore */ }
        }
        rideStateConsumerId?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) { /* ignore */ }
        }
        userProfileConsumerId?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) { /* ignore */ }
        }

        powerConsumerId = null
        heartRateConsumerId = null
        cadenceConsumerId = null
        rideStateConsumerId = null
        userProfileConsumerId = null
    }

    private fun onPowerUpdate(power: Int) {
        currentPower = power
        val protocol = currentProtocol ?: return

        // Only process if we're in Running state
        if (_state.value !is TestState.Running) return

        val elapsedMs = getElapsedTimeMs()

        // Feed power to protocol
        protocol.onPowerSample(power, elapsedMs)

        // Track power for avg/max calculations
        if (power > 0) {
            powerSamples.add(power)
            lastMinutePowerSamples.addLast(power)

            // Keep only last ~60 samples for 1-minute rolling average (assuming ~1 sample/sec)
            while (lastMinutePowerSamples.size > 60) {
                lastMinutePowerSamples.removeFirst()
            }

            // Update max 1-minute power
            if (lastMinutePowerSamples.size >= 30) { // At least 30 seconds of data
                val currentMinuteAvg = lastMinutePowerSamples.average().toInt()
                if (currentMinuteAvg > maxOneMinutePower) {
                    maxOneMinutePower = currentMinuteAvg
                }
            }
        }

        // Check for auto-end (Ramp test)
        val targetPower = protocol.getTargetPower(elapsedMs, currentFtp) ?: 0
        if (protocol.shouldEndTest(power, targetPower)) {
            completeTest()
        }
    }

    private fun onRideStateChange(rideState: RideState) {
        when (rideState) {
            is RideState.Paused -> pauseTest()
            RideState.Recording -> resumeTest()
            RideState.Idle -> {
                // Ride ended
                val currentState = _state.value
                if (currentState is TestState.Running || currentState is TestState.Paused) {
                    stopTest(FailureReason.RIDE_ENDED)
                }
            }
        }
    }

    private fun pauseTest() {
        synchronized(stateLock) {
            val currentState = _state.value
            if (currentState is TestState.Running) {
                pausedTimeMs = System.currentTimeMillis()
                updateJob?.cancel()
                _state.value = TestState.Paused(
                    protocol = currentState.protocol,
                    elapsedMs = getElapsedTimeMs(),
                    pausedAt = pausedTimeMs
                )
            }
        }
    }

    private fun resumeTest() {
        synchronized(stateLock) {
            val currentState = _state.value
            if (currentState is TestState.Paused) {
                totalPausedMs += System.currentTimeMillis() - pausedTimeMs

                // Restart update loop
                startUpdateLoop()

                // Immediately update state to Running
                updateState()
            }
        }
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                if (_state.value is TestState.Running || currentProtocol != null) {
                    updateState()
                }
                delay(1000) // Update every second
            }
        }
    }

    private fun updateState() {
        val protocol = currentProtocol ?: return

        // Don't update if paused
        if (_state.value is TestState.Paused) return

        val elapsedMs = getElapsedTimeMs()

        // Check if test is complete (time-based)
        val currentPhase = protocol.getCurrentPhase(elapsedMs)
        if (currentPhase == TestPhase.COMPLETED) {
            completeTest()
            return
        }

        val currentInterval = protocol.getCurrentInterval(elapsedMs) ?: return
        val targetPower = protocol.getTargetPower(elapsedMs, currentFtp)

        // Calculate average power
        val avgPower = if (powerSamples.isNotEmpty()) {
            powerSamples.average().toInt()
        } else 0

        val runningState = TestState.Running(
            protocol = protocol.type,
            phase = currentPhase,
            currentInterval = currentInterval,
            intervalIndex = protocol.getCurrentIntervalIndex(elapsedMs),
            elapsedMs = elapsedMs,
            timeRemainingInInterval = protocol.getTimeRemainingInInterval(elapsedMs),
            currentPower = currentPower,
            targetPower = targetPower,
            currentStep = (protocol as? RampTest)?.getCurrentStep(elapsedMs),
            estimatedTotalSteps = (protocol as? RampTest)?.getEstimatedTotalSteps(currentFtp),
            maxOneMinutePower = maxOneMinutePower,
            averagePower = avgPower,
            heartRate = currentHeartRate,
            cadence = currentCadence
        )

        // Only update if state actually changed (reduces unnecessary recompositions)
        val currentValue = _state.value
        if (currentValue != runningState) {
            _state.value = runningState
        }

        // Check alerts
        alertManager.checkAlerts(
            state = runningState,
            soundEnabled = settings.soundAlerts,
            screenWakeEnabled = settings.screenWake,
            motivationEnabled = settings.showMotivation
        )
    }

    private fun getElapsedTimeMs(): Long {
        return System.currentTimeMillis() - testStartTimeMs - totalPausedMs
    }

    /**
     * Get current test state for data fields.
     */
    fun getCurrentState(): TestState = _state.value

    /**
     * Check if a test is currently active.
     */
    fun isTestActive(): Boolean {
        return _state.value is TestState.Running || _state.value is TestState.Paused
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        updateJob?.cancel()
        updateJob = null
        disconnectFromKarooStreams()
        scope.cancel()
    }
}
