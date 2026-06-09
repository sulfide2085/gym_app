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
    var workouts by remember { mutableStateOf(seedWorkouts(selectedDate)) }
    var exerciseLibrary by remember { mutableStateOf(defaultExerciseLibrary()) }
    var session by remember { mutableStateOf(CloudflareAuthStore.load(context)) }
    var cloudStateLoaded by rememberSaveable(session?.token) { mutableStateOf(!CloudflareConfig.isConfigured || session == null) }

    if (CloudflareConfig.isConfigured && session == null) {
        AuthScreen(
            onAuthenticated = { nextSession ->
                CloudflareAuthStore.save(context, nextSession)
                session = nextSession
                cloudStateLoaded = false
            }
        )
        return
    }

    LaunchedEffect(session?.token) {
        val currentSession = session
        if (CloudflareConfig.isConfigured && currentSession != null) {
            CloudflareSyncClient.fetchState(currentSession.token)?.let { remote ->
                if (remote.workouts.isNotEmpty()) {
                    workouts = remote.workouts
                }
                if (remote.exercises.isNotEmpty()) {
                    exerciseLibrary = remote.exercises
                }
            }
            cloudStateLoaded = true
        }
    }

    LaunchedEffect(workouts, exerciseLibrary, cloudStateLoaded) {
        val currentSession = session
        if (CloudflareConfig.isConfigured && cloudStateLoaded && currentSession != null) {
            delay(900)
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
                    onSessionChange = { nextSession ->
                        CloudflareAuthStore.save(context, nextSession)
                        session = nextSession
                    },
                    onLogout = {
                        val token = session?.token
                        CloudflareAuthStore.clear(context)
                        session = null
                        activeTab = AppTab.Today
                        workouts = seedWorkouts(selectedDate)
                        exerciseLibrary = defaultExerciseLibrary()
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
