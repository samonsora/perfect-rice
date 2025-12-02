package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box // ← 📦 これが足りなかった！
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.team1application.ui.theme.Team1ApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// 1. 大元のホーム画面
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> RirekiScreen() // 👈 左
            1 -> HomeMainContent() // 🏠 真ん中
            2 -> ClockScreen() // 👉 右
        }
    }
}

// --------------------------------------------------
// 🏠 真ん中のページ（修正版！）
// --------------------------------------------------
@Composable
fun HomeMainContent() {
    var timeString by remember { mutableStateOf("00:00") }

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
            Text(
                text = timeString,
                // ✨ ここが巨大化の呪文！ ✨
                // 100.sp, 120.sp... 数字を大きくすればどこまでもデカくなるよ！
                fontSize = 110.sp,

                // 文字を太くして、デジタル時計っぽくクッキリさせる！
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,

                // 行の高さも調整して、上下の余白をいい感じに締める
                lineHeight = 110.sp,

                // 色を変えたいならこう書く！(例：濃いグレー)
                color = androidx.compose.ui.graphics.Color.DarkGray
            )
        }

        // 🟦 下のエリア（ボタンとか）
        Column(
            modifier = Modifier
                .weight(2f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ホーム画面！",
                style = MaterialTheme.typography.headlineMedium
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

            Button(onClick = { /* ボタンの動作 */ }) {
                Text(text = "記録")
            }
        } // ← 【修正】下のエリアの Column を閉じる！

    } // ← 【修正】大元の Column を閉じる！
} // ← 【修正】関数の終わりを閉じる！


// --------------------------------------------------
// 📺 プレビュー
// --------------------------------------------------
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Team1ApplicationTheme {
        HomeScreen()
    }
}