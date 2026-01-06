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
 * AlarmScheduler（絶対時刻指定）と異なり、System.currentTimeMillis() という
 * 「世界共通の通し番号（Unix Time）」を基準にしているため、タイムゾーンの影響を受けない。
 * 「今」という絶対的な瞬間に、物理的な「5分（ミリ秒）」を加算しているだけなので、
 * アプリ内部の時計が何時を指していても、正確な「5分後」が計算される。
 */
class SnoozeScheduler(private val context: Context) {

    // AlarmManager を取得（OSにアラームを登録するため）
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    fun scheduleSnooze(alarmId: Int, minutes: Int, nextCount: Int) {

        // 現在時刻を取得
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            /**
             * Calendar.add メソッドの特性:
             * 指定した単位（MINUTE）を加算すると、24時を超えた場合でも
             * 上位の単位（HOUR, DAY）を自動的に繰り上げてくれる。
             * そのため、「24時を超えたら翌日にする」という手動判定ロジックは不要。
             */
            // 指定分数だけ加算
            add(Calendar.MINUTE, minutes)
        }

        // AlarmReceiver に渡す Intent を作成
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId) // 元のアラームIDを引き継ぐ
            putExtra("CURRENT_SNOOZE_COUNT", nextCount) // 次の回数
            putExtra("IS_SNOOZE", true)   // スヌーズかどうかのチェック
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
