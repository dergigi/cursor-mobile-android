package com.cursor.mobile.presentation.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.data.model.CursorState
import com.cursor.mobile.data.repository.LocalRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalRemoteHomeUiState(
    val socketConnected: Boolean = false,
    val cursorState: CursorState = CursorState(),
    val error: String? = null
)

@HiltViewModel
class LocalRemoteHomeViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val repository: LocalRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalRemoteHomeUiState())
    val uiState: StateFlow<LocalRemoteHomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.connectionStatus,
                repository.state
            ) { connection, state ->
                LocalRemoteHomeUiState(
                    socketConnected = connection.connected,
                    cursorState = state
                )
            }.collect { _uiState.value = it }
        }
    }

    fun connectIfNeeded() {
        viewModelScope.launch {
            try {
                if (apiKeyManager.getConnectionMode() == ConnectionMode.LOCAL_REMOTE) {
                    if (repository.connectionStatus.value.connected) return@launch
                    repository.reconnect()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    fun switchWindow(windowId: String) {
        repository.switchWindow(windowId)
    }
}
