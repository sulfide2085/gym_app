package com.example.gym_app

import org.json.JSONArray
import org.json.JSONObject

data class AppDataSnapshot(
    val workouts: Map<String, WorkoutDay>,
    val exercises: List<ExerciseDefinition>
)

object AppDataJson {
    fun encode(
        workouts: Map<String, WorkoutDay>,
        exercises: List<ExerciseDefinition>
    ): String = JSONObject()
        .put("schemaVersion", 1)
        .put("exercises", JSONArray().apply {
            exercises.forEach { exercise ->
                put(JSONObject()
                    .put("id", exercise.id)
                    .put("name", exercise.name)
                    .put("bodyPart", exercise.bodyPart)
                    .put("equipment", exercise.equipment)
                    .put("isStretching", exercise.isStretching)
                )
            }
        })
        .put("workouts", JSONArray().apply {
            workouts.values.sortedBy { it.date }.forEach { workout ->
                put(JSONObject()
                    .put("date", workout.date)
                    .put("title", workout.title)
                    .put("exercises", JSONArray().apply {
                        workout.exercises.forEach { exercise ->
                            put(JSONObject()
                                .put("id", exercise.id)
                                .put("name", exercise.name)
                                .put("bodyPart", exercise.bodyPart)
                                .put("equipment", exercise.equipment)
                                .put("sets", JSONArray().apply {
                                    exercise.sets.forEach { set ->
                                        put(JSONObject()
                                            .put("id", set.id)
                                            .put("weight", set.weight)
                                            .put("reps", set.reps)
                                            .put("completed", set.completed)
                                        )
                                    }
                                })
                            )
                        }
                    })
                )
            }
        })
        .toString()

    fun decode(text: String): AppDataSnapshot {
        val json = JSONObject(text)
        val exercises = json.optJSONArray("exercises").toExerciseDefinitions()
        val workouts = json.optJSONArray("workouts").toWorkoutDays().associateBy { it.date }
        return AppDataSnapshot(workouts = workouts, exercises = exercises)
    }

    fun decode(json: JSONObject): AppDataSnapshot {
        val exercises = json.optJSONArray("exercises").toExerciseDefinitions()
        val workouts = json.optJSONArray("workouts").toWorkoutDays().associateBy { it.date }
        return AppDataSnapshot(workouts = workouts, exercises = exercises)
    }

    private fun JSONArray?.toExerciseDefinitions(): List<ExerciseDefinition> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            ExerciseDefinition(
                id = item.optLong("id", System.nanoTime() + index),
                name = item.optString("name"),
                bodyPart = item.optString("bodyPart", "其他"),
                equipment = item.optString("equipment", "其他"),
                isStretching = item.optBoolean("isStretching", false)
            )
        }.filter { it.name.isNotBlank() }
    }

    private fun JSONArray?.toWorkoutDays(): List<WorkoutDay> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            WorkoutDay(
                date = item.optString("date"),
                title = item.optString("title", "训练记录"),
                exercises = item.optJSONArray("exercises").toWorkoutExercises(index)
            )
        }.filter { it.date.isNotBlank() }
    }

    private fun JSONArray?.toWorkoutExercises(dayIndex: Int): List<WorkoutExercise> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            WorkoutExercise(
                id = item.optLong("id", System.nanoTime() + dayIndex * 100 + index),
                name = item.optString("name"),
                bodyPart = item.optString("bodyPart", "其他"),
                equipment = item.optString("equipment", "其他"),
                sets = item.optJSONArray("sets").toSetEntries(dayIndex, index)
            )
        }.filter { it.name.isNotBlank() }
    }

    private fun JSONArray?.toSetEntries(dayIndex: Int, exerciseIndex: Int): List<SetEntry> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            val item = getJSONObject(index)
            SetEntry(
                id = item.optLong("id", System.nanoTime() + dayIndex * 1000 + exerciseIndex * 100 + index),
                weight = item.optString("weight"),
                reps = item.optString("reps"),
                completed = item.optBoolean("completed", true)
            )
        }
    }
}
