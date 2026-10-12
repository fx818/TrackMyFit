package com.trackmyfit.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun LogScreen(vm: MainViewModel, onOpenExercise: (String) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val date by vm.selectedDate.collectAsStateWithLifecycle()
    val rest by vm.restRemaining.collectAsStateWithLifecycle()
    val restTotal by vm.restTotal.collectAsStateWithLifecycle()
    val day = data.dayFor(date)
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    val unit = data.settings.unit
    val today = LocalDate.now()

    var showPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCopy by remember { mutableStateOf(false) }
    var showNewLabel by remember { mutableStateOf(false) }
    var setDialog by remember { mutableStateOf<SetDialogState?>(null) }
    var noteDialog by remember { mutableStateOf<ExerciseEntry?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
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
                    if (date != today) {
                        IconButton(onClick = { vm.setDate(today) }) { Icon(Icons.Filled.Today, "Go to today") }
                    }
                    IconButton(onClick = { vm.shiftDate(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add exercise") },
            )
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
                LabelRow(
                    all = data.allLabels(), selected = day.labels,
                    onToggle = { vm.toggleLabel(date, it) }, onNew = { showNewLabel = true },
                )
            }
            if (day.entries.isNotEmpty()) {
                item { DaySummary(day, unit) }
            }
            if (day.entries.isEmpty()) {
                item {
                    val suggestion = remember(data, date, day.labels) { findRepeatCandidate(data, date, day.labels) }
                    EmptyState(
                        icon = Icons.Filled.FitnessCenter,
                        title = if (day.labels.any { it.equals("Rest", true) }) "Rest day" else "No exercises logged",
                        body = "Pick a label above, then add exercises and log your sets, reps and weight.",
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showPicker = true }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise") }
                            if (suggestion != null) {
                                val (sDate, sDay) = suggestion
                                OutlinedButton(onClick = { vm.copyWorkout(sDate, date, withSets = false) }) {
                                    Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Repeat ${sDay.labels.joinToString(" / ").ifEmpty { "workout" }} (${fmtShortDate(sDate)})",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (Stats.workoutDays(data).any { it.date != date.toString() }) {
                                TextButton(onClick = { showCopy = true }) { Icon(Icons.Filled.ContentCopy, null); Spacer(Modifier.width(6.dp)); Text("Copy a previous workout") }
                            }
                        }
                    }
                }
            }
            items(day.entries, key = { it.id }) { entry ->
                val ex = exMap[entry.exerciseId]
                val prev = remember(data, date, entry.exerciseId) { Stats.lastBefore(data, entry.exerciseId, date) }
                val best = remember(data, date, entry.exerciseId) { Stats.bestWeightBefore(data, entry.exerciseId, date) }
                val idx = day.entries.indexOf(entry)
                EntryCard(
                    entry = entry, exercise = ex, prev = prev, bestBefore = best, unit = unit,
                    canMoveUp = idx > 0, canMoveDown = idx < day.entries.lastIndex,
                    onAddSet = {
                        val template = entry.sets.lastOrNull() ?: prev?.entry?.sets?.firstOrNull()
                        setDialog = SetDialogState(entry.id, null, entry.sets.size + 1, ex?.name ?: "Exercise", template?.reps ?: 10, template?.weightKg ?: 0.0)
                    },
                    onEditSet = { i, s -> setDialog = SetDialogState(entry.id, s.id, i + 1, ex?.name ?: "Exercise", s.reps, s.weightKg) },
                    onMove = { up -> vm.moveEntry(date, entry.id, up) },
                    onRemove = { vm.removeEntry(date, entry.id) },
                    onNote = { noteDialog = entry },
                    onHistory = { onOpenExercise(entry.exerciseId) },
                )
            }
            item {
                DayNoteField(day.note) { vm.setDayNote(date, it) }
            }
            if (!day.isEmpty) {
                item {
                    TextButton(onClick = { confirmClear = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Clear this day")
                    }
                }
            }
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
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { vm.setDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }
    if (showCopy) {
        CopyWorkoutDialog(data, exclude = date, unit = unit, onDismiss = { showCopy = false }) { src, withSets ->
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
            onSave = { reps, kg ->
                if (st.setId == null) vm.addSet(date, st.entryId, reps, kg) else vm.updateSet(date, st.entryId, st.setId, reps, kg)
                setDialog = null
            },
            onDelete = if (st.setId != null) ({ vm.deleteSet(date, st.entryId, st.setId); setDialog = null }) else null,
        )
    }
    noteDialog?.let { e ->
        TextInputDialog(title = "Exercise note", label = "Note", initial = e.note, singleLine = false, allowEmpty = true, onDismiss = { noteDialog = null }) {
            vm.setEntryNote(date, e.id, it.trim()); noteDialog = null
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear this day?") },
            text = { Text("All labels, exercises, sets and notes for ${fmtLongDate(date)} will be deleted.") },
            confirmButton = { TextButton(onClick = { vm.clearDay(date); confirmClear = false }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

/** Most recent earlier workout sharing a label with today (or the most recent workout if no labels set). */
private fun findRepeatCandidate(data: AppData, date: LocalDate, labels: List<String>): Pair<LocalDate, WorkoutDay>? {
    if (labels.isEmpty()) return null
    return Stats.workoutDays(data)
        .filter { it.date != date.toString() && LocalDate.parse(it.date) < date.plusDays(1) }
        .firstOrNull { d -> d.labels.any { l -> labels.any { it.equals(l, true) } } }
        ?.let { LocalDate.parse(it.date) to it }
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
            SummaryItem(day.entries.sumOf { e -> e.sets.sumOf { it.reps } }.toString(), "Reps")
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
    unit: WeightUnit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onAddSet: () -> Unit,
    onEditSet: (Int, WorkoutSet) -> Unit,
    onMove: (Boolean) -> Unit,
    onRemove: () -> Unit,
    onNote: () -> Unit,
    onHistory: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(onClick = onHistory).padding(vertical = 4.dp)) {
                    Text(exercise?.name ?: "Deleted exercise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(exercise?.muscleGroup, exercise?.equipment?.ifBlank { null }).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("History & progress") }, leadingIcon = { Icon(Icons.Filled.Insights, null) }, onClick = { menu = false; onHistory() })
                        DropdownMenuItem(text = { Text(if (entry.note.isBlank()) "Add note" else "Edit note") }, leadingIcon = { Icon(Icons.Filled.EditNote, null) }, onClick = { menu = false; onNote() })
                        if (canMoveUp) DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) }, onClick = { menu = false; onMove(true) })
                        if (canMoveDown) DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) }, onClick = { menu = false; onMove(false) })
                        DropdownMenuItem(text = { Text("Remove") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = {
                            menu = false; if (entry.sets.isEmpty()) onRemove() else confirmRemove = true
                        })
                    }
                }
            }
            if (prev != null) {
                Text(
                    "Last (${fmtShortDate(prev.date)}): " + prev.entry.sets.joinToString(", ") { "${fmtWeightBare(it.weightKg, unit)}×${it.reps}" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = 12.dp, top = 2.dp),
                )
            }
            if (entry.sets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(end = 12.dp)) {
                    Text("SET", Modifier.width(44.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("WEIGHT", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("REPS", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                entry.sets.forEachIndexed { i, s ->
                    val isPr = bestBefore != null && s.weightKg > bestBefore
                    Row(
                        Modifier.fillMaxWidth().clickable { onEditSet(i, s) }.padding(vertical = 6.dp).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.width(44.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(26.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        Text(fmtWeight(s.weightKg, unit), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text("${s.reps} reps", style = MaterialTheme.typography.bodyLarge)
                            if (isPr) {
                                Spacer(Modifier.width(8.dp))
                                Pill("PR", color = MaterialTheme.colorScheme.primary, textColor = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, end = 12.dp))
            }
            TextButton(onClick = onAddSet) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Add set") }
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
            LinearProgressIndicator(
                progress = { if (total > 0) remaining / total.toFloat() else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Rest  %d:%02d".format(remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f),
                )
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
    val exerciseName: String,
    val reps: Int,
    val weightKg: Double,
)

@Composable
fun SetDialog(state: SetDialogState, unit: WeightUnit, onDismiss: () -> Unit, onSave: (Int, Double) -> Unit, onDelete: (() -> Unit)?) {
    var weight by remember { mutableStateOf(fmtNum(Units.toDisplay(state.weightKg, unit))) }
    var reps by remember { mutableStateOf(state.reps.toString()) }
    val w = weight.replace(',', '.').toDoubleOrNull()
    val r = reps.toIntOrNull()
    val valid = w != null && w >= 0 && r != null && r > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (state.setId == null) "Add set ${state.setNumber}" else "Edit set ${state.setNumber}")
                Text(state.exerciseName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Weight (${unit.label})", weight, { weight = it }, step = if (unit == WeightUnit.KG) 2.5 else 5.0, decimal = true)
                StepperField("Reps", reps, { reps = it }, step = 1.0, decimal = false)
                Text("Use 0 weight for bodyweight exercises.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSave(r!!, Units.fromDisplay(w!!, unit)) }) { Text("Save") }
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
private fun CopyWorkoutDialog(data: AppData, exclude: LocalDate, unit: WeightUnit, onDismiss: () -> Unit, onCopy: (LocalDate, Boolean) -> Unit) {
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
                    Text("Also copy sets / weights")
                }
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(days, key = { it.date }) { d ->
                        val ld = LocalDate.parse(d.date)
                        ListItem(
                            headlineContent = { Text(d.labels.joinToString(" / ").ifEmpty { "Workout" }) },
                            supportingContent = {
                                Text(
                                    fmtLongDate(ld) + "\n" + d.entries.mapNotNull { exMap[it.exerciseId]?.name }.joinToString(", "),
                                    maxLines = 3, overflow = TextOverflow.Ellipsis,
                                )
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
