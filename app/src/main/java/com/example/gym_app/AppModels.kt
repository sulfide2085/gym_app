package com.example.gym_app

enum class AppTab(val title: String) {
    Today("今日"),
    Timer("计时"),
    Exercises("动作"),
    Calendar("日历")
}

data class SetEntry(
    val id: Long,
    val weight: String = "",
    val reps: String = "",
    val completed: Boolean = false
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

data class DayWorkoutSummary(
    val day: Int,
    val title: String,
    val completedSets: Int,
    val totalSets: Int,
    val exercises: List<String>
)

fun <T> List<T>.updateAt(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { currentIndex, item -> if (currentIndex == index) transform(item) else item }
