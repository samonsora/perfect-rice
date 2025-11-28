package com.example.team1application

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class Clock {

    // --- タイムゾーンを設定するヘルパー関数を定義 ---
    private fun getJstCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        // タイムゾーンを "Asia/Tokyo" (日本標準時: JST) に設定
        calendar.timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        return calendar
    }

    /**
     * 現在の時刻を "時:分:秒" の形式で取得する。
     * minSdk 24でも動作するように java.util.Calendar を使用。
     * @return 現在時刻の文字列 (例: "14:07:00")
     */
    fun getCurrentTime(): String {
        // 現在のインスタンスを取得
        val calendar = getJstCalendar()

        // 日付と時刻のフォーマットを指定（例: 時:分:秒）
        // Locale.getDefault() で端末のロケールを使用
        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("Asia/Tokyo") // SimpleDateFormatにも設定

        // CalendarインスタンスからDateオブジェクトを取得し、フォーマットを適用
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

    // --- 新規追加の関数 ---

    /**
     * 現在時刻の時、分、秒を Calendar から取得し、Tripleとして返す。
     * @return Triple<時, 分, 秒>
     */
    fun getHoursMinutesSeconds(): Triple<Int, Int, Int> {
        val calendar = getJstCalendar()
        val hour = calendar.get(Calendar.HOUR) // 12時間形式で取得 (0-11)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)
        val ampm = calendar.get(Calendar.AM_PM) // AM/PM (0/1)

        // 12時間形式 (0-11) を通常の時 (0-11) に変換。
        // HOUR は 12AM (00:00) を 0、12PM を 0 として返す場合があるため調整
        val h = if (ampm == Calendar.PM && hour != 0) hour + 12 else if (ampm == Calendar.AM && hour == 0) 0 else hour

        return Triple(h, minute, second)
    }

    /**
     * 時、分、秒から時計の針の角度（度）を計算する。
     * 角度は 12時（または 0秒/分）の位置を 0度とし、時計回りに増加する。
     * @param h 時 (0-23)
     * @param m 分 (0-59)
     * @param s 秒 (0-59)
     * @return Triple<時針の角度, 分針の角度, 秒針の角度>
     */
    fun calculateHandAngles(h: Int, m: Int, s: Int): Triple<Float, Float, Float> {
        // 秒針の角度: 1秒で 360/60 = 6度
        val secondAngle = s * 6f

        // 分針の角度: 1分で 360/60 = 6度。秒の影響も考慮
        // m/60 * 360 + s/60 * 6 = (m + s/60) * 6
        val minuteAngle = (m + s / 60f) * 6f

        // 時針の角度: 12時間で 360度。1時間で 30度。分と秒の影響も考慮
        // h_12/12 * 360 = h_12 * 30. (h_12 + m/60 + s/3600) * 30
        // h % 12 で 12時間形式に変換
        val hourAngle = ((h % 12) + m / 60f + s / 3600f) * 30f

        // 角度を 12時の位置 (Y軸正方向) から時計回りに計算するため、
        // 90度ずらし（時計の12時を0度にする）
        // そして、Composeの rotate 関数は時計回りが正なので、このまま使用可能。
        // ただし、時計の表示では12時が0度（Y軸正方向）なので、計算した角度から 90度を引くか、
        // 単に計算ロジックに合わせる。ここで計算した角度は、X軸正方向から反時計回りを0度
        // とした場合の角度です。ここでは、**12時の方向を0度**とし、**時計回りを正**として調整します。

        // 基準（12時）から時計回りを正の角度とする (0-360)
        return Triple(hourAngle, minuteAngle, secondAngle)
    }
}