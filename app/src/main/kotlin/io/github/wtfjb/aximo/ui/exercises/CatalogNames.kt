package io.github.wtfjb.aximo.ui.exercises

import io.github.wtfjb.aximo.R
import io.github.wtfjb.aximo.domain.exercise.CatalogExercise

/** Display name of a catalog exercise, stored once when the catalog is created. */
fun CatalogExercise.nameRes(): Int = when (this) {
    CatalogExercise.BENCH_PRESS -> R.string.catalog_bench_press
    CatalogExercise.INCLINE_BENCH_PRESS -> R.string.catalog_incline_bench_press
    CatalogExercise.DUMBBELL_BENCH_PRESS -> R.string.catalog_dumbbell_bench_press
    CatalogExercise.INCLINE_DUMBBELL_PRESS -> R.string.catalog_incline_dumbbell_press
    CatalogExercise.CABLE_FLY -> R.string.catalog_cable_fly
    CatalogExercise.PUSH_UP -> R.string.catalog_push_up
    CatalogExercise.DIP -> R.string.catalog_dip
    CatalogExercise.PULL_UP -> R.string.catalog_pull_up
    CatalogExercise.CHIN_UP -> R.string.catalog_chin_up
    CatalogExercise.LAT_PULLDOWN -> R.string.catalog_lat_pulldown
    CatalogExercise.BARBELL_ROW -> R.string.catalog_barbell_row
    CatalogExercise.DUMBBELL_ROW -> R.string.catalog_dumbbell_row
    CatalogExercise.SEATED_CABLE_ROW -> R.string.catalog_seated_cable_row
    CatalogExercise.DEADLIFT -> R.string.catalog_deadlift
    CatalogExercise.FACE_PULL -> R.string.catalog_face_pull
    CatalogExercise.SQUAT -> R.string.catalog_squat
    CatalogExercise.FRONT_SQUAT -> R.string.catalog_front_squat
    CatalogExercise.LEG_PRESS -> R.string.catalog_leg_press
    CatalogExercise.ROMANIAN_DEADLIFT -> R.string.catalog_romanian_deadlift
    CatalogExercise.BULGARIAN_SPLIT_SQUAT -> R.string.catalog_bulgarian_split_squat
    CatalogExercise.LUNGE -> R.string.catalog_lunge
    CatalogExercise.LEG_EXTENSION -> R.string.catalog_leg_extension
    CatalogExercise.LEG_CURL -> R.string.catalog_leg_curl
    CatalogExercise.HIP_THRUST -> R.string.catalog_hip_thrust
    CatalogExercise.CALF_RAISE -> R.string.catalog_calf_raise
    CatalogExercise.OVERHEAD_PRESS -> R.string.catalog_overhead_press
    CatalogExercise.DUMBBELL_SHOULDER_PRESS -> R.string.catalog_dumbbell_shoulder_press
    CatalogExercise.LATERAL_RAISE -> R.string.catalog_lateral_raise
    CatalogExercise.REVERSE_FLY -> R.string.catalog_reverse_fly
    CatalogExercise.BARBELL_CURL -> R.string.catalog_barbell_curl
    CatalogExercise.DUMBBELL_CURL -> R.string.catalog_dumbbell_curl
    CatalogExercise.HAMMER_CURL -> R.string.catalog_hammer_curl
    CatalogExercise.TRICEPS_PUSHDOWN -> R.string.catalog_triceps_pushdown
    CatalogExercise.SKULL_CRUSHER -> R.string.catalog_skull_crusher
    CatalogExercise.OVERHEAD_TRICEPS_EXTENSION -> R.string.catalog_overhead_triceps_extension
    CatalogExercise.HANGING_LEG_RAISE -> R.string.catalog_hanging_leg_raise
    CatalogExercise.CABLE_CRUNCH -> R.string.catalog_cable_crunch
    CatalogExercise.AB_WHEEL -> R.string.catalog_ab_wheel
}
