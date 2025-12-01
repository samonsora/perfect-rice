package com.example.team1application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas // Canvasを追加
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // sizeを追加
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect // LaunchedEffectを追加
import androidx.compose.runtime.getValue // byを使用するために必要
import androidx.compose.runtime.mutableStateOf // mutableStateOfを追加
import androidx.compose.runtime.remember // rememberを追加
import androidx.compose.runtime.setValue // byを使用するために必要
import androidx.compose.runtime.withFrameMillis // withFrameMillisを追加
import androidx.compose.ui.Alignment // Alignmentを追加
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset // Offsetを追加
import androidx.compose.ui.graphics.Color // Colorを追加
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate // rotateを追加
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.team1application.ui.theme.Team1ApplicationTheme
import kotlinx.coroutines.isActive // LaunchedEffectのループを安全に保つために必要

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Team1ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GreetingContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

