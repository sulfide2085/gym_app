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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GymBuddyApp() {
    var activeTab by rememberSaveable { mutableStateOf(AppTab.Today) }
    var workoutTitle by rememberSaveable { mutableStateOf("今日训练") }
    var exercises by remember {
        mutableStateOf(
            listOf(
                WorkoutExercise(
                    id = 1,
                    name = "高位下拉",
                    bodyPart = "背部",
                    equipment = "器械",
                    sets = listOf(
                        SetEntry(id = 1, weight = "45", reps = "12", completed = true),
                        SetEntry(id = 2, weight = "50", reps = "10", completed = false)
                    )
                )
            )
        )
    }
    var exerciseLibrary by remember {
        mutableStateOf(
            listOf(
                ExerciseDefinition(1, "高位下拉", "背部", "器械"),
                ExerciseDefinition(2, "卧推", "胸部", "杠铃"),
                ExerciseDefinition(3, "深蹲", "腿部", "杠铃"),
                ExerciseDefinition(4, "平板支撑", "核心", "自重"),
                ExerciseDefinition(5, "哑铃卧推", "胸部", "哑铃"),
                ExerciseDefinition(6, "上斜卧推", "胸部", "杠铃"),
                ExerciseDefinition(7, "绳索夹胸", "胸部", "绳索"),
                ExerciseDefinition(8, "俯卧撑", "胸部", "自重"),
                ExerciseDefinition(9, "坐姿划船", "背部", "器械"),
                ExerciseDefinition(10, "杠铃划船", "背部", "杠铃"),
                ExerciseDefinition(11, "引体向上", "背部", "自重"),
                ExerciseDefinition(12, "硬拉", "背部", "杠铃"),
                ExerciseDefinition(13, "腿举", "腿部", "器械"),
                ExerciseDefinition(14, "罗马尼亚硬拉", "腿部", "杠铃"),
                ExerciseDefinition(15, "箭步蹲", "腿部", "哑铃"),
                ExerciseDefinition(16, "腿弯举", "腿部", "器械"),
                ExerciseDefinition(17, "提踵", "腿部", "器械"),
                ExerciseDefinition(18, "肩推", "肩部", "哑铃"),
                ExerciseDefinition(19, "侧平举", "肩部", "哑铃"),
                ExerciseDefinition(20, "面拉", "肩部", "绳索"),
                ExerciseDefinition(21, "反向飞鸟", "肩部", "哑铃"),
                ExerciseDefinition(22, "二头弯举", "手臂", "哑铃"),
                ExerciseDefinition(23, "锤式弯举", "手臂", "哑铃"),
                ExerciseDefinition(24, "绳索下压", "手臂", "绳索"),
                ExerciseDefinition(25, "臂屈伸", "手臂", "自重"),
                ExerciseDefinition(26, "卷腹", "核心", "自重"),
                ExerciseDefinition(27, "悬垂举腿", "核心", "自重"),
                ExerciseDefinition(28, "俄罗斯转体", "核心", "负重"),
                ExerciseDefinition(29, "跑步机", "有氧", "器械"),
                ExerciseDefinition(30, "椭圆机", "有氧", "器械"),
                ExerciseDefinition(31, "划船机", "有氧", "器械"),
                ExerciseDefinition(32, "战绳", "有氧", "器械")
            )
        )
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
                    title = workoutTitle,
                    onTitleChange = { workoutTitle = it },
                    exercises = exercises,
                    onExercisesChange = { exercises = it },
                    library = exerciseLibrary
                )
                AppTab.Timer -> TimerScreen()
                AppTab.Exercises -> ExerciseLibraryScreen(
                    exercises = exerciseLibrary,
                    onExercisesChange = { exerciseLibrary = it }
                )
                AppTab.Calendar -> CalendarScreen(
                    todayTitle = workoutTitle,
                    todayExercises = exercises
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
            .shadow(18.dp, RoundedCornerShape(30.dp), clip = false, ambientColor = Color(0x24000000), spotColor = Color(0x18000000))
            .background(AppGlass, RoundedCornerShape(30.dp))
            .border(BorderStroke(1.dp, AppLine), RoundedCornerShape(30.dp))
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
                    contentColor = if (active) Color.White else AppMuted
                ),
                shape = RoundedCornerShape(26.dp),
                onClick = { onSelect(tab) }
            ) {
                Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
