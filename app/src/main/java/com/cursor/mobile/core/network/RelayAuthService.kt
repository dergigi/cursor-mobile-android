package com.cursor.mobile.core.network

import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.data.model.HealthResponse
import com.cursor.mobile.data.model.LoginRequest
import com.cursor.mobile.data.model.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelayAuthService @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = Logger.ANDROID
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    private suspend fun baseUrl(): String {
        return apiKeyManager.getRelayBaseUrl()
            ?: throw IllegalStateException("No relay base URL configured")
    }

    suspend fun login(password: String): LoginResponse {
        val url = baseUrl()
        val response: HttpResponse = client.post("$url/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(password))
        }
        return response.body()
    }

    suspend fun health(): HealthResponse {
        val url = baseUrl()
        return client.get("$url/health").body()
    }

    suspend fun loginWithUrl(url: String, password: String): LoginResponse {
        val trimmedUrl = url.trim().trimEnd('/')
        val response: HttpResponse = client.post("$trimmedUrl/api/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(password))
        }
        return response.body()
    }

    suspend fun healthWithUrl(url: String): HealthResponse {
        val trimmedUrl = url.trim().trimEnd('/')
        return client.get("$trimmedUrl/health").body()
    }
}
