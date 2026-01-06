package com.example.team1application
//AlarmActivity.kt
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val currentSnoozeCount = intent.getIntExtra("CURRENT_SNOOZE_COUNT", 0)

        val allAlarms = AlarmDataStore.loadAlarms(this)
        val currentSetting = allAlarms.find { it.id == alarmId }

        // 💡 修正ポイント：拡張プロパティを使用して正しく取得
        val maxSnoozeCount = currentSetting?.snoozeCountInt ?: 0
        val isUnlimited = currentSetting?.isSnoozeUnlimited ?: false
        val snoozeIntervalStr = currentSetting?.snoozeInterval?.replace("分", "") ?: "5"
        val snoozeInterval = snoozeIntervalStr.toIntOrNull() ?: 5

        setContent {
            AlarmScreen(
                currentCount = currentSnoozeCount,
                maxCount = maxSnoozeCount,
                isUnlimited = isUnlimited,
                onStopClick = { stopAlarmAndFinish() },
                onSnoozeClick = {
                    // 無制限、または現在の回数が上限未満なら実行
                    if (isUnlimited || currentSnoozeCount < maxSnoozeCount) {
                        snoozeAndFinish(alarmId, currentSnoozeCount + 1, snoozeInterval)
                    }
                }
            )
        }
    }

    private fun stopAlarmAndFinish() {
        stopService(Intent(this, AlarmService::class.java))
        finish()
    }

    private fun snoozeAndFinish(alarmId: Int, nextCount: Int, interval: Int) {
        val snoozeScheduler = SnoozeScheduler(this)

        // 次回のインテントに「次の回数」を渡す必要があるため、
        // SnoozeSchedulerの引数にnextCountを渡せるよう修正が必要です
        snoozeScheduler.scheduleSnooze(alarmId, interval, nextCount)

        stopService(Intent(this, AlarmService::class.java))
        finish()
    }
}
@Composable
fun AlarmScreen(
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
        Text(text = "はよ起きろ", style = androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified))

        // 💡 表示の切り替え
        val countText = if (isUnlimited) {
            "スヌーズ回数: $currentCount / 無制限"
        } else {
            "スヌーズ回数: $currentCount / $maxCount 回"
        }
        Text(text = countText)

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStopClick) { Text("止める") }

        Spacer(modifier = Modifier.height(16.dp))

        // 💡 無制限なら常に有効、上限ありなら比較
        val canSnooze = isUnlimited || currentCount < maxCount
        Button(
            onClick = onSnoozeClick,
            enabled = canSnooze
        ) {
            Text(if (canSnooze) "スヌーズ" else "スヌーズ上限です")
        }
    }
}
