package com.lenzbeyer.hathor.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lenzbeyer.hathor.HathorApplication.Companion.DOWNLOAD_CHANNEL_ID
import com.lenzbeyer.hathor.MainActivity
import com.lenzbeyer.hathor.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Foreground service that hosts the download pipeline.
 *
 * Starts/stops based on whether the queue is non-empty. UI subscribes to JobManager state.
 */
@AndroidEntryPoint
class DownloadService : Service() {

    @Inject lateinit var jobManager: JobManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        // TODO: collect jobManager.state and update notification with progress.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE  -> jobManager.pause()
            ACTION_CANCEL -> { jobManager.cancel(); stopSelf() }
            ACTION_START  -> { /* job is enqueued externally before service start */ }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        collectorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification("Idle", "0 / 0", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING,
            )
        } else {
            // API 33: pre-mediaProcessing constant; fall back to plain startForeground.
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(title: String, status: String, progress: Int): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(status)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START  = "hathor.action.START"
        const val ACTION_PAUSE  = "hathor.action.PAUSE"
        const val ACTION_CANCEL = "hathor.action.CANCEL"
    }
}
