package com.example.gym_app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExerciseLibraryScreen(
    exercises: List<ExerciseDefinition>,
    onExercisesChange: (List<ExerciseDefinition>) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    var selectedPart by rememberSaveable { mutableStateOf("全部") }
    var editingId by rememberSaveable { mutableLongStateOf(-1L) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var draftPart by rememberSaveable { mutableStateOf("") }
    var draftEquipment by rememberSaveable { mutableStateOf("") }

    val parts = listOf("全部", "胸部", "背部", "腿部", "肩部", "手臂", "核心", "有氧", "其他")
    val filtered = exercises.filter {
        (selectedPart == "全部" || it.bodyPart == selectedPart) &&
            (search.isBlank() || it.name.contains(search))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ScreenHeader(label = "LIBRARY", title = "全部动作", meta = "${filtered.size}/${exercises.size} 个动作")
        FlatTextField(value = search, onValueChange = { search = it }, placeholder = "搜索动作")

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEach { part ->
                ModeButton(part, selectedPart == part) {
                    selectedPart = part
                }
            }
        }

        filtered.forEach { exercise ->
            if (editingId == exercise.id) {
                ExerciseEditor(
                    name = draftName,
                    bodyPart = draftPart,
                    equipment = draftEquipment,
                    onNameChange = { draftName = it },
                    onBodyPartChange = { draftPart = it },
                    onEquipmentChange = { draftEquipment = it },
                    onCancel = { editingId = -1L },
                    onSave = {
                        val nextName = draftName.ifBlank { exercise.name }
                        val nextPart = draftPart.ifBlank { exercise.bodyPart }
                        val nextEquipment = draftEquipment.ifBlank { exercise.equipment }
                        onExercisesChange(
                            exercises.map {
                                if (it.id == exercise.id) {
                                    it.copy(name = nextName, bodyPart = nextPart, equipment = nextEquipment)
                                } else {
                                    it
                                }
                            }
                        )
                        editingId = -1L
                    }
                )
            } else {
                ExerciseRow(
                    exercise = exercise,
                    onEdit = {
                        editingId = exercise.id
                        draftName = exercise.name
                        draftPart = exercise.bodyPart
                        draftEquipment = exercise.equipment
                    },
                    onDelete = {
                        onExercisesChange(exercises.filterNot { it.id == exercise.id })
                    }
                )
            }
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = AppSurface),
            shape = RoundedCornerShape(28.dp),
            onClick = {
                val bodyPart = if (selectedPart == "全部") "其他" else selectedPart
                onExercisesChange(exercises + ExerciseDefinition(System.nanoTime(), "新动作", bodyPart, "自定义"))
            }
        ) {
            Text("新增动作", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseDefinition,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(AppBlueSoft, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(exercise.bodyPart.take(1), fontWeight = FontWeight.Black, color = AppBlue)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.name, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, color = AppText)
                    Text("${exercise.bodyPart} / ${exercise.equipment}", color = AppMuted, fontSize = 12.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    onClick = onEdit
                ) {
                    Text("编辑", color = AppBlue, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    onClick = onDelete
                ) {
                    Text("删除", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditor(
    name: String,
    bodyPart: String,
    equipment: String,
    onNameChange: (String) -> Unit,
    onBodyPartChange: (String) -> Unit,
    onEquipmentChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("编辑动作", fontWeight = FontWeight.Black, color = AppText)
            FlatTextField(value = name, onValueChange = onNameChange, placeholder = "动作名称")
            FlatTextField(value = bodyPart, onValueChange = onBodyPartChange, placeholder = "部位")
            FlatTextField(value = equipment, onValueChange = onEquipmentChange, placeholder = "器械")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    onClick = onCancel
                ) {
                    Text("取消", color = AppMuted, fontWeight = FontWeight.Bold)
                }
                Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlue, contentColor = Color.White),
                    onClick = onSave
                ) {
                    Text("保存", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
