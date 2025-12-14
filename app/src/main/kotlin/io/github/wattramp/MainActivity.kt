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
import io.github.wattramp.data.PreferencesRepository
import io.github.wattramp.data.TestHistoryData
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
            // Observe settings for theme and apply language
            val settings by viewModel.settings.collectAsState(
                initial = PreferencesRepository.Settings()
            )

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

    // Collect state flows
    val settings by viewModel.settings.collectAsState(
        initial = PreferencesRepository.Settings()
    )
    val history by viewModel.testHistory.collectAsState(
        initial = TestHistoryData()
    )

    // Observe test state
    val testState by viewModel.activeTestState.collectAsState(initial = TestState.Idle)

    // Session state from ViewModel
    val testStarted by viewModel.sessionTestStarted.collectAsState()
    val hasNavigated by viewModel.sessionHasNavigated.collectAsState()

    // Navigate based on test state changes
    LaunchedEffect(testState, testStarted, hasNavigated) {
        val currentRoute = navController.currentDestination?.route

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
                onStartTest = { protocol ->
                    viewModel.startTest(protocol)
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
            val runningState = testState as? TestState.Running

            if (runningState != null) {
                RunningScreen(
                    runningState = runningState,
                    onStopTest = {
                        viewModel.dismissResults()
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
