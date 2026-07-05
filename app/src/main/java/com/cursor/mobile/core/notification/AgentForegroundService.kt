package com.cursor.mobile.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cursor.mobile.MainActivity
import com.cursor.mobile.R
import kotlinx.coroutines.*

class AgentForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val agentId = intent?.getStringExtra(EXTRA_AGENT_ID) ?: ""
        val agentName = intent?.getStringExtra(EXTRA_AGENT_NAME) ?: "Agent"
        val runId = intent?.getStringExtra(EXTRA_RUN_ID) ?: ""

        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val notification = buildProgressNotification(agentId, agentName, runId, 0, true)
        startForeground(NOTIFICATION_ID, notification)

        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            // Simulate progress updates; in production, poll the run status via API
            var progress = 0
            while (progress < 100 && isActive) {
                delay(2000)
                progress += 5
                val updated = buildProgressNotification(agentId, agentName, runId, progress.coerceAtMost(100), progress < 100)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, updated)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildProgressNotification(
        agentId: String,
        agentName: String,
        runId: String,
        progress: Int,
        indeterminate: Boolean
    ): android.app.Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("agentId", agentId)
            putExtra("runId", runId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AgentForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(agentName)
            .setContentText("Agent is running${if (progress > 0) " • $progress%" else ""}")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, indeterminate)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Agent Live Activity",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active Cursor agents on lock screen"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "cursor_agent_live_activity"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_AGENT_ID = "agent_id"
        const val EXTRA_AGENT_NAME = "agent_name"
        const val EXTRA_RUN_ID = "run_id"
        const val ACTION_STOP = "com.cursor.mobile.ACTION_STOP_LIVE_ACTIVITY"

        fun start(context: Context, agentId: String, agentName: String, runId: String) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                putExtra(EXTRA_AGENT_ID, agentId)
                putExtra(EXTRA_AGENT_NAME, agentName)
                putExtra(EXTRA_RUN_ID, runId)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, AgentForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
