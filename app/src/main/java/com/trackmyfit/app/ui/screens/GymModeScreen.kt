package com.trackmyfit.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Letter labels (A, B, C...) for the supersets of a day, in order of appearance. */
fun groupLetters(entries: List<ExerciseEntry>): Map<String, Char> {
    val m = LinkedHashMap<String, Char>()
    entries.forEach { if (it.group.isNotEmpty() && it.group !in m) m[it.group] = 'A' + m.size }
    return m
}

private data class Planned(val reps: Int, val weightKg: Double, val durationSec: Int, val distanceKm: Double)

@Composable
fun GymModeScreen(vm: MainViewModel, onExit: () -> Unit, onOpenExercise: (String) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val date by vm.selectedDate.collectAsStateWithLifecycle()
    val rest by vm.restRemaining.collectAsStateWithLifecycle()
    val restTotal by vm.restTotal.collectAsStateWithLifecycle()
    val day = data.dayFor(date)
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    val unit = data.settings.unit
    val letters = remember(day.entries) { groupLetters(day.entries) }

    // Keep the screen on while training.
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }

    var index by rememberSaveable { mutableIntStateOf(firstUnfinished(day)) }
    if (day.entries.isEmpty()) {
        Surface(Modifier.fillMaxSize()) {
            EmptyState(Icons.Filled.FitnessCenter, "Nothing planned", "Add exercises or start a routine on the Log screen first.", Modifier.systemBarsPadding()) {
                Button(onClick = onExit) { Text("Back") }
            }
        }
        return
    }
    index = index.coerceIn(0, day.entries.lastIndex)
    val entry = day.entries[index]
    val ex = exMap[entry.exerciseId]
    var howTo by remember { mutableStateOf(false) }
    var editSet by remember { mutableStateOf<Pair<Int, WorkoutSet>?>(null) }
    var plates by remember { mutableStateOf<Double?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }

    /** After logging a set: superset -> next exercise in the round; otherwise stay until target sets are done. */
    fun advanceAfterSet(loggedEntryId: String) {
        val d = vm.data.value.dayFor(date)
        val i = d.entries.indexOfFirst { it.id == loggedEntryId }
        if (i < 0) return
        val e = d.entries[i]
        fun setsLeft(x: ExerciseEntry): Int = (x.targetSets - x.sets.count { it.isWorking }).coerceAtLeast(0)
        if (e.group.isNotEmpty()) {
            val members = d.entries.withIndex().filter { it.value.group == e.group }
            fun done(x: ExerciseEntry) = x.sets.count { it.isWorking }
            val top = members.maxOf { done(it.value) }
            // 1) someone hasn't done this round yet: go to the next one (in order, wrapping, me last)
            val ordered = members.filter { it.index > i } + members.filter { it.index <= i }
            val behind = ordered.firstOrNull { done(it.value) < top }
            if (behind != null) { index = behind.index; return }
            // 2) round complete: start the next round at the first member that still has sets to do
            val hasTargets = members.any { it.value.targetSets > 0 }
            val nextRound = if (hasTargets) members.firstOrNull { setsLeft(it.value) > 0 } else members.first()
            if (nextRound != null) { index = nextRound.index; return }
            // 3) superset finished: move on
            val nextOutside = d.entries.withIndex().firstOrNull { it.index > members.last().index && (setsLeft(it.value) > 0 || it.value.sets.isEmpty()) }
            if (nextOutside != null) index = nextOutside.index
            return
        }
        if (e.targetSets > 0 && setsLeft(e) == 0) {
            val next = d.entries.withIndex().firstOrNull { it.index > i && (setsLeft(it.value) > 0 || it.value.sets.isEmpty()) }
            if (next != null) index = next.index
        }
    }

    Scaffold(
        topBar = {
            GymTopBar(day, date, onExit = onExit, onFinish = { confirmFinish = true }, onStart = { vm.startWorkout(date) })
        },
        bottomBar = {
            Column {
                AnimatedVisibility(rest != null) {
                    GymRestBar(rest ?: 0, restTotal, onAdd = { vm.addRest(15) }, onSub = { vm.addRest(-15) }, onSkip = { vm.stopRest() })
                }
                ExerciseStrip(day, exMap, letters, index) { index = it }
            }
        },
    ) { padding ->
        AnimatedContent(targetState = entry.id, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "ex", modifier = Modifier.padding(padding)) { _ ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { index = (index - 1).coerceAtLeast(0) }, enabled = index > 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous exercise") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Exercise ${index + 1} of ${day.entries.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(ex?.name ?: "Exercise", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (entry.group.isNotEmpty()) {
                            val members = day.entries.filter { it.group == entry.group }
                            val pos = members.indexOfFirst { it.id == entry.id } + 1
                            Pill(
                                (if (members.size >= 3) "Circuit " else "Superset ") + "${letters[entry.group]} · $pos/${members.size}",
                                MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }
                    IconButton(onClick = { index = (index + 1).coerceAtMost(day.entries.lastIndex) }, enabled = index < day.entries.lastIndex) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next exercise") }
                }
                val ctx = LocalContext.current
                if (ex != null && hasDemo(ctx, ex.id)) {
                    // Compact demo: keeps the "Done" button above the fold on most phones.
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { howTo = true }) {
                        Box(Modifier.width(150.dp)) { ExerciseDemo(ex.id, showControls = false) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.OndemandVideo, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Form guide", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            Text(listOfNotNull(ex.muscleGroup, ex.equipment.ifBlank { null }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                val type = ex?.trackType ?: TrackType.WEIGHT_REPS
                val prev = remember(data, date, entry.exerciseId) { Stats.lastBefore(data, entry.exerciseId, date) }
                val sugg = remember(data, date, entry) { Stats.suggest(data, ex, entry, date, unit) }
                if (prev != null) {
                    Text(
                        "Last time (${fmtShortDate(prev.date)}): " + prev.entry.sets.filter { it.isWorking }.joinToString(", ") { fmtSetShort(it, type, unit) },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (sugg != null && entry.sets.none { it.isWorking }) {
                    Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.TipsAndUpdates, null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when (sugg.kind) {
                                    Stats.SuggestKind.INCREASE_WEIGHT -> "Go for ${fmtWeight(sugg.weightKg, unit)} × ${sugg.reps} today 💪"
                                    Stats.SuggestKind.MATCH_REPS -> "Stay at ${fmtWeight(sugg.weightKg, unit)}, aim for ${sugg.reps} reps every set"
                                    Stats.SuggestKind.ADD_REPS -> "Aim for ${sugg.reps} reps per set"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // Planned / logged sets
                val working = entry.sets.count { it.isWorking }
                val target = if (entry.targetSets > 0) entry.targetSets else maxOf(sugg?.sets ?: 3, working + 1)
                val pendingCount = (target - working).coerceAtLeast(if (entry.sets.isEmpty()) 1 else 0)
                val base: Planned = run {
                    val last = entry.sets.lastOrNull { it.isWorking }
                    when {
                        last != null -> Planned(last.reps, last.weightKg, last.durationSec, last.distanceKm)
                        sugg != null -> Planned(sugg.reps, sugg.weightKg, 0, 0.0)
                        entry.targetReps > 0 || entry.targetWeightKg > 0 || entry.targetDurationSec > 0 -> Planned(entry.targetReps, entry.targetWeightKg, entry.targetDurationSec, 0.0)
                        prev != null -> prev.entry.sets.first { it.isWorking || true }.let { Planned(it.reps, it.weightKg, it.durationSec, it.distanceKm) }
                        else -> Planned(if (type == TrackType.WEIGHT_REPS) 10 else 0, 0.0, if (type == TrackType.TIME) 60 else 0, 0.0)
                    }
                }
                Text(
                    "Sets  ·  $working / $target done",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                )
                var n = 0
                entry.sets.forEachIndexed { i, s ->
                    val label = if (s.isWorking) "${++n}" else "W"
                    DoneSetRow(label, s, type, unit) { editSet = i to s }
                }
                // The active (next) set is editable inline; later planned sets are previews.
                key(entry.id, entry.sets.size) {
                    if (pendingCount > 0) {
                        val bring = remember { BringIntoViewRequester() }
                        LaunchedEffect(Unit) { if (entry.sets.isNotEmpty()) { delay(150); bring.bringIntoView() } }
                        ActiveSetCard(
                            modifier = Modifier.bringIntoViewRequester(bring),
                            setNumber = working + 1, planned = base, type = type, unit = unit,
                            isBarbell = ex?.equipment == "Barbell" || ex?.equipment == "Smith Machine",
                            onPlates = { plates = it },
                            onDone = { set ->
                                vm.addSets(date, entry.id, set, 1, ex?.name)
                                advanceAfterSet(entry.id)
                            },
                        )
                        for (k in 1 until pendingCount) {
                            PlannedRow(working + 1 + k, base, type, unit)
                        }
                    }
                }
                OutlinedButton(
                    onClick = { vm.setEntryTarget(date, entry.id, target + 1, entry.targetReps.takeIf { it > 0 } ?: base.reps, entry.targetWeightKg.takeIf { it > 0 } ?: base.weightKg, entry.targetDurationSec.takeIf { it > 0 } ?: base.durationSec) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add another set") }
                if (pendingCount == 0 && index < day.entries.lastIndex) {
                    Button(onClick = { index++ }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Next: ${exMap[day.entries[index + 1].exerciseId]?.name ?: "exercise"}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
                    }
                }
                if (pendingCount == 0 && index == day.entries.lastIndex) {
                    Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Filled.Flag, null); Spacer(Modifier.width(6.dp)); Text("Finish workout")
                    }
                }
                TextButton(onClick = { onOpenExercise(entry.exerciseId) }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("History & progress") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    if (howTo && ex != null) HowToSheet(ex) { howTo = false }
    editSet?.let { (i, s) ->
        SetDialog(
            state = SetDialogState(entry.id, s.id, i + 1, ex, s, 1), unit = unit,
            onDismiss = { editSet = null },
            onSave = { set, _ -> vm.updateSet(date, entry.id, set.copy(id = s.id)); editSet = null },
            onDelete = { vm.deleteSet(date, entry.id, s.id); editSet = null },
            onPlates = { plates = it },
        )
    }
    plates?.let { w -> PlateCalculatorDialog(unit, w, data.settings.barWeightKg, onBarChange = { b -> vm.updateSettings { it.copy(barWeightKg = b) } }) { plates = null } }
    if (confirmFinish) {
        val totalSets = Stats.setCount(day)
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            icon = { Icon(Icons.Filled.EmojiEvents, null) },
            title = { Text("Finish workout?") },
            text = {
                Text("$totalSets sets · ${fmtVolume(Stats.volumeKg(day), unit)} lifted" +
                    (day.startedAt.takeIf { it > 0 }?.let { " · " + fmtMinutes(((System.currentTimeMillis() - it) / 60000).toInt().coerceAtLeast(1)) } ?: ""))
            },
            confirmButton = { TextButton(onClick = { vm.finishWorkout(date); vm.stopRest(); confirmFinish = false; onExit() }) { Text("Finish") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep going") } },
        )
    }
}

private fun firstUnfinished(day: WorkoutDay): Int {
    val i = day.entries.indexOfFirst { e -> e.sets.isEmpty() || (e.targetSets > 0 && e.sets.count { it.isWorking } < e.targetSets) }
    return if (i < 0) 0 else i
}

@Composable
private fun GymTopBar(day: WorkoutDay, date: LocalDate, onExit: () -> Unit, onFinish: () -> Unit, onStart: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(day.startedAt) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    Surface(tonalElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Filled.Close, "Exit gym mode") }
            Column(Modifier.weight(1f)) {
                Text(day.labels.joinToString(" · ").ifEmpty { "Workout" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                when {
                    day.isTimerRunning -> Text("⏱ " + fmtDuration(((now - day.startedAt) / 1000).toInt()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    day.durationMinutes != null -> Text("Duration ${fmtMinutes(day.durationMinutes!!)}", style = MaterialTheme.typography.labelLarge)
                    date == LocalDate.now() -> Text("Timer starts with your first set", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Text(fmtLongDate(date), style = MaterialTheme.typography.labelSmall)
                }
            }
            if (!day.isTimerRunning && day.durationMinutes == null && date == LocalDate.now()) {
                TextButton(onClick = onStart) { Text("Start timer") }
            }
            FilledTonalButton(onClick = onFinish) { Icon(Icons.Filled.Flag, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Finish") }
        }
    }
}

@Composable
private fun DoneSetRow(label: String, s: WorkoutSet, type: TrackType, unit: WeightUnit, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Set $label", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp))
            Text(fmtSet(s, type, unit), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            SetTypeBadge(s.type)
            Icon(Icons.Filled.Edit, "Edit", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PlannedRow(number: Int, p: Planned, type: TrackType, unit: WeightUnit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
            Text("$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            fmtSet(WorkoutSet("", p.reps, p.weightKg, p.durationSec, p.distanceKm), type, unit),
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Big tappable value with − / + buttons. */
@Composable
private fun BigStepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(44.dp)) { Icon(Icons.Filled.Remove, "Less") }
            Text(
                value, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 64.dp).clickable(onClick = onTap).padding(horizontal = 6.dp),
                maxLines = 1,
            )
            FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(44.dp)) { Icon(Icons.Filled.Add, "More") }
        }
    }
}

@Composable
private fun ActiveSetCard(
    modifier: Modifier = Modifier,
    setNumber: Int,
    planned: Planned,
    type: TrackType,
    unit: WeightUnit,
    isBarbell: Boolean,
    onPlates: (Double) -> Unit,
    onDone: (WorkoutSet) -> Unit,
) {
    var weight by remember { mutableDoubleStateOf(Units.toDisplay(planned.weightKg, unit)) }
    var reps by remember { mutableIntStateOf(planned.reps) }
    var dur by remember { mutableIntStateOf(planned.durationSec) }
    var dist by remember { mutableDoubleStateOf(Units.distToDisplay(planned.distanceKm, unit)) }
    var setType by remember { mutableStateOf(SetType.NORMAL) }
    var editing by remember { mutableStateOf<String?>(null) }
    val wStep = if (unit == WeightUnit.KG) 2.5 else 5.0

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set $setNumber", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            when (type) {
                TrackType.WEIGHT_REPS -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigStepper("Weight (${unit.label})", fmtNum(weight), { weight = (weight - wStep).coerceAtLeast(0.0) }, { weight += wStep }, { editing = "w" }, Modifier.weight(1f))
                    BigStepper("Reps", "$reps", { reps = (reps - 1).coerceAtLeast(1) }, { reps++ }, { editing = "r" }, Modifier.weight(1f))
                }
                TrackType.TIME -> BigStepper("Time", fmtDuration(dur), { dur = (dur - 5).coerceAtLeast(5) }, { dur += 5 }, { editing = "d" })
                TrackType.DISTANCE_TIME -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigStepper("Dist (${unit.distLabel})", fmtNum(dist), { dist = (dist - 0.5).coerceAtLeast(0.0) }, { dist += 0.5 }, { editing = "k" }, Modifier.weight(1f))
                    BigStepper("Time", fmtDuration(dur), { dur = (dur - 60).coerceAtLeast(0) }, { dur += 60 }, { editing = "d" }, Modifier.weight(1f))
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SetType.entries.forEach { t -> FilterChip(selected = setType == t, onClick = { setType = t }, label = { Text(t.label) }) }
                if (isBarbell) AssistChip(onClick = { onPlates(Units.fromDisplay(weight, unit)) }, label = { Text("Plates") }, leadingIcon = { Icon(Icons.Filled.Calculate, null, Modifier.size(18.dp)) })
            }
            val valid = when (type) { TrackType.WEIGHT_REPS -> reps > 0; TrackType.TIME -> dur > 0; TrackType.DISTANCE_TIME -> dur > 0 || dist > 0 }
            Button(
                enabled = valid,
                onClick = {
                    onDone(
                        WorkoutSet(
                            "", reps = if (type == TrackType.WEIGHT_REPS) reps else 0,
                            weightKg = Units.fromDisplay(weight, unit),
                            durationSec = if (type == TrackType.WEIGHT_REPS) 0 else dur,
                            distanceKm = if (type == TrackType.DISTANCE_TIME) Units.distFromDisplay(dist, unit) else 0.0,
                            type = setType,
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Check, null, Modifier.size(28.dp)); Spacer(Modifier.width(8.dp))
                Text("Done", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
    editing?.let { what ->
        var text by remember(what) {
            mutableStateOf(when (what) { "w" -> fmtNum(weight); "r" -> "$reps"; "k" -> fmtNum(dist); else -> "" })
        }
        var d2 by remember(what) { mutableIntStateOf(dur) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(when (what) { "w" -> "Weight (${unit.label})"; "r" -> "Reps"; "k" -> "Distance (${unit.distLabel})"; else -> "Time" }) },
            text = {
                if (what == "d") DurationField("Time", d2, { d2 = it })
                else OutlinedTextField(
                    text, { v -> text = v.filter { it.isDigit() || ((what == "w" || what == "k") && (it == '.' || it == ',')) }.take(7) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = if (what == "r") androidx.compose.ui.text.input.KeyboardType.Number else androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val v = text.replace(',', '.').toDoubleOrNull()
                    when (what) {
                        "w" -> if (v != null) weight = v
                        "r" -> text.toIntOrNull()?.let { reps = it }
                        "k" -> if (v != null) dist = v
                        "d" -> dur = d2
                    }
                    editing = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GymRestBar(remaining: Int, total: Int, onAdd: () -> Unit, onSub: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REST", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(fmtDuration(remaining), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            LinearProgressIndicator(progress = { if (total > 0) remaining / total.toFloat() else 0f }, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                OutlinedButton(onClick = onSub) { Text("−15s") }
                OutlinedButton(onClick = onAdd) { Text("+15s") }
                Button(onClick = onSkip) { Text("Skip rest") }
            }
        }
    }
}

@Composable
private fun ExerciseStrip(day: WorkoutDay, exMap: Map<String, Exercise>, letters: Map<String, Char>, current: Int, onPick: (Int) -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            day.entries.forEachIndexed { i, e ->
                val done = e.sets.count { it.isWorking }
                val target = e.targetSets
                val complete = (target > 0 && done >= target) || (target == 0 && done > 0)
                val sel = i == current
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when { sel -> MaterialTheme.colorScheme.primary; complete -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f); else -> MaterialTheme.colorScheme.surfaceContainerHighest },
                    modifier = Modifier.clickable { onPick(i) },
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp).widthIn(max = 120.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (e.group.isNotEmpty()) Text("${letters[e.group]} ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                                color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                            if (complete) Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                        }
                        Text(
                            exMap[e.exerciseId]?.name ?: "?", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (target > 0) "$done/$target" else "$done sets", style = MaterialTheme.typography.labelSmall,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
