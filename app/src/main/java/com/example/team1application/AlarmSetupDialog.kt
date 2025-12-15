package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

// AlarmTimeDirectInput および AlarmTimePicker は、このファイル内または外部に定義されている前提。

/**
 * ユーザーが新しいアラームの時刻を設定するためのダイアログUI。
 *
 * @param onDismiss ダイアログを閉じる際に呼び出す関数
 * @param onSave 設定が完了した際に、選択された時刻文字列 (例: "07:30") を渡して呼び出す関数
 */
@Composable
fun AlarmSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // タイムゾーンとフォーマットの設定（JSTを使用）
    val jstTimeZone = remember { TimeZone.getTimeZone("Asia/Tokyo") }
    val timeFormat = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = jstTimeZone // JSTを設定
        }
    }

    // デフォルトの時刻を現在のJST時刻で設定
    val defaultTimeString = remember {
        val calendar = Calendar.getInstance(jstTimeZone)
        timeFormat.format(calendar.time)
    }

    // UIステート
    var timeInput by remember {
        // 初期値を現在の時刻にし、カーソルを末尾に設定
        mutableStateOf(TextFieldValue(defaultTimeString, selection = TextRange(defaultTimeString.length)))
    }
    var timeError by remember { mutableStateOf<String?>(null) }
    var previousTimeInput by remember { mutableStateOf(timeInput) }
    var isDirectInputMode by remember { mutableStateOf(false) } // ピッカー/直接入力モード切替

    // 時刻のバリデーションロジック
    val isValidTime = remember(timeInput.text) {
        val parser = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            isLenient = false // 厳密なパース
            timeZone = jstTimeZone // バリデーションパーサーにも JST を設定
        }
        val text = timeInput.text
        try {
            parser.parse(text)
            timeError = null
            if (previousTimeInput.text != text) {
                previousTimeInput = timeInput
            }
            true
        } catch (_: Exception) {
            // 入力中（長さが5未満）はエラーとしない
            if (text.length == 5) {
                timeError = "時刻を正しく入力してください (HH:MM)"
            }
            false
        }
    }

    // timeInputの更新とカーソル位置の調整を行う共通関数
    val updateTimeState: (TextFieldValue) -> Unit = { newTimeValue ->
        timeInput = newTimeValue
        // バリデーションを即時実行
        isValidTime.let { }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "アラーム時刻の設定",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- 時刻入力エリア (モード切り替え) ---
                if (isDirectInputMode) {
                    // 直接数字入力モード (AlarmTimeDirectInput は定義が省略されている)
                    AlarmTimeDirectInput(
                        timeInput = timeInput,
                        timeError = timeError,
                        onTimeChange = updateTimeState
                    )
                } else {
                    // ピッカーモード (AlarmTimePicker は定義が省略されている)
                    AlarmTimePicker(
                        timeInput = timeInput,
                        timeFormat = timeFormat,
                        onTimeChange = updateTimeState
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 「モード切替」ボタン ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            isDirectInputMode = !isDirectInputMode
                            val text = timeInput.text
                            // モード切替時、カーソルを末尾に設定し直す
                            timeInput = TextFieldValue(text, selection = TextRange(text.length))
                            previousTimeInput = timeInput
                            // バリデーションを再評価し、エラーメッセージを設定
                            timeError = if (isValidTime) null else "時刻を正しく入力してください (HH:MM)"
                        }
                    ) {
                        Text(if (isDirectInputMode) "ピッカーに戻る" else "直接数字入力")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- ボタンエリア (キャンセルとOK) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isValidTime) {
                                // 選択された時刻を親（AlarmScreen）に渡し、
                                // 詳細設定画面への遷移をトリガーさせる
                                onSave(timeInput.text) // -> onTimeSelected が呼ばれる
                            } else if (timeInput.text.length == 5) {
                                // isValidTimeがfalseの場合、エラーを表示
                                timeError = "時刻を正しく入力してください (HH:MM)"
                            }
                        },
                        enabled = isValidTime // 時刻が有効な場合のみOKボタンを有効化
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}