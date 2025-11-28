package com.example.team1application

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




