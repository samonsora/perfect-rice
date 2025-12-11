package com.example.team1application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.team1application.ui.theme.Team1ApplicationTheme
// 権限ヘルパー関数はcom.example.team1applicationパッケージ内にあると仮定


class MainActivity : ComponentActivity() {

    // 💡 onCreate は画面表示の初期化に専念させる
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    var isTitle by remember { mutableStateOf(true) }

                    // ✨✨ ここが「フワッ」とする魔法陣！ ✨✨
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

    // 🚨 修正点 1: 権限チェックメソッドをクラス直下に移動 🚨
    // 画面に戻ってきたときに権限チェックを再実行するため onResume() に配置するのが適切

    override fun onResume() {
        super.onResume()

        // 権限チェックと要求のロジックを実行
        checkUsageAccessPermission()
    }

    /**
     * 使用状況アクセス権限をチェックし、許可されていなければ設定画面へ誘導します。
     * このメソッドは MainActivity クラスの直下に定義する必要があります。
     */
    private fun checkUsageAccessPermission() {
        // 権限チェック
        if (!isUsageAccessGranted(this)) {
            // 権限がなければ設定画面へ誘導
            requestUsageAccess(this)
        }
        // 権限があれば、SleepReminder が Worker をスケジュール済み
    }
}
// 💡 注意: isUsageAccessGranted と requestUsageAccess の定義は、
// このファイルの外側 (同じパッケージの別の .kt ファイル) にある必要があります。