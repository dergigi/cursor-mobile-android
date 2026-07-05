package com.cursor.mobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Agent(
    val id: String,
    val name: String,
    val status: String,
    val env: Env? = null,
    val repos: List<RepoConfig>? = null,
    val workOnCurrentBranch: Boolean? = null,
    val autoCreatePR: Boolean? = null,
    val url: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val latestRunId: String? = null
)

@Serializable
data class Env(
    val type: String,
    val name: String? = null
)

@Serializable
data class RepoConfig(
    val url: String,
    val startingRef: String? = null,
    val prUrl: String? = null
)

@Serializable
data class Run(
    val id: String,
    val agentId: String,
    val status: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val durationMs: Long? = null,
    val result: String? = null,
    val git: GitInfo? = null
)

@Serializable
data class GitInfo(
    val branches: List<BranchInfo>? = null
)

@Serializable
data class BranchInfo(
    val repoUrl: String,
    val branch: String? = null,
    val prUrl: String? = null
)

@Serializable
data class AgentWithRun(
    val agent: Agent,
    val run: Run? = null
)

@Serializable
data class AgentListResponse(
    val items: List<Agent>,
    val nextCursor: String? = null
)

@Serializable
data class RunListResponse(
    val items: List<Run>,
    val nextCursor: String? = null
)

@Serializable
data class CreateAgentRequest(
    val prompt: PromptInput,
    val model: ModelSelection? = null,
    val name: String? = null,
    val repos: List<RepoConfig>? = null,
    val workOnCurrentBranch: Boolean? = null,
    val autoCreatePR: Boolean? = null,
    val mode: String? = null,
    val agentId: String? = null
)

@Serializable
data class CreateRunRequest(
    val prompt: PromptInput,
    val mode: String? = null
)

@Serializable
data class PromptInput(
    val text: String,
    val images: List<ImageInput>? = null
)

@Serializable
data class ImageInput(
    val data: String? = null,
    val mimeType: String? = null,
    val url: String? = null
)

@Serializable
data class ModelSelection(
    val id: String,
    val params: List<ModelParam>? = null
)

@Serializable
data class ModelParam(
    val id: String,
    val value: String
)

@Serializable
data class ModelInfo(
    val id: String,
    val displayName: String,
    val description: String? = null,
    val aliases: List<String>? = null,
    val parameters: List<ModelParameter>? = null,
    val variants: List<ModelVariant>? = null
)

@Serializable
data class ModelParameter(
    val id: String,
    val displayName: String? = null,
    val values: List<ParameterValue>? = null
)

@Serializable
data class ParameterValue(
    val value: String,
    val displayName: String? = null
)

@Serializable
data class ModelVariant(
    val params: List<ModelParam>,
    val displayName: String,
    val description: String? = null,
    val isDefault: Boolean? = null
)

@Serializable
data class ModelsResponse(
    val items: List<ModelInfo>
)

@Serializable
data class RepositoryItem(
    val url: String
)

@Serializable
data class RepositoriesResponse(
    val items: List<RepositoryItem>
)

@Serializable
data class UsageInfo(
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val cacheWriteTokens: Long = 0,
    val cacheReadTokens: Long = 0,
    val totalTokens: Long = 0
)

@Serializable
data class RunUsage(
    val id: String,
    val usageUuid: String? = null,
    val usage: UsageInfo
)

@Serializable
data class UsageResponse(
    val totalUsage: UsageInfo,
    val runs: List<RunUsage>
)

@Serializable
data class ArtifactItem(
    val path: String,
    val sizeBytes: Long,
    val updatedAt: String? = null
)

@Serializable
data class ArtifactsResponse(
    val items: List<ArtifactItem>
)

@Serializable
data class ArtifactDownloadResponse(
    val url: String,
    val expiresAt: String? = null
)

@Serializable
data class MeResponse(
    val apiKeyName: String? = null,
    val createdAt: String? = null,
    val userId: Int? = null,
    val userEmail: String? = null,
    val userFirstName: String? = null,
    val userLastName: String? = null,
    val plan: SubscriptionPlan? = null
)

@Serializable
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val tier: String? = null,
    val isPaid: Boolean = false,
    val features: List<String> = emptyList()
)

@Serializable
data class CancelRunResponse(
    val id: String
)

@Serializable
data class AgentIdResponse(
    val id: String
)

@Serializable
data class Worker(
    val id: String,
    val type: String, // "cloud", "self_hosted", "local"
    val name: String? = null,
    val status: String? = null,
    val machineId: String? = null
)

@Serializable
data class WorkerListResponse(
    val items: List<Worker>,
    val nextCursor: String? = null
)

@Serializable
data class RemoteControlSession(
    val id: String,
    val agentId: String,
    val runId: String,
    val workerId: String,
    val status: String,
    val machineName: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateRemoteControlRequest(
    val agentId: String,
    val runId: String,
    val workerId: String
)

@Serializable
data class PullRequest(
    val id: String,
    val number: Int,
    val title: String,
    val url: String,
    val state: String,
    val repoUrl: String,
    val branch: String? = null,
    val baseBranch: String? = null,
    val isDraft: Boolean = false,
    val mergeable: Boolean? = null,
    val mergeState: String? = null
)

@Serializable
data class PullRequestListResponse(
    val items: List<PullRequest>,
    val nextCursor: String? = null
)

@Serializable
data class DiffFile(
    val path: String,
    val changeType: String, // "added", "modified", "deleted"
    val additions: Int = 0,
    val deletions: Int = 0,
    val patch: String? = null
)

@Serializable
data class PullRequestDetail(
    val pr: PullRequest,
    val files: List<DiffFile> = emptyList(),
    val commits: List<CommitInfo> = emptyList(),
    val reviewThreads: List<ReviewThread> = emptyList(),
    val deployments: List<Deployment> = emptyList()
)

@Serializable
data class CommitInfo(
    val sha: String,
    val message: String,
    val author: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ReviewThread(
    val id: String,
    val path: String? = null,
    val line: Int? = null,
    val body: String,
    val author: String? = null,
    val isResolved: Boolean = false,
    val replies: List<ReviewReply> = emptyList()
)

@Serializable
data class ReviewReply(
    val id: String,
    val body: String,
    val author: String? = null,
    val createdAt: String? = null
)

@Serializable
data class AddReviewReplyRequest(
    val body: String
)

@Serializable
data class Deployment(
    val id: String,
    val environment: String,
    val state: String,
    val url: String? = null,
    val createdAt: String? = null
)

@Serializable
data class MergeRequest(
    val prId: String,
    val method: String, // "merge", "squash", "rebase"
    val title: String? = null,
    val message: String? = null,
    val deleteBranch: Boolean = false
)

@Serializable
data class MergeResponse(
    val success: Boolean,
    val message: String? = null,
    val prUrl: String? = null
)

@Serializable
data class PrActionResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class SlashCommand(
    val id: String,
    val name: String,
    val description: String,
    val args: List<CommandArg> = emptyList()
)

@Serializable
data class CommandArg(
    val id: String,
    val name: String,
    val description: String? = null,
    val required: Boolean = false
)

@Serializable
data class SlashCommandsResponse(
    val items: List<SlashCommand>
)

@Serializable
data class Skill(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null
)

@Serializable
data class SkillsResponse(
    val items: List<Skill>
)

@Serializable
data class Automation(
    val id: String,
    val name: String,
    val description: String? = null,
    val trigger: String? = null,
    val enabled: Boolean = false
)

@Serializable
data class AutomationsResponse(
    val items: List<Automation>
)

@Serializable
data class RunAutomationRequest(
    val automationId: String,
    val agentId: String? = null,
    val prompt: PromptInput? = null
)

@Serializable
data class McpServer(
    val id: String,
    val name: String,
    val description: String? = null,
    val type: String, // "sse", "stdio", "http"
    val url: String? = null,
    val command: String? = null,
    val args: List<String> = emptyList(),
    val env: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = false,
    val status: String? = null
)

@Serializable
data class McpServersResponse(
    val items: List<McpServer>
)

@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: String? = null
)

@Serializable
data class McpToolsResponse(
    val items: List<McpTool>
)

@Serializable
data class McpInvokeRequest(
    val serverId: String,
    val toolName: String,
    val arguments: Map<String, String> = emptyMap()
)

@Serializable
data class McpInvokeResponse(
    val result: String? = null,
    val success: Boolean = true,
    val error: String? = null
)

// SSE event types
sealed class SseEvent {
    data class Status(val runId: String, val status: String) : SseEvent()
    data class Assistant(val text: String) : SseEvent()
    data class Thinking(val text: String) : SseEvent()
    data class ToolCall(
        val callId: String,
        val name: String,
        val status: String,
        val args: String? = null,
        val result: String? = null
    ) : SseEvent()
    data class Result(
        val runId: String,
        val status: String,
        val text: String? = null,
        val durationMs: Long? = null
    ) : SseEvent()
    data class Error(val code: String, val message: String) : SseEvent()
    data object Heartbeat : SseEvent()
    data object Done : SseEvent()
}

// Chat message for UI
data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCall: ToolCallInfo? = null
)

enum class MessageRole { USER, ASSISTANT, THINKING, TOOL, SYSTEM, RESULT }

data class ToolCallInfo(
    val callId: String,
    val name: String,
    val status: String,
    val args: String? = null,
    val result: String? = null
)
