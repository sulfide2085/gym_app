package com.example.gym_app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthenticated: (AuthSession) -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AppBgTop, AppBg, AppBgBottom)))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text("Gym", color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (isRegisterMode) "创建账户" else "登录账户",
                        color = AppText,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                FlatTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "用户名"
                )
                PasswordField(
                    value = password,
                    onValueChange = { password = it }
                )

                errorText?.let {
                    Text(it, color = Color(0xFFFF3B30), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                IosActionButton(
                    text = if (loading) "请稍候" else if (isRegisterMode) "注册并登录" else "登录",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (loading) return@IosActionButton
                        loading = true
                        errorText = null
                        scope.launch {
                            val result = if (isRegisterMode) {
                                CloudflareSyncClient.register(username, password)
                            } else {
                                CloudflareSyncClient.login(username, password)
                            }
                            loading = false
                            result.session?.let(onAuthenticated)
                            if (result.session == null) {
                                errorText = result.error ?: "认证失败"
                            }
                        }
                    }
                )

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorText = null
                    }
                ) {
                    Text(
                        if (isRegisterMode) "已有账户，去登录" else "没有账户，创建一个",
                        color = AppBlue,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit
) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x180B1220), spotColor = Color(0x120B1220))
            .border(BorderStroke(1.dp, AppLine), RoundedCornerShape(18.dp))
            .border(BorderStroke(0.6.dp, AppHairline), RoundedCornerShape(18.dp)),
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("密码", color = AppMuted) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
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
