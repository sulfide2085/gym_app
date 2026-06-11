package com.example.gym_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun ExerciseLibraryScreen(
    exercises: List<ExerciseDefinition>,
    workouts: Map<String, WorkoutDay>,
    onExercisesChange: (List<ExerciseDefinition>) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedPart by rememberSaveable { mutableStateOf("全部") }
    var editingExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }
    var showCreate by rememberSaveable { mutableStateOf(false) }

    val parts = listOf("全部") + BodyPartOptions
    val filtered = exercises.filter {
        (selectedPart == "全部" || it.bodyPart == selectedPart) &&
            (search.isBlank() || it.name.contains(search) || it.bodyPart.contains(search) || it.equipment.contains(search))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(label = "动作库", title = "全部动作", meta = "${filtered.size}/${exercises.size} 个动作")
        FlatTextField(value = search, onValueChange = { search = it }, placeholder = "搜索动作")
        PartFilterRows(parts = parts, selectedPart = selectedPart, onSelect = { selectedPart = it })

        filtered.forEach { exercise ->
            ExerciseRow(
                exercise = exercise,
                recentDays = recentWorkoutsForExercise(workouts, exercise.name),
                onEdit = { editingExercise = exercise },
                onDelete = { onExercisesChange(exercises.filterNot { it.id == exercise.id }) }
            )
        }

        IosActionButton(
            text = "新增动作",
            modifier = Modifier.fillMaxWidth(),
            onClick = { showCreate = true }
        )
    }

    if (showCreate) {
        ExerciseFormDialog(
            title = "新增动作",
            initial = ExerciseDefinition(System.nanoTime(), "", selectedPart.takeUnless { it == "全部" } ?: "其他", "机器"),
            onDismiss = { showCreate = false },
            onSave = { next ->
                onExercisesChange(exercises + next.copy(name = next.name.ifBlank { "新动作" }))
                showCreate = false
            }
        )
    }

    editingExercise?.let { exercise ->
        ExerciseFormDialog(
            title = "编辑动作",
            initial = exercise,
            onDismiss = { editingExercise = null },
            onSave = { next ->
                onExercisesChange(exercises.map { if (it.id == exercise.id) next else it })
                editingExercise = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(
    exercise: ExerciseDefinition,
    recentDays: List<WorkoutDay>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var dragOffset by remember(exercise.id) { mutableFloatStateOf(0f) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val revealWidth = 96.dp
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset.coerceIn(-revealPx, 0f),
        label = "exerciseRowSwipe"
    )
    val rowShape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
    ) {
        if (animatedOffset < -1f) Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFFF3B30), rowShape),
                contentAlignment = Alignment.Center
            ) {
                TrashCanIcon(
                    modifier = Modifier.size(width = 28.dp, height = 30.dp),
                    tint = Color.White
                )
            }
        }

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(exercise.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val shouldDelete = dragOffset < -revealPx * 0.55f
                            dragOffset = 0f
                            if (shouldDelete) confirmDelete = true
                        },
                        onDragCancel = { dragOffset = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset = (dragOffset + dragAmount).coerceIn(-revealPx, 0f)
                        }
                    )
                }
                .combinedClickable(
                    onClick = {},
                    onLongClick = onEdit
                )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(AppBlueSoft, RoundedCornerShape(14.dp))
                            .border(androidx.compose.foundation.BorderStroke(1.dp, AppLine), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(exercise.bodyPart.take(1), fontWeight = FontWeight.Black, color = AppBlue)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(exercise.name, fontWeight = FontWeight.Black, color = AppText, lineHeight = 20.sp)
                            if (exercise.isStretching) {
                                Spacer(Modifier.width(6.dp))
                                Text("拉伸", color = AppBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("${exercise.bodyPart} / ${exercise.equipment}", color = AppMuted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }

                if (recentDays.isEmpty()) {
                    Text("暂无训练记录", color = AppMuted, fontSize = 12.sp)
                } else {
                    HistorySetCardsRow(
                        cards = recentDays.take(3).map { day ->
                            val trained = day.exercises.first { it.name == exercise.name }
                            HistorySetCardData(
                                title = day.date,
                                sets = trained.sets
                            )
                        }
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = {
                confirmDelete = false
                dragOffset = 0f
            },
            title = { Text("删除动作", fontWeight = FontWeight.Black, color = AppText) },
            text = { Text("确定删除「${exercise.name}」吗？训练历史不会被删除。", color = AppMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        dragOffset = 0f
                        onDelete()
                    }
                ) {
                    Text("删除", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        dragOffset = 0f
                    }
                ) {
                    Text("取消", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ExerciseFormDialog(
    title: String,
    initial: ExerciseDefinition,
    onDismiss: () -> Unit,
    onSave: (ExerciseDefinition) -> Unit
) {
    var name by rememberSaveable(initial.id) { mutableStateOf(initial.name) }
    var bodyPart by rememberSaveable(initial.id) { mutableStateOf(initial.bodyPart) }
    var equipment by rememberSaveable(initial.id) { mutableStateOf(initial.equipment) }
    var isStretching by rememberSaveable(initial.id) { mutableStateOf(initial.isStretching) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black, color = AppText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlatTextField(value = name, onValueChange = { name = it }, placeholder = "动作名称")
                Text("分类", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OptionRows(options = BodyPartOptions, selected = bodyPart, onSelect = { bodyPart = it })
                Text("器械", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OptionRows(options = EquipmentOptions, selected = equipment, onSelect = { equipment = it })
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("拉伸动作", color = AppText, fontWeight = FontWeight.Bold)
                        Text("开启后无需输入重量", color = AppMuted, fontSize = 12.sp)
                    }
                    ModeButton(
                        text = if (isStretching) "是" else "否",
                        selected = isStretching,
                        modifier = Modifier.weight(1f),
                        onClick = { isStretching = !isStretching }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    onSave(initial.copy(name = name.trim(), bodyPart = bodyPart, equipment = equipment, isStretching = isStretching))
                }
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(shape = RoundedCornerShape(16.dp), onClick = onDismiss) {
                Text("取消", color = AppMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}
