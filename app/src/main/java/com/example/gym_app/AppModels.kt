package com.example.gym_app

enum class AppTab(val title: String) {
    Today("今日"),
    Timer("计时"),
    Exercises("动作"),
    Calendar("日历"),
    Account("我的")
}

data class SetEntry(
    val id: Long,
    val weight: String = "",
    val reps: String = "",
    val completed: Boolean = true
)

data class WorkoutExercise(
    val id: Long,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val sets: List<SetEntry>
)

data class ExerciseDefinition(
    val id: Long,
    val name: String,
    val bodyPart: String,
    val equipment: String
)

data class WorkoutDay(
    val date: String,
    val title: String,
    val exercises: List<WorkoutExercise>
)

data class DayWorkoutSummary(
    val date: String,
    val title: String,
    val completedSets: Int,
    val totalSets: Int,
    val exercises: List<String>
)

fun <T> List<T>.updateAt(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { currentIndex, item -> if (currentIndex == index) transform(item) else item }

val BodyPartOptions = listOf("胸", "背", "腿", "肩", "手臂", "核心", "有氧", "其他")
val EquipmentOptions = listOf("杠铃", "哑铃", "机器", "绳索", "自重", "有氧器械", "其他")

fun defaultExerciseLibrary(): List<ExerciseDefinition> = listOf(
    ExerciseDefinition(1, "哑铃上斜推胸", "胸", "哑铃"),
    ExerciseDefinition(2, "卧推（杠铃）", "胸", "杠铃"),
    ExerciseDefinition(3, "器械下压", "胸", "机器"),
    ExerciseDefinition(4, "高位下拉", "背", "机器"),
    ExerciseDefinition(5, "坐姿划船", "背", "机器"),
    ExerciseDefinition(6, "蹬腿机", "腿", "机器"),
    ExerciseDefinition(7, "哑铃推肩", "肩", "哑铃"),
    ExerciseDefinition(8, "哑铃飞鸟", "肩", "哑铃"),
    ExerciseDefinition(9, "有氧运动", "有氧", "有氧器械"),
    ExerciseDefinition(10, "战绳", "有氧", "绳索"),
    ExerciseDefinition(11, "板凳撑体", "手臂", "自重")
)

fun sampleWorkoutHistory(): Map<String, WorkoutDay> = listOf(
    WorkoutDay(
        date = "2026-05-29",
        title = "背",
        exercises = listOf(
            workoutExercise(101, "坐姿划船", "背", "机器", 30 to 10, 35 to 10, 40 to 10, 40 to 10, 45 to 10, 50 to 10),
            workoutExercise(102, "高位下拉", "背", "机器", 27 to 10, 27 to 10, 32 to 10, 32 to 10, 32 to 10)
        )
    ),
    WorkoutDay(
        date = "2026-05-25",
        title = "胸",
        exercises = listOf(
            workoutExercise(201, "卧推（杠铃）", "胸", "杠铃", 30 to 10, 35 to 10, 35 to 10, 40 to 10, 40 to 10),
            workoutExercise(202, "哑铃上斜推胸", "胸", "哑铃", 14 to 10, 10 to 10, 10 to 10, 10 to 14, 10 to 14),
            workoutExercise(203, "器械下压", "胸", "机器", 35 to 10, 35 to 10, 40 to 10, 40 to 10, 40 to 10)
        )
    ),
    WorkoutDay(
        date = "2026-05-22",
        title = "练肩",
        exercises = listOf(
            workoutExercise(301, "哑铃推肩", "肩", "哑铃", 8 to 10, 8 to 10, 8 to 10, 8 to 10, 8 to 10),
            workoutExercise(302, "哑铃飞鸟", "肩", "哑铃", 6 to 10, 6 to 10, 6 to 10, 6 to 10, 6 to 10)
        )
    ),
    WorkoutDay(
        date = "2026-05-21",
        title = "练背",
        exercises = listOf(
            workoutExercise(401, "高位下拉", "背", "机器", 20 to 10, 25 to 10, 30 to 10, 30 to 10, 25 to 10, 25 to 10),
            workoutExercise(402, "坐姿划船", "背", "机器", 25 to 10, 30 to 10, 35 to 10, 40 to 10)
        )
    ),
    WorkoutDay(
        date = "2026-05-19",
        title = "胸",
        exercises = listOf(
            workoutExercise(501, "哑铃上斜推胸", "胸", "哑铃", 14 to 10, 14 to 10, 14 to 9),
            workoutExercise(502, "卧推（杠铃）", "胸", "杠铃", 5 to 10, 10 to 10, 10 to 10, 5 to 10, 5 to 10)
        )
    ),
    WorkoutDay(
        date = "2026-03-11",
        title = "背与腿",
        exercises = listOf(
            workoutExercise(601, "高位下拉", "背", "机器", 25 to 10, 30 to 10, 35 to 10, 35 to 10, 35 to 10),
            workoutExercise(602, "坐姿划船", "背", "机器", 30 to 10, 35 to 10, 35 to 10, 35 to 10, 35 to 10),
            workoutExercise(603, "蹬腿机", "腿", "机器", 65 to 10, 65 to 10, 65 to 10, 65 to 10, 65 to 10)
        )
    ),
    WorkoutDay(
        date = "2026-01-20",
        title = "练腿",
        exercises = listOf(
            workoutExercise(701, "蹬腿机", "腿", "机器", 50 to 10, 50 to 10, 70 to 10, 70 to 10, 85 to 10, 85 to 10)
        )
    )
).associateBy { it.date }

fun recentWorkoutsForExercise(
    workouts: Map<String, WorkoutDay>,
    exerciseName: String,
    limit: Int = 3
): List<WorkoutDay> = workouts.values
    .filter { day -> day.exercises.any { it.name == exerciseName } }
    .sortedByDescending { it.date }
    .take(limit)

fun bestSetLabel(exercise: WorkoutExercise): String {
    val best = exercise.sets.maxByOrNull { set ->
        (set.weight.toDoubleOrNull() ?: 0.0) * (set.reps.toIntOrNull() ?: 0)
    } ?: return "无数据"
    val weight = best.weight.removeSuffix(".0")
    return "${weight}kg x ${best.reps}"
}

fun fullSetsLabel(exercise: WorkoutExercise): String =
    exercise.sets.joinToString(" / ") { set ->
        val weight = set.weight.removeSuffix(".0").ifBlank { "-" }
        val reps = set.reps.ifBlank { "-" }
        "${weight}kg x $reps"
    }.ifBlank { "无组数据" }

private fun workoutExercise(
    id: Long,
    name: String,
    bodyPart: String,
    equipment: String,
    vararg sets: Pair<Int, Int>
): WorkoutExercise = WorkoutExercise(
    id = id,
    name = name,
    bodyPart = bodyPart,
    equipment = equipment,
    sets = sets.mapIndexed { index, set ->
        SetEntry(
            id = id * 100 + index,
            weight = set.first.toString(),
            reps = set.second.toString(),
            completed = true
        )
    }
)
