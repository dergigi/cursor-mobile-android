package com.cursor.mobile.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.data.repository.LocalRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModeSelectUiState(
    val hasCloudCredentials: Boolean = false,
    val hasLocalCredentials: Boolean = false,
    val relayUrl: String? = null
)

@HiltViewModel
class ModeSelectViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val localRemoteRepository: LocalRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModeSelectUiState())
    val uiState: StateFlow<ModeSelectUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                apiKeyManager.apiKeyFlow,
                apiKeyManager.relaySessionTokenFlow,
                apiKeyManager.relayBaseUrlFlow
            ) { apiKey, relayToken, relayUrl ->
                ModeSelectUiState(
                    hasCloudCredentials = !apiKey.isNullOrBlank(),
                    hasLocalCredentials = !relayToken.isNullOrBlank(),
                    relayUrl = relayUrl
                )
            }.collect { _uiState.value = it }
        }
    }

    fun prepareCloudMode(onReady: () -> Unit, onNeedsAuth: () -> Unit) {
        viewModelScope.launch {
            apiKeyManager.saveConnectionMode(ConnectionMode.CLOUD)
            if (apiKeyManager.getApiKey().isNullOrBlank()) {
                onNeedsAuth()
            } else {
                onReady()
            }
        }
    }

    fun prepareLocalMode(onReady: () -> Unit, onNeedsAuth: () -> Unit) {
        viewModelScope.launch {
            apiKeyManager.saveConnectionMode(ConnectionMode.LOCAL_REMOTE)
            if (apiKeyManager.getRelaySessionToken().isNullOrBlank()) {
                onNeedsAuth()
            } else {
                localRemoteRepository.reconnect()
                onReady()
            }
        }
    }
}
