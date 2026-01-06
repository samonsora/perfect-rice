package com.example.team1application
//AlarmActivity.kt
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AlarmActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI
        setContent {
            AlarmScreen(
                onStopClick = { stopAlarmAndFinish() },
                onSnoozeClick = { snoozeAndFinish() }
            )
        }

    }


    /** アラームを止めて画面を閉じる */
    private fun stopAlarmAndFinish() {
        stopService(Intent(this, AlarmService::class.java))

        finish()
    }

    /** スヌーズ処理（5分後に再アラーム） */
    private fun snoozeAndFinish() {

        // 現在のアラームIDを取得（なければ -1）
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        // スヌーズ用スケジューラを生成
        val snoozeScheduler = SnoozeScheduler(this)

        // 5分後に再アラームを設定
        snoozeScheduler.scheduleSnooze(alarmId, 5)

        // 現在鳴っている音を停止
        stopService(Intent(this, AlarmService::class.java))

        // 画面を閉じる
        finish()
    }



}

@Composable
fun AlarmScreen(
    onStopClick: () -> Unit,
    onSnoozeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("おはようございます")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStopClick) {
            Text("止める")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onSnoozeClick) {
            Text("スヌーズ")
        }
    }
}