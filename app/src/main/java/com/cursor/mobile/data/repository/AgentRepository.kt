package com.cursor.mobile.data.repository

import com.cursor.mobile.core.network.CursorApiService
import com.cursor.mobile.core.network.SseClient
import com.cursor.mobile.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val api: CursorApiService,
    private val sseClient: SseClient
) {
    // --- Agents ---
    suspend fun createAgent(request: CreateAgentRequest) = api.createAgent(request)
    suspend fun listAgents(limit: Int = 20, cursor: String? = null) = api.listAgents(limit, cursor)
    suspend fun getAgent(agentId: String) = api.getAgent(agentId)
    suspend fun archiveAgent(agentId: String) = api.archiveAgent(agentId)
    suspend fun unarchiveAgent(agentId: String) = api.unarchiveAgent(agentId)
    suspend fun deleteAgent(agentId: String) = api.deleteAgent(agentId)

    // --- Runs ---
    suspend fun createRun(agentId: String, request: CreateRunRequest) = api.createRun(agentId, request)
    suspend fun listRuns(agentId: String, limit: Int = 20, cursor: String? = null) = api.listRuns(agentId, limit, cursor)
    suspend fun getRun(agentId: String, runId: String) = api.getRun(agentId, runId)
    suspend fun cancelRun(agentId: String, runId: String) = api.cancelRun(agentId, runId)

    // --- Streaming ---
    fun streamRun(agentId: String, runId: String): Flow<SseEvent> = sseClient.streamRun(agentId, runId)

    // --- Usage ---
    suspend fun getUsage(agentId: String, runId: String? = null) = api.getUsage(agentId, runId)

    // --- Artifacts ---
    suspend fun listArtifacts(agentId: String) = api.listArtifacts(agentId)
    suspend fun downloadArtifact(agentId: String, path: String) = api.downloadArtifact(agentId, path)

    // --- Metadata ---
    suspend fun getMe() = api.getMe()
    suspend fun listModels() = api.listModels()
    suspend fun listRepositories() = api.listRepositories()

    // --- Workers / Machines ---
    suspend fun listWorkers() = api.listWorkers()
    suspend fun getWorker(workerId: String) = api.getWorker(workerId)

    // --- Remote Control ---
    suspend fun createRemoteControl(request: CreateRemoteControlRequest) = api.createRemoteControl(request)
    suspend fun getRemoteControlSession(sessionId: String) = api.getRemoteControlSession(sessionId)
    suspend fun listRemoteControlSessions(agentId: String? = null) = api.listRemoteControlSessions(agentId)
    suspend fun disconnectRemoteControl(sessionId: String) = api.disconnectRemoteControl(sessionId)

    // --- Pull Requests ---
    suspend fun listPullRequests(agentId: String? = null, repoUrl: String? = null) = api.listPullRequests(agentId, repoUrl)
    suspend fun getPullRequest(prId: String) = api.getPullRequest(prId)
    suspend fun mergePullRequest(prId: String, request: MergeRequest) = api.mergePullRequest(prId, request)
    suspend fun updatePullRequestBranch(prId: String) = api.updatePullRequestBranch(prId)
    suspend fun markPullRequestReady(prId: String) = api.markPullRequestReady(prId)
    suspend fun toggleAutoMerge(prId: String, enable: Boolean) = api.toggleAutoMerge(prId, enable)
    suspend fun closePullRequest(prId: String) = api.closePullRequest(prId)
    suspend fun reopenPullRequest(prId: String) = api.reopenPullRequest(prId)
    suspend fun publishPullRequest(prId: String) = api.publishPullRequest(prId)
    suspend fun fixWithAgent(prId: String) = api.fixWithAgent(prId)
    suspend fun addReviewReply(prId: String, threadId: String, request: AddReviewReplyRequest) =
        api.addReviewReply(prId, threadId, request)
    suspend fun resolveReviewThread(prId: String, threadId: String) = api.resolveReviewThread(prId, threadId)

    // --- Slash Commands, Skills, Automations ---
    suspend fun listSlashCommands() = api.listSlashCommands()
    suspend fun listSkills() = api.listSkills()
    suspend fun listAutomations() = api.listAutomations()
    suspend fun runAutomation(request: RunAutomationRequest) = api.runAutomation(request)

    // --- MCP Servers ---
    suspend fun listMcpServers() = api.listMcpServers()
    suspend fun getMcpServer(serverId: String) = api.getMcpServer(serverId)
    suspend fun listMcpTools(serverId: String) = api.listMcpTools(serverId)
    suspend fun invokeMcpTool(request: McpInvokeRequest) = api.invokeMcpTool(request)
}
