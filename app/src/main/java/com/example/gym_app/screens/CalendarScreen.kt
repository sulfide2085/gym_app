package com.example.gym_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(
    todayTitle: String,
    todayExercises: List<WorkoutExercise>
) {
    val today = Calendar.getInstance()
    val year = today.get(Calendar.YEAR)
    val month = today.get(Calendar.MONTH)
    val todayDay = today.get(Calendar.DAY_OF_MONTH)
    var selectedDay by rememberSaveable { mutableIntStateOf(todayDay) }
    val monthStart = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = monthStart.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val trainingDays = setOf(2, 6, 8, 13, 21, todayDay)
    val calendarCells = List(leadingBlanks) { null } + (1..daysInMonth).map { it }
    val selectedSummary = buildWorkoutSummary(
        day = selectedDay,
        todayDay = todayDay,
        todayTitle = todayTitle,
        todayExercises = todayExercises,
        trainingDays = trainingDays
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(
            label = "REVIEW",
            title = String.format(Locale.US, "%04d.%02d", year, month + 1),
            meta = "${trainingDays.count { it <= daysInMonth }} 天训练"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(day, modifier = Modifier.weight(1f), color = AppMuted, fontWeight = FontWeight.Black)
            }
        }
        calendarCells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val trained = day in trainingDays
                        val isToday = day == todayDay
                        val isSelected = day == selectedDay
                        val shape = RoundedCornerShape(18.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(shape)
                                .background(if (trained) AppGreenSoft else AppGlass, shape)
                                .border(
                                    BorderStroke(
                                        if (isSelected || isToday) 2.dp else 1.dp,
                                        if (isSelected || isToday) AppBlue else AppLine
                                    ),
                                    shape
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$day",
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) AppBlue else if (trained) AppGreen else AppText
                            )
                        }
                    }
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }

        DayWorkoutPanel(summary = selectedSummary)
    }
}

@Composable
private fun DayWorkoutPanel(summary: DayWorkoutSummary?) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (summary == null) {
                Text("当日训练", fontWeight = FontWeight.Black, color = AppText, fontSize = 18.sp)
                Text("这一天还没有训练记录。", color = AppMuted)
            } else {
                Text("${summary.day} 日训练", fontWeight = FontWeight.Black, color = AppText, fontSize = 18.sp)
                Text(summary.title, color = AppMuted)
                Text("${summary.completedSets}/${summary.totalSets} 组完成", color = AppGreen, fontWeight = FontWeight.Black)
                summary.exercises.forEach { name ->
                    Text("• $name", color = AppText)
                }
            }
        }
    }
}

private fun buildWorkoutSummary(
    day: Int,
    todayDay: Int,
    todayTitle: String,
    todayExercises: List<WorkoutExercise>,
    trainingDays: Set<Int>
): DayWorkoutSummary? {
    if (day == todayDay) {
        return DayWorkoutSummary(
            day = day,
            title = todayTitle,
            completedSets = todayExercises.sumOf { exercise -> exercise.sets.count { it.completed } },
            totalSets = todayExercises.sumOf { it.sets.size },
            exercises = todayExercises.map { it.name }
        )
    }

    if (day !in trainingDays) {
        return null
    }

    val names = if (day % 2 == 0) listOf("卧推", "高位下拉") else listOf("深蹲", "平板支撑")
    return DayWorkoutSummary(
        day = day,
        title = "历史训练",
        completedSets = names.size * 2,
        totalSets = names.size * 2,
        exercises = names
    )
}
