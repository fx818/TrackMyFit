package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(vm: MainViewModel, onOpenDay: (LocalDate) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val unit = data.settings.unit
    var month by remember { mutableStateOf(YearMonth.now()) }
    var labelFilter by remember { mutableStateOf<String?>(null) }
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    val monthDays = remember(data, month, labelFilter) {
        data.days.values.filter {
            val d = LocalDate.parse(it.date)
            YearMonth.from(d) == month && !it.isEmpty && (labelFilter == null || it.labels.any { l -> l.equals(labelFilter, true) })
        }.sortedByDescending { it.date }
    }
    val usedLabels = remember(data) { Stats.labelCounts(data).map { it.first } }

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ElevatedCard {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") }
                            Text(
                                month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") }
                        }
                        CalendarGrid(month, data, onOpenDay)
                    }
                }
            }
            if (usedLabels.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScrollSafe()) {
                        FilterChip(selected = labelFilter == null, onClick = { labelFilter = null }, label = { Text("All") })
                        usedLabels.forEach { l -> FilterChip(selected = labelFilter == l, onClick = { labelFilter = if (labelFilter == l) null else l }, label = { Text(l) }) }
                    }
                }
            }
            if (monthDays.isEmpty()) {
                item { EmptyState(Icons.Filled.EventBusy, "Nothing logged this month", "Tap any day on the calendar to log a workout for it.") }
            }
            items(monthDays, key = { it.date }) { d ->
                val date = LocalDate.parse(d.date)
                ElevatedCard(Modifier.fillMaxWidth().clickable { onOpenDay(date) }) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(fmtLongDate(date), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (d.labels.isNotEmpty()) Text(d.labels.joinToString(" · "), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                            if (d.hasWorkout) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${Stats.setCount(d)} sets" + (d.durationMinutes?.let { " · " + fmtMinutes(it) } ?: ""), style = MaterialTheme.typography.labelMedium)
                                    Text(fmtVolume(Stats.volumeKg(d), unit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (d.entries.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            d.entries.forEach { e ->
                                val ex = exMap[e.exerciseId]
                                val working = e.sets.filter { it.isWorking }
                                val summary = when {
                                    working.isEmpty() -> "—"
                                    (ex?.trackType ?: TrackType.WEIGHT_REPS) == TrackType.WEIGHT_REPS -> {
                                        val top = working.maxOf { it.weightKg }
                                        "${working.size} sets" + if (top > 0) " · top ${fmtWeight(top, unit)}" else " · ${working.sumOf { it.reps }} reps"
                                    }
                                    else -> "${working.size} × " + fmtSetShort(working.maxBy { it.durationSec + it.distanceKm * 1000 }, ex!!.trackType, unit)
                                }
                                Row {
                                    Text(ex?.name ?: "Deleted", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (d.note.isNotBlank()) {
                            Text(d.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.horizontalScrollSafe(): Modifier = this.then(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()))

@Composable
private fun CalendarGrid(month: YearMonth, data: AppData, onOpenDay: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val first = month.atDay(1)
    val offset = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val cells = offset + month.lengthOfMonth()
    val rows = (cells + 6) / 7
    Column {
        Row {
            DayOfWeek.entries.forEach { d ->
                Text(
                    d.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        for (r in 0 until rows) {
            Row {
                for (c in 0 until 7) {
                    val idx = r * 7 + c - offset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (idx in 1..month.lengthOfMonth()) {
                            val date = month.atDay(idx)
                            val day = data.days[date.toString()]
                            val worked = day?.hasWorkout == true
                            val rest = day?.labels?.any { it.equals("Rest", true) } == true
                            val bg = when {
                                worked -> MaterialTheme.colorScheme.primary
                                rest -> MaterialTheme.colorScheme.secondaryContainer
                                day != null -> MaterialTheme.colorScheme.surfaceVariant
                                else -> androidx.compose.ui.graphics.Color.Transparent
                            }
                            val fg = when {
                                worked -> MaterialTheme.colorScheme.onPrimary
                                rest -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape).background(bg).clickable { onOpenDay(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$idx", color = fg,
                                    fontWeight = if (date == today) FontWeight.ExtraBold else FontWeight.Normal,
                                    style = if (date == today) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (date == today && !worked) {
                                Box(Modifier.fillMaxSize().padding(1.dp)) {
                                    Surface(Modifier.fillMaxSize(), shape = CircleShape, color = androidx.compose.ui.graphics.Color.Transparent,
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
