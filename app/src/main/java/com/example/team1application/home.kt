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

// 1. これが新しい「大元のホーム画面」！
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 📖 4ページあって、最初は0ページ目からスタート！
    val pagerState = rememberPagerState(pageCount = { 5 }, initialPage = 0)

    // ↔️ 横スワイプの魔法陣
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HomeMainContent() // ホーム画面
            1 -> AlarmScreen() // アラーム設定画面
            2 -> RirekiScreen() // 履歴表示画面
            3 -> ClockScreen() // 現在時刻表示画面
            4 ->SettingsScreen(onBack = { }) // 設定画面
             //
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
                fontSize = 120.sp,

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
        }

    }
}


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