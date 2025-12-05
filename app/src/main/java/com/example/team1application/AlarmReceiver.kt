package com.example.team1application

// AlarmReceiver.kt
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    // アラームがトリガーされた際にこのメソッドが呼ばれる
    override fun onReceive(context: Context, intent: Intent) {
        // **警告:** onReceive内では長時間かかる処理（ネットワークアクセスなど）は実行しないでください。
        // 代わりに、WorkManagerやForeground Serviceを開始してください。

        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "メッセージなし"

        Log.d("AlarmManager", "🎉 アラームがトリガーされました！メッセージ: $message")

        // ユーザーへ通知するためにToastを表示 (デモンストレーション用)
        Toast.makeText(context, "⏰ アラーム実行: $message", Toast.LENGTH_LONG).show()

        // ここでWorkManagerのOneTimeWorkRequestなどをスケジュールするのが現代のベストプラクティスです。
    }
}