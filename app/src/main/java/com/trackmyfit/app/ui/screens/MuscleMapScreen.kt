package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.DayOfWeek
import java.time.LocalDate

enum class MapRange(val label: String) { WEEK("This week"), D7("Last 7 days"), D30("Last 30 days") }

fun rangeDates(r: MapRange, today: LocalDate): Pair<LocalDate, LocalDate> = when (r) {
    MapRange.WEEK -> today.with(DayOfWeek.MONDAY) to today
    MapRange.D7 -> today.minusDays(6) to today
    MapRange.D30 -> today.minusDays(29) to today
}

/** Recommended weekly hard sets per muscle (rough evidence-based range for hypertrophy). */
private const val TARGET_WEEKLY = 10.0

@Composable
fun rememberMuscleLoad(data: AppData, from: LocalDate, to: LocalDate): Map<String, Double> {
    val ctx = LocalContext.current
    return remember(data, from, to) {
        val exMap = data.exerciseMap()
        Muscles.load(data, from, to, exMap) { ExerciseInfoRepo.get(ctx, it) }
    }
}

/** Intensity relative to the weekly target (scaled for longer ranges). */
fun intensities(load: Map<String, Double>, days: Long): Map<String, Float> {
    val target = TARGET_WEEKLY * (days / 7.0).coerceAtLeast(1.0)
    return load.mapValues { (it.value / target).toFloat().coerceIn(0f, 1f) }
}

@Composable
fun MuscleMapCard(data: AppData, onOpen: () -> Unit) {
    val today = LocalDate.now()
    val (from, to) = rangeDates(MapRange.D7, today)
    val load = rememberMuscleLoad(data, from, to)
    DashCard("Muscles trained · last 7 days", Icons.Filled.Accessibility, action = { TextButton(onClick = onOpen) { Text("Details") } }) {
        if (load.isEmpty()) {
            Text("Log a workout and the muscles you trained will light up here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
        }
        BodyHeatmap(intensities(load, 7), height = 220.dp)
        Spacer(Modifier.height(6.dp))
        HeatLegend(Modifier.align(Alignment.CenterHorizontally))
        val neglected = Muscles.regions.filter { (load[it.id] ?: 0.0) < 2.0 }.map { it.label }
        if (load.isNotEmpty() && neglected.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("Little or no work: " + neglected.take(5).joinToString(", ") + if (neglected.size > 5) "…" else "",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MuscleMapScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    var range by remember { mutableStateOf(MapRange.D7) }
    val today = LocalDate.now()
    val (from, to) = rangeDates(range, today)
    val days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1
    val load = rememberMuscleLoad(data, from, to)
    val target = TARGET_WEEKLY * (days / 7.0).coerceAtLeast(1.0)

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Muscle map") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MapRange.entries.forEachIndexed { i, r ->
                        SegmentedButton(selected = range == r, onClick = { range = r }, shape = SegmentedButtonDefaults.itemShape(i, MapRange.entries.size)) { Text(r.label) }
                    }
                }
            }
            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BodyHeatmap(intensities(load, days), height = 320.dp)
                        Spacer(Modifier.height(8.dp))
                        HeatLegend()
                    }
                }
            }
            item {
                Text(
                    "Bars show working sets per muscle (secondary muscles count as half a set). " +
                        "Around ${target.toInt()} sets per muscle in this period is a good target for muscle growth.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val rows = Muscles.regions.map { it to (load[it.id] ?: 0.0) }.sortedByDescending { it.second }
            items(rows, key = { it.first.id }) { (r, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.label, Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        val frac = (v / (target * 1.5)).toFloat().coerceIn(0f, 1f)
                        if (frac > 0f) Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(7.dp)).background(heatColor((v / target).toFloat(), MaterialTheme.colorScheme.outline)))
                    }
                    Text(
                        fmtNum(Math.round(v * 2) / 2.0), Modifier.width(44.dp).padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        color = when {
                            v < target * 0.3 -> MaterialTheme.colorScheme.error
                            v >= target -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
            item {
                Text("Muscle diagram based on react-body-highlighter (MIT).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
