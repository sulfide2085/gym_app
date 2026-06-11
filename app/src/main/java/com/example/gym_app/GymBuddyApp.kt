package com.example.gym_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GymBuddyApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    var selectedDate by rememberSaveable { mutableStateOf(todayIsoDate()) }
    var workouts by remember { mutableStateOf(emptyMap<String, WorkoutDay>()) }
    var exerciseLibrary by remember { mutableStateOf(emptyList<ExerciseDefinition>()) }
    var localDataLoaded by rememberSaveable { mutableStateOf(false) }
    var session by remember { mutableStateOf(CloudflareAuthStore.load(context)) }
    var cloudSynced by rememberSaveable(session?.token) { mutableStateOf(false) }

    // Step 1: Load from local database immediately
    LaunchedEffect(Unit) {
        val localWorkouts = GymLocalStore.loadWorkouts(context)
        val localExercises = GymLocalStore.loadExercises(context)
        if (localWorkouts.isNotEmpty()) {
            workouts = localWorkouts
        }
        if (localExercises.isNotEmpty()) {
            exerciseLibrary = localExercises
        } else {
            // First launch: seed default data to local DB
            val seed = seedWorkouts(selectedDate)
            val defaults = defaultExerciseLibrary()
            workouts = seed
            exerciseLibrary = defaults
            GymLocalStore.saveAllWorkouts(context, seed)
            GymLocalStore.saveExercises(context, defaults)
        }
        localDataLoaded = true
    }

    if (CloudflareConfig.isConfigured && session == null && localDataLoaded) {
        AuthScreen(
            onAuthenticated = { nextSession ->
                CloudflareAuthStore.save(context, nextSession)
                session = nextSession
                cloudSynced = false
            }
        )
        return
    }

    // Step 2: Sync from cloud (merge with local)
    LaunchedEffect(session?.token, localDataLoaded) {
        val currentSession = session
        if (CloudflareConfig.isConfigured && currentSession != null && localDataLoaded) {
            val remote = CloudflareSyncClient.fetchState(currentSession.token)
            if (remote != null) {
                // Merge: prefer newer data per day
                val merged = remote.workouts.toMutableMap()
                workouts.forEach { (date, local) ->
                    val existing = merged[date]
                    if (existing == null || local.exercises.size > existing.exercises.size) {
                        merged[date] = local
                    }
                }
                workouts = merged
                exerciseLibrary = remote.exercises
                // Persist merged result locally
                GymLocalStore.saveAllWorkouts(context, workouts)
                GymLocalStore.saveExercises(context, exerciseLibrary)
            }
            cloudSynced = true
        }
    }

    // Step 3a: Save locally on every change
    LaunchedEffect(workouts, exerciseLibrary, localDataLoaded) {
        if (!localDataLoaded) return@LaunchedEffect
        GymLocalStore.saveAllWorkouts(context, workouts)
        GymLocalStore.saveExercises(context, exerciseLibrary)
    }

    // Step 3b: Sync to cloud every 5 minutes (first sync immediately after login)
    LaunchedEffect(session?.token, cloudSynced, localDataLoaded) {
        val currentSession = session
        if (!CloudflareConfig.isConfigured || !cloudSynced || currentSession == null || !localDataLoaded) return@LaunchedEffect
        // Immediate sync after login
        CloudflareSyncClient.saveState(currentSession.token, workouts, exerciseLibrary)
        // Then every 5 minutes
        while (true) {
            delay(5 * 60 * 1000L)
            CloudflareSyncClient.saveState(currentSession.token, workouts, exerciseLibrary)
        }
    }

    fun updateSelectedWorkout(transform: (WorkoutDay) -> WorkoutDay) {
        val current = workouts[selectedDate] ?: emptyWorkoutForDate(selectedDate)
        workouts = workouts + (selectedDate to transform(current))
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { BottomNavigation(activeTab = activeTab, onSelect = { activeTab = it }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AppBgTop, AppBg, AppBgBottom)))
                .padding(padding),
        ) {
            when (activeTab) {
                AppTab.Today -> TodayScreen(
                    date = selectedDate,
                    title = workouts[selectedDate]?.title ?: emptyWorkoutForDate(selectedDate).title,
                    onTitleChange = { title ->
                        updateSelectedWorkout { it.copy(title = title) }
                    },
                    exercises = workouts[selectedDate]?.exercises.orEmpty(),
                    onExercisesChange = { exercises ->
                        updateSelectedWorkout { it.copy(exercises = exercises) }
                    },
                    history = workouts,
                    library = exerciseLibrary
                )
                AppTab.Timer -> TimerScreen()
                AppTab.Exercises -> ExerciseLibraryScreen(
                    exercises = exerciseLibrary,
                    workouts = workouts,
                    onExercisesChange = { exerciseLibrary = it }
                )
                AppTab.Calendar -> CalendarScreen(
                    workouts = workouts,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    onEditSelectedDate = { activeTab = AppTab.Today }
                )
                AppTab.Account -> AccountScreen(
                    session = requireNotNull(session),
                    workouts = workouts,
                    exerciseLibrary = exerciseLibrary,
                    onSessionChange = { nextSession ->
                        CloudflareAuthStore.save(context, nextSession)
                        session = nextSession
                    },
                    onImportData = { snapshot ->
                        workouts = snapshot.workouts
                        exerciseLibrary = snapshot.exercises
                        scope.launch {
                            GymLocalStore.saveAllWorkouts(context, snapshot.workouts)
                            GymLocalStore.saveExercises(context, snapshot.exercises)
                        }
                    },
                    onClearData = {
                        workouts = emptyMap()
                        exerciseLibrary = emptyList()
                        scope.launch {
                            GymLocalStore.saveAllWorkouts(context, emptyMap())
                            GymLocalStore.saveExercises(context, emptyList())
                        }
                    },
                    onLogout = {
                        val token = session?.token
                        CloudflareAuthStore.clear(context)
                        session = null
                        activeTab = AppTab.Today
                        workouts = if (CloudflareConfig.isConfigured) emptyMap() else seedWorkouts(selectedDate)
                        exerciseLibrary = if (CloudflareConfig.isConfigured) emptyList() else defaultExerciseLibrary()
                        if (token != null) {
                            scope.launch { CloudflareSyncClient.logout(token) }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavigation(activeTab: AppTab, onSelect: (AppTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(22.dp, RoundedCornerShape(30.dp), clip = false, ambientColor = Color(0x260B1220), spotColor = Color(0x1F0B1220))
            .background(
                Brush.verticalGradient(listOf(Color(0xF2FFFFFF), AppGlass, Color(0xBFFFFFFF))),
                RoundedCornerShape(30.dp)
            )
            .border(BorderStroke(1.dp, AppLine), RoundedCornerShape(30.dp))
            .border(BorderStroke(0.6.dp, AppHairline), RoundedCornerShape(30.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppTab.entries.forEach { tab ->
            val active = tab == activeTab
            TextButton(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (active) AppBlue else Color.Transparent,
                    contentColor = if (active) Color.White else AppText
                ),
                shape = RoundedCornerShape(26.dp),
                onClick = { onSelect(tab) }
            ) {
                Text(tab.title, fontWeight = FontWeight.Black, fontSize = 15.sp)
            }
        }
    }
}

private fun todayIsoDate(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun seedWorkouts(today: String): Map<String, WorkoutDay> {
    val history = sampleWorkoutHistory()
    if (today in history) return history
    return history + (today to WorkoutDay(
        date = today,
        title = "今日训练",
        exercises = listOf(
            WorkoutExercise(
                id = 1,
                name = "高位下拉",
                bodyPart = "背",
                equipment = "机器",
                sets = listOf(
                    SetEntry(id = 1, weight = "45", reps = "12", completed = true),
                    SetEntry(id = 2, weight = "50", reps = "10", completed = true)
                )
            )
        )
    ))
}

private fun emptyWorkoutForDate(date: String): WorkoutDay =
    WorkoutDay(date = date, title = if (date == todayIsoDate()) "今日训练" else "训练记录", exercises = emptyList())
