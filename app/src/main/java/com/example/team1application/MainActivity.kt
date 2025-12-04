package com.example.team1application

import android.media.MediaPlayer
import android.os.Bundle
import android.view.GestureDetector // ← 動きを見極める魔法
import android.view.MotionEvent
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

class MainActivity : ComponentActivity() {

    // 🎵 音を鳴らすプレイヤー
    private var globalMediaPlayer: MediaPlayer? = null

    // 🦅 動きを見極める使い魔（ジェスチャー検出器）
    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 🎵 音の準備
        try {
            globalMediaPlayer = MediaPlayer.create(this, R.raw.tap_sound)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 🦅 使い魔を召喚！ここで「タップ」と「スクロール」を見分けさせるよ
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            // ✨ 「ポンッとタップして離した時 (SingleTapUp)」だけ反応する！
            // スクロールした時はこれは呼ばれないから、音は鳴らないよ👍
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                playTapSound()
                return true
            }
        })

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

    // ✨✨ 画面へのタッチを監視する場所 ✨✨
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // ここで使い魔に「今の動き見てて！」って渡す
        if (ev != null) {
            gestureDetector.onTouchEvent(ev)
        }

        // 元々のボタン操作なども邪魔しないように返す
        return super.dispatchTouchEvent(ev)
    }

    // 🎵 音を鳴らす関数
    private fun playTapSound() {
        try {
            globalMediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.seekTo(0)
                    player.start()
                } else {
                    player.start()
                }
            }
        } catch (e: Exception) {
            // エラー無視
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        globalMediaPlayer?.release()
        globalMediaPlayer = null
    }
}