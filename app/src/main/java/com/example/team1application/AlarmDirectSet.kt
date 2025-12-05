package com.example.team1application

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/**
 * アラーム時刻の直接数字入力UIコンポーネント
 * AlarmSetupDialogから呼び出され、状態を受け取って更新を通知する
 *
 * @param timeInput 現在の時刻入力状態 (TextFieldValue)
 * @param timeError 現在のエラーメッセージ
 * @param onTimeChange 時刻状態が変更されたときに呼び出す関数 (TextFieldValueを返す)
 */
@Composable
fun AlarmTimeDirectInput(
    timeInput: TextFieldValue,
    timeError: String?,
    onTimeChange: (TextFieldValue) -> Unit
) {
    OutlinedTextField(
        value = timeInput,
        onValueChange = { v ->
            val currentText = timeInput.text // 変更前のテキスト
            val newText = v.text // 変更後のテキスト
            val oldCursor = timeInput.selection.start // 変更前のカーソル位置
            val newCursor = v.selection.start // 変更後の意図されたカーソル位置
            val colonIndex = 2

            println(newText)

            // 1. 全角数字を半角に変換し、数字とコロンのみを抽出
            val filteredNewText = newText
                .replace(Regex("[０-９]")) {
                    (it.value[0].code - '０'.code + '0'.code).toChar().toString()
                }
                .filter { it.isDigit() || it == ':' }

            println(filteredNewText)

            // 2. 変更の種類を特定（削除、挿入）
            val isDeletion = filteredNewText.length < currentText.length
            val isInsertion = filteredNewText.length > currentText.length

            // 挿入された文字の特定 (同一数字の入力問題を解消したロジック)
            val charInput: Char? = if (isInsertion && filteredNewText.length == currentText.length + 1) {
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
            // 警告解消のため、初期値設定を削除
            var nextText: String
            var nextCursor: Int

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
                // 警告を解消するため、このブロックを必須とする
                nextText = currentText
                nextCursor = newCursor.coerceIn(0, 5) // カーソル位置を 0-5 に制限
            }

            // 4. 最終的なステートの更新
            onTimeChange(
                TextFieldValue(
                    text = nextText,
                    selection = TextRange(nextCursor)
                )
            )
        },
        label = { Text("時刻 (例: 07:30)") },
        isError = timeError != null && timeInput.text.length == 5,
        supportingText = {
            if (timeInput.text.length == 5 && timeError != null) {
                Text(text = timeError, color = MaterialTheme.colorScheme.error)
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
}