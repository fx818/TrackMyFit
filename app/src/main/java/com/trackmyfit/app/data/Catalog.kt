package com.trackmyfit.app.data

object Catalog {
    val muscleGroups = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Forearms",
        "Legs", "Glutes", "Calves", "Abs", "Cardio", "Full Body",
    )

    val equipment = listOf(
        "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight",
        "Kettlebell", "Smith Machine", "EZ Bar", "Band", "Other",
    )

    val defaultLabels = listOf(
        "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Legs", "Arms", "Abs",
        "Push", "Pull", "Upper Body", "Lower Body", "Full Body", "Cardio", "Rest",
    )

    private val raw: List<Exercise> = listOf(
        // Chest
        Exercise("b_barbell_bench_press", "Barbell Bench Press", "Chest", "Barbell"),
        Exercise("b_incline_barbell_bench_press", "Incline Barbell Bench Press", "Chest", "Barbell"),
        Exercise("b_decline_barbell_bench_press", "Decline Barbell Bench Press", "Chest", "Barbell"),
        Exercise("b_dumbbell_bench_press", "Dumbbell Bench Press", "Chest", "Dumbbell"),
        Exercise("b_incline_dumbbell_press", "Incline Dumbbell Press", "Chest", "Dumbbell"),
        Exercise("b_decline_dumbbell_press", "Decline Dumbbell Press", "Chest", "Dumbbell"),
        Exercise("b_dumbbell_fly", "Dumbbell Fly", "Chest", "Dumbbell"),
        Exercise("b_incline_dumbbell_fly", "Incline Dumbbell Fly", "Chest", "Dumbbell"),
        Exercise("b_cable_crossover", "Cable Crossover", "Chest", "Cable"),
        Exercise("b_low_to_high_cable_fly", "Low-to-High Cable Fly", "Chest", "Cable"),
        Exercise("b_pec_deck_fly", "Pec Deck Fly", "Chest", "Machine"),
        Exercise("b_chest_press_machine", "Chest Press Machine", "Chest", "Machine"),
        Exercise("b_incline_chest_press_machine", "Incline Chest Press Machine", "Chest", "Machine"),
        Exercise("b_smith_machine_bench_press", "Smith Machine Bench Press", "Chest", "Smith Machine"),
        Exercise("b_smith_machine_incline_press", "Smith Machine Incline Press", "Chest", "Smith Machine"),
        Exercise("b_push_up", "Push-Up", "Chest", "Bodyweight"),
        Exercise("b_chest_dips", "Chest Dips", "Chest", "Bodyweight"),
        Exercise("b_dumbbell_pullover", "Dumbbell Pullover", "Chest", "Dumbbell"),
        // Back
        Exercise("b_deadlift", "Deadlift", "Back", "Barbell"),
        Exercise("b_pull_up", "Pull-Up", "Back", "Bodyweight"),
        Exercise("b_chin_up", "Chin-Up", "Back", "Bodyweight"),
        Exercise("b_lat_pulldown", "Lat Pulldown", "Back", "Cable"),
        Exercise("b_close_grip_lat_pulldown", "Close-Grip Lat Pulldown", "Back", "Cable"),
        Exercise("b_barbell_row", "Barbell Row", "Back", "Barbell"),
        Exercise("b_pendlay_row", "Pendlay Row", "Back", "Barbell"),
        Exercise("b_one_arm_dumbbell_row", "One-Arm Dumbbell Row", "Back", "Dumbbell"),
        Exercise("b_seated_cable_row", "Seated Cable Row", "Back", "Cable"),
        Exercise("b_t_bar_row", "T-Bar Row", "Back", "Barbell"),
        Exercise("b_chest_supported_row", "Chest-Supported Row", "Back", "Dumbbell"),
        Exercise("b_machine_row", "Machine Row", "Back", "Machine"),
        Exercise("b_straight_arm_pulldown", "Straight-Arm Pulldown", "Back", "Cable"),
        Exercise("b_rack_pull", "Rack Pull", "Back", "Barbell"),
        Exercise("b_back_extension", "Back Extension", "Back", "Bodyweight"),
        Exercise("b_inverted_row", "Inverted Row", "Back", "Bodyweight"),
        Exercise("b_assisted_pull_up", "Assisted Pull-Up", "Back", "Machine"),
        Exercise("b_barbell_shrug", "Barbell Shrug", "Back", "Barbell"),
        Exercise("b_dumbbell_shrug", "Dumbbell Shrug", "Back", "Dumbbell"),
        // Shoulders
        Exercise("b_overhead_press", "Overhead Press", "Shoulders", "Barbell"),
        Exercise("b_seated_dumbbell_shoulder_press", "Seated Dumbbell Shoulder Press", "Shoulders", "Dumbbell"),
        Exercise("b_arnold_press", "Arnold Press", "Shoulders", "Dumbbell"),
        Exercise("b_machine_shoulder_press", "Machine Shoulder Press", "Shoulders", "Machine"),
        Exercise("b_smith_machine_shoulder_press", "Smith Machine Shoulder Press", "Shoulders", "Smith Machine"),
        Exercise("b_dumbbell_lateral_raise", "Dumbbell Lateral Raise", "Shoulders", "Dumbbell"),
        Exercise("b_cable_lateral_raise", "Cable Lateral Raise", "Shoulders", "Cable"),
        Exercise("b_machine_lateral_raise", "Machine Lateral Raise", "Shoulders", "Machine"),
        Exercise("b_dumbbell_front_raise", "Dumbbell Front Raise", "Shoulders", "Dumbbell"),
        Exercise("b_rear_delt_fly", "Rear Delt Fly", "Shoulders", "Dumbbell"),
        Exercise("b_reverse_pec_deck", "Reverse Pec Deck", "Shoulders", "Machine"),
        Exercise("b_face_pull", "Face Pull", "Shoulders", "Cable"),
        Exercise("b_upright_row", "Upright Row", "Shoulders", "Barbell"),
        Exercise("b_push_press", "Push Press", "Shoulders", "Barbell"),
        // Biceps
        Exercise("b_barbell_curl", "Barbell Curl", "Biceps", "Barbell"),
        Exercise("b_ez_bar_curl", "EZ Bar Curl", "Biceps", "EZ Bar"),
        Exercise("b_dumbbell_curl", "Dumbbell Curl", "Biceps", "Dumbbell"),
        Exercise("b_hammer_curl", "Hammer Curl", "Biceps", "Dumbbell"),
        Exercise("b_incline_dumbbell_curl", "Incline Dumbbell Curl", "Biceps", "Dumbbell"),
        Exercise("b_preacher_curl", "Preacher Curl", "Biceps", "EZ Bar"),
        Exercise("b_concentration_curl", "Concentration Curl", "Biceps", "Dumbbell"),
        Exercise("b_cable_curl", "Cable Curl", "Biceps", "Cable"),
        Exercise("b_bayesian_cable_curl", "Bayesian Cable Curl", "Biceps", "Cable"),
        Exercise("b_spider_curl", "Spider Curl", "Biceps", "Dumbbell"),
        Exercise("b_machine_bicep_curl", "Machine Bicep Curl", "Biceps", "Machine"),
        Exercise("b_rope_hammer_curl", "Rope Hammer Curl", "Biceps", "Cable"),
        // Triceps
        Exercise("b_close_grip_bench_press", "Close-Grip Bench Press", "Triceps", "Barbell"),
        Exercise("b_tricep_rope_pushdown", "Tricep Rope Pushdown", "Triceps", "Cable"),
        Exercise("b_tricep_bar_pushdown", "Tricep Bar Pushdown", "Triceps", "Cable"),
        Exercise("b_overhead_cable_tricep_extension", "Overhead Cable Tricep Extension", "Triceps", "Cable"),
        Exercise("b_overhead_dumbbell_extension", "Overhead Dumbbell Extension", "Triceps", "Dumbbell"),
        Exercise("b_skull_crusher", "Skull Crusher", "Triceps", "EZ Bar"),
        Exercise("b_tricep_dips", "Tricep Dips", "Triceps", "Bodyweight"),
        Exercise("b_bench_dips", "Bench Dips", "Triceps", "Bodyweight"),
        Exercise("b_tricep_kickback", "Tricep Kickback", "Triceps", "Dumbbell"),
        Exercise("b_diamond_push_up", "Diamond Push-Up", "Triceps", "Bodyweight"),
        Exercise("b_machine_tricep_extension", "Machine Tricep Extension", "Triceps", "Machine"),
        Exercise("b_single_arm_cable_pushdown", "Single-Arm Cable Pushdown", "Triceps", "Cable"),
        // Forearms
        Exercise("b_wrist_curl", "Wrist Curl", "Forearms", "Dumbbell"),
        Exercise("b_reverse_wrist_curl", "Reverse Wrist Curl", "Forearms", "Dumbbell"),
        Exercise("b_reverse_barbell_curl", "Reverse Barbell Curl", "Forearms", "Barbell"),
        Exercise("b_farmer_s_walk", "Farmer's Walk", "Forearms", "Dumbbell"),
        Exercise("b_dead_hang", "Dead Hang", "Forearms", "Bodyweight"),
        // Legs
        Exercise("b_back_squat", "Back Squat", "Legs", "Barbell"),
        Exercise("b_front_squat", "Front Squat", "Legs", "Barbell"),
        Exercise("b_leg_press", "Leg Press", "Legs", "Machine"),
        Exercise("b_hack_squat", "Hack Squat", "Legs", "Machine"),
        Exercise("b_smith_machine_squat", "Smith Machine Squat", "Legs", "Smith Machine"),
        Exercise("b_goblet_squat", "Goblet Squat", "Legs", "Dumbbell"),
        Exercise("b_bulgarian_split_squat", "Bulgarian Split Squat", "Legs", "Dumbbell"),
        Exercise("b_walking_lunge", "Walking Lunge", "Legs", "Dumbbell"),
        Exercise("b_reverse_lunge", "Reverse Lunge", "Legs", "Dumbbell"),
        Exercise("b_step_up", "Step-Up", "Legs", "Dumbbell"),
        Exercise("b_leg_extension", "Leg Extension", "Legs", "Machine"),
        Exercise("b_lying_leg_curl", "Lying Leg Curl", "Legs", "Machine"),
        Exercise("b_seated_leg_curl", "Seated Leg Curl", "Legs", "Machine"),
        Exercise("b_romanian_deadlift", "Romanian Deadlift", "Legs", "Barbell"),
        Exercise("b_dumbbell_romanian_deadlift", "Dumbbell Romanian Deadlift", "Legs", "Dumbbell"),
        Exercise("b_stiff_leg_deadlift", "Stiff-Leg Deadlift", "Legs", "Barbell"),
        Exercise("b_sumo_deadlift", "Sumo Deadlift", "Legs", "Barbell"),
        Exercise("b_good_morning", "Good Morning", "Legs", "Barbell"),
        Exercise("b_pendulum_squat", "Pendulum Squat", "Legs", "Machine"),
        Exercise("b_nordic_hamstring_curl", "Nordic Hamstring Curl", "Legs", "Bodyweight"),
        Exercise("b_bodyweight_squat", "Bodyweight Squat", "Legs", "Bodyweight"),
        // Glutes
        Exercise("b_hip_thrust", "Hip Thrust", "Glutes", "Barbell"),
        Exercise("b_glute_bridge", "Glute Bridge", "Glutes", "Bodyweight"),
        Exercise("b_cable_glute_kickback", "Cable Glute Kickback", "Glutes", "Cable"),
        Exercise("b_hip_abduction_machine", "Hip Abduction Machine", "Glutes", "Machine"),
        Exercise("b_hip_adduction_machine", "Hip Adduction Machine", "Glutes", "Machine"),
        Exercise("b_cable_pull_through", "Cable Pull-Through", "Glutes", "Cable"),
        // Calves
        Exercise("b_standing_calf_raise", "Standing Calf Raise", "Calves", "Machine"),
        Exercise("b_seated_calf_raise", "Seated Calf Raise", "Calves", "Machine"),
        Exercise("b_leg_press_calf_raise", "Leg Press Calf Raise", "Calves", "Machine"),
        Exercise("b_smith_machine_calf_raise", "Smith Machine Calf Raise", "Calves", "Smith Machine"),
        Exercise("b_single_leg_calf_raise", "Single-Leg Calf Raise", "Calves", "Dumbbell"),
        // Abs
        Exercise("b_crunch", "Crunch", "Abs", "Bodyweight"),
        Exercise("b_cable_crunch", "Cable Crunch", "Abs", "Cable"),
        Exercise("b_machine_crunch", "Machine Crunch", "Abs", "Machine"),
        Exercise("b_hanging_leg_raise", "Hanging Leg Raise", "Abs", "Bodyweight"),
        Exercise("b_hanging_knee_raise", "Hanging Knee Raise", "Abs", "Bodyweight"),
        Exercise("b_lying_leg_raise", "Lying Leg Raise", "Abs", "Bodyweight"),
        Exercise("b_plank", "Plank", "Abs", "Bodyweight"),
        Exercise("b_side_plank", "Side Plank", "Abs", "Bodyweight"),
        Exercise("b_russian_twist", "Russian Twist", "Abs", "Bodyweight"),
        Exercise("b_ab_wheel_rollout", "Ab Wheel Rollout", "Abs", "Other"),
        Exercise("b_decline_sit_up", "Decline Sit-Up", "Abs", "Bodyweight"),
        Exercise("b_bicycle_crunch", "Bicycle Crunch", "Abs", "Bodyweight"),
        Exercise("b_mountain_climber", "Mountain Climber", "Abs", "Bodyweight"),
        Exercise("b_cable_woodchopper", "Cable Woodchopper", "Abs", "Cable"),
        // Cardio
        Exercise("b_treadmill", "Treadmill", "Cardio", "Machine"),
        Exercise("b_stationary_bike", "Stationary Bike", "Cardio", "Machine"),
        Exercise("b_rowing_machine", "Rowing Machine", "Cardio", "Machine"),
        Exercise("b_elliptical", "Elliptical", "Cardio", "Machine"),
        Exercise("b_stair_climber", "Stair Climber", "Cardio", "Machine"),
        Exercise("b_jump_rope", "Jump Rope", "Cardio", "Other"),
        Exercise("b_running", "Running", "Cardio", "Bodyweight"),
        Exercise("b_cycling", "Cycling", "Cardio", "Other"),
        // Full Body
        Exercise("b_power_clean", "Power Clean", "Full Body", "Barbell"),
        Exercise("b_clean_and_jerk", "Clean and Jerk", "Full Body", "Barbell"),
        Exercise("b_snatch", "Snatch", "Full Body", "Barbell"),
        Exercise("b_kettlebell_swing", "Kettlebell Swing", "Full Body", "Kettlebell"),
        Exercise("b_thruster", "Thruster", "Full Body", "Barbell"),
        Exercise("b_burpee", "Burpee", "Full Body", "Bodyweight"),
        Exercise("b_turkish_get_up", "Turkish Get-Up", "Full Body", "Kettlebell"),
        Exercise("b_battle_ropes", "Battle Ropes", "Full Body", "Other"),
        Exercise("b_sled_push", "Sled Push", "Full Body", "Other"),
    )

    private val timeIds = setOf(
        "b_plank", "b_side_plank", "b_dead_hang", "b_battle_ropes", "b_mountain_climber", "b_jump_rope", "b_stair_climber",
    )
    private val distanceIds = setOf(
        "b_treadmill", "b_stationary_bike", "b_rowing_machine", "b_elliptical", "b_running", "b_cycling",
    )

    val defaultExercises: List<Exercise> = raw.map {
        when (it.id) {
            in timeIds -> it.copy(trackType = TrackType.TIME)
            in distanceIds -> it.copy(trackType = TrackType.DISTANCE_TIME)
            else -> it
        }
    }
}
