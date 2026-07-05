package com.cursor.mobile.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cursor.mobile.data.repository.AgentRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AgentPollingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: AgentRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val response = repository.listAgents(limit = 20)

            response.items.forEach { agent ->
                if (agent.status.equals("ACTIVE", ignoreCase = true)) {
                    agent.latestRunId?.let { runId ->
                        try {
                            val run = repository.getRun(agent.id, runId)
                            if (run.status in listOf("FINISHED", "ERROR", "CANCELLED")) {
                                notificationHelper.showAgentNotification(
                                    agentId = agent.id,
                                    agentName = agent.name,
                                    status = run.status,
                                    message = when (run.status) {
                                        "FINISHED" -> run.result?.take(100) ?: "Agent completed successfully"
                                        "ERROR" -> "Agent encountered an error"
                                        "CANCELLED" -> "Agent was cancelled"
                                        else -> "Agent status changed"
                                    }
                                )
                            } else if (run.status == "RUNNING") {
                                notificationHelper.startLiveActivity(agent.id, agent.name, run.id)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "agent_polling"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AgentPollingWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
