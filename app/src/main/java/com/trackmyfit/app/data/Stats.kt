package com.trackmyfit.app.data

import java.time.DayOfWeek
import java.time.LocalDate

object Stats {
    fun volumeKg(e: ExerciseEntry): Double = e.sets.sumOf { it.reps * it.weightKg }
    fun volumeKg(d: WorkoutDay): Double = d.entries.sumOf { volumeKg(it) }
    fun setCount(d: WorkoutDay): Int = d.entries.sumOf { it.sets.size }

    /** Epley estimated one-rep max. */
    fun e1rm(weight: Double, reps: Int): Double = when {
        reps <= 0 || weight <= 0.0 -> 0.0
        reps == 1 -> weight
        else -> weight * (1 + reps / 30.0)
    }

    data class HistoryPoint(val date: LocalDate, val entry: ExerciseEntry) {
        val maxWeightKg: Double get() = entry.sets.maxOfOrNull { it.weightKg } ?: 0.0
        val bestE1rmKg: Double get() = entry.sets.maxOfOrNull { e1rm(it.weightKg, it.reps) } ?: 0.0
        val volumeKg: Double get() = volumeKg(entry)
        val totalReps: Int get() = entry.sets.sumOf { it.reps }
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
                if (s.weightKg <= 0.0) continue
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

    /** Sets per muscle group between two dates (inclusive). */
    fun muscleSplit(data: AppData, from: LocalDate, to: LocalDate, exMap: Map<String, Exercise>): List<Pair<String, Int>> {
        val m = HashMap<String, Int>()
        for (d in data.days.values) {
            if (LocalDate.parse(d.date) !in from..to) continue
            for (e in d.entries) {
                val g = exMap[e.exerciseId]?.muscleGroup ?: continue
                if (e.sets.isNotEmpty()) m[g] = (m[g] ?: 0) + e.sets.size
            }
        }
        return m.toList().sortedByDescending { it.second }
    }

    /** Exercise ids ordered by how often they were logged with a weight. */
    fun topWeightedExercises(data: AppData, limit: Int): List<String> {
        val m = HashMap<String, Int>()
        for (d in data.days.values) for (e in d.entries) if (e.sets.any { it.weightKg > 0 }) m[e.exerciseId] = (m[e.exerciseId] ?: 0) + 1
        return m.toList().sortedByDescending { it.second }.take(limit).map { it.first }
    }

    fun totalWorkouts(data: AppData): Int = data.days.values.count { it.hasWorkout }

    fun labelCounts(data: AppData): List<Pair<String, Int>> =
        data.days.values.filter { it.hasWorkout }.flatMap { it.labels }
            .groupingBy { it }.eachCount().toList().sortedByDescending { it.second }
}
