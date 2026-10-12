package com.trackmyfit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trackmyfit.app.data.Units
import com.trackmyfit.app.data.WeightUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun fmtNum(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "0"
    return BigDecimal(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

fun fmtWeight(kg: Double, unit: WeightUnit): String = fmtNum(Units.toDisplay(kg, unit)) + " " + unit.label
fun fmtWeightBare(kg: Double, unit: WeightUnit): String = fmtNum(Units.toDisplay(kg, unit))
fun fmtVolume(kg: Double, unit: WeightUnit): String =
    String.format(Locale.getDefault(), "%,d", Math.round(Units.toDisplay(kg, unit))) + " " + unit.label

private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
private val longFmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault())
fun fmtShortDate(d: LocalDate): String = d.format(shortFmt)
fun fmtLongDate(d: LocalDate): String = d.format(longFmt)
fun relativeDay(d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> d.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary, modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, sub: String? = null) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) { Spacer(Modifier.height(16.dp)); action() }
    }
}

/** Number input with -/+ buttons. */
@Composable
fun StepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    step: Double,
    decimal: Boolean,
    modifier: Modifier = Modifier,
) {
    fun bump(dir: Int) {
        val cur = value.replace(',', '.').toDoubleOrNull() ?: 0.0
        val next = (cur + dir * step).coerceAtLeast(0.0)
        onValueChange(if (decimal) fmtNum(next) else next.toInt().toString())
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = { bump(-1) }) { Icon(Icons.Filled.Remove, "Decrease") }
        OutlinedTextField(
            value = value,
            onValueChange = { s -> if (s.length <= 7 && s.all { it.isDigit() || (decimal && (it == '.' || it == ',')) }) onValueChange(s) },
            label = { Text(label) },
            singleLine = true,
            textStyle = LocalTextStyleCentered(),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        FilledTonalIconButton(onClick = { bump(1) }) { Icon(Icons.Filled.Add, "Increase") }
    }
}

@Composable
private fun LocalTextStyleCentered(): TextStyle =
    MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)

/** Simple line chart drawn with Canvas. */
@Composable
fun LineChart(
    values: List<Double>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    valueFormatter: (Double) -> String = { fmtNum(it) },
    refLine: Double? = null,
    height: androidx.compose.ui.unit.Dp = 180.dp,
) {
    val color = MaterialTheme.colorScheme.primary
    val refColor = MaterialTheme.colorScheme.tertiary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 10.sp, color = labelColor)
    Canvas(modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val leftPad = 44.dp.toPx(); val bottomPad = 18.dp.toPx(); val topPad = 8.dp.toPx(); val rightPad = 8.dp.toPx()
        val w = size.width - leftPad - rightPad
        val h = size.height - bottomPad - topPad
        var min = minOf(values.min(), refLine ?: values.min()); var max = maxOf(values.max(), refLine ?: values.max())
        if (max - min < 1e-6) { min -= 1; max += 1 }
        val padRange = (max - min) * 0.1
        min = (min - padRange).coerceAtLeast(0.0); max += padRange
        for (i in 0..3) {
            val y = topPad + h * i / 3f
            drawLine(grid, Offset(leftPad, y), Offset(size.width - rightPad, y), strokeWidth = 1f)
            val v = max - (max - min) * i / 3.0
            val t = measurer.measure(valueFormatter(v), style)
            drawText(t, topLeft = Offset(leftPad - t.size.width - 6.dp.toPx(), y - t.size.height / 2f))
        }
        fun px(i: Int) = leftPad + if (values.size == 1) w / 2f else w * i / (values.size - 1).toFloat()
        fun py(v: Double) = topPad + h * (1f - ((v - min) / (max - min)).toFloat())
        if (refLine != null) {
            val y = py(refLine)
            drawLine(refColor, Offset(leftPad, y), Offset(size.width - rightPad, y), strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f)))
        }
        val fill = Path()
        values.forEachIndexed { i, v -> if (i == 0) fill.moveTo(px(i), py(v)) else fill.lineTo(px(i), py(v)) }
        fill.lineTo(px(values.lastIndex), topPad + h); fill.lineTo(px(0), topPad + h); fill.close()
        drawPath(fill, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0f)), startY = topPad, endY = topPad + h))
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(px(i), py(v)) else path.lineTo(px(i), py(v)) }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        if (values.size <= 40) values.forEachIndexed { i, v -> drawCircle(color, 3.5.dp.toPx(), Offset(px(i), py(v))) }
        if (xLabels.isNotEmpty()) {
            val idx = if (xLabels.size <= 2) xLabels.indices.toList() else listOf(0, xLabels.size / 2, xLabels.size - 1)
            idx.distinct().forEach { i ->
                val t = measurer.measure(xLabels[i], style)
                val x = (px(i) - t.size.width / 2f).coerceIn(leftPad, size.width - t.size.width.toFloat())
                drawText(t, topLeft = Offset(x, size.height - t.size.height))
            }
        }
    }
}

/** Simple vertical bar chart. */
@Composable
fun BarChart(values: List<Int>, xLabels: List<String>, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val color = MaterialTheme.colorScheme.primary
    val dim = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontSize = 10.sp, color = labelColor)
    Canvas(modifier.fillMaxWidth().height(150.dp)) {
        if (values.isEmpty()) return@Canvas
        val bottomPad = 16.dp.toPx(); val topPad = 14.dp.toPx()
        val h = size.height - bottomPad - topPad
        val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        val slot = size.width / values.size
        val bw = slot * 0.6f
        values.forEachIndexed { i, v ->
            val bh = h * v / max
            val x = slot * i + (slot - bw) / 2
            val c = if (highlightLast && i == values.lastIndex) color else dim
            drawRoundRect(c, topLeft = Offset(x, topPad + h - bh), size = androidx.compose.ui.geometry.Size(bw, bh.coerceAtLeast(2f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            if (v > 0) {
                val t = measurer.measure(v.toString(), style)
                drawText(t, topLeft = Offset(x + bw / 2 - t.size.width / 2f, topPad + h - bh - t.size.height))
            }
            if (i < xLabels.size && (values.size <= 8 || i % 2 == (values.size - 1) % 2)) {
                val t = measurer.measure(xLabels[i], style)
                drawText(t, topLeft = Offset(x + bw / 2 - t.size.width / 2f, size.height - t.size.height))
            }
        }
    }
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.secondaryContainer, textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}
