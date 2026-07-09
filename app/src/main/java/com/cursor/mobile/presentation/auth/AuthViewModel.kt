package com.cursor.mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.network.CursorApiService
import com.cursor.mobile.core.network.RelayAuthService
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.data.model.HealthResponse
import com.cursor.mobile.data.repository.LocalRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val apiKey: String = "",
    val relayUrl: String = "",
    val relayPassword: String = "",
    val connectionMode: ConnectionMode = ConnectionMode.CLOUD,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val userName: String? = null,
    val plan: com.cursor.mobile.data.model.SubscriptionPlan? = null,
    val relayHealth: HealthResponse? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val apiService: CursorApiService,
    private val relayAuthService: RelayAuthService,
    private val localRemoteRepository: LocalRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            apiKeyManager.isAuthenticated.collect { auth ->
                _uiState.update { it.copy(isAuthenticated = auth) }
                if (auth) {
                    when (apiKeyManager.getConnectionMode()) {
                        ConnectionMode.CLOUD -> loadCloudProfile()
                        ConnectionMode.LOCAL_REMOTE -> loadRelayHealth()
                    }
                }
            }
        }
        viewModelScope.launch {
            apiKeyManager.connectionModeFlow.collect { mode ->
                _uiState.update { it.copy(connectionMode = mode) }
            }
        }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(apiKey = key, error = null) }
    }

    fun onRelayUrlChange(url: String) {
        _uiState.update { it.copy(relayUrl = url, error = null) }
    }

    fun onRelayPasswordChange(password: String) {
        _uiState.update { it.copy(relayPassword = password, error = null) }
    }

    fun setConnectionMode(mode: ConnectionMode) {
        _uiState.update { it.copy(connectionMode = mode, error = null) }
    }

    fun authenticate() {
        when (_uiState.value.connectionMode) {
            ConnectionMode.CLOUD -> authenticateCloud()
            ConnectionMode.LOCAL_REMOTE -> authenticateLocal()
        }
    }

    private fun authenticateCloud() {
        val key = _uiState.value.apiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(error = "API key cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                apiKeyManager.saveApiKey(key)
                val me = apiService.getMe()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userName = me.userFirstName ?: me.userEmail,
                        plan = me.plan
                    )
                }
            } catch (e: Exception) {
                apiKeyManager.clearApiKey()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Authentication failed"
                    )
                }
            }
        }
    }

    private fun authenticateLocal() {
        val url = _uiState.value.relayUrl.trim()
        val password = _uiState.value.relayPassword

        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Relay URL cannot be empty") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(error = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = localRemoteRepository.login(url, password)
                if (response.token != null) {
                    val health = relayAuthService.healthWithUrl(url)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            relayHealth = health
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.error ?: "Invalid relay password"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to connect to relay"
                    )
                }
            }
        }
    }

    fun testRelayConnection() {
        val url = _uiState.value.relayUrl.trim()
        val password = _uiState.value.relayPassword
        if (url.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "URL and password are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = relayAuthService.loginWithUrl(url, password)
                if (response.token != null) {
                    val health = relayAuthService.healthWithUrl(url)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            relayHealth = health,
                            error = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = response.error ?: "Invalid password"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Connection test failed"
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            apiKeyManager.clearApiKey()
            apiKeyManager.clearRelayCredentials()
            apiKeyManager.saveConnectionMode(ConnectionMode.CLOUD)
            _uiState.update {
                AuthUiState(
                    connectionMode = ConnectionMode.CLOUD,
                    isAuthenticated = false
                )
            }
        }
    }

    private suspend fun loadCloudProfile() {
        try {
            val me = apiService.getMe()
            _uiState.update {
                it.copy(
                    userName = me.userFirstName ?: me.userEmail,
                    plan = me.plan
                )
            }
        } catch (_: Exception) {}
    }

    private suspend fun loadRelayHealth() {
        try {
            val health = relayAuthService.health()
            _uiState.update { it.copy(relayHealth = health) }
        } catch (_: Exception) {}
    }
}
