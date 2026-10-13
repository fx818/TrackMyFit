package com.trackmyfit.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun LogScreen(vm: MainViewModel, onOpenExercise: (String) -> Unit, onOpenRoutines: () -> Unit, onGymMode: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val date by vm.selectedDate.collectAsStateWithLifecycle()
    val rest by vm.restRemaining.collectAsStateWithLifecycle()
    val restTotal by vm.restTotal.collectAsStateWithLifecycle()
    val day = data.dayFor(date)
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    val unit = data.settings.unit
    val today = LocalDate.now()
    val letters = remember(day.entries) { groupLetters(day.entries) }

    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCopy by remember { mutableStateOf(false) }
    var showNewLabel by remember { mutableStateOf(false) }
    var setDialog by remember { mutableStateOf<SetDialogState?>(null) }
    var noteDialog by remember { mutableStateOf<ExerciseEntry?>(null) }
    var targetDialog by remember { mutableStateOf<ExerciseEntry?>(null) }
    var howTo by remember { mutableStateOf<Exercise?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var saveRoutine by remember { mutableStateOf(false) }
    var plateFor by remember { mutableStateOf<Double?>(null) }
    var durationDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackMsg) { snackMsg?.let { snackbar.showSnackbar(it); snackMsg = null } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = { vm.shiftDate(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day") }
                },
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showDatePicker = true }.padding(horizontal = 12.dp),
                    ) {
                        Text(relativeDay(date, today), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(fmtLongDate(date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (date != today) IconButton(onClick = { vm.setDate(today) }) { Icon(Icons.Filled.Today, "Go to today") }
                    IconButton(onClick = { vm.shiftDate(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day") }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(menu, { menu = false }) {
                            DropdownMenuItem(text = { Text("Routines") }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, null) }, onClick = { menu = false; onOpenRoutines() })
                            if (day.entries.isNotEmpty()) DropdownMenuItem(text = { Text("Save as routine") }, leadingIcon = { Icon(Icons.Filled.BookmarkAdd, null) }, onClick = { menu = false; saveRoutine = true })
                            DropdownMenuItem(text = { Text("Copy a previous workout") }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }, onClick = { menu = false; showCopy = true })
                            DropdownMenuItem(text = { Text("Plate calculator") }, leadingIcon = { Icon(Icons.Filled.Calculate, null) }, onClick = { menu = false; plateFor = 0.0 })
                            DropdownMenuItem(text = { Text("Set duration manually") }, leadingIcon = { Icon(Icons.Filled.Timer, null) }, onClick = { menu = false; durationDialog = true })
                            if (!day.isEmpty) DropdownMenuItem(text = { Text("Clear this day") }, leadingIcon = { Icon(Icons.Filled.DeleteOutline, null) }, onClick = { menu = false; confirmClear = true })
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (day.entries.isNotEmpty()) {
                    SmallFloatingActionButton(onClick = { showPicker = true }) { Icon(Icons.Filled.Add, "Add exercise") }
                    ExtendedFloatingActionButton(
                        onClick = onGymMode,
                        icon = { Icon(Icons.Filled.PlayArrow, null) },
                        text = { Text("Gym mode") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ExtendedFloatingActionButton(onClick = { showPicker = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add exercise") })
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(rest != null) {
                RestTimerBar(rest ?: 0, restTotal, onAdd = { vm.addRest(15) }, onSub = { vm.addRest(-15) }, onSkip = { vm.stopRest() })
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                LabelRow(all = data.allLabels(), selected = day.labels, onToggle = { vm.toggleLabel(date, it) }, onNew = { showNewLabel = true })
            }
            item {
                WorkoutTimerBar(
                    day = day, isToday = date == today,
                    onStart = { vm.startWorkout(date) }, onFinish = { vm.finishWorkout(date) },
                    onResume = { vm.resumeWorkout(date) }, onEdit = { durationDialog = true },
                )
            }
            if (day.entries.isNotEmpty()) item { DaySummary(day, unit) }
            if (day.entries.isEmpty()) {
                item {
                    val suggestion = remember(data, date, day.labels) { findRepeatCandidate(data, date, day.labels) }
                    EmptyState(
                        icon = Icons.Filled.FitnessCenter,
                        title = if (day.labels.any { it.equals("Rest", true) }) "Rest day" else "No exercises logged",
                        body = "Pick a label above, start one of your routines, or add exercises and log sets, reps and weight.",
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showPicker = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise") }
                            if (suggestion != null) {
                                val (sDate, sDay) = suggestion
                                OutlinedButton(onClick = { vm.copyWorkout(sDate, date, withSets = false) }) {
                                    Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(6.dp))
                                    Text("Repeat ${sDay.labels.joinToString(" / ").ifEmpty { "workout" }} (${fmtShortDate(sDate)})", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (Stats.workoutDays(data).any { it.date != date.toString() }) {
                                TextButton(onClick = { showCopy = true }) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy a previous workout") }
                            }
                        }
                    }
                }
                item { RoutineQuickStart(data, onStart = { vm.startRoutine(it, date); snackMsg = "Started ${it.name}" }, onManage = onOpenRoutines) }
            }
            items(day.entries, key = { it.id }) { entry ->
                val ex = exMap[entry.exerciseId]
                val type = ex?.trackType ?: TrackType.WEIGHT_REPS
                val prev = remember(data, date, entry.exerciseId) { Stats.lastBefore(data, entry.exerciseId, date) }
                val best = remember(data, date, entry.exerciseId) { Stats.bestWeightBefore(data, entry.exerciseId, date) }
                val sugg = remember(data, date, entry) { Stats.suggest(data, ex, entry, date, unit) }
                val idx = day.entries.indexOf(entry)
                val groupPos = if (entry.group.isEmpty()) null else {
                    val members = day.entries.filter { it.group == entry.group }
                    Triple(letters[entry.group] ?: 'A', members.indexOfFirst { it.id == entry.id }, members.size)
                }
                EntryCard(
                    entry = entry, exercise = ex, prev = prev, bestBefore = best, suggestion = sugg, unit = unit,
                    canMoveUp = idx > 0, canMoveDown = idx < day.entries.lastIndex,
                    groupPos = groupPos,
                    onLinkNext = { vm.linkWithNext(date, entry.id) },
                    onUnlink = { vm.unlinkEntry(date, entry.id) },
                    onAddSet = {
                        val last = entry.sets.lastOrNull()
                        val template: WorkoutSet = when {
                            last != null -> last.copy(type = if (last.type == SetType.WARMUP) SetType.NORMAL else last.type)
                            sugg != null -> WorkoutSet("", reps = sugg.reps, weightKg = sugg.weightKg)
                            entry.targetReps > 0 || entry.targetWeightKg > 0 || entry.targetDurationSec > 0 ->
                                WorkoutSet("", reps = entry.targetReps, weightKg = entry.targetWeightKg, durationSec = entry.targetDurationSec)
                            prev != null -> (prev.entry.sets.firstOrNull { it.isWorking } ?: prev.entry.sets.first()).copy(type = SetType.NORMAL)
                            else -> WorkoutSet("", reps = if (type == TrackType.WEIGHT_REPS) 10 else 0)
                        }
                        val remaining = (entry.targetSets - entry.sets.count { it.isWorking }).coerceAtLeast(0)
                        val count = when {
                            entry.sets.isNotEmpty() -> 1
                            entry.targetSets > 0 -> entry.targetSets
                            sugg != null -> sugg.sets
                            else -> 1
                        }.let { if (entry.sets.isNotEmpty() && remaining > 1) 1 else it }
                        setDialog = SetDialogState(entry.id, null, entry.sets.size + 1, ex, template.copy(id = "", rpe = 0.0), count)
                    },
                    onEditSet = { i, s -> setDialog = SetDialogState(entry.id, s.id, i + 1, ex, s, 1) },
                    onMove = { up -> vm.moveEntry(date, entry.id, up) },
                    onRemove = { vm.removeEntry(date, entry.id) },
                    onNote = { noteDialog = entry },
                    onTarget = { targetDialog = entry },
                    onHistory = { onOpenExercise(entry.exerciseId) },
                    onHowTo = { ex?.let { howTo = it } },
                    onPlates = { w -> plateFor = w },
                )
            }
            item { DayNoteField(day.note) { vm.setDayNote(date, it) } }
        }
    }

    if (showPicker) {
        ExercisePickerDialog(
            vm = vm, data = data, dayLabels = day.labels, alreadyAdded = day.entries.map { it.exerciseId }.toSet(),
            onDismiss = { showPicker = false },
            onConfirm = { ids -> vm.addExercises(date, ids); showPicker = false },
        )
    }
    if (showDatePicker) {
        DatePick(date, onDismiss = { showDatePicker = false }) { vm.setDate(it); showDatePicker = false }
    }
    if (showCopy) {
        CopyWorkoutDialog(data, exclude = date, onDismiss = { showCopy = false }) { src, withSets ->
            vm.copyWorkout(src, date, withSets); showCopy = false
        }
    }
    if (showNewLabel) {
        TextInputDialog(title = "New label", label = "Label (e.g. Chest & Triceps)", onDismiss = { showNewLabel = false }) {
            vm.addCustomLabel(it); vm.toggleLabel(date, it.trim()); showNewLabel = false
        }
    }
    setDialog?.let { st ->
        SetDialog(
            state = st, unit = unit,
            onDismiss = { setDialog = null },
            onSave = { set, count ->
                if (st.setId == null) vm.addSets(date, st.entryId, set, count, st.exercise?.name)
                else vm.updateSet(date, st.entryId, set.copy(id = st.setId))
                setDialog = null
            },
            onDelete = if (st.setId != null) ({ vm.deleteSet(date, st.entryId, st.setId); setDialog = null }) else null,
            onPlates = { w -> plateFor = w },
        )
    }
    noteDialog?.let { e ->
        TextInputDialog(title = "Exercise note", label = "Note", initial = e.note, singleLine = false, allowEmpty = true, onDismiss = { noteDialog = null }) {
            vm.setEntryNote(date, e.id, it.trim()); noteDialog = null
        }
    }
    targetDialog?.let { e ->
        TargetDialog(e, exMap[e.exerciseId], unit, onDismiss = { targetDialog = null }) { s, r, w, d ->
            vm.setEntryTarget(date, e.id, s, r, w, d); targetDialog = null
        }
    }
    howTo?.let { HowToSheet(it) { howTo = null } }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear this day?") },
            text = { Text("All labels, exercises, sets and notes for ${fmtLongDate(date)} will be deleted.") },
            confirmButton = { TextButton(onClick = { vm.clearDay(date); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
    if (saveRoutine) {
        TextInputDialog(
            title = "Save as routine", label = "Routine name",
            initial = day.labels.joinToString(" & ").ifEmpty { "My workout" },
            onDismiss = { saveRoutine = false },
        ) { name -> vm.routineFromDay(date, name); saveRoutine = false; snackMsg = "Routine \"${name.trim()}\" saved" }
    }
    plateFor?.let { w ->
        PlateCalculatorDialog(unit, w, data.settings.barWeightKg, onBarChange = { b -> vm.updateSettings { it.copy(barWeightKg = b) } }) { plateFor = null }
    }
    if (durationDialog) {
        var mins by remember { mutableStateOf(day.durationMinutes?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { durationDialog = false },
            title = { Text("Workout duration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(mins, { mins = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Text("Use this if you forgot to start the timer.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { vm.setDuration(date, mins.toIntOrNull() ?: 0); durationDialog = false }) { Text("Save") } },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.resetDuration(date); durationDialog = false }) { Text("Reset") }
                    TextButton(onClick = { durationDialog = false }) { Text("Cancel") }
                }
            },
        )
    }
}

@Composable
fun DatePick(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } ?: onDismiss() }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) { DatePicker(state) }
}

private fun findRepeatCandidate(data: AppData, date: LocalDate, labels: List<String>): Pair<LocalDate, WorkoutDay>? {
    if (labels.isEmpty()) return null
    return Stats.workoutDays(data)
        .filter { it.date != date.toString() && LocalDate.parse(it.date) < date.plusDays(1) }
        .firstOrNull { d -> d.labels.any { l -> labels.any { it.equals(l, true) } } }
        ?.let { LocalDate.parse(it.date) to it }
}

@Composable
private fun RoutineQuickStart(data: AppData, onStart: (Routine) -> Unit, onManage: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ListAlt, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Start a routine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = onManage) { Text(if (data.routines.isEmpty()) "Create" else "Manage") }
            }
            if (data.routines.isEmpty()) {
                Text("Save your usual workouts (e.g. \"Chest Day A\") with target sets × reps and start them in one tap.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.routines.forEach { r ->
                        AssistChip(onClick = { onStart(r) }, label = { Text("${r.name} · ${r.items.size}") }, leadingIcon = { Icon(Icons.Filled.PlayArrow, null, Modifier.size(18.dp)) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutTimerBar(day: WorkoutDay, isToday: Boolean, onStart: () -> Unit, onFinish: () -> Unit, onResume: () -> Unit, onEdit: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (day.isTimerRunning) {
        LaunchedEffect(day.startedAt) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
    }
    val shape = RoundedCornerShape(14.dp)
    when {
        day.isTimerRunning -> Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = shape) {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Workout in progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(fmtDuration(((now - day.startedAt) / 1000).toInt()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Button(onClick = onFinish) { Icon(Icons.Filled.Flag, null); Spacer(Modifier.width(4.dp)); Text("Finish") }
            }
        }
        day.durationMinutes != null -> Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = shape, modifier = Modifier.clickable(onClick = onEdit)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(8.dp))
                Text("Duration ${fmtMinutes(day.durationMinutes!!)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (isToday && day.startedAt > 0 && day.durationMin == 0) TextButton(onClick = onResume) { Text("Resume") }
                Icon(Icons.Filled.Edit, "Edit", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        isToday -> OutlinedButton(onClick = onStart, modifier = Modifier.fillMaxWidth(), shape = shape) {
            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start workout timer")
        }
        else -> {}
    }
}

@Composable
private fun LabelRow(all: List<String>, selected: List<String>, onToggle: (String) -> Unit, onNew: () -> Unit) {
    val ordered = selected + all.filter { a -> selected.none { it.equals(a, true) } }
    Column {
        Text("Today's focus", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ordered.forEach { l ->
                val isSel = selected.any { it.equals(l, true) }
                FilterChip(
                    selected = isSel, onClick = { onToggle(l) }, label = { Text(l) },
                    leadingIcon = if (isSel) ({ Icon(Icons.Filled.Check, null, Modifier.size(18.dp)) }) else null,
                )
            }
            AssistChip(onClick = onNew, label = { Text("New label") }, leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) })
        }
    }
}

@Composable
private fun DaySummary(day: WorkoutDay, unit: WeightUnit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryItem(day.entries.size.toString(), "Exercises")
            SummaryItem(Stats.setCount(day).toString(), "Sets")
            SummaryItem(Stats.repCount(day).toString(), "Reps")
            SummaryItem(fmtVolume(Stats.volumeKg(day), unit), "Volume")
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f))
    }
}

@Composable
private fun EntryCard(
    entry: ExerciseEntry,
    exercise: Exercise?,
    prev: Stats.HistoryPoint?,
    bestBefore: Double?,
    suggestion: Stats.Suggestion?,
    unit: WeightUnit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onAddSet: () -> Unit,
    onEditSet: (Int, WorkoutSet) -> Unit,
    onMove: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onNote: () -> Unit,
    onTarget: () -> Unit,
    onHistory: () -> Unit,
    onHowTo: () -> Unit,
    onPlates: (Double) -> Unit,
    groupPos: Triple<Char, Int, Int>? = null,
    onLinkNext: () -> Unit = {},
    onUnlink: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val type = exercise?.trackType ?: TrackType.WEIGHT_REPS
    val demo = remember(entry.exerciseId) { hasDemo(ctx, entry.exerciseId) }
    var menu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val tertiary = MaterialTheme.colorScheme.tertiary
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
    if (groupPos != null) {
        // Superset bracket: drawn (not measured) so it can extend into the 12dp gap to the next card.
        val last = groupPos.second == groupPos.third - 1
        val first = groupPos.second == 0
        Box(Modifier.width(22.dp).fillMaxHeight().drawBehind {
            val w = 4.dp.toPx()
            val x = (size.width - w) / 2f
            val extra = if (last) 0f else 12.dp.toPx()
            drawRoundRect(
                tertiary, topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height + extra),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(if (first || last) w / 2 else 0f),
            )
        })
        Spacer(Modifier.width(4.dp))
    }
    ElevatedCard(Modifier.weight(1f)) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            if (groupPos != null) {
                Text(
                    (if (groupPos.third >= 3) "CIRCUIT " else "SUPERSET ") + "${groupPos.first}${groupPos.second + 1}",
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tertiary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (demo) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onHowTo)) {
                        ExerciseDemo(entry.exerciseId, Modifier.fillMaxSize(), maxWidth = 160, showControls = false)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Column(Modifier.weight(1f).clickable(onClick = onHistory).padding(vertical = 4.dp)) {
                    Text(exercise?.name ?: "Deleted exercise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(exercise?.muscleGroup, exercise?.equipment?.ifBlank { null }).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (demo) {
                    IconButton(onClick = onHowTo) { Icon(Icons.Filled.OndemandVideo, "How to", tint = MaterialTheme.colorScheme.primary) }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (demo) DropdownMenuItem(text = { Text("How to (demo)") }, leadingIcon = { Icon(Icons.Filled.OndemandVideo, null) }, onClick = { menu = false; onHowTo() })
                        DropdownMenuItem(text = { Text("History & progress") }, leadingIcon = { Icon(Icons.Filled.Insights, null) }, onClick = { menu = false; onHistory() })
                        DropdownMenuItem(text = { Text("Set target") }, leadingIcon = { Icon(Icons.Filled.TrackChanges, null) }, onClick = { menu = false; onTarget() })
                        DropdownMenuItem(text = { Text(if (entry.note.isBlank()) "Add note" else "Edit note") }, leadingIcon = { Icon(Icons.Filled.EditNote, null) }, onClick = { menu = false; onNote() })
                        if (exercise?.equipment == "Barbell" || exercise?.equipment == "Smith Machine") {
                            DropdownMenuItem(text = { Text("Plate calculator") }, leadingIcon = { Icon(Icons.Filled.Calculate, null) }, onClick = {
                                menu = false; onPlates(entry.sets.lastOrNull()?.weightKg ?: suggestion?.weightKg ?: entry.targetWeightKg)
                            })
                        }
                        if (canMoveDown) DropdownMenuItem(
                            text = { Text(if (groupPos != null) "Add next exercise to superset" else "Superset with next") },
                            leadingIcon = { Icon(Icons.Filled.Link, null) }, onClick = { menu = false; onLinkNext() },
                        )
                        if (groupPos != null) DropdownMenuItem(text = { Text("Remove from superset") }, leadingIcon = { Icon(Icons.Filled.LinkOff, null) }, onClick = { menu = false; onUnlink() })
                        if (canMoveUp) DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) }, onClick = { menu = false; onMove(true) })
                        if (canMoveDown) DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) }, onClick = { menu = false; onMove(false) })
                        DropdownMenuItem(text = { Text("Remove") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = {
                            menu = false; if (entry.sets.isEmpty()) onRemove() else confirmRemove = true
                        })
                    }
                }
            }
            // Target from routine / copy
            if (entry.targetSets > 0) {
                val done = entry.sets.count { it.isWorking }
                val what = when (type) {
                    TrackType.WEIGHT_REPS -> "${entry.targetSets} × ${entry.targetReps}" + if (entry.targetWeightKg > 0) " @ ${fmtWeight(entry.targetWeightKg, unit)}" else ""
                    else -> "${entry.targetSets} × " + fmtDuration(entry.targetDurationSec)
                }
                Row(Modifier.padding(top = 4.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrackChanges, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(4.dp))
                    Text("Target $what", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text("$done/${entry.targetSets} sets", style = MaterialTheme.typography.labelMedium,
                        color = if (done >= entry.targetSets) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (prev != null) {
                Text(
                    "Last (${fmtShortDate(prev.date)}): " + prev.entry.sets.filter { it.isWorking }.joinToString(", ") { fmtSetShort(it, type, unit) },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 12.dp, top = 2.dp),
                )
            }
            if (suggestion != null && entry.sets.none { it.isWorking }) {
                SuggestionLine(suggestion, unit)
            }
            if (entry.sets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                var working = 0
                entry.sets.forEachIndexed { i, s ->
                    val label = if (s.isWorking) "${++working}" else "W"
                    val isPr = type == TrackType.WEIGHT_REPS && s.isWorking && bestBefore != null && s.weightKg > bestBefore
                    Row(
                        Modifier.fillMaxWidth().clickable { onEditSet(i, s) }.padding(vertical = 6.dp).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (s.isWorking) MaterialTheme.colorScheme.primaryContainer else Color(0xFF4FA3E0).copy(alpha = 0.25f),
                            modifier = Modifier.size(26.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(fmtSet(s, type, unit), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (s.type != SetType.NORMAL && s.type != SetType.WARMUP) { SetTypeBadge(s.type); Spacer(Modifier.width(6.dp)) }
                        if (s.rpe > 0) { Text("RPE ${fmtNum(s.rpe)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(6.dp)) }
                        if (isPr) Pill("PR", color = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, end = 12.dp))
            }
            TextButton(onClick = onAddSet) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text(if (entry.sets.isEmpty()) "Log sets" else "Add set") }
        }
    }
    }
    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove ${exercise?.name ?: "exercise"}?") },
            text = { Text("Its ${entry.sets.size} logged set(s) for this day will be deleted.") },
            confirmButton = { TextButton(onClick = { confirmRemove = false; onRemove() }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SuggestionLine(s: Stats.Suggestion, unit: WeightUnit) {
    val text = when (s.kind) {
        Stats.SuggestKind.INCREASE_WEIGHT -> "Try ${fmtWeight(s.weightKg, unit)} × ${s.reps} today — you hit all your reps last time."
        Stats.SuggestKind.MATCH_REPS -> "Stay at ${fmtWeight(s.weightKg, unit)} and aim for ${s.reps} reps on every set."
        Stats.SuggestKind.ADD_REPS -> "Try ${s.reps} reps per set — one more than last time."
    }
    Surface(color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 6.dp, end = 12.dp)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TipsAndUpdates, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DayNoteField(note: String, onChange: (String) -> Unit) {
    var text by remember(note) { mutableStateOf(note) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onChange(it) },
        label = { Text("Workout notes") },
        placeholder = { Text("How did it feel? Energy, sleep, pain…") },
        leadingIcon = { Icon(Icons.Filled.EditNote, null) },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
    )
}

@Composable
private fun RestTimerBar(remaining: Int, total: Int, onAdd: () -> Unit, onSub: () -> Unit, onSkip: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, tonalElevation = 3.dp) {
        Column {
            LinearProgressIndicator(progress = { if (total > 0) remaining / total.toFloat() else 0f }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text("Rest  " + fmtDuration(remaining), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                TextButton(onClick = onSub) { Text("-15s") }
                TextButton(onClick = onAdd) { Text("+15s") }
                TextButton(onClick = onSkip) { Text("Skip") }
            }
        }
    }
}

data class SetDialogState(
    val entryId: String,
    val setId: String?,
    val setNumber: Int,
    val exercise: Exercise?,
    val initial: WorkoutSet,
    val count: Int,
)

@Composable
fun SetDialog(
    state: SetDialogState,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onSave: (WorkoutSet, Int) -> Unit,
    onDelete: (() -> Unit)?,
    onPlates: (Double) -> Unit,
) {
    val type = state.exercise?.trackType ?: TrackType.WEIGHT_REPS
    val init = state.initial
    var weight by remember { mutableStateOf(if (init.weightKg > 0 || type == TrackType.WEIGHT_REPS) fmtNum(Units.toDisplay(init.weightKg, unit)) else "") }
    var reps by remember { mutableStateOf(if (init.reps > 0) init.reps.toString() else "") }
    var duration by remember { mutableIntStateOf(init.durationSec) }
    var distance by remember { mutableStateOf(if (init.distanceKm > 0) fmtNum(Units.distToDisplay(init.distanceKm, unit)) else "") }
    var count by remember { mutableStateOf(state.count.toString()) }
    var setType by remember { mutableStateOf(init.type) }
    var rpe by remember { mutableDoubleStateOf(init.rpe) }
    val adding = state.setId == null

    val w = weight.replace(',', '.').toDoubleOrNull() ?: if (weight.isBlank()) 0.0 else null
    val r = reps.toIntOrNull() ?: 0
    val dist = distance.replace(',', '.').toDoubleOrNull() ?: if (distance.isBlank()) 0.0 else null
    val n = count.toIntOrNull() ?: 0
    val valid = w != null && w >= 0 && dist != null && (!adding || n in 1..20) && when (type) {
        TrackType.WEIGHT_REPS -> r > 0
        TrackType.TIME -> duration > 0
        TrackType.DISTANCE_TIME -> duration > 0 || (dist > 0)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (adding) "Log sets" else "Edit set ${state.setNumber}")
                Text(state.exercise?.name ?: "Exercise", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (adding) StepperField("Sets", count, { count = it }, step = 1.0, decimal = false)
                when (type) {
                    TrackType.WEIGHT_REPS -> {
                        StepperField("Weight (${unit.label})", weight, { weight = it }, step = if (unit == WeightUnit.KG) 2.5 else 5.0, decimal = true)
                        StepperField("Reps", reps, { reps = it }, step = 1.0, decimal = false)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("0 weight = bodyweight", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            if (state.exercise?.equipment == "Barbell" || state.exercise?.equipment == "Smith Machine") {
                                TextButton(onClick = { onPlates(Units.fromDisplay(w ?: 0.0, unit)) }) { Icon(Icons.Filled.Calculate, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Plates") }
                            }
                        }
                    }
                    TrackType.TIME -> {
                        DurationField("Time", duration, { duration = it })
                        StepperField("Added weight (${unit.label}, optional)", weight, { weight = it }, step = if (unit == WeightUnit.KG) 2.5 else 5.0, decimal = true)
                    }
                    TrackType.DISTANCE_TIME -> {
                        StepperField("Distance (${unit.distLabel})", distance, { distance = it }, step = 0.5, decimal = true)
                        DurationField("Time", duration, { duration = it })
                        fmtPace(Units.distFromDisplay(dist ?: 0.0, unit), duration, unit)?.let {
                            Text("Pace $it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text("Set type", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetType.entries.forEach { t -> FilterChip(selected = setType == t, onClick = { setType = t }, label = { Text(t.label) }) }
                }
                if (setType == SetType.WARMUP) Text("Warm-up sets don't count towards volume, sets or PRs.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (type == TrackType.WEIGHT_REPS) {
                    Text("Effort (RPE, optional)" + if (rpe > 0) ": ${fmtNum(rpe)}" else "", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(6.0, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0).forEach { v ->
                            FilterChip(selected = rpe == v, onClick = { rpe = if (rpe == v) 0.0 else v }, label = { Text(fmtNum(v)) })
                        }
                    }
                    Text("RPE 10 = nothing left, 8 = ~2 reps left in the tank.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                val set = WorkoutSet(
                    id = "",
                    reps = if (type == TrackType.WEIGHT_REPS) r else 0,
                    weightKg = Units.fromDisplay(w ?: 0.0, unit),
                    durationSec = if (type == TrackType.WEIGHT_REPS) 0 else duration,
                    distanceKm = if (type == TrackType.DISTANCE_TIME) Units.distFromDisplay(dist ?: 0.0, unit) else 0.0,
                    type = setType,
                    rpe = rpe,
                )
                onSave(set, n.coerceAtLeast(1))
            }) { Text(if (adding) (if (n > 1) "Add $n sets" else "Add set") else "Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun TargetDialog(entry: ExerciseEntry, ex: Exercise?, unit: WeightUnit, onDismiss: () -> Unit, onSave: (Int, Int, Double, Int) -> Unit) {
    val type = ex?.trackType ?: TrackType.WEIGHT_REPS
    var sets by remember { mutableStateOf(if (entry.targetSets > 0) entry.targetSets.toString() else "3") }
    var reps by remember { mutableStateOf(if (entry.targetReps > 0) entry.targetReps.toString() else "10") }
    var weight by remember { mutableStateOf(if (entry.targetWeightKg > 0) fmtNum(Units.toDisplay(entry.targetWeightKg, unit)) else "") }
    var dur by remember { mutableIntStateOf(entry.targetDurationSec) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Target for ${ex?.name ?: "exercise"}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Sets", sets, { sets = it }, 1.0, false)
                if (type == TrackType.WEIGHT_REPS) {
                    StepperField("Reps", reps, { reps = it }, 1.0, false)
                    StepperField("Weight (${unit.label})", weight, { weight = it }, if (unit == WeightUnit.KG) 2.5 else 5.0, true)
                } else DurationField("Time per set", dur, { dur = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(sets.toIntOrNull() ?: 0, reps.toIntOrNull() ?: 0, Units.fromDisplay(weight.replace(',', '.').toDoubleOrNull() ?: 0.0, unit), dur)
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(0, 0, 0.0, 0) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    singleLine: Boolean = true,
    allowEmpty: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(enabled = allowEmpty || text.isNotBlank(), onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CopyWorkoutDialog(data: AppData, exclude: LocalDate, onDismiss: () -> Unit, onCopy: (LocalDate, Boolean) -> Unit) {
    val days = remember(data) { Stats.workoutDays(data).filter { it.date != exclude.toString() }.take(30) }
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    var withSets by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copy a workout") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { withSets = !withSets }) {
                    Checkbox(checked = withSets, onCheckedChange = { withSets = it })
                    Text("Also copy the logged sets")
                }
                if (days.isEmpty()) Text("No previous workouts yet.")
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(days, key = { it.date }) { d ->
                        val ld = LocalDate.parse(d.date)
                        ListItem(
                            headlineContent = { Text(d.labels.joinToString(" / ").ifEmpty { "Workout" }) },
                            supportingContent = {
                                Text(fmtLongDate(ld) + "\n" + d.entries.mapNotNull { exMap[it.exerciseId]?.name }.joinToString(", "), maxLines = 3, overflow = TextOverflow.Ellipsis)
                            },
                            modifier = Modifier.clickable { onCopy(ld, withSets) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
