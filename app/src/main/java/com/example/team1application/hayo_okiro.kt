package com.example.team1application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@Composable
fun GetupTimeDisplay(modifier: Modifier = Modifier) {
    // 別のファイルで定義した関数を呼び出し、現在時刻を取得
    val currentTimeString = getCurrentTime()

    // Boxコンポーザブルを使用して、その中の要素（Text）を中央に配置
    Box(
        // BoxにModifier.fillMaxSize()を適用し、親（Surface）の領域全体を使う
        modifier = Modifier.fillMaxSize(),
        // Box内のコンテンツ（Text）を中央に揃える
        contentAlignment = Alignment.Center
    ) {

        // ★ 1. アナログ時計の配置 (画面上部に約40%のスペースを占めさせる)
        ClockScreen(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp) // 高さを固定してアナログ時計にスペースを与える
                .align(Alignment.TopCenter) // Boxの上部中央に配置
                .offset(y = 60.dp)
        )

        // 現在時刻を表示するTextコンポーザブル
        Text(
            text = "現在時刻",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp) // 時刻との間にパディング
        )
        Text(
            text = "\n\n\n\n\n$currentTimeString",
            // デフォルトのModifierを適用。ここではTextの装飾は最低限。
            modifier = Modifier,
            fontSize = 60.sp,
            // テキストを中央揃えにするためにTextAlign.Centerを使用するのが一般的ですが、
            // Boxの中央配置だけでも画面中央には表示されます。
        )
        // ★ 変更点1: 「はよ寝ろ!!」テキストの追加
        Text(
            text = "はよ寝ろ！！",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(start = 20.dp)
                .padding(bottom = 240.dp)
                .align(Alignment.BottomCenter)// ボタンとの間にスペースを確保
        )

        // ★ 変更点2: OKボタンの配置 (1つに変更し、中央下部に配置)
        Button(
            onClick = { /* OKボタンがクリックされた時の処理 */ },
            modifier = Modifier
                .align(Alignment.BottomCenter) // 親のBoxの中央下部に配置
                .padding(bottom = 120.dp) // ボタンの位置を上に調整
                .width(90.dp) // 幅を調整
                .height(72.dp), // 高さを調整
            shape = RoundedCornerShape(36.dp), // 角丸の形状
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)) // 薄いグレーの背景色
        ) {
            Text("OK", color = Color.Black, fontSize = 18.sp) // ボタンテキストを「OK」に変更
        }

        // ★ 変更点3: 元の左右のボタンは削除
    }

}
