package com.example.team1application

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val logDateFormat = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", java.util.Locale.JAPAN).apply {
        timeZone = java.util.TimeZone.getTimeZone("Asia/Tokyo")
    }

    fun syncWithStorage() {
        val allSettings = AlarmDataStore.loadAlarms(context)
        Log.d("AlarmScheduler", "🔄 同期開始: ストレージ内の全件数 = ${allSettings.size}")
        updateAllAlarms(allSettings)
    }

    private fun updateAllAlarms(settings: List<AlarmSetting>) {
        cancelAllExistingAlarms(settings)

        // 💡 フィルタリングの結果をログ出力
        val activeAlarms = settings.filter { it.isActive }
        Log.d("AlarmScheduler", "🔔 アクティブなアラーム数: ${activeAlarms.size}")

        if (activeAlarms.isEmpty()) {
            Log.w("AlarmScheduler", "⚠️ アクティブなアラームがないため、登録をスキップしました。")
        }

        activeAlarms.forEach { setting ->
            scheduleAlarm(setting)
        }
    }

    private fun scheduleAlarm(setting: AlarmSetting) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "❌ Exactアラーム権限がありません")
                return
            }
        }

        val daysOfWeek = DayOfWeekUtils.parseDays(setting.days)
        val timeParts = setting.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        if (daysOfWeek.isEmpty()) {
            // 曜日指定がない場合は「毎日（次の指定時刻）」として登録
            Log.d("AlarmScheduler", "ID:${setting.id} は曜日指定がないため、次回の時刻にのみ設定します。")

            // 曜日を考慮せず、単純に次の「時:分」を計算
            val calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tokyo")).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // すでに過ぎている時刻なら明日にする
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // 曜日指定なし用の RequestCode（例えば 0 を使う）
            val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, 0)
            val pendingIntent = createPendingIntent(setting.id, requestCode)

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.i("AlarmScheduler", "✅ 予約完了(曜日指定なし): ID:${setting.id}, 次回発火:${logDateFormat.format(calendar.time)}")

        } else {
            // 曜日指定がある場合は従来通りループで登録
            daysOfWeek.forEach { dayOfWeek ->
                val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, dayOfWeek)
                val pendingIntent = createPendingIntent(setting.id, requestCode)
                val triggerTime = calculateNextTriggerTime(dayOfWeek, hour, minute).timeInMillis

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.i("AlarmScheduler", "✅ 予約完了(曜日指定あり): ID:${setting.id}, 曜日コード:$dayOfWeek")
            }
        }
    }

    private fun cancelAllExistingAlarms(settings: List<AlarmSetting>) {
        settings.forEach { setting ->
            for (day in 1..7) {
                val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, day)
                alarmManager.cancel(createPendingIntent(setting.id, requestCode))
            }
        }
    }

    private fun createPendingIntent(settingId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", settingId)
            // 初回のアラームのスヌーズ回数は 0
            putExtra("CURRENT_SNOOZE_COUNT", 0)
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
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar
    }
}