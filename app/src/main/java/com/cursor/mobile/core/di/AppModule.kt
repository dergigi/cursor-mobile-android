package com.cursor.mobile.core.di

import android.content.Context
import com.cursor.mobile.core.network.CursorApiService
import com.cursor.mobile.core.network.RelayAuthService
import com.cursor.mobile.core.network.RelaySocketClient
import com.cursor.mobile.core.network.SseClient
import com.cursor.mobile.core.notification.NotificationHelper
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.BiometricHelper
import com.cursor.mobile.core.update.UpdateManager
import com.cursor.mobile.data.repository.AgentRepository
import com.cursor.mobile.data.repository.LocalRemoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiKeyManager(@ApplicationContext context: Context): ApiKeyManager {
        return ApiKeyManager(context)
    }

    @Provides
    @Singleton
    fun provideCursorApiService(apiKeyManager: ApiKeyManager): CursorApiService {
        return CursorApiService(apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideSseClient(apiKeyManager: ApiKeyManager): SseClient {
        return SseClient(apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideRelayAuthService(apiKeyManager: ApiKeyManager): RelayAuthService {
        return RelayAuthService(apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideRelaySocketClient(apiKeyManager: ApiKeyManager): RelaySocketClient {
        return RelaySocketClient(apiKeyManager)
    }

    @Provides
    @Singleton
    fun provideAgentRepository(
        api: CursorApiService,
        sseClient: SseClient
    ): AgentRepository {
        return AgentRepository(api, sseClient)
    }

    @Provides
    @Singleton
    fun provideLocalRemoteRepository(
        apiKeyManager: ApiKeyManager,
        relayAuthService: RelayAuthService,
        relaySocketClient: RelaySocketClient
    ): LocalRemoteRepository {
        return LocalRemoteRepository(apiKeyManager, relayAuthService, relaySocketClient)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun provideBiometricHelper(@ApplicationContext context: Context): BiometricHelper {
        return BiometricHelper(context)
    }

    @Provides
    @Singleton
    fun provideUpdateManager(@ApplicationContext context: Context): UpdateManager {
        return UpdateManager(context)
    }
}
