package com.example.team1application

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentVolume = 0.0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        // スヌーズ回数を受け取る（デフォルトは0）
        val snoozeCount = intent?.getIntExtra("CURRENT_SNOOZE_COUNT", 0) ?: 0

        val allAlarms = AlarmDataStore.loadAlarms(this)
        val setting = allAlarms.find { it.id == alarmId }

        // 2. 通知の開始（Foreground Service 必須）
        // アラーム名がある場合は通知に表示
        // 通知にスヌーズ回数情報を渡す
        startForegroundNotification(setting?.name ?: "アラーム", alarmId, snoozeCount)

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
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // 非推奨の setAudioStreamType を避け、AudioAttributes を使用
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        mediaPlayer = MediaPlayer().apply {
            setDataSource(this@AlarmService, uri)
            setAudioAttributes(audioAttributes)
            isLooping = true
            prepare()

            // 音量設定の適用
            val targetVolume = setting?.volume ?: 0.5f
            if (setting?.fadeIn == true) {
                // フェードインが有効な場合、0から開始
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
        val step = targetVolume / 10f // 10秒かけて最大にする

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

    private fun startForegroundNotification(title: String, alarmId: Int, snoozeCount: Int) {
        val channelId = "alarm_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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