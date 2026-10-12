package com.trackmyfit.app.data

import java.time.DayOfWeek
import java.time.LocalDate

object Stats {
    fun volumeKg(e: ExerciseEntry): Double = e.sets.filter { it.isWorking }.sumOf { it.reps * it.weightKg }
    fun volumeKg(d: WorkoutDay): Double = d.entries.sumOf { volumeKg(it) }
    fun setCount(d: WorkoutDay): Int = d.entries.sumOf { e -> e.sets.count { it.isWorking } }
    fun repCount(d: WorkoutDay): Int = d.entries.sumOf { e -> e.sets.filter { it.isWorking }.sumOf { it.reps } }

    /** Epley estimated one-rep max. */
    fun e1rm(weight: Double, reps: Int): Double = when {
        reps <= 0 || weight <= 0.0 -> 0.0
        reps == 1 -> weight
        else -> weight * (1 + reps / 30.0)
    }

    data class HistoryPoint(val date: LocalDate, val entry: ExerciseEntry) {
        val working: List<WorkoutSet> get() = entry.sets.filter { it.isWorking }
        val maxWeightKg: Double get() = working.maxOfOrNull { it.weightKg } ?: 0.0
        val bestE1rmKg: Double get() = working.maxOfOrNull { e1rm(it.weightKg, it.reps) } ?: 0.0
        val volumeKg: Double get() = volumeKg(entry)
        val totalReps: Int get() = working.sumOf { it.reps }
        val maxReps: Int get() = working.maxOfOrNull { it.reps } ?: 0
        val totalDurationSec: Int get() = working.sumOf { it.durationSec }
        val maxDurationSec: Int get() = working.maxOfOrNull { it.durationSec } ?: 0
        val totalDistanceKm: Double get() = working.sumOf { it.distanceKm }
    }

    fun exerciseHistory(data: AppData, exerciseId: String): List<HistoryPoint> =
        data.days.values.flatMap { d ->
            val date = LocalDate.parse(d.date)
            d.entries.filter { it.exerciseId == exerciseId && it.sets.isNotEmpty() }
                .map { HistoryPoint(date, it) }
        }.sortedBy { it.date }

    fun lastBefore(data: AppData, exerciseId: String, date: LocalDate): HistoryPoint? =
        exerciseHistory(data, exerciseId).lastOrNull { it.date < date }

    fun bestWeightBefore(data: AppData, exerciseId: String, date: LocalDate): Double? =
        exerciseHistory(data, exerciseId).filter { it.date < date }.maxOfOrNull { it.maxWeightKg }

    fun workoutDays(data: AppData): List<WorkoutDay> =
        data.days.values.filter { it.hasWorkout }.sortedByDescending { it.date }

    fun workoutsBetween(data: AppData, from: LocalDate, to: LocalDate): Int =
        data.days.values.count { it.hasWorkout && LocalDate.parse(it.date) in from..to }

    fun workoutsThisWeek(data: AppData, today: LocalDate): Int =
        workoutsBetween(data, today.with(DayOfWeek.MONDAY), today.with(DayOfWeek.SUNDAY))

    /** Consecutive days (ending today or yesterday) that have a workout or a "Rest" label. */
    fun currentStreak(data: AppData, today: LocalDate): Int {
        val active = data.days.values
            .filter { it.hasWorkout || it.labels.any { l -> l.equals("Rest", ignoreCase = true) } }
            .map { LocalDate.parse(it.date) }.toSet()
        var d = if (today in active) today else today.minusDays(1)
        var n = 0
        while (d in active) { n++; d = d.minusDays(1) }
        return n
    }

    fun weeklyCounts(data: AppData, weeks: Int, today: LocalDate): List<Pair<LocalDate, Int>> {
        val start = today.with(DayOfWeek.MONDAY).minusWeeks((weeks - 1).toLong())
        return (0 until weeks).map { i ->
            val ws = start.plusWeeks(i.toLong())
            ws to workoutsBetween(data, ws, ws.plusDays(6))
        }
    }

    data class PR(val exerciseId: String, val weightKg: Double, val reps: Int, val date: LocalDate)

    fun personalRecords(data: AppData): List<PR> {
        val best = HashMap<String, PR>()
        for (d in data.days.values.sortedBy { it.date }) {
            val date = LocalDate.parse(d.date)
            for (e in d.entries) for (s in e.sets) {
                if (s.weightKg <= 0.0 || !s.isWorking || s.reps <= 0) continue
                val cur = best[e.exerciseId]
                if (cur == null || s.weightKg > cur.weightKg || (s.weightKg == cur.weightKg && s.reps > cur.reps)) {
                    best[e.exerciseId] = PR(e.exerciseId, s.weightKg, s.reps, date)
                }
            }
        }
        return best.values.sortedByDescending { it.date }
    }

    fun usageCounts(data: AppData): Map<String, Int> {
        val m = HashMap<String, Int>()
        for (d in data.days.values) for (e in d.entries) if (e.sets.isNotEmpty()) m[e.exerciseId] = (m[e.exerciseId] ?: 0) + 1
        return m
    }

    fun recentExerciseIds(data: AppData, limit: Int): List<String> =
        data.days.values.sortedByDescending { it.date }
            .flatMap { d -> d.entries.map { it.exerciseId } }
            .distinct().take(limit)

    fun weeklyVolumeKg(data: AppData, weeks: Int, today: LocalDate): List<Pair<LocalDate, Double>> {
        val start = today.with(DayOfWeek.MONDAY).minusWeeks((weeks - 1).toLong())
        return (0 until weeks).map { i ->
            val ws = start.plusWeeks(i.toLong()); val we = ws.plusDays(6)
            ws to data.days.values.filter { LocalDate.parse(it.date) in ws..we }.sumOf { volumeKg(it) }
        }
    }

    /** Working sets per muscle group between two dates (inclusive). */
    fun muscleSplit(data: AppData, from: LocalDate, to: LocalDate, exMap: Map<String, Exercise>): List<Pair<String, Int>> {
        val m = HashMap<String, Int>()
        for (d in data.days.values) {
            if (LocalDate.parse(d.date) !in from..to) continue
            for (e in d.entries) {
                val g = exMap[e.exerciseId]?.muscleGroup ?: continue
                val n = e.sets.count { it.isWorking }
                if (n > 0) m[g] = (m[g] ?: 0) + n
            }
        }
        return m.toList().sortedByDescending { it.second }
    }

    /** Exercise ids ordered by how often they were logged with a weight. */
    fun topWeightedExercises(data: AppData, limit: Int): List<String> {
        val m = HashMap<String, Int>()
        for (d in data.days.values) for (e in d.entries) if (e.sets.any { it.isWorking && it.weightKg > 0 }) m[e.exerciseId] = (m[e.exerciseId] ?: 0) + 1
        return m.toList().sortedByDescending { it.second }.take(limit).map { it.first }
    }

    fun totalWorkouts(data: AppData): Int = data.days.values.count { it.hasWorkout }

    fun labelCounts(data: AppData): List<Pair<String, Int>> =
        data.days.values.filter { it.hasWorkout }.flatMap { it.labels }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    // ---------------- Progressive overload suggestion ----------------

    enum class SuggestKind { INCREASE_WEIGHT, MATCH_REPS, ADD_REPS }

    data class Suggestion(
        val kind: SuggestKind,
        val weightKg: Double,
        val reps: Int,
        val sets: Int,
        val last: HistoryPoint,
    )

    /** Weight jump appropriate for the exercise (in kg). */
    fun increment(ex: Exercise, unit: WeightUnit): Double {
        val big = ex.muscleGroup in setOf("Legs", "Glutes", "Back") && ex.equipment in setOf("Barbell", "Machine", "Smith Machine")
        return if (unit == WeightUnit.KG) when {
            big -> 5.0
            ex.equipment == "Dumbbell" -> 2.0
            ex.equipment == "Kettlebell" -> 4.0
            else -> 2.5
        } else Units.fromDisplay(if (big) 10.0 else 5.0, unit)
    }

    /**
     * Simple double-progression rule:
     * - hit the target reps on every working set at the top weight last time -> add weight;
     * - otherwise keep the weight and aim to hit the target reps on all sets;
     * - bodyweight-only exercises -> add a rep.
     */
    fun suggest(data: AppData, ex: Exercise?, entry: ExerciseEntry, date: LocalDate, unit: WeightUnit): Suggestion? {
        if (ex == null || ex.trackType != TrackType.WEIGHT_REPS) return null
        val last = lastBefore(data, ex.id, date) ?: return null
        val working = last.entry.sets.filter { it.isWorking && it.reps > 0 }
        if (working.isEmpty()) return null
        val sets = if (entry.targetSets > 0) entry.targetSets else working.size
        val top = working.maxOf { it.weightKg }
        if (top <= 0.0) {
            return Suggestion(SuggestKind.ADD_REPS, 0.0, working.maxOf { it.reps } + 1, sets, last)
        }
        val atTop = working.filter { it.weightKg == top }
        val target = if (entry.targetReps > 0) entry.targetReps else atTop.maxOf { it.reps }
        return if (atTop.all { it.reps >= target }) {
            Suggestion(SuggestKind.INCREASE_WEIGHT, top + increment(ex, unit), target, sets, last)
        } else {
            Suggestion(SuggestKind.MATCH_REPS, top, target, sets, last)
        }
    }
}
