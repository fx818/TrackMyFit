package com.trackmyfit.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trackmyfit.app.notify.Notifier
import com.trackmyfit.app.ui.screens.*
import java.time.LocalDate

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Dashboard", Icons.Filled.Dashboard),
    Tab("log", "Log", Icons.Filled.FitnessCenter),
    Tab("history", "History", Icons.Filled.CalendarMonth),
    Tab("body", "Body", Icons.Filled.MonitorWeight),
    Tab("exercises", "Exercises", Icons.Filled.Search),
)

fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val ctx = LocalContext.current
    val data by vm.data.collectAsStateWithLifecycle()
    val navRequest by vm.navRequest.collectAsStateWithLifecycle()
    var openWeight by remember { mutableStateOf(false) }

    // Ask once for notification permission (rest timer + reminders) on Android 13+.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !data.settings.askedNotificationPermission && !Notifier.canPost(ctx)) {
            vm.updateSettings { it.copy(askedNotificationPermission = true) }
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // Deep links from widget / notifications.
    LaunchedEffect(navRequest) {
        when (navRequest) {
            "log" -> { vm.setDate(LocalDate.now()); nav.switchTab("log") }
            "weight" -> { nav.switchTab("home"); openWeight = true }
        }
        if (navRequest != null) vm.consumeNav()
    }

    val openDay: (LocalDate) -> Unit = { d -> vm.setDate(d); nav.switchTab("log") }
    val openExercise: (String) -> Unit = { nav.navigate("exercise/$it") }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tabs.any { it.route == route }) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { if (t.route == "log" && route != "log") vm.setDate(LocalDate.now()); nav.switchTab(t.route) },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            NavHost(nav, startDestination = "home") {
                composable("home") {
                    DashboardScreen(
                        vm,
                        onOpenLog = { vm.setDate(LocalDate.now()); nav.switchTab("log") },
                        onOpenBody = { nav.switchTab("body") },
                        onOpenSettings = { nav.navigate("settings") },
                        onOpenExercise = openExercise,
                        onOpenSteps = { nav.navigate("steps") },
                        showWeightDialog = openWeight,
                        onWeightDialogShown = { openWeight = false },
                    )
                }
                composable("log") { LogScreen(vm, onOpenExercise = openExercise, onOpenRoutines = { nav.navigate("routines") }) }
                composable("history") { HistoryScreen(vm, onOpenDay = openDay) }
                composable("body") { BodyScreen(vm, onOpenPhotos = { nav.navigate("photos") }) }
                composable("exercises") { ExercisesScreen(vm, onOpen = openExercise) }
                composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }, onOpenSteps = { nav.navigate("steps") }) }
                composable("routines") { RoutinesScreen(vm, onBack = { nav.popBackStack() }, onStarted = { nav.switchTab("log") }) }
                composable("photos") { PhotosScreen(vm, onBack = { nav.popBackStack() }) }
                composable("steps") { StepsScreen(vm, onBack = { nav.popBackStack() }) }
                composable("exercise/{id}") { entry ->
                    ExerciseDetailScreen(vm, exerciseId = entry.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() }, onOpenDay = openDay)
                }
            }
        }
    }
}
