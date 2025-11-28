package com.example.team1application

import androidx.compose.foundation.clickable // ← タップ魔法のインポート
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme

@Composable
fun TitleScreen(
    onTap: () -> Unit, // 👈 「タップされたら何するか」を受け取る穴を用意！
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // ✨ ここが「画面全体をタップ可能にする」魔法！
            .clickable { onTap() }
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "　生活習慣\nリペアキット",
            style = MaterialTheme.typography.displayMedium,
            // 真ん中揃えにするならこれも足すといいかも！
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Text(
            text = "画面をタップしてスタート", // わかりやすくしてみた！
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        // ボタンも残しておくね（ボタンを押しても何もしないけど、画面タップで進むよ）

        }
    }


@Preview(showBackground = true)
@Composable
fun TitleScreenPreview() {
    Team1ApplicationTheme {
        // プレビューでは「タップしても何もしない（{}）」としておく
        TitleScreen(onTap = {})
    }
}