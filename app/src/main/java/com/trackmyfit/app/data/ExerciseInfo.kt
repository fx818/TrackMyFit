package com.trackmyfit.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Form instructions + demo images bundled in assets/ (from the public-domain free-exercise-db). */
@Serializable
data class ExerciseInfo(
    val images: Int = 0,
    val instructions: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val primary: List<String> = emptyList(),
    val secondary: List<String> = emptyList(),
    val level: String = "",
    val source: String = "",
)

object ExerciseInfoRepo {
    private val json = Json { ignoreUnknownKeys = true }
    @Volatile private var cache: Map<String, ExerciseInfo>? = null
    private val bitmaps = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun all(context: Context): Map<String, ExerciseInfo> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val m = try {
                val text = context.assets.open("exercise_info.json").bufferedReader().use { it.readText() }
                json.decodeFromString<Map<String, ExerciseInfo>>(text)
            } catch (e: Exception) {
                Log.e("TrackMyFit", "exercise_info.json missing", e); emptyMap()
            }
            cache = m
            return m
        }
    }

    fun get(context: Context, exerciseId: String): ExerciseInfo? = all(context)[exerciseId]

    /** Loads demo frame [index] for an exercise; [maxWidth] downsamples for thumbnails. */
    suspend fun frame(context: Context, exerciseId: String, index: Int, maxWidth: Int = 480): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$exerciseId#$index#$maxWidth"
        bitmaps.get(key)?.let { return@withContext it }
        try {
            val path = "demos/${exerciseId}_$index.webp"
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(path).use { BitmapFactory.decodeStream(it, null, opts) }
            var sample = 1
            while (opts.outWidth / (sample * 2) >= maxWidth) sample *= 2
            val bmp = context.assets.open(path).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
            if (bmp != null) bitmaps.put(key, bmp)
            bmp
        } catch (e: Exception) {
            null
        }
    }
}
