package com.example.gym_app

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TimerScreen() {
    var repeatMode by rememberSaveable { mutableStateOf(false) }
    var running by rememberSaveable { mutableStateOf(false) }
    var baseMillis by rememberSaveable { mutableLongStateOf(0L) }
    var elapsedMillis by rememberSaveable { mutableLongStateOf(0L) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var targetSeconds by rememberSaveable { mutableStateOf("60") }
    var segments by remember { mutableStateOf(listOf<Long>()) }

    LaunchedEffect(running) {
        while (running) {
            elapsedMillis = baseMillis + (SystemClock.elapsedRealtime() - startedAt)
            delay(50)
        }
    }

    val targetMillis = ((targetSeconds.toLongOrNull() ?: 60L).coerceIn(5L, 3600L)) * 1000L
    val displayMillis = if (repeatMode) (targetMillis - elapsedMillis).coerceAtLeast(0L) else elapsedMillis
    val reached = repeatMode && elapsedMillis >= targetMillis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(label = "TIMER", title = "训练计时", meta = if (repeatMode) "清零并继续" else "普通计时")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("普通计时", !repeatMode, Modifier.weight(1f)) {
                repeatMode = false
                running = false
                baseMillis = 0L
                elapsedMillis = 0L
            }
            ModeButton("清零继续", repeatMode, Modifier.weight(1f)) {
                repeatMode = true
                running = false
                baseMillis = 0L
                elapsedMillis = 0L
            }
        }

        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(if (repeatMode) if (reached) "到点" else "剩余" else "已计时", color = AppMuted, fontWeight = FontWeight.Black)
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val timerFontSize = (maxWidth.value * 0.17f).coerceIn(44f, 76f).sp
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = formatDuration(displayMillis),
                        fontSize = timerFontSize,
                        lineHeight = timerFontSize,
                        fontWeight = FontWeight.Black,
                        color = if (reached) AppYellow else AppText,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                if (repeatMode) {
                    FlatTextField(value = targetSeconds, onValueChange = { targetSeconds = it.filter(Char::isDigit) }, placeholder = "目标秒数")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen, contentColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                onClick = {
                    if (running) {
                        baseMillis = elapsedMillis
                        running = false
                    } else {
                        startedAt = SystemClock.elapsedRealtime()
                        running = true
                    }
                }
            ) {
                Text(if (running) "暂停" else "开始", fontWeight = FontWeight.Black)
            }
            Button(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (repeatMode) AppYellow else AppGlassStrong,
                    contentColor = if (repeatMode) AppText else AppText
                ),
                shape = RoundedCornerShape(28.dp),
                onClick = {
                    if (repeatMode) {
                        segments = listOf(if (elapsedMillis > 0L) elapsedMillis else targetMillis) + segments
                        baseMillis = 0L
                        elapsedMillis = 0L
                        startedAt = SystemClock.elapsedRealtime()
                        running = true
                    } else {
                        baseMillis = 0L
                        elapsedMillis = 0L
                        startedAt = SystemClock.elapsedRealtime()
                    }
                }
            ) {
                Text(if (repeatMode) "清零并继续" else "清零", fontWeight = FontWeight.Black)
            }
        }

        if (repeatMode) {
            Text("最近分段", fontWeight = FontWeight.Black, color = AppText)
            segments.take(8).forEachIndexed { index, segment ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("第 ${segments.size - index} 段", color = AppMuted)
                    Text(formatDuration(segment), fontWeight = FontWeight.Black, color = AppText)
                }
            }
        }
    }
}
