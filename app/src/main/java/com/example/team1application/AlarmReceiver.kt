package com.example.team1application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        Log.d("AlarmReceiver", "⏰ ID:$alarmId のアラームがトリガーされました！")

        // ここにアラームトリガー時の処理を記述します。
        Toast.makeText(context, "【アラーム】ID: $alarmId - 時間です！", Toast.LENGTH_LONG).show()

        // 注意: 長時間かかる処理はWorkManagerやForeground Serviceを開始して実行してください。
        /*
        * 2分単位
        * アプリ起動画面(メイン画面では実行可能)
        * アプリを起動したまま、スマホのホーム画面では実行可能
        * アプリを起動してアラームをセット、スワイプからアプリを落としたら実行可能
        * アプロのスリープ画面では、実行可能
         */
    }
}