package com.example.team1application

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

    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // UI
        setContent {
            AlarmScreen(
                onStopClick = { stopAlarmAndFinish() }
            )
        }

        // アラーム音を鳴らす
        startAlarm()
    }

    /** アラーム音を鳴らす */
    private fun startAlarm() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        player = MediaPlayer().apply {
            setDataSource(this@AlarmActivity, uri)
            setAudioStreamType(AudioManager.STREAM_ALARM)
            isLooping = true
            prepare()
            start()
        }
    }

    /** アラームを止めて画面を閉じる */
    private fun stopAlarmAndFinish() {
        player?.stop()
        player?.release()
        player = null
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.stop()
        player?.release()
    }
}

@Composable
fun AlarmScreen(
    onStopClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("はよ起きろカス")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStopClick) {
            Text("止める")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* スヌーズは後で実装 */ }) {
            Text("スヌーズ（未実装）")
        }
    }
}