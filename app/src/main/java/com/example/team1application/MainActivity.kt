package com.example.team1application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // コンフリクト激戦区
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var isTitle by remember { mutableStateOf(true) }

                    if (isTitle) {

                      TitleScreen(onTap = { isTitle = false },
                        modifier = Modifier.padding(innerPadding)
                    )
                    } else {
                        // ■ スイッチがOFFになったら、こっち（ホーム）を表示！
                        HomeScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }


                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Team1ApplicationTheme {

        Greeting("Android")

        // プレビューでは固定の時刻（例: 10時10分30秒）で描画
        val myClock = Clock()
        val fixedAngles = myClock.calculateHandAngles(10, 10, 30)

        Column {
            GreetingContent(Modifier.padding(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "プレビュー用固定表示:", modifier = Modifier.padding(horizontal = 16.dp))
            ClockDisplay(modifier = Modifier.size(200.dp), handAngles = fixedAngles)
        }
    }
}
