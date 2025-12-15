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
                onStopClick = { stopAlarmAndFinish() }
            )
        }

    }


    /** アラームを止めて画面を閉じる */
    private fun stopAlarmAndFinish() {
        stopService(Intent(this, AlarmService::class.java))

        finish()
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