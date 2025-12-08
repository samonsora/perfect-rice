package com.example.team1application

/**
 * アナログ時計の針の角度を計算するユーティリティクラス。
 * 角度は 12時（または 0秒/分）の位置を 0度とし、時計回りに増加する。
 * @constructor 新しいClockAngleインスタンスを作成します。
 */
class ClockAngle {

    /**
     * 時、分、秒から時計の針の角度（度）を計算する。
     * @param h 時 (0-23)
     * @param m 分 (0-59)
     * @param s 秒 (0-59)
     * @return Triple<時針の角度, 分針の角度, 秒針の角度>
     */
    fun calculateHandAngles(h: Int, m: Int, s: Int): Triple<Float, Float, Float> {
        // --- 各針の移動速度 ---
        // 秒針: 6度/秒
        val secondAngle = s * 6f

        // 分針の角度: 6度/分 + 秒の影響
        val minuteAngle = (m + s / 60f) * 6f

        // 時針の角度: 30度/時 + 分・秒の影響 (h % 12 で 0-11 に正規化)
        val hourAngle = ((h % 12) + m / 60f + s / 3600f) * 30f

        // 12時/0分/0秒の位置を0度、時計回りを正とする角度
        return Triple(hourAngle, minuteAngle, secondAngle)
    }
}