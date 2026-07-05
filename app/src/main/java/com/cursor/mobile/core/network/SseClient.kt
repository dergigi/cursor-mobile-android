package com.cursor.mobile.core.network

import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.data.model.SseEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseClient @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS) // SSE needs long read
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    fun streamRun(agentId: String, runId: String): Flow<SseEvent> = callbackFlow {
        val baseUrl = apiKeyManager.getBaseUrl()
        val apiKey = apiKeyManager.getApiKey() ?: throw IllegalStateException("No API key")

        val request = Request.Builder()
            .url("$baseUrl/v1/agents/$agentId/runs/$runId/stream")
            .header("Accept", "text/event-stream")
            .header("Authorization", Credentials.basic(apiKey, ""))
            .build()

        val factory = EventSources.createFactory(client)

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                val sseEvent = parseSseEvent(type, data)
                if (sseEvent != null) {
                    trySend(sseEvent)
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t ?: Exception("SSE connection failed: ${response?.code}"))
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }

    private fun parseSseEvent(type: String?, data: String): SseEvent? {
        if (data.isBlank()) return null

        return try {
            val jsonElement = json.parseToJsonElement(data)
            val obj = jsonElement as? JsonObject ?: return null

            when (type) {
                "status" -> SseEvent.Status(
                    runId = obj["runId"]?.jsonPrimitive?.content ?: "",
                    status = obj["status"]?.jsonPrimitive?.content ?: ""
                )
                "assistant" -> SseEvent.Assistant(
                    text = obj["text"]?.jsonPrimitive?.content ?: ""
                )
                "thinking" -> SseEvent.Thinking(
                    text = obj["text"]?.jsonPrimitive?.content ?: ""
                )
                "tool_call" -> SseEvent.ToolCall(
                    callId = obj["callId"]?.jsonPrimitive?.content ?: "",
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    status = obj["status"]?.jsonPrimitive?.content ?: "",
                    args = obj["args"]?.toString(),
                    result = obj["result"]?.toString()
                )
                "result" -> SseEvent.Result(
                    runId = obj["runId"]?.jsonPrimitive?.content ?: "",
                    status = obj["status"]?.jsonPrimitive?.content ?: "",
                    text = obj["text"]?.jsonPrimitive?.contentOrNull,
                    durationMs = obj["durationMs"]?.jsonPrimitive?.longOrNull
                )
                "error" -> SseEvent.Error(
                    code = obj["code"]?.jsonPrimitive?.content ?: "",
                    message = obj["message"]?.jsonPrimitive?.content ?: ""
                )
                "heartbeat" -> SseEvent.Heartbeat
                "done" -> SseEvent.Done
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
