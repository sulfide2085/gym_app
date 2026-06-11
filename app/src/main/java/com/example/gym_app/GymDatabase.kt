package com.example.gym_app

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Simple file-based local storage using AppDataJson for serialization.
 * Stores data as a JSON file in the app's internal storage directory.
 */
object GymLocalStore {
    private const val DATA_FILE_NAME = "gym_data.json"

    private fun dataFile(context: Context): File =
        File(context.filesDir, DATA_FILE_NAME)

    suspend fun loadExercises(context: Context): List<ExerciseDefinition> {
        val file = dataFile(context)
        if (!file.exists()) return emptyList()
        return try {
            val json = JSONObject(file.readText())
            AppDataJson.decode(json).exercises
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveExercises(context: Context, exercises: List<ExerciseDefinition>) {
        val existing = loadSnapshot(context)
        writeSnapshot(context, existing.copy(exercises = exercises))
    }

    suspend fun loadWorkouts(context: Context): Map<String, WorkoutDay> {
        val file = dataFile(context)
        if (!file.exists()) return emptyMap()
        return try {
            val json = JSONObject(file.readText())
            AppDataJson.decode(json).workouts
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun saveWorkout(context: Context, workout: WorkoutDay) {
        val existing = loadSnapshot(context)
        val updated = existing.workouts.toMutableMap()
        updated[workout.date] = workout
        writeSnapshot(context, existing.copy(workouts = updated))
    }

    suspend fun saveAllWorkouts(context: Context, workouts: Map<String, WorkoutDay>) {
        val existing = loadSnapshot(context)
        writeSnapshot(context, existing.copy(workouts = workouts))
    }

    private fun loadSnapshot(context: Context): AppDataSnapshot {
        val file = dataFile(context)
        if (!file.exists()) return AppDataSnapshot(emptyMap(), emptyList())
        return try {
            AppDataJson.decode(file.readText())
        } catch (_: Exception) {
            AppDataSnapshot(emptyMap(), emptyList())
        }
    }

    private fun writeSnapshot(context: Context, snapshot: AppDataSnapshot) {
        try {
            val json = AppDataJson.encode(snapshot.workouts, snapshot.exercises)
            dataFile(context).writeText(json)
        } catch (_: Exception) {
            // Silent failure — data remains in memory
        }
    }
}
