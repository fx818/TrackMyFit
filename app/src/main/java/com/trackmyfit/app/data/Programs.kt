package com.trackmyfit.app.data

/** Ready-made training programs. Each program is a set of routines you can add in one tap. */
object Programs {
    data class Program(
        val id: String,
        val name: String,
        val level: String,
        val daysPerWeek: String,
        val description: String,
        val schedule: String,
        val routines: List<Routine>,
    )

    private fun i(id: String, sets: Int, reps: Int, dur: Int = 0, group: String = "") =
        RoutineItem("b_$id", sets, reps, 0.0, dur, group)

    private fun r(id: String, name: String, labels: List<String>, vararg items: RoutineItem) =
        Routine("prog_$id", name, labels, items.toList())

    val all = listOf(
        Program(
            "fb_beginner", "Full Body — Beginner", "Beginner", "3 days/week",
            "Simple whole-body sessions alternating A and B. Ideal for your first 3–6 months. Add a little weight whenever you hit all reps.",
            "Mon: A · Wed: B · Fri: A, then next week B · A · B",
            listOf(
                r("fb_a", "Full Body A", listOf("Full Body"),
                    i("back_squat", 3, 8), i("barbell_bench_press", 3, 8), i("barbell_row", 3, 8),
                    i("overhead_press", 2, 10), i("plank", 3, 0, dur = 45)),
                r("fb_b", "Full Body B", listOf("Full Body"),
                    i("romanian_deadlift", 3, 8), i("incline_dumbbell_press", 3, 10), i("lat_pulldown", 3, 10),
                    i("leg_press", 2, 12), i("dumbbell_lateral_raise", 2, 12), i("hanging_knee_raise", 3, 10)),
            ),
        ),
        Program(
            "upper_lower", "Upper / Lower", "Intermediate", "4 days/week",
            "Each muscle trained twice a week with a mix of heavy and moderate work. Great balance of strength and size.",
            "Mon: Upper A · Tue: Lower A · Thu: Upper B · Fri: Lower B",
            listOf(
                r("ul_ua", "Upper A", listOf("Upper Body"),
                    i("barbell_bench_press", 4, 6), i("barbell_row", 4, 6), i("seated_dumbbell_shoulder_press", 3, 10),
                    i("lat_pulldown", 3, 10), i("ez_bar_curl", 2, 12, group = "a"), i("tricep_rope_pushdown", 2, 12, group = "a")),
                r("ul_la", "Lower A", listOf("Lower Body"),
                    i("back_squat", 4, 6), i("romanian_deadlift", 3, 8), i("leg_press", 3, 10),
                    i("lying_leg_curl", 3, 12), i("standing_calf_raise", 4, 12)),
                r("ul_ub", "Upper B", listOf("Upper Body"),
                    i("overhead_press", 4, 6), i("pull_up", 4, 8), i("incline_dumbbell_press", 3, 10),
                    i("seated_cable_row", 3, 10), i("dumbbell_lateral_raise", 3, 15, group = "a"), i("face_pull", 3, 15, group = "a")),
                r("ul_lb", "Lower B", listOf("Lower Body"),
                    i("deadlift", 3, 5), i("bulgarian_split_squat", 3, 10), i("leg_extension", 3, 12),
                    i("seated_leg_curl", 3, 12), i("hip_thrust", 3, 10), i("seated_calf_raise", 3, 15)),
            ),
        ),
        Program(
            "ppl", "Push / Pull / Legs", "Intermediate", "3 or 6 days/week",
            "The classic split. Run it 3×/week for balance, or 6×/week (twice through) for maximum volume.",
            "Push · Pull · Legs · rest · repeat",
            listOf(
                r("ppl_push", "Push", listOf("Push"),
                    i("barbell_bench_press", 4, 8), i("overhead_press", 3, 8), i("incline_dumbbell_press", 3, 10),
                    i("dumbbell_lateral_raise", 3, 15), i("tricep_rope_pushdown", 3, 12, group = "a"), i("overhead_cable_tricep_extension", 3, 12, group = "a")),
                r("ppl_pull", "Pull", listOf("Pull"),
                    i("deadlift", 3, 5), i("pull_up", 3, 8), i("barbell_row", 3, 8),
                    i("face_pull", 3, 15), i("barbell_curl", 3, 10, group = "a"), i("hammer_curl", 3, 12, group = "a")),
                r("ppl_legs", "Legs", listOf("Legs"),
                    i("back_squat", 4, 8), i("romanian_deadlift", 3, 10), i("leg_press", 3, 12),
                    i("leg_extension", 3, 12, group = "a"), i("lying_leg_curl", 3, 12, group = "a"), i("standing_calf_raise", 4, 12)),
            ),
        ),
        Program(
            "bro_split", "Bro Split", "Intermediate", "5 days/week",
            "One muscle group per day with lots of volume. Popular for bodybuilding; each muscle is hit hard once a week.",
            "Mon: Chest · Tue: Back · Wed: Shoulders · Thu: Legs · Fri: Arms",
            listOf(
                r("bro_chest", "Chest Day", listOf("Chest"),
                    i("barbell_bench_press", 4, 8), i("incline_dumbbell_press", 3, 10), i("chest_dips", 3, 10),
                    i("cable_crossover", 3, 15), i("pec_deck_fly", 2, 15)),
                r("bro_back", "Back Day", listOf("Back"),
                    i("deadlift", 3, 5), i("pull_up", 3, 8), i("t_bar_row", 3, 10),
                    i("lat_pulldown", 3, 12), i("one_arm_dumbbell_row", 3, 10), i("straight_arm_pulldown", 2, 15)),
                r("bro_shoulders", "Shoulder Day", listOf("Shoulders"),
                    i("overhead_press", 4, 8), i("arnold_press", 3, 10), i("dumbbell_lateral_raise", 4, 15),
                    i("rear_delt_fly", 3, 15), i("barbell_shrug", 3, 12)),
                r("bro_legs", "Leg Day", listOf("Legs"),
                    i("back_squat", 4, 8), i("leg_press", 3, 12), i("romanian_deadlift", 3, 10),
                    i("walking_lunge", 3, 12), i("leg_extension", 3, 15), i("seated_calf_raise", 4, 15)),
                r("bro_arms", "Arm Day", listOf("Arms"),
                    i("close_grip_bench_press", 3, 8), i("barbell_curl", 3, 10),
                    i("skull_crusher", 3, 10, group = "a"), i("incline_dumbbell_curl", 3, 10, group = "a"),
                    i("tricep_rope_pushdown", 3, 12, group = "b"), i("hammer_curl", 3, 12, group = "b"), i("wrist_curl", 2, 15)),
            ),
        ),
        Program(
            "strength_531", "5/3/1 Style Strength", "Advanced", "4 days/week",
            "Strength-focused: one main lift per day with low reps, plus assistance work. Track your top sets and add weight every cycle.",
            "Mon: Squat · Tue: Bench · Thu: Deadlift · Fri: Overhead Press",
            listOf(
                r("s_squat", "Squat Day", listOf("Legs"),
                    i("back_squat", 5, 5), i("leg_press", 3, 10), i("lying_leg_curl", 3, 10), i("hanging_leg_raise", 3, 10)),
                r("s_bench", "Bench Day", listOf("Chest"),
                    i("barbell_bench_press", 5, 5), i("dumbbell_bench_press", 3, 10), i("barbell_row", 3, 10), i("tricep_dips", 3, 10)),
                r("s_dead", "Deadlift Day", listOf("Back"),
                    i("deadlift", 5, 3), i("front_squat", 3, 8), i("pull_up", 3, 8), i("ab_wheel_rollout", 3, 10)),
                r("s_ohp", "Overhead Press Day", listOf("Shoulders"),
                    i("overhead_press", 5, 5), i("chin_up", 3, 8), i("dumbbell_lateral_raise", 3, 15), i("face_pull", 3, 15)),
            ),
        ),
        Program(
            "home_bodyweight", "Home / Bodyweight", "Beginner", "3 days/week",
            "No equipment besides a pull-up bar. Great for travel or training at home. Increase reps or slow down the tempo to progress.",
            "Mon · Wed · Fri (same circuit)",
            listOf(
                r("bw_circuit", "Bodyweight Circuit", listOf("Full Body"),
                    i("push_up", 3, 15, group = "a"), i("bodyweight_squat", 3, 20, group = "a"), i("inverted_row", 3, 12, group = "a"),
                    i("glute_bridge", 3, 15, group = "b"), i("pull_up", 3, 6, group = "b"), i("plank", 3, 0, dur = 45, group = "b"),
                    i("mountain_climber", 3, 0, dur = 30), i("burpee", 3, 10)),
            ),
        ),
    )
}
