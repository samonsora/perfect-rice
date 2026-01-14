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

        Log.d("USAGE_CHECK", "--- 監視タスク開始 ---")

        // 1. 監視対象アプリのロード
        val targetPackages = TargetAppDataStore.loadTargetApps(context)
        if (targetPackages.isEmpty()) {
            Log.d("USAGE_CHECK", "⚠️ 監視対象アプリが設定されていません。")
            return Result.success()
        }
        Log.d("USAGE_CHECK", "監視対象: $targetPackages")

        // 2. 就寝アラーム(BEDTIME)のデータを取得
        val allAlarms = AlarmDataStore.loadAlarms(context)
        val bedtimeAlarm = allAlarms.find { it.isActive && it.type == AlarmType.BEDTIME }

        if (bedtimeAlarm == null) {
            Log.d("USAGE_CHECK", "ℹ️ 有効な就寝アラームがないためスキップします。")
            return Result.success()
        }

        // 3. アラーム時刻の設定
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
        if (now.before(checkTimeStart) || now.after(bedtime)) {
            Log.d("USAGE_CHECK", "💤 監視時間外（範囲外）: 現在 ${now.time}, 判定範囲: ${checkTimeStart.time} ～ ${bedtime.time}")
            return Result.success()
        }
        Log.d("USAGE_CHECK", "✅ 監視時間内（範囲内）です。アプリ使用状況をチェックします。")

        // 5. アプリ使用状況の検出
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 15) // 直近15分間の動きを確認

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null

        // ログ用に全イベントをチェック（デバッグ用）
        var eventCount = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            eventCount++
            // 最後に「フォアグラウンド（使用開始）」になったアプリを特定
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
            }
        }

        Log.d("USAGE_CHECK", "検出イベント数: $eventCount, 最新フォアグラウンド: $currentForegroundPackage")

        // 6. 判定と通知
        if (currentForegroundPackage != null && targetPackages.contains(currentForegroundPackage)) {
            val pm = context.packageManager
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(currentForegroundPackage, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                currentForegroundPackage // 名前が取れなければパッケージ名
            }

            Log.d("USAGE_CHECK", "🔔 一致検出！通知を送ります: $appLabel")
            showNotification(context, "夜更かし注意！${appLabel} を終了して、そろそろ寝ませんか？")
        } else {
            Log.d("USAGE_CHECK", "対象アプリは現在使用されていません。")
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
            .setVibrate(longArrayOf(0, 500, 200, 500))

        notificationManager.notify(1001, builder.build())
    }
}