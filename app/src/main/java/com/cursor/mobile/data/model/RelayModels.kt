package com.cursor.mobile.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LoginRequest(val password: String)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val error: String? = null
)

@Serializable
data class HealthResponse(
    val ok: Boolean = false,
    val authRequired: Boolean = false,
    val sessionValid: Boolean = false,
    val connected: Boolean = false,
    val extractorStatus: String = "idle",
    val lastExtractionAt: Long? = null,
    val consecutiveExtractionFailures: Int = 0,
    val lastExtractionError: String? = null,
    val agentStatus: String = "idle",
    val clients: Int = 0,
    val uptime: Double = 0.0,
    val windows: List<CursorWindow> = emptyList(),
    val messageCount: Int = 0
)

@Serializable
data class CursorWindow(
    val id: String,
    val title: String,
    val url: String,
    val wsUrl: String? = null
)

@Serializable
data class ChatTab(
    val composerId: String,
    val title: String,
    val isActive: Boolean,
    val status: String,
    val selectorPath: String
)

@Serializable
data class RelayModeInfo(
    val current: String,
    val available: List<RelayModeOption> = emptyList()
)

@Serializable
data class RelayModeOption(
    val id: String,
    val label: String,
    val icon: String = ""
)

@Serializable
data class RelayModelInfo(
    val current: String,
    val currentId: String
)

@Serializable
data class ComposerQueueItem(val id: String, val text: String)

@Serializable
data class ComposerQueueState(
    val items: List<ComposerQueueItem> = emptyList(),
    val queueLabel: String? = null
)

@Serializable
data class QuestionnaireOption(
    val letter: String,
    val label: String,
    val isFreeform: Boolean,
    val selectorPath: String
)

@Serializable
data class QuestionnaireQuestion(
    val number: String,
    val text: String,
    val options: List<QuestionnaireOption>,
    val isActive: Boolean
)

@Serializable
data class Questionnaire(
    val questions: List<QuestionnaireQuestion>,
    val activeIndex: Int,
    val totalLabel: String,
    val skipSelectorPath: String,
    val continueSelectorPath: String,
    val continueDisabled: Boolean
)

@Serializable
data class CursorState(
    val connected: Boolean = false,
    val extractorStatus: String = "idle",
    val lastExtractionAt: Long? = null,
    val consecutiveExtractionFailures: Int = 0,
    val lastExtractionError: String? = null,
    val agentStatus: String = "idle",
    val agentActivityText: String? = null,
    val agentActivityLive: Boolean = false,
    val agentActivitySource: String = "none",
    val messages: List<ChatElement> = emptyList(),
    val pendingApprovals: List<Approval> = emptyList(),
    val inputAvailable: Boolean = false,
    val chatTabs: List<ChatTab> = emptyList(),
    val activeComposerId: String = "",
    val mode: RelayModeInfo = RelayModeInfo(""),
    val model: RelayModelInfo = RelayModelInfo("", ""),
    val windows: List<CursorWindow> = emptyList(),
    val activeWindowId: String = "",
    val composerQueue: ComposerQueueState = ComposerQueueState(),
    val questionnaire: Questionnaire? = null
)

@Serializable
sealed class ChatElement {
    abstract val id: String
    abstract val flatIndex: Int
    abstract val type: String
}

@Serializable
data class HumanMessage(
    override val id: String,
    override val flatIndex: Int,
    val text: String,
    val mentions: List<Mention> = emptyList(),
    val quoted: QuotedText? = null
) : ChatElement() {
    override val type: String = "human"
}

@Serializable
data class Mention(val name: String, val mentionType: String)

@Serializable
data class QuotedText(val text: String)

@Serializable
data class AssistantMessage(
    override val id: String,
    override val flatIndex: Int,
    val text: String,
    val html: String = "",
    val codeBlocks: List<CodeBlockItem> = emptyList()
) : ChatElement() {
    override val type: String = "assistant"
}

@Serializable
data class CodeBlockItem(
    val blockKind: String,
    val filename: String? = null,
    val language: String? = null,
    val code: String,
    val diffLines: List<DiffLine>? = null
)

@Serializable
data class DiffLine(val kind: String, val text: String)

@Serializable
data class ToolCallElement(
    override val id: String,
    override val flatIndex: Int,
    val toolCallId: String,
    val status: String,
    val action: String,
    val details: String,
    val filename: String? = null,
    val additions: Int? = null,
    val deletions: Int? = null,
    val summaryText: String? = null,
    val actions: List<RunAction> = emptyList(),
    val blocked: String? = null,
    val diffBlock: CodeBlockItem? = null
) : ChatElement() {
    override val type: String = "tool"
}

@Serializable
data class ThoughtBlock(
    override val id: String,
    override val flatIndex: Int,
    val duration: String,
    val action: String? = null,
    val detail: String? = null,
    val thoughtKind: String? = null
) : ChatElement() {
    override val type: String = "thought"
}

@Serializable
data class PlanBlock(
    override val id: String,
    override val flatIndex: Int,
    val label: String,
    val title: String,
    val todosCompleted: Int,
    val todosTotal: Int,
    val description: String? = null,
    val descriptionHtml: String? = null,
    val todos: List<PlanTodo> = emptyList(),
    val todosMoreCount: Int = 0,
    val model: String? = null,
    val modelDropdownSelectorPath: String? = null,
    val actions: List<PlanAction> = emptyList()
) : ChatElement() {
    override val type: String = "plan"
}

@Serializable
data class PlanTodo(val text: String, val status: String)

@Serializable
data class PlanAction(val label: String, val type: String, val selectorPath: String)

@Serializable
data class TodoListBlock(
    override val id: String,
    override val flatIndex: Int,
    val title: String,
    val todosCompleted: Int,
    val todosTotal: Int,
    val todos: List<PlanTodo>
) : ChatElement() {
    override val type: String = "todo_list"
}

@Serializable
data class RunCommand(
    override val id: String,
    override val flatIndex: Int,
    val toolCallId: String,
    val description: String,
    val candidates: String,
    val command: String,
    val actions: List<RunAction>
) : ChatElement() {
    override val type: String = "run_command"
}

@Serializable
data class RunAction(val label: String, val type: String, val selectorPath: String)

@Serializable
data class LoadingIndicator(
    override val id: String,
    override val flatIndex: Int,
    val text: String? = null
) : ChatElement() {
    override val type: String = "loading"
}

@Serializable
data class Approval(
    val id: String,
    val description: String,
    val actions: List<ApprovalAction>
)

@Serializable
data class ApprovalAction(val label: String, val type: String, val selectorPath: String)

@Serializable
data class CommandPayload(
    val commandId: String,
    val text: String? = null,
    val selectorPath: String? = null,
    val windowId: String? = null,
    val tabSelectorPath: String? = null,
    val modeId: String? = null,
    val modelId: String? = null,
    val actionType: String? = null,
    val actionSelectorPath: String? = null
)

@Serializable
data class CommandResult(
    val commandId: String,
    val ok: Boolean,
    val error: String? = null,
    val data: JsonElement? = null
)

@Serializable
data class ConnectionStatus(
    val connected: Boolean,
    val reason: String? = null
)
