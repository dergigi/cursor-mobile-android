package com.cursor.mobile.presentation.local

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.LocalRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalRemoteChatUiState(
    val composerId: String = "",
    val activeTabTitle: String = "Chat",
    val agentStatus: String = "idle",
    val messages: List<ChatElement> = emptyList(),
    val pendingApprovals: List<Approval> = emptyList(),
    val inputAvailable: Boolean = false,
    val draftMessage: String = "",
    val socketConnected: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocalRemoteChatViewModel @Inject constructor(
    private val repository: LocalRemoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalRemoteChatUiState())
    val uiState: StateFlow<LocalRemoteChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.connectionStatus,
                repository.state
            ) { connection, state ->
                val activeTab = state.chatTabs.find { it.composerId == state.activeComposerId }
                _uiState.value.copy(
                    socketConnected = connection.connected,
                    activeTabTitle = activeTab?.title ?: "Chat",
                    agentStatus = state.agentStatus,
                    messages = state.messages,
                    pendingApprovals = state.pendingApprovals,
                    inputAvailable = state.inputAvailable
                )
            }.collect { _uiState.value = it }
        }
    }

    fun setComposerId(composerId: String) {
        _uiState.update { it.copy(composerId = composerId) }
        val tab = repository.state.value.chatTabs.find { it.composerId == composerId }
        tab?.let { repository.switchTab(it.selectorPath) }
    }

    fun onDraftChange(text: String) {
        _uiState.update { it.copy(draftMessage = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.draftMessage.trim()
        if (text.isNotBlank()) {
            repository.sendMessage(text)
            _uiState.update { it.copy(draftMessage = "") }
        }
    }

    fun approve(selectorPath: String) {
        repository.approve(selectorPath)
    }

    fun reject(selectorPath: String) {
        repository.reject(selectorPath)
    }

    fun runCommand(selectorPath: String) {
        repository.runCommand(selectorPath)
    }

    fun skipCommand(selectorPath: String) {
        repository.skipCommand(selectorPath)
    }

    fun allowCommand(selectorPath: String) {
        repository.allowCommand(selectorPath)
    }

    fun viewPlan(selectorPath: String) {
        repository.viewPlan(selectorPath)
    }

    fun buildPlan(selectorPath: String) {
        repository.buildPlan(selectorPath)
    }
}
