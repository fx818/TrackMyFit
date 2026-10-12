package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

enum class Range(val label: String, val days: Long?) { M1("1M", 30), M3("3M", 90), M6("6M", 182), Y1("1Y", 365), ALL("All", null) }

@Composable
fun DashboardScreen(
    vm: MainViewModel,
    onOpenLog: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    val data by vm.data.collectAsStateWithLifecycle()
    val unit = data.settings.unit
    val today = LocalDate.now()
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    var showWeight by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val hour = LocalTime.now().hour
                        val greet = when { hour < 12 -> "Good morning"; hour < 17 -> "Good afternoon"; else -> "Good evening" }
                        Text(greet + (data.profile.name.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(fmtLongDate(today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Settings") } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { TodayCard(data, today, exMap, onOpenLog) }
            item { QuickStats(data, today, unit) }
            item { BodyCard(data, unit, onLogWeight = { showWeight = true }, onOpenBody = onOpenBody) }
            item { WeightTrendCard(data, unit, onLogWeight = { showWeight = true }) }
            item { WorkoutFrequencyCard(data, today) }
            item { VolumeCard(data, today, unit) }
            item { StrengthCard(data, exMap, unit, onOpenExercise) }
            item { MuscleSplitCard(data, today, exMap) }
            item { RecentPrsCard(data, exMap, unit, onOpenExercise) }
        }
    }
    if (showWeight) {
        WeightLogDialog(
            unit = unit, initialKg = data.bodyWeights.lastOrNull()?.weightKg,
            onDismiss = { showWeight = false },
            onSave = { d, kg -> vm.logBodyWeight(d, kg); showWeight = false },
        )
    }
}

@Composable
fun DashCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                action?.invoke()
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun TodayCard(data: AppData, today: LocalDate, exMap: Map<String, Exercise>, onOpenLog: () -> Unit) {
    val day = data.dayFor(today)
    Card(
        onClick = onOpenLog,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("TODAY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (day.entries.isEmpty()) {
                    Text(
                        if (day.labels.any { it.equals("Rest", true) }) "Rest day — recover well" else "Ready to train?",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (day.labels.isNotEmpty()) "Focus: " + day.labels.joinToString(" · ") else "Tap to start logging today's workout",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(day.labels.joinToString(" · ").ifEmpty { "Workout" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${day.entries.size} exercises · ${Stats.setCount(day)} sets\n" + day.entries.mapNotNull { exMap[it.exerciseId]?.name }.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(if (day.entries.isEmpty()) Icons.Filled.PlayCircle else Icons.Filled.Edit, null, Modifier.size(40.dp))
        }
    }
}

@Composable
private fun QuickStats(data: AppData, today: LocalDate, unit: WeightUnit) {
    val weekStart = today.with(DayOfWeek.MONDAY)
    val thisWeek = Stats.workoutsBetween(data, weekStart, today)
    val streak = Stats.currentStreak(data, today)
    val vols = Stats.weeklyVolumeKg(data, 2, today)
    val total = Stats.totalWorkouts(data)
    // Compare against last week only once this week has some volume (avoids "-100%" every Monday).
    val volChange = if (vols[0].second > 0 && vols[1].second > 0) (vols[1].second - vols[0].second) / vols[0].second * 100 else null
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("This week", "$thisWeek", Icons.Filled.CalendarMonth, Modifier.width(120.dp), sub = "workouts")
        StatCard("Streak", "$streak", Icons.Filled.LocalFireDepartment, Modifier.width(120.dp), sub = if (streak == 1) "day" else "days")
        StatCard("Week volume", fmtVolume(vols[1].second, unit), Icons.Filled.FitnessCenter, Modifier.width(150.dp),
            sub = volChange?.let { (if (it >= 0) "+" else "") + "%.0f%% vs last week".format(it) } ?: "lifted")
        StatCard("All time", "$total", Icons.Filled.EmojiEvents, Modifier.width(120.dp), sub = "workouts")
    }
}

@Composable
fun ChangeChip(deltaKg: Double, unit: WeightUnit, label: String) {
    val icon = when { deltaKg > 0.05 -> Icons.AutoMirrored.Filled.TrendingUp; deltaKg < -0.05 -> Icons.AutoMirrored.Filled.TrendingDown; else -> Icons.AutoMirrored.Filled.TrendingFlat }
    val word = when { deltaKg > 0.05 -> "gained"; deltaKg < -0.05 -> "lost"; else -> "no change" }
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(
                    (if (deltaKg > 0.05) "+" else if (deltaKg < -0.05) "−" else "") + fmtWeight(abs(deltaKg), unit),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                )
            }
            Text(word, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Weight change over the last [days] days, using the closest entry on/before the start date. */
fun weightChange(entries: List<BodyWeightEntry>, days: Long?, today: LocalDate = LocalDate.now()): Double? {
    if (entries.size < 2) return null
    val last = entries.last()
    val base = if (days == null) entries.first() else {
        val start = today.minusDays(days)
        entries.lastOrNull { LocalDate.parse(it.date) <= start } ?: entries.first()
    }
    if (base == last) return null
    return last.weightKg - base.weightKg
}

@Composable
private fun BodyCard(data: AppData, unit: WeightUnit, onLogWeight: () -> Unit, onOpenBody: () -> Unit) {
    val latest = data.bodyWeights.lastOrNull()
    val h = data.profile.heightCm
    DashCard("Body & BMI", Icons.Filled.MonitorWeight, action = {
        TextButton(onClick = onOpenBody) { Text("Details") }
    }) {
        if (latest == null || h <= 0) {
            Text(
                when {
                    h <= 0 && latest == null -> "Add your height and log your weight to track BMI and weight gain / loss."
                    h <= 0 -> "Add your height to calculate your BMI."
                    else -> "Log your weight to calculate your BMI."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (h <= 0) OutlinedButton(onClick = onOpenBody) { Text("Set height") }
                Button(onClick = onLogWeight) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Log weight") }
            }
            if (latest != null) {
                Spacer(Modifier.height(8.dp))
                Text("Current weight: ${fmtWeight(latest.weightKg, unit)}", fontWeight = FontWeight.SemiBold)
            }
            return@DashCard
        }
        val bmi = Bmi.calc(latest.weightKg, h)!!
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtWeight(latest.weightKg, unit), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("on " + fmtShortDate(LocalDate.parse(latest.date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("BMI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("%.1f".format(bmi), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = bmiColor(bmi))
                Text(Bmi.category(bmi), style = MaterialTheme.typography.labelMedium, color = bmiColor(bmi), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(10.dp))
        BmiGauge(bmi)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            weightChange(data.bodyWeights, 7)?.let { ChangeChip(it, unit, "7 days") }
            weightChange(data.bodyWeights, 30)?.let { ChangeChip(it, unit, "30 days") }
            weightChange(data.bodyWeights, null)?.let { ChangeChip(it, unit, "Since start") }
        }
        val goal = data.profile.goalWeightKg
        if (goal > 0) {
            val start = data.bodyWeights.first().weightKg
            val toGo = goal - latest.weightKg
            val totalNeeded = goal - start
            val progress = if (abs(totalNeeded) < 0.01) 1f else ((latest.weightKg - start) / totalNeeded).toFloat().coerceIn(0f, 1f)
            Spacer(Modifier.height(12.dp))
            Row {
                Text("Goal ${fmtWeight(goal, unit)}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(
                    if (abs(toGo) < 0.05) "Goal reached! 🎉" else (if (toGo > 0) "${fmtWeight(toGo, unit)} to gain" else "${fmtWeight(-toGo, unit)} to lose"),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogWeight, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Log today's weight") }
    }
}

@Composable
private fun RangeSelector(range: Range, onChange: (Range) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        Range.entries.forEachIndexed { i, r ->
            SegmentedButton(selected = range == r, onClick = { onChange(r) }, shape = SegmentedButtonDefaults.itemShape(i, Range.entries.size), icon = {}) {
                Text(r.label)
            }
        }
    }
}

@Composable
fun WeightTrendCard(data: AppData, unit: WeightUnit, onLogWeight: () -> Unit) {
    var range by remember { mutableStateOf(Range.M3) }
    val today = LocalDate.now()
    val pts = remember(data.bodyWeights, range) {
        val d = range.days
        data.bodyWeights.filter { d == null || LocalDate.parse(it.date) >= today.minusDays(d) }
    }
    DashCard("Weight trend", Icons.AutoMirrored.Filled.ShowChart) {
        if (data.bodyWeights.size < 2) {
            Text("Log your weight on at least two days to see the graph of how much you gain or lose.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLogWeight) { Text("Log weight") }
            return@DashCard
        }
        RangeSelector(range) { range = it }
        Spacer(Modifier.height(10.dp))
        if (pts.size < 2) {
            Text("Not enough entries in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val delta = pts.last().weightKg - pts.first().weightKg
            Text(
                when {
                    delta > 0.05 -> "▲ Gained ${fmtWeight(delta, unit)}"
                    delta < -0.05 -> "▼ Lost ${fmtWeight(-delta, unit)}"
                    else -> "● Stable"
                } + " in ${if (range == Range.ALL) "total" else "the last " + range.label}",
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            LineChart(
                pts.map { Units.toDisplay(it.weightKg, unit) },
                pts.map { fmtShortDate(LocalDate.parse(it.date)) },
                refLine = data.profile.goalWeightKg.takeIf { it > 0 }?.let { Units.toDisplay(it, unit) },
            )
            if (data.profile.goalWeightKg > 0) {
                Text("Dashed line = goal weight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun WorkoutFrequencyCard(data: AppData, today: LocalDate) {
    val weeks = remember(data) { Stats.weeklyCounts(data, 12, today) }
    DashCard("Workouts per week", Icons.Filled.BarChart) {
        if (weeks.all { it.second == 0 }) {
            Text("Your weekly workout count will appear here once you log workouts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DashCard
        }
        val avg = weeks.map { it.second }.average()
        Text("Avg %.1f / week over 12 weeks".format(avg), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        BarChart(weeks.map { it.second }, weeks.map { fmtShortDate(it.first) })
    }
}

@Composable
private fun VolumeCard(data: AppData, today: LocalDate, unit: WeightUnit) {
    // Drop the current week while it's still empty so the line doesn't misleadingly dive to 0.
    val weeks = remember(data) { Stats.weeklyVolumeKg(data, 12, today).let { w -> if (w.last().second == 0.0) w.dropLast(1) else w } }
    DashCard("Training volume", Icons.AutoMirrored.Filled.TrendingUp) {
        if (weeks.all { it.second == 0.0 }) {
            Text("Total weight lifted per week (sets × reps × weight) will be graphed here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DashCard
        }
        Text("Weekly weight lifted (${unit.label})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LineChart(
            weeks.map { Units.toDisplay(it.second, unit) }, weeks.map { fmtShortDate(it.first) },
            valueFormatter = { if (it >= 10000) "%.0fk".format(it / 1000) else if (it >= 1000) "%.1fk".format(it / 1000) else "%.0f".format(it) },
        )
    }
}

@Composable
private fun StrengthCard(data: AppData, exMap: Map<String, Exercise>, unit: WeightUnit, onOpenExercise: (String) -> Unit) {
    val top = remember(data) { Stats.topWeightedExercises(data, 8) }
    var selected by remember(top) { mutableStateOf(top.firstOrNull()) }
    DashCard("Strength progress", Icons.Filled.EmojiEvents) {
        val sel = selected
        if (sel == null) {
            Text("Log weighted sets and your estimated 1-rep max per exercise will be graphed here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DashCard
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            top.forEach { id -> FilterChip(selected = id == sel, onClick = { selected = id }, label = { Text(exMap[id]?.name ?: "?") }) }
        }
        val hist = remember(data, sel) { Stats.exerciseHistory(data, sel).filter { it.maxWeightKg > 0 }.takeLast(20) }
        Spacer(Modifier.height(8.dp))
        if (hist.size >= 2) {
            val first = hist.first().bestE1rmKg; val last = hist.last().bestE1rmKg
            val pct = if (first > 0) (last - first) / first * 100 else 0.0
            Text(
                "Est. 1RM ${fmtWeight(last, unit)}  (" + (if (pct >= 0) "+" else "") + "%.1f%%)".format(pct),
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary,
            )
            LineChart(hist.map { Units.toDisplay(it.bestE1rmKg, unit) }, hist.map { fmtShortDate(it.date) })
        } else {
            Text("Log this exercise in one more session to see the trend.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { onOpenExercise(sel) }) { Text("Full history") }
    }
}

private val groupColors = listOf(
    Color(0xFFFF7A2F), Color(0xFF4FA3E0), Color(0xFF3DBE7A), Color(0xFFF2B33D), Color(0xFFB57EDC), Color(0xFFE5534B),
    Color(0xFF2EC4B6), Color(0xFFFF9EB5), Color(0xFF8D99AE), Color(0xFFC9E265), Color(0xFF6C8CFF), Color(0xFFD4A373),
)

@Composable
private fun MuscleSplitCard(data: AppData, today: LocalDate, exMap: Map<String, Exercise>) {
    val split = remember(data) { Stats.muscleSplit(data, today.minusDays(29), today, exMap) }
    DashCard("Muscle focus · last 30 days", Icons.Filled.Accessibility) {
        if (split.isEmpty()) {
            Text("Sets per muscle group will show here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@DashCard
        }
        val max = split.maxOf { it.second }.toFloat()
        split.forEach { (g, n) ->
            val c = groupColors[Catalog.muscleGroups.indexOf(g).coerceAtLeast(0) % groupColors.size]
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(g, Modifier.width(84.dp), style = MaterialTheme.typography.labelLarge)
                Box(Modifier.weight(1f).height(14.dp)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(n / max).clip(RoundedCornerShape(7.dp)).background(c))
                }
                Text("$n sets", Modifier.width(60.dp).padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecentPrsCard(data: AppData, exMap: Map<String, Exercise>, unit: WeightUnit, onOpenExercise: (String) -> Unit) {
    val prs = remember(data) { Stats.personalRecords(data).take(5) }
    if (prs.isEmpty()) return
    DashCard("Personal records", Icons.Filled.MilitaryTech) {
        prs.forEach { pr ->
            Row(Modifier.fillMaxWidth().clickable { onOpenExercise(pr.exerciseId) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(exMap[pr.exerciseId]?.name ?: "?", fontWeight = FontWeight.Medium)
                    Text(fmtLongDate(pr.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${fmtWeight(pr.weightKg, unit)} × ${pr.reps}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
