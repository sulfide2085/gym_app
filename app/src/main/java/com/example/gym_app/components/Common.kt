package com.example.gym_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassShape = RoundedCornerShape(26.dp)

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(18.dp, GlassShape, clip = false, ambientColor = Color(0x22000000), spotColor = Color(0x18000000))
            .background(AppGlass, GlassShape)
            .border(BorderStroke(1.dp, AppLine), GlassShape)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun ScreenHeader(label: String, title: String, meta: String) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Column {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppBlue)
            Text(title, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Black, color = AppText)
            Spacer(Modifier.height(4.dp))
            Text(meta, color = AppMuted, fontSize = 14.sp)
        }
    }
}

@Composable
fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    TextField(
        modifier = modifier
            .height(56.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x14000000), spotColor = Color(0x0F000000))
            .border(BorderStroke(1.dp, AppLine), RoundedCornerShape(18.dp)),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = AppMuted) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppGlassStrong,
            unfocusedContainerColor = AppGlass,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = AppBlue
        )
    )
}

@Composable
fun ModeButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(if (selected) 4.dp else 0.dp, shape, clip = false)
            .clip(shape)
            .background(if (selected) AppBlue else AppGlass, shape)
            .border(BorderStroke(1.dp, if (selected) Color.Transparent else AppLine), shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Bold, color = if (selected) Color.White else AppText)
    }
}

@Composable
fun PartFilterRows(
    parts: List<String>,
    selectedPart: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        parts.chunked(5).forEach { rowParts ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowParts.forEach { part ->
                    ModeButton(
                        text = part,
                        selected = selectedPart == part,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(part) }
                    )
                }
                repeat(5 - rowParts.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

fun formatDuration(milliseconds: Long): String {
    val totalCentiseconds = milliseconds / 10
    val minutes = totalCentiseconds / 6000
    val seconds = (totalCentiseconds % 6000) / 100
    val centiseconds = totalCentiseconds % 100
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}
