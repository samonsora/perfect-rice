package com.example.team1application

// DayOfWeekUtils.kt
import java.util.Calendar

object DayOfWeekUtils {
    // 曜日文字列とそのCalendar定数のマッピング (日:1, 月:2, ..., 土:7)
    private val dayMapping = mapOf(
        "日" to Calendar.SUNDAY, "月" to Calendar.MONDAY, "火" to Calendar.TUESDAY,
        "水" to Calendar.WEDNESDAY, "木" to Calendar.THURSDAY, "金" to Calendar.FRIDAY,
        "土" to Calendar.SATURDAY
    )

    /**
     * 曜日文字列 (例: "月, 火") を Calendar.DAY_OF_WEEK の定数リストに変換する
     */
    fun parseDays(days: String): List<Int> {
        // ... (前回の回答と同じロジックをここに配置)
        if (days == "毎日") {
            return dayMapping.values.toList()
        }
        return days.split(",").mapNotNull { dayStr ->
            dayMapping[dayStr.trim()]
        }
    }

    /**
     * 設定IDとCalendar定数から、PendingIntent用のユニークなリクエストコードを生成
     */
    fun generateRequestCode(settingId: Int, dayOfWeek: Int): Int {
        return settingId * 100 + dayOfWeek
    }
}