package com.cursor.mobile.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cursor_settings")

enum class ConnectionMode {
    CLOUD,
    LOCAL_REMOTE
}

@Singleton
class ApiKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val CONNECTION_MODE = stringPreferencesKey("connection_mode")
        private val RELAY_BASE_URL = stringPreferencesKey("relay_base_url")
        private val RELAY_SESSION_TOKEN = stringPreferencesKey("relay_session_token")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private const val DEFAULT_BASE_URL = "https://api.cursor.com"
    }

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { it[API_KEY] }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map {
        it[BASE_URL] ?: DEFAULT_BASE_URL
    }

    val connectionModeFlow: Flow<ConnectionMode> = context.dataStore.data.map { prefs ->
        when (prefs[CONNECTION_MODE]) {
            "local_remote" -> ConnectionMode.LOCAL_REMOTE
            else -> ConnectionMode.CLOUD
        }
    }

    val relayBaseUrlFlow: Flow<String?> = context.dataStore.data.map { it[RELAY_BASE_URL] }

    val relaySessionTokenFlow: Flow<String?> = context.dataStore.data.map { it[RELAY_SESSION_TOKEN] }

    val isAuthenticated: Flow<Boolean> = context.dataStore.data.map { prefs ->
        when (prefs[CONNECTION_MODE]) {
            "local_remote" -> !prefs[RELAY_SESSION_TOKEN].isNullOrBlank()
            else -> !prefs[API_KEY].isNullOrBlank()
        }
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map {
        it[THEME_MODE] ?: "system"
    }

    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map {
        it[BIOMETRIC_ENABLED] ?: false
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit {
            it[API_KEY] = key.trim()
            it[CONNECTION_MODE] = "cloud"
        }
    }

    suspend fun clearApiKey() {
        context.dataStore.edit { it.remove(API_KEY) }
    }

    suspend fun saveBaseUrl(url: String) {
        context.dataStore.edit { it[BASE_URL] = url.trim().trimEnd('/') }
    }

    suspend fun saveConnectionMode(mode: ConnectionMode) {
        context.dataStore.edit {
            it[CONNECTION_MODE] = when (mode) {
                ConnectionMode.CLOUD -> "cloud"
                ConnectionMode.LOCAL_REMOTE -> "local_remote"
            }
        }
    }

    suspend fun saveRelayCredentials(baseUrl: String, sessionToken: String) {
        context.dataStore.edit {
            it[RELAY_BASE_URL] = baseUrl.trim().trimEnd('/')
            it[RELAY_SESSION_TOKEN] = sessionToken.trim()
            it[CONNECTION_MODE] = "local_remote"
        }
    }

    suspend fun clearRelayCredentials() {
        context.dataStore.edit {
            it.remove(RELAY_BASE_URL)
            it.remove(RELAY_SESSION_TOKEN)
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun saveBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun getApiKey(): String? {
        return context.dataStore.data.first()[API_KEY]
    }

    suspend fun getBaseUrl(): String {
        return context.dataStore.data.first()[BASE_URL] ?: DEFAULT_BASE_URL
    }

    suspend fun getConnectionMode(): ConnectionMode {
        return when (context.dataStore.data.first()[CONNECTION_MODE]) {
            "local_remote" -> ConnectionMode.LOCAL_REMOTE
            else -> ConnectionMode.CLOUD
        }
    }

    suspend fun getRelayBaseUrl(): String? {
        return context.dataStore.data.first()[RELAY_BASE_URL]
    }

    suspend fun getRelaySessionToken(): String? {
        return context.dataStore.data.first()[RELAY_SESSION_TOKEN]
    }

    suspend fun getThemeMode(): String {
        return context.dataStore.data.first()[THEME_MODE] ?: "system"
    }

    suspend fun isBiometricEnabled(): Boolean {
        return context.dataStore.data.first()[BIOMETRIC_ENABLED] ?: false
    }
}
