package com.example.team1application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas // Canvasを追加
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // sizeを追加
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // LaunchedEffectを追加
import androidx.compose.runtime.getValue // byを使用するために必要
import androidx.compose.runtime.mutableStateOf // mutableStateOfを追加
import androidx.compose.runtime.remember // rememberを追加
import androidx.compose.runtime.setValue // byを使用するために必要
import androidx.compose.runtime.withFrameMillis // withFrameMillisを追加
import androidx.compose.ui.Alignment // Alignmentを追加
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // Offsetを追加
import androidx.compose.ui.graphics.Color // Colorを追加
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate // rotateを追加
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme
import kotlinx.coroutines.isActive // LaunchedEffectのループを安全に保つために必要

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GreetingContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ユーザーが提供した `Greeting` を `GreetingContent` に変更し、時刻表示と時計描画ロジックを追加
@Composable
fun GreetingContent(modifier: Modifier = Modifier) {
    // Alarmクラスのインスタンスを作成 (再コンポジションで再作成されないように remember を使用しても良いが、ここではシンプルに)
    val myAlarm = Alarm()

    // 現在の時刻と日付、針の角度を状態として保持
    var currentTime by remember { mutableStateOf(myAlarm.getCurrentTime()) }
    var currentDate by remember { mutableStateOf(myAlarm.getCurrentDate()) }
    // 角度は Triple<時, 分, 秒>
    var handAngles by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

    // 毎秒（または毎フレーム）時刻を更新する処理
    LaunchedEffect(Unit) {
        while (isActive) {
            // withFrameMillis を使用すると、より滑らかなアニメーションが可能
            // ここでは秒針を滑らかにするため、毎フレーム更新
            withFrameMillis {
                // 時刻を更新
                currentTime = myAlarm.getCurrentTime()
                currentDate = myAlarm.getCurrentDate()

                // 角度を計算
                val (h, m, s) = myAlarm.getHoursMinutesSeconds()
                handAngles = myAlarm.calculateHandAngles(h, m, s)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally // 中央揃え
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // --- アナログ時計の描画 ---
        ClockDisplay(
            modifier = Modifier.size(300.dp), // 時計のサイズを指定
            handAngles = handAngles
        )
        // --- アナログ時計の描画 終了 ---

        Spacer(modifier = Modifier.height(32.dp))

        // --- デジタル表示 ---
        Text(
            text = "現在の日付: $currentDate",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "現在の時刻: $currentTime",
            style = MaterialTheme.typography.headlineLarge // デジタル時計を目立たせる
        )
    }
}

@Composable
fun ClockDisplay(modifier: Modifier = Modifier, handAngles: Triple<Float, Float, Float>) {
    val (hourAngle, minuteAngle, secondAngle) = handAngles

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // 1. 文字盤の円を描画
        drawCircle(
            color = Color.Black,
            center = center,
            radius = radius,
            style = Stroke(width = 4.dp.toPx())
        )

        // 2. 目盛りを描画 (12, 3, 6, 9時)
        for (i in 0 until 12) {
            val angle = i * 30f // 30度間隔
            rotate(angle) {
                drawLine(
                    color = Color.Black,
                    start = center + Offset(0f, -radius),
                    end = center + Offset(0f, -radius + if (i % 3 == 0) 20.dp.toPx() else 10.dp.toPx()),
                    strokeWidth = if (i % 3 == 0) 4.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. 針を描画 (秒、分、時 の順で、より目立つように描画)

        // 時針 (短く太い)
        drawHand(
            angle = hourAngle,
            length = radius * 0.6f,
            width = 8.dp.toPx(),
            color = Color.DarkGray,
            center = center
        )

        // 分針 (長く細い)
        drawHand(
            angle = minuteAngle,
            length = radius * 0.9f,
            width = 5.dp.toPx(),
            color = Color.DarkGray,
            center = center
        )

        // 秒針 (最も長く細く、色を赤に)
        drawHand(
            angle = secondAngle,
            length = radius * 0.8f,
            width = 2.dp.toPx(),
            color = Color.Red,
            center = center
        )

        // 4. 中心点を描画
        drawCircle(
            color = Color.Black,
            center = center,
            radius = 6.dp.toPx()
        )
    }
}

// 針を描画するためのヘルパー関数
private fun DrawScope.drawHand(angle: Float, length: Float, width: Float, color: Color, center: Offset) {
    // 針の回転。時計の12時(Y軸正方向)を0度とし、時計回りを正とする
    rotate(angle) {
        drawLine(
            color = color,
            start = center + Offset(0f, width / 2), // 中心から少しオフセットして描画開始 (好みによる)
            end = center + Offset(0f, -length), // Y軸上向きに針を描画
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Team1ApplicationTheme {
        // プレビューでは固定の時刻（例: 10時10分30秒）で描画
        val myAlarm = Alarm()
        val fixedAngles = myAlarm.calculateHandAngles(10, 10, 30)

        Column {
            GreetingContent(Modifier.padding(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "プレビュー用固定表示:", modifier = Modifier.padding(horizontal = 16.dp))
            ClockDisplay(modifier = Modifier.size(200.dp), handAngles = fixedAngles)
        }
    }
}