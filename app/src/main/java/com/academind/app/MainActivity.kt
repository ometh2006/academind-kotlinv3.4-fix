package com.academind.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.academind.app.ui.screens.*
import com.academind.app.ui.components.ToastMessage
import com.academind.app.ui.theme.AcadeMindTheme
import com.academind.app.viewmodel.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AcadeMindApp() }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Screen("home",     "Home",     Icons.Default.Home)
    object Subjects : Screen("subjects", "Subjects", Icons.Default.Book)
    object Analytics: Screen("analytics","Analytics",Icons.Default.BarChart)
    object History  : Screen("history",  "History",  Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val NAV_SCREENS = listOf(Screen.Home, Screen.Subjects, Screen.Analytics, Screen.History, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcadeMindApp() {
    val vm: MainViewModel = viewModel()
    val appState      by vm.appState.collectAsStateWithLifecycle()
    val dashStats     by vm.dashboardStats.collectAsStateWithLifecycle()
    val userPrefs     by vm.userPrefs.collectAsStateWithLifecycle()

    // Countdown — update every second
    var countdown by remember { mutableStateOf(vm.computeCountdown(userPrefs.examDate)) }
    LaunchedEffect(userPrefs.examDate) {
        while (true) { countdown = vm.computeCountdown(userPrefs.examDate); delay(1000) }
    }

    // Toast state
    var toastMsg  by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(Unit) {
        vm.toastMessage.collect { toastMsg = it }
    }

    AcadeMindTheme(darkTheme = userPrefs.isDarkTheme) {
        if (!userPrefs.isSetupComplete) {
            SetupScreen(vm)
            return@AcadeMindTheme
        }

        var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

        Scaffold(
            bottomBar = {
                NavigationBar(tonalElevation = 0.dp) {
                    NAV_SCREENS.forEach { screen ->
                        NavigationBarItem(
                            selected    = currentScreen == screen,
                            onClick     = { currentScreen = screen },
                            icon        = { Icon(screen.icon, contentDescription = screen.label) },
                            label       = { Text(screen.label) }
                        )
                    }
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(when (currentScreen) {
                            Screen.Home      -> "🎓 Dashboard"
                            Screen.Subjects  -> "📚 Subjects"
                            Screen.Analytics -> "📊 Analytics"
                            Screen.History   -> "📋 History"
                            Screen.Settings  -> "⚙️ Settings"
                            else             -> "AcadeMind"
                        })
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor         = MaterialTheme.colorScheme.surface,
                        titleContentColor      = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = {
                toastMsg?.let { (msg, type) ->
                    SnackbarHost(remember { SnackbarHostState() }) {
                        ToastMessage(msg, type) { toastMsg = null }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (currentScreen) {
                    Screen.Home      -> HomeScreen(appState, dashStats, countdown)
                    Screen.Subjects  -> SubjectsScreen(vm, appState)
                    Screen.Analytics -> AnalyticsScreen(appState)
                    Screen.History   -> HistoryScreen(vm, appState)
                    Screen.Settings  -> SettingsScreen(vm, appState)
                    else             -> HomeScreen(appState, dashStats, countdown)
                }
            }
        }
    }
}
