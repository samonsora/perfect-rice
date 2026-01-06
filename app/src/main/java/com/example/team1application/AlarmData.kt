package com.example.team1application

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * 1回分のアラーム設定を保持するデータクラス
 */
@Serializable
data class AlarmSetting(
    val id: Int,
    val time: String,
    val days: String,
    val isActive: Boolean,
    val name: String,
    val snoozeInterval: String,
    val snoozeCount: String,
    val volume: Float,
    val fadeIn: Boolean
){
    // 💡 これを追加： "無制限" かどうかを判定する
    val isSnoozeUnlimited: Boolean
        get() = snoozeCount == "無制限"

    // "3回" という文字列から数値の 3 を取り出す
    val snoozeCountInt: Int
        get() = if (isSnoozeUnlimited) {
            0 // 無制限の場合、ロジックで判定
        } else {
            // "1回" -> "1" に置換してから数値変換
            snoozeCount.replace("回", "").toIntOrNull() ?: 0
        }
}

/**
 * アラーム設定の永続化を担うクラス
 */
object AlarmDataStore {
    private const val FILE_NAME = "alarm_settings.json"
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun saveAlarms(context: Context, alarms: List<AlarmSetting>) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonString = json.encodeToString(alarms)
            file.writeText(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun loadAlarms(context: Context): List<AlarmSetting> {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return emptyList()

            val jsonString = file.readText().trim()
            if (jsonString.isEmpty()) return emptyList()

            json.decodeFromString<List<AlarmSetting>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

fun generateNewAlarmId(currentAlarms: List<AlarmSetting>): Int {
    return (currentAlarms.maxOfOrNull { it.id } ?: 0) + 1
}

/**
 * 💡 保存ロジック：ファイル保存の後に OS のスケジュールを同期します
 */
fun alarmSave(context: Context, allAlarms: SnapshotStateList<AlarmSetting>, newAlarmSetting: AlarmSetting) {
    if (newAlarmSetting.id == -1) {
        // 新規追加
        val newId = generateNewAlarmId(allAlarms)
        allAlarms.add(newAlarmSetting.copy(id = newId))
    } else {
        // 既存更新
        val index = allAlarms.indexOfFirst { it.id == newAlarmSetting.id }
        if (index != -1) {
            allAlarms[index] = newAlarmSetting
        } else {
            allAlarms.add(newAlarmSetting)
        }
    }

    // 1. ファイルに保存
    AlarmDataStore.saveAlarms(context, allAlarms)

    // 2. 🔥 OS（AlarmManager）と同期
    // これにより、追加・更新されたアラームが即座に OS に登録されます
    AlarmScheduler(context).syncWithStorage()
}

/**
 * 💡 削除ロジック：削除保存の後に OS のスケジュールを同期します
 */
fun deleteAlarmAndSave(context: Context, allAlarms: SnapshotStateList<AlarmSetting>, alarmId: Int) {
    val initialSize = allAlarms.size
    allAlarms.removeAll { it.id == alarmId }

    if (allAlarms.size < initialSize) {
        // 1. ファイルに保存
        AlarmDataStore.saveAlarms(context, allAlarms)

        // 2. 🔥 OS（AlarmManager）と同期
        // これにより、削除されたアラームのスケジュールが OS から解除されます
        AlarmScheduler(context).syncWithStorage()
    }
}