package com.example.gym_app

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    session: AuthSession,
    workouts: Map<String, WorkoutDay>,
    exerciseLibrary: List<ExerciseDefinition>,
    onSessionChange: (AuthSession) -> Unit,
    onImportData: (AppDataSnapshot) -> Unit,
    onClearData: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nickname by rememberSaveable(session.user.id) { mutableStateOf(session.user.nickname) }
    var saveMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var updateMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUpdate by rememberSaveable { mutableStateOf<AppUpdateInfo?>(null) }
    var downloading by rememberSaveable { mutableStateOf(false) }
    var downloadStatus by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClearData by rememberSaveable { mutableStateOf(false) }
    var dataMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var autoSync by rememberSaveable { mutableStateOf(true) }
    var denseHistory by rememberSaveable { mutableStateOf(true) }
    val currentVersionCode = rememberVersionCode()
    val currentVersionLabel = rememberVersionLabel()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(AppDataJson.encode(workouts, exerciseLibrary).toByteArray(Charsets.UTF_8))
                    } ?: error("无法写入文件")
                }.onSuccess {
                    dataMessage = "已导出"
                }.onFailure {
                    dataMessage = it.message ?: "导出失败"
                }
            }
        }
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                runCatching {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("无法读取文件")
                    AppDataJson.decode(text)
                }.onSuccess { snapshot ->
                    onImportData(snapshot)
                    dataMessage = "已导入"
                }.onFailure {
                    dataMessage = it.message ?: "导入失败"
                }
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(label = "我的", title = session.user.nickname, meta = "@${session.user.username}")

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("昵称", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlatTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = "昵称"
                )
                IosActionButton(
                    text = "保存昵称",
                    modifier = Modifier.fillMaxWidth(),
                    style = IosButtonStyle.Secondary,
                    onClick = {
                        scope.launch {
                            saveMessage = null
                            val result = CloudflareSyncClient.updateProfile(session.token, nickname)
                            val user = result.user
                            if (user != null) {
                                onSessionChange(session.copy(user = user))
                                saveMessage = "已保存"
                            } else {
                                saveMessage = result.error ?: "保存失败"
                            }
                        }
                    }
                )
                saveMessage?.let {
                    Text(it, color = if (it == "已保存") AppGreen else Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("设置", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                SettingSwitchRow("自动同步", autoSync, onChange = { autoSync = it })
                SettingSwitchRow("紧凑历史", denseHistory, onChange = { denseHistory = it })
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("数据", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IosActionButton(
                        text = "导出",
                        modifier = Modifier.weight(1f),
                        style = IosButtonStyle.Secondary,
                        onClick = { exportLauncher.launch("gym-data.json") }
                    )
                    IosActionButton(
                        text = "导入",
                        modifier = Modifier.weight(1f),
                        style = IosButtonStyle.Secondary,
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
                    )
                }
                IosActionButton(
                    text = "清空数据",
                    modifier = Modifier.fillMaxWidth(),
                    style = IosButtonStyle.Destructive,
                    onClick = { confirmClearData = true }
                )
                dataMessage?.let {
                    Text(it, color = if (it == "已导出" || it == "已导入") AppGreen else Color(0xFFFF3B30), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("软件更新", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("当前版本 $currentVersionLabel", color = AppText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                IosActionButton(
                    text = if (downloading) "下载中..." else "检查更新",
                    modifier = Modifier.fillMaxWidth(),
                    style = IosButtonStyle.Secondary,
                    enabled = !downloading,
                    onClick = {
                        scope.launch {
                            updateMessage = null
                            val result = CloudflareSyncClient.checkUpdate(currentVersionCode)
                            val update = result.update
                            when {
                                update == null -> updateMessage = result.error ?: "检查失败"
                                update.hasUpdate -> pendingUpdate = update
                                else -> updateMessage = "已是最新版本"
                            }
                        }
                    }
                )
                updateMessage?.let {
                    Text(it, color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                downloadStatus?.let {
                    Text(it, color = AppBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        GlassPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("账户", color = AppMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(session.user.id, color = AppText, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
                IosActionButton(
                    text = "退出登录",
                    modifier = Modifier.fillMaxWidth(),
                    style = IosButtonStyle.Destructive,
                    onClick = onLogout
                )
            }
        }
    }

    pendingUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { pendingUpdate = null },
            title = { Text("发现新版本 ${update.latestVersionName}", fontWeight = FontWeight.Black, color = AppText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    update.releaseNotes.forEach { note ->
                        Text(note, color = AppMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = update.apkUrl
                        pendingUpdate = null
                        downloading = true
                        downloadStatus = "正在下载..."
                        ApkInstaller.downloadAndInstall(context, url) { status ->
                            downloadStatus = status
                            if (status != "正在下载...") downloading = false
                        }
                    }
                ) {
                    Text("下载", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUpdate = null }) {
                    Text("取消", color = AppMuted, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (confirmClearData) {
        AlertDialog(
            onDismissRequest = { confirmClearData = false },
            title = { Text("清空数据", fontWeight = FontWeight.Black, color = AppText) },
            text = { Text("会清空当前账号的训练记录和动作库，并同步到云端。", color = AppMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClearData = false
                        onClearData()
                        dataMessage = "已清空"
                    }
                ) {
                    Text("清空", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearData = false }) {
                    Text("取消", color = AppBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppBlue,
                checkedBorderColor = AppBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE2E8F0),
                uncheckedBorderColor = AppHairline
            )
        )
    }
}

@Composable
private fun rememberVersionCode(): Long {
    val context = LocalContext.current
    return rememberSaveable {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }
}

@Composable
private fun rememberVersionLabel(): String {
    val context = LocalContext.current
    return rememberSaveable {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "1.0"
    }
}
