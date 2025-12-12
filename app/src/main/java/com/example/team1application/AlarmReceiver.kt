package com.example.team1application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val i = Intent(context, AlarmActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        /*
        * アプリ画面表示中はできる
        * しかし、スリープやスマホのホーム画面などの様に
        * アプリ画面日出力の場合はできないっぽい。onCreateオーバーライドしてるのに。。。
        * アプリを落とした場合なんかもってのほか
         */
        context.startActivity(i)
        
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