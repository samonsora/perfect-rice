package com.example.team1application

import android.app.NotificationChannel // 通知チャネルの設定に必要 (Android O以降)
import android.app.NotificationManager // 通知の管理と表示を行う
import android.app.usage.UsageEvents // アプリのフォアグラウンド/バックグラウンドイベントを取得
import android.app.usage.UsageStatsManager // 使用状況統計のメインAPI
import android.content.Context // アプリケーションコンテキスト
import android.os.Build // Androidバージョン判定のため
import androidx.core.app.NotificationCompat // 互換性のある通知を作成
import java.util.Calendar // 時刻判定のため
import android.util.Log
import androidx.work.CoroutineWorker // 👈 追加
import androidx.work.WorkerParameters // 👈 追加


class UsageCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // 監視対象のアプリのパッケージ名リスト (必要に応じて変更)
    private val targetPackages = listOf("com.instagram.android", "com.google.android.youtube")
    // ユーザー設定の就寝時刻 (23時0分を例とする)
    private val bedtimeHour = 5
    // 時刻判定を開始する、就寝時刻より前の分数
    private val checkingBeforeMinutes = 30


    override suspend fun doWork(): Result {
        // 現在時刻と就寝時刻をCalendarオブジェクトで取得
        Log.d("USAGE_CHECK", "WorkerがOSによって起動されました。時刻判定を開始します。")
        val now = Calendar.getInstance()
        val bedtime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // 就寝時刻の10分前の時刻を計算
        val checkTimeStart = bedtime.clone() as Calendar
        checkTimeStart.add(Calendar.MINUTE, -checkingBeforeMinutes)

        // 判定ロジック: 現在時刻が「10分前」〜「就寝時刻」の範囲外なら終了
        if (now.before(checkTimeStart) || now.after(bedtime)) {
            return Result.success() // 成功を返して次の実行を待つ
        }

        // --- アプリ使用状況の検出 ---

        // 判定期間の終わり (現在時刻)
        val endTime = System.currentTimeMillis()
        // 判定期間の始まり (直近5分前)
        val startTime = endTime - (5 * 60 * 1000)

        // UsageStatsManagerのインスタンス取得
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 直近5分間のアプリイベントを取得
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null

        // 期間内の最新のフォアグラウンドイベントを検出
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // アプリがフォアグラウンドに来たイベントを記録
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
            }
        }

        // 3. 判定と通知の実行
        if (currentForegroundPackage != null && currentForegroundPackage in targetPackages) {
            // パッケージ名からアプリ名を取得
            val appName = getAppName(applicationContext, currentForegroundPackage)
            // 通知を表示
            showNotification(
                applicationContext,
                "就寝準備！",
                "現在起動中の「$appName」を終了しましょう！"
            )
        }

        return Result.success() // 処理完了
    }


    // パッケージ名からユーザーに見せるアプリ名を取得する関数
    private fun getAppName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.packageManager
            // アプリケーション情報からラベル（アプリ名）を取得
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            // 失敗時はパッケージ名をそのまま表示
            packageName
        }
    }

    // 通知機能の実装
    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "sleep_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O (API 26) 以降の通知チャネル設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "就寝リマインダー",
                NotificationManager.IMPORTANCE_HIGH // ポップアップ通知のため高重要度
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 通知コンテンツの作成 (NotificationCompatで互換性を保つ)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.btn_star_big_on) // 通知バーのアイコン
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // ポップアップを強制
            .setAutoCancel(true) // 通知をタップしたら消えるように設定

        // 実際の通知の表示
        notificationManager.notify(1001, builder.build())
    }
}