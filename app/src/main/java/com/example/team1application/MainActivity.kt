package com.example.team1application

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.team1application.ui.theme.Team1ApplicationTheme



class MainActivity : ComponentActivity() {
    private lateinit var alarmInitializer: AlarmInitializer

    // 🔔 通知許可をリクエストするための魔法のランチャー
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 許可されたら、すぐに次のチェック（アラームなど）に進むためにonResumeのような処理をしてもいいけど
            // 基本的には何もしなくても、次の起動やonResumeで自然にチェックされるよ
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. AlarmInitializerをインスタンス化
        alarmInitializer = AlarmInitializer(applicationContext)
        // 2. 本機能の初期設定を実行
        alarmInitializer.initializeAlarms()

        // 🔔 アプリ起動時に、通知許可が必要なら聞く（Android 13以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var isTitle by remember { mutableStateOf(true) }

                    // ✨✨ 画面切り替えのアニメーション ✨✨
                    Crossfade(
                        targetState = isTitle,
                        label = "画面切り替え",
                        animationSpec = tween(durationMillis = 700)
                    ) { isShowingTitle ->
                        if (isShowingTitle) {
                            TitleScreen(
                                onTap = { isTitle = false },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            HomeScreen(
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    // 🚨 ここに全ての権限チェックを集約！ 🚨
    // 画面が表示されるたびに、上から順番に「まだ足りない権限はないかな？」って確認するよ
    override fun onResume() {
        super.onResume()
        checkAllPermissionsSequence()
    }

    /**
     * 権限を順番にチェックするメソッド。
     * 同時に複数の設定画面を開かないように、if-else if で繋いでいるのがポイント！
     */
    private fun checkAllPermissionsSequence() {
        // 1. まずは正確なアラーム（Android 12以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                // 許可がない場合、アラーム設定画面へGo！
                // ここで return することで、下の処理（使用状況チェック）は一時停止するよ
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        // 2. アラームがOKなら、次は使用状況アクセス権限
        if (!isUsageAccessGranted(this)) {
            // 許可がない場合、使用状況設定画面へGo！
            requestUsageAccess(this)
            return
        }
    }
}