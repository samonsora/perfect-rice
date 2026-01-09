package com.example.team1application

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


var SnoozeIntervalData = 0
var SnoozeCountData = 0
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        val alarmType = intent.getStringExtra("ALARM_TYPE") ?: ""

        // 1. 設定ファイルをロード
        val allAlarms = AlarmDataStore.loadAlarms(this)
        val currentSetting = allAlarms.find { it.id == alarmId }

        // 2. 最大回数や無制限フラグを取得
        val maxSnoozeCount = currentSetting?.snoozeCountInt ?: 0
        val isUnlimited = currentSetting?.isSnoozeUnlimited ?: false

        // 3. スヌーズの間隔を取得 (例: "5分" -> 5)
        // 文字列から"分"を除去して数値に変換する処理をここで行います
        val snoozeIntervalStr = currentSetting?.snoozeInterval?.replace("分", "") ?: "5"
        val snoozeInterval = snoozeIntervalStr.toIntOrNull() ?: 5
        //履歴に受け渡すようにglobalに
        SnoozeIntervalData = snoozeInterval
        Log.d("intarval", "スヌーズ間隔: ${SnoozeIntervalData}分")
        // 4. 現在の回数を Intent から取得
        val currentSnoozeCount = intent.getIntExtra("CURRENT_SNOOZE_COUNT", 0)

        setContent {
            AlarmScreen(
                alarmType = alarmType,
                currentCount = currentSnoozeCount,
                maxCount = maxSnoozeCount,
                isUnlimited = isUnlimited,
                onStopClick = { stopAlarmAndFinish() },
                onSnoozeClick = {
                    // 無制限、または現在の回数が上限未満なら実行
                    if (isUnlimited || currentSnoozeCount < maxSnoozeCount) {
                        snoozeAndFinish(alarmId, currentSnoozeCount + 1, snoozeInterval)
                    }
                    Log.d("snoozecount", "スヌーズ回数: ${SnoozeCountData}回")

                }
            )
        }
    }

    private fun stopAlarmAndFinish() {
        // 1. 音を止める命令を出す
        stopService(Intent(this, AlarmService::class.java))

        // 2. Serviceの終了を待たず、ここで即座にメモ（フラグ）を書き換える
        // commit() を使うことで、次の行に行く前に書き込みを完了させます
        getSharedPreferences("alarm_prefs", MODE_PRIVATE).edit()
            .putBoolean("is_ringing", false)
            .putInt("ringing_alarm_id", -1)
            .commit() // apply() ではなく commit() にするとより確実です

        // 3. 履歴保存
        saveSleepRecord()

        // 4. 最後に画面を閉じる
        finish()
    }
    private fun saveSleepRecord() {
        val dataManager = SleepDataManager(this)
        val records = dataManager.loadRecords().toMutableList()

        // 現在の時刻を取得 (起床時刻)
        val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        val now = Date()

        val wakeUpTimeStr = sdfTime.format(now)
        val dateStr = sdfDate.format(now)

        // 設定から就寝時刻（あるいは設定時刻）を取得
        // ※本来はAlarmSettingから取得すべきですが、簡易的に現在の時刻から計算、
        // もしくはIntentで渡された設定時刻を使用します。
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val allAlarms = AlarmDataStore.loadAlarms(this)
        val currentSetting = allAlarms.find { it.id == alarmId }
        val scheduledTime = currentSetting?.time ?: "00:00"

        // scheduledTime（HH:mm）をDateに変換
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val baseTime = timeFormat.parse(scheduledTime)

        // スヌーズでずらす分数（回数 × 間隔）
        val snoozeOffsetMinutes = SnoozeCountData * SnoozeIntervalData

        // Calendarで分を加算
        val calendar = java.util.Calendar.getInstance()
        calendar.time = baseTime!!
        calendar.add(java.util.Calendar.MINUTE, snoozeOffsetMinutes)

        // 実際の起床時間（スヌーズ反映後）
        val actualWakeUpTimeStr = timeFormat.format(calendar.time)

        // 睡眠時間の計算 (calculateDuration関数を利用)
        // 就寝用(BEDTIME)アラームの場合は「就寝時刻」として記録を分ける工夫が必要ですが、
        // ここでは「起床(WAKE_UP)」時に1つのレコードとして完結させる例です。
        val duration = calculateDuration(scheduledTime, actualWakeUpTimeStr, SnoozeCountData)

        val newRecord = SleepRecord(
            date = dateStr,
            sleepTime = duration,           // 計算された睡眠時間
            wakeUpTime = actualWakeUpTimeStr ,     // 実際に止めた時刻
            bedtime = scheduledTime,        // 本来鳴るはずだった時刻（または設定された就寝時刻）
            snoozeCount = SnoozeCountData,  // グローバル変数から取得
            snoozeDuration = "${SnoozeIntervalData}分" // グローバル変数から取得
        )

        // 同じ日付のデータがあれば上書き、なければ追加（ロジックは用途に合わせて調整）
        records.add(newRecord)
        dataManager.saveRecords(records)

        Log.d("AlarmActivity", "履歴を保存しました: $newRecord")
    }

    private fun snoozeAndFinish(alarmId: Int, nextCount: Int, interval: Int) {
        val snoozeScheduler = SnoozeScheduler(this)
        SnoozeCountData = nextCount
        // 次回のインテントに「次の回数」を渡す必要があるため、
        // SnoozeSchedulerの引数にnextCountを渡せるよう修正が必要です
        snoozeScheduler.scheduleSnooze(alarmId, interval, nextCount)

        stopService(Intent(this, AlarmService::class.java))
        finish()
    }

}

@Composable
fun AlarmScreen(
    alarmType: String,
    currentCount: Int,
    maxCount: Int,
    isUnlimited: Boolean,
    onStopClick: () -> Unit,
    onSnoozeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. メインメッセージの表示
        val mainMessage = when (alarmType) {
            "WAKE_UP" -> "おはようございます"
            "BEDTIME" -> "おやすみなさい"
            else -> ""
        }

        if (mainMessage.isNotEmpty()) {
            Text(
                text = mainMessage,
                // fontSizeを大きく設定（例: 32sp）
                fontSize = 32.sp,
                // 太字にするとより見やすくなります
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        //  2. 起床用（WAKE_UP）の場合のみスヌーズ回数を表示
        if (alarmType == "WAKE_UP") {
            val countText = if (isUnlimited) {
                "スヌーズ回数: $currentCount / 無制限"
            } else {
                "スヌーズ回数: $currentCount / $maxCount 回"
            }
            Text(text = countText,
                fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. 停止ボタン（これは共通）
        Button(onClick = onStopClick,
            modifier = Modifier.size(width = 200.dp, height = 60.dp)) {
            Text(if (alarmType == "BEDTIME") "了解" else "止める")
        }

        // 4. 起床用（WAKE_UP）の場合のみスヌーズボタンを表示
        if (alarmType == "WAKE_UP") {
            Spacer(modifier = Modifier.height(16.dp))
            val canSnooze = isUnlimited || currentCount < maxCount
            Button(
                onClick = onSnoozeClick,
                modifier = Modifier.size(width = 200.dp, height = 60.dp),
                enabled = canSnooze
            ) {
                Text(if (canSnooze) "スヌーズ" else "スヌーズ上限です")
            }
        }
    }


}
