package com.example.team1application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

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
}