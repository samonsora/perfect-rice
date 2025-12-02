package com.example.team1application

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * JST (日本標準時) を基準とした時刻計算とフォーマットを行うユーティリティクラス。
 * このクラスはUIに依存せず、時間データの提供のみを行います。
 * @constructor 新しいClockTimeインスタンスを作成します。
 */
class ClockTime1 {

    // --- タイムゾーンを設定するヘルパー関数を定義 ---
    private fun getJstCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        // タイムゾーンを "Asia/Tokyo" (日本標準時: JST) に設定
        calendar.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        return calendar
    }

    /**
     * 現在の時刻を "時:分:秒" の形式で取得する。
     * @return 現在時刻の文字列 (例: "14:07:00")
     */
    fun getCurrentTime(): String {
        // 現在のインスタンスを取得
        val calendar = getJstCalendar()

        // 日付と時刻のフォーマットを指定（例: 時:分:秒）
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Tokyo") // SimpleDateFormatにも設定

        return dateFormat.format(calendar.time)
    }

    /**
     * 現在の年、月、日、曜日を "yyyy/MM/dd (E)" の形式で取得する。
     * @return 現在日付と曜日の文字列 (例: "2025/11/26 (水)")
     */
    fun getCurrentDate(): String {
        val calendar = getJstCalendar()
        // フォーマット指定 (yyyy:年, MM:月, dd:日, E:曜日)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    /**
     * 現在時刻の時、分、秒を Calendar から取得し、Tripleとして返す。
     * @return Triple<時, 分, 秒> (24時間形式の時)
     */
    fun getHoursMinutesSeconds(): Triple<Int, Int, Int> {
        val calendar = getJstCalendar()
        // Calendar.HOUR は 12時間形式で 0-11 を返す
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val ampm = calendar.get(Calendar.AM_PM) // AM/PM (0/1)

        // 24時間形式の時を計算 (0-23)
        val h = when {
            ampm == Calendar.PM && hour != 0 -> hour + 12
            ampm == Calendar.AM && hour == 0 -> 0
            else -> hour
        }

        return Triple(h, minute, second)
    }
}