package com.trackmyfit.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
                    )
                }
                composable("log") { LogScreen(vm, onOpenExercise = openExercise) }
                composable("history") { HistoryScreen(vm, onOpenDay = openDay) }
                composable("body") { BodyScreen(vm) }
                composable("exercises") { ExercisesScreen(vm, onOpen = openExercise) }
                composable("settings") { SettingsScreen(vm, onBack = { nav.popBackStack() }) }
                composable("exercise/{id}") { entry ->
                    ExerciseDetailScreen(vm, exerciseId = entry.arguments?.getString("id") ?: "", onBack = { nav.popBackStack() }, onOpenDay = openDay)
                }
            }
        }
    }
}
