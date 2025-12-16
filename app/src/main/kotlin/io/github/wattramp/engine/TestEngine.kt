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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Main test engine that manages the FTP test state machine.
 * Thread-safe implementation with proper synchronization and bounded data structures.
 */
class TestEngine(private val extension: WattRampExtension) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val preferencesRepository by lazy { PreferencesRepository(extension) }
    private val alertManager by lazy { AlertManager(extension) }

    // Current state with thread-safe access
    private val _state = MutableStateFlow<TestState>(TestState.Idle)
    val state: StateFlow<TestState> = _state.asStateFlow()

    // Current protocol - using AtomicReference for thread-safe access
    private val currentProtocolRef = AtomicReference<TestProtocol?>(null)
    private val currentProtocol: TestProtocol?
        get() = currentProtocolRef.get()

    // Test timing - using atomic types for thread safety
    private val testStartTimeMs = AtomicLong(0L)
    private val pausedTimeMs = AtomicLong(0L)
    private val totalPausedMs = AtomicLong(0L)

    // Current power, HR, cadence - using atomic types for thread safety
    private val currentPower = AtomicInteger(0)
    private val currentHeartRate = AtomicInteger(0)
    private val currentCadence = AtomicInteger(0)
    private val currentFtp = AtomicInteger(200)

    // Max HR from user profile for zone calculations
    private val userMaxHr = AtomicInteger(190) // Default 190

    @Volatile
    private var settings: PreferencesRepository.Settings = PreferencesRepository.Settings()

    // Power tracking - bounded data structures for memory efficiency
    // Use thread-safe CopyOnWriteArrayList with size limit
    companion object {
        private const val MAX_POWER_SAMPLES = 4000 // ~67 minutes at 1 sample/sec
        private const val ROLLING_WINDOW_SIZE = 60 // 60 samples for 1-minute rolling average
    }

    private val powerSamples = CopyOnWriteArrayList<Int>()
    private val maxOneMinutePower = AtomicInteger(0)

    // Thread-safe bounded deque for rolling average
    private val lastMinutePowerSamples = ArrayDeque<Int>(ROLLING_WINDOW_SIZE + 5)
    private val powerSamplesLock = Any()

    // Karoo system consumer IDs - using AtomicReference for thread safety
    private val powerConsumerId = AtomicReference<String?>(null)
    private val heartRateConsumerId = AtomicReference<String?>(null)
    private val cadenceConsumerId = AtomicReference<String?>(null)
    private val rideStateConsumerId = AtomicReference<String?>(null)
    private val userProfileConsumerId = AtomicReference<String?>(null)

    // Update job
    private var updateJob: Job? = null

    // Mutex for state updates (coroutine-friendly synchronization)
    private val stateMutex = Mutex()

    init {
        // Observe settings
        scope.launch {
            preferencesRepository.settingsFlow.collect { newSettings ->
                settings = newSettings
                currentFtp.set(newSettings.currentFtp)
            }
        }
    }

    /**
     * Start a new FTP test with the specified protocol.
     */
    fun startTest(protocolType: ProtocolType) {
        scope.launch {
            stateMutex.withLock {
                if (_state.value !is TestState.Idle) {
                    stopTestInternal(FailureReason.USER_STOPPED)
                }

                // Create protocol instance
                val ftp = currentFtp.get()
                val protocol = when (protocolType) {
                    ProtocolType.RAMP -> RampTest(
                        startPower = settings.rampStartPower,
                        stepIncrement = settings.rampStep,
                        warmupDurationMin = settings.warmupDuration,
                        cooldownDurationMin = settings.cooldownDuration
                    )
                    ProtocolType.TWENTY_MINUTE -> TwentyMinTest(ftp)
                    ProtocolType.EIGHT_MINUTE -> EightMinTest(ftp)
                }

                currentProtocolRef.set(protocol)
                protocol.reset()
                alertManager.reset()

                testStartTimeMs.set(System.currentTimeMillis())
                totalPausedMs.set(0L)
                pausedTimeMs.set(0L)

                // Reset power tracking with bounded clearing
                powerSamples.clear()
                maxOneMinutePower.set(0)
                synchronized(powerSamplesLock) {
                    lastMinutePowerSamples.clear()
                }
                currentHeartRate.set(0)
                currentCadence.set(0)
                currentPower.set(0)

                // Connect to Karoo streams
                connectToKarooStreams()

                // Start update loop
                startUpdateLoop()

                // Initial state
                updateState()
            }
        }
    }

    /**
     * Stop the current test.
     */
    fun stopTest(reason: FailureReason = FailureReason.USER_STOPPED) {
        scope.launch {
            stateMutex.withLock {
                stopTestInternal(reason)
            }
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
                    startTime = testStartTimeMs.get(),
                    previousFtp = currentFtp.get(),
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

        currentProtocolRef.set(null)
    }

    /**
     * Mark test as complete and show results.
     */
    private fun completeTest() {
        scope.launch {
            stateMutex.withLock {
                val protocol = currentProtocol ?: return@withLock

                updateJob?.cancel()
                updateJob = null
                disconnectFromKarooStreams()

                val ftp = currentFtp.get()
                val maxPower = maxOneMinutePower.get()
                val avgPower = if (powerSamples.isNotEmpty()) {
                    powerSamples.toList().average().toInt()
                } else 0

                val result = try {
                    protocol.getTestResult(
                        startTime = testStartTimeMs.get(),
                        previousFtp = ftp,
                        method = settings.ftpCalcMethod
                    )
                } catch (e: Exception) {
                    // If result calculation fails, create a minimal result
                    TestResult(
                        id = java.util.UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        protocol = protocol.type,
                        calculatedFtp = ftp,
                        previousFtp = ftp,
                        testDurationMs = getElapsedTimeMs(),
                        maxOneMinutePower = maxPower,
                        averagePower = avgPower,
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
                launch(Dispatchers.IO) {
                    try {
                        preferencesRepository.saveTestResult(result)
                    } catch (e: Exception) {
                        // Log error but don't crash
                    }
                }

                currentProtocolRef.set(null)
            }
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
        scope.launch {
            stateMutex.withLock {
                _state.value = TestState.Idle
            }
        }
    }

    private fun connectToKarooStreams() {
        // Check if Karoo system is connected before subscribing
        if (!extension.karooSystem.connected) {
            android.util.Log.w("TestEngine", "KarooSystem not connected, streams may fail")
        }

        try {
            // Subscribe to power data
            powerConsumerId.set(extension.karooSystem.addConsumer(
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
            })

            // Subscribe to heart rate data
            heartRateConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.HEART_RATE)
            ) { event: OnStreamState ->
                when (val streamState = event.state) {
                    is StreamState.Streaming -> {
                        streamState.dataPoint.singleValue?.toInt()?.let { hr ->
                            currentHeartRate.set(hr)
                        }
                    }
                    else -> { /* Ignore other states */ }
                }
            })

            // Subscribe to cadence data
            cadenceConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.CADENCE)
            ) { event: OnStreamState ->
                when (val streamState = event.state) {
                    is StreamState.Streaming -> {
                        streamState.dataPoint.singleValue?.toInt()?.let { rpm ->
                            currentCadence.set(rpm)
                        }
                    }
                    else -> { /* Ignore other states */ }
                }
            })

            // Subscribe to ride state
            rideStateConsumerId.set(extension.karooSystem.addConsumer<RideState> { rideState ->
                onRideStateChange(rideState)
            })

            // Get user profile (initial FTP and max HR)
            userProfileConsumerId.set(extension.karooSystem.addConsumer<UserProfile> { profile ->
                // Safe access with null check
                try {
                    val profileFtp = profile.ftp
                    if (profileFtp > 0 && currentFtp.get() == 200) {
                        currentFtp.set(profileFtp)
                    }
                    // Extract max HR from user profile for accurate HR zone calculations
                    val maxHr = profile.maxHr
                    if (maxHr > 0) {
                        userMaxHr.set(maxHr)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("TestEngine", "Error reading user profile: ${e.message}")
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("TestEngine", "Error connecting to Karoo streams: ${e.message}")
        }
    }

    private fun disconnectFromKarooStreams() {
        // Disconnect each consumer atomically with getAndSet to prevent race conditions
        powerConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w("TestEngine", "Error removing power consumer: ${e.message}")
            }
        }
        heartRateConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w("TestEngine", "Error removing HR consumer: ${e.message}")
            }
        }
        cadenceConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w("TestEngine", "Error removing cadence consumer: ${e.message}")
            }
        }
        rideStateConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w("TestEngine", "Error removing ride state consumer: ${e.message}")
            }
        }
        userProfileConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w("TestEngine", "Error removing user profile consumer: ${e.message}")
            }
        }
    }

    private fun onPowerUpdate(power: Int) {
        currentPower.set(power)

        // Get protocol atomically to ensure consistency
        val protocol = currentProtocol ?: return

        // Only process if we're in Running state
        if (_state.value !is TestState.Running) return

        val elapsedMs = getElapsedTimeMs()

        // Feed power to protocol
        protocol.onPowerSample(power, elapsedMs)

        // Track power for avg/max calculations with bounded storage
        if (power > 0) {
            // Add to power samples with size limit to prevent unbounded growth
            if (powerSamples.size < MAX_POWER_SAMPLES) {
                powerSamples.add(power)
            }

            // Thread-safe update of rolling window
            synchronized(powerSamplesLock) {
                lastMinutePowerSamples.addLast(power)

                // Keep only last 60 samples for 1-minute rolling average
                while (lastMinutePowerSamples.size > ROLLING_WINDOW_SIZE) {
                    lastMinutePowerSamples.removeFirst()
                }

                // Update max 1-minute power atomically
                if (lastMinutePowerSamples.size >= 30) { // At least 30 seconds of data
                    val currentMinuteAvg = lastMinutePowerSamples.average().toInt()
                    maxOneMinutePower.updateAndGet { current ->
                        maxOf(current, currentMinuteAvg)
                    }
                }
            }
        }

        // Check for auto-end (Ramp test)
        val ftp = currentFtp.get()
        val targetPower = protocol.getTargetPower(elapsedMs, ftp) ?: 0
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
        scope.launch {
            stateMutex.withLock {
                val currentState = _state.value
                if (currentState is TestState.Running) {
                    val pauseTime = System.currentTimeMillis()
                    pausedTimeMs.set(pauseTime)
                    updateJob?.cancel()
                    _state.value = TestState.Paused(
                        protocol = currentState.protocol,
                        elapsedMs = getElapsedTimeMs(),
                        pausedAt = pauseTime
                    )
                }
            }
        }
    }

    private fun resumeTest() {
        scope.launch {
            stateMutex.withLock {
                val currentState = _state.value
                if (currentState is TestState.Paused) {
                    val pauseDuration = System.currentTimeMillis() - pausedTimeMs.get()
                    totalPausedMs.addAndGet(pauseDuration)

                    // Restart update loop
                    startUpdateLoop()

                    // Immediately update state to Running
                    updateState()
                }
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
        val ftp = currentFtp.get()
        val targetPower = protocol.getTargetPower(elapsedMs, ftp)

        // Calculate average power from thread-safe list
        val avgPower = if (powerSamples.isNotEmpty()) {
            powerSamples.toList().average().toInt()
        } else 0

        val runningState = TestState.Running(
            protocol = protocol.type,
            phase = currentPhase,
            currentInterval = currentInterval,
            intervalIndex = protocol.getCurrentIntervalIndex(elapsedMs),
            elapsedMs = elapsedMs,
            timeRemainingInInterval = protocol.getTimeRemainingInInterval(elapsedMs),
            currentPower = currentPower.get(),
            targetPower = targetPower,
            currentStep = (protocol as? RampTest)?.getCurrentStep(elapsedMs),
            estimatedTotalSteps = (protocol as? RampTest)?.getEstimatedTotalSteps(ftp),
            maxOneMinutePower = maxOneMinutePower.get(),
            averagePower = avgPower,
            heartRate = currentHeartRate.get(),
            cadence = currentCadence.get(),
            userMaxHr = userMaxHr.get() // Pass user's max HR for zone calculations
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
        return System.currentTimeMillis() - testStartTimeMs.get() - totalPausedMs.get()
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
     * Get the user's max HR from profile for external use.
     */
    fun getUserMaxHr(): Int = userMaxHr.get()

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
