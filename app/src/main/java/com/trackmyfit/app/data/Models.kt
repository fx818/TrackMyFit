package com.trackmyfit.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String = "",
    val isCustom: Boolean = false,
)

@Serializable
data class WorkoutSet(
    val id: String,
    val reps: Int,
    val weightKg: Double,
)

@Serializable
data class ExerciseEntry(
    val id: String,
    val exerciseId: String,
    val sets: List<WorkoutSet> = emptyList(),
    val note: String = "",
)

@Serializable
data class WorkoutDay(
    val date: String, // ISO yyyy-MM-dd
    val labels: List<String> = emptyList(),
    val entries: List<ExerciseEntry> = emptyList(),
    val note: String = "",
) {
    val isEmpty: Boolean get() = labels.isEmpty() && entries.isEmpty() && note.isBlank()
    val hasWorkout: Boolean get() = entries.any { it.sets.isNotEmpty() }
}

@Serializable
data class BodyWeightEntry(val date: String, val weightKg: Double)

@Serializable
enum class WeightUnit(val label: String) { KG("kg"), LB("lb") }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val unit: WeightUnit = WeightUnit.KG,
    val restTimerEnabled: Boolean = true,
    val restSeconds: Int = 90,
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

@Serializable
enum class Sex { UNSPECIFIED, MALE, FEMALE }

@Serializable
data class UserProfile(
    val name: String = "",
    val heightCm: Double = 0.0,
    val goalWeightKg: Double = 0.0,
    val birthYear: Int = 0,
    val sex: Sex = Sex.UNSPECIFIED,
)

@Serializable
data class AppData(
    val version: Int = 1,
    val profile: UserProfile = UserProfile(),
    val customExercises: List<Exercise> = emptyList(),
    val hiddenExerciseIds: Set<String> = emptySet(),
    val days: Map<String, WorkoutDay> = emptyMap(),
    val customLabels: List<String> = emptyList(),
    val bodyWeights: List<BodyWeightEntry> = emptyList(),
    val settings: AppSettings = AppSettings(),
)

object Bmi {
    fun calc(weightKg: Double, heightCm: Double): Double? =
        if (weightKg <= 0 || heightCm <= 0) null else weightKg / ((heightCm / 100.0) * (heightCm / 100.0))

    fun category(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    /** Healthy weight range (BMI 18.5 – 24.9) for a height, in kg. */
    fun healthyRangeKg(heightCm: Double): Pair<Double, Double>? =
        if (heightCm <= 0) null else (18.5 * (heightCm / 100).let { it * it }) to (24.9 * (heightCm / 100).let { it * it })
}

object Units {
    const val LB_PER_KG = 2.2046226218
    fun toDisplay(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) kg else kg * LB_PER_KG
    fun fromDisplay(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) value else value / LB_PER_KG
}
