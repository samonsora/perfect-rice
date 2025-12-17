package com.example.team1application

import android.content.Context
import android.util.Log

/**
 * アラーム機能の初期化と操作ロジックを担うクラス。
 * Activityから本機能の具体的な実装を分離する。
 */
class AlarmInitializer(private val context: Context) {

    // 💡 修正: MutableList<AlarmSetting> として明示的に宣言し、toMutableList() で初期化する。
    //         これにより、リストの要素の代入（更新）が可能になる。
    private val alarmSettings: MutableList<AlarmSetting> = mutableListOf()
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

    fun toggleAlarm3State() {
        val index = alarmSettings.indexOfFirst { it.id == 3 }
        if (index != -1) {
            val currentSetting = alarmSettings[index]

            // 状態リストの内容を更新 (52行目付近)
            // 💡 修正後: alarmSettings が MutableList のため、この代入操作が可能になる。
            alarmSettings[index] = currentSetting.copy(isActive = !currentSetting.isActive)

            // 状態が変わったため、アラームを再スケジュール (isActive=false のアラームはキャンセルされる)
            scheduler.updateAllAlarms(alarmSettings)

            Log.d("AlarmInitializer", "アラームID 3 の状態が ${alarmSettings[index].isActive} に切り替わり、スケジュールを更新しました。")
        }
    }

    // 必要に応じて、他のアラーム操作メソッド（新規追加、削除など）もここに追加できます。
}