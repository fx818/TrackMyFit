package com.trackmyfit.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val s = data.settings
    val ctx = LocalContext.current
    var confirmErase by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var manageLabels by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
        }.onSuccess { Toast.makeText(ctx, "Backup saved", Toast.LENGTH_SHORT).show() }
            .onFailure { Toast.makeText(ctx, "Export failed: ${it.message}", Toast.LENGTH_LONG).show() }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }
            .onSuccess { pendingImport = it }
            .onFailure { Toast.makeText(ctx, "Couldn't read file", Toast.LENGTH_LONG).show() }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            SectionTitle("Units")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                WeightUnit.entries.forEachIndexed { i, u ->
                    SegmentedButton(selected = s.unit == u, onClick = { vm.updateSettings { it.copy(unit = u) } }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                        Text(if (u == WeightUnit.KG) "Metric (kg, cm)" else "Imperial (lb, ft)")
                    }
                }
            }
            SectionTitle("Theme")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val opts = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                opts.forEachIndexed { i, (t, l) ->
                    SegmentedButton(selected = s.theme == t, onClick = { vm.updateSettings { it.copy(theme = t) } }, shape = SegmentedButtonDefaults.itemShape(i, opts.size)) { Text(l) }
                }
            }
            SectionTitle("Rest timer")
            ListItem(
                headlineContent = { Text("Start rest timer after each set") },
                supportingContent = { Text("Vibrates when rest is over (today's log only)") },
                trailingContent = { Switch(checked = s.restTimerEnabled, onCheckedChange = { c -> vm.updateSettings { it.copy(restTimerEnabled = c) } }) },
            )
            if (s.restTimerEnabled) {
                Text("Rest duration: %d:%02d".format(s.restSeconds / 60, s.restSeconds % 60), modifier = Modifier.padding(horizontal = 16.dp))
                Slider(
                    value = s.restSeconds.toFloat(), valueRange = 15f..300f, steps = (300 - 15) / 15 - 1,
                    onValueChange = { v -> vm.updateSettings { it.copy(restSeconds = (Math.round(v / 15f) * 15)) } },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            SectionTitle("Day labels")
            ListItem(
                headlineContent = { Text("Manage custom labels") },
                supportingContent = { Text(if (data.customLabels.isEmpty()) "None yet — add from the Log screen" else data.customLabels.joinToString(", ")) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, null) },
                modifier = Modifier.clickable { manageLabels = true },
            )
            SectionTitle("Your data")
            Text(
                "Everything is stored only on this phone (no account, no cloud, no database). Export a backup file to keep it safe or move it to a new phone.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp),
            )
            ListItem(
                headlineContent = { Text("Export backup") }, supportingContent = { Text("Save all data as a JSON file") },
                leadingContent = { Icon(Icons.Filled.Upload, null) },
                modifier = Modifier.clickable { exportLauncher.launch("trackmyfit-backup-${LocalDate.now()}.json") },
            )
            ListItem(
                headlineContent = { Text("Import backup") }, supportingContent = { Text("Restore from a JSON backup file") },
                leadingContent = { Icon(Icons.Filled.Download, null) },
                modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            )
            ListItem(
                headlineContent = { Text("Erase all data", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { confirmErase = true },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "TrackMyFit 1.0 · ${Stats.totalWorkouts(data)} workouts · ${data.allExercises().size} exercises",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase all data?") },
            text = { Text("All workouts, body weights, profile and custom exercises will be permanently deleted. Export a backup first if unsure.") },
            confirmButton = { TextButton(onClick = { vm.eraseAll(); confirmErase = false }) { Text("Erase", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmErase = false }) { Text("Cancel") } },
        )
    }
    pendingImport?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces ALL current data in the app with the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    val r = vm.importJson(text)
                    Toast.makeText(ctx, if (r.isSuccess) "Backup restored" else "Invalid backup file", Toast.LENGTH_LONG).show()
                    pendingImport = null
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
        )
    }
    if (manageLabels) {
        AlertDialog(
            onDismissRequest = { manageLabels = false },
            title = { Text("Custom labels") },
            text = {
                Column {
                    if (data.customLabels.isEmpty()) Text("No custom labels.")
                    data.customLabels.forEach { l ->
                        ListItem(headlineContent = { Text(l) }, trailingContent = {
                            IconButton(onClick = { vm.removeCustomLabel(l) }) { Icon(Icons.Filled.Delete, "Delete") }
                        })
                    }
                    Text("Removing a label doesn't change days already tagged with it.", style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = { TextButton(onClick = { manageLabels = false }) { Text("Done") } },
        )
    }
}
