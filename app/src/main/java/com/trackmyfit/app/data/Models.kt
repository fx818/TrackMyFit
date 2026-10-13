package com.trackmyfit.app.data

import kotlinx.serialization.Serializable

/** How an exercise is measured. */
@Serializable
enum class TrackType(val label: String) {
    WEIGHT_REPS("Weight & reps"),
    TIME("Time"),
    DISTANCE_TIME("Distance & time"),
}

@Serializable
enum class SetType(val short: String, val label: String) {
    NORMAL("", "Normal"),
    WARMUP("W", "Warm-up"),
    DROP("D", "Drop set"),
    FAILURE("F", "To failure"),
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String = "",
    val isCustom: Boolean = false,
    val trackType: TrackType = TrackType.WEIGHT_REPS,
)

@Serializable
data class WorkoutSet(
    val id: String,
    val reps: Int = 0,
    val weightKg: Double = 0.0,
    val durationSec: Int = 0,
    val distanceKm: Double = 0.0,
    val type: SetType = SetType.NORMAL,
    /** Rate of perceived exertion 1-10, 0 = not recorded. */
    val rpe: Double = 0.0,
) {
    /** Warm-up sets don't count towards volume, PRs or set totals. */
    val isWorking: Boolean get() = type != SetType.WARMUP
}

@Serializable
data class ExerciseEntry(
    val id: String,
    val exerciseId: String,
    val sets: List<WorkoutSet> = emptyList(),
    val note: String = "",
    val targetSets: Int = 0,
    val targetReps: Int = 0,
    val targetWeightKg: Double = 0.0,
    val targetDurationSec: Int = 0,
    /** Entries sharing the same non-empty group id form a superset / circuit. */
    val group: String = "",
)

@Serializable
data class WorkoutDay(
    val date: String, // ISO yyyy-MM-dd
    val labels: List<String> = emptyList(),
    val entries: List<ExerciseEntry> = emptyList(),
    val note: String = "",
    /** Epoch millis when the workout timer was started / finished (0 = not set). */
    val startedAt: Long = 0,
    val finishedAt: Long = 0,
    /** Manually entered duration in minutes; overrides the timer when > 0. */
    val durationMin: Int = 0,
) {
    val isEmpty: Boolean get() = labels.isEmpty() && entries.isEmpty() && note.isBlank() && startedAt == 0L && durationMin == 0
    val hasWorkout: Boolean get() = entries.any { it.sets.isNotEmpty() }
    val isTimerRunning: Boolean get() = startedAt > 0 && finishedAt == 0L && durationMin == 0

    /** Duration in minutes, or null if unknown / still running. */
    val durationMinutes: Int?
        get() = when {
            durationMin > 0 -> durationMin
            startedAt > 0 && finishedAt > startedAt -> ((finishedAt - startedAt) / 60000L).toInt().coerceAtLeast(1)
            else -> null
        }
}

@Serializable
data class RoutineItem(
    val exerciseId: String,
    val sets: Int = 3,
    val reps: Int = 10,
    val weightKg: Double = 0.0,
    val durationSec: Int = 0,
    val group: String = "",
)

@Serializable
data class Routine(
    val id: String,
    val name: String,
    val labels: List<String> = emptyList(),
    val items: List<RoutineItem> = emptyList(),
)

@Serializable
data class BodyWeightEntry(val date: String, val weightKg: Double)

/** Body measurements for one date. Lengths in cm, body fat in %. */
@Serializable
data class BodyMeasurement(val date: String, val values: Map<String, Double> = emptyMap())

@Serializable
data class ProgressPhoto(val id: String, val date: String, val file: String, val note: String = "")

@Serializable
data class StepsSnapshot(val date: String, val steps: Long)

@Serializable
enum class WeightUnit(val label: String, val distLabel: String, val lenLabel: String) {
    KG("kg", "km", "cm"),
    LB("lb", "mi", "in"),
}

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
data class AppSettings(
    val unit: WeightUnit = WeightUnit.KG,
    val restTimerEnabled: Boolean = true,
    val restSeconds: Int = 90,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val weeklyGoal: Int = 4,
    val stepGoal: Int = 8000,
    val remindersEnabled: Boolean = false,
    /** ISO day-of-week numbers, 1 = Monday ... 7 = Sunday. */
    val reminderDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val reminderHour: Int = 18,
    val reminderMinute: Int = 0,
    val barWeightKg: Double = 20.0,
    val askedNotificationPermission: Boolean = false,
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
    val version: Int = 2,
    val profile: UserProfile = UserProfile(),
    val customExercises: List<Exercise> = emptyList(),
    val hiddenExerciseIds: Set<String> = emptySet(),
    val days: Map<String, WorkoutDay> = emptyMap(),
    val customLabels: List<String> = emptyList(),
    val bodyWeights: List<BodyWeightEntry> = emptyList(),
    val measurements: List<BodyMeasurement> = emptyList(),
    val photos: List<ProgressPhoto> = emptyList(),
    val routines: List<Routine> = emptyList(),
    val stepsCache: StepsSnapshot? = null,
    val settings: AppSettings = AppSettings(),
)

object Measures {
    data class Def(val key: String, val label: String, val isPercent: Boolean = false)

    val all = listOf(
        Def("body_fat", "Body fat", isPercent = true),
        Def("waist", "Waist"),
        Def("chest", "Chest"),
        Def("shoulders", "Shoulders"),
        Def("arms", "Arms"),
        Def("forearms", "Forearms"),
        Def("thighs", "Thighs"),
        Def("calves", "Calves"),
        Def("hips", "Hips"),
        Def("neck", "Neck"),
    )
    val byKey = all.associateBy { it.key }
}

object Bmi {
    fun calc(weightKg: Double, heightCm: Double): Double? =
        if (weightKg <= 0 || heightCm <= 0) null else weightKg / ((heightCm / 100.0) * (heightCm / 100.0))

    fun category(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    /** Healthy weight range (BMI 18.5 - 24.9) for a height, in kg. */
    fun healthyRangeKg(heightCm: Double): Pair<Double, Double>? =
        if (heightCm <= 0) null else (18.5 * (heightCm / 100).let { it * it }) to (24.9 * (heightCm / 100).let { it * it })
}

object Units {
    const val LB_PER_KG = 2.2046226218
    const val MI_PER_KM = 0.621371192
    const val IN_PER_CM = 0.3937007874

    fun toDisplay(kg: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) kg else kg * LB_PER_KG
    fun fromDisplay(value: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) value else value / LB_PER_KG

    fun distToDisplay(km: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) km else km * MI_PER_KM
    fun distFromDisplay(v: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) v else v / MI_PER_KM

    fun lenToDisplay(cm: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) cm else cm * IN_PER_CM
    fun lenFromDisplay(v: Double, unit: WeightUnit): Double = if (unit == WeightUnit.KG) v else v / IN_PER_CM
}
