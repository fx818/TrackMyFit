package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
        LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(ex.muscleGroup)
                    if (ex.equipment.isNotBlank()) Pill(ex.equipment)
                    if (ex.isCustom) Pill("Custom", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            if (history.isEmpty()) {
                item { EmptyState(Icons.Filled.Insights, "No history yet", "Log this exercise and your progress will show up here.") }
                return@LazyColumn
            }
            item {
                val best = history.maxBy { it.maxWeightKg }
                val bestRm = history.maxOf { it.bestE1rmKg }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Best weight", fmtWeight(best.maxWeightKg, unit), Icons.Filled.EmojiEvents, Modifier.weight(1f), sub = fmtShortDate(best.date))
                    StatCard("Est. 1RM", fmtWeight(bestRm, unit), Icons.AutoMirrored.Filled.TrendingUp, Modifier.weight(1f))
                    StatCard("Sessions", history.size.toString(), Icons.Filled.CalendarMonth, Modifier.weight(1f))
                }
            }
            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        val labels = listOf("Max weight", "Est. 1RM", "Volume")
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            labels.forEachIndexed { i, l ->
                                SegmentedButton(selected = metric == i, onClick = { metric = i }, shape = SegmentedButtonDefaults.itemShape(i, labels.size)) { Text(l) }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        val pts = history.takeLast(30)
                        val values = pts.map {
                            Units.toDisplay(when (metric) { 0 -> it.maxWeightKg; 1 -> it.bestE1rmKg; else -> it.volumeKg }, unit)
                        }
                        if (values.all { it == 0.0 }) {
                            Text("No weighted sets yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LineChart(values, pts.map { fmtShortDate(it.date) })
                        }
                    }
                }
            }
            item { SectionTitle("History") }
            items(history.reversed(), key = { it.entry.id }) { h ->
                OutlinedCard(Modifier.fillMaxWidth().clickable { onOpenDay(h.date) }) {
                    Column(Modifier.padding(12.dp)) {
                        Row {
                            Text(fmtLongDate(h.date), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(fmtVolume(h.volumeKg, unit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            h.entry.sets.mapIndexed { i, s -> "${i + 1}) ${fmtWeight(s.weightKg, unit)} × ${s.reps}" }.joinToString("   "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (h.entry.note.isNotBlank()) Text(h.entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (edit && ex != null) {
        ExerciseEditDialog(initial = ex, onDismiss = { edit = false }) { n, g, eq -> vm.updateCustomExercise(ex.id, n, g, eq); edit = false }
    }
    if (confirmDelete && ex != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${ex.name}?") },
            text = { Text("This also removes it from ${history.size} logged workout(s). This can't be undone.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onBack(); vm.deleteCustomExercise(ex.id) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
