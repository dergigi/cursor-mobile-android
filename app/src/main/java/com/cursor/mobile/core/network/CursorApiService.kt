package com.cursor.mobile.core.network

import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CursorApiService @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        // The Cursor API rejects null fields ("Expected string, received null"),
        // so omit them from request bodies instead of sending explicit nulls.
        explicitNulls = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("CursorApi", message)
                }
            }
            sanitizeHeader { it == HttpHeaders.Authorization }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
        // Fail on non-2xx responses and surface the server's own error message
        // instead of letting an error envelope hit the success deserializer.
        expectSuccess = true
        HttpResponseValidator {
            handleResponseExceptionWithRequest { cause, _ ->
                val response = (cause as? ResponseException)?.response
                    ?: return@handleResponseExceptionWithRequest
                val body = response.bodyAsText()
                val message = try {
                    val obj = Json.parseToJsonElement(body).jsonObject
                    (obj["error"] as? JsonObject)?.get("message")?.jsonPrimitive?.contentOrNull
                        ?: obj["message"]?.jsonPrimitive?.contentOrNull
                } catch (_: Exception) {
                    null
                }
                throw IllegalStateException(
                    message ?: "HTTP ${response.status.value} ${response.status.description}"
                )
            }
        }
    }

    private suspend fun getBaseUrl(): String = apiKeyManager.getBaseUrl()

    private suspend fun HttpRequestBuilder.auth() {
        val key = apiKeyManager.getApiKey() ?: throw IllegalStateException("No API key configured")
        basicAuth(key, "")
        contentType(ContentType.Application.Json)
    }

    // --- Agents ---

    suspend fun createAgent(request: CreateAgentRequest): AgentWithRun {
        return client.post("${getBaseUrl()}/v1/agents") {
            auth()
            setBody(request)
        }.body()
    }

    suspend fun listAgents(limit: Int = 20, cursor: String? = null): AgentListResponse {
        return client.get("${getBaseUrl()}/v1/agents") {
            auth()
            parameter("limit", limit)
            cursor?.let { parameter("cursor", it) }
        }.body()
    }

    suspend fun getAgent(agentId: String): Agent {
        return client.get("${getBaseUrl()}/v1/agents/$agentId") {
            auth()
        }.body()
    }

    suspend fun archiveAgent(agentId: String): AgentIdResponse {
        return client.post("${getBaseUrl()}/v1/agents/$agentId/archive") {
            auth()
        }.body()
    }

    suspend fun unarchiveAgent(agentId: String): AgentIdResponse {
        return client.post("${getBaseUrl()}/v1/agents/$agentId/unarchive") {
            auth()
        }.body()
    }

    suspend fun deleteAgent(agentId: String): AgentIdResponse {
        return client.delete("${getBaseUrl()}/v1/agents/$agentId") {
            auth()
        }.body()
    }

    // --- Runs ---

    suspend fun createRun(agentId: String, request: CreateRunRequest): Run {
        val response = client.post("${getBaseUrl()}/v1/agents/$agentId/runs") {
            auth()
            setBody(request)
        }
        // Unlike GET (which returns a raw Run), POST /v1/agents/{id}/runs wraps the
        // result as {"run": {...}}. Unwrap it, tolerating either shape.
        val element = json.parseToJsonElement(response.bodyAsText())
        val runElement = if (element is JsonObject && "run" in element) element.getValue("run") else element
        return json.decodeFromJsonElement(Run.serializer(), runElement)
    }

    suspend fun listRuns(agentId: String, limit: Int = 20, cursor: String? = null): RunListResponse {
        return client.get("${getBaseUrl()}/v1/agents/$agentId/runs") {
            auth()
            parameter("limit", limit)
            cursor?.let { parameter("cursor", it) }
        }.body()
    }

    suspend fun getRun(agentId: String, runId: String): Run {
        return client.get("${getBaseUrl()}/v1/agents/$agentId/runs/$runId") {
            auth()
        }.body()
    }

    suspend fun cancelRun(agentId: String, runId: String): CancelRunResponse {
        return client.post("${getBaseUrl()}/v1/agents/$agentId/runs/$runId/cancel") {
            auth()
        }.body()
    }

    // --- Usage ---

    suspend fun getUsage(agentId: String, runId: String? = null): UsageResponse {
        return client.get("${getBaseUrl()}/v1/agents/$agentId/usage") {
            auth()
            runId?.let { parameter("runId", it) }
        }.body()
    }

    // --- Artifacts ---

    suspend fun listArtifacts(agentId: String): ArtifactsResponse {
        return client.get("${getBaseUrl()}/v1/agents/$agentId/artifacts") {
            auth()
        }.body()
    }

    suspend fun downloadArtifact(agentId: String, path: String): ArtifactDownloadResponse {
        return client.get("${getBaseUrl()}/v1/agents/$agentId/artifacts/download") {
            auth()
            parameter("path", path)
        }.body()
    }

    // --- Metadata ---

    suspend fun getMe(): MeResponse {
        return client.get("${getBaseUrl()}/v1/me") {
            auth()
        }.body()
    }

    suspend fun listModels(): ModelsResponse {
        return client.get("${getBaseUrl()}/v1/models") {
            auth()
        }.body()
    }

    suspend fun listRepositories(): RepositoriesResponse {
        return client.get("${getBaseUrl()}/v1/repositories") {
            auth()
        }.body()
    }

    // --- Workers / Machines ---

    suspend fun listWorkers(): WorkerListResponse {
        return client.get("${getBaseUrl()}/v1/workers") {
            auth()
        }.body()
    }

    suspend fun getWorker(workerId: String): Worker {
        return client.get("${getBaseUrl()}/v1/workers/$workerId") {
            auth()
        }.body()
    }

    // --- Remote Control ---

    suspend fun createRemoteControl(request: CreateRemoteControlRequest): RemoteControlSession {
        return client.post("${getBaseUrl()}/v1/remote-control") {
            auth()
            setBody(request)
        }.body()
    }

    suspend fun getRemoteControlSession(sessionId: String): RemoteControlSession {
        return client.get("${getBaseUrl()}/v1/remote-control/$sessionId") {
            auth()
        }.body()
    }

    suspend fun listRemoteControlSessions(agentId: String? = null): List<RemoteControlSession> {
        return client.get("${getBaseUrl()}/v1/remote-control") {
            auth()
            agentId?.let { parameter("agentId", it) }
        }.body()
    }

    suspend fun disconnectRemoteControl(sessionId: String): RemoteControlSession {
        return client.post("${getBaseUrl()}/v1/remote-control/$sessionId/disconnect") {
            auth()
        }.body()
    }

    // --- Pull Requests ---

    suspend fun listPullRequests(agentId: String? = null, repoUrl: String? = null): PullRequestListResponse {
        return client.get("${getBaseUrl()}/v1/pull-requests") {
            auth()
            agentId?.let { parameter("agentId", it) }
            repoUrl?.let { parameter("repoUrl", it) }
        }.body()
    }

    suspend fun getPullRequest(prId: String): PullRequestDetail {
        return client.get("${getBaseUrl()}/v1/pull-requests/$prId") {
            auth()
        }.body()
    }

    suspend fun mergePullRequest(prId: String, request: MergeRequest): MergeResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/merge") {
            auth()
            setBody(request)
        }.body()
    }

    suspend fun updatePullRequestBranch(prId: String): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/update-branch") {
            auth()
        }.body()
    }

    suspend fun markPullRequestReady(prId: String): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/ready") {
            auth()
        }.body()
    }

    suspend fun toggleAutoMerge(prId: String, enable: Boolean): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/auto-merge") {
            auth()
            parameter("enable", enable)
        }.body()
    }

    suspend fun closePullRequest(prId: String): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/close") {
            auth()
        }.body()
    }

    suspend fun reopenPullRequest(prId: String): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/reopen") {
            auth()
        }.body()
    }

    suspend fun publishPullRequest(prId: String): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/publish") {
            auth()
        }.body()
    }

    suspend fun fixWithAgent(prId: String): AgentIdResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/fix-with-agent") {
            auth()
        }.body()
    }

    suspend fun addReviewReply(prId: String, threadId: String, request: AddReviewReplyRequest): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/threads/$threadId/replies") {
            auth()
            setBody(request)
        }.body()
    }

    suspend fun resolveReviewThread(prId: String, threadId: String): PrActionResponse {
        return client.post("${getBaseUrl()}/v1/pull-requests/$prId/threads/$threadId/resolve") {
            auth()
        }.body()
    }

    // --- Slash Commands, Skills, Automations ---

    suspend fun listSlashCommands(): SlashCommandsResponse {
        return client.get("${getBaseUrl()}/v1/slash-commands") {
            auth()
        }.body()
    }

    suspend fun listSkills(): SkillsResponse {
        return client.get("${getBaseUrl()}/v1/skills") {
            auth()
        }.body()
    }

    suspend fun listAutomations(): AutomationsResponse {
        return client.get("${getBaseUrl()}/v1/automations") {
            auth()
        }.body()
    }

    suspend fun runAutomation(request: RunAutomationRequest): AgentWithRun {
        return client.post("${getBaseUrl()}/v1/automations/run") {
            auth()
            setBody(request)
        }.body()
    }

    // --- MCP Servers ---

    suspend fun listMcpServers(): McpServersResponse {
        return client.get("${getBaseUrl()}/v1/mcp/servers") {
            auth()
        }.body()
    }

    suspend fun getMcpServer(serverId: String): McpServer {
        return client.get("${getBaseUrl()}/v1/mcp/servers/$serverId") {
            auth()
        }.body()
    }

    suspend fun listMcpTools(serverId: String): McpToolsResponse {
        return client.get("${getBaseUrl()}/v1/mcp/servers/$serverId/tools") {
            auth()
        }.body()
    }

    suspend fun invokeMcpTool(request: McpInvokeRequest): McpInvokeResponse {
        return client.post("${getBaseUrl()}/v1/mcp/invoke") {
            auth()
            setBody(request)
        }.body()
    }
}
