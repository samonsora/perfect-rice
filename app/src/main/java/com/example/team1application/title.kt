package com.example.team1application

import android.media.MediaPlayer // ← 音魔法のインポート
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope // ← 時間魔法のインポート
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext // ← 文脈魔法のインポート
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme
import kotlinx.coroutines.delay // ← 待機魔法のインポート
import kotlinx.coroutines.launch // ← 非同期魔法のインポート

@Composable
fun TitleScreen(
    onTap: () -> Unit, // 👈 「タップされたら何するか」を受け取る穴を用意！
    modifier: Modifier = Modifier
) {
    // 🪄 必要な道具を準備！
    val context = LocalContext.current // 「今ここ」の情報
    val scope = rememberCoroutineScope() // 「時間を操る」ためのスコープ

    // 🎵 音を鳴らすプレイヤーを用意（R.raw.xxx は自分のファイル名に変えて！）
    // ※ プレビューでエラーになる場合は、ここをコメントアウトしてね
    val mediaPlayer = MediaPlayer.create(context, R.raw.tap_sound)

    Column(
        modifier = modifier
            .fillMaxSize()
            // ✨ ここが新しいタップ魔法！
            .clickable {
                // 1. 音をスタート！ポーン♪
                mediaPlayer.start()

                // 2. 時間操作の魔法陣を展開（コルーチン）
                scope.launch {
                    // 3. 音の余韻を楽しむために、少し待つ（例：300ミリ秒 = 0.3秒）
                    delay(300)

                    // 4. 待ち終わったら、画面切り替えの合図を送る！
                    onTap()
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // (中身のTextとかはそのまま…)
        Text(
            text = "　生活習慣\nリペアキット",
            style = MaterialTheme.typography.displayMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        // ...
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