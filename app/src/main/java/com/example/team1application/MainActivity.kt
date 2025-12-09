package com.example.team1application

import android.provider.Settings
import android.app.AlarmManager
import android.content.Intent
import android.media.audiofx.EnvironmentalReverb
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import java.time.Clock



class MainActivity : ComponentActivity() {
    private lateinit var alarmInitializer: AlarmInitializer

    //  今後このoncleate多分消えてなくなるからどっかに避難
    @RequiresApi(Build.VERSION_CODES.S)
    private var currentTextSize: Float = 16f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // コンフリクト激戦区
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 🪄 魔法のスイッチ
                    var isTitle by remember { mutableStateOf(true) }

                    val alarmManager = getSystemService(AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }




                    // 1. AlarmInitializerをインスタンス化（ロジックの引き出し）
                    // applicationContext を渡し、Activityの寿命に依存しないようにする
                    alarmInitializer = AlarmInitializer(applicationContext)

                    // 2. 本機能の初期設定を実行
                    alarmInitializer.initializeAlarms()

                    // ✨✨ ここが「フワッ」とする魔法陣！ ✨✨
                    Crossfade(
                        targetState = isTitle, // このスイッチを見張るよ！
                        label = "画面切り替え",
                        // 👇 魔法にかける時間（ミリ秒）。1000 = 1秒。
                        animationSpec = tween(durationMillis = 700)
                    ) { isShowingTitle ->

                        // ここで中身を出し分けるの！
                        if (isShowingTitle) {
                            TitleScreen(
                                onTap = { isTitle = false },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            HomeScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

