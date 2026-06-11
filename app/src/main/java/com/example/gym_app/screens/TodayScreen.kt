package com.example.gym_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TodayScreen(
    date: String,
    title: String,
    onTitleChange: (String) -> Unit,
    exercises: List<WorkoutExercise>,
    onExercisesChange: (List<WorkoutExercise>) -> Unit,
    history: Map<String, WorkoutDay>,
    library: List<ExerciseDefinition>
) {
    var showAddExercise by rememberSaveable { mutableStateOf(false) }
    val total = exercises.sumOf { it.sets.size }
    val filledSets = exercises.sumOf { exercise ->
        exercise.sets.count { it.weight.isNotBlank() || it.reps.isNotBlank() }
    }
    val availableExercises = library.filter { candidate -> exercises.none { it.name == candidate.name } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(label = date, title = title, meta = if (total == 0) "还没有训练内容" else "${exercises.size} 个动作 · $total 组")
        SummaryStrip(
            exerciseCount = exercises.size,
            total = total,
            filledSets = filledSets
        )
        FlatTextField(value = title, onValueChange = onTitleChange, placeholder = "训练标题")

        exercises.forEachIndexed { exerciseIndex, exercise ->
            val isStretching = library.any { it.name == exercise.name && it.isStretching }
            ExerciseEditorBlock(
                exercise = exercise,
                isStretching = isStretching,
                recentDays = recentWorkoutsForExercise(history, exercise.name, limit = 4)
                    .filterNot { it.date == date }
                    .take(3),
                onAddSet = {
                    onExercisesChange(
                        exercises.updateAt(exerciseIndex) {
                            val last = it.sets.lastOrNull()
                            it.copy(sets = it.sets + SetEntry(
                                id = System.nanoTime(),
                                weight = if (isStretching) "" else (last?.weight ?: ""),
                                reps = last?.reps ?: ""
                            ))
                        }
                    )
                },
                onUpdateSet = { setIndex, nextSet ->
                    onExercisesChange(
                        exercises.updateAt(exerciseIndex) { item ->
                            item.copy(sets = item.sets.updateAt(setIndex) { nextSet })
                        }
                    )
                },
                onRemoveSet = { setIndex ->
                    onExercisesChange(
                        exercises.updateAt(exerciseIndex) { item ->
                            item.copy(sets = item.sets.filterIndexed { currentIndex, _ -> currentIndex != setIndex })
                        }
                    )
                },
                onRemove = {
                    onExercisesChange(exercises.filterNot { it.id == exercise.id })
                }
            )
        }

        IosActionButton(
            text = "添加动作",
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            onClick = { showAddExercise = true }
        )

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
private fun SummaryStrip(exerciseCount: Int, total: Int, filledSets: Int) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryMetric("动作", "${exerciseCount} 个", Modifier.weight(1f))
            SummaryMetric("组数", "$total", Modifier.weight(1f))
            SummaryMetric("已填", "$filledSets", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = AppText, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun AddExerciseDialog(
    availableExercises: List<ExerciseDefinition>,
    onDismiss: () -> Unit,
    onSelect: (ExerciseDefinition) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedPart by rememberSaveable { mutableStateOf("全部") }
    val parts = listOf("全部") + BodyPartOptions
    val visibleExercises = availableExercises.filter {
        val matchesPart = selectedPart == "全部" || it.bodyPart == selectedPart
        val matchesQuery = query.isBlank() ||
            it.name.contains(query) ||
            it.bodyPart.contains(query) ||
            it.equipment.contains(query)
        matchesPart && matchesQuery
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择动作", fontWeight = FontWeight.Black, color = AppText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlatTextField(value = query, onValueChange = { query = it }, placeholder = "搜索动作、部位或器械")
                PartFilterRows(parts = parts, selectedPart = selectedPart, onSelect = { selectedPart = it })
                if (visibleExercises.isEmpty()) {
                    Text("没有可添加的动作。", color = AppMuted)
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
                                shape = RoundedCornerShape(16.dp),
                                onClick = { onSelect(exercise) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ExerciseInitial(exercise.bodyPart)
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(exercise.name, fontWeight = FontWeight.Black, color = AppText)
                                            if (exercise.isStretching) {
                                                Spacer(Modifier.width(6.dp))
                                                Text("拉伸", color = AppBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("${exercise.bodyPart} / ${exercise.equipment}", color = AppMuted, fontSize = 12.sp)
                                    }
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
    isStretching: Boolean,
    recentDays: List<WorkoutDay>,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, SetEntry) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onRemove: () -> Unit
) {
    var confirmRemove by rememberSaveable { mutableStateOf(false) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExerciseInitial(exercise.bodyPart)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = AppText)
                    Text("${exercise.bodyPart} / ${exercise.equipment}", fontSize = 12.sp, color = AppMuted)
                }
                IosIconButton(
                    modifier = Modifier.size(42.dp),
                    style = IosButtonStyle.Secondary,
                    onClick = { confirmRemove = true }
                ) {
                    TrashCanIcon(modifier = Modifier.size(width = 16.dp, height = 18.dp))
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exercise.sets.forEachIndexed { index, set ->
                    SetEditorRow(
                        index = index + 1,
                        set = set,
                        isStretching = isStretching,
                        onWeightChange = {
                            onUpdateSet(index, set.copy(weight = it.filter { char -> char.isDigit() || char == '.' }))
                        },
                        onRepsChange = {
                            onUpdateSet(index, set.copy(reps = it.filter(Char::isDigit)))
                        },
                        onRemove = { onRemoveSet(index) }
                    )
                }
            }

            IosActionButton(
                text = "新增一组",
                modifier = Modifier.fillMaxWidth(),
                style = IosButtonStyle.Secondary,
                onClick = onAddSet
            )

            if (recentDays.isNotEmpty()) {
                RecentHistoryRow(exercise.name, recentDays)
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("移除动作", fontWeight = FontWeight.Black, color = AppText) },
            text = { Text("从这次训练移除「${exercise.name}」吗？动作库和历史记录不会被删除。", color = AppMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemove()
                    }
                ) {
                    Text("移除", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("取消", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SetEditorRow(
    index: Int,
    set: SetEntry,
    isStretching: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    val rowShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x78FFFFFF), rowShape)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 34.dp, height = 42.dp)
                .background(AppBlueSoft, RoundedCornerShape(14.dp))
                .border(BorderStroke(1.dp, AppHairline), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("$index", color = AppBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        if (!isStretching) {
            SetValuePill(
                value = set.weight,
                unit = "kg",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
                onValueChange = onWeightChange
            )
        }
        SetValuePill(
            value = set.reps,
            unit = if (isStretching) "秒" else "次",
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f),
            onValueChange = onRepsChange
        )
        IosIconButton(
            modifier = Modifier.size(42.dp),
            style = IosButtonStyle.Secondary,
            onClick = onRemove
        ) {
            TrashCanIcon(modifier = Modifier.size(width = 15.dp, height = 17.dp))
        }
    }
}

@Composable
private fun SetValuePill(
    value: String,
    unit: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .height(42.dp)
            .background(AppGlassStrong, shape)
            .border(BorderStroke(1.dp, AppHairline), shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(
                color = AppText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            "0",
                            color = AppMuted.copy(alpha = 0.45f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(Modifier.width(4.dp))
        Text(unit, color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun RecentHistoryRow(exerciseName: String, recentDays: List<WorkoutDay>) {
    HistorySetCardsRow(
        cards = recentDays.take(3).map { day ->
            val exercise = day.exercises.first { it.name == exerciseName }
            HistorySetCardData(
                title = day.date,
                sets = exercise.sets
            )
        }
    )
}

@Composable
private fun ExerciseInitial(bodyPart: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(AppBlueSoft, RoundedCornerShape(14.dp))
            .border(androidx.compose.foundation.BorderStroke(1.dp, AppLine), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(bodyPart.take(1), fontWeight = FontWeight.Black, color = AppBlue)
    }
}
