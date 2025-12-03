package com.example.team1application

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape // ← 形の魔法
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // ← 切り抜きの魔法
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// 1. 大元のホーム画面
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 2)

    // 🔑 アラームデータの管理
    val allAlarms = remember { getDummyAlarmSettings() }

    // 🔄 データ更新
    val onToggleActive: (Int, Boolean) -> Unit = { alarmId, newState ->
        val index = allAlarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val oldAlarm = allAlarms[index]
            allAlarms[index] = oldAlarm.copy(isActive = newState)
        }
    }

    // 🕰️ 12時間後の計算
    val targetTimeDisplay = remember(allAlarms.toList()) {
        val activeAlarm = allAlarms.firstOrNull { it.isActive }
        if (activeAlarm != null) {
            calculate12HoursLater(activeAlarm.time)
        } else {
            "--:--"
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            //0 ->
            1 -> RirekiScreen()
            2 -> HomeMainContent(targetTime = targetTimeDisplay)
            3 -> AlarmScreen(
                alarms = allAlarms,
                onToggleActive = onToggleActive
            )
            4 -> ClockScreen()
            else -> Text("準備中...")
        }
    }
}

// --------------------------------------------------
// 🏠 ホーム画面の中身（修正版！）
// --------------------------------------------------
@Composable
fun HomeMainContent(targetTime: String) {
    var timeString by remember { mutableStateOf("00:00") }

    // 🎨 プレートの色
    val plateColor = Color(0xFFCFD8DC)

    LaunchedEffect(Unit) {
        while (isActive) {
            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            timeString = formatter.format(calendar.time)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🟥 上のエリア（時計）
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // さっき "12:34" になってたのを、動く時計(timeString)に戻したよ！
            Text(
                text = timeString,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                lineHeight = 110.sp
            )
        }

        // 🟦 下のエリア
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            // ✨✨ ここが魔法のプレート（ここだけ色付き！） ✨✨
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 16.dp)
                    // 角丸と背景色
                    .clip(RoundedCornerShape(20.dp))
                    .background(plateColor)
                    // 文字周りの余白
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "次のアラーム時間",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                Text(
                    text = targetTime,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            } // ← プレートの閉じカッコ（ボタンは外に出す！）

            // バネ（残りスペースを埋める）
            Spacer(modifier = Modifier.weight(1f))

            // 🔘 ボタン（プレートの外にある！）
            Button(
                onClick = { /* 動作 */ },
                modifier = Modifier.padding(bottom = 48.dp)
            ) {
                Text("記録")
            }

            // 下の余白調整
            Spacer(modifier = Modifier.weight(0.5f))

        } // ← 下エリアのColumn閉じ
    } // ← 大元のColumn閉じ
}

// 🧙‍♀️ 計算用の魔法
fun calculate12HoursLater(originalTime: String): String {
    return try {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = formatter.parse(originalTime) ?: return "--:--"
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.HOUR_OF_DAY, 0)
        formatter.format(calendar.time)
    } catch (e: Exception) {
        "--:--"
    }
}