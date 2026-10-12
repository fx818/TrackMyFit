package com.trackmyfit.app.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.notify.Notifier
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onOpenSteps: () -> Unit) {
    val data by vm.data.collectAsStateWithLifecycle()
    val s = data.settings
    val ctx = LocalContext.current
    var confirmErase by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var manageLabels by remember { mutableStateOf(false) }
    var stepGoalDialog by remember { mutableStateOf(false) }
    var notifGranted by remember { mutableStateOf(Notifier.canPost(ctx)) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        notifGranted = ok
        if (!ok) Toast.makeText(ctx, "Notifications are off — enable them in system settings for reminders and the rest timer.", Toast.LENGTH_LONG).show()
    }
    fun ensureNotif() {
        if (!Notifier.canPost(ctx) && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

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
            SectionTitle("Goals")
            Text("Workouts per week: ${s.weeklyGoal}", modifier = Modifier.padding(horizontal = 16.dp))
            Slider(
                value = s.weeklyGoal.toFloat(), valueRange = 1f..7f, steps = 5,
                onValueChange = { v -> vm.updateSettings { it.copy(weeklyGoal = v.toInt()) } },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            ListItem(
                headlineContent = { Text("Daily step goal") },
                supportingContent = { Text(java.text.NumberFormat.getIntegerInstance().format(s.stepGoal) + " steps") },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null) },
                modifier = Modifier.clickable { stepGoalDialog = true },
            )
            ListItem(
                headlineContent = { Text("Step tracking (Health Connect)") },
                supportingContent = { Text("Connect / view your steps") },
                leadingContent = { Icon(Icons.Filled.MonitorHeart, null) },
                modifier = Modifier.clickable(onClick = onOpenSteps),
            )

            SectionTitle("Reminders")
            ListItem(
                headlineContent = { Text("Gym day reminders") },
                supportingContent = { Text("Skipped automatically if you've already logged a workout or marked a rest day") },
                trailingContent = {
                    Switch(checked = s.remindersEnabled, onCheckedChange = { c ->
                        if (c) ensureNotif()
                        vm.updateSettings { it.copy(remindersEnabled = c) }
                    })
                },
            )
            if (s.remindersEnabled) {
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        val sel = d.value in s.reminderDays
                        FilterChip(selected = sel, onClick = {
                            vm.updateSettings { it.copy(reminderDays = if (sel) it.reminderDays - d.value else it.reminderDays + d.value) }
                        }, label = { Text(d.getDisplayName(TextStyle.SHORT, Locale.getDefault())) })
                    }
                }
                ListItem(
                    headlineContent = { Text("Reminder time") },
                    supportingContent = { Text("%02d:%02d".format(s.reminderHour, s.reminderMinute)) },
                    leadingContent = { Icon(Icons.Filled.Alarm, null) },
                    modifier = Modifier.clickable {
                        TimePickerDialog(ctx, { _, h, m -> vm.updateSettings { it.copy(reminderHour = h, reminderMinute = m) } }, s.reminderHour, s.reminderMinute, true).show()
                    },
                )
            }
            if (!notifGranted) {
                ListItem(
                    headlineContent = { Text("Notifications are off") },
                    supportingContent = { Text("Needed for reminders and the rest timer while you're in other apps. Tap to allow.") },
                    leadingContent = { Icon(Icons.Filled.NotificationsOff, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        if (Build.VERSION.SDK_INT >= 33 && !s.askedNotificationPermission) {
                            vm.updateSettings { it.copy(askedNotificationPermission = true) }; ensureNotif()
                        } else {
                            ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                        }
                    },
                )
            }

            SectionTitle("Units")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                WeightUnit.entries.forEachIndexed { i, u ->
                    SegmentedButton(selected = s.unit == u, onClick = { vm.updateSettings { it.copy(unit = u) } }, shape = SegmentedButtonDefaults.itemShape(i, 2)) {
                        Text(if (u == WeightUnit.KG) "Metric (kg, km)" else "Imperial (lb, mi)")
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
                supportingContent = { Text("Shows a countdown notification and alerts you when rest is over, even in other apps") },
                trailingContent = { Switch(checked = s.restTimerEnabled, onCheckedChange = { c -> if (c) ensureNotif(); vm.updateSettings { it.copy(restTimerEnabled = c) } }) },
            )
            if (s.restTimerEnabled) {
                Text("Rest duration: " + fmtDuration(s.restSeconds), modifier = Modifier.padding(horizontal = 16.dp))
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
                "Everything is stored only on this phone (no account, no cloud, no database). Export a backup file to keep it safe or move it to a new phone. Progress photos are not included in the backup file.",
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
                "TrackMyFit 1.1 · ${Stats.totalWorkouts(data)} workouts · ${data.allExercises().size} exercises\nExercise demos & instructions: free-exercise-db (public domain)",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp),
            )
        }
    }

    if (stepGoalDialog) {
        var t by remember { mutableStateOf(s.stepGoal.toString()) }
        AlertDialog(
            onDismissRequest = { stepGoalDialog = false },
            title = { Text("Daily step goal") },
            text = {
                Column {
                    OutlinedTextField(t, { t = it.filter(Char::isDigit).take(6) }, label = { Text("Steps") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(6000, 8000, 10000, 12000).forEach { v -> AssistChip(onClick = { t = v.toString() }, label = { Text("${v / 1000}k") }) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.updateSettings { it.copy(stepGoal = (t.toIntOrNull() ?: 8000).coerceIn(500, 100000)) }; stepGoalDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { stepGoalDialog = false }) { Text("Cancel") } },
        )
    }
    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase all data?") },
            text = { Text("All workouts, routines, body weights, measurements, photos, profile and custom exercises will be permanently deleted. Export a backup first if unsure.") },
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
