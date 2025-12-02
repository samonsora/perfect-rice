package com.example.team1application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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