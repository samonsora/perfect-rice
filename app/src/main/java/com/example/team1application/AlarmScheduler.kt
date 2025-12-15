package com.example.team1application

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import java.util.Calendar

/**
 * ダミーのアラーム設定を作成する関数
 * → 実際にはRoomやDataStoreなどから読み込む前提の仮データ
 */
fun getDummyAlarmSettings(): SnapshotStateList<AlarmSetting> {
    // SnapshotStateList にしてUIが再描画を検知できるようにする
    return listOf(
        AlarmSetting(1, "05:51", "土, 日, 月, 火, 水, 木, 金", true)
    ).toMutableStateList()
}

/**
 * アラームを実際に OS（AlarmManager）にスケジュール設定するクラス。
 * 初期設定や変更処理は AlarmInitializer に任せ、
 * 「実際にアラームを OS に登録する責務」だけを受け持つ。
 */
class AlarmScheduler(private val context: Context) {

    // AlarmManager を取得。OS にアラームを依頼するためのシステムサービス。
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * すべてのアラーム設定に基づいて、OS のアラーム登録を更新する。
     * 1. 既存アラームを全キャンセル
     * 2. isActive = true のアラームだけ再登録
     */
    fun updateAllAlarms(settings: List<AlarmSetting>) {
        // まず全アラームをキャンセル（変更がある可能性があるため）
        cancelAllExistingAlarms(settings)

        // 次に有効な設定のみ OS に登録
        settings.filter { it.isActive }.forEach { setting ->
            scheduleAlarm(setting)
        }
    }

    /**
     * 1つの設定について「曜日ごと」にアラームを exact で設定する。
     * setExactAndAllowWhileIdle を使うため weekly repeat は使えない。
     * → そのため、Receiver 側で「次の週の同じ時刻に再スケジュール」する方式にする。
     */
    private fun scheduleAlarm(setting: AlarmSetting) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("AlarmScheduler", "❌ Exact アラーム許可がありません")
                // 必要なら設定画面へ誘導
                return
            }
        }

        // "月, 火, 水" → Android の DayOfWeek (Calendar.MONDAY etc) に変換
        val daysOfWeek = DayOfWeekUtils.parseDays(setting.days)

        // "06:30" を hour と minute に分解
        val (hour, minute) = setting.time.split(":").map { it.toInt() }

        // アラームを曜日ごとに設定
        daysOfWeek.forEach { dayOfWeek ->

            // 各曜日ごとに unique な requestCode を作成
            val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, dayOfWeek)

            // 対象アラームを識別する PendingIntent を作成
            val pendingIntent = createPendingIntent(setting.id, requestCode)

            // 次回の発火時刻（ミリ秒）を計算
            val triggerTime = calculateNextTriggerTime(dayOfWeek, hour, minute).timeInMillis

            // ---------- 🔥 ここが最重要：Exact アラームの登録 🔥 ----------
            // 端末が Doze でも正確な時刻で発火させたい場合はこれ一択
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,  // スリープでも起こす
                triggerTime,              // 正確な発火時刻
                pendingIntent
            )
            // -----------------------------------------------------------------

            Log.i(
                "AlarmScheduler",
                "🔔 EXACT アラーム登録: ID:${setting.id}, 曜日:$dayOfWeek, 次回:${java.util.Date(triggerTime)} (RC:$requestCode)"
            )
        }
    }
    /**
     * すでに登録されているアラームをすべてキャンセルする。
     * → UI でスイッチ OFF とか変更があったときに対応。
     */
    private fun cancelAllExistingAlarms(settings: List<AlarmSetting>) {
        settings.forEach { setting ->
            // 曜日リストを取得
            DayOfWeekUtils.parseDays(setting.days).forEach { dayOfWeek ->

                // 各曜日の requestCode を決定
                val requestCode = DayOfWeekUtils.generateRequestCode(setting.id, dayOfWeek)

                // 対応する PendingIntent を作成
                val pendingIntent = createPendingIntent(setting.id, requestCode)

                // AlarmManager に cancel 依頼
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    /**
     * AlarmReceiver に届けるための PendingIntent を生成する関数
     * - requestCode が違えば別のアラームとして扱われる
     */
    private fun createPendingIntent(settingId: Int, requestCode: Int): PendingIntent {

        // AlarmReceiver に渡す Intent を作成（どのアラームか ID を付与）
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", settingId)
        }

        // PendingIntent を生成して返す
        return PendingIntent.getBroadcast(
            context,
            requestCode, // 一意識のため必須
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 次に鳴るべきアラームの日時を計算する。
     * - 今日の設定時刻が過ぎていれば翌日以降に
     * - 指定の曜日になるまで日付を進める
     */
    private fun calculateNextTriggerTime(dayOfWeek: Int, hour: Int, minute: Int): Calendar {

        val calendar = Calendar.getInstance().apply {
            // 現在時刻で初期化
            timeInMillis = System.currentTimeMillis()

            // 指定された時刻に合わせる
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // 今すでに設定時刻を過ぎていたら翌日に変更
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 指定した曜日になるまで1日ずつ進める
        while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar
    }
}
