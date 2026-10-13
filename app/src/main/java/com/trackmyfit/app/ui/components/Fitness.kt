package com.trackmyfit.app.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trackmyfit.app.data.*
import java.util.Locale

// ---------------------------------------------------------------- formatting

fun fmtDuration(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

fun fmtMinutes(min: Int): String = if (min >= 60) "${min / 60}h ${"%02d".format(min % 60)}m" else "${min}m"

fun fmtDistance(km: Double, unit: WeightUnit): String = fmtNum(Units.distToDisplay(km, unit)) + " " + unit.distLabel

fun fmtPace(km: Double, sec: Int, unit: WeightUnit): String? {
    val d = Units.distToDisplay(km, unit)
    if (d <= 0 || sec <= 0) return null
    val p = (sec / d).toInt()
    return "%d:%02d/%s".format(p / 60, p % 60, unit.distLabel)
}

/** One-line description of a set, depending on how the exercise is tracked. */
fun fmtSet(s: WorkoutSet, type: TrackType, unit: WeightUnit): String = when (type) {
    TrackType.WEIGHT_REPS -> if (s.weightKg > 0) "${fmtWeight(s.weightKg, unit)} × ${s.reps}" else "${s.reps} reps"
    TrackType.TIME -> fmtDuration(s.durationSec) + if (s.weightKg > 0) " · +${fmtWeight(s.weightKg, unit)}" else ""
    TrackType.DISTANCE_TIME -> listOfNotNull(
        s.distanceKm.takeIf { it > 0 }?.let { fmtDistance(it, unit) },
        s.durationSec.takeIf { it > 0 }?.let { fmtDuration(it) },
        fmtPace(s.distanceKm, s.durationSec, unit),
    ).joinToString(" · ").ifEmpty { "—" }
}

fun fmtSetShort(s: WorkoutSet, type: TrackType, unit: WeightUnit): String = when (type) {
    TrackType.WEIGHT_REPS -> if (s.weightKg > 0) "${fmtWeightBare(s.weightKg, unit)}×${s.reps}" else "${s.reps}"
    TrackType.TIME -> fmtDuration(s.durationSec)
    TrackType.DISTANCE_TIME -> listOfNotNull(
        s.distanceKm.takeIf { it > 0 }?.let { fmtDistance(it, unit) },
        s.durationSec.takeIf { it > 0 }?.let { fmtDuration(it) },
    ).joinToString(" ")
}

/** Parses "m:ss", "h:mm:ss" or plain seconds. */
fun parseDuration(text: String): Int? {
    val t = text.trim()
    if (t.isEmpty()) return null
    val parts = t.split(":").map { it.trim().toIntOrNull() ?: return null }
    return when (parts.size) {
        1 -> parts[0]
        2 -> parts[0] * 60 + parts[1]
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        else -> null
    }
}

fun capitalizeWords(s: String) = s.split(" ").joinToString(" ") { w -> w.replaceFirstChar { it.titlecase(Locale.getDefault()) } }

// ---------------------------------------------------------------- set type badge

@Composable
fun SetTypeBadge(type: SetType) {
    if (type == SetType.NORMAL) return
    val (bg, fg) = when (type) {
        SetType.WARMUP -> Color(0xFF4FA3E0) to Color.White
        SetType.DROP -> Color(0xFFB57EDC) to Color.White
        SetType.FAILURE -> Color(0xFFE5534B) to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = bg, shape = RoundedCornerShape(4.dp)) {
        Text(type.label, color = fg, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
    }
}

// ---------------------------------------------------------------- progress ring

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    stroke: Dp = 10.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    content: @Composable BoxScope.() -> Unit,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(700), label = "ring")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val d = this.size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            drawArc(color, -90f, 360f * animated, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
        }
        content()
    }
}

// ---------------------------------------------------------------- exercise demo

/**
 * Animated demo. Photo demos play their frames start -> end -> start, cross-fading between
 * neighbouring frames and holding briefly on the start and end positions (2 frames = simple
 * cross-fade, 5 frames = smoother motion). Exercises without photos fall back to the
 * vector stick-figure animation from poses.json. Tap to pause.
 */
@Composable
fun ExerciseDemo(exerciseId: String, modifier: Modifier = Modifier, maxWidth: Int = 480, showControls: Boolean = true) {
    val ctx = LocalContext.current
    val info = remember(exerciseId) { ExerciseInfoRepo.get(ctx, exerciseId) }
    val count = info?.images ?: 0
    if (count == 0) {
        val pose = remember(exerciseId) { PoseRepo.get(ctx, exerciseId) }
        if (pose != null) PoseAnimation(pose, modifier, showControls)
        return
    }
    var frames by remember(exerciseId) { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(exerciseId, count, maxWidth) {
        // Thumbnails only need start/end; full demos load every frame.
        val idx = if (maxWidth < 300 && count > 2) listOf(0, count - 1) else (0 until count).toList()
        frames = idx.mapNotNull { ExerciseInfoRepo.frame(ctx, exerciseId, it, maxWidth) }
    }
    var playing by remember { mutableStateOf(true) }
    val transition = rememberInfiniteTransition(label = "demo")
    // Phase 0..1..0: hold at start, move to end, hold, move back.
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600
                0f at 0
                0f at 600 using LinearEasing
                1f at 1800 using FastOutSlowInEasing
                1f at 2400 using LinearEasing
                0f at 3600 using FastOutSlowInEasing
            },
        ),
        label = "phase",
    )
    var frozen by remember { mutableFloatStateOf(0f) }
    val phase = if (playing) t else frozen
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(850f / 567f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEDEDED))
            .clickable { if (playing) frozen = t; playing = !playing },
    ) {
        if (frames.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            val pos = phase * (frames.size - 1)
            val i = pos.toInt().coerceIn(0, frames.size - 1)
            val next = (i + 1).coerceAtMost(frames.size - 1)
            val f = pos - i
            Image(frames[i].asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (next != i && f > 0.001f) {
                Image(frames[next].asImageBitmap(), null, Modifier.fillMaxSize().alpha(f), contentScale = ContentScale.Crop)
            }
        }
        if (showControls && frames.size > 1) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    when { phase < 0.15f -> "Start"; phase > 0.85f -> "Finish"; else -> "Moving" },
                    color = Color.White, style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
fun ExerciseInfoContent(exercise: Exercise, modifier: Modifier = Modifier, showDemo: Boolean = true) {
    val ctx = LocalContext.current
    val info = remember(exercise.id) { ExerciseInfoRepo.get(ctx, exercise.id) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (showDemo && hasDemo(ctx, exercise.id)) {
            ExerciseDemo(exercise.id)
            Text(
                if ((info?.images ?: 0) > 0) "Tap the demo to pause" else "Animated guide · tap to pause",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (info == null) {
            Text(
                if (exercise.isCustom) "This is your custom exercise, so there's no demo for it. Add form notes to each logged exercise if you like."
                else "No demo available for this exercise.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        if (info.primary.isNotEmpty() || info.secondary.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                info.primary.forEach { Pill(capitalizeWords(it), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary) }
                info.secondary.forEach { Pill(capitalizeWords(it)) }
                if (info.level.isNotBlank()) Pill(capitalizeWords(info.level), MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface)
            }
        }
        if (info.instructions.isNotEmpty()) {
            Text("How to do it", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            info.instructions.forEachIndexed { i, step ->
                Row {
                    Box(
                        Modifier.size(22.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                    Spacer(Modifier.width(10.dp))
                    Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
            }
        }
        if (info.tips.isNotEmpty()) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(6.dp))
                        Text("Form tips", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    info.tips.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                }
            }
        }
        if (info.source == "free-exercise-db") {
            Text("Demo photos & instructions: free-exercise-db (public domain)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Bottom sheet with the animated demo + form instructions. */
@Composable
fun HowToSheet(exercise: Exercise, onDismiss: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(listOf(exercise.muscleGroup, exercise.equipment).filter { it.isNotBlank() }.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            ExerciseInfoContent(exercise)
        }
    }
}

fun hasDemo(ctx: android.content.Context, exerciseId: String): Boolean =
    (ExerciseInfoRepo.get(ctx, exerciseId)?.images ?: 0) > 0 || PoseRepo.get(ctx, exerciseId) != null

// ---------------------------------------------------------------- plate calculator

private fun plateColor(p: Double, unit: WeightUnit): Color {
    val kg = if (unit == WeightUnit.KG) p else p / Units.LB_PER_KG
    return when {
        kg >= 24 -> Color(0xFFE5534B)
        kg >= 19 -> Color(0xFF4FA3E0)
        kg >= 14 -> Color(0xFFF2B33D)
        kg >= 9 -> Color(0xFF3DBE7A)
        kg >= 4.5 -> Color(0xFFEEEEEE)
        kg >= 2 -> Color(0xFF555A70)
        else -> Color(0xFF9AA0B5)
    }
}

@Composable
fun PlateCalculatorDialog(unit: WeightUnit, initialKg: Double, barKg: Double, onBarChange: (Double) -> Unit, onDismiss: () -> Unit) {
    val plates = if (unit == WeightUnit.KG) Plates.kgPlates else Plates.lbPlates
    val bars = if (unit == WeightUnit.KG) Plates.kgBars else Plates.lbBars
    var target by remember { mutableStateOf(if (initialKg > 0) fmtNum(Units.toDisplay(initialKg, unit)) else "") }
    var bar by remember {
        val disp = Units.toDisplay(barKg, unit)
        mutableDoubleStateOf(bars.minByOrNull { kotlin.math.abs(it - disp) } ?: bars.first())
    }
    val t = target.replace(',', '.').toDoubleOrNull() ?: 0.0
    val res = Plates.calculate(t, bar, plates)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Calculate, null) },
        title = { Text("Plate calculator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StepperField("Total weight (${unit.label})", target, { target = it }, step = if (unit == WeightUnit.KG) 2.5 else 5.0, decimal = true)
                Text("Bar", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bars.forEach { b ->
                        FilterChip(selected = bar == b, onClick = { bar = b; onBarChange(Units.fromDisplay(b, unit)) },
                            label = { Text(if (b == 0.0) "None" else fmtNum(b)) })
                    }
                }
                HorizontalDivider()
                if (t <= bar) {
                    Text(if (t <= 0) "Enter a weight." else "That's just the bar — no plates needed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Each side:", style = MaterialTheme.typography.labelLarge)
                    // visual bar
                    Row(Modifier.fillMaxWidth().height(90.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(36.dp).height(10.dp).background(Color(0xFF9AA0B5), RoundedCornerShape(2.dp)))
                        Box(Modifier.width(6.dp).height(26.dp).background(Color(0xFF6C7088)))
                        res.perSide.forEach { p ->
                            val h = (40 + 50 * (p / plates.max())).dp
                            Box(
                                Modifier.padding(horizontal = 1.dp).width(14.dp).height(h)
                                    .background(plateColor(p, unit), RoundedCornerShape(3.dp))
                                    .border(1.dp, Color.Black.copy(alpha = 0.25f), RoundedCornerShape(3.dp)),
                            )
                        }
                        Box(Modifier.width(20.dp).height(10.dp).background(Color(0xFF9AA0B5), RoundedCornerShape(2.dp)))
                    }
                    val grouped = res.perSide.groupingBy { it }.eachCount().toList().sortedByDescending { it.first }
                    Text(
                        grouped.joinToString("  +  ") { (p, n) -> "${n} × ${fmtNum(p)}" } + " ${unit.label}",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    )
                    if (res.remainder > 0.001) {
                        Text("Closest possible: ${fmtNum(res.loaded)} ${unit.label} (${fmtNum(res.remainder)} short)", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

// ---------------------------------------------------------------- duration field

@Composable
fun DurationField(label: String, seconds: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var min by remember { mutableStateOf(if (seconds > 0) (seconds / 60).toString() else "") }
    var sec by remember { mutableStateOf(if (seconds > 0) (seconds % 60).toString() else "") }
    fun push() = onChange((min.toIntOrNull() ?: 0) * 60 + (sec.toIntOrNull() ?: 0).coerceIn(0, 59))
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                min, { v -> min = v.filter(Char::isDigit).take(3); push() }, label = { Text("min") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            )
            Text(":", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                sec, { v -> sec = v.filter(Char::isDigit).take(2); push() }, label = { Text("sec") }, singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.weight(1f), textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            )
        }
    }
}
