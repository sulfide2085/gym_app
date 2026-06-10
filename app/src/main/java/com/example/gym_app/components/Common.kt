package com.example.gym_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
            .shadow(22.dp, GlassShape, clip = false, ambientColor = Color(0x260B1220), spotColor = Color(0x1F0B1220))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF2FFFFFF), AppGlass, Color(0xAFFFFFFF))
                ),
                GlassShape
            )
            .border(BorderStroke(1.dp, AppLine), GlassShape)
            .border(BorderStroke(0.6.dp, AppHairline), GlassShape)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun ScreenHeader(label: String, title: String, meta: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AppMuted)
        Text(title, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black, color = AppText)
        Spacer(Modifier.height(2.dp))
        Text(meta, color = AppMuted, fontSize = 14.sp)
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
            .height(58.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x180B1220), spotColor = Color(0x120B1220))
            .border(BorderStroke(1.dp, AppLine), RoundedCornerShape(18.dp))
            .border(BorderStroke(0.6.dp, AppHairline), RoundedCornerShape(18.dp)),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = AppMuted) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = AppGlassStrong,
            unfocusedContainerColor = AppGlassStrong,
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
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(if (pressed) 0.96f else 1f, label = "modeButtonPress").value
    Box(
        modifier = modifier
            .height(46.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.82f else 1f
            }
            .shadow(if (selected) 8.dp else 2.dp, shape, clip = false, ambientColor = Color(0x180B1220), spotColor = Color(0x120B1220))
            .clip(shape)
            .background(if (selected) AppBlue else AppGlassStrong, shape)
            .border(BorderStroke(1.dp, if (selected) Color.Transparent else AppHairline), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = text },
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 14.sp, color = if (selected) Color.White else AppText)
    }
}

@Composable
fun IosActionButton(
    text: String,
    modifier: Modifier = Modifier,
    style: IosButtonStyle = IosButtonStyle.Primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(if (pressed) 0.97f else 1f, label = "iosButtonPress").value
    val background = when (style) {
        IosButtonStyle.Primary -> AppBlue
        IosButtonStyle.Secondary -> Color.Transparent
        IosButtonStyle.Destructive -> Color(0xFFFFE5E5)
    }
    val textColor = when (style) {
        IosButtonStyle.Primary -> Color.White
        IosButtonStyle.Secondary -> AppBlue
        IosButtonStyle.Destructive -> Color(0xFFFF3B30)
    }
    val enabledAlpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.78f else enabledAlpha
            }
            .clip(shape)
            .background(background, shape)
            .border(BorderStroke(1.dp, if (style == IosButtonStyle.Primary) Color.Transparent else AppHairline), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor.copy(alpha = enabledAlpha), fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun IosIconButton(
    modifier: Modifier = Modifier,
    style: IosButtonStyle = IosButtonStyle.Secondary,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val scale = animateFloatAsState(if (pressed) 0.94f else 1f, label = "iosIconButtonPress").value
    val background = when (style) {
        IosButtonStyle.Primary -> AppBlue
        IosButtonStyle.Secondary -> AppGlassStrong
        IosButtonStyle.Destructive -> Color(0xFFFFE5E5)
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.78f else 1f
            }
            .clip(shape)
            .background(background, shape)
            .border(BorderStroke(1.dp, if (style == IosButtonStyle.Primary) Color.Transparent else AppHairline), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

enum class IosButtonStyle {
    Primary,
    Secondary,
    Destructive
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

@Composable
fun OptionRows(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(columns).forEach { rowOptions ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowOptions.forEach { option ->
                    ModeButton(
                        text = option,
                        selected = selected == option,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(option) }
                    )
                }
                repeat(columns - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HistorySetCard(
    title: String,
    sets: List<SetEntry>,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Column(
        modifier = modifier
            .background(AppGlassStrong, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, AppHairline), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            title,
            color = AppText,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Black
        )
        subtitle?.let {
            Text(it, color = AppMuted, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold)
        }
        sets.forEach { set ->
            HistorySetRow(set = set)
        }
    }
}

data class HistorySetCardData(
    val title: String,
    val sets: List<SetEntry>,
    val subtitle: String? = null
)

@Composable
fun HistorySetCardsRow(
    cards: List<HistorySetCardData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards.take(3).forEach { card ->
            HistorySetCard(
                title = card.title,
                sets = card.sets,
                subtitle = card.subtitle,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
        repeat(3 - cards.take(3).size) {
            Spacer(modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun HistorySetRow(set: SetEntry) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = set.historyValueText(),
        color = AppMuted,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun TrashCanIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFFF3B30)
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 2.15.dp.toPx())
        val bodyTop = size.height * 0.34f
        val bodyLeft = size.width * 0.2f
        val bodyWidth = size.width * 0.6f
        val bodyHeight = size.height * 0.56f

        drawLine(
            color = tint,
            start = Offset(size.width * 0.1f, size.height * 0.22f),
            end = Offset(size.width * 0.9f, size.height * 0.22f),
            strokeWidth = stroke.width
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.38f, size.height * 0.1f),
            end = Offset(size.width * 0.62f, size.height * 0.1f),
            strokeWidth = stroke.width
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
            style = stroke
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.43f, size.height * 0.44f),
            end = Offset(size.width * 0.43f, size.height * 0.77f),
            strokeWidth = stroke.width
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.57f, size.height * 0.44f),
            end = Offset(size.width * 0.57f, size.height * 0.77f),
            strokeWidth = stroke.width
        )
    }
}

private fun SetEntry.historyValueText(): String {
    val weight = weight.removeSuffix(".0").ifBlank { "-" }
    val reps = reps.ifBlank { "-" }
    return "${weight}kg x $reps"
}

fun formatDuration(milliseconds: Long): String {
    val totalCentiseconds = milliseconds / 10
    val minutes = totalCentiseconds / 6000
    val seconds = (totalCentiseconds % 6000) / 100
    val centiseconds = totalCentiseconds % 100
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}
