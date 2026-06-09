package com.example.gym_app

import android.content.Context

object CloudflareAuthStore {
    private const val PREFS_NAME = "cloudflare_auth"
    private const val TOKEN_KEY = "token"
    private const val USER_ID_KEY = "user_id"
    private const val USERNAME_KEY = "username"
    private const val NICKNAME_KEY = "nickname"
    private const val EXPIRES_AT_KEY = "expires_at"

    fun load(context: Context): AuthSession? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val token = prefs.getString(TOKEN_KEY, null)
        val userId = prefs.getString(USER_ID_KEY, null)
        val username = prefs.getString(USERNAME_KEY, null)
        val nickname = prefs.getString(NICKNAME_KEY, null)
        val expiresAt = prefs.getLong(EXPIRES_AT_KEY, 0L)

        if (token.isNullOrBlank() || userId.isNullOrBlank() || username.isNullOrBlank()) return null
        if (expiresAt <= System.currentTimeMillis()) {
            clear(context)
            return null
        }

        return AuthSession(
            token = token,
            user = AuthUser(id = userId, username = username, nickname = nickname ?: username),
            expiresAt = expiresAt
        )
    }

    fun save(context: Context, session: AuthSession) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TOKEN_KEY, session.token)
            .putString(USER_ID_KEY, session.user.id)
            .putString(USERNAME_KEY, session.user.username)
            .putString(NICKNAME_KEY, session.user.nickname)
            .putLong(EXPIRES_AT_KEY, session.expiresAt)
            .apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
