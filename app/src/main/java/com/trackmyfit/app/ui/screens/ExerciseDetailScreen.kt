package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.LocalDate

@Composable
fun ExerciseDetailScreen(vm: MainViewModel, exerciseId: String, onBack: () -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val ex = data.exerciseMap()[exerciseId]
    val unit = data.settings.unit
    val history = remember(data, exerciseId) { Stats.exerciseHistory(data, exerciseId) }
    var tab by remember { mutableIntStateOf(0) }
    var metric by remember { mutableIntStateOf(0) }
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                title = { Text(ex?.name ?: "Exercise") },
                actions = {
                    if (ex?.isCustom == true) {
                        IconButton(onClick = { edit = true }) { Icon(Icons.Filled.Edit, "Edit") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete") }
                    }
                },
            )
        },
    ) { padding ->
        if (ex == null) {
            EmptyState(Icons.Filled.SearchOff, "Exercise not found", "", Modifier.padding(padding))
            return@Scaffold
        }
        val type = ex.trackType
        LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(ex.muscleGroup)
                    if (ex.equipment.isNotBlank()) Pill(ex.equipment)
                    Pill(type.label, MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
                    if (ex.isCustom) Pill("Custom", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            item {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("How to") }, icon = { Icon(Icons.Filled.OndemandVideo, null) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Progress (${history.size})") }, icon = { Icon(Icons.Filled.Insights, null) })
                }
            }
            if (tab == 0) {
                item { ExerciseInfoContent(ex) }
                return@LazyColumn
            }
            if (history.isEmpty()) {
                item { EmptyState(Icons.Filled.Insights, "No history yet", "Log this exercise and your progress will show up here.") }
                return@LazyColumn
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (type) {
                        TrackType.WEIGHT_REPS -> {
                            val best = history.maxBy { it.maxWeightKg }
                            if (best.maxWeightKg > 0) {
                                StatCard("Best weight", fmtWeight(best.maxWeightKg, unit), Icons.Filled.EmojiEvents, Modifier.weight(1f), sub = fmtShortDate(best.date))
                                StatCard("Est. 1RM", fmtWeight(history.maxOf { it.bestE1rmKg }, unit), Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
                            } else {
                                StatCard("Best reps", history.maxOf { it.maxReps }.toString(), Icons.Filled.EmojiEvents, Modifier.weight(1f))
                            }
                        }
                        TrackType.TIME -> StatCard("Longest", fmtDuration(history.maxOf { it.maxDurationSec }), Icons.Filled.EmojiEvents, Modifier.weight(1f))
                        TrackType.DISTANCE_TIME -> {
                            StatCard("Total distance", fmtDistance(history.sumOf { it.totalDistanceKm }, unit), Icons.Filled.Route, Modifier.weight(1f))
                            StatCard("Total time", fmtMinutes(history.sumOf { it.totalDurationSec } / 60), Icons.Filled.Timer, Modifier.weight(1f))
                        }
                    }
                    StatCard("Sessions", history.size.toString(), Icons.Filled.CalendarMonth, Modifier.weight(1f))
                }
            }
            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        val weighted = history.any { it.maxWeightKg > 0 }
                        val labels = when (type) {
                            TrackType.WEIGHT_REPS -> if (weighted) listOf("Max weight", "Est. 1RM", "Volume") else listOf("Max reps", "Total reps")
                            TrackType.TIME -> listOf("Longest set", "Total time")
                            TrackType.DISTANCE_TIME -> listOf("Distance", "Time")
                        }
                        if (metric >= labels.size) metric = 0
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            labels.forEachIndexed { i, l ->
                                SegmentedButton(selected = metric == i, onClick = { metric = i }, shape = SegmentedButtonDefaults.itemShape(i, labels.size)) { Text(l) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val pts = history.takeLast(30)
                        val (values, fmt) = when (type) {
                            TrackType.WEIGHT_REPS -> if (weighted) pts.map {
                                Units.toDisplay(when (metric) { 0 -> it.maxWeightKg; 1 -> it.bestE1rmKg; else -> it.volumeKg }, unit)
                            } to { v: Double -> fmtNum(v) } else pts.map { (if (metric == 0) it.maxReps else it.totalReps).toDouble() } to { v: Double -> "%.0f".format(v) }
                            TrackType.TIME -> pts.map { (if (metric == 0) it.maxDurationSec else it.totalDurationSec).toDouble() } to { v: Double -> fmtDuration(v.toInt()) }
                            TrackType.DISTANCE_TIME -> (if (metric == 0) pts.map { Units.distToDisplay(it.totalDistanceKm, unit) } else pts.map { it.totalDurationSec / 60.0 }) to
                                { v: Double -> if (metric == 0) fmtNum(v) else "%.0fm".format(v) }
                        }
                        if (values.all { it == 0.0 }) Text("Nothing to chart yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else LineChart(values, pts.map { fmtShortDate(it.date) }, valueFormatter = fmt)
                    }
                }
            }
            item { SectionTitle("History") }
            items(history.reversed(), key = { it.entry.id }) { h ->
                OutlinedCard(Modifier.fillMaxWidth().clickable { onOpenDay(h.date) }) {
                    Column(Modifier.padding(12.dp)) {
                        Row {
                            Text(fmtLongDate(h.date), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            if (type == TrackType.WEIGHT_REPS && h.volumeKg > 0) Text(fmtVolume(h.volumeKg, unit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        var n = 0
                        Text(
                            h.entry.sets.joinToString("   ") { s -> (if (s.isWorking) "${++n})" else "W)") + " " + fmtSet(s, type, unit) + (if (s.type == SetType.DROP || s.type == SetType.FAILURE) " (${s.type.short})" else "") },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (h.entry.note.isNotBlank()) Text(h.entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (edit && ex != null) {
        ExerciseEditDialog(initial = ex, onDismiss = { edit = false }) { n, g, eq, t -> vm.updateCustomExercise(ex.id, n, g, eq, t); edit = false }
    }
    if (confirmDelete && ex != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${ex.name}?") },
            text = { Text("This also removes it from ${history.size} logged workout(s) and any routines. This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onBack(); vm.deleteCustomExercise(ex.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
