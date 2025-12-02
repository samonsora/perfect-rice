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
    // 📖 4ページあって、最初は0ページ目からスタート！
    val pagerState = rememberPagerState(pageCount = { 4 }, initialPage = 0)

    // ↔️ 横スワイプの魔法陣
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HomeMainContent() // ホーム画面
            1 -> RirekiScreen() // 履歴
            2 -> ClockScreen() //
            3 -> AlarmScreen() //
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