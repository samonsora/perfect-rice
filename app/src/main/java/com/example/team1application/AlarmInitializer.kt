package com.example.team1application

import android.content.Context
import android.util.Log

class AlarmInitializer(context: Context) {

    private val scheduler = AlarmScheduler(context)

    fun initializeAlarms() {
        Log.i("AlarmInitializer", "🚀 初期化プロセス開始")
        try {
            scheduler.syncWithStorage()
            Log.i("AlarmInitializer", "✅ 初期同期完了")
        } catch (e: Exception) {
            Log.e("AlarmInitializer", "❌ 初期化失敗: ${e.message}")
        }
    }
}