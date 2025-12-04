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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * リアルタイムのアナログ時計とデジタル表示を組み合わせた画面。
 * ClockTimeとClockAngleクラスから時間データと角度計算データを取得します。
 *
 * @param modifier 親のレイアウト設定を受け取るModifier
 */
@Composable
fun ClockScreen(modifier: Modifier = Modifier) {
    // ClockTimeとClockAngleのインスタンスを作成
    val myClockTime = remember { ClockTime() }
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
 * 時計の文字盤、目盛り、数字、針を描画するCanvasコンポーネント。
 */
@Composable
fun ClockDisplay(modifier: Modifier = Modifier, handAngles: Triple<Float, Float, Float>) {
    val (hourAngle, minuteAngle, secondAngle) = handAngles
    // テキスト描画のためのTextMeasurerを準備
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // 数字を配置する円の半径 (外周から少し内側)
        val numberRadius = radius * 0.80f
        val majorTickLength = 20.dp.toPx()
        val minorTickLength = 10.dp.toPx()

        // 1. 文字盤の円を描画
        drawCircle(
            color = Color.Black,
            center = center,
            radius = radius,
            style = Stroke(width = 4.dp.toPx())
        )

        // 2. 目盛りを描画 (60個の目盛り: 12個の主要目盛りと48個の補助目盛り)
        for (i in 0 until 60) {
            val angle = i * 6f // 360度 / 60目盛り = 6度

            // 5の倍数（0, 5, 10, ...）が時針の主要目盛り
            val isMajorTick = i % 5 == 0
            val tickLength = if (isMajorTick) majorTickLength else minorTickLength
            val tickWidth = if (isMajorTick) 4.dp.toPx() else 2.dp.toPx()
            val color = if (isMajorTick) Color.Black else Color.Gray

            // 12時の位置から時計回りに回転して目盛りを描画
            rotate(angle) {
                drawLine(
                    color = color,
                    // スタート位置: 外周
                    start = center + Offset(0f, -radius),
                    // エンド位置: 外周から内側へtickLengthだけ
                    end = center + Offset(0f, -radius + tickLength),
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. 時刻の数字を描画 (1から12)
        for (h in 1..12) {
            // 時刻に対する角度 (12を0度として時計回り)
            // 標準の三角関数(cos, sin)は右側(3時)を0度とするため、90度(pi/2)をオフセットとして使用
            val hourAngleDegrees = (h * 30f) - 90f

            // 角度をラジアンに変換
            val angleRad = Math.toRadians(hourAngleDegrees.toDouble()).toFloat()

            // 数字の中心位置を計算 (X軸はCos、Y軸はSin)
            val x = center.x + numberRadius * cos(angleRad)
            val y = center.y + numberRadius * sin(angleRad)

            // テキストのスタイル
            val textLayoutResult = textMeasurer.measure(
                text = h.toString(),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // テキストを中央揃えにするためのオフセット
            val textX = x - textLayoutResult.size.width / 2
            val textY = y - textLayoutResult.size.height / 2

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY)
            )
        }

        // 4. 針を描画 (時、分、秒の順)

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

        // 5. 中心点を描画
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
            // 中心から針の幅分下にオフセットし、針の根元を太く見せる
            start = center + Offset(0f, width),
            end = center + Offset(0f, -length),
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
}