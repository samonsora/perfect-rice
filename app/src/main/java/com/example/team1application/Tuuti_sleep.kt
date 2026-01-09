package com.example.team1application

import android.app.NotificationChannel // 通知チャネルの設定に必要 (Android O以降)
import android.app.NotificationManager // 通知の管理と表示を行う
import android.app.usage.UsageEvents // アプリのフォアグラウンド/バックグラウンドイベントを取得
import android.app.usage.UsageStatsManager // 使用状況統計のメインAPI
import android.content.Context // アプリケーションコンテキスト
import android.os.Build // Androidバージョン判定のため
import androidx.core.app.NotificationCompat
import java.util.Calendar // 時刻判定のため
import android.util.Log // ログ出力用
import java.util.Locale // ロケール設定のため
import androidx.work.CoroutineWorker // WorkManagerで非同期処理を行うためのベースクラス
import androidx.work.WorkerParameters // Workerのパラメータを受け取るクラス

class UsageCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    // 監視対象のアプリのパッケージ名リスト
    private val targetPackages = listOf("com.instagram.android", "com.google.android.youtube")
    private val bedtimeHour = 3
    // 時刻判定を開始する、就寝時刻より前の分数 (例: 120分 = 2時間前からチェック開始)
    private val checkingBeforeMinutes = 60

    /**
     * WorkManagerから呼び出されるメインの処理メソッド
     */
    override suspend fun doWork(): Result {
        // 現在時刻と就寝時刻をCalendarオブジェクトで取得
        Log.d("USAGE_CHECK", "WorkerがOSによって起動されました。時刻判定を開始します。")
        val now = Calendar.getInstance() // 現在時刻
        val bedtime = Calendar.getInstance().apply { // 就寝時刻のCalendarオブジェクトを作成
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        // 監視開始時刻（就寝時刻より checkingBeforeMinutes 分前）を計算
        val checkTimeStart = bedtime.clone() as Calendar
        checkTimeStart.add(Calendar.MINUTE, -checkingBeforeMinutes)

        // ログ出力用の時刻文字列整形
        val nowHourMinute = String.format(
            Locale.getDefault(), // 地域の標準ロケールを使用
            "%02d:%02d", // "HH:MM" 形式
            now.get(Calendar.HOUR_OF_DAY),
            now.get(Calendar.MINUTE)
        )
        val checkStartHourMinute = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            checkTimeStart.get(Calendar.HOUR_OF_DAY),
            checkTimeStart.get(Calendar.MINUTE)
        )
        val bedtimeHourMinute = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            bedtime.get(Calendar.HOUR_OF_DAY),
            bedtime.get(Calendar.MINUTE)
        )
        Log.i("USAGE_CHECK_TIME", "現在時刻: $nowHourMinute")
        Log.i("USAGE_CHECK_TIME", "監視開始時刻: $checkStartHourMinute (就寝時刻: $bedtimeHourMinute)")

        // 判定ロジック: 現在時刻が「監視開始時刻」〜「就寝時刻」の範囲外なら、処理せずに終了
        if (now.before(checkTimeStart) || now.after(bedtime)) {
            Log.w("USAGE_CHECK_TIME", "Workerは監視時間外のため、処理をスキップします。")
            return Result.success() // 成功を返して次の実行を待つ
        }
        Log.d("USAGE_CHECK", "--- 監視時間内です。アプリ使用状況の検出を開始します。 ---")

        // --- アプリ使用状況の検出 ---

        // 判定期間の終わり (現在時刻)
        val endTime = System.currentTimeMillis()
        // 判定期間の始まり (監視開始時刻)
        // 注意: ここで `checkTimeStart.timeInMillis` を使うことで、設定した全監視期間内のイベントを取得する
        val startTime = checkTimeStart.timeInMillis

        // UsageStatsManagerのインスタンス取得
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // 設定した期間 (例: 2時間) のアプリイベントを取得
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null

        // 期間内の最新のフォアグラウンドイベントを検出 (ループの最後が最新となる)
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            // アプリがフォアグラウンドに来たイベント (再開された) を記録
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                // 最も新しいフォアグラウンドのパッケージ名が currentForegroundPackage に保持される
                currentForegroundPackage = event.packageName
                Log.d("USAGE_DEBUG", "検出イベント: ${event.packageName} (時刻: ${event.timeStamp})")
            }
        }
        if (currentForegroundPackage == null) {
            Log.d("USAGE_DEBUG", "監視期間中にフォアグラウンドイベントは検出されませんでした。")
        } else {
            Log.d("USAGE_DEBUG", "最新のフォアグラウンドパッケージ: $currentForegroundPackage")
        }

        // 3. 判定と通知の実行
        // 最新のフォアグラウンドパッケージが監視対象リストに含まれているかチェック
        if (currentForegroundPackage != null && currentForegroundPackage in targetPackages) {
            // パッケージ名からユーザーに見せるアプリ名を取得

            // 通知を表示
            showNotification(
                applicationContext,
                "現在起動中のアプリを終了しましょう！" // 終了を促すメッセージ
            )
        }

        return Result.success() // 処理完了
    }
    private fun showNotification(context: Context, message: String) {
        val title = "就寝準備！"
        Log.e("USAGE_NOTIFY", "通知を表示します: $title - $message")
        val channelId = "sleep_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O (API 26) 以降の通知チャネル設定（必須）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "就寝リマインダー",
                NotificationManager.IMPORTANCE_HIGH // 高重要度で設定し、ポップアップ通知（ヘッドアップ通知）を促す
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 通知コンテンツの作成 (NotificationCompatで古いAndroidバージョンとの互換性を保つ)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.btn_star_big_on) // 通知バーに表示される小さなアイコン
            .setContentTitle(title) // 通知のタイトル
            .setContentText(message) // 通知の本文
            .setPriority(NotificationCompat.PRIORITY_HIGH) // ポップアップ通知を促す
            .setAutoCancel(true) // 通知をタップしたら自動で消えるように設定

        // 実際の通知の表示 (ID 1001 で通知を識別・更新)
        notificationManager.notify(1001, builder.build())
    }
}