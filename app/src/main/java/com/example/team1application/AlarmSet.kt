package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * ユーザーが新しいアラームを設定するためのダイアログUI
 * API Level 24対応のためSimpleDateFormatを使用
 * @param onDismiss ダイアログを閉じる際に呼び出す関数
 * @param onSave 設定が完了した際に、選択された時刻文字列 (例: "07:30") を渡して呼び出す関数
 */
@Composable
fun AlarmSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // API 24互換の現在時刻取得とフォーマット
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val defaultTimeString = remember { timeFormat.format(Calendar.getInstance().time) }

    // timeInputの初期値を、現在の時刻を hh:mm 形式で取得し、カーソルを末尾に設定
    var timeInput by remember {
        mutableStateOf(TextFieldValue(defaultTimeString, selection = TextRange(defaultTimeString.length)))
    }
    var timeError by remember { mutableStateOf<String?>(null) }
    var previousTimeInput by remember { mutableStateOf(timeInput) }

    var isDirectInputMode by remember { mutableStateOf(false) }

    val calendar = remember(timeInput.text) {
        val cal = Calendar.getInstance()
        try {
            // パラメータ 'e' を '_' に変更
            cal.time = timeFormat.parse(timeInput.text) ?: cal.time
        } catch (_: Exception) {
            // パース失敗時は現在の時刻のまま
        }
        cal
    }

    val isValidTime = remember(timeInput.text) {
        val parser = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            isLenient = false // 厳密なチェックを有効にする
        }
        val text = timeInput.text
        try {
            parser.parse(text)
            // 時刻の範囲チェック (00:00 - 23:59) は SimpleDateFormat(isLenient=false) が行ってくれる
            timeError = null
            if (previousTimeInput.text != text) {
                previousTimeInput = timeInput
            }
            true
        } catch (_: Exception) {
            // パラメータ 'e' を '_' に変更
            // 形式が "hh:mm" であっても、"24:00" など不正な時刻はここで弾かれる
            timeError = "hh:mm形式で入力してください"
            false
        }
    }

    /**
     * Calendarを操作し、結果をtimeInputステートに反映するヘルパー関数
     */
    val updateTime: (Int, Int) -> Unit = { field, amount ->
        calendar.add(field, amount)
        val newTimeString = timeFormat.format(calendar.time)
        // カーソルは常に末尾に設定
        timeInput = TextFieldValue(newTimeString, selection = TextRange(newTimeString.length))
        previousTimeInput = timeInput
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
                    text = "新しいアラームを設定",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- 時刻表示/入力エリア ---
                if (isDirectInputMode) {
                    OutlinedTextField(
                        value = timeInput,
                        // 引数名を v に変更 (newValue の短縮形)
                        onValueChange = { v ->
                            val currentText = timeInput.text // 変更前のテキスト
                            val newText = v.text // 変更後のテキスト
                            val oldCursor = timeInput.selection.start // 変更前のカーソル位置
                            val newCursor = v.selection.start // 変更後の意図されたカーソル位置
                            val colonIndex = 2

                            // 1. 全角数字を半角に変換し、数字とコロンのみを抽出
                            // cleanInput は未使用のため削除。フィルタリングと変換は newText で行う
                            val filteredNewText = newText
                                .replace(Regex("[０-９]")) {
                                    (it.value[0].code - '０'.code + '0'.code).toChar().toString()
                                }
                                .filter { it.isDigit() || it == ':' }

                            // 2. 変更の種類を特定（削除、挿入）
                            val isDeletion = filteredNewText.length < currentText.length
                            val isInsertion = filteredNewText.length > currentText.length

                            // 挿入された文字の特定
                            val charInput: Char? = if (isInsertion && filteredNewText.length == currentText.length + 1) {
                                // 修正: 挿入が単一文字であると仮定し、新しいテキストから古いテキストの文字を削除した残りの文字を挿入文字とする。
                                // 例: "07:30" -> "07:340" (filteredNewText)
                                // 07:30: currentText

                                // ただし、この方法ではコロン(:)が入力された後に次の文字を入力すると、
                                // "07:3:4"のようになり、filteredNewTextにコロンが含まれているとロジックが破綻する。

                                // 最も安定したロジック:
                                // 1. currentTextの文字を新しいテキストから一旦全て取り除く
                                var remaining = filteredNewText
                                currentText.forEach { c -> remaining = remaining.replaceFirst(c.toString(), "") }

                                // 2. 残りが1文字で数字であればそれが挿入された文字
                                if (remaining.length == 1 && remaining[0].isDigit()) {
                                    remaining[0]
                                } else {
                                    null
                                }
                            } else null

                            // 3. 最終的な hh:mm 形式の文字列を生成
                            var hh = currentText.substring(0, 2)
                            var mm = currentText.substring(3, 5)
                            var nextText: String = currentText
                            var nextCursor = oldCursor

                            if (isDeletion) {
                                // --- 削除ロジック (Backspace) ---
                                val delIndex = oldCursor - 1 // 削除対象のインデックス

                                nextCursor = when (delIndex) {
                                    colonIndex -> {
                                        // ルール: コロンの削除は受け付けず、コロンの一つ左側の数字（h2）がクリアされる
                                        hh = hh.replaceRange(1, 2, '0'.toString())
                                        colonIndex - 1 // カーソルを位置2へ移動
                                    }
                                    in 0..1 -> {
                                        // 時(hh)の削除: 左側の数字をクリア
                                        hh = hh.replaceRange(delIndex, delIndex + 1, '0'.toString())
                                        delIndex // カーソルを左へ移動
                                    }
                                    in 3..4 -> {
                                        // 分(mm)の削除: 左側の数字をクリア
                                        mm = mm.replaceRange(delIndex - 3, delIndex - 2, '0'.toString())
                                        delIndex // カーソルを左へ移動
                                    }
                                    else -> oldCursor
                                }
                                nextText = "$hh:$mm"

                            } else if (isInsertion && charInput != null) {
                                // --- 数字入力ロジック (上書き) ---
                                val insIndex = oldCursor // 上書き対象のインデックス (カーソル位置)

                                nextCursor = when (insIndex) {
                                    colonIndex -> {
                                        // コロンの位置(2)では、分の十の位(mm[0] = global index 3)を上書きし、
                                        // カーソルを3へスキップ
                                        mm = mm.replaceRange(0, 1, charInput.toString()) // mmのインデックス0を置換
                                        colonIndex + 2 // カーソルを4へ
                                    }
                                    in 0..1 -> {
                                        // 時(hh)の上書き
                                        hh = hh.replaceRange(insIndex, insIndex + 1, charInput.toString())
                                        insIndex + 1
                                    }
                                    in 3..4 -> {
                                        // 分(mm)の上書き
                                        // insIndex 3 -> mmインデックス 0 を置換
                                        // insIndex 4 -> mmインデックス 1 を置換
                                        mm = mm.replaceRange(insIndex - 3, insIndex - 2, charInput.toString())
                                        insIndex + 1
                                    }
                                    else -> oldCursor // 位置5またはその他の例外は無視
                                }
                                nextText = "$hh:$mm"
                            } else {
                                // --- その他の変更 (カーソル移動など) ---
                                nextText = currentText
                                nextCursor = newCursor.coerceIn(0, 5) // カーソル位置を 0-5 に制限
                            }

                            // 4. 最終的なステートの更新
                            timeInput = TextFieldValue(
                                text = nextText,
                                selection = TextRange(nextCursor)
                            )
                        },
                        label = { Text("時刻 (例: 07:30)") },
                        isError = timeError != null && timeInput.text.length == 5,
                        supportingText = {
                            if (timeInput.text.length == 5 && timeError != null) {
                                Text(text = timeError!!, color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("24時間表記 (HH:mm) - 自動でhh:mm形式を維持")
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // **ピッカーモードのUI**
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timeInput.text,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )


                        // カスタム操作ボタンの行
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 時と分の表示 (省略) - 実際にはカレンダーの値を使用
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("")
                                    Text(
                                        text = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0'),
                                        fontSize = 32.sp,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("")
                                }
                                Text(" : ", fontSize = 32.sp)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("")
                                    Text(
                                        text = calendar.get(Calendar.MINUTE).toString().padStart(2, '0'),
                                        fontSize = 32.sp,
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("")
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

                Spacer(modifier = Modifier.height(24.dp))

                // --- 「直接数字入力」ボタン ---
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
                            timeError = if (isValidTime) null else "hh:mm形式で入力してください"
                        }
                    ) {
                        Text(if (isDirectInputMode) "ピッカーに戻る" else "直接数字入力")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- ボタンエリア ---
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
                                onSave(timeInput.text)
                            } else {
                                // isValidTimeがfalseの場合、エラーを表示
                                if (timeError == null) {
                                    timeError = "時刻を正しく入力してください (HH:MM)"
                                }
                            }
                        },
                        enabled = isValidTime
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}