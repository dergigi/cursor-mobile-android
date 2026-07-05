package com.cursor.mobile.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentDetailUiState(
    val agent: Agent? = null,
    val runs: List<Run> = emptyList(),
    val usage: UsageResponse? = null,
    val artifacts: List<ArtifactItem> = emptyList(),
    val pullRequests: List<PullRequest> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AgentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AgentRepository
) : ViewModel() {

    private val agentId: String = savedStateHandle["agentId"] ?: ""

    private val _uiState = MutableStateFlow(AgentDetailUiState())
    val uiState: StateFlow<AgentDetailUiState> = _uiState.asStateFlow()

    init {
        if (agentId.isNotBlank()) {
            loadAll()
        }
    }

    fun setAgentId(id: String) {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val agent = repository.getAgent(agentId)
                _uiState.update { it.copy(agent = agent) }

                // Load runs, usage, artifacts in parallel
                try {
                    val runs = repository.listRuns(agentId, limit = 20)
                    _uiState.update { it.copy(runs = runs.items) }
                } catch (_: Exception) {}

                try {
                    val usage = repository.getUsage(agentId)
                    _uiState.update { it.copy(usage = usage) }
                } catch (_: Exception) {}

                try {
                    val artifacts = repository.listArtifacts(agentId)
                    _uiState.update { it.copy(artifacts = artifacts.items) }
                } catch (_: Exception) {}

                try {
                    val prs = repository.listPullRequests(agentId = agentId)
                    _uiState.update { it.copy(pullRequests = prs.items) }
                } catch (_: Exception) {}

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun archiveAgent() {
        viewModelScope.launch {
            try {
                repository.archiveAgent(agentId)
                loadAll()
            } catch (_: Exception) {}
        }
    }

    fun deleteAgent() {
        viewModelScope.launch {
            try {
                repository.deleteAgent(agentId)
            } catch (_: Exception) {}
        }
    }

    fun cancelRun(runId: String) {
        viewModelScope.launch {
            try {
                repository.cancelRun(agentId, runId)
                loadAll()
            } catch (_: Exception) {}
        }
    }
}
