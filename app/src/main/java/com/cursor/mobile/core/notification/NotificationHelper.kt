package com.cursor.mobile.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.cursor.mobile.MainActivity
import com.cursor.mobile.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "cursor_agent_updates"
        const val CHANNEL_NAME = "Agent Updates"
        const val CHANNEL_DESCRIPTION = "Notifications for Cursor agent status changes"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun showAgentNotification(
        agentId: String,
        agentName: String,
        status: String,
        message: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("agentId", agentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            agentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$agentName - $status")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(agentId.hashCode(), notification)

        // Stop live activity when agent reaches a terminal state
        if (status in listOf("FINISHED", "ERROR", "CANCELLED", "FAILED")) {
            AgentForegroundService.stop(context)
        }
    }

    fun startLiveActivity(agentId: String, agentName: String, runId: String) {
        AgentForegroundService.start(context, agentId, agentName, runId)
    }

    fun stopLiveActivity() {
        AgentForegroundService.stop(context)
    }
}
