package com.example.team1application

// AlarmScheduler.kt
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 全てのアラーム設定を読み込み、スケジュール（設定またはキャンセル）を更新する。
     * @param settings AlarmSettingのリスト
     */
    fun updateAllAlarms(settings: List<AlarmSetting>) {
        // 1. 全てのアラームを一旦キャンセル（設定変更や無効化に対応するため）
        cancelAllExistingAlarms(settings)

        // 2. 🟢 isActiveがtrueの設定のみをスケジュール 🟢
        settings.filter { it.isActive }.forEach { setting ->
            scheduleAlarm(setting)
        }
    }

    /**
     * 特定の設定に基づいて、曜日ごとのアラームをスケジュールする。
     */
    private fun scheduleAlarm(setting: AlarmSetting) {
        val daysOfWeek = DayOfWeekUtils.parseDays(setting.days)
        // time (例: "06:30") を時と分に分解
        val (hour, minute) = setting.time.split(":").map { it.toIntOrNull() ?: 0 }

        daysOfWeek.forEach { dayOfWeek ->
            val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, dayOfWeek)
            val pendingIntent = createPendingIntent(setting.id, requestCode)

            // 直近のトリガー時刻を計算
            val triggerTime = calculateNextTriggerTime(dayOfWeek, hour, minute)

            // 3. アラームの設定 (一週間ごとの繰り返し)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, // 絶対時刻を基準とし、デバイスをスリープ解除する
                triggerTime.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, // 1週間間隔で繰り返し
                pendingIntent
            )
            Log.i("AlarmScheduler", "✅ 設定ID:${setting.id} (${dayOfWeek}) を ${triggerTime.time} にスケジュールしました (RC:$requestCode)")
        }
    }

    // ... (cancelAllExistingAlarms、createPendingIntent、calculateNextTriggerTime は前回の回答と同じロジックを使用)
    private fun cancelAllExistingAlarms(settings: List<AlarmSetting>) {
        settings.forEach { setting ->
            DayOfWeekUtils.parseDays(setting.days).forEach { dayOfWeek ->
                val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, dayOfWeek)
                val pendingIntent = createPendingIntent(setting.id, requestCode)
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    private fun createPendingIntent(settingId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", settingId) // どの設定がトリガーされたかReceiverに伝える
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun calculateNextTriggerTime(dayOfWeek: Int, hour: Int, minute: Int): Calendar {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 現在時刻と比較し、指定時刻が既に過ぎている場合は翌日以降に設定
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 指定の曜日になるまで日付を進める
        while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar
    }
}