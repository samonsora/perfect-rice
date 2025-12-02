package com.example.team1application

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

/**
 * リアルタイムのアナログ時計とデジタル表示を組み合わせた画面。
 * ClockTimeとClockAngleクラスから時間データと角度計算データを取得します。
 *
 * @param modifier 親のレイアウト設定を受け取るModifier
 */
@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    // ClockTimeとClockAngleのインスタンスを作成
    val myClockTime = remember { ClockTime1() }
    // 角度計算用のインスタンスを新しく作成
    val myClockAngle = remember { ClockAngle() }

    // 時刻と角度を状態として保持し、LaunchedEffectで更新
    var currentTime by remember { mutableStateOf(myClockTime.getCurrentTime()) }
    var currentDate by remember { mutableStateOf(myClockTime.getCurrentDate()) }
    var handAngles by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

    // 毎フレーム時刻を更新するためのエフェクト
    LaunchedEffect(Unit) {
        while (isActive) {
            // 秒針を滑らかに動かすため、毎フレーム更新
            withFrameMillis {
                currentTime = myClockTime.getCurrentTime()
                currentDate = myClockTime.getCurrentDate()

                // 時分秒を取得
                val (h, m, s) = myClockTime.getHoursMinutesSeconds()

                // 角度計算（ClockAngleインスタンスを使用）
                handAngles = myClockAngle.calculateHandAngles(h, m, s)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "リアルタイムアナログ時計",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // --- アナログ時計の描画 ---
        ClockDisplay(
            modifier = Modifier.size(300.dp),
            handAngles = handAngles
        )
        // --- アナログ時計の描画 終了 ---

        Spacer(modifier = Modifier.height(32.dp))

        // --- デジタル表示 ---
        Text(
            text = "現在の日付: $currentDate",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "現在の時刻: $currentTime",
            style = MaterialTheme.typography.displaySmall
        )
    }
}

/**
 * 時計の文字盤と針を描画するCanvasコンポーネント。
 */
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

        // 2. 目盛りを描画
        for (i in 0 until 12) {
            val angle = i * 30f
            rotate(angle) {
                val isMajorTick = i % 3 == 0
                val tickLength = if (isMajorTick) 20.dp.toPx() else 10.dp.toPx()
                val tickWidth = if (isMajorTick) 4.dp.toPx() else 2.dp.toPx()

                drawLine(
                    color = Color.Black,
                    start = center + Offset(0f, -radius),
                    end = center + Offset(0f, -radius + tickLength),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. 針を描画 (時、分、秒の順)

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
            length = radius * 0.95f,
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

/**
 * 個別の時計の針を描画するためのヘルパー関数。
 * 角度は12時の位置から時計回りに回転します。
 */
private fun DrawScope.drawHand(angle: Float, length: Float, width: Float, color: Color, center: Offset) {
    // 角度を時計回りに回転
    rotate(angle) {
        drawLine(
            color = color,
            start = center + Offset(0f, width),
            end = center + Offset(0f, -length),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}

// --------------------------------------------------
// 📺 プレビュー
// --------------------------------------------------
@Preview(showBackground = true)
@Composable
fun ClockScreenPreview() {
    // Team1ApplicationTheme が定義されていないため、単純なプレビューで代替します。
    // 実際のプロジェクトではTeam1ApplicationThemeを使用してください。
    ClockScreen()
}