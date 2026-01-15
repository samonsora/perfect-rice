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
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var alarmInitializer: AlarmInitializer

    // 🔔 通知許可リクエスト用ランチャー
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- 1. アラームの復元と初期設定 ---
        alarmInitializer = AlarmInitializer(applicationContext)
        alarmInitializer.initializeAlarms()

        // --- 2. 鳴動中チェック：起動時に確認 ---
        checkRingingAndNavigate()

        // --- 3. 通知許可の確認 (Android 13以上) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // --- 就寝前アプリ使用チェックの予約 ---
        setupUsageCheckWorker()

        // --- 4. メインUIの構築 ---
        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var isTitle by remember { mutableStateOf(true) }

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

    // --- Workerの定期実行を登録するメソッド ---
    private fun setupUsageCheckWorker() {
        val workRequest = PeriodicWorkRequestBuilder<UsageCheckWorker>(
            15, TimeUnit.MINUTES // システム制限上の最小間隔
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "UsageCheckWork",
            ExistingPeriodicWorkPolicy.KEEP, // すでに予約済みなら何もしない（二重登録防止）
            workRequest
        )
    }

    override fun onResume() {
        super.onResume()
        // アプリがバックグラウンドから戻った時にもチェック
        checkRingingAndNavigate()
        checkAllPermissionsSequence()
    }

    /**
     * 共有メモリから鳴動中の情報を取得して遷移
     */
    private fun checkRingingAndNavigate() {
        val prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE)
        val isRinging = prefs.getBoolean("is_ringing", false)
        val alarmId = prefs.getInt("ringing_alarm_id", -1)

        // isRinging が true かつ alarmId が有効な時だけ遷移
        if (isRinging && alarmId != -1) {
            val alarmIntent = Intent(this, AlarmActivity::class.java).apply {
                // SINGLE_TOP を活用し、二重起動を防ぐ
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("ALARM_ID", alarmId)
                putExtra("ALARM_TYPE", prefs.getString("ringing_alarm_type", ""))
                putExtra("CURRENT_SNOOZE_COUNT", prefs.getInt("ringing_snooze_count", 0))
            }
            startActivity(alarmIntent)
        }
    }

    private fun checkAllPermissionsSequence() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        if (!isUsageAccessGranted(this)) {
            requestUsageAccess(this)
            return
        }
    }
}