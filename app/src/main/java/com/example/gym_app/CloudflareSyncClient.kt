package com.example.gym_app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class CloudflareGymState(
    val workouts: Map<String, WorkoutDay>,
    val exercises: List<ExerciseDefinition>,
    val updatedAt: Long
)

data class AuthUser(
    val id: String,
    val username: String,
    val nickname: String
)

data class AuthSession(
    val token: String,
    val user: AuthUser,
    val expiresAt: Long
)

data class AuthResult(
    val session: AuthSession?,
    val error: String?
)

data class ProfileResult(
    val user: AuthUser?,
    val error: String?
)

data class AppUpdateInfo(
    val latestVersionCode: Long,
    val latestVersionName: String,
    val hasUpdate: Boolean,
    val required: Boolean,
    val apkUrl: String,
    val releaseNotes: List<String>
)

data class UpdateResult(
    val update: AppUpdateInfo?,
    val error: String?
)

object CloudflareSyncClient {
    suspend fun register(username: String, password: String): AuthResult = authenticate(
        path = "/api/auth/register",
        username = username,
        password = password
    )

    suspend fun login(username: String, password: String): AuthResult = authenticate(
        path = "/api/auth/login",
        username = username,
        password = password
    )

    suspend fun logout(token: String): Boolean = withContext(Dispatchers.IO) {
        if (!CloudflareConfig.isConfigured) return@withContext false

        val connection = openConnection("/api/auth/logout", "POST", token)
        try {
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }

    suspend fun updateProfile(token: String, nickname: String): ProfileResult = withContext(Dispatchers.IO) {
        if (!CloudflareConfig.isConfigured) return@withContext ProfileResult(null, "同步服务未配置")

        val connection = openConnection("/api/auth/me", "PATCH", token)
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        try {
            val body = JSONObject().put("nickname", nickname).toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val responseBody = readResponseBody(connection)
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (connection.responseCode !in 200..299) {
                return@withContext ProfileResult(null, json.optString("error", "保存失败"))
            }
            ProfileResult(parseUser(json.getJSONObject("user")), null)
        } catch (error: Exception) {
            ProfileResult(null, error.message ?: "网络请求失败")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun checkUpdate(currentVersionCode: Long): UpdateResult = withContext(Dispatchers.IO) {
        if (!CloudflareConfig.isConfigured) return@withContext UpdateResult(null, "同步服务未配置")

        val connection = openConnection("/api/app/update?versionCode=$currentVersionCode", "GET")
        try {
            val responseBody = readResponseBody(connection)
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (connection.responseCode !in 200..299) {
                return@withContext UpdateResult(null, json.optString("error", "检查失败"))
            }
            UpdateResult(
                AppUpdateInfo(
                    latestVersionCode = json.optLong("latestVersionCode"),
                    latestVersionName = json.optString("latestVersionName"),
                    hasUpdate = json.optBoolean("hasUpdate"),
                    required = json.optBoolean("required"),
                    apkUrl = json.optString("apkUrl"),
                    releaseNotes = json.optJSONArray("releaseNotes").toStringList()
                ),
                null
            )
        } catch (error: Exception) {
            UpdateResult(null, error.message ?: "网络请求失败")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun fetchState(token: String): CloudflareGymState? = withContext(Dispatchers.IO) {
        if (!CloudflareConfig.isConfigured) return@withContext null

        val connection = openConnection("/api/state", "GET", token)

        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseState(JSONObject(body))
        } finally {
            connection.disconnect()
        }
    }

    suspend fun saveState(
        token: String,
        workouts: Map<String, WorkoutDay>,
        exercises: List<ExerciseDefinition>
    ): Boolean = withContext(Dispatchers.IO) {
        if (!CloudflareConfig.isConfigured) return@withContext false

        val connection = openConnection("/api/state", "PUT", token)
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        try {
            val body = AppDataJson.encode(workouts, exercises)
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            connection.responseCode in 200..299
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun authenticate(path: String, username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!CloudflareConfig.isConfigured) {
            return@withContext AuthResult(null, "同步服务未配置")
        }

        val connection = openConnection(path, "POST")
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        try {
            val body = JSONObject()
                .put("username", username.trim())
                .put("password", password)
                .toString()
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
            val responseBody = readResponseBody(connection)
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (connection.responseCode !in 200..299) {
                return@withContext AuthResult(null, json.optString("error", "认证失败"))
            }
            AuthResult(parseSession(json), null)
        } catch (error: Exception) {
            AuthResult(null, error.message ?: "网络请求失败")
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(path: String, method: String, token: String? = null): HttpURLConnection {
        val url = URL("${CloudflareConfig.API_BASE_URL.trimEnd('/')}$path")
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8000
            readTimeout = 8000
            if (!token.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }
    }

    private fun readResponseBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        return stream?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

    private fun parseSession(json: JSONObject): AuthSession {
        return AuthSession(
            token = json.getString("token"),
            user = parseUser(json.getJSONObject("user")),
            expiresAt = json.getLong("expiresAt")
        )
    }

    private fun parseUser(json: JSONObject): AuthUser =
        AuthUser(
            id = json.getString("id"),
            username = json.getString("username"),
            nickname = json.optString("nickname", json.getString("username"))
        )

    private fun parseState(json: JSONObject): CloudflareGymState {
        val snapshot = AppDataJson.decode(json)
        return CloudflareGymState(
            workouts = snapshot.workouts,
            exercises = snapshot.exercises,
            updatedAt = json.optLong("updatedAt", 0L)
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
    }
}
