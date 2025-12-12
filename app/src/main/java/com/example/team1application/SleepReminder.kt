package com.example.team1application
import android.app.Application
import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// Androidアプリケーションの初期化を担うカスタムクラス
class SleepReminder : Application() {

    // アプリケーションが起動したときにOSによって一度だけ呼び出される
    override fun onCreate() {
        super.onCreate()

        // バックグラウンド監視のスケジュール設定を開始
        scheduleUsageCheck(this)
    }
    private fun scheduleUsageCheck(context: Context) {

        // 5分ごとに UsageCheckWorker を実行するリクエストを作成
        val periodicRequest = PeriodicWorkRequestBuilder<UsageCheckWorker>(
            repeatInterval = 15, // 実行間隔: 15分
            repeatIntervalTimeUnit = TimeUnit.MINUTES // 単位: 分
        )
            .addTag("UsageCheckTask") // WorkManagerでタスクを識別するためのタグ
            .build()

        // WorkManagerにタスクの予約を依頼する
        // "UsageCheckTask" という名前のタスクが既に存在する場合、現在のものを保持する (KEEP)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UsageCheckTask",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
}