package com.example.team1application

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

/**
 * 現在の時刻を文字列として取得する関数
 * @return "HH:mm:ss" 形式の現在時刻文字列
 */
fun getCurrentTime(): String {
    // 日時フォーマットを定義 (例: 14:30:05)
    val dateFormat = SimpleDateFormat("hh:mm", Locale.JAPAN)

    // タイムゾーンを日本標準時 (JST) に明示的に設定
    dateFormat.timeZone = TimeZone.getTimeZone("Asia/Tokyo")

    // 現在の日時を取得
    val currentTime = Date()

    // 日時を定義したフォーマットで文字列に変換して返す
    return dateFormat.format(currentTime)
}



//CurrentTimeDisplayの引数「modifier = Modifier.padding(innerPadding)」

/**
 * 現在時刻を表示するコンポーザブル関数
 */
@Composable
fun CurrentTimeDisplay(modifier: Modifier = Modifier) {
    // 別のファイルで定義した関数を呼び出し、現在時刻を取得
    val currentTimeString = getCurrentTime()

    // Boxコンポーザブルを使用して、その中の要素（Text）を中央に配置
    Box(
        // BoxにModifier.fillMaxSize()を適用し、親（Surface）の領域全体を使う
        modifier = Modifier.fillMaxSize(),
        // Box内のコンテンツ（Text）を中央に揃える
        contentAlignment = Alignment.Center
    ) {
        // 現在時刻を表示するTextコンポーザブル
        Text(
            text = "現在の時刻\n",
            // デフォルトのModifierを適用。ここではTextの装飾は最低限。
            modifier = Modifier,
            fontSize = 32.sp,
            // テキストを中央揃えにするためにTextAlign.Centerを使用するのが一般的ですが、
            // Boxの中央配置だけでも画面中央には表示されます。
        )
        Text(
            text = "\n\n\n$currentTimeString",
            // デフォルトのModifierを適用。ここではTextの装飾は最低限。
            modifier = Modifier,
            fontSize = 60.sp,
            // テキストを中央揃えにするためにTextAlign.Centerを使用するのが一般的ですが、
            // Boxの中央配置だけでも画面中央には表示されます。
        )

        // ★ 左下ボタン (ストップ)
        Button(
            onClick = { /* ストップボタンがクリックされた時の処理 */ },
            modifier = Modifier
                .align(Alignment.BottomStart) // 親のBoxの左下に配置
                .padding(start = 24.dp, bottom = 120.dp) // 画像に近いパディング// ★ 変更2: 高さを約1.5倍に拡大 (元のボタンのデフォルト高さは約48dp程度を想定)
                .width(120.dp)
                .height(72.dp) ,
            shape = RoundedCornerShape(120.dp), // 角丸の形状
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)) // 薄いグレーの背景色modifier = Modifier.width(200.dp)
        ) {
            Text("ストップ", color = Color.Black) // 黒文字
        }

        // ★ 右下ボタン (スヌーズ)
        Button(
            onClick = { /* スヌーズボタンがクリックされた時の処理 */ },
            modifier = Modifier
                .align(Alignment.BottomEnd) // 親のBoxの右下に配置
                .padding(end = 24.dp, bottom = 120.dp) // 画像に近いパディング
                .width(120.dp)
                .height(72.dp) ,
            shape = RoundedCornerShape(300.dp), // 角丸の形状
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)) // 薄いグレーの背景色
        ) {
            Text("スヌーズ", color = Color.Black) // 黒文字
        }
    }

}




