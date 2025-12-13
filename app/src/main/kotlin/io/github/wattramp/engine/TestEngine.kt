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

/**
 * Main test engine that manages the FTP test state machine.
 */
class TestEngine(private val extension: WattRampExtension) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val preferencesRepository by lazy { PreferencesRepository(extension) }
    private val alertManager by lazy { AlertManager(extension) }

    // Current state
    private val _state = MutableStateFlow<TestState>(TestState.Idle)
    val state: StateFlow<TestState> = _state.asStateFlow()

    // Current protocol
    private var currentProtocol: TestProtocol? = null

    // Test timing
    private var testStartTimeMs: Long = 0L
    private var pausedTimeMs: Long = 0L
    private var totalPausedMs: Long = 0L

    // Current power, HR, cadence and settings
    private var currentPower: Int = 0
    private var currentHeartRate: Int = 0
    private var currentCadence: Int = 0
    private var currentFtp: Int = 200
    private var settings: PreferencesRepository.Settings = PreferencesRepository.Settings()

    // Power tracking for avg/max
    private val powerSamples = mutableListOf<Int>()
    private var maxOneMinutePower: Int = 0
    private var lastMinutePowerSamples = mutableListOf<Int>()

    // Karoo system consumer IDs
    private var powerConsumerId: String? = null
    private var heartRateConsumerId: String? = null
    private var cadenceConsumerId: String? = null
    private var rideStateConsumerId: String? = null
    private var userProfileConsumerId: String? = null

    // Update job
    private var updateJob: Job? = null

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
        if (_state.value !is TestState.Idle) {
            stopTest(FailureReason.USER_STOPPED)
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

        // Reset power tracking
        powerSamples.clear()
        maxOneMinutePower = 0
        lastMinutePowerSamples.clear()
        currentHeartRate = 0
        currentCadence = 0

        // Connect to Karoo streams
        connectToKarooStreams()

        // Start update loop
        startUpdateLoop()

        // Initial state
        updateState()
    }

    /**
     * Stop the current test.
     */
    fun stopTest(reason: FailureReason = FailureReason.USER_STOPPED) {
        val protocol = currentProtocol ?: return

        updateJob?.cancel()
        disconnectFromKarooStreams()

        val elapsedMs = getElapsedTimeMs()

        // Get partial result if test has been running
        val partialResult = if (elapsedMs > 60_000L) {
            protocol.getTestResult(
                startTime = testStartTimeMs,
                previousFtp = currentFtp,
                method = settings.ftpCalcMethod
            )
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
        val protocol = currentProtocol ?: return

        updateJob?.cancel()
        disconnectFromKarooStreams()

        val result = protocol.getTestResult(
            startTime = testStartTimeMs,
            previousFtp = currentFtp,
            method = settings.ftpCalcMethod
        )

        _state.value = TestState.Completed(result)

        // Show completion alert
        alertManager.showCompleteAlert(
            calculatedFtp = result.calculatedFtp,
            previousFtp = result.previousFtp,
            soundEnabled = settings.soundAlerts,
            screenWakeEnabled = settings.screenWake
        )

        // Save result to history
        scope.launch {
            preferencesRepository.saveTestResult(result)
        }

        currentProtocol = null
    }

    /**
     * Save the FTP result to Karoo and preferences.
     */
    fun saveFtpResult(ftp: Int) {
        scope.launch {
            preferencesRepository.updateCurrentFtp(ftp)
        }
        // Note: Updating Karoo's user profile FTP would require additional SDK calls
        // if supported by the API
    }

    /**
     * Dismiss results and return to idle state.
     */
    fun dismissResults() {
        _state.value = TestState.Idle
    }

    private fun connectToKarooStreams() {
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
            if (currentFtp == 200) { // Only update if using default
                currentFtp = profile.ftp
            }
        }
    }

    private fun disconnectFromKarooStreams() {
        powerConsumerId?.let { extension.karooSystem.removeConsumer(it) }
        heartRateConsumerId?.let { extension.karooSystem.removeConsumer(it) }
        cadenceConsumerId?.let { extension.karooSystem.removeConsumer(it) }
        rideStateConsumerId?.let { extension.karooSystem.removeConsumer(it) }
        userProfileConsumerId?.let { extension.karooSystem.removeConsumer(it) }

        powerConsumerId = null
        heartRateConsumerId = null
        cadenceConsumerId = null
        rideStateConsumerId = null
        userProfileConsumerId = null
    }

    private fun onPowerUpdate(power: Int) {
        currentPower = power
        val protocol = currentProtocol ?: return
        val elapsedMs = getElapsedTimeMs()

        // Feed power to protocol
        protocol.onPowerSample(power, elapsedMs)

        // Track power for avg/max calculations
        if (power > 0) {
            powerSamples.add(power)
            lastMinutePowerSamples.add(power)

            // Keep only last ~60 samples for 1-minute rolling average (assuming ~1 sample/sec)
            while (lastMinutePowerSamples.size > 60) {
                lastMinutePowerSamples.removeAt(0)
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
                if (currentState is TestState.Running) {
                    stopTest(FailureReason.RIDE_ENDED)
                }
            }
        }
    }

    private fun pauseTest() {
        val currentState = _state.value
        if (currentState is TestState.Running) {
            pausedTimeMs = System.currentTimeMillis()
            _state.value = TestState.Paused(
                protocol = currentState.protocol,
                elapsedMs = getElapsedTimeMs(),
                pausedAt = pausedTimeMs
            )
        }
    }

    private fun resumeTest() {
        val currentState = _state.value
        if (currentState is TestState.Paused) {
            totalPausedMs += System.currentTimeMillis() - pausedTimeMs
            updateState()
        }
    }

    private fun startUpdateLoop() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (isActive) {
                updateState()
                delay(1000) // Update every second
            }
        }
    }

    private fun updateState() {
        val protocol = currentProtocol ?: return
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

        _state.value = runningState

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
        disconnectFromKarooStreams()
        scope.cancel()
    }
}
