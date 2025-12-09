package com.example.team1application

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * 1回分のアラーム設定を保持するデータクラス
 * 外部ファイルに保存するため、@Serializableアノテーションを付ける
 */
@kotlinx.serialization.Serializable // 👈 データをシリアライズ可能にする
data class AlarmSetting(
    val id: Int,
    val time: String, // 例: "07:00"
    val days: String, // 例: "月, 火, 水, 木, 金" または "毎日"
    val isActive: Boolean
)

/**
 * アラーム設定の永続化を担うクラス
 * ファイルへの読み書きロジックを集約する
 */
object AlarmDataStore { // objectでシングルトンとして定義
    private const val FILE_NAME = "alarm_settings.json"
    private val json = Json { prettyPrint = true } // JSONパーサー

    // データの保存
    fun saveAlarms(context: Context, alarms: List<AlarmSetting>) {
        try {
            // アプリケーション固有のファイル保存ディレクトリを取得
            val file = File(context.filesDir, FILE_NAME)
            // リストをJSON文字列にエンコード
            val jsonString = json.encodeToString(alarms)

            // ファイルに書き込み
            file.writeText(jsonString)
            println("✅ アラーム設定をファイルに保存しました: ${file.absolutePath}")
        } catch (e: IOException) {
            println("❌ ファイル書き込みエラー: ${e.message}")
            e.printStackTrace()
        }
    }

    // データの読み込み
    fun loadAlarms(context: Context): List<AlarmSetting> {
        return try {
            val file = File(context.filesDir, FILE_NAME)

            if (!file.exists()) {
                println("⚠️ ファイルが見つかりません。ダミーデータを返します。")
                // 初回起動時など、ファイルがない場合はダミーデータを返す
                return createInitialDummyData()
            }

            // ファイルから全テキストを読み込み
            val jsonString = file.readText()

            // JSON文字列をList<AlarmSetting>にデコード
            val alarms = json.decodeFromString<List<AlarmSetting>>(jsonString)
            println("✅ アラーム設定をファイルから読み込みました (${alarms.size}件)")
            alarms
        } catch (e: IOException) {
            println("❌ ファイル読み込みエラー: ${e.message}")
            e.printStackTrace()
            createInitialDummyData()
        } catch (e: Exception) {
            println("❌ JSONデコードエラー: ${e.message}")
            e.printStackTrace()
            createInitialDummyData()
        }
    }

    // ファイルが存在しない場合に初期データを作成する関数
    private fun createInitialDummyData(): List<AlarmSetting> {
        return listOf(
        )
    }
}

// 💡 注意: 新しいアラームを追加する際に使うため、最大のIDを追跡するヘルパー関数
fun generateNewAlarmId(currentAlarms: List<AlarmSetting>): Int {
    return (currentAlarms.maxOfOrNull { it.id } ?: 0) + 1
}

/**
 * 新規アラーム追加ロジック。
 * 実際はアラーム設定画面での入力値(time)を受け取ります。
 * @param context Android Context
 * @param allAlarms アラームのステートリスト
 * @param time ユーザーが設定した時刻の文字列 (例: "14:30")
 */
fun addNewAlarmAndSave(context: Context, allAlarms: SnapshotStateList<AlarmSetting>, time: String) {
    val newId = generateNewAlarmId(allAlarms)
    // 💡 修正: time引数を使用
    val newAlarm = AlarmSetting(newId, time, "指定なし", true)

    // ステートリストに追加
    allAlarms.add(newAlarm)

    // ファイルに保存
    AlarmDataStore.saveAlarms(context, allAlarms)
}

/**
 * 💡 指定されたIDのアラームをステートリストから削除し、その結果をファイルに保存する関数
 */
fun deleteAlarmAndSave(context: Context, allAlarms: SnapshotStateList<AlarmSetting>, alarmId: Int) {
    // 1. ステートリストから対象のアラームを削除（UI更新をトリガー）
    val initialSize = allAlarms.size
    allAlarms.removeAll { it.id == alarmId }

    if (allAlarms.size < initialSize) {
        // 2. 変更があった場合のみ、ファイルに永続化 (保存) する
        AlarmDataStore.saveAlarms(context, allAlarms)
        println("✅ アラームID $alarmId を削除し、保存しました。")
    } else {
        println("⚠️ アラームID $alarmId は見つからず、削除されませんでした。")
    }
}