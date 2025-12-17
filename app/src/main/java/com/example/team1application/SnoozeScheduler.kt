package com.example.team1application

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * スヌーズ専用のアラーム登録クラス
 * 「今から5分後に1回だけ鳴らす」責務を持つ
 */
class SnoozeScheduler(private val context: Context) {

    // AlarmManager を取得（OSにアラームを登録するため）
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 5分後にアラームを再登録する
     */
    fun scheduleSnooze(alarmId: Int, minutes: Int) {

        // 現在時刻を取得
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            // 指定分数だけ加算（今回は5分）
            add(Calendar.MINUTE, minutes)
        }

        // AlarmReceiver に渡す Intent を作成
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId) // 元のアラームIDを引き継ぐ
            putExtra("IS_SNOOZE", true)   // スヌーズかどうかのフラグ
        }

        // スヌーズ用 requestCode（通常アラームと衝突しない値）
        val requestCode = alarmId * 1000 + 99

        // PendingIntent を生成
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // === Exact Alarm 許可チェック ===
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("SnoozeScheduler", "❌ Exact Alarm 権限なし")
                return
            }
        }

        // === 安全に Exact Alarm を登録 ===
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e("SnoozeScheduler", "❌ Exact Alarm 設定失敗", e)
        }


        Log.i(
            "SnoozeScheduler",
            "😴 スヌーズ設定: ${minutes}分後 (${calendar.time})"
        )
    }
}
