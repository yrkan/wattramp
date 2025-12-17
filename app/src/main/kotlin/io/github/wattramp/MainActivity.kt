package io.github.wattramp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult
import io.github.wattramp.engine.TestState
import io.github.wattramp.ui.components.GuideOverlay
import io.github.wattramp.ui.components.getGuideTourSteps
import io.github.wattramp.ui.components.GUIDE_TOUR_TOTAL_STEPS
import io.github.wattramp.ui.screens.*
import io.github.wattramp.ui.theme.WattRampTheme
import io.github.wattramp.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun attachBaseContext(newBase: android.content.Context) {
        // Wrap context with selected locale before activity is created
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create ViewModel using AndroidViewModelFactory
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        // Read theme synchronously to avoid flash on startup
        val initialTheme = LocaleHelper.getSavedTheme(this)

        setContent {
            // Start with saved theme, update dynamically from settings
            val settings by viewModel.settings.collectAsState()
            val currentTheme by remember(initialTheme) {
                mutableStateOf(initialTheme)
            }.apply {
                value = settings.theme
            }

            WattRampTheme(appTheme = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WattRampApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WattRampApp(viewModel: MainViewModel) {
    val navController = rememberNavController()

    // Core navigation state - only what's needed for navigation decisions
    val testState by viewModel.activeTestState.collectAsState()
    val testStarted by viewModel.sessionTestStarted.collectAsState()
    val hasNavigated by viewModel.sessionHasNavigated.collectAsState()
    val guideTourStep by viewModel.guideTourStep.collectAsState()

    // Navigate to running screen when test starts
    LaunchedEffect(testState, testStarted) {
        if (testState !is TestState.Running || !testStarted) return@LaunchedEffect

        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect

        // Only navigate from screens where test can be started
        val canStartFrom = currentRoute == "home" ||
                          currentRoute.startsWith("checklist/") ||
                          currentRoute.startsWith("sensor_warning/")

        if (canStartFrom) {
            navController.navigate("running") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Navigate to result screen when test completes
    LaunchedEffect(testState, hasNavigated) {
        if (testState !is TestState.Completed || hasNavigated) return@LaunchedEffect

        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect

        if (currentRoute == "running") {
            viewModel.setHasNavigated(true)
            navController.navigate("result") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Guide demo state - created once with stable FTP value
    val initialFtp = remember { viewModel.settings.value.currentFtp }
    val initialRampStart = remember { viewModel.settings.value.rampStartPower }
    val initialRampStep = remember { viewModel.settings.value.rampStep }

    val guideDemoRunningState = remember(initialFtp) {
        val ftp = initialFtp
        TestState.Running(
            protocol = ProtocolType.RAMP,
            phase = TestPhase.TESTING,
            currentInterval = Interval(
                name = "Ramp Step 8",
                phase = TestPhase.TESTING,
                durationMs = 60 * 1000L,
                targetPowerPercent = null,
                isRamp = true,
                rampStartPower = initialRampStart,
                rampStepWatts = initialRampStep
            ),
            intervalIndex = 8,
            elapsedMs = 13 * 60 * 1000L,
            timeRemainingInInterval = 32 * 1000L,
            currentPower = (ftp * 0.95).toInt(),
            targetPower = (ftp * 0.97).toInt(),
            currentStep = 8,
            estimatedTotalSteps = 12,
            maxOneMinutePower = (ftp * 0.95).toInt(),
            heartRate = 165,
            cadence = 88
        )
    }

    val guideDemoCompletedResult = remember(initialFtp) {
        val ftp = initialFtp
        val maxPower = (ftp * 1.33).toInt()
        val newFtp = (maxPower * 0.75).toInt()
        TestResult(
            id = java.util.UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            protocol = ProtocolType.RAMP,
            calculatedFtp = newFtp,
            previousFtp = ftp,
            testDurationMs = 22 * 60 * 1000L,
            maxOneMinutePower = maxPower,
            averagePower = (ftp * 0.85).toInt(),
            stepsCompleted = 12,
            formula = "$maxPower × 0.75 = $newFtp",
            normalizedPower = (ftp * 0.92).toInt(),
            variabilityIndex = 1.08,
            averageHeartRate = 168,
            efficiencyFactor = 1.52,
            saved = false
        )
    }

    // Helper function to handle guide tour navigation
    fun handleGuideTourNext() {
        val nextScreen = viewModel.nextGuideTourStep()
        if (nextScreen != null) {
            navController.navigate(nextScreen) {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        } else {
            // Tour ended - just go back to home without recreating it
            navController.popBackStack("home", inclusive = false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            // Disable all navigation animations for instant transitions
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
        composable("home") {
            // Collect state locally for this screen
            val settings by viewModel.settings.collectAsState()
            val recoverySession by viewModel.recoverySession.collectAsState()

            HomeScreen(
                currentFtp = settings.currentFtp,
                recoverySession = recoverySession,
                onNavigateToChecklist = { protocol ->
                    navController.navigate("checklist/${protocol.name}")
                },
                onAcceptRecovery = {
                    viewModel.acceptRecovery()
                },
                onDeclineRecovery = {
                    viewModel.declineRecovery()
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

        composable("checklist/{protocol}") { backStackEntry ->
            val protocolName = backStackEntry.arguments?.getString("protocol") ?: "RAMP"
            val protocol = ProtocolType.valueOf(protocolName)
            ChecklistScreen(
                protocol = protocol,
                onStartTest = {
                    // Check power sensor and start test
                    if (viewModel.isDemoMode || !viewModel.isPowerSensorAvailable()) {
                        // Show warning - navigate to sensor warning screen
                        navController.navigate("sensor_warning/${protocol.name}")
                    } else {
                        // Start test - navigation handled by LaunchedEffect
                        viewModel.startTest(protocol)
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("sensor_warning/{protocol}") { backStackEntry ->
            val protocolName = backStackEntry.arguments?.getString("protocol") ?: "RAMP"
            val protocol = ProtocolType.valueOf(protocolName)
            SensorWarningScreen(
                onConfirm = {
                    // Start test - navigation handled by LaunchedEffect
                    viewModel.startTest(protocol)
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            val settings by viewModel.settings.collectAsState()

            SettingsScreen(
                settings = settings,
                onUpdateFtp = { ftp ->
                    viewModel.updateCurrentFtp(ftp)
                },
                onUpdateRampStart = { power ->
                    viewModel.updateRampStartPower(power)
                },
                onUpdateRampStep = { step ->
                    viewModel.updateRampStep(step)
                },
                onUpdateSoundAlerts = { enabled ->
                    viewModel.updateSoundAlerts(enabled)
                },
                onUpdateScreenWake = { enabled ->
                    viewModel.updateScreenWake(enabled)
                },
                onUpdateShowMotivation = { enabled ->
                    viewModel.updateShowMotivation(enabled)
                },
                onUpdateWarmupDuration = { minutes ->
                    viewModel.updateWarmupDuration(minutes)
                },
                onUpdateCooldownDuration = { minutes ->
                    viewModel.updateCooldownDuration(minutes)
                },
                onUpdateLanguage = { language ->
                    viewModel.updateLanguage(language)
                    // Save language and recreate activity if changed
                    if (LocaleHelper.applyLanguage(context, language)) {
                        // Delay for smooth transition
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            (context as? android.app.Activity)?.recreate()
                        }, 200)
                    }
                },
                onUpdateTheme = { theme ->
                    viewModel.updateTheme(theme)
                    // Save to SharedPreferences for next startup
                    LocaleHelper.saveTheme(context, theme)
                },
                onClearHistory = {
                    viewModel.clearTestHistory()
                },
                onStartDemo = {
                    viewModel.startGuideTour()
                    navController.popBackStack("home", inclusive = false)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("history") {
            val history by viewModel.testHistory.collectAsState()

            HistoryScreen(
                history = history,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("zones") {
            val settings by viewModel.settings.collectAsState()

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
            // During guide tour, use demo state directly
            if (guideTourStep >= 0) {
                RunningScreen(
                    runningState = guideDemoRunningState,
                    onStopTest = { handleGuideTourNext() }
                )
                return@composable
            }

            // Remember last valid running state to prevent empty content during navigation
            var lastRunningState by remember { mutableStateOf<TestState.Running?>(null) }
            val currentRunningState = testState as? TestState.Running

            // Update last running state when we have a valid state
            if (currentRunningState != null) {
                lastRunningState = currentRunningState
            }

            // Show running screen with current or last valid state
            val displayState = currentRunningState ?: lastRunningState

            if (displayState != null) {
                RunningScreen(
                    runningState = displayState,
                    onStopTest = {
                        // Navigate first to avoid flicker, then clean up state
                        navController.popBackStack("home", inclusive = false)
                        viewModel.stopTest()
                    }
                )
            } else {
                // Edge case: no state available, go back home
                LaunchedEffect(Unit) {
                    navController.popBackStack("home", inclusive = false)
                }
            }
        }

        composable("result") {
            // During guide tour, use demo result directly
            if (guideTourStep >= 0) {
                ResultScreen(
                    result = guideDemoCompletedResult,
                    onSaveToKaroo = { handleGuideTourNext() },
                    onDiscard = { handleGuideTourNext() }
                )
                return@composable
            }

            val completedState = testState as? TestState.Completed

            // If we're on this route but there's no completed test, go back home
            if (completedState == null || !testStarted) {
                LaunchedEffect(Unit) {
                    navController.popBackStack("home", inclusive = false)
                }
                return@composable
            }

            ResultScreen(
                result = completedState.result,
                onSaveToKaroo = {
                    viewModel.updateCurrentFtp(completedState.result.calculatedFtp)
                    viewModel.saveTestResult(completedState.result.copy(saved = true))
                    // Navigate first to avoid flicker, then clean up state
                    navController.popBackStack("home", inclusive = false)
                    viewModel.dismissResults()
                },
                onDiscard = {
                    // Navigate first to avoid flicker, then clean up state
                    navController.popBackStack("home", inclusive = false)
                    viewModel.dismissResults()
                }
            )
        }
        }

        // Guide Tour Overlay
        if (guideTourStep >= 0 && guideTourStep < GUIDE_TOUR_TOTAL_STEPS) {
            val steps = getGuideTourSteps()
            val step = steps[guideTourStep]
            GuideOverlay(
                currentStep = guideTourStep,
                totalSteps = GUIDE_TOUR_TOTAL_STEPS,
                step = step,
                onNext = { handleGuideTourNext() },
                onSkip = {
                    viewModel.endGuideTour()
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
    }
}
