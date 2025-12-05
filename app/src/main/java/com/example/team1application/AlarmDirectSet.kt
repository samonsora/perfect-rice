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
    // 既存の直接入力ロジックを移植
    val colonIndex = 2

    OutlinedTextField(
        value = timeInput,
        onValueChange = { v ->
            val currentText = timeInput.text // 変更前のテキスト ("HH:MM")
            val newText = v.text // 変更後のテキスト
            val oldCursor = timeInput.selection.start // 変更前のカーソル位置
            val newCursor = v.selection.start // 変更後の意図されたカーソル位置

            println(newText)

            // 1. 全角数字を半角に変換し、数字とコロンのみを抽出
            val filteredNewText = newText
                .replace(Regex("[０-９]")) {
                    (it.value[0].code - '０'.code + '0'.code).toChar().toString()
                }
                .filter { it.isDigit() || it == ':' }
                .take(5) // 長さを5に制限

            println(filteredNewText)

            // 2. 変更の種類を特定（削除、挿入）
            val isDeletion = filteredNewText.length < currentText.length
            val isInsertion = filteredNewText.length > currentText.length

            // 挿入された文字の特定
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
            var hh = if (currentText.length >= 2) currentText.substring(0, 2) else "00"
            var mm = if (currentText.length >= 5) currentText.substring(3, 5) else "00"
            var nextText: String
            var nextCursor: Int

            if (isDeletion) {
                // 削除ロジック（変更なし）
                val delIndex = oldCursor - 1 // 削除対象のインデックス

                nextCursor = when (delIndex) {
                    colonIndex -> {
                        hh = hh.replaceRange(1, 2, '0'.toString())
                        colonIndex - 1
                    }
                    in 0..1 -> {
                        hh = hh.replaceRange(delIndex, delIndex + 1, '0'.toString())
                        delIndex
                    }
                    in 3..4 -> {
                        mm = mm.replaceRange(delIndex - 3, delIndex - 2, '0'.toString())
                        delIndex
                    }
                    else -> oldCursor
                }
                nextText = "$hh:$mm"

            } else if (isInsertion && charInput != null) {
                // --- 数字入力ロジック (上書き) ---
                val insIndex = oldCursor // 上書き対象のインデックス (カーソル位置)

                // コロンの位置は、入力された数字で上書きせずにスキップする。
                // ただし、コロンの位置にいる場合は分に移動させる。
                if (insIndex == colonIndex) {
                    nextText = currentText // テキストは変更しない
                    nextCursor = insIndex + 1 // カーソルを次の数字の位置へ強制移動
                }
                // カーソル位置の文字が数字であり、かつそれが '0' である場合にのみ上書きする
                else if (insIndex in 0..4 && currentText.getOrNull(insIndex) == '0') {
                    when (insIndex) {
                        in 0..1 -> {
                            // 時(hh)の上書き
                            hh = hh.replaceRange(insIndex, insIndex + 1, charInput.toString())
                            nextCursor = insIndex + 1
                        }
                        in 3..4 -> {
                            // 分(mm)の上書き
                            mm = mm.replaceRange(insIndex - 3, insIndex - 2, charInput.toString())
                            nextCursor = insIndex + 1
                        }
                        else -> { // これは insIndex == 2 のコロンの位置を処理する場所ですが、上ですでに処理されています。
                            nextCursor = oldCursor
                        }
                    }
                    nextText = "$hh:$mm"
                } else {
                    // カーソル位置の文字が '0' ではない場合、または範囲外の場合、入力を無視しカーソルのみを移動させる
                    nextText = currentText
                    nextCursor = insIndex + 1 // カーソルを1つ右に移動させる（標準的な入力動作を模倣）
                    if (nextCursor == colonIndex) {
                        nextCursor++ // コロンをスキップ
                    }
                }

                // カーソル位置の最終調整 (最大5を超えないように)
                nextCursor = nextCursor.coerceIn(0, 5)


            } else {
                // --- その他の変更 (カーソル移動など) ---
                nextText = currentText
                nextCursor = newCursor.coerceIn(0, 5) // カーソル位置を 0-5 に制限
            }

            // 4. 最終的なステートの更新を親コンポーネントに通知
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
}