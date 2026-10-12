package com.trackmyfit.app.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trackmyfit.app.TrackMyFitApp
import com.trackmyfit.app.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

fun AppData.allExercises(): List<Exercise> = Catalog.defaultExercises + customExercises
fun AppData.exerciseMap(): Map<String, Exercise> = allExercises().associateBy { it.id }
fun AppData.allLabels(): List<String> = Catalog.defaultLabels + customLabels.filter { c -> Catalog.defaultLabels.none { it.equals(c, true) } }
fun AppData.dayFor(date: LocalDate): WorkoutDay = days[date.toString()] ?: WorkoutDay(date.toString())

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as TrackMyFitApp).repository
    val data: StateFlow<AppData> = repo.data

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Remaining rest-timer seconds, or null when not running. */
    private val _rest = MutableStateFlow<Int?>(null)
    val restRemaining: StateFlow<Int?> = _rest.asStateFlow()
    private val _restTotal = MutableStateFlow(90)
    val restTotal: StateFlow<Int> = _restTotal.asStateFlow()
    private var restJob: Job? = null

    fun setDate(date: LocalDate) { _selectedDate.value = date }
    fun shiftDate(days: Long) { _selectedDate.value = _selectedDate.value.plusDays(days) }

    // ---------- Day / log ----------
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

    fun addSet(date: LocalDate, entryId: String, reps: Int, weightKg: Double) {
        updateEntry(date, entryId) { it.copy(sets = it.sets + WorkoutSet(newId(), reps, weightKg)) }
        val s = data.value.settings
        if (s.restTimerEnabled && date == LocalDate.now()) startRest(s.restSeconds)
    }

    fun updateSet(date: LocalDate, entryId: String, setId: String, reps: Int, weightKg: Double) =
        updateEntry(date, entryId) { e -> e.copy(sets = e.sets.map { if (it.id == setId) it.copy(reps = reps, weightKg = weightKg) else it }) }

    fun deleteSet(date: LocalDate, entryId: String, setId: String) =
        updateEntry(date, entryId) { e -> e.copy(sets = e.sets.filterNot { it.id == setId }) }

    fun setEntryNote(date: LocalDate, entryId: String, note: String) = updateEntry(date, entryId) { it.copy(note = note) }

    fun setDayNote(date: LocalDate, note: String) = updateDay(date) { it.copy(note = note) }

    /** Copies labels and exercises from another day. If [withSets] the sets are copied too (as a template to edit). */
    fun copyWorkout(from: LocalDate, to: LocalDate, withSets: Boolean) {
        val src = data.value.dayFor(from)
        updateDay(to) { day ->
            day.copy(
                labels = (day.labels + src.labels).distinctBy { it.lowercase() },
                entries = day.entries + src.entries.map { e ->
                    ExerciseEntry(newId(), e.exerciseId, if (withSets) e.sets.map { it.copy(id = newId()) } else emptyList())
                },
            )
        }
    }

    fun clearDay(date: LocalDate) {
        val key = date.toString()
        repo.update { it.copy(days = it.days - key) }
    }

    // ---------- Exercises ----------
    fun addCustomExercise(name: String, group: String, equipment: String): String {
        val ex = Exercise(id = "c_" + newId(), name = name.trim(), muscleGroup = group, equipment = equipment, isCustom = true)
        repo.update { it.copy(customExercises = it.customExercises + ex) }
        return ex.id
    }

    fun updateCustomExercise(id: String, name: String, group: String, equipment: String) = repo.update { d ->
        d.copy(customExercises = d.customExercises.map {
            if (it.id == id) it.copy(name = name.trim(), muscleGroup = group, equipment = equipment) else it
        })
    }

    fun deleteCustomExercise(id: String) = repo.update { d ->
        d.copy(
            customExercises = d.customExercises.filterNot { it.id == id },
            days = d.days.mapValues { (_, day) -> day.copy(entries = day.entries.filterNot { it.exerciseId == id }) }
                .filterValues { !it.isEmpty },
        )
    }

    // ---------- Labels ----------
    fun addCustomLabel(label: String) {
        val l = label.trim()
        if (l.isEmpty()) return
        repo.update { d -> if (d.allLabels().any { it.equals(l, true) }) d else d.copy(customLabels = d.customLabels + l) }
    }

    fun removeCustomLabel(label: String) = repo.update { d -> d.copy(customLabels = d.customLabels - label) }

    // ---------- Body weight ----------
    fun logBodyWeight(date: LocalDate, kg: Double) = repo.update { d ->
        d.copy(bodyWeights = (d.bodyWeights.filterNot { it.date == date.toString() } + BodyWeightEntry(date.toString(), kg)).sortedBy { it.date })
    }

    fun deleteBodyWeight(date: String) = repo.update { d -> d.copy(bodyWeights = d.bodyWeights.filterNot { it.date == date }) }

    // ---------- Profile ----------
    fun updateProfile(transform: (UserProfile) -> UserProfile) = repo.update { it.copy(profile = transform(it.profile)) }

    // ---------- Settings ----------
    fun updateSettings(transform: (AppSettings) -> AppSettings) = repo.update { it.copy(settings = transform(it.settings)) }

    fun exportJson(): String = repo.exportJson()

    fun importJson(text: String): Result<Unit> = runCatching { repo.replaceAll(repo.parse(text)) }

    fun eraseAll() = repo.replaceAll(AppData(settings = data.value.settings))

    // ---------- Rest timer ----------
    fun startRest(seconds: Int) {
        restJob?.cancel()
        _restTotal.value = seconds
        _rest.value = seconds
        restJob = viewModelScope.launch {
            while ((_rest.value ?: 0) > 0) {
                delay(1000)
                _rest.value = (_rest.value ?: 0) - 1
            }
            _rest.value = null
            vibrate()
        }
    }

    fun addRest(seconds: Int) {
        val cur = _rest.value ?: return
        _rest.value = (cur + seconds).coerceAtLeast(1)
        _restTotal.value = maxOf(_restTotal.value, _rest.value ?: 0)
    }

    fun stopRest() { restJob?.cancel(); _rest.value = null }

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
