package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("skill_discovery_session_prefs", Context.MODE_PRIVATE)

    private val _authToken = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(!_authToken.value.isNullOrEmpty())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUserId = MutableStateFlow(prefs.getString(KEY_USER_ID, "std_vinay") ?: "std_vinay")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _currentClassCode = MutableStateFlow(prefs.getString(KEY_CLASS_CODE, "CSE2026") ?: "CSE2026")
    val currentClassCode: StateFlow<String> = _currentClassCode.asStateFlow()

    private val _apiBaseUrl = MutableStateFlow(
        normalizeApiBaseUrl(prefs.getString(KEY_API_BASE_URL, BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL)
    )
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private val _isSandboxBackend = MutableStateFlow(prefs.getBoolean(KEY_SANDBOX_BACKEND, true))
    val isSandboxBackend: StateFlow<Boolean> = _isSandboxBackend.asStateFlow()

    private val _lastUpdatedTime = MutableStateFlow(prefs.getLong(KEY_LAST_UPDATED, System.currentTimeMillis()))
    val lastUpdatedTime: StateFlow<Long> = _lastUpdatedTime.asStateFlow()

    fun setSession(token: String, userId: String, classCode: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_CLASS_CODE, classCode)
            .apply()
        _authToken.value = token
        _isLoggedIn.value = true
        _currentUserId.value = userId
        _currentClassCode.value = classCode
    }

    fun switchUser(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        _currentUserId.value = userId
    }

    fun setApiBaseUrl(url: String) {
        val formattedUrl = normalizeApiBaseUrl(url)
        prefs.edit().putString(KEY_API_BASE_URL, formattedUrl).apply()
        _apiBaseUrl.value = formattedUrl
    }

    private fun normalizeApiBaseUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return "https://api.skilldiscovery.app/api/v1/"

        val uriWithScheme = if (!trimmed.contains("://")) "http://$trimmed" else trimmed
        val parsedUrl = Uri.parse(uriWithScheme)
        val host = parsedUrl.host.orEmpty()
        val port = parsedUrl.port

        val isLocalHost = host == "localhost" || host == "127.0.0.1" || host.startsWith("10.") || host.startsWith("192.168.")

        val scheme = when {
            parsedUrl.scheme != null -> parsedUrl.scheme!!
            isLocalHost -> "http"
            else -> "https"
        }

        val effectivePort = if (isLocalHost && port == -1 && scheme == "http") 8000 else port
        val authority = if (effectivePort != -1) "$host:$effectivePort" else host

        val rawPath = parsedUrl.encodedPath.orEmpty()
        val formattedPath = when {
            rawPath.isEmpty() || rawPath == "/" -> "/api/v1/"
            !rawPath.endsWith("/api/v1") && !rawPath.endsWith("/api/v1/") && !rawPath.contains("/api/") -> {
                if (rawPath.endsWith("/")) "${rawPath}api/v1/" else "$rawPath/api/v1/"
            }
            else -> rawPath
        }

        val normalizedUrl = Uri.Builder()
            .scheme(scheme)
            .encodedAuthority(authority)
            .encodedPath(formattedPath)
            .encodedQuery(parsedUrl.encodedQuery)
            .build()
            .toString()

        return if (normalizedUrl.endsWith("/")) normalizedUrl else "$normalizedUrl/"
    }

    fun setSandboxBackend(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SANDBOX_BACKEND, enabled).apply()
        _isSandboxBackend.value = enabled
    }

    fun setClassCode(code: String) {
        prefs.edit().putString(KEY_CLASS_CODE, code).apply()
        _currentClassCode.value = code
    }

    fun updateLastSyncTime() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_UPDATED, now).apply()
        _lastUpdatedTime.value = now
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
        _authToken.value = null
        _isLoggedIn.value = false
        _currentUserId.value = ""
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_CLASS_CODE = "class_code"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_SANDBOX_BACKEND = "sandbox_backend"
        private const val KEY_LAST_UPDATED = "last_updated_time"
    }
}
