package com.example.team1application

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

/**
 * 詳細なアラーム設定画面
 * @param existingAlarm 編集時は既存のデータを、新規時は null を渡します
 */
@Composable
fun AlarmSetUI(
    onDismiss: () -> Unit,
    onSave: (AlarmSetting) -> Unit,
    onDelete: () -> Unit,
    initialTime: String,
    isNewAlarm: Boolean,
    existingAlarm: AlarmSetting? = null // 💡 追加：未定義エラーを解消
) {
    // 1. 画面で設定する時刻の状態
    var alarmTime by remember { mutableStateOf(initialTime) }
    var showTimeDialog by remember { mutableStateOf(false) }

    // 2. スヌーズの設定状態 (既存データがあればそれを初期値にする)
    val snoozeIntervalOptions = listOf("なし", "5分", "10分", "15分", "30分")
    val snoozeCountOptions = listOf("無制限", "1回", "2回", "3回", "4回", "5回")

    var snoozeInterval by remember {
        mutableStateOf(existingAlarm?.snoozeInterval ?: snoozeIntervalOptions[0])
    }
    var snoozeCount by remember {
        mutableStateOf(existingAlarm?.snoozeCount ?: snoozeCountOptions[0])
    }

    // 3. 音量の設定状態
    var alarmVolume by remember {
        mutableFloatStateOf(existingAlarm?.volume ?: 0.5f)
    }
    var showVolumeDialog by remember { mutableStateOf(false) }

    // カラーテーマの取得 (未定義エラー回避のためここで定義)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = onSurfaceColor
                    )
                }
                Text(
                    text = "アラームの設定",
                    style = MaterialTheme.typography.titleLarge,
                    color = onSurfaceColor
                )
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                if (!isNewAlarm) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("削除")
                    }
                    Spacer(Modifier.width(8.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f).height(48.dp))
                    Spacer(Modifier.width(8.dp))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = onSurfaceColor
                    )
                ) {
                    Text("キャンセル")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        // 💡 修正点：全ての入力値を一つの AlarmSetting としてまとめて親に渡す
                        val result = AlarmSetting(
                            id = existingAlarm?.id ?: -1, // 新規なら -1
                            time = alarmTime,
                            days = existingAlarm?.days ?: "繰り返さない",
                            isActive = existingAlarm?.isActive ?: true,
                            name = existingAlarm?.name ?: "指定なし",
                            snoozeInterval = snoozeInterval,
                            snoozeCount = snoozeCount,
                            volume = alarmVolume,
                            fadeIn = existingAlarm?.fadeIn ?: false
                        )
                        onSave(result)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("完了")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        AlarmSettingListContent(
            modifier = Modifier.padding(paddingValues),
            currentTime = alarmTime,
            onTimeClick = { showTimeDialog = true },
            snoozeInterval = snoozeInterval,
            onSnoozeIntervalChange = { newInterval ->
                snoozeInterval = newInterval
                if (newInterval == "なし") snoozeCount = snoozeCountOptions[0]
            },
            snoozeCount = snoozeCount,
            onSnoozeCountChange = { snoozeCount = it },
            snoozeIntervalOptions = snoozeIntervalOptions,
            snoozeCountOptions = snoozeCountOptions,
            alarmVolume = alarmVolume,
            onVolumeClick = { showVolumeDialog = true }
        )
    }

    if (showTimeDialog) {
        AlarmSetupDialog(
            onDismiss = { showTimeDialog = false },
            onSave = { newTime ->
                alarmTime = newTime
                showTimeDialog = false
            }
        )
    }

    if (showVolumeDialog) {
        AlarmVolumeDialog(
            initialVolume = alarmVolume,
            onDismiss = { showVolumeDialog = false },
            onSave = { newVolume ->
                alarmVolume = newVolume
                showVolumeDialog = false
            }
        )
    }
}

// --- 以下、既存の Composable 群 (変更なし) ---

@Composable
fun AlarmSettingListContent(
    modifier: Modifier = Modifier,
    currentTime: String,
    onTimeClick: () -> Unit,
    snoozeInterval: String,
    onSnoozeIntervalChange: (String) -> Unit,
    snoozeCount: String,
    onSnoozeCountChange: (String) -> Unit,
    snoozeIntervalOptions: List<String>,
    snoozeCountOptions: List<String>,
    alarmVolume: Float,
    onVolumeClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        fun header(text: String) = item {
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )
        }

        header("基本設定")
        item { AlarmTimeSettingItem(time = currentTime, onClick = onTimeClick) }
        item { AlarmSimpleSettingItem("繰り返し", "繰り返さない") }
        item {
            AlarmDropdownSettingItem(
                title = "スヌーズの間隔",
                selectedValue = snoozeInterval,
                options = snoozeIntervalOptions,
                onValueChange = onSnoozeIntervalChange
            )
        }

        if (snoozeInterval != "なし") {
            item {
                AlarmDropdownSettingItem(
                    title = "スヌーズの回数",
                    selectedValue = snoozeCount,
                    options = snoozeCountOptions,
                    onValueChange = onSnoozeCountChange
                )
            }
        }

        header("アラーム名")
        item { AlarmSimpleSettingItem("アラーム名", "指定なし") }

        header("アラーム音の設定")
        item { AlarmSimpleSettingItem("アラーム音", "デフォルト") }
        item {
            AlarmVolumeSettingItem(
                volume = alarmVolume,
                onClick = onVolumeClick
            )
        }
        item { AlarmSwitchSettingItem("フェードイン", "音量を徐々に大きくする") }
    }
}

@Composable
fun AlarmSimpleSettingItem(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AlarmDropdownSettingItem(title: String, selectedValue: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = { expanded = true }).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(text = selectedValue, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false })
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun AlarmTimeSettingItem(time: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "時刻", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(text = time, color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AlarmVolumeSettingItem(volume: Float, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "最大音量", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        Text(text = "${(volume * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AlarmSwitchSettingItem(title: String, subtitle: String) {
    var checked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = { checked = it })
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun AlarmVolumeDialog(initialVolume: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var currentVolume by remember { mutableFloatStateOf(initialVolume) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "最大音量を設定", style = MaterialTheme.typography.titleLarge)
                Text(text = "${(currentVolume * 100).roundToInt()}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Slider(value = currentVolume, onValueChange = { currentVolume = it }, valueRange = 0f..1f)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Button(onClick = { onSave(currentVolume) }) { Text("完了") }
                }
            }
        }
    }
}