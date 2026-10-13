package com.trackmyfit.app.data

/**
 * Maps exercises to the muscle regions of the body heat-map (assets/body_map.json).
 * Region ids follow react-body-highlighter (MIT).
 */
object Muscles {
    data class Region(val id: String, val label: String)

    val regions = listOf(
        Region("chest", "Chest"),
        Region("front-deltoids", "Front delts"),
        Region("back-deltoids", "Rear delts"),
        Region("biceps", "Biceps"),
        Region("triceps", "Triceps"),
        Region("forearm", "Forearms"),
        Region("abs", "Abs"),
        Region("obliques", "Obliques"),
        Region("trapezius", "Traps"),
        Region("upper-back", "Upper back / lats"),
        Region("lower-back", "Lower back"),
        Region("gluteal", "Glutes"),
        Region("quadriceps", "Quads"),
        Region("hamstring", "Hamstrings"),
        Region("adductor", "Adductors"),
        Region("abductors", "Abductors"),
        Region("calves", "Calves"),
    )
    val labelOf = regions.associate { it.id to it.label }

    /** Body-map regions that are drawn but never trained (or are aliases). */
    val aliases = mapOf("left-soleus" to "calves", "right-soleus" to "calves")
    val neutral = setOf("head", "neck", "knees")

    /** free-exercise-db muscle name -> regions */
    private val fromDb = mapOf(
        "chest" to listOf("chest"),
        "shoulders" to listOf("front-deltoids"),
        "biceps" to listOf("biceps"),
        "triceps" to listOf("triceps"),
        "forearms" to listOf("forearm"),
        "abdominals" to listOf("abs"),
        "obliques" to listOf("obliques"),
        "traps" to listOf("trapezius"),
        "lats" to listOf("upper-back"),
        "middle back" to listOf("upper-back"),
        "lower back" to listOf("lower-back"),
        "glutes" to listOf("gluteal"),
        "quadriceps" to listOf("quadriceps"),
        "hamstrings" to listOf("hamstring"),
        "adductors" to listOf("adductor"),
        "abductors" to listOf("abductors"),
        "calves" to listOf("calves"),
        "neck" to emptyList(),
        "hip flexors" to listOf("abs"),
    )

    /** Fallback by our muscle group. */
    private val fromGroup = mapOf(
        "Chest" to (listOf("chest") to listOf("front-deltoids", "triceps")),
        "Back" to (listOf("upper-back") to listOf("biceps", "back-deltoids", "trapezius")),
        "Shoulders" to (listOf("front-deltoids") to listOf("triceps", "trapezius")),
        "Biceps" to (listOf("biceps") to listOf("forearm")),
        "Triceps" to (listOf("triceps") to emptyList()),
        "Forearms" to (listOf("forearm") to emptyList()),
        "Legs" to (listOf("quadriceps") to listOf("gluteal", "hamstring")),
        "Glutes" to (listOf("gluteal") to listOf("hamstring")),
        "Calves" to (listOf("calves") to emptyList()),
        "Abs" to (listOf("abs") to listOf("obliques")),
        "Cardio" to (emptyList<String>() to listOf("quadriceps", "calves")),
        "Full Body" to (listOf("quadriceps", "gluteal") to listOf("front-deltoids", "upper-back", "abs")),
    )

    /** Rear-delt / side-delt exercises the database labels just "shoulders". */
    private val rearDelt = setOf("b_rear_delt_fly", "b_reverse_pec_deck", "b_face_pull")
    private val sideDeltIds = setOf("b_dumbbell_lateral_raise", "b_cable_lateral_raise", "b_machine_lateral_raise", "b_upright_row")

    private fun map(names: List<String>, exId: String): List<String> = names.flatMap { n ->
        if (n == "shoulders") when (exId) {
            in rearDelt -> listOf("back-deltoids")
            in sideDeltIds -> listOf("front-deltoids", "back-deltoids")
            else -> listOf("front-deltoids")
        } else fromDb[n] ?: emptyList()
    }.distinct()

    /** (primary, secondary) regions for an exercise. */
    fun forExercise(ex: Exercise, info: ExerciseInfo?): Pair<List<String>, List<String>> {
        if (info != null && info.primary.isNotEmpty()) {
            val p = map(info.primary, ex.id)
            val s = map(info.secondary, ex.id) - p.toSet()
            if (p.isNotEmpty()) return p to s
        }
        val (p, s) = fromGroup[ex.muscleGroup] ?: (emptyList<String>() to emptyList())
        return p to s
    }

    /**
     * Weekly training load per region: each working set counts 1 for primary muscles
     * and 0.5 for secondary muscles.
     */
    fun load(
        data: AppData,
        from: java.time.LocalDate,
        to: java.time.LocalDate,
        exMap: Map<String, Exercise>,
        infoOf: (String) -> ExerciseInfo?,
    ): Map<String, Double> {
        val m = HashMap<String, Double>()
        for (d in data.days.values) {
            if (java.time.LocalDate.parse(d.date) !in from..to) continue
            for (e in d.entries) {
                val ex = exMap[e.exerciseId] ?: continue
                val n = e.sets.count { it.isWorking }
                if (n == 0) continue
                val (p, s) = forExercise(ex, infoOf(ex.id))
                p.forEach { m[it] = (m[it] ?: 0.0) + n }
                s.forEach { m[it] = (m[it] ?: 0.0) + n * 0.5 }
            }
        }
        return m
    }
}
