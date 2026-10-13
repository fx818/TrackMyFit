package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.LocalDate

@Composable
fun RoutinesScreen(vm: MainViewModel, onBack: () -> Unit, onStarted: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    val unit = data.settings.unit
    var editing by remember { mutableStateOf<Routine?>(null) }
    var confirmDelete by remember { mutableStateOf<Routine?>(null) }
    var startFor by remember { mutableStateOf<Routine?>(null) }
    var programInfo by remember { mutableStateOf<Programs.Program?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var snackMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(snackMsg) { snackMsg?.let { snackbar.showSnackbar(it); snackMsg = null } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Routines & programs") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editing = Routine(newId(), "") }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New routine") })
        },
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("Ready-made programs") }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Programs.all.forEach { pr ->
                        val added = pr.routines.all { r -> data.routines.any { it.id == r.id } }
                        ElevatedCard(onClick = { programInfo = pr }, modifier = Modifier.width(220.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(pr.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${pr.level} · ${pr.daysPerWeek}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp))
                                Text(pr.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(6.dp))
                                if (added) Text("✓ Added", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                                else Text("${pr.routines.size} routines", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            item { SectionTitle("My routines") }
            if (data.routines.isEmpty()) {
                item {
                    EmptyState(Icons.AutoMirrored.Filled.ListAlt, "No routines yet",
                        "Add a ready-made program above, create one here, or log a workout and use ⋮ → \"Save as routine\" on the Log screen.")
                }
            }
            items(data.routines, key = { it.id }) { r ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (r.labels.isNotEmpty()) Text(r.labels.joinToString(" · "), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                            IconButton(onClick = { editing = r }) { Icon(Icons.Filled.Edit, "Edit") }
                            IconButton(onClick = { confirmDelete = r }) { Icon(Icons.Filled.DeleteOutline, "Delete") }
                        }
                        Spacer(Modifier.height(4.dp))
                        val rLetters = groupLettersOf(r.items.map { it.group })
                        r.items.forEach { it2 ->
                            val ex = exMap[it2.exerciseId]
                            Row {
                                if (it2.group.isNotEmpty()) Text("${rLetters[it2.group]} ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                Text(ex?.name ?: "Deleted", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(describeItem(it2, ex, unit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = { startFor = r }, modifier = Modifier.fillMaxWidth(), enabled = r.items.isNotEmpty()) {
                            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start")
                        }
                    }
                }
            }
        }
    }
    programInfo?.let { pr ->
        AlertDialog(
            onDismissRequest = { programInfo = null },
            title = { Text(pr.name) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${pr.level} · ${pr.daysPerWeek}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(pr.description)
                    Text("Schedule: ${pr.schedule}", style = MaterialTheme.typography.bodySmall)
                    pr.routines.forEach { r ->
                        HorizontalDivider()
                        Text(r.name, fontWeight = FontWeight.Bold)
                        val rl = groupLettersOf(r.items.map { it.group })
                        r.items.forEach { it2 ->
                            val ex = exMap[it2.exerciseId]
                            Text(
                                (if (it2.group.isNotEmpty()) "${rl[it2.group]}  " else "•  ") + (ex?.name ?: it2.exerciseId) + "  —  " + describeItem(it2, ex, unit),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Text("Letters (A, B) mark supersets: do those exercises back-to-back, then rest.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val n = vm.addProgram(pr)
                    snackMsg = if (n > 0) "Added ${pr.name} ($n routines)" else "${pr.name} routines refreshed"
                    programInfo = null
                }) { Text(if (pr.routines.all { r -> data.routines.any { it.id == r.id } }) "Reset to default" else "Add to my routines") }
            },
            dismissButton = { TextButton(onClick = { programInfo = null }) { Text("Close") } },
        )
    }
    editing?.let { r -> RoutineEditor(vm, data, r, onDismiss = { editing = null }) { vm.saveRoutine(it); editing = null } }
    confirmDelete?.let { r ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${r.name}?") },
            text = { Text("Workouts you already logged with it are kept.") },
            confirmButton = { TextButton(onClick = { vm.deleteRoutine(r.id); confirmDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
        )
    }
    startFor?.let { r ->
        var date by remember { mutableStateOf(vm.selectedDate.value) }
        var pick by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { startFor = null },
            title = { Text("Start ${r.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Adds its ${r.items.size} exercises with targets to:")
                    OutlinedButton(onClick = { pick = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text(relativeDay(date) + " · " + fmtShortDate(date)) }
                }
            },
            confirmButton = { TextButton(onClick = { vm.startRoutine(r, date); vm.setDate(date); startFor = null; onStarted() }) { Text("Start") } },
            dismissButton = { TextButton(onClick = { startFor = null }) { Text("Cancel") } },
        )
        if (pick) DatePick(date, onDismiss = { pick = false }) { date = it; pick = false }
    }
}

fun describeItem(item: RoutineItem, ex: Exercise?, unit: WeightUnit): String = when (ex?.trackType ?: TrackType.WEIGHT_REPS) {
    TrackType.WEIGHT_REPS -> "${item.sets} × ${item.reps}" + if (item.weightKg > 0) " @ ${fmtWeight(item.weightKg, unit)}" else ""
    else -> "${item.sets} × " + if (item.durationSec > 0) fmtDuration(item.durationSec) else "—"
}

@Composable
private fun RoutineEditor(vm: MainViewModel, data: AppData, initial: Routine, onDismiss: () -> Unit, onSave: (Routine) -> Unit) {
    val exMap = remember(data.customExercises) { data.exerciseMap() }
    val unit = data.settings.unit
    var name by remember { mutableStateOf(initial.name) }
    var labels by remember { mutableStateOf(initial.labels) }
    val items = remember { mutableStateListOf<RoutineItem>().apply { addAll(initial.items) } }
    var picker by remember { mutableStateOf(false) }
    var editIdx by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                    Text(if (initial.name.isBlank()) "New routine" else "Edit routine", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(enabled = name.isNotBlank() && items.isNotEmpty(), onClick = { onSave(initial.copy(name = name.trim(), labels = labels, items = items.toList())) }) { Text("Save") }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        OutlinedTextField(name, { name = it }, label = { Text("Name (e.g. Chest Day A)") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Text("Labels", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            data.allLabels().forEach { l ->
                                val sel = labels.any { it.equals(l, true) }
                                FilterChip(selected = sel, onClick = { labels = if (sel) labels.filterNot { it.equals(l, true) } else labels + l }, label = { Text(l) })
                            }
                        }
                    }
                    item { SectionTitle("Exercises (${items.size})") }
                    itemsIndexed(items, key = { i, it -> "$i-${it.exerciseId}" }) { i, it2 ->
                        val ex = exMap[it2.exerciseId]
                        val el = groupLettersOf(items.map { it.group })
                        OutlinedCard(Modifier.fillMaxWidth().clickable { editIdx = i }) {
                            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                                    if (it2.group.isNotEmpty()) Text("Superset ${el[it2.group]}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                                    Text(ex?.name ?: "Deleted", fontWeight = FontWeight.SemiBold)
                                    Text(describeItem(it2, ex, unit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (i < items.lastIndex) {
                                    val linked = it2.group.isNotEmpty() && items[i + 1].group == it2.group
                                    IconButton(onClick = {
                                        val list = items.toMutableList()
                                        if (linked) {
                                            // split the group after this item
                                            val g = newId().take(8)
                                            var k = i + 1
                                            while (k < list.size && list[k].group == it2.group) { list[k] = list[k].copy(group = g); k++ }
                                        } else {
                                            val g = it2.group.ifEmpty { items[i + 1].group.ifEmpty { newId().take(8) } }
                                            val old = list[i + 1].group
                                            for (k in list.indices) if (k == i || k == i + 1 || (old.isNotEmpty() && list[k].group == old)) list[k] = list[k].copy(group = g)
                                        }
                                        val norm = normalizeGroups(list, { it.group }, { e, gg -> e.copy(group = gg) })
                                        items.clear(); items.addAll(norm)
                                    }) { Icon(if (linked) Icons.Filled.LinkOff else Icons.Filled.Link, if (linked) "Unlink from next" else "Superset with next", tint = if (linked) MaterialTheme.colorScheme.tertiary else LocalContentColor.current) }
                                }
                                if (i > 0) IconButton(onClick = {
                                    val t = items[i]; items[i] = items[i - 1]; items[i - 1] = t
                                    val norm = normalizeGroups(items.toList(), { it.group }, { e, gg -> e.copy(group = gg) }); items.clear(); items.addAll(norm)
                                }) { Icon(Icons.Filled.ArrowUpward, "Up") }
                                IconButton(onClick = {
                                    items.removeAt(i)
                                    val norm = normalizeGroups(items.toList(), { it.group }, { e, gg -> e.copy(group = gg) }); items.clear(); items.addAll(norm)
                                }) { Icon(Icons.Filled.Close, "Remove") }
                            }
                        }
                    }
                    item {
                        OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercises") }
                    }
                }
            }
        }
    }
    if (picker) {
        ExercisePickerDialog(vm, data, labels, items.map { it.exerciseId }.toSet(), onDismiss = { picker = false }) { ids ->
            ids.forEach { id ->
                val ex = exMap[id] ?: data.exerciseMap()[id]
                val last = Stats.lastBefore(data, id, LocalDate.now().plusDays(1))
                val w = last?.working ?: emptyList()
                items.add(
                    RoutineItem(
                        id, sets = w.size.takeIf { it > 0 } ?: 3,
                        reps = w.maxOfOrNull { it.reps }?.takeIf { it > 0 } ?: if (ex?.trackType == TrackType.WEIGHT_REPS || ex == null) 10 else 0,
                        weightKg = w.maxOfOrNull { it.weightKg } ?: 0.0,
                        durationSec = w.maxOfOrNull { it.durationSec } ?: if (ex?.trackType == TrackType.TIME) 60 else 0,
                    ),
                )
            }
            picker = false
        }
    }
    editIdx?.let { i ->
        val it2 = items.getOrNull(i) ?: return@let
        val ex = exMap[it2.exerciseId]
        var sets by remember(i) { mutableStateOf(it2.sets.toString()) }
        var reps by remember(i) { mutableStateOf(it2.reps.toString()) }
        var weight by remember(i) { mutableStateOf(if (it2.weightKg > 0) fmtNum(Units.toDisplay(it2.weightKg, unit)) else "") }
        var dur by remember(i) { mutableIntStateOf(it2.durationSec) }
        AlertDialog(
            onDismissRequest = { editIdx = null },
            title = { Text(ex?.name ?: "Exercise") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepperField("Sets", sets, { sets = it }, 1.0, false)
                    if ((ex?.trackType ?: TrackType.WEIGHT_REPS) == TrackType.WEIGHT_REPS) {
                        StepperField("Reps", reps, { reps = it }, 1.0, false)
                        StepperField("Weight (${unit.label}, optional)", weight, { weight = it }, if (unit == WeightUnit.KG) 2.5 else 5.0, true)
                    } else DurationField("Time per set", dur, { dur = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    items[i] = it2.copy(
                        sets = (sets.toIntOrNull() ?: 3).coerceIn(1, 20), reps = reps.toIntOrNull() ?: 0,
                        weightKg = Units.fromDisplay(weight.replace(',', '.').toDoubleOrNull() ?: 0.0, unit), durationSec = dur,
                    )
                    editIdx = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { editIdx = null }) { Text("Cancel") } },
        )
    }
}

fun groupLettersOf(groups: List<String>): Map<String, Char> {
    val m = LinkedHashMap<String, Char>()
    groups.forEach { if (it.isNotEmpty() && it !in m) m[it] = 'A' + m.size }
    return m
}
