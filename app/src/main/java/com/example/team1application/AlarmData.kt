package com.example.team1application

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * 1回分のアラーム設定を保持するデータクラス
 * 外部ファイルに保存するため、@Serializableアノテーションを付ける
 */
@Serializable
data class AlarmSetting(
    val id: Int,
    val time: String, // 例: "07:00"
    val days: String, // 例: "月, 火, 水, 木, 金" または "毎日"
    val isActive: Boolean,
    val name: String, // アラーム名 (例: "指定なし")
    val snoozeInterval: String, // スヌーズ間隔 (例: "5分", "なし")
    val snoozeCount: String, // スヌーズ回数 (例: "3回", "無制限")
    val volume: Float, // 音量 (0.0f ~ 1.0f)
    val fadeIn: Boolean // フェードインの有無
)

/**
 * アラーム設定の永続化を担うクラス
 * ファイルへの読み書きロジックを集約する
 */
object AlarmDataStore { // objectでシングルトンとして定義
    private const val FILE_NAME = "alarm_settings.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true } // 💡 互換性のため ignoreUnknownKeys を追加

    // データの保存
    fun saveAlarms(context: Context, alarms: List<AlarmSetting>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonString = json.encodeToString(alarms)
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
                println("⚠️ ファイルが見つかりません。空のリストを返します。")
                return emptyList() // 💡 修正: ダミーデータではなく空のリストを返す
            }

            val jsonString = file.readText().trim()
            if (jsonString.isEmpty()) {
                println("⚠️ ファイルは存在しますが内容が空です。空のリストを返します。")
                return emptyList()
            }

            val alarms = json.decodeFromString<List<AlarmSetting>>(jsonString)
            println("✅ アラーム設定をファイルから読み込みました (${alarms.size}件)")
            alarms
        } catch (e: Exception) {
            println("❌ ファイル/JSONデコードエラー: ${e.message}")
            e.printStackTrace()
            return emptyList()
        }
    }
}

// 💡 注意: 新しいアラームを追加する際に使うため、最大のIDを追跡するヘルパー関数
fun generateNewAlarmId(currentAlarms: List<AlarmSetting>): Int {
    return (currentAlarms.maxOfOrNull { it.id } ?: 0) + 1
}

/**
 * 💡 新規アラーム追加/既存アラーム更新のロジック。
 * AlarmSetUIで設定された、全ての情報を含む新しいAlarmSettingを受け取る。
 * * @param newAlarmSetting ID=-1の場合は新規、IDが既存の場合は更新と判断する。
 */
fun alarmSave(context: Context, allAlarms: SnapshotStateList<AlarmSetting>, newAlarmSetting: AlarmSetting) {
    if (newAlarmSetting.id == -1) {
        // --- 新規追加 ---
        val newId = generateNewAlarmId(allAlarms)
        val finalAlarm = newAlarmSetting.copy(id = newId)

        allAlarms.add(finalAlarm)
        println("✅ アラームID $newId を新規として追加しました。")

    } else {
        // --- 既存更新 ---
        val index = allAlarms.indexOfFirst { it.id == newAlarmSetting.id }
        if (index != -1) {
            allAlarms[index] = newAlarmSetting // ステートリストの要素を更新
            println("✅ アラームID ${newAlarmSetting.id} を新しい設定で更新しました。")
        } else {
            // 既存IDが見つからない場合は、新規として追加（本来は起こらないはず）
            allAlarms.add(newAlarmSetting)
            println("⚠️ アラームID ${newAlarmSetting.id} が見つからなかったため、新規として追加しました。")
        }
    }

    // ファイルに保存
    AlarmDataStore.saveAlarms(context, allAlarms)
}


/**
 * 指定されたIDのアラームをステートリストから削除し、その結果をファイルに保存する関数
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