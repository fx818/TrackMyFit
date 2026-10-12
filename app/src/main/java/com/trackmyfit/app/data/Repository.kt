package com.trackmyfit.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Stores everything in a single JSON file inside the app's private storage
 * (no database). Writes are atomic (temp file + rename) and the previous
 * version is kept as a .bak file in case the main file is ever corrupted.
 */
class Repository(context: Context) {
    private val dir = context.filesDir
    private val file = File(dir, FILE_NAME)
    private val backupFile = File(dir, "$FILE_NAME.bak")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()

    private val _data = MutableStateFlow(load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private fun load(): AppData {
        for (f in listOf(file, backupFile)) {
            if (!f.exists()) continue
            try {
                return json.decodeFromString(AppData.serializer(), f.readText())
            } catch (e: Exception) {
                Log.e("TrackMyFit", "Failed to read ${f.name}", e)
            }
        }
        return AppData()
    }

    fun update(transform: (AppData) -> AppData) {
        _data.update(transform)
        persist()
    }

    fun replaceAll(newData: AppData) {
        _data.value = newData
        persist()
    }

    private fun persist() {
        scope.launch {
            writeMutex.withLock {
                try {
                    val text = json.encodeToString(AppData.serializer(), _data.value)
                    val tmp = File(dir, "$FILE_NAME.tmp")
                    tmp.writeText(text)
                    if (file.exists()) file.copyTo(backupFile, overwrite = true)
                    if (!tmp.renameTo(file)) {
                        tmp.copyTo(file, overwrite = true)
                        tmp.delete()
                    }
                } catch (e: Exception) {
                    Log.e("TrackMyFit", "Failed to save data", e)
                }
            }
        }
    }

    fun exportJson(): String = prettyJson.encodeToString(AppData.serializer(), _data.value)

    fun parse(text: String): AppData = json.decodeFromString(AppData.serializer(), text)

    companion object {
        const val FILE_NAME = "trackmyfit_data.json"
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
        val prettyJson = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    }
}
