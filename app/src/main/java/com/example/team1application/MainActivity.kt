package com.example.team1application


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.team1application.ui.theme.Team1ApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CurrentTimeDisplay(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * 現在時刻を表示するコンポーザブル関数
 */
@Composable
fun CurrentTimeDisplay(modifier: Modifier = Modifier) {
    // 別のファイルで定義した関数を呼び出し、現在時刻を取得
    val currentTimeString = getCurrentTime()

    // Boxコンポーザブルを使用して、その中の要素（Text）を中央に配置
    Box(
        // BoxにModifier.fillMaxSize()を適用し、親（Surface）の領域全体を使う
        modifier = Modifier.fillMaxSize(),
        // Box内のコンテンツ（Text）を中央に揃える
        contentAlignment = Alignment.Center
    ) {
        // 現在時刻を表示するTextコンポーザブル
        Text(
            text = "現在の時刻\n",
            // デフォルトのModifierを適用。ここではTextの装飾は最低限。
            modifier = Modifier,
            fontSize = 32.sp,
            // テキストを中央揃えにするためにTextAlign.Centerを使用するのが一般的ですが、
            // Boxの中央配置だけでも画面中央には表示されます。
        )
        Text(
            text = "\n\n\n$currentTimeString",
            // デフォルトのModifierを適用。ここではTextの装飾は最低限。
            modifier = Modifier,
            fontSize = 60.sp,
            // テキストを中央揃えにするためにTextAlign.Centerを使用するのが一般的ですが、
            // Boxの中央配置だけでも画面中央には表示されます。
        )
    }
}