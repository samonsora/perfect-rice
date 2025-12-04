package com.example.team1application

import androidx.compose.foundation.Canvas // Canvasを使用するために必要
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // 座標を扱うために必要
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.cos // 三角関数を使用するために必要
import kotlin.math.sin // 三角関数を使用するために必要
import kotlin.math.PI // 円周率を使用する要(π)
import java.util.Calendar // 現在の「分」と「時」を取得するために必要
import java.util.TimeZone

@Composable
fun AnalogClock(modifier: Modifier = Modifier) {
    // 現在時刻を取得
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"))
    val minute = calendar.get(Calendar.MINUTE)
    val hour = calendar.get(Calendar.HOUR) % 12 // 0-11時

    // 描画の中心と半径を計算するためのBox
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp) // 画面端から少し離す
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f * 0.9f // 時計盤面の半径
            val clockFaceColor = Color(0xFFE0E0E0) // 画像の薄いグレーの盤面色

            // 1. 時計の盤面 (円)
            drawCircle(
                color = clockFaceColor,
                center = center,
                radius = radius
            )

            // 2. 時刻の目盛り
            repeat(12) { i ->
                val angle = (PI / 6 * i).toFloat() // 30度ごと
                val startRadius = radius * 0.9f
                val endRadius = if (i % 3 == 0) radius * 0.8f else radius * 0.85f // 3時間ごとは長めに
                val lineLength = if (i % 3 == 0) radius * 0.1f else radius * 0.05f

                // 目盛りの開始点と終了点
                val start = Offset(
                    x = center.x + startRadius * sin(angle),
                    y = center.y - startRadius * cos(angle)
                )
                val end = Offset(
                    x = center.x + endRadius * sin(angle),
                    y = center.y - endRadius * cos(angle)
                )

                drawLine(
                    color = Color.Black,
                    start = start,
                    end = end,
                    strokeWidth = 4f
                )
            }

            // 3. 時針と分針 (現在の時刻に基づいて計算)

            // 分針の角度 (0分で0度、60分で360度)
            val minAngle = minute / 60f * 2f * PI
            // 時針の角度 (12時間で360度 + 分による補正)
            val hourAngle = ((hour % 12) / 12f * 2f * PI) + (minute / 60f * (PI / 6))

            val minuteHandLength = radius * 0.7f
            val hourHandLength = radius * 0.5f

            // 分針
            drawLine(
                color = Color.Black,
                start = center,
                end = Offset(
                    x = center.x + minuteHandLength * sin(minAngle.toFloat()),
                    y = center.y - minuteHandLength * cos(minAngle.toFloat())
                ),
                strokeWidth = 8f // 太さ
            )

            // 時針
            drawLine(
                color = Color.Black,
                start = center,
                end = Offset(
                    x = center.x + hourHandLength * sin(hourAngle.toFloat()),
                    y = center.y - hourHandLength * cos(hourAngle.toFloat())
                ),
                strokeWidth = 8f // 太さ
            )

            // 針の接続部 (中心の黒い点)
            drawCircle(
                color = Color.Black,
                center = center,
                radius = 8f
            )
        }
    }
}