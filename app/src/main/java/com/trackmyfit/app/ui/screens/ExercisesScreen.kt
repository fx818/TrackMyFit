package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*

/** Maps a day label to muscle groups, used to pre-filter the exercise picker. */
fun groupsForLabels(labels: List<String>): Set<String> = labels.flatMap { l ->
    when (l.lowercase()) {
        "chest" -> listOf("Chest")
        "back" -> listOf("Back")
        "shoulders" -> listOf("Shoulders")
        "biceps" -> listOf("Biceps", "Forearms")
        "triceps" -> listOf("Triceps")
        "arms" -> listOf("Biceps", "Triceps", "Forearms")
        "legs", "lower body" -> listOf("Legs", "Glutes", "Calves")
        "abs", "core" -> listOf("Abs")
        "push" -> listOf("Chest", "Shoulders", "Triceps")
        "pull" -> listOf("Back", "Biceps", "Forearms")
        "upper body" -> listOf("Chest", "Back", "Shoulders", "Biceps", "Triceps")
        "cardio" -> listOf("Cardio")
        else -> Catalog.muscleGroups.filter { it.equals(l, true) }
    }
}.toSet()

private fun matches(e: Exercise, q: String): Boolean {
    if (q.isBlank()) return true
    val hay = (e.name + " " + e.muscleGroup + " " + e.equipment).lowercase()
    return q.lowercase().split(" ").filter { it.isNotBlank() }.all { hay.contains(it) }
}

@Composable
fun ExercisePickerDialog(
    vm: MainViewModel,
    data: AppData,
    dayLabels: List<String>,
    alreadyAdded: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val suggested = remember(dayLabels) { groupsForLabels(dayLabels) }
    var query by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(suggested) }
    val selected = remember { mutableStateListOf<String>() }
    var showAdd by remember { mutableStateOf(false) }
    val usage = remember(data) { Stats.usageCounts(data) }
    val all = remember(data.customExercises) { data.allExercises() }
    val list = remember(all, query, groups, usage) {
        all.filter { (groups.isEmpty() || it.muscleGroup in groups) && matches(it, query) }
            .sortedWith(compareByDescending<Exercise> { usage[it.id] ?: 0 }.thenBy { it.name })
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
                    Text("Add exercises", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, null); Text("New") }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search ${all.size} exercises") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                GroupFilterRow(groups) { groups = it }
                LazyColumn(Modifier.weight(1f)) {
                    if (list.isEmpty()) {
                        item {
                            EmptyState(Icons.Filled.SearchOff, "No exercises found", "Create it as a custom exercise.") {
                                Button(onClick = { showAdd = true }) { Text("Create \"${query.ifBlank { "exercise" }}\"") }
                            }
                        }
                    }
                    items(list, key = { it.id }) { e ->
                        val isSel = e.id in selected
                        ListItem(
                            headlineContent = { Text(e.name) },
                            supportingContent = {
                                Text(listOf(e.muscleGroup, e.equipment).filter { it.isNotBlank() }.joinToString(" · ") +
                                    (if (e.id in alreadyAdded) " · already added" else ""))
                            },
                            trailingContent = { Checkbox(checked = isSel, onCheckedChange = null) },
                            leadingContent = { if (e.isCustom) Icon(Icons.Filled.Person, "Custom") else Icon(Icons.Filled.FitnessCenter, null) },
                            colors = if (isSel) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) else ListItemDefaults.colors(),
                            modifier = Modifier.clickable { if (isSel) selected.remove(e.id) else selected.add(e.id) },
                        )
                    }
                }
                Surface(tonalElevation = 3.dp) {
                    Button(
                        enabled = selected.isNotEmpty(),
                        onClick = { onConfirm(selected.toList()) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text(if (selected.isEmpty()) "Select exercises" else "Add ${selected.size} exercise" + if (selected.size > 1) "s" else "") }
                }
            }
        }
    }
    if (showAdd) {
        ExerciseEditDialog(
            initial = null, initialName = query, initialGroup = groups.singleOrNull(),
            onDismiss = { showAdd = false },
            onSave = { n, g, eq -> val id = vm.addCustomExercise(n, g, eq); selected.add(id); query = ""; groups = emptySet(); showAdd = false },
        )
    }
}

@Composable
private fun GroupFilterRow(groups: Set<String>, onChange: (Set<String>) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = groups.isEmpty(), onClick = { onChange(emptySet()) }, label = { Text("All") })
        Catalog.muscleGroups.forEach { g ->
            FilterChip(selected = g in groups, onClick = { onChange(if (g in groups) groups - g else groups + g) }, label = { Text(g) })
        }
    }
}

@Composable
fun ExercisesScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var groups by remember { mutableStateOf(emptySet<String>()) }
    var customOnly by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    val usage = remember(data) { Stats.usageCounts(data) }
    val list = remember(data.customExercises, query, groups, customOnly) {
        data.allExercises().filter { (!customOnly || it.isCustom) && (groups.isEmpty() || it.muscleGroup in groups) && matches(it, query) }
            .sortedBy { it.name }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Exercises") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showAdd = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Custom exercise") })
        },
    ) { padding ->
        Column(Modifier.padding(top = padding.calculateTopPadding())) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                placeholder = { Text("Search") }, leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") } },
                singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = customOnly, onClick = { customOnly = !customOnly }, label = { Text("My exercises (${data.customExercises.size})") },
                    leadingIcon = { Icon(Icons.Filled.Person, null, Modifier.size(18.dp)) })
                FilterChip(selected = groups.isEmpty(), onClick = { groups = emptySet() }, label = { Text("All") })
                Catalog.muscleGroups.forEach { g ->
                    FilterChip(selected = g in groups, onClick = { groups = if (g in groups) groups - g else groups + g }, label = { Text(g) })
                }
            }
            Text("${list.size} exercises", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                items(list, key = { it.id }) { e ->
                    val n = usage[e.id] ?: 0
                    ListItem(
                        headlineContent = { Text(e.name) },
                        supportingContent = { Text(listOf(e.muscleGroup, e.equipment).filter { it.isNotBlank() }.joinToString(" · ")) },
                        leadingContent = { if (e.isCustom) Icon(Icons.Filled.Person, "Custom") else Icon(Icons.Filled.FitnessCenter, null) },
                        trailingContent = { if (n > 0) Text("$n×", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.clickable { onOpen(e.id) },
                    )
                }
            }
        }
    }
    if (showAdd) {
        ExerciseEditDialog(initial = null, initialName = query, initialGroup = groups.singleOrNull(), onDismiss = { showAdd = false }) { n, g, eq ->
            vm.addCustomExercise(n, g, eq); showAdd = false
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditDialog(
    initial: Exercise?,
    initialName: String = "",
    initialGroup: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: initialName) }
    var group by remember { mutableStateOf(initial?.muscleGroup ?: initialGroup ?: Catalog.muscleGroups.first()) }
    var equipment by remember { mutableStateOf(initial?.equipment ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New exercise" else "Edit exercise") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                SectionTitle("Muscle group")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Catalog.muscleGroups.forEach { g -> FilterChip(selected = g == group, onClick = { group = g }, label = { Text(g) }) }
                }
                SectionTitle("Equipment")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Catalog.equipment.forEach { g -> FilterChip(selected = g == equipment, onClick = { equipment = if (equipment == g) "" else g }, label = { Text(g) }) }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, group, equipment) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
