package com.example.team1application

// AlarmInitializer.kt
import android.content.Context
import android.content.Intent
import android.util.Log

// Alarm.kt から getDummyAlarmSettings をインポート

// AlarmScheduler.kt をインポート
// (↑ 実際のパッケージ名に合わせて変更してください)

/**
 * アラーム機能の初期化と操作ロジックを担うクラス。
 * Activityから本機能の具体的な実装を分離する。
 */
class AlarmInitializer(private val context: Context) {

    // 内部で AlarmScheduler と AlarmSetting データを保持
    private val alarmSettings = getDummyAlarmSettings()
    private val scheduler = AlarmScheduler(context)

    /**
     * 💡 [MainActivity から呼び出す関数]
     * アラーム機能の初期設定を完了させます。
     * 有効な設定に基づいて全てのアラームを設定します。
     */
    fun initializeAlarms() {
        Log.i("AlarmInitializer", "アラーム初期化処理を開始しました。")
        scheduler.updateAllAlarms(alarmSettings)
        Log.i("AlarmInitializer", "初期アラーム設定が完了しました。")
    }

    /**
     * [設定変更の例]
     * アラームID 3 の isActive 状態を切り替え、アラームを更新します。
     * このメソッドをUI（ボタンなど）に紐づけることで、設定変更とスケジュールの更新を一元管理できます。
     */
    fun toggleAlarm3State() {
        val index = alarmSettings.indexOfFirst { it.id == 3 }
        if (index != -1) {
            val currentSetting = alarmSettings[index]

            // 状態リストの内容を更新
            alarmSettings[index] = currentSetting.copy(isActive = !currentSetting.isActive)

            // 状態が変わったため、アラームを再スケジュール (isActive=false のアラームはキャンセルされる)
            scheduler.updateAllAlarms(alarmSettings)

            Log.d("AlarmInitializer", "アラームID 3 の状態が ${alarmSettings[index].isActive} に切り替わり、スケジュールを更新しました。")
        }
    }

    // 必要に応じて、他のアラーム操作メソッド（新規追加、削除など）もここに追加できます。
}