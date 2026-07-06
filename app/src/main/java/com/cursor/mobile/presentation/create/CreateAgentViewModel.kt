package com.cursor.mobile.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.storage.RunPromptStore
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateAgentUiState(
    val prompt: String = "",
    val selectedRepo: String = "",
    val selectedModel: String = "",
    val branch: String = "",
    val agentName: String = "",
    val mode: String = "agent",
    val autoCreatePR: Boolean = false,
    val workOnCurrentBranch: Boolean = false,
    val selectedWorkerId: String = "",
    val selectedWorkerType: String = "cloud", // cloud, self_hosted, local
    val useRepository: Boolean = true,
    val models: List<ModelInfo> = emptyList(),
    val repositories: List<RepositoryItem> = emptyList(),
    val workers: List<Worker> = emptyList(),
    val isLoading: Boolean = false,
    val isModelsLoading: Boolean = false,
    val isWorkersLoading: Boolean = false,
    val error: String? = null,
    val createdAgentId: String? = null
)

@HiltViewModel
class CreateAgentViewModel @Inject constructor(
    private val repository: AgentRepository,
    private val promptStore: RunPromptStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAgentUiState())
    val uiState: StateFlow<CreateAgentUiState> = _uiState.asStateFlow()

    init {
        loadModels()
        loadRepositories()
        loadWorkers()
    }

    fun onPromptChange(value: String) { _uiState.update { it.copy(prompt = value) } }
    fun onRepoChange(value: String) { _uiState.update { it.copy(selectedRepo = value) } }
    fun onModelChange(value: String) { _uiState.update { it.copy(selectedModel = value) } }
    fun onBranchChange(value: String) { _uiState.update { it.copy(branch = value) } }
    fun onNameChange(value: String) { _uiState.update { it.copy(agentName = value) } }
    fun onModeChange(value: String) { _uiState.update { it.copy(mode = value) } }
    fun onAutoCreatePRChange(value: Boolean) { _uiState.update { it.copy(autoCreatePR = value) } }
    fun onWorkOnCurrentBranchChange(value: Boolean) { _uiState.update { it.copy(workOnCurrentBranch = value) } }
    fun onUseRepositoryChange(value: Boolean) { _uiState.update { it.copy(useRepository = value) } }
    fun onWorkerChange(workerId: String, workerType: String) {
        _uiState.update { it.copy(selectedWorkerId = workerId, selectedWorkerType = workerType) }
    }

    private fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isModelsLoading = true) }
            try {
                val response = repository.listModels()
                _uiState.update {
                    it.copy(
                        models = response.items,
                        isModelsLoading = false,
                        selectedModel = response.items.firstOrNull()?.id ?: ""
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isModelsLoading = false) }
            }
        }
    }

    private fun loadRepositories() {
        viewModelScope.launch {
            try {
                val response = repository.listRepositories()
                _uiState.update {
                    it.copy(
                        repositories = response.items,
                        selectedRepo = response.items.firstOrNull()?.url ?: ""
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadWorkers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorkersLoading = true) }
            try {
                val response = repository.listWorkers()
                val cloudWorker = response.items.firstOrNull { it.type == "cloud" }
                _uiState.update {
                    it.copy(
                        workers = response.items,
                        isWorkersLoading = false,
                        selectedWorkerId = cloudWorker?.id ?: response.items.firstOrNull()?.id ?: "",
                        selectedWorkerType = cloudWorker?.type ?: "cloud"
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isWorkersLoading = false) }
            }
        }
    }

    fun createAgent() {
        val state = _uiState.value
        if (state.prompt.isBlank()) {
            _uiState.update { it.copy(error = "Prompt cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val repos = if (state.useRepository && state.selectedRepo.isNotBlank()) {
                    listOf(RepoConfig(
                        url = state.selectedRepo,
                        startingRef = state.branch.ifBlank { null }
                    ))
                } else null

                val model = if (state.selectedModel.isNotBlank()) {
                    ModelSelection(id = state.selectedModel)
                } else null

                val request = CreateAgentRequest(
                    prompt = PromptInput(text = state.prompt),
                    model = model,
                    name = state.agentName.ifBlank { null },
                    repos = repos,
                    workOnCurrentBranch = state.workOnCurrentBranch,
                    autoCreatePR = state.autoCreatePR,
                    mode = state.mode,
                    agentId = state.selectedWorkerId.takeIf { it.isNotBlank() }
                )

                val response = repository.createAgent(request)
                // Save the first prompt locally so it stays visible in chat history.
                (response.run?.id ?: response.agent.latestRunId)?.let { runId ->
                    promptStore.savePrompt(runId, state.prompt)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        createdAgentId = response.agent.id
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to create agent")
                }
            }
        }
    }

    fun clearCreatedAgent() {
        _uiState.update { it.copy(createdAgentId = null) }
    }
}
