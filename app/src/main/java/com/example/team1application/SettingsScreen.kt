package com.example.team1application

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val repo = remember { FontSizeRepository(context) }

    // 🔹 DataStore から読み込んだ値（保存された文字サイズ）
    val savedFontSize by repo.fontSizeFlow.collectAsState(initial = 20f)

    // 🔹 画面上のプレビュー用
    var previewFontSize by remember { mutableStateOf(savedFontSize) }

    // DataStore から値が更新されたら、プレビューも更新
    LaunchedEffect(savedFontSize) {
        previewFontSize = savedFontSize
    }

    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {

        Text("設定", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        // プレビュー
        Text("文字サイズプレビュー", fontSize = previewFontSize.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Text("文字サイズ：${previewFontSize.toInt()}")

        // スライダー（これは保存しない）
        Slider(
            value = previewFontSize,
            onValueChange = { previewFontSize = it },
            valueRange = 12f..40f
        )

        Spacer(modifier = Modifier.height(30.dp))

        // 🔵 保存ボタン
        Button(
            onClick = {
                scope.launch {
                    repo.saveFontSize(previewFontSize)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 戻るボタン
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("戻る")
        }
    }
}
