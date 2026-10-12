package com.trackmyfit.app.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trackmyfit.app.TrackMyFitApp
import com.trackmyfit.app.data.*
import com.trackmyfit.app.health.StepsRepository
import com.trackmyfit.app.notify.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.math.ceil

fun newId(): String = UUID.randomUUID().toString()

fun AppData.allExercises(): List<Exercise> = Catalog.defaultExercises + customExercises
fun AppData.exerciseMap(): Map<String, Exercise> = allExercises().associateBy { it.id }
fun AppData.allLabels(): List<String> = Catalog.defaultLabels + customLabels.filter { c -> Catalog.defaultLabels.none { it.equals(c, true) } }
fun AppData.dayFor(date: LocalDate): WorkoutDay = days[date.toString()] ?: WorkoutDay(date.toString())

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tmfApp = app as TrackMyFitApp
    private val repo = tmfApp.repository
    val data: StateFlow<AppData> = repo.data
    val steps = StepsRepository(app)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** One-shot navigation request coming from a notification / widget ("log", "weight"). */
    private val _navRequest = MutableStateFlow<String?>(null)
    val navRequest: StateFlow<String?> = _navRequest.asStateFlow()
    fun requestNav(tab: String?) { _navRequest.value = tab }
    fun consumeNav() { _navRequest.value = null }

    // ---------------- Rest timer ----------------
    private val _rest = MutableStateFlow<Int?>(null)
    val restRemaining: StateFlow<Int?> = _rest.asStateFlow()
    private val _restTotal = MutableStateFlow(90)
    val restTotal: StateFlow<Int> = _restTotal.asStateFlow()
    private var restJob: Job? = null
    private var restEndAt = 0L
    private var restLabel: String? = null

    fun setDate(date: LocalDate) { _selectedDate.value = date }
    fun shiftDate(days: Long) { _selectedDate.value = _selectedDate.value.plusDays(days) }

    // ---------------- Day / log ----------------
    private fun updateDay(date: LocalDate, transform: (WorkoutDay) -> WorkoutDay) {
        val key = date.toString()
        repo.update { d ->
            val newDay = transform(d.days[key] ?: WorkoutDay(key))
            d.copy(days = if (newDay.isEmpty) d.days - key else d.days + (key to newDay))
        }
    }

    private fun updateEntry(date: LocalDate, entryId: String, transform: (ExerciseEntry) -> ExerciseEntry) =
        updateDay(date) { day -> day.copy(entries = day.entries.map { if (it.id == entryId) transform(it) else it }) }

    fun toggleLabel(date: LocalDate, label: String) = updateDay(date) { day ->
        val has = day.labels.any { it.equals(label, true) }
        day.copy(labels = if (has) day.labels.filterNot { it.equals(label, true) } else day.labels + label)
    }

    fun addExercises(date: LocalDate, exerciseIds: List<String>) = updateDay(date) { day ->
        day.copy(entries = day.entries + exerciseIds.map { ExerciseEntry(id = newId(), exerciseId = it) })
    }

    fun removeEntry(date: LocalDate, entryId: String) = updateDay(date) { day ->
        day.copy(entries = day.entries.filterNot { it.id == entryId })
    }

    fun moveEntry(date: LocalDate, entryId: String, up: Boolean) = updateDay(date) { day ->
        val list = day.entries.toMutableList()
        val i = list.indexOfFirst { it.id == entryId }
        val j = if (up) i - 1 else i + 1
        if (i >= 0 && j in list.indices) { val t = list[i]; list[i] = list[j]; list[j] = t }
        day.copy(entries = list)
    }

    /** Adds [count] identical sets (e.g. 3 sets x 10 reps @ 60 kg). */
    fun addSets(date: LocalDate, entryId: String, template: WorkoutSet, count: Int, exerciseName: String?) {
        val n = count.coerceIn(1, 20)
        updateEntry(date, entryId) { e -> e.copy(sets = e.sets + List(n) { template.copy(id = newId()) }) }
        val s = data.value.settings
        if (s.restTimerEnabled && date == LocalDate.now() && n == 1) startRest(s.restSeconds, exerciseName)
        if (date == LocalDate.now()) autoStartWorkoutTimer(date)
    }

    fun updateSet(date: LocalDate, entryId: String, set: WorkoutSet) =
        updateEntry(date, entryId) { e -> e.copy(sets = e.sets.map { if (it.id == set.id) set else it }) }

    fun deleteSet(date: LocalDate, entryId: String, setId: String) =
        updateEntry(date, entryId) { e -> e.copy(sets = e.sets.filterNot { it.id == setId }) }

    fun setEntryNote(date: LocalDate, entryId: String, note: String) = updateEntry(date, entryId) { it.copy(note = note) }

    fun setEntryTarget(date: LocalDate, entryId: String, sets: Int, reps: Int, weightKg: Double, durationSec: Int) =
        updateEntry(date, entryId) { it.copy(targetSets = sets, targetReps = reps, targetWeightKg = weightKg, targetDurationSec = durationSec) }

    fun setDayNote(date: LocalDate, note: String) = updateDay(date) { it.copy(note = note) }

    fun copyWorkout(from: LocalDate, to: LocalDate, withSets: Boolean) {
        val src = data.value.dayFor(from)
        updateDay(to) { day ->
            day.copy(
                labels = (day.labels + src.labels).distinctBy { it.lowercase() },
                entries = day.entries + src.entries.map { e ->
                    val working = e.sets.filter { it.isWorking }
                    ExerciseEntry(
                        newId(), e.exerciseId,
                        sets = if (withSets) e.sets.map { it.copy(id = newId()) } else emptyList(),
                        targetSets = working.size,
                        targetReps = working.maxOfOrNull { it.reps } ?: 0,
                        targetWeightKg = working.maxOfOrNull { it.weightKg } ?: 0.0,
                        targetDurationSec = working.maxOfOrNull { it.durationSec } ?: 0,
                    )
                },
            )
        }
    }

    fun clearDay(date: LocalDate) {
        val key = date.toString()
        repo.update { it.copy(days = it.days - key) }
    }

    // ---------------- Workout duration ----------------
    fun startWorkout(date: LocalDate) = updateDay(date) { it.copy(startedAt = System.currentTimeMillis(), finishedAt = 0, durationMin = 0) }
    fun finishWorkout(date: LocalDate) = updateDay(date) { if (it.startedAt > 0) it.copy(finishedAt = System.currentTimeMillis()) else it }
    fun resumeWorkout(date: LocalDate) = updateDay(date) { it.copy(finishedAt = 0, durationMin = 0) }
    fun setDuration(date: LocalDate, minutes: Int) = updateDay(date) { it.copy(durationMin = minutes.coerceAtLeast(0)) }
    fun resetDuration(date: LocalDate) = updateDay(date) { it.copy(startedAt = 0, finishedAt = 0, durationMin = 0) }

    private fun autoStartWorkoutTimer(date: LocalDate) {
        val d = data.value.dayFor(date)
        if (d.startedAt == 0L && d.durationMin == 0) updateDay(date) { it.copy(startedAt = System.currentTimeMillis()) }
    }

    // ---------------- Routines ----------------
    fun saveRoutine(r: Routine) = repo.update { d ->
        d.copy(routines = if (d.routines.any { it.id == r.id }) d.routines.map { if (it.id == r.id) r else it } else d.routines + r)
    }

    fun deleteRoutine(id: String) = repo.update { d -> d.copy(routines = d.routines.filterNot { it.id == id }) }

    fun startRoutine(r: Routine, date: LocalDate) = updateDay(date) { day ->
        day.copy(
            labels = (day.labels + r.labels).distinctBy { it.lowercase() },
            entries = day.entries + r.items.map {
                ExerciseEntry(newId(), it.exerciseId, targetSets = it.sets, targetReps = it.reps, targetWeightKg = it.weightKg, targetDurationSec = it.durationSec)
            },
        )
    }

    fun routineFromDay(date: LocalDate, name: String): Routine {
        val day = data.value.dayFor(date)
        val r = Routine(
            id = newId(), name = name.trim(), labels = day.labels,
            items = day.entries.map { e ->
                val w = e.sets.filter { it.isWorking }
                RoutineItem(
                    e.exerciseId,
                    sets = w.size.takeIf { it > 0 } ?: e.targetSets.takeIf { it > 0 } ?: 3,
                    reps = w.maxOfOrNull { it.reps }?.takeIf { it > 0 } ?: e.targetReps.takeIf { it > 0 } ?: 10,
                    weightKg = w.maxOfOrNull { it.weightKg } ?: e.targetWeightKg,
                    durationSec = w.maxOfOrNull { it.durationSec } ?: e.targetDurationSec,
                )
            },
        )
        saveRoutine(r)
        return r
    }

    // ---------------- Exercises ----------------
    fun addCustomExercise(name: String, group: String, equipment: String, trackType: TrackType = TrackType.WEIGHT_REPS): String {
        val ex = Exercise(id = "c_" + newId(), name = name.trim(), muscleGroup = group, equipment = equipment, isCustom = true, trackType = trackType)
        repo.update { it.copy(customExercises = it.customExercises + ex) }
        return ex.id
    }

    fun updateCustomExercise(id: String, name: String, group: String, equipment: String, trackType: TrackType) = repo.update { d ->
        d.copy(customExercises = d.customExercises.map {
            if (it.id == id) it.copy(name = name.trim(), muscleGroup = group, equipment = equipment, trackType = trackType) else it
        })
    }

    fun deleteCustomExercise(id: String) = repo.update { d ->
        d.copy(
            customExercises = d.customExercises.filterNot { it.id == id },
            days = d.days.mapValues { (_, day) -> day.copy(entries = day.entries.filterNot { it.exerciseId == id }) }
                .filterValues { !it.isEmpty },
            routines = d.routines.map { r -> r.copy(items = r.items.filterNot { it.exerciseId == id }) },
        )
    }

    // ---------------- Labels ----------------
    fun addCustomLabel(label: String) {
        val l = label.trim()
        if (l.isEmpty()) return
        repo.update { d -> if (d.allLabels().any { it.equals(l, true) }) d else d.copy(customLabels = d.customLabels + l) }
    }

    fun removeCustomLabel(label: String) = repo.update { d -> d.copy(customLabels = d.customLabels - label) }

    // ---------------- Body ----------------
    fun logBodyWeight(date: LocalDate, kg: Double) = repo.update { d ->
        d.copy(bodyWeights = (d.bodyWeights.filterNot { it.date == date.toString() } + BodyWeightEntry(date.toString(), kg)).sortedBy { it.date })
    }

    fun deleteBodyWeight(date: String) = repo.update { d -> d.copy(bodyWeights = d.bodyWeights.filterNot { it.date == date }) }

    fun updateProfile(transform: (UserProfile) -> UserProfile) = repo.update { it.copy(profile = transform(it.profile)) }

    fun logMeasurements(date: LocalDate, values: Map<String, Double>) = repo.update { d ->
        val others = d.measurements.filterNot { it.date == date.toString() }
        val clean = values.filterValues { it > 0 }
        d.copy(measurements = (if (clean.isEmpty()) others else others + BodyMeasurement(date.toString(), clean)).sortedBy { it.date })
    }

    fun deleteMeasurement(date: String) = repo.update { d -> d.copy(measurements = d.measurements.filterNot { it.date == date }) }

    // ---------------- Progress photos ----------------
    private val photoDir: File get() = File(getApplication<Application>().filesDir, "photos").apply { mkdirs() }
    fun photoFile(p: ProgressPhoto): File = File(photoDir, p.file)

    /** Copies + downsizes a picked / captured image into private storage. */
    fun addPhoto(uri: Uri, date: LocalDate, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val ctx = getApplication<Application>()
                    val bmp = decodeScaled(ctx, uri, 1600) ?: return@runCatching false
                    val name = "p_${System.currentTimeMillis()}.jpg"
                    File(photoDir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    repo.update { d -> d.copy(photos = (d.photos + ProgressPhoto(newId(), date.toString(), name)).sortedBy { it.date }) }
                    true
                }.getOrDefault(false)
            }
            onDone(ok)
        }
    }

    fun deletePhoto(p: ProgressPhoto) {
        runCatching { photoFile(p).delete() }
        repo.update { d -> d.copy(photos = d.photos.filterNot { it.id == p.id }) }
    }

    fun setPhotoNote(p: ProgressPhoto, note: String) = repo.update { d -> d.copy(photos = d.photos.map { if (it.id == p.id) it.copy(note = note) else it }) }

    private fun decodeScaled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rotation = cr.openInputStream(uri)?.use {
            when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        if (rotation == 0f && scale >= 1f) return bmp
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    // ---------------- Steps (Health Connect) ----------------
    data class StepsState(
        val availability: StepsRepository.Availability = StepsRepository.Availability.UNSUPPORTED,
        val permitted: Boolean = false,
        val today: Long? = null,
        val history: List<Pair<LocalDate, Long>> = emptyList(),
        val loading: Boolean = false,
    )

    private val _steps = MutableStateFlow(StepsState())
    val stepsState: StateFlow<StepsState> = _steps.asStateFlow()

    fun refreshSteps() {
        viewModelScope.launch {
            val avail = steps.availability()
            if (avail != StepsRepository.Availability.AVAILABLE) {
                _steps.value = StepsState(availability = avail); return@launch
            }
            _steps.value = _steps.value.copy(availability = avail, loading = true)
            val permitted = steps.hasPermission()
            if (!permitted) {
                _steps.value = StepsState(availability = avail, permitted = false); return@launch
            }
            val hist = steps.dailySteps(30) ?: emptyList()
            val today = hist.lastOrNull()?.second ?: steps.stepsOn(LocalDate.now())
            _steps.value = StepsState(avail, true, today, hist, false)
            if (today != null) {
                val snap = StepsSnapshot(LocalDate.now().toString(), today)
                if (data.value.stepsCache != snap) repo.update { it.copy(stepsCache = snap) }
            }
        }
    }

    // ---------------- Settings ----------------
    fun updateSettings(transform: (AppSettings) -> AppSettings) = repo.update { it.copy(settings = transform(it.settings)) }

    fun exportJson(): String = repo.exportJson()

    fun importJson(text: String): Result<Unit> = runCatching { repo.replaceAll(repo.parse(text)) }

    fun eraseAll() {
        runCatching { photoDir.listFiles()?.forEach { it.delete() } }
        repo.replaceAll(AppData(settings = data.value.settings))
    }

    // ---------------- Rest timer ----------------
    fun startRest(seconds: Int, label: String? = null) {
        restJob?.cancel()
        restLabel = label
        restEndAt = System.currentTimeMillis() + seconds * 1000L
        _restTotal.value = seconds
        _rest.value = seconds
        Notifier.showRestCountdown(getApplication(), restEndAt, label)
        restJob = viewModelScope.launch {
            while (true) {
                val left = ceil((restEndAt - System.currentTimeMillis()) / 1000.0).toInt()
                if (left <= 0) break
                _rest.value = left
                delay(250)
            }
            _rest.value = null
            val ctx = getApplication<Application>()
            if (tmfApp.inForeground) {
                Notifier.cancelRest(ctx)
                vibrate()
            } else {
                Notifier.showRestDone(ctx)
            }
        }
    }

    fun addRest(seconds: Int) {
        if (_rest.value == null) return
        restEndAt = (restEndAt + seconds * 1000L).coerceAtLeast(System.currentTimeMillis() + 1000)
        val left = ceil((restEndAt - System.currentTimeMillis()) / 1000.0).toInt()
        _rest.value = left
        _restTotal.value = maxOf(_restTotal.value, left)
        Notifier.showRestCountdown(getApplication(), restEndAt, restLabel)
    }

    fun stopRest() {
        restJob?.cancel(); _rest.value = null
        Notifier.cancelRest(getApplication())
    }

    private fun vibrate() {
        val ctx = getApplication<Application>()
        val vib: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250, 150, 400), -1))
    }
}
