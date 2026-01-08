package com.example.team1application

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.content.edit
import androidx.core.net.toUri

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentVolume = 0.0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntOfDefault("ALARM_ID", -1) ?: -1
        val alarmType = intent?.getStringExtra("ALARM_TYPE") ?: ""
        val currentSnoozeCount = intent?.getIntExtra("CURRENT_SNOOZE_COUNT", 0) ?: 0

        getSharedPreferences("alarm_prefs", MODE_PRIVATE).edit { putBoolean("is_ringing", true) }

        // 1. データストアから最新の設定を読み込む
        val allAlarms = AlarmDataStore.loadAlarms(this)
        val setting = allAlarms.find { it.id == alarmId }

        // 2. 通知の開始（Foreground Service 必須）
        // アラーム名がある場合は通知に表示
        startForegroundNotification(setting?.name ?: "アラーム", alarmType, alarmId, currentSnoozeCount)

        // 3. アラーム音の再生
        if (setting != null) {
            startAlarmSound(setting)
        } else {
            // 設定が見つからない場合のフォールバック
            startAlarmSound(null)
        }

        return START_STICKY
    }

    private fun startAlarmSound(setting: AlarmSetting?) {
        val soundName = setting?.soundName ?: "alarmsound1"

        // 💡 2. 文字列からリソースIDを動的に取得
        val resId = resources.getIdentifier(soundName, "raw", packageName)

        // 万が一IDが見つからない場合のフォールバック
        val finalResId = if (resId != 0) resId else R.raw.alarmsound1

        val uri = "android.resource://${packageName}/$finalResId".toUri()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)
            setDataSource(this@AlarmService, uri)
            setAudioAttributes(audioAttributes)
            isLooping = true
            prepare()

            val targetVolume = setting?.volume ?: 0.5f
            if (setting?.fadeIn == true) {
                setVolume(0f, 0f)
                startFadeIn(targetVolume)
            } else {
                setVolume(targetVolume, targetVolume)
            }

            start()
        }
    }

    private fun startFadeIn(targetVolume: Float) {
        currentVolume = 0.0f
        val interval = 1000L // 1秒ごとに更新
        val step = targetVolume / 20f // 20秒かけて最大にする

        val runnable = object : Runnable {
            override fun run() {
                if (currentVolume < targetVolume) {
                    currentVolume += step
                    mediaPlayer?.setVolume(currentVolume, currentVolume)
                    handler.postDelayed(this, interval)
                }
            }
        }
        handler.post(runnable)
    }

    private fun startForegroundNotification(title: String, alarmType: String, alarmId: Int, currentSnoozeCount: Int) {
        val channelId = "alarm_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "アラーム通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "アラーム鳴動中に表示されます"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null) // 通知音自体は MediaPlayer で鳴らすので消音
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TYPE", alarmType)
            putExtra("CURRENT_SNOOZE_COUNT", currentSnoozeCount) // ここが重要！
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setPriority(Notification.PRIORITY_HIGH) // API 26 未満のための互換性
        }

        val notification = notificationBuilder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("アラームが鳴っています")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true) // フルスクリーン
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        getSharedPreferences("alarm_prefs", MODE_PRIVATE).edit { putBoolean("is_ringing", false) }
        super.onDestroy()
    }
}

// Intent拡張関数（APIレベルによる挙動の差を吸収）
fun Intent.getIntOfDefault(key: String, defaultValue: Int): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getIntExtra(key, defaultValue)
    } else {
        this.getIntExtra(key, defaultValue)
    }
}