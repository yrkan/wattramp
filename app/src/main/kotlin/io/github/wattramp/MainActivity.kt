package io.github.wattramp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wattramp.data.PreferencesRepository
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestHistoryData
import io.github.wattramp.data.TestResult
import io.github.wattramp.engine.TestState
import io.github.wattramp.ui.screens.*
import io.github.wattramp.ui.theme.WattRampTheme
import io.github.wattramp.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var preferencesRepository: PreferencesRepository

    // Demo mode state for testing without Karoo
    private val demoTestState = MutableStateFlow<TestState>(TestState.Idle)
    private var isDemoMode = false

    // Session-level flags - reset on every onCreate
    private var sessionTestStarted = false
    private var sessionHasNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesRepository = PreferencesRepository(this)

        // Apply saved language setting on app start
        kotlinx.coroutines.runBlocking {
            val settings = preferencesRepository.settingsFlow.first()
            LocaleHelper.applyLanguage(this@MainActivity, settings.language)
        }

        // Check if running on Karoo (extension available)
        isDemoMode = WattRampExtension.instance == null

        // CRITICAL: Reset ALL state on app start
        sessionTestStarted = false
        sessionHasNavigated = false
        demoTestState.value = TestState.Idle

        setContent {
            // Observe settings for theme
            val settings by preferencesRepository.settingsFlow.collectAsState(
                initial = PreferencesRepository.Settings()
            )

            WattRampTheme(appTheme = settings.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WattRampApp(
                        preferencesRepository = preferencesRepository,
                        testStateFlow = if (isDemoMode) demoTestState else
                            WattRampExtension.instance?.testEngine?.state ?: demoTestState,
                        onStartTest = { protocol ->
                            sessionTestStarted = true
                            sessionHasNavigated = false
                            startTest(protocol)
                        },
                        onDismissResults = {
                            sessionTestStarted = false
                            sessionHasNavigated = false
                            dismissResults()
                        },
                        getTestStarted = { sessionTestStarted },
                        setTestStarted = { sessionTestStarted = it },
                        getHasNavigated = { sessionHasNavigated },
                        setHasNavigated = { sessionHasNavigated = it }
                    )
                }
            }
        }
    }

    private fun startTest(protocol: ProtocolType) {
        if (isDemoMode) {
            // Demo mode - simulate test with Running state first
            Toast.makeText(
                this,
                "Demo Mode: Simulating ${protocol.displayName}...",
                Toast.LENGTH_SHORT
            ).show()

            kotlinx.coroutines.MainScope().launch {
                val currentFtp = preferencesRepository.settingsFlow.first().currentFtp

                // Create a running state to show progress
                val demoRunningState = TestState.Running(
                    protocol = protocol,
                    phase = io.github.wattramp.data.TestPhase.WARMUP,
                    currentInterval = io.github.wattramp.data.Interval(
                        name = "Warmup",
                        phase = io.github.wattramp.data.TestPhase.WARMUP,
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
                demoTestState.value = demoRunningState

                // Simulate progress updates
                for (i in 1..3) {
                    delay(1000)
                    val progressState = demoRunningState.copy(
                        elapsedMs = i * 1000L,
                        timeRemainingInInterval = (5 * 60 * 1000L) - (i * 1000L),
                        currentStep = if (protocol == ProtocolType.RAMP) i + 1 else null
                    )
                    demoTestState.value = progressState
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

                demoTestState.value = TestState.Completed(demoResult)
            }
        } else {
            // Real Karoo mode
            WattRampExtension.instance?.testEngine?.startTest(protocol)
        }
    }

    private fun dismissResults() {
        if (isDemoMode) {
            demoTestState.value = TestState.Idle
        } else {
            WattRampExtension.instance?.testEngine?.dismissResults()
        }
    }
}

@Composable
fun WattRampApp(
    preferencesRepository: PreferencesRepository,
    testStateFlow: kotlinx.coroutines.flow.StateFlow<TestState>,
    onStartTest: (ProtocolType) -> Unit,
    onDismissResults: () -> Unit,
    getTestStarted: () -> Boolean,
    setTestStarted: (Boolean) -> Unit,
    getHasNavigated: () -> Boolean,
    setHasNavigated: (Boolean) -> Unit
) {
    val navController = rememberNavController()

    // Collect state flows
    val settings by preferencesRepository.settingsFlow.collectAsState(
        initial = PreferencesRepository.Settings()
    )
    val history by preferencesRepository.testHistoryFlow.collectAsState(
        initial = TestHistoryData()
    )

    // Observe test state
    val testState by testStateFlow.collectAsState()

    // Read session flags from Activity
    val testStarted = getTestStarted()
    val hasNavigated = getHasNavigated()

    // Navigate based on test state changes
    LaunchedEffect(testState) {
        val currentTestStarted = getTestStarted()
        val currentHasNavigated = getHasNavigated()
        val currentRoute = navController.currentDestination?.route

        when {
            // Navigate to running screen when test starts
            testState is TestState.Running && currentTestStarted && currentRoute == "home" -> {
                navController.navigate("running")
            }
            // Navigate to result screen when test completes (from home or running)
            testState is TestState.Completed && currentTestStarted && !currentHasNavigated &&
                (currentRoute == "home" || currentRoute == "running") -> {
                setHasNavigated(true)
                navController.navigate("result") {
                    popUpTo("home")
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                currentFtp = settings.currentFtp,
                onStartTest = { protocol ->
                    onStartTest(protocol)
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToHistory = {
                    navController.navigate("history")
                },
                onNavigateToZones = {
                    navController.navigate("zones")
                },
                onNavigateToTutorial = {
                    navController.navigate("tutorial")
                }
            )
        }

        composable("settings") {
            val scope = rememberCoroutineScope()
            val context = androidx.compose.ui.platform.LocalContext.current

            SettingsScreen(
                settings = settings,
                onUpdateFtp = { ftp ->
                    scope.launch { preferencesRepository.updateCurrentFtp(ftp) }
                },
                onUpdateRampStart = { power ->
                    scope.launch { preferencesRepository.updateRampStartPower(power) }
                },
                onUpdateRampStep = { step ->
                    scope.launch { preferencesRepository.updateRampStep(step) }
                },
                onUpdateSoundAlerts = { enabled ->
                    scope.launch { preferencesRepository.updateSoundAlerts(enabled) }
                },
                onUpdateScreenWake = { enabled ->
                    scope.launch { preferencesRepository.updateScreenWake(enabled) }
                },
                onUpdateShowMotivation = { enabled ->
                    scope.launch { preferencesRepository.updateShowMotivation(enabled) }
                },
                onUpdateWarmupDuration = { minutes ->
                    scope.launch { preferencesRepository.updateWarmupDuration(minutes) }
                },
                onUpdateCooldownDuration = { minutes ->
                    scope.launch { preferencesRepository.updateCooldownDuration(minutes) }
                },
                onUpdateLanguage = { language ->
                    scope.launch {
                        preferencesRepository.updateLanguage(language)
                        LocaleHelper.applyLanguage(context, language)
                    }
                },
                onUpdateTheme = { theme ->
                    scope.launch { preferencesRepository.updateTheme(theme) }
                },
                onClearHistory = {
                    scope.launch { preferencesRepository.clearTestHistory() }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("history") {
            HistoryScreen(
                history = history,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("zones") {
            ZonesScreen(
                ftp = settings.currentFtp,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("tutorial") {
            TutorialScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("running") {
            val runningState = testState as? TestState.Running

            if (runningState != null) {
                RunningScreen(
                    runningState = runningState,
                    onStopTest = {
                        onDismissResults()
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                )
            } else if (testState is TestState.Completed) {
                // Test completed, will navigate via LaunchedEffect
            } else {
                // Invalid state, go back home
                LaunchedEffect(Unit) {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
        }

        composable("result") {
            val completedState = testState as? TestState.Completed
            val scope = rememberCoroutineScope()
            val currentTestStarted = getTestStarted()

            // If we're on this route but there's no completed test OR we didn't start a test, go back home
            if (completedState == null || !currentTestStarted) {
                LaunchedEffect(Unit) {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
                return@composable
            }

            ResultScreen(
                result = completedState.result,
                onSaveToKaroo = {
                    scope.launch {
                        preferencesRepository.updateCurrentFtp(completedState.result.calculatedFtp)
                        preferencesRepository.saveTestResult(
                            completedState.result.copy(saved = true)
                        )
                    }
                    onDismissResults()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onDiscard = {
                    onDismissResults()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
