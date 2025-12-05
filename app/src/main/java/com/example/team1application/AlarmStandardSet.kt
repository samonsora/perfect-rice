package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * アラーム時刻のピッカーUIコンポーネント
 * @param timeInput 現在の時刻入力状態 (TextFieldValue)
 * @param timeFormat 時刻のフォーマット (HH:mm)
 * @param onTimeChange 時刻状態が変更されたときに呼び出す関数
 */
@Composable
fun AlarmTimePicker(
    timeInput: TextFieldValue,
    timeFormat: SimpleDateFormat, // timeFormatは外部から渡される
    onTimeChange: (TextFieldValue) -> Unit
) {
    // timeInputのtextからCalendarインスタンスを生成
    val calendar = remember(timeInput.text) {
        val cal = Calendar.getInstance()
        try {
            // パース失敗時は現在の時刻のまま
            cal.time = timeFormat.parse(timeInput.text) ?: cal.time
        } catch (_: Exception) {
            // パース失敗時は timeInput の値を無視し、現在の時刻を使用
        }
        cal
    }

    /**
     * Calendarを操作し、結果をtimeInputステートに反映するヘルパー関数
     */
    val updateTime: (Int, Int) -> Unit = { field, amount ->
        calendar.add(field, amount)
        val newTimeString = timeFormat.format(calendar.time)
        // カーソルは常に末尾に設定
        onTimeChange(
            TextFieldValue(newTimeString, selection = TextRange(newTimeString.length))
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 大きな時刻表示
        Text(
            text = timeInput.text,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp)
        )

        // カスタム操作ボタンの行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp), // ボタンの高さに合わせて調整
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 時と分の現在の値表示
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("時", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'),
                        fontSize = 32.sp,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(" : ", fontSize = 32.sp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = calendar.get(Calendar.MINUTE).toString().padStart(2, '0'),
                        fontSize = 32.sp,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // カスタム操作ボタン
            Column {
                OutlinedButton(onClick = { updateTime(Calendar.MINUTE, 10) }) { Text("+10分") }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { updateTime(Calendar.MINUTE, 5) }) { Text("+5分") }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { updateTime(Calendar.MINUTE, -5) }) { Text("-5分") }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { updateTime(Calendar.MINUTE, -10) }) { Text("-10分") }
            }
        }
    }
}