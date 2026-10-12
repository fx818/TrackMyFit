package com.trackmyfit.app.ui.screens

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.health.StepsRepository
import com.trackmyfit.app.ui.MainViewModel
import com.trackmyfit.app.ui.components.*
import java.text.NumberFormat

@Composable
fun StepsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val st by vm.stepsState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val nf = remember { NumberFormat.getIntegerInstance() }
    val permLauncher = rememberLauncherForActivityResult(vm.steps.permissionContract()) { vm.refreshSteps() }
    LaunchedEffect(Unit) { vm.refreshSteps() }
    val goal = data.settings.stepGoal

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Steps") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { IconButton(onClick = { vm.refreshSteps() }) { Icon(Icons.Filled.Refresh, "Refresh") } },
        )
    }) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                st.availability == StepsRepository.Availability.UNSUPPORTED -> item {
                    EmptyState(Icons.Filled.ErrorOutline, "Not supported on this phone",
                        "Step tracking uses Android Health Connect, which isn't available on this device.")
                }
                st.availability == StepsRepository.Availability.NEEDS_INSTALL_OR_UPDATE -> item {
                    EmptyState(Icons.Filled.Download, "Install Health Connect",
                        "TrackMyFit reads your steps from Health Connect (Google's free health data hub). Install or update it from the Play Store, then come back here.") {
                        Button(onClick = { runCatching { ctx.startActivity(vm.steps.installIntent()) } }) { Text("Open Play Store") }
                    }
                }
                !st.permitted -> item {
                    EmptyState(Icons.AutoMirrored.Filled.DirectionsWalk, "Connect your steps",
                        "Allow TrackMyFit to READ step counts from Health Connect. Your phone, Google Fit, Samsung Health, Fitbit or your watch write steps there. Nothing leaves your phone.") {
                        Button(onClick = { runCatching { permLauncher.launch(vm.steps.permissions) } }) { Text("Allow step access") }
                    }
                }
                else -> {
                    item {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                val today = st.today ?: 0
                                ProgressRing(today / goal.coerceAtLeast(1).toFloat(), size = 170.dp, stroke = 16.dp, color = Color(0xFF3DBE7A)) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(nf.format(today), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        Text("of ${nf.format(goal)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Text("≈ ${fmtNum(today * 0.000762)} km · ≈ ${(today * 0.04).toInt()} kcal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (st.history.isNotEmpty()) {
                        val last7 = st.history.takeLast(7)
                        item {
                            DashCard("Last 7 days", Icons.Filled.BarChart) {
                                BarChart(last7.map { (it.second / 100).toInt() }, last7.map { it.first.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()) })
                                Text("Bars in hundreds of steps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item {
                            DashCard("Last 30 days", Icons.AutoMirrored.Filled.ShowChart) {
                                val avg = st.history.map { it.second }.average()
                                val hit = st.history.count { it.second >= goal }
                                Text("Average ${nf.format(avg.toLong())} / day · goal met on $hit of ${st.history.size} days", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(6.dp))
                                LineChart(st.history.map { it.second.toDouble() }, st.history.map { fmtShortDate(it.first) },
                                    refLine = goal.toDouble(), valueFormatter = { if (it >= 1000) "%.1fk".format(it / 1000) else "%.0f".format(it) })
                                Text("Dashed line = daily goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    item {
                        Text("Steps are read from Health Connect. If they look low, make sure an app that counts steps (Google Fit, Samsung Health, Fitbit, your watch app) is connected to Health Connect.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { try { ctx.startActivity(vm.steps.settingsIntent()) } catch (_: ActivityNotFoundException) {} }) { Text("Open Health Connect settings") }
                    }
                }
            }
        }
    }
}
