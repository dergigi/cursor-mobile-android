package com.cursor.mobile.presentation.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.data.model.Agent
import com.cursor.mobile.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = false
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val repository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadAgents()
    }

    fun loadAgents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.listAgents(limit = 50)
                _uiState.update {
                    it.copy(
                        agents = response.items,
                        isLoading = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load agents")
                }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        viewModelScope.launch {
            try {
                val response = repository.listAgents(limit = 50, cursor = cursor)
                _uiState.update {
                    it.copy(
                        agents = it.agents + response.items,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun archiveAgent(agentId: String) {
        viewModelScope.launch {
            try {
                repository.archiveAgent(agentId)
                loadAgents()
            } catch (_: Exception) {}
        }
    }
}
