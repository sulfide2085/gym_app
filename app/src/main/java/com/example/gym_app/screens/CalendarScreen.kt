package com.example.gym_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    workouts: Map<String, WorkoutDay>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    onEditSelectedDate: () -> Unit
) {
    val today = Calendar.getInstance()
    val todayString = todayDateString()
    val year = selectedDate.substring(0, 4).toIntOrNull() ?: today.get(Calendar.YEAR)
    val monthNumber = selectedDate.substring(5, 7).toIntOrNull() ?: (today.get(Calendar.MONTH) + 1)
    val selectedDay = selectedDate.substring(8, 10).toIntOrNull() ?: today.get(Calendar.DAY_OF_MONTH)
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthNumber - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = monthStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val calendarCells = List(leadingBlanks) { null } + (1..daysInMonth).map { it }
    val monthlyWorkouts = workouts.keys.count {
        it.startsWith(String.format(Locale.US, "%04d-%02d", year, monthNumber))
    }
    val selectedWorkout = workouts[selectedDate]
    val switchMonth: (Int) -> Unit = { delta ->
        onDateSelected(monthShiftDate(year, monthNumber, delta))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            label = "日历",
            title = String.format(Locale.US, "%04d.%02d", year, monthNumber),
            meta = "$monthlyWorkouts 天训练记录"
        )
        MonthSwitcher(
            onPrevious = { switchMonth(-1) },
            onToday = { onDateSelected(todayString) },
            onNext = { switchMonth(1) }
        )
        CalendarMonthGrid(
            year = year,
            monthNumber = monthNumber,
            selectedDay = selectedDay,
            todayString = todayString,
            calendarCells = calendarCells,
            workouts = workouts,
            onDateSelected = onDateSelected,
            onMonthSwipe = switchMonth
        )
        DayWorkoutPanel(
            date = selectedDate,
            workout = selectedWorkout,
            onEdit = onEditSelectedDate
        )
    }
}

@Composable
private fun CalendarMonthGrid(
    year: Int,
    monthNumber: Int,
    selectedDay: Int,
    todayString: String,
    calendarCells: List<Int?>,
    workouts: Map<String, WorkoutDay>,
    onDateSelected: (String) -> Unit,
    onMonthSwipe: (Int) -> Unit
) {
    Column(
        modifier = Modifier.pointerInput(year, monthNumber) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    when {
                        totalDrag <= -80f -> onMonthSwipe(1)
                        totalDrag >= 80f -> onMonthSwipe(-1)
                    }
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f }
            )
        },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    day,
                    modifier = Modifier.weight(1f),
                    color = AppMuted,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        calendarCells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = dateString(year, monthNumber, day)
                        val workout = workouts[date]
                        val trained = workout?.exercises?.isNotEmpty() == true
                        CalendarDayCell(
                            modifier = Modifier.weight(1f),
                            day = day,
                            trained = trained,
                            selected = day == selectedDay,
                            isToday = date == todayString,
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun MonthSwitcher(
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onPrevious) {
            Text("上月", color = AppBlue, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onToday) {
            Text("本月", color = AppMuted, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onNext) {
            Text("下月", color = AppBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    day: Int,
    trained: Boolean,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(if (trained) AppGreenSoft else AppGlass, shape)
            .border(
                BorderStroke(
                    if (selected) 2.dp else 1.dp,
                    if (selected) AppBlue else AppLine
                ),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                fontWeight = FontWeight.Black,
                color = if (selected) AppBlue else if (trained) AppGreen else AppText
            )
            if (trained) {
                Box(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .background(AppGreen, RoundedCornerShape(2.dp))
                        .fillMaxWidth(0.22f)
                        .aspectRatio(3f)
                )
            }
            if (isToday) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 18.dp, height = 14.dp)
                        .background(if (selected) AppBlue else AppText, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "今",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun DayWorkoutPanel(
    date: String,
    workout: WorkoutDay?,
    onEdit: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(date, color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        workout?.title ?: "未安排训练",
                        fontWeight = FontWeight.Black,
                        color = AppText,
                        fontSize = 20.sp
                    )
                }
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onEdit
                ) {
                    Text("编辑这天", fontWeight = FontWeight.Bold)
                }
            }

            if (workout == null || workout.exercises.isEmpty()) {
                Text("这一天还没有训练内容，点编辑可直接新增。", color = AppMuted)
            } else {
                val totalSets = workout.exercises.sumOf { it.sets.size }
                Text("${workout.exercises.size} 个动作 · $totalSets 组", color = AppGreen, fontWeight = FontWeight.Black)
                workout.exercises.chunked(3).forEach { rowExercises ->
                    HistorySetCardsRow(
                        cards = rowExercises.map { exercise ->
                            HistorySetCardData(
                                title = exercise.name,
                                sets = exercise.sets
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun dateString(year: Int, monthNumber: Int, day: Int): String =
    String.format(Locale.US, "%04d-%02d-%02d", year, monthNumber, day)

private fun monthShiftDate(year: Int, monthNumber: Int, delta: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, monthNumber - 1 + delta)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return dateString(
        year = calendar.get(Calendar.YEAR),
        monthNumber = calendar.get(Calendar.MONTH) + 1,
        day = 1
    )
}

private fun todayDateString(): String {
    val today = Calendar.getInstance()
    return dateString(
        year = today.get(Calendar.YEAR),
        monthNumber = today.get(Calendar.MONTH) + 1,
        day = today.get(Calendar.DAY_OF_MONTH)
    )
}
