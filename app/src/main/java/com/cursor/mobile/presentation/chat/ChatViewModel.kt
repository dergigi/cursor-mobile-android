package com.cursor.mobile.presentation.chat

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.storage.RunPromptStore
import com.cursor.mobile.data.model.*
import com.cursor.mobile.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

data class ChatUiState(
    val agentId: String = "",
    val agent: Agent? = null,
    val messages: List<ChatMessage> = emptyList(),
    val currentRunId: String? = null,
    val runStatus: String = "",
    val isStreaming: Boolean = false,
    val inputText: String = "",
    val error: String? = null,
    val attachedImageBase64: String? = null,
    val showAnnotation: Boolean = false,
    val annotationUri: String? = null,
    val showRemoteControlDialog: Boolean = false,
    val availableWorkers: List<Worker> = emptyList(),
    val isWorkersLoading: Boolean = false,
    val slashCommands: List<SlashCommand> = emptyList(),
    val showSlashCommands: Boolean = false,
    val skills: List<Skill> = emptyList(),
    val automations: List<Automation> = emptyList(),
    val showCommandPalette: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AgentRepository,
    private val promptStore: RunPromptStore
) : ViewModel() {

    private val agentId: String = savedStateHandle["agentId"] ?: ""

    private val _uiState = MutableStateFlow(ChatUiState(agentId = agentId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    private var voiceHelper: VoiceInputHelper? = null
    private val _voiceState = MutableStateFlow(VoiceInputState())
    val voiceState: StateFlow<VoiceInputState> = _voiceState.asStateFlow()

    init {
        if (agentId.isNotBlank()) {
            loadAgent()
            loadConversation()
        }
        loadSlashCommands()
        loadSkillsAndAutomations()
    }

    fun initVoiceHelper(context: Context) {
        voiceHelper = VoiceInputHelper(context)
    }

    private fun loadSlashCommands() {
        viewModelScope.launch {
            try {
                val response = repository.listSlashCommands()
                _uiState.update { it.copy(slashCommands = response.items) }
            } catch (_: Exception) {}
        }
    }

    private fun loadSkillsAndAutomations() {
        viewModelScope.launch {
            try {
                val skills = repository.listSkills()
                val automations = repository.listAutomations()
                _uiState.update {
                    it.copy(
                        skills = skills.items,
                        automations = automations.items
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun setAgentId(id: String) {
        _uiState.update { it.copy(agentId = id) }
        loadAgent()
        loadConversation()
    }

    private fun loadAgent() {
        viewModelScope.launch {
            try {
                val agent = repository.getAgent(agentId)
                _uiState.update { it.copy(agent = agent) }
                agent.latestRunId?.let { runId ->
                    _uiState.update { it.copy(currentRunId = runId) }
                    observeRun(runId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun loadConversation() {
        viewModelScope.launch {
            try {
                val runs = repository.listRuns(agentId, limit = 50).items.reversed()

                // The API doesn't return the typed prompt, so fall back to a locally saved copy.
                val savedPrompts = promptStore.getPrompts(runs.map { it.id })

                // The list endpoint returns run metadata only, not the result text.
                // Fetch each completed run's detail in parallel to recover its output.
                val completed = listOf("FINISHED", "ERROR", "CANCELLED")
                val details = coroutineScope {
                    runs.map { run ->
                        async {
                            if (run.status in completed) {
                                runCatching { repository.getRun(agentId, run.id) }.getOrNull()
                            } else null
                        }
                    }.awaitAll()
                }

                val messages = mutableListOf<ChatMessage>()
                runs.forEachIndexed { index, run ->
                    messages.add(
                        ChatMessage(
                            id = "${run.id}-user",
                            role = MessageRole.USER,
                            content = savedPrompts[run.id] ?: "Run ${run.id.takeLast(8)}",
                            timestamp = run.createdAt?.let { parseTimestamp(it) } ?: System.currentTimeMillis()
                        )
                    )

                    val result = details[index]?.result ?: run.result
                    if (run.status in completed && !result.isNullOrBlank()) {
                        messages.add(
                            ChatMessage(
                                id = "${run.id}-result",
                                role = MessageRole.RESULT,
                                content = result,
                                timestamp = run.updatedAt?.let { parseTimestamp(it) } ?: System.currentTimeMillis()
                            )
                        )
                    }
                }

                _uiState.update { it.copy(messages = messages) }
            } catch (_: Exception) {}
        }
    }

    fun onInputChange(text: String) {
        val showSlash = text.startsWith("/") && text.length <= 20 && text.none { it.isWhitespace() }
        _uiState.update {
            it.copy(
                inputText = text,
                showSlashCommands = showSlash
            )
        }
    }

    fun dismissSlashCommands() {
        _uiState.update { it.copy(showSlashCommands = false) }
    }

    fun applySlashCommand(command: SlashCommand) {
        _uiState.update {
            it.copy(
                inputText = "/${command.name} ",
                showSlashCommands = false
            )
        }
    }

    fun applySkill(skill: Skill) {
        _uiState.update {
            it.copy(
                inputText = "@${skill.name} ",
                showCommandPalette = false
            )
        }
    }

    fun applyAutomation(automation: Automation) {
        _uiState.update { it.copy(showCommandPalette = false) }
        viewModelScope.launch {
            try {
                val request = RunAutomationRequest(
                    automationId = automation.id,
                    agentId = _uiState.value.agentId
                )
                val response = repository.runAutomation(request)
                _uiState.update {
                    it.copy(
                        agentId = response.agent.id,
                        agent = response.agent,
                        currentRunId = response.run?.id
                    )
                }
                response.run?.id?.let { observeRun(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showCommandPalette() {
        _uiState.update { it.copy(showCommandPalette = true) }
    }

    fun dismissCommandPalette() {
        _uiState.update { it.copy(showCommandPalette = false) }
    }

    fun showRemoteControlDialog() {
        _uiState.update { it.copy(showRemoteControlDialog = true, isWorkersLoading = true) }
        viewModelScope.launch {
            try {
                val response = repository.listWorkers()
                _uiState.update {
                    it.copy(
                        availableWorkers = response.items.filter { worker ->
                            worker.type == "local" || worker.type == "self_hosted"
                        },
                        isWorkersLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isWorkersLoading = false) }
            }
        }
    }

    fun dismissRemoteControlDialog() {
        _uiState.update { it.copy(showRemoteControlDialog = false) }
    }

    fun connectRemoteControl(worker: Worker) {
        val currentAgentId = _uiState.value.agentId
        val currentRunId = _uiState.value.currentRunId
        if (currentAgentId.isBlank() || currentRunId == null) {
            _uiState.update { it.copy(error = "No active run to remote control") }
            return
        }

        _uiState.update { it.copy(showRemoteControlDialog = false, isStreaming = true) }
        viewModelScope.launch {
            try {
                val request = CreateRemoteControlRequest(
                    agentId = currentAgentId,
                    runId = currentRunId,
                    workerId = worker.id
                )
                val session = repository.createRemoteControl(request)
                val systemMsg = ChatMessage(
                    id = "rc-${session.id}",
                    role = MessageRole.SYSTEM,
                    content = "Remote Control enabled on ${worker.name ?: worker.id}. Agent loop runs in cloud; tools execute on this machine."
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + systemMsg,
                        isStreaming = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        error = e.message ?: "Failed to enable Remote Control"
                    )
                }
            }
        }
    }

    fun toggleVoiceInput() {
        val helper = voiceHelper ?: return

        if (_voiceState.value.isListening) {
            helper.stopListening()
        } else {
            helper.startListening { result ->
                _uiState.update { it.copy(inputText = it.inputText + result) }
            }
        }

        // Observe voice state
        viewModelScope.launch {
            helper.state.collect { state ->
                _voiceState.value = state
            }
        }
    }

    fun attachImage(bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream)
        val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        _uiState.update { it.copy(attachedImageBase64 = base64) }
    }

    fun removeAttachedImage() {
        _uiState.update { it.copy(attachedImageBase64 = null) }
    }

    fun showAnnotation(uri: String) {
        _uiState.update { it.copy(showAnnotation = true, annotationUri = uri) }
    }

    fun hideAnnotation() {
        _uiState.update { it.copy(showAnnotation = false, annotationUri = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() && _uiState.value.attachedImageBase64 == null) return

        // Handle /remote-control slash command
        if (text == "/remote-control" || text.startsWith("/remote-control ")) {
            showRemoteControlDialog()
            _uiState.update { it.copy(inputText = "") }
            return
        }

        val currentAgentId = _uiState.value.agentId
        val imageBase64 = _uiState.value.attachedImageBase64

        val userMsg = ChatMessage(
            id = "user-${System.currentTimeMillis()}",
            role = MessageRole.USER,
            content = buildString {
                append(text)
                if (imageBase64 != null) append("\n[Image attached]")
            }
        )
        _uiState.update {
            it.copy(
                messages = it.messages + userMsg,
                inputText = "",
                attachedImageBase64 = null,
                isStreaming = true,
                error = null,
                showSlashCommands = false
            )
        }

        viewModelScope.launch {
            try {
                val images = if (imageBase64 != null) {
                    listOf(ImageInput(data = imageBase64, mimeType = "image/png"))
                } else null

                val run = if (currentAgentId.isBlank()) {
                    val response = repository.createAgent(
                        CreateAgentRequest(
                            prompt = PromptInput(text = text, images = images)
                        )
                    )
                    _uiState.update { it.copy(agentId = response.agent.id, agent = response.agent) }
                    response.run!!
                } else {
                    repository.createRun(
                        currentAgentId,
                        CreateRunRequest(prompt = PromptInput(text = text, images = images))
                    )
                }

                _uiState.update { it.copy(currentRunId = run.id) }
                // Persist the prompt locally so it stays visible when the chat is reopened.
                promptStore.savePrompt(run.id, userMsg.content)
                _uiState.value.agent?.let { agent ->
                    // In production, inject NotificationHelper and call startLiveActivity here
                }
                observeRun(run.id)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isStreaming = false,
                        error = e.message ?: "Failed to send message"
                    )
                }
                val errorMsg = ChatMessage(
                    id = "error-${System.currentTimeMillis()}",
                    role = MessageRole.SYSTEM,
                    content = "Error: ${e.message}"
                )
                _uiState.update { it.copy(messages = it.messages + errorMsg) }
            }
        }
    }

    private fun observeRun(runId: String) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val currentAgentId = _uiState.value.agentId
            if (currentAgentId.isBlank()) return@launch

            _uiState.update { it.copy(isStreaming = true) }

            repository.streamRun(currentAgentId, runId).collect { event ->
                when (event) {
                    is SseEvent.Status -> {
                        _uiState.update { it.copy(runStatus = event.status) }
                    }
                    is SseEvent.Assistant -> {
                        val messages = _uiState.value.messages.toMutableList()
                        val lastIdx = messages.indexOfLast { it.role == MessageRole.ASSISTANT }
                        if (lastIdx >= 0) {
                            messages[lastIdx] = messages[lastIdx].copy(
                                content = messages[lastIdx].content + event.text
                            )
                        } else {
                            messages.add(
                                ChatMessage(
                                    id = "assistant-${System.currentTimeMillis()}",
                                    role = MessageRole.ASSISTANT,
                                    content = event.text
                                )
                            )
                        }
                        _uiState.update { it.copy(messages = messages) }
                    }
                    is SseEvent.Thinking -> {
                        val messages = _uiState.value.messages.toMutableList()
                        val lastIdx = messages.indexOfLast { it.role == MessageRole.THINKING }
                        if (lastIdx >= 0) {
                            messages[lastIdx] = messages[lastIdx].copy(
                                content = messages[lastIdx].content + event.text
                            )
                        } else {
                            messages.add(
                                ChatMessage(
                                    id = "thinking-${System.currentTimeMillis()}",
                                    role = MessageRole.THINKING,
                                    content = event.text
                                )
                            )
                        }
                        _uiState.update { it.copy(messages = messages) }
                    }
                    is SseEvent.ToolCall -> {
                        val toolMsg = ChatMessage(
                            id = "tool-${event.callId}-${System.currentTimeMillis()}",
                            role = MessageRole.TOOL,
                            content = buildToolCallContent(event),
                            toolCall = ToolCallInfo(
                                callId = event.callId,
                                name = event.name,
                                status = event.status,
                                args = event.args,
                                result = event.result
                            )
                        )
                        val messages = _uiState.value.messages.toMutableList()
                        val existingIdx = messages.indexOfLast {
                            it.toolCall?.callId == event.callId
                        }
                        if (existingIdx >= 0) {
                            messages[existingIdx] = toolMsg
                        } else {
                            messages.add(toolMsg)
                        }
                        _uiState.update { it.copy(messages = messages) }
                    }
                    is SseEvent.Result -> {
                        val resultMsg = ChatMessage(
                            id = "result-${System.currentTimeMillis()}",
                            role = MessageRole.RESULT,
                            content = buildString {
                                append("Run ${event.status.lowercase()}")
                                event.durationMs?.let { append(" in ${it / 1000.0}s") }
                                event.text?.let { append("\n\n$it") }
                            }
                        )
                        _uiState.update {
                            it.copy(
                                messages = it.messages + resultMsg,
                                isStreaming = false,
                                runStatus = event.status
                            )
                        }
                    }
                    is SseEvent.Error -> {
                        _uiState.update {
                            it.copy(
                                isStreaming = false,
                                error = event.message
                            )
                        }
                    }
                    is SseEvent.Heartbeat -> { /* ignore */ }
                    is SseEvent.Done -> {
                        _uiState.update { it.copy(isStreaming = false) }
                    }
                }
            }
        }
    }

    fun cancelRun() {
        val aid = _uiState.value.agentId
        val rid = _uiState.value.currentRunId
        if (aid.isBlank() || rid == null) return

        viewModelScope.launch {
            try {
                repository.cancelRun(aid, rid)
                _uiState.update { it.copy(isStreaming = false) }
            } catch (_: Exception) {}
        }
    }

    private fun buildToolCallContent(event: SseEvent.ToolCall): String {
        return buildString {
            append("[${event.name}] ")
            if (event.status == "running") {
                append("running...")
            } else {
                append("completed")
            }
        }
    }

    private fun parseTimestamp(iso: String): Long {
        return try {
            java.time.Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceHelper?.destroy()
    }
}
