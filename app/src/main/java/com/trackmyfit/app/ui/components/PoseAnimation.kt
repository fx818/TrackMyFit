package com.trackmyfit.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.trackmyfit.app.data.PoseAnim
import com.trackmyfit.app.data.PoseProp

/*
 * Kotlin port of the reference renderer (render.py) — same draw order, sizes and easing.
 */
private val BODY = Color(0xFFFF7A2F)
private val PROP_COLORS = mapOf(
    "equip" to Color(0xFF8A90A6), "plate" to Color(0xFF3A3F55),
    "accent" to Color(0xFFFFB547), "cable" to Color(0xFFB0B5C8),
)
private const val HOLD = 250

private fun ease(u: Float) = u * u * (3 - 2 * u)
private fun lerp(a: Float, b: Float, s: Float) = a + (b - a) * s

private fun lerpProp(p: PoseProp, q: PoseProp, s: Float) = p.copy(
    x1 = lerp(p.x1, q.x1, s), y1 = lerp(p.y1, q.y1, s), x2 = lerp(p.x2, q.x2, s), y2 = lerp(p.y2, q.y2, s),
    x = lerp(p.x, q.x, s), y = lerp(p.y, q.y, s), r = lerp(p.r, q.r, s),
    w = lerp(p.w, q.w, s), h = lerp(p.h, q.h, s),
)

private fun frameAt(anim: PoseAnim, phase: Float): Pair<Map<String, Offset>, List<PoseProp>> {
    val kfs = anim.keyframes
    fun pts(m: Map<String, List<Float>>) = m.mapValues { Offset(it.value[0], it.value[1]) }
    if (phase <= kfs.first().t) return pts(kfs.first().joints) to kfs.first().props
    for (i in 0 until kfs.size - 1) {
        val a = kfs[i]; val b = kfs[i + 1]
        if (phase >= a.t && phase <= b.t) {
            val u = if (b.t == a.t) 0f else (phase - a.t) / (b.t - a.t)
            val s = ease(u)
            val j = a.joints.mapValues { (k, v) ->
                val w = b.joints[k] ?: v
                Offset(lerp(v[0], w[0], s), lerp(v[1], w[1], s))
            }
            val props = a.props.zip(b.props).map { (p, q) -> lerpProp(p, q, s) }
            return j to props
        }
    }
    return pts(kfs.last().joints) to kfs.last().props
}

private fun phaseAt(anim: PoseAnim, ms: Long): Float {
    val d = anim.durationMs.coerceAtLeast(300)
    if (anim.mode == "loop") return (ms % d).toFloat() / d
    val cycle = 2 * d + 2 * HOLD
    var m = (ms % cycle).toInt()
    if (m < HOLD) return 0f
    m -= HOLD
    if (m < d) return m.toFloat() / d
    m -= d
    if (m < HOLD) return 1f
    m -= HOLD
    return 1f - m.toFloat() / d
}

private fun DrawScope.drawPose(anim: PoseAnim, phase: Float, bg: Color) {
    val k = size.minDimension / 100f
    val ox = (size.width - 100f * k) / 2f
    val oy = (size.height - 100f * k) / 2f
    fun p(x: Float, y: Float) = Offset(ox + x * k, oy + y * k)
    fun seg(a: Offset, b: Offset, w: Float, c: Color) =
        drawLine(c, p(a.x, a.y), p(b.x, b.y), strokeWidth = w * k, cap = StrokeCap.Round)
    fun prop(pp: PoseProp) {
        val c = PROP_COLORS[pp.c] ?: PROP_COLORS["equip"]!!
        when (pp.type) {
            "line" -> seg(Offset(pp.x1, pp.y1), Offset(pp.x2, pp.y2), pp.w, c)
            "circle" -> drawCircle(c, pp.r * k, p(pp.x, pp.y))
            "rect" -> drawRoundRect(c, p(pp.x, pp.y), Size(pp.w * k, pp.h * k), CornerRadius(pp.rx * k))
        }
    }
    drawRect(bg)
    seg(Offset(2f, 96.6f), Offset(98f, 96.6f), 0.6f, Color(0xFFC8C8D2))
    anim.static.forEach { prop(it) }
    val (j, props) = frameAt(anim, phase)
    fun J(n: String) = j[n] ?: Offset.Zero
    val far = BODY.copy(alpha = 0.55f)
    listOf("hip_f" to "kn_f", "kn_f" to "an_f", "an_f" to "toe_f", "sh_f" to "el_f", "el_f" to "wr_f").forEach { (a, b) -> seg(J(a), J(b), 4.2f, far) }
    seg(J("neck"), J("pelvis"), 7f, BODY)
    seg(J("sh_n"), J("sh_f"), 5f, BODY)
    seg(J("hip_n"), J("hip_f"), 5f, BODY)
    drawCircle(BODY, 5f * k, p(J("head").x, J("head").y))
    listOf("hip_n" to "kn_n", "kn_n" to "an_n", "an_n" to "toe_n").forEach { (a, b) -> seg(J(a), J(b), 4.2f, BODY) }
    props.forEach { prop(it) }
    listOf("sh_n" to "el_n", "el_n" to "wr_n").forEach { (a, b) -> seg(J(a), J(b), 4.2f, BODY) }
}

@Composable
fun PoseAnimation(anim: PoseAnim, modifier: Modifier = Modifier, showControls: Boolean = true) {
    var playing by remember { mutableStateOf(true) }
    var clock by remember { mutableLongStateOf(0L) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        val start = withFrameMillis { it } - clock
        while (true) { withFrameMillis { clock = it - start } }
    }
    val bg = Color(0xFFF5F5FA)
    Box(
        modifier.fillMaxWidth().aspectRatio(850f / 567f).clip(RoundedCornerShape(16.dp)).background(bg)
            .clickable { playing = !playing },
    ) {
        Canvas(Modifier.fillMaxSize()) { drawPose(anim, phaseAt(anim, clock), bg) }
        if (showControls) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Animated guide", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
