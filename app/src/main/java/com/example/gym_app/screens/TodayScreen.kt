package com.example.gym_app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    exercises: List<WorkoutExercise>,
    onExercisesChange: (List<WorkoutExercise>) -> Unit,
    library: List<ExerciseDefinition>
) {
    var showAddExercise by rememberSaveable { mutableStateOf(false) }
    val completed = exercises.sumOf { exercise -> exercise.sets.count { it.completed } }
    val total = exercises.sumOf { it.sets.size }
    val availableExercises = library.filter { candidate -> exercises.none { it.name == candidate.name } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(label = "TODAY", title = title, meta = if (total == 0) "未开始" else "$completed/$total 组完成")
        FlatTextField(value = title, onValueChange = onTitleChange, placeholder = "训练标题")

        exercises.forEachIndexed { exerciseIndex, exercise ->
            ExerciseEditorBlock(
                exercise = exercise,
                onAddSet = {
                    onExercisesChange(
                        exercises.updateAt(exerciseIndex) {
                            it.copy(sets = it.sets + SetEntry(id = System.nanoTime()))
                        }
                    )
                },
                onUpdateSet = { setIndex, nextSet ->
                    onExercisesChange(
                        exercises.updateAt(exerciseIndex) { item ->
                            item.copy(sets = item.sets.updateAt(setIndex) { nextSet })
                        }
                    )
                }
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = Color.White),
            shape = RoundedCornerShape(28.dp),
            onClick = {
                showAddExercise = true
            }
        ) {
            Text("添加动作", fontWeight = FontWeight.Black)
        }

        if (showAddExercise) {
            AddExerciseDialog(
                availableExercises = availableExercises,
                onDismiss = { showAddExercise = false },
                onSelect = { next ->
                    onExercisesChange(
                        exercises + WorkoutExercise(
                            id = System.nanoTime(),
                            name = next.name,
                            bodyPart = next.bodyPart,
                            equipment = next.equipment,
                            sets = listOf(SetEntry(id = System.nanoTime()))
                        )
                    )
                    showAddExercise = false
                }
            )
        }
    }
}

@Composable
private fun AddExerciseDialog(
    availableExercises: List<ExerciseDefinition>,
    onDismiss: () -> Unit,
    onSelect: (ExerciseDefinition) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visibleExercises = availableExercises.filter {
        query.isBlank() ||
            it.name.contains(query) ||
            it.bodyPart.contains(query) ||
            it.equipment.contains(query)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择动作", fontWeight = FontWeight.Black, color = AppText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlatTextField(value = query, onValueChange = { query = it }, placeholder = "搜索动作、部位或器械")
                if (availableExercises.isEmpty()) {
                    Text("动作库里的动作都已经加入今日训练。", color = AppMuted)
                } else if (visibleExercises.isEmpty()) {
                    Text("没有匹配的动作。", color = AppMuted)
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        visibleExercises.forEach { exercise ->
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                onClick = { onSelect(exercise) }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(exercise.name, fontWeight = FontWeight.Black, color = AppText)
                                    Text("${exercise.bodyPart} / ${exercise.equipment}", color = AppMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = AppBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ExerciseEditorBlock(
    exercise: WorkoutExercise,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, SetEntry) -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = AppText)
                    Text("${exercise.bodyPart} / ${exercise.equipment}", fontSize = 13.sp, color = AppMuted)
                }
                Text("${exercise.sets.count { it.completed }}/${exercise.sets.size}", color = AppGreen, fontWeight = FontWeight.Black)
            }

            exercise.sets.forEachIndexed { index, set ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", modifier = Modifier.width(24.dp), fontWeight = FontWeight.Black, color = AppMuted)
                    FlatTextField(
                        modifier = Modifier.weight(1f),
                        value = set.weight,
                        onValueChange = { onUpdateSet(index, set.copy(weight = it.filter { char -> char.isDigit() || char == '.' })) },
                        placeholder = "kg"
                    )
                    FlatTextField(
                        modifier = Modifier.weight(1f),
                        value = set.reps,
                        onValueChange = { onUpdateSet(index, set.copy(reps = it.filter(Char::isDigit))) },
                        placeholder = "次"
                    )
                    Button(
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (set.completed) AppGreen else AppGlassStrong,
                            contentColor = if (set.completed) Color.White else AppMuted
                        ),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { onUpdateSet(index, set.copy(completed = !set.completed)) }
                    ) {
                        Text("✓", fontWeight = FontWeight.Black)
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                onClick = onAddSet
            ) {
                Text("新增一组", fontWeight = FontWeight.Bold, color = AppBlue)
            }
        }
    }
}
