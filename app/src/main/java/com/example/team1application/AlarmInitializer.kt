package com.example.team1application

import android.content.Context
import android.util.Log

/**
 * アラーム機能の初期化・状態変更・更新などの「アプリ側のロジック」を担当するクラス。
 * AlarmScheduler は「OS にアラーム登録する専門家」なので、
 * このクラスは「アプリの状態管理」や「設定変更の調整」を担当する。
 */
class AlarmInitializer(private val context: Context) {

    /**
     * アラーム設定のリストを保持する変数。
     *
     * getDummyAlarmSettings() で取得した SnapshotStateList を
     * → toMutableList() で **MutableList** に変換して代入している。
     *
     * なぜ必要？
     * - SnapshotStateList のままだと「UIは再描画できるけど、状態が持ちづらい」
     * - MutableList にすることで「要素の置き換え（id=3 の isActive変更など）」が可能になる
     */
    private val alarmSettings: MutableList<AlarmSetting> = getDummyAlarmSettings().toMutableList()

    /**
     * AlarmScheduler のインスタンス。
     * → 実際に OS の AlarmManager にアラームを依頼する担当者。
     */
    private val scheduler = AlarmScheduler(context)

    /**
     * アプリ起動時に MainActivity から呼ばれるメソッド。
     * - ログ出力
     * - AlarmScheduler に全アラーム更新を依頼
     * という初期処理を行う。
     */
    fun initializeAlarms() {
        Log.i("AlarmInitializer", "アラーム初期化処理を開始しました。")

        // AlarmScheduler に「今持っている設定を全部 OS に反映して！」と依頼
        scheduler.updateAllAlarms(alarmSettings)

        Log.i("AlarmInitializer", "初期アラーム設定が完了しました。")
    }

    /**
     * ID=3 のアラームの isActive を ON/OFF 切り替える処理。
     * UI のスイッチ ON/OFF ボタンのような存在。
     *
     * 現状は「ID=3 専用の機能」だが、
     * あとで汎用化しやすいような実装にしてある。
     */
    fun toggleAlarm3State() {

        // id=3 の設定が alarmSettings のどの index にあるかを検索
        val index = alarmSettings.indexOfFirst { it.id == 3 }

        // 見つからなければ何もしない
        if (index != -1) {
            val currentSetting = alarmSettings[index]

            /**
             * AlarmSetting は data class なので
             * - isActive だけを変更したコピー
             * を簡単に作れる（copy() メソッド）
             *
             * alarmSettings は MutableList のため
             * → alarmSettings[index] = 新しい値
             * という更新が可能
             */
            alarmSettings[index] = currentSetting.copy(
                isActive = !currentSetting.isActive
            )

            /**
             * 設定変更後は、OS のアラームを再生成しないといけない。
             * → updateAllAlarms で
             *   - OFF になったアラームはキャンセル
             *   - ON のものだけ再スケジュール
             */
            scheduler.updateAllAlarms(alarmSettings)

            Log.d(
                "AlarmInitializer",
                "アラームID 3 の状態が ${alarmSettings[index].isActive} に切り替わり、スケジュールを更新しました。"
            )
        }
    }

    // 今後追加したい機能の例：
    // - addAlarm(setting: AlarmSetting)
    // - removeAlarm(settingId: Int)
    // - updateAlarm(settingId: Int, newSetting: AlarmSetting)
    // などの「アプリ内のロジック」はこのクラスに追加していくのが適切。
}
