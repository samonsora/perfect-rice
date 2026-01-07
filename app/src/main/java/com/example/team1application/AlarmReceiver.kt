package com.example.team1application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat


class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Intent から ID と現在のスヌーズ回数を受け取る
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val currentSnoozeCount = intent.getIntExtra("CURRENT_SNOOZE_COUNT", 0)

        val allAlarms = AlarmDataStore.loadAlarms(context)
        val setting = allAlarms.find { it.id == alarmId }
        val typeString = setting?.type?.name ?: ""

        Log.d("AlarmReceiver", "⏰ ID:$alarmId (スヌーズ回数:$currentSnoozeCount) のアラームを受信しました")

        // 2. AlarmService を起動する Intent を作成
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            // 💡 サービスへ ID と回数をバトンタッチする
            putExtra("ALARM_ID", alarmId)
            putExtra("CURRENT_SNOOZE_COUNT", currentSnoozeCount)
            putExtra("ALARM_TYPE", typeString)
        }

        // 3. サービスの開始
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "サービスの開始に失敗しました: ${e.message}")
        }
    }
}
/*
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val serviceIntent = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
        /*
        * アプリ画面表示中はできる
        * しかし、スリープやスマホのホーム画面などの様に
        * アプリ画面非出力の場合はできないっぽい。onCreateオーバーライドしてるのに。。。
        * アプリを落とした場合なんかもってのほか
         */

        
        Log.d("AlarmReceiver", "⏰ ID:$alarmId のアラームがトリガーされました！")


        // 注意: 長時間かかる処理はWorkManagerやForeground Serviceを開始して実行してください。
        /*
        * 2分単位
        * アプリ起動画面(メイン画面では実行可能)
        * アプリを起動したまま、スマホのホーム画面では実行可能
        * アプリを起動してアラームをセット、スワイプからアプリを落としたら実行可能
        * アプロのスリープ画面では、実行可能
         */
    }
}*/