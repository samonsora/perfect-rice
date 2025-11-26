package com.example.team1application

import androidx.compose.foundation.layout.Arrangement // ← 追加！
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme // ← 追加！
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment // ← 追加！
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // Column = 縦に並べるレイアウト
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        // ✨ ここが「中央寄せ」の魔法！
        verticalArrangement = Arrangement.Center, // 縦の真ん中に！
        horizontalAlignment = Alignment.CenterHorizontally // 横の真ん中に！
    ) {
        // ✨ ここが「巨大化」の魔法！
        Text(
            text = "　生活習慣\nリペアキット",
            style = MaterialTheme.typography.displayMedium // めっちゃ大きく！
        )

        // 少し隙間をあける魔法（パディング）を下に追加
        Text(
            text = "　",
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge // ちょっと大きく読みやすく
        )

        Button(onClick = { /* ボタンを押した時の動作 */ }) {
            Text(text = "タイマー")
        }
        Button(onClick = { /* ボタンを押した時の動作 */ }) {
            Text(text = "記録")
        }
        Button(onClick = { /* ボタンを押した時の動作 */ }) {
            Text(text = "設定")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    Team1ApplicationTheme {
        HomeScreen()
    }
}