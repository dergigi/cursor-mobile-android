package com.cursor.mobile.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.network.CursorApiService
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.data.model.SubscriptionPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val apiKey: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val userName: String? = null,
    val plan: SubscriptionPlan? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val apiService: CursorApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            apiKeyManager.isAuthenticated.collect { auth ->
                _uiState.update { it.copy(isAuthenticated = auth) }
                if (auth) {
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
    }
        }
    }

    fun onApiKeyChange(key: String) {
        _uiState.update { it.copy(apiKey = key, error = null) }
    }

    fun authenticate() {
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

    fun logout() {
        viewModelScope.launch {
            apiKeyManager.clearApiKey()
            _uiState.update { AuthUiState() }
        }
    }
}
