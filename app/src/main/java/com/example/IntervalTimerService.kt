package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class IntervalTimerService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        private const val NOTIFICATION_ID = 8801
        private const val CHANNEL_ID = "interval_timer_channel"
    }

    private var serviceJob: Job? = null
    private val serviceExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("IntervalTimerService", "Unhandled exception in serviceScopeCoroutines", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceExceptionHandler + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Ensure startForeground is called immediately within onCreate to avoid ForegroundServiceDidNotStartInTimeException
        startForegroundServiceWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("IntervalTimerService", "Received action: $action")

        if (action == ACTION_START) {
            acquireWakeLock()
            startForegroundServiceWithNotification()
            startTimerLoop()
        } else {
            // Null intents, ACTION_PAUSE, or any other action should stop the service cleanly
            stopTimerLoop()
            releaseWakeLock()
            TimerStateManager.setPlayingState(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "IntervalChime::TimerWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            // Keep the CPU awake. Use a safe fallback timeout of 4 hours
            wakeLock?.acquire(4 * 60 * 60 * 1000L)
            Log.d("IntervalTimerService", "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d("IntervalTimerService", "WakeLock released")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Interval Timer Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the periodic chime ticking accurately when the screen is locked."
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(remainingText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Using standard alarm clock/timer system icon as the notification icon
        val iconRes = android.R.drawable.ic_lock_idle_alarm

        // Interactive "Pause" action accessible directly from the notification drawer
        val pauseIntent = Intent(this, IntervalTimerService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            1,
            pauseIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Bold style highlight for the remaining time
        val styledText = android.text.SpannableStringBuilder().apply {
            append("Remaining time: ")
            val start = length
            append(remainingText)
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                length,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Interval Chime Active")
            .setContentText(styledText)
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Prevent annoying sound or vibration alerts on every single second change
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .build()
    }

    private fun startForegroundServiceWithNotification() {
        val remaining = TimerStateManager.remainingSeconds.value
        val notification = buildNotification(formatTime(remaining))
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e("IntervalTimerService", "Failed to start foreground with mediaPlayback type", e)
            try {
                // Fallback: try starting without mediaPlayback type or stop self if forced to
                startForeground(NOTIFICATION_ID, notification)
            } catch (ex: Throwable) {
                Log.e("IntervalTimerService", "Failed fallback startForeground as well", ex)
                stopSelf()
            }
        }
    }

    private fun updateNotification(remaining: Int) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = buildNotification(formatTime(remaining))
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Throwable) {
            Log.e("IntervalTimerService", "Failed to update notification", e)
        }
    }

    private fun startTimerLoop() {
        if (serviceJob?.isActive == true) return

        serviceJob = serviceScope.launch {
            TimerStateManager.initialize(applicationContext)
            
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                delay(50)
                val now = System.currentTimeMillis()
                val dt = now - lastTime
                if (dt >= 1000) {
                    val secondsElapsed = (dt / 1000).toInt()
                    lastTime += secondsElapsed * 1000

                    val currentRemaining = TimerStateManager.remainingSeconds.value
                    val nextRemaining = currentRemaining - secondsElapsed
                    
                    if (nextRemaining <= 0) {
                        // Play chime & show ripple feedback
                        withContext(Dispatchers.Main) {
                            TimerStateManager.triggerChimeFeedback()
                        }
                        
                        val resetDuration = TimerStateManager.totalDurationSeconds.value
                        TimerStateManager.updateRemainingSeconds(resetDuration)
                        updateNotification(resetDuration)
                    } else {
                        TimerStateManager.updateRemainingSeconds(nextRemaining)
                        updateNotification(nextRemaining)
                    }
                }
            }
        }
    }

    private fun stopTimerLoop() {
        serviceJob?.cancel()
        serviceJob = null
    }

    private fun formatTime(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onDestroy() {
        stopTimerLoop()
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
