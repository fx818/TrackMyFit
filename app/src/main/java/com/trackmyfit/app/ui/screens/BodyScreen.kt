package com.trackmyfit.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trackmyfit.app.data.*
import com.trackmyfit.app.ui.*
import com.trackmyfit.app.ui.components.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

fun fmtHeight(cm: Double, unit: WeightUnit): String {
    if (cm <= 0) return "Not set"
    if (unit == WeightUnit.KG) return "${fmtNum(cm)} cm"
    val totalIn = (cm / 2.54).roundToInt()
    return "${totalIn / 12}′ ${totalIn % 12}″"
}

@Composable
fun BodyScreen(vm: MainViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val unit = data.settings.unit
    val p = data.profile
    val latest = data.bodyWeights.lastOrNull()
    var showWeight by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf(false) }
    var deleteEntry by remember { mutableStateOf<BodyWeightEntry?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Body & BMI") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showWeight = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Log weight") })
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding(), 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                DashCard("Profile", Icons.Filled.Person, action = { TextButton(onClick = { editProfile = true }) { Text("Edit") } }) {
                    Row {
                        ProfileItem("Height", fmtHeight(p.heightCm, unit), Modifier.weight(1f))
                        ProfileItem("Goal", if (p.goalWeightKg > 0) fmtWeight(p.goalWeightKg, unit) else "Not set", Modifier.weight(1f))
                        ProfileItem("Age", if (p.birthYear > 0) "${LocalDate.now().year - p.birthYear}" else "—", Modifier.weight(1f))
                    }
                    if (p.heightCm <= 0) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { editProfile = true }) { Text("Add your height for BMI") }
                    }
                }
            }
            if (latest != null && p.heightCm > 0) {
                val bmi = Bmi.calc(latest.weightKg, p.heightCm)!!
                item {
                    DashCard("BMI", Icons.Filled.MonitorHeart) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("%.1f".format(bmi), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = bmiColor(bmi))
                            Spacer(Modifier.width(10.dp))
                            Text(Bmi.category(bmi), style = MaterialTheme.typography.titleMedium, color = bmiColor(bmi), modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        BmiGauge(bmi)
                        Spacer(Modifier.height(10.dp))
                        Bmi.healthyRangeKg(p.heightCm)?.let { (lo, hi) ->
                            Text("Healthy weight for your height: ${fmtWeightBare(lo, unit)} – ${fmtWeight(hi, unit)}", style = MaterialTheme.typography.bodyMedium)
                            val w = latest.weightKg
                            val msg = when {
                                w < lo -> "You are ${fmtWeight(lo - w, unit)} below the healthy range."
                                w > hi -> "You are ${fmtWeight(w - hi, unit)} above the healthy range."
                                else -> "You are within the healthy range."
                            }
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val bmr = bmr(latest.weightKg, p)
                        if (bmr != null) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            Row {
                                ProfileItem("BMR", "${bmr.roundToInt()} kcal", Modifier.weight(1f))
                                ProfileItem("Maintenance*", "${(bmr * 1.55).roundToInt()} kcal", Modifier.weight(1f))
                            }
                            Text("*Estimate for moderately active (3–5 workouts/week), Mifflin-St Jeor formula.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Add age and sex in your profile to estimate daily calories (BMR).", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }
                        Text("BMI doesn't distinguish muscle from fat, so it can read high for muscular people.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            item { WeightTrendCard(data, unit, onLogWeight = { showWeight = true }) }
            if (data.bodyWeights.size >= 2 && p.heightCm > 0) {
                item {
                    DashCard("BMI trend", Icons.AutoMirrored.Filled.ShowChart) {
                        val pts = data.bodyWeights.takeLast(60)
                        LineChart(pts.map { Bmi.calc(it.weightKg, p.heightCm) ?: 0.0 }, pts.map { fmtShortDate(LocalDate.parse(it.date)) }, valueFormatter = { "%.1f".format(it) }, height = 150.dp)
                    }
                }
            }
            if (data.bodyWeights.isNotEmpty()) {
                item { SectionTitle("Weight log") }
                val rev = data.bodyWeights.reversed()
                items(rev, key = { it.date }) { e ->
                    val i = rev.indexOf(e)
                    val prev = rev.getOrNull(i + 1)
                    val diff = prev?.let { e.weightKg - it.weightKg }
                    ListItem(
                        headlineContent = { Text(fmtWeight(e.weightKg, unit), fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(fmtLongDate(LocalDate.parse(e.date)) + (Bmi.calc(e.weightKg, p.heightCm)?.let { " · BMI %.1f".format(it) } ?: ""))
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (diff != null && kotlin.math.abs(diff) >= 0.05) {
                                    Text(
                                        (if (diff > 0) "+" else "−") + fmtWeightBare(kotlin.math.abs(diff), unit),
                                        color = if (diff > 0) BmiYellow else BmiGreen, fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                IconButton(onClick = { deleteEntry = e }) { Icon(Icons.Filled.DeleteOutline, "Delete") }
                            }
                        },
                    )
                }
            }
        }
    }
    if (showWeight) {
        WeightLogDialog(unit, latest?.weightKg, onDismiss = { showWeight = false }) { d, kg -> vm.logBodyWeight(d, kg); showWeight = false }
    }
    if (editProfile) {
        ProfileDialog(p, unit, onDismiss = { editProfile = false }) { vm.updateProfile { _ -> it }; editProfile = false }
    }
    deleteEntry?.let { e ->
        AlertDialog(
            onDismissRequest = { deleteEntry = null },
            title = { Text("Delete entry?") },
            text = { Text("${fmtWeight(e.weightKg, unit)} on ${fmtLongDate(LocalDate.parse(e.date))}") },
            confirmButton = { TextButton(onClick = { vm.deleteBodyWeight(e.date); deleteEntry = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteEntry = null }) { Text("Cancel") } },
        )
    }
}

/** Mifflin-St Jeor basal metabolic rate. */
fun bmr(weightKg: Double, p: UserProfile): Double? {
    if (p.heightCm <= 0 || p.birthYear <= 0 || p.sex == Sex.UNSPECIFIED) return null
    val age = LocalDate.now().year - p.birthYear
    val base = 10 * weightKg + 6.25 * p.heightCm - 5 * age
    return if (p.sex == Sex.MALE) base + 5 else base - 161
}

@Composable
private fun ProfileItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun WeightLogDialog(unit: WeightUnit, initialKg: Double?, onDismiss: () -> Unit, onSave: (LocalDate, Double) -> Unit) {
    var text by remember { mutableStateOf(initialKg?.let { fmtNum(Units.toDisplay(it, unit)) } ?: "") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var pickDate by remember { mutableStateOf(false) }
    val v = text.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log body weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Weight (${unit.label})", text, { text = it }, step = if (unit == WeightUnit.KG) 0.1 else 0.2, decimal = true)
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CalendarMonth, null); Spacer(Modifier.width(6.dp)); Text(fmtLongDate(date))
                }
                Text("Logging again on the same date replaces that day's entry.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(enabled = v != null && v > 0 && v < 1000, onClick = { onSave(date, Units.fromDisplay(v!!, unit)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
    if (pickDate) {
        val st = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = { TextButton(onClick = { st.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }; pickDate = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) { DatePicker(st) }
    }
}

@Composable
private fun ProfileDialog(p: UserProfile, unit: WeightUnit, onDismiss: () -> Unit, onSave: (UserProfile) -> Unit) {
    val metric = unit == WeightUnit.KG
    var name by remember { mutableStateOf(p.name) }
    var cm by remember { mutableStateOf(if (p.heightCm > 0) fmtNum(p.heightCm) else "") }
    val totalIn = (p.heightCm / 2.54).roundToInt()
    var ft by remember { mutableStateOf(if (p.heightCm > 0) "${totalIn / 12}" else "") }
    var inch by remember { mutableStateOf(if (p.heightCm > 0) "${totalIn % 12}" else "") }
    var goal by remember { mutableStateOf(if (p.goalWeightKg > 0) fmtNum(Units.toDisplay(p.goalWeightKg, unit)) else "") }
    var year by remember { mutableStateOf(if (p.birthYear > 0) "${p.birthYear}" else "") }
    var sex by remember { mutableStateOf(p.sex) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)

    fun heightCm(): Double = if (metric) cm.replace(',', '.').toDoubleOrNull() ?: 0.0
        else ((ft.toIntOrNull() ?: 0) * 12 + (inch.toIntOrNull() ?: 0)) * 2.54

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.verticalScrollSafe()) {
                OutlinedTextField(name, { name = it }, label = { Text("Name (optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth())
                if (metric) {
                    OutlinedTextField(cm, { cm = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) }, label = { Text("Height (cm)") }, singleLine = true, keyboardOptions = num, modifier = Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(ft, { ft = it.filter(Char::isDigit).take(1) }, label = { Text("Height ft") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                        OutlinedTextField(inch, { inch = it.filter(Char::isDigit).take(2) }, label = { Text("in") }, singleLine = true, keyboardOptions = num, modifier = Modifier.weight(1f))
                    }
                }
                OutlinedTextField(goal, { goal = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6) }, label = { Text("Goal weight (${unit.label}, optional)") }, singleLine = true, keyboardOptions = num, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Birth year (optional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Text("Sex (for calorie estimate)", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val opts = listOf(Sex.MALE to "Male", Sex.FEMALE to "Female", Sex.UNSPECIFIED to "Skip")
                    opts.forEachIndexed { i, (s, l) ->
                        SegmentedButton(selected = sex == s, onClick = { sex = s }, shape = SegmentedButtonDefaults.itemShape(i, opts.size)) { Text(l) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = heightCm()
                val y = year.toIntOrNull()?.takeIf { it in 1900..LocalDate.now().year } ?: 0
                onSave(UserProfile(
                    name = name.trim(),
                    heightCm = if (h in 50.0..272.0) h else 0.0,
                    goalWeightKg = goal.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }?.let { Units.fromDisplay(it, unit) } ?: 0.0,
                    birthYear = y, sex = sex,
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Modifier.verticalScrollSafe(): Modifier = this.then(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()))
