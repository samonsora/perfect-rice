package com.example.team1application

// AlarmScheduler.kt または Activity/Fragment内のメソッド
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.Toast

fun scheduleOneTimeAlarm(context: Context, delayInSeconds: Long, message: String) {
    // 1. AlarmManagerのインスタンスを取得
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 2. 実行したい処理（BroadcastReceiver）を定義したIntentを作成
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("EXTRA_MESSAGE", message)
    }

    // 3. PendingIntentを作成 (アラームがトリガーされたときにシステムが実行するIntent)
    // リクエストコード: 0 (複数のアラームを管理する場合はユニークな値を設定)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        // API 31 (S) 以降では IMUTTABLE または MUTABLE のフラグが必須
        // FLAG_IMMUTABLE が推奨されます。
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // 4. トリガー時刻の計算
    // SystemClock.elapsedRealtime(): デバイス起動からの経過時間 (ELAPSED_REALTIMEの基準)
    val triggerTime = SystemClock.elapsedRealtime() + delayInSeconds * 1000L

    // 5. アラームの設定
    // ELAPSED_REALTIME_WAKEUP: 起動からの経過時間で設定し、スリープ解除する
    alarmManager.set(
        AlarmManager.ELAPSED_REALTIME_WAKEUP,
        triggerTime,
        pendingIntent
    )

    Toast.makeText(context, "${delayInSeconds}秒後にアラームを設定しました", Toast.LENGTH_SHORT).show()
}

fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 設定時と同じIntentとリクエストコードでPendingIntentを再作成する
    val intent = Intent(context, AlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0, // 設定時と同じリクエストコード
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )

    alarmManager.cancel(pendingIntent)
    Toast.makeText(context, "アラームをキャンセルしました", Toast.LENGTH_SHORT).show()
}