package com.example.gym_app

object CloudflareConfig {
    const val API_BASE_URL = "https://gym-app-api.sulfide2085.workers.dev"
    const val SYNC_TOKEN = ""

    val isConfigured: Boolean
        get() = !API_BASE_URL.contains("YOUR_SUBDOMAIN")
}
