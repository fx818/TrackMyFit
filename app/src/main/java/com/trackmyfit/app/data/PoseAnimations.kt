package com.trackmyfit.app.data

import android.content.Context
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Keyframed stick-figure animations (assets/poses.json) for exercises without demo photos. */
@Serializable
data class PoseProp(
    val type: String,
    val x1: Float = 0f, val y1: Float = 0f, val x2: Float = 0f, val y2: Float = 0f,
    val x: Float = 0f, val y: Float = 0f, val r: Float = 0f,
    val w: Float = 2f, val h: Float = 0f, val rx: Float = 1f,
    val c: String = "equip",
)

@Serializable
data class PoseKeyframe(val t: Float, val joints: Map<String, List<Float>>, val props: List<PoseProp> = emptyList())

@Serializable
data class PoseAnim(
    val view: String = "side",
    val durationMs: Int = 2400,
    val mode: String = "pingpong",
    val static: List<PoseProp> = emptyList(),
    val keyframes: List<PoseKeyframe> = emptyList(),
)

object PoseRepo {
    @Volatile private var cache: Map<String, PoseAnim>? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun all(context: Context): Map<String, PoseAnim> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val m = try {
                val text = context.assets.open("poses.json").bufferedReader().use { it.readText() }
                json.decodeFromString<Map<String, PoseAnim>>(text).filterValues { it.keyframes.size >= 2 }
            } catch (e: Exception) {
                Log.w("TrackMyFit", "poses.json not available", e); emptyMap()
            }
            cache = m
            return m
        }
    }

    fun get(context: Context, id: String): PoseAnim? = all(context)[id]
}
