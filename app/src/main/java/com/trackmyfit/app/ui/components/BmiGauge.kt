package com.trackmyfit.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

val BmiBlue = Color(0xFF4FA3E0)
val BmiGreen = Color(0xFF3DBE7A)
val BmiYellow = Color(0xFFF2B33D)
val BmiRed = Color(0xFFE5534B)

fun bmiColor(bmi: Double): Color = when {
    bmi < 18.5 -> BmiBlue
    bmi < 25 -> BmiGreen
    bmi < 30 -> BmiYellow
    else -> BmiRed
}

/** Horizontal BMI scale 15–40 with a marker for the current value. */
@Composable
fun BmiGauge(bmi: Double, modifier: Modifier = Modifier) {
    val lo = 15.0; val hi = 40.0
    val marker = MaterialTheme.colorScheme.onSurface
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val barTop = 10.dp.toPx(); val barH = 10.dp.toPx()
            fun x(v: Double) = (size.width * ((v - lo) / (hi - lo))).toFloat()
            val segs = listOf(lo to 18.5 to BmiBlue, 18.5 to 25.0 to BmiGreen, 25.0 to 30.0 to BmiYellow, 30.0 to hi to BmiRed)
            segs.forEachIndexed { i, (range, c) ->
                val (a, b) = range
                val gap = if (i < segs.lastIndex) 2.dp.toPx() else 0f
                drawRoundRect(c, Offset(x(a), barTop), Size(x(b) - x(a) - gap, barH), CornerRadius(barH / 2))
            }
            val mx = x(bmi.coerceIn(lo, hi))
            val tri = Path().apply {
                moveTo(mx - 6.dp.toPx(), 0f); lineTo(mx + 6.dp.toPx(), 0f); lineTo(mx, barTop - 1.dp.toPx()); close()
            }
            drawPath(tri, marker)
            drawLine(marker, Offset(mx, barTop - 1.dp.toPx()), Offset(mx, barTop + barH + 1.dp.toPx()), strokeWidth = 2.5.dp.toPx())
        }
        Row(Modifier.fillMaxWidth()) {
            val st = MaterialTheme.typography.labelSmall
            val c = MaterialTheme.colorScheme.onSurfaceVariant
            Text("15", style = st, color = c)
            Spacer(Modifier.weight(3.5f / 25f))
            Text("18.5", style = st, color = c)
            Spacer(Modifier.weight(6.5f / 25f))
            Text("25", style = st, color = c)
            Spacer(Modifier.weight(5f / 25f))
            Text("30", style = st, color = c)
            Spacer(Modifier.weight(10f / 25f))
            Text("40", style = st, color = c)
        }
    }
}
