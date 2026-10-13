package com.trackmyfit.app.ui.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trackmyfit.app.data.Muscles
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BodyPart(val m: String, val p: List<List<Float>>)

@Serializable
private data class BodyMap(val front: List<BodyPart>, val back: List<BodyPart>)

private object BodyMapRepo {
    @Volatile private var map: BodyMap? = null
    fun get(ctx: Context): BodyMap? {
        map?.let { return it }
        return runCatching {
            Json { ignoreUnknownKeys = true }.decodeFromString<BodyMap>(ctx.assets.open("body_map.json").bufferedReader().use { it.readText() })
        }.getOrNull()?.also { map = it }
    }
}

/** Colour scale used by the heat-map (0 = untrained ... 1 = most trained). */
val HeatScale = listOf(Color(0xFFFFD9B8), Color(0xFFFFA45C), Color(0xFFFF7A2F), Color(0xFFD9480F))

fun heatColor(v: Float, untrained: Color): Color {
    if (v <= 0f) return untrained
    val x = v.coerceIn(0f, 1f) * (HeatScale.size - 1)
    val i = x.toInt().coerceAtMost(HeatScale.size - 2)
    return lerp(HeatScale[i], HeatScale[i + 1], x - i)
}

/**
 * Front + back body outline; each muscle region coloured by [intensity] (0..1).
 * Body-map shapes from react-body-highlighter (MIT, © 2020 GV79).
 */
@Composable
fun BodyHeatmap(intensity: Map<String, Float>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 260.dp) {
    val ctx = LocalContext.current
    val map = remember { BodyMapRepo.get(ctx) } ?: return
    val untrained = MaterialTheme.colorScheme.surfaceContainerHighest
    val neutral = MaterialTheme.colorScheme.outlineVariant
    val outline = MaterialTheme.colorScheme.surface
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf("Front" to map.front, "Back" to map.back).forEach { (label, parts) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.height(height).aspectRatio(100f / 220f)) {
                    val k = size.height / 220f
                    val ox = (size.width - 100f * k) / 2f
                    for (part in parts) {
                        val region = Muscles.aliases[part.m] ?: part.m
                        val color = if (region in Muscles.neutral) neutral else heatColor(intensity[region] ?: 0f, untrained)
                        for (poly in part.p) {
                            if (poly.size < 6) continue
                            val path = Path().apply {
                                moveTo(ox + poly[0] * k, poly[1] * k)
                                var i = 2
                                while (i + 1 < poly.size) { lineTo(ox + poly[i] * k, poly[i + 1] * k); i += 2 }
                                close()
                            }
                            drawPath(path, color)
                            drawPath(path, outline, style = Stroke(width = 1.2f))
                        }
                    }
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun HeatLegend(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Canvas(Modifier.width(120.dp).height(10.dp)) {
            val n = 30
            for (i in 0 until n) {
                drawRect(heatColor((i + 1f) / n, Color.Gray), topLeft = Offset(size.width * i / n, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width / n + 1, size.height))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
