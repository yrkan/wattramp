package io.github.wattramp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.wattramp.engine.TestState
import io.github.wattramp.ui.screens.*
import io.github.wattramp.ui.theme.WattRampTheme
import io.github.wattramp.util.LocaleHelper

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create ViewModel using AndroidViewModelFactory
        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[MainViewModel::class.java]

        setContent {
            // Settings is already a StateFlow, so we can collect directly
            val settings by viewModel.settings.collectAsState()

            // Apply language setting (non-blocking, using LaunchedEffect)
            LaunchedEffect(settings.language) {
                LocaleHelper.applyLanguage(this@MainActivity, settings.language)
            }

            WattRampTheme(appTheme = settings.theme) {
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

    // Collect StateFlows - no initial value needed as StateFlow always has a value
    val settings by viewModel.settings.collectAsState()
    val history by viewModel.testHistory.collectAsState()
    val testState by viewModel.activeTestState.collectAsState()
    val testStarted by viewModel.sessionTestStarted.collectAsState()
    val hasNavigated by viewModel.sessionHasNavigated.collectAsState()
    val recoverySession by viewModel.recoverySession.collectAsState()

    // Navigate based on test state changes
    LaunchedEffect(testState, testStarted, hasNavigated) {
        val currentRoute = navController.currentDestination?.route ?: return@LaunchedEffect

        when {
            // Navigate to running screen when test starts
            testState is TestState.Running && testStarted && currentRoute == "home" -> {
                navController.navigate("running")
            }
            // Navigate to result screen when test completes (from home or running)
            testState is TestState.Completed && testStarted && !hasNavigated &&
                (currentRoute == "home" || currentRoute == "running") -> {
                viewModel.setHasNavigated(true)
                navController.navigate("result") {
                    popUpTo("home")
                }
            }
            // Handle failed state - go back to home
            testState is TestState.Failed && testStarted &&
                (currentRoute == "running") -> {
                viewModel.setHasNavigated(true)
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                currentFtp = settings.currentFtp,
                showChecklist = settings.showChecklist,
                recoverySession = recoverySession,
                onStartTest = { protocol ->
                    viewModel.startTest(protocol)
                },
                onDismissChecklist = {
                    viewModel.dismissChecklist()
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

        composable("settings") {
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
                    LocaleHelper.applyLanguage(context, language)
                },
                onUpdateTheme = { theme ->
                    viewModel.updateTheme(theme)
                },
                onClearHistory = {
                    viewModel.clearTestHistory()
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
            when (val state = testState) {
                is TestState.Running -> {
                    RunningScreen(
                        runningState = state,
                        onStopTest = {
                            viewModel.dismissResults()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
                is TestState.Paused -> {
                    // Show paused state (could show a paused overlay)
                    // For now, just wait for resume
                }
                is TestState.Completed -> {
                    // Test completed, will navigate via LaunchedEffect
                }
                is TestState.Failed -> {
                    // Will navigate back via LaunchedEffect
                }
                is TestState.Idle -> {
                    // Invalid state, go back home
                    LaunchedEffect(Unit) {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    }
                }
            }
        }

        composable("result") {
            val completedState = testState as? TestState.Completed

            // If we're on this route but there's no completed test OR we didn't start a test, go back home
            if (completedState == null || !testStarted) {
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
                    viewModel.updateCurrentFtp(completedState.result.calculatedFtp)
                    viewModel.saveTestResult(completedState.result.copy(saved = true))
                    viewModel.dismissResults()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onDiscard = {
                    viewModel.dismissResults()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
