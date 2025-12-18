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

    // Track last time we received valid power data
    private val lastPowerReceivedMs = AtomicLong(0L)

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
    private val hrSamples = CopyOnWriteArrayList<Int>() // For analytics calculation
    private val maxOneMinutePower = AtomicInteger(0)

    // Running sums for efficient average calculation (avoid toList().average())
    private val powerSamplesSum = AtomicLong(0)
    private val hrSamplesSum = AtomicLong(0)

    // Thread-safe bounded deque for rolling average
    private val lastMinutePowerSamples = ArrayDeque<Int>(ROLLING_WINDOW_SIZE + 5)
    private val powerSamplesLock = Any()

    // Karoo system consumer IDs - using AtomicReference for thread safety
    private val powerConsumerId = AtomicReference<String?>(null)
    private val heartRateConsumerId = AtomicReference<String?>(null)
    private val cadenceConsumerId = AtomicReference<String?>(null)
    private val rideStateConsumerId = AtomicReference<String?>(null)
    private val userProfileConsumerId = AtomicReference<String?>(null)

    // Update job - using AtomicReference to prevent race conditions
    private val updateJobRef = AtomicReference<Job?>(null)

    // Track if ride was ever recording during this test session
    @Volatile
    private var wasRideRecording = false

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
     * Returns false if test cannot be started (e.g., KarooSystem not connected).
     */
    fun startTest(protocolType: ProtocolType): Boolean {
        // Reset state to Idle first to ensure clean start
        // This prevents stale Failed/Completed states from interfering
        _state.value = TestState.Idle

        // Pre-check: ensure KarooSystem is connected before starting
        if (!extension.karooSystem.connected) {
            android.util.Log.e("TestEngine", "Cannot start test: KarooSystem not connected")
            _state.value = TestState.Failed(
                protocol = protocolType,
                reason = FailureReason.SYSTEM_ERROR,
                partialResult = null
            )
            return false
        }

        // Do initialization on Default dispatcher to avoid blocking UI
        scope.launch(Dispatchers.Default) {
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

                // Reset power and HR tracking with bounded clearing
                powerSamples.clear()
                hrSamples.clear()
                maxOneMinutePower.set(0)
                powerSamplesSum.set(0)
                hrSamplesSum.set(0)
                synchronized(powerSamplesLock) {
                    lastMinutePowerSamples.clear()
                }
                currentHeartRate.set(0)
                currentCadence.set(0)
                currentPower.set(0)
                wasRideRecording = false
            }

            // Connect to Karoo streams (needs to be on Main for callbacks)
            withContext(Dispatchers.Main) {
                connectToKarooStreams()
            }

            // Start update loop and initial state update
            withContext(Dispatchers.Main.immediate) {
                startUpdateLoop()
                updateState()
                android.util.Log.i("TestEngine", "Test started, state: ${_state.value}")
            }
        }
        return true
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

        // Cancel update job atomically
        updateJobRef.getAndSet(null)?.cancel()
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

                // Cancel update job atomically
                updateJobRef.getAndSet(null)?.cancel()
                disconnectFromKarooStreams()

                val ftp = currentFtp.get()
                val maxPower = maxOneMinutePower.get()
                val sampleCount = powerSamples.size
                val avgPower = if (sampleCount > 0) {
                    (powerSamplesSum.get() / sampleCount).toInt()
                } else 0

                val baseResult = try {
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

                // Calculate extended analytics (NP, VI, EF)
                val analytics = AnalyticsCalculator.calculateAll(
                    powerSamples = powerSamples.toList(),
                    hrSamples = hrSamples.toList()
                )

                // Augment result with analytics
                val result = baseResult.copy(
                    normalizedPower = analytics.normalizedPower,
                    variabilityIndex = analytics.variabilityIndex,
                    averageHeartRate = analytics.averageHeartRate,
                    efficiencyFactor = analytics.efficiencyFactor
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
                            // Store HR samples for analytics (only during test, limit size)
                            if (hr > 0 && _state.value is TestState.Running &&
                                hrSamples.size < MAX_POWER_SAMPLES) {
                                hrSamples.add(hr)
                                hrSamplesSum.addAndGet(hr.toLong())
                            }
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

            // Get user profile (initial FTP, max HR, and weight)
            userProfileConsumerId.set(extension.karooSystem.addConsumer<UserProfile> { profile ->
                // Safe access with null check
                try {
                    val profileFtp = profile.ftp
                    // Only use profile FTP if it's valid and user hasn't set a custom FTP
                    // (check if FTP is still at default value)
                    if (profileFtp > 0) {
                        val current = currentFtp.get()
                        // Use profile FTP if current is default OR if profile FTP is significantly different
                        // This allows initial setup from profile while respecting user changes
                        if (current == PreferencesRepository.DEFAULT_FTP) {
                            currentFtp.set(profileFtp)
                            android.util.Log.i("TestEngine", "Using FTP from Karoo profile: $profileFtp")
                        }
                    }
                    // Extract max HR from user profile for accurate HR zone calculations
                    val maxHr = profile.maxHr
                    if (maxHr > 0) {
                        userMaxHr.set(maxHr)
                    }
                    // Extract weight from user profile for W/kg calculations
                    val weight = profile.weight
                    if (weight > 0) {
                        scope.launch(Dispatchers.IO) {
                            preferencesRepository.updateUserWeight(weight)
                            android.util.Log.i("TestEngine", "Using weight from Karoo profile: ${weight}kg")
                        }
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

        // Track when we last received valid power data
        if (power > 0) {
            lastPowerReceivedMs.set(System.currentTimeMillis())
        }

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
                powerSamplesSum.addAndGet(power.toLong())
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

        // Check for auto-end (Ramp test) - only during TESTING phase
        // Don't check during WARMUP or COOLDOWN as power may be low or zero intentionally
        val currentPhase = protocol.getCurrentPhase(elapsedMs)
        if (currentPhase == TestPhase.TESTING || currentPhase == TestPhase.TESTING_2) {
            val ftp = currentFtp.get()
            val targetPower = protocol.getTargetPower(elapsedMs, ftp) ?: 0
            if (protocol.shouldEndTest(power, targetPower)) {
                completeTest()
            }
        }
    }

    private fun onRideStateChange(rideState: RideState) {
        when (rideState) {
            is RideState.Paused -> pauseTest()
            RideState.Recording -> {
                wasRideRecording = true
                resumeTest()
            }
            RideState.Idle -> {
                // Only stop the test if a ride was actually recording and then ended
                // Don't stop if no ride was ever started (user testing without recording)
                if (wasRideRecording) {
                    val currentState = _state.value
                    if (currentState is TestState.Running || currentState is TestState.Paused) {
                        stopTest(FailureReason.RIDE_ENDED)
                    }
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
                    // Cancel update job atomically
                    updateJobRef.getAndSet(null)?.cancel()
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
        // Cancel existing job and create new one atomically
        val newJob = scope.launch {
            while (isActive) {
                if (_state.value is TestState.Running || currentProtocol != null) {
                    updateState()
                }
                delay(1000) // Update every second
            }
        }
        // Cancel old job after creating new one to avoid gap
        updateJobRef.getAndSet(newJob)?.cancel()
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

        // Calculate average power using running sum (avoids toList() allocation)
        val sampleCount = powerSamples.size
        val avgPower = if (sampleCount > 0) {
            (powerSamplesSum.get() / sampleCount).toInt()
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
            userMaxHr = userMaxHr.get(),
            zoneTolerance = settings.zoneTolerance
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
     * Check if power sensor data was received recently.
     * Returns true if we've received valid power data in the last 5 seconds.
     */
    fun isPowerSensorAvailable(): Boolean {
        val lastReceived = lastPowerReceivedMs.get()
        if (lastReceived == 0L) return false
        return (System.currentTimeMillis() - lastReceived) < 5000
    }

    /**
     * Start listening for power data temporarily (for sensor check before test).
     * Call stopSensorCheck() to stop listening.
     */
    private val sensorCheckConsumerId = AtomicReference<String?>(null)

    fun startSensorCheck() {
        if (!extension.karooSystem.connected) return

        // Don't start if already checking
        if (sensorCheckConsumerId.get() != null) return

        try {
            sensorCheckConsumerId.set(extension.karooSystem.addConsumer(
                OnStreamState.StartStreaming(DataType.Type.POWER)
            ) { event: OnStreamState ->
                when (val streamState = event.state) {
                    is StreamState.Streaming -> {
                        streamState.dataPoint.singleValue?.toInt()?.let { power ->
                            if (power > 0) {
                                lastPowerReceivedMs.set(System.currentTimeMillis())
                            }
                        }
                    }
                    else -> { /* Ignore */ }
                }
            })
        } catch (e: Exception) {
            android.util.Log.w("TestEngine", "Error starting sensor check: ${e.message}")
        }
    }

    fun stopSensorCheck() {
        sensorCheckConsumerId.getAndSet(null)?.let { id ->
            try {
                extension.karooSystem.removeConsumer(id)
            } catch (e: Exception) {
                android.util.Log.w("TestEngine", "Error stopping sensor check: ${e.message}")
            }
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        // Cancel update job atomically
        updateJobRef.getAndSet(null)?.cancel()
        stopSensorCheck()
        disconnectFromKarooStreams()
        scope.cancel()
    }
}
