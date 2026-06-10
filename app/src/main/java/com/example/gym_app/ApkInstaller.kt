package com.example.gym_app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {
    private var downloadId: Long = -1
    private var onCompleteReceiver: BroadcastReceiver? = null

    fun downloadAndInstall(context: Context, url: String, onStatusChange: (String) -> Unit) {
        cleanup(context)

        val fileName = "gym-app-update.apk"
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("健身记录 更新")
            .setDescription("正在下载新版本...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)
        onStatusChange("正在下载...")

        onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    ctx.unregisterReceiver(this)
                    onCompleteReceiver = null

                    val file = File(
                        ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )
                    if (file.exists()) {
                        installApk(ctx, file)
                        onStatusChange("下载完成，正在安装...")
                    } else {
                        onStatusChange("下载失败")
                    }
                }
            }
        }
        context.registerReceiver(
            onCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    private fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun cleanup(context: Context) {
        onCompleteReceiver?.let {
            runCatching { context.unregisterReceiver(it) }
            onCompleteReceiver = null
        }
        downloadId = -1
    }
}
