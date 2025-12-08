package com.example.team1application

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TitleScreen(
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable {

                scope.launch {
                    delay(300)
                    onTap()
                }
            }
    ) {
        // 🖼️ 背景画像：ここがポイント！
        Image(
            painter = painterResource(id = R.drawable.kakasi),
            contentDescription = "背景画像",
            // 親(Box)いっぱいに広げる
            modifier = Modifier.fillMaxSize(),
            // ✨【重要】ここが「拡大して余白を消す」魔法！
            // Crop = 画面を埋め尽くすように拡大する（はみ出た分はカット）
            // FillBounds = 画像を無理やり引き伸ばして画面サイズに合わせる
            contentScale = ContentScale.Crop
        )

        // 📝 文字などのコンテンツ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "　生活習慣　\nリペアキット",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.White
            )

            Text(
                text = "画面をタップしてスタート",
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(200.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TitleScreenPreview() {
    Team1ApplicationTheme {
        TitleScreen(onTap = {})
    }
}