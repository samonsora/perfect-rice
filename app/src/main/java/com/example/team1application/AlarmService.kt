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
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import java.io.File

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentVolume = 0.0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val alarmType = intent?.getStringExtra("ALARM_TYPE") ?: ""
        val currentSnoozeCount = intent?.getIntExtra("CURRENT_SNOOZE_COUNT", 0) ?: 0

        // MainActivityから読み取れるように詳細情報を保存
        getSharedPreferences("alarm_prefs", MODE_PRIVATE).edit {
            putBoolean("is_ringing", true)
            putInt("ringing_alarm_id", alarmId)
            putString("ringing_alarm_type", alarmType)
            putInt("ringing_snooze_count", currentSnoozeCount)
        }

        val alarmActivityIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TYPE", alarmType)
            putExtra("CURRENT_SNOOZE_COUNT", currentSnoozeCount)
        }
        startActivity(alarmActivityIntent)

        val allAlarms = AlarmDataStore.loadAlarms(this)
        val setting = allAlarms.find { it.id == alarmId }

        startForegroundNotification(setting?.name ?: "アラーム", alarmType, alarmId, currentSnoozeCount)

        // 設定を渡して音を再生
        startAlarmSound(setting)

        return START_STICKY
    }

    /**
     * 選択された曲の設定に従って再生する
     */
    private fun startAlarmSound(setting: AlarmSetting?) {
        // 新しい音を鳴らす前に、古いプレイヤーがあれば確実に解放する
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (_: Exception) {
                // 既に停止している場合などのエラーを無視
            } finally {
                it.release() // 確実に解放
                mediaPlayer = null
                Log.d("AlarmService", "Previous MediaPlayer released.")
            }
        }

        val soundName = setting?.soundName ?: "alarmsound1"

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(audioAttributes)

            try {
                // 1. 選択された名前がデフォルトのリソース音(alarmsound1)かどうか判定
                if (soundName == "alarmsound1") {
                    val resId = resources.getIdentifier(soundName, "raw", packageName)
                    val finalResId = if (resId != 0) resId else R.raw.alarmsound1
                    setDataSource(this@AlarmService, "android.resource://$packageName/$finalResId".toUri())
                } else {
                    // 2. それ以外（追加された曲）の場合は、内部ストレージのファイルを直接指定
                    val customFile = File(filesDir, "$soundName.mp3")
                    if (customFile.exists()) {
                        setDataSource(customFile.absolutePath)
                    } else {
                        // ファイルが見つからない場合のセーフティとしてデフォルトを再生
                        val resId = resources.getIdentifier("alarmsound1", "raw", packageName)
                        setDataSource(this@AlarmService, "android.resource://$packageName/$resId".toUri())
                    }
                }

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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startFadeIn(targetVolume: Float) {
        currentVolume = 0.0f
        val interval = 1000L
        val step = targetVolume / 20f

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
                setSound(null, null)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_TYPE", alarmType)
            putExtra("CURRENT_SNOOZE_COUNT", currentSnoozeCount)
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
            Notification.Builder(this).setPriority(Notification.PRIORITY_HIGH)
        }

        val notification = notificationBuilder
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("アラームが鳴っています")
            .setCategory(Notification.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
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