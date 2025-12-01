package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager // ← スワイプ魔法
import androidx.compose.foundation.pager.rememberPagerState // ← ページ記憶魔法
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme

// 1. これが新しい「大元のホーム画面」！
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 📖 3ページあって、最初は真ん中（1ページ目）からスタート！
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 1)

    // ↔️ 横スワイプの魔法陣
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> RirekiScreen() // 👈 左のページ
            1 -> HomeMainContent() // 🏠 真ん中のページ（元のホーム画面）
            2 -> ClockScreen() // 👉 右のページ
        }
    }
}

// --------------------------------------------------
// 🏠 真ん中のページ
// --------------------------------------------------
@Composable
fun HomeMainContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ホーム画面！",
            style = MaterialTheme.typography.displayMedium
        )



        Button(onClick = { /* ボタンの動作 */ }) {
            Text(text = "記録")
        }
    }
}



// --------------------------------------------------


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