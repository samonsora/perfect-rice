package com.example.team1application

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.TimeZone

class UsageCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val checkingBeforeMinutes = 60

    override suspend fun doWork(): Result {
        val context = applicationContext
        val tz = TimeZone.getTimeZone("Asia/Tokyo")

        // 1. 監視対象アプリのロード
        val targetPackages = TargetAppDataStore.loadTargetApps(context)
        if (targetPackages.isEmpty()) {
            Log.d("USAGE_CHECK", "監視対象アプリが設定されていないため終了します。")
            return Result.success()
        }

        // 2. 就寝アラーム(BEDTIME)のデータを取得
        val allAlarms = AlarmDataStore.loadAlarms(context)
        val bedtimeAlarm = allAlarms.find { it.isActive && it.type == AlarmType.BEDTIME }

        if (bedtimeAlarm == null) {
            Log.d("USAGE_CHECK", "有効な就寝アラームがないため終了します。")
            return Result.success()
        }

        // 3. アラーム時刻をパースして Calendar に設定
        val timeParts = bedtimeAlarm.time.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()

        val now: Calendar = Calendar.getInstance(tz)
        val bedtime: Calendar = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 監視開始時刻（アラームの60分前）
        val checkTimeStart = bedtime.clone() as Calendar
        checkTimeStart.add(Calendar.MINUTE, -checkingBeforeMinutes)

        // 4. 時刻判定ロジック
        // ※ 深夜0時をまたぐ設定に対応するため now.after(checkTimeStart) も追加するとより正確です
        if (now.before(checkTimeStart) || now.after(bedtime)) {
            Log.d("USAGE_CHECK", "監視時間外です（現在: ${now.time}, 範囲: ${checkTimeStart.time} ～ ${bedtime.time}）")
            return Result.success()
        }

        // 5. アプリ使用状況の検出 (以下は以前のロジックと同じ)
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = checkTimeStart.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
            }
        }

        // 6. 判定と通知
        if (currentForegroundPackage != null && targetPackages.contains(currentForegroundPackage)) {
            val pm = context.packageManager
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(currentForegroundPackage, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                "アプリ"
            }

            showNotification(context, "夜更かし注意！${appLabel} を終了して、そろそろ寝ませんか？")
        }

        return Result.success()
    }

    private fun showNotification(context: Context, message: String) {
        val title = "就寝リマインダー"
        val channelId = "sleep_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "就寝リマインダー",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "就寝前のアプリ使用を抑制する通知です"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // 振動パターンの追加

        notificationManager.notify(1001, builder.build())
    }
}