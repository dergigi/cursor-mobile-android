package com.cursor.mobile.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class McpServersUiState(
    val servers: List<McpServer> = emptyList(),
    val selectedServer: McpServer? = null,
    val tools: List<McpTool> = emptyList(),
    val isLoading: Boolean = false,
    val isToolsLoading: Boolean = false,
    val error: String? = null,
    val invokeResult: String? = null
)

@HiltViewModel
class McpServersViewModel @Inject constructor(
    private val repository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(McpServersUiState())
    val uiState: StateFlow<McpServersUiState> = _uiState.asStateFlow()

    init {
        loadServers()
    }

    fun loadServers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = repository.listMcpServers()
                _uiState.update {
                    it.copy(
                        servers = response.items,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load MCP servers")
                }
            }
        }
    }

    fun selectServer(server: McpServer) {
        _uiState.update { it.copy(selectedServer = server, tools = emptyList(), invokeResult = null) }
        loadTools(server.id)
    }

    fun loadTools(serverId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isToolsLoading = true) }
            try {
                val response = repository.listMcpTools(serverId)
                _uiState.update {
                    it.copy(
                        tools = response.items,
                        isToolsLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isToolsLoading = false, error = e.message)
                }
            }
        }
    }

    fun invokeTool(serverId: String, toolName: String, arguments: Map<String, String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, invokeResult = null) }
            try {
                val request = McpInvokeRequest(
                    serverId = serverId,
                    toolName = toolName,
                    arguments = arguments
                )
                val response = repository.invokeMcpTool(request)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        invokeResult = response.result ?: response.error ?: "No result"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to invoke tool")
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun dismissResult() {
        _uiState.update { it.copy(invokeResult = null) }
    }
}
