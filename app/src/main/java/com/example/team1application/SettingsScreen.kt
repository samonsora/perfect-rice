package com.example.team1application

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.example.team1application.ui.theme.Team1ApplicationTheme


@Composable
fun SettingsScreen(onBack: () -> Unit) {

    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    // ---------- 文字サイズ ----------
    val savedFontSize by repo.fontSizeFlow.collectAsState(initial = 20f)
    var previewFontSize by remember { mutableStateOf(savedFontSize) }

    LaunchedEffect(savedFontSize) {
        previewFontSize = savedFontSize
    }

    // ---------- ダークモード ----------
    val savedDarkMode by repo.darkModeFlow.collectAsState(initial = false)
    var darkMode by remember { mutableStateOf(savedDarkMode) }

    LaunchedEffect(savedDarkMode) {
        darkMode = savedDarkMode
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text("設定", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        // ▼ 文字サイズプレビュー
        Text("文字サイズプレビュー", fontSize = previewFontSize.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Text("文字サイズ：${previewFontSize.toInt()}")

        Slider(
            value = previewFontSize,
            onValueChange = { previewFontSize = it },
            valueRange = 12f..40f
        )

        Spacer(modifier = Modifier.height(30.dp))

        // ▼ ダークモードスイッチ（追加）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("ダークモード")
            Switch(
                checked = darkMode,
                onCheckedChange = { enabled ->
                    darkMode = enabled
                    scope.launch { repo.saveDarkMode(enabled) }
                }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 文字サイズ保存
        Button(
            onClick = {
                scope.launch { repo.saveFontSize(previewFontSize) }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 戻る
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("戻る")
        }

    }
}


@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    Team1ApplicationTheme {
        SettingsScreen(onBack = {})
    }
}

