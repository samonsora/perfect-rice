package com.example.team1application.ui.theme

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone


class Clock {

    // --- タイムゾーンを設定するヘルパー関数を定義 ---
    private fun getJstCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        // タイムゾーンを "Asia/Tokyo" (日本標準時: JST) に設定
        calendar.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        return calendar
    }

    /**
     * 現在の時刻を "時:分:秒" の形式で取得する。
     * minSdk 24でも動作するように java.util.Calendar を使用。
     * @return 現在時刻の文字列 (例: "14:07:00")
     */
    fun getCurrentTime(): String {
        // 現在のインスタンスを取得
        val calendar = getJstCalendar()

        // 日付と時刻のフォーマットを指定（例: 時:分:秒）
        // Locale.getDefault() で端末のロケールを使用
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Tokyo") // SimpleDateFormatにも設定

        // CalendarインスタンスからDateオブジェクトを取得し、フォーマットを適用
        return dateFormat.format(calendar.time)
    }

    /**
     * 現在の年、月、日、曜日を "yyyy/MM/dd (E)" の形式で取得する。
     * @return 現在日付と曜日の文字列 (例: "2025/11/26 (水)")
     */
    fun getCurrentDate(): String {
        val calendar = getJstCalendar()
        // フォーマット指定 (yyyy:年, MM:月, dd:日, E:曜日)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    // --- 新規追加の関数 ---

    /**
     * 現在時刻の時、分、秒を Calendar から取得し、Tripleとして返す。
     * @return Triple<時, 分, 秒>
     */
    fun getHoursMinutesSeconds(): Triple<Int, Int, Int> {
        val calendar = getJstCalendar()
        val hour = calendar.get(Calendar.HOUR) // 12時間形式で取得 (0-11)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val ampm = calendar.get(Calendar.AM_PM) // AM/PM (0/1)

        // 12時間形式 (0-11) を通常の時 (0-11) に変換。
        // HOUR は 12AM (00:00) を 0、12PM を 0 として返す場合があるため調整
        val h = if (ampm == Calendar.PM && hour != 0) hour + 12 else if (ampm == Calendar.AM && hour == 0) 0 else hour

        return Triple(h, minute, second)
    }

    /**
     * 時、分、秒から時計の針の角度（度）を計算する。
     * 角度は 12時（または 0秒/分）の位置を 0度とし、時計回りに増加する。
     * @param h 時 (0-23)
     * @param m 分 (0-59)
     * @param s 秒 (0-59)
     * @return Triple<時針の角度, 分針の角度, 秒針の角度>
     */
    fun calculateHandAngles(h: Int, m: Int, s: Int): Triple<Float, Float, Float> {
        // 秒針の角度: 1秒で 360/60 = 6度
        val secondAngle = s * 6f

        // 分針の角度: 1分で 360/60 = 6度。秒の影響も考慮
        // m/60 * 360 + s/60 * 6 = (m + s/60) * 6
        val minuteAngle = (m + s / 60f) * 6f

        // 時針の角度: 12時間で 360度。1時間で 30度。分と秒の影響も考慮
        // h_12/12 * 360 = h_12 * 30. (h_12 + m/60 + s/3600) * 30
        // h % 12 で 12時間形式に変換
        val hourAngle = ((h % 12) + m / 60f + s / 3600f) * 30f

        // 角度を 12時の位置 (Y軸正方向) から時計回りに計算するため、
        // 90度ずらし（時計の12時を0度にする）
        // そして、Composeの rotate 関数は時計回りが正なので、このまま使用可能。
        // ただし、時計の表示では12時が0度（Y軸正方向）なので、計算した角度から 90度を引くか、
        // 単に計算ロジックに合わせる。ここで計算した角度は、X軸正方向から反時計回りを0度
        // とした場合の角度です。ここでは、**12時の方向を0度**とし、**時計回りを正**として調整します。

        // 基準（12時）から時計回りを正の角度とする (0-360)
        return Triple(hourAngle, minuteAngle, secondAngle)
    }
}

// ユーザーが提供した `Greeting` を `GreetingContent` に変更し、時刻表示と時計描画ロジックを追加
@Composable
fun GreetingContent(modifier: Modifier = Modifier) {
    // Clockクラスのインスタンスを作成 (再コンポジションで再作成されないように remember を使用しても良いが、ここではシンプルに)
    val myClock = Clock()

    // 現在の時刻と日付、針の角度を状態として保持
    var currentTime by remember { mutableStateOf(myClock.getCurrentTime()) }
    var currentDate by remember { mutableStateOf(myClock.getCurrentDate()) }
    // 角度は Triple<時, 分, 秒>
    var handAngles by remember { mutableStateOf(Triple(0f, 0f, 0f)) }

    // 毎秒（または毎フレーム）時刻を更新する処理
    LaunchedEffect(Unit) {
        while (isActive) {
            // withFrameMillis を使用すると、より滑らかなアニメーションが可能
            // ここでは秒針を滑らかにするため、毎フレーム更新
            withFrameMillis {
                // 時刻を更新
                currentTime = myClock.getCurrentTime()
                currentDate = myClock.getCurrentDate()

                // 角度を計算
                val (h, m, s) = myClock.getHoursMinutesSeconds()
                handAngles = myClock.calculateHandAngles(h, m, s)
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