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
 */
@Composable
fun AlarmSetUI(
    onDismiss: () -> Unit,
    onSave: (AlarmSetting) -> Unit,
    onDelete: () -> Unit,
    initialTime: String,
    isNewAlarm: Boolean,
    existingAlarm: AlarmSetting? = null
) {
    // --- 状態管理 ---
    var alarmTime by remember { mutableStateOf(initialTime) }
    var alarmName by remember { mutableStateOf(existingAlarm?.name ?: "指定なし") }
    var alarmVolume by remember { mutableFloatStateOf(existingAlarm?.volume ?: 0.5f) }
    var isFadeInEnabled by remember { mutableStateOf(existingAlarm?.fadeIn ?: false) }

    val snoozeIntervalOptions = listOf("なし", "5分", "10分", "15分", "30分")
    val snoozeCountOptions = listOf("無制限", "1回", "2回", "3回", "4回", "5回")
    var snoozeInterval by remember { mutableStateOf(existingAlarm?.snoozeInterval ?: snoozeIntervalOptions[0]) }
    var snoozeCount by remember { mutableStateOf(existingAlarm?.snoozeCount ?: snoozeCountOptions[0]) }

    // ダイアログ表示フラグ
    var showTimeDialog by remember { mutableStateOf(false) }
    var showVolumeDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る", tint = onSurfaceColor)
                }
                Text("アラームの設定", style = MaterialTheme.typography.titleLarge, color = onSurfaceColor)
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp).background(MaterialTheme.colorScheme.surface).padding(16.dp, 8.dp),
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
                    ) { Text("削除") }
                    Spacer(Modifier.width(8.dp))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = onSurfaceColor)
                ) { Text("キャンセル") }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        val result = AlarmSetting(
                            id = existingAlarm?.id ?: -1,
                            time = alarmTime,
                            days = existingAlarm?.days ?: "繰り返さない",
                            isActive = existingAlarm?.isActive ?: true,
                            name = alarmName,
                            snoozeInterval = snoozeInterval,
                            snoozeCount = snoozeCount,
                            volume = alarmVolume,
                            fadeIn = isFadeInEnabled
                        )
                        onSave(result)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) { Text("完了") }
            }
        }
    ) { paddingValues ->
        AlarmSettingListContent(
            modifier = Modifier.padding(paddingValues),
            currentTime = alarmTime,
            onTimeClick = { showTimeDialog = true },
            alarmName = alarmName,
            onNameClick = { showNameDialog = true },
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
            onVolumeClick = { showVolumeDialog = true },
            isFadeInEnabled = isFadeInEnabled, // 💡 パラメーターを渡す
            onFadeInChange = { isFadeInEnabled = it } // 💡 パラメーターを渡す
        )
    }

    if (showTimeDialog) {
        AlarmSetupDialog(onDismiss = { showTimeDialog = false }, onSave = { alarmTime = it; showTimeDialog = false })
    }

    if (showVolumeDialog) {
        AlarmVolumeDialog(initialVolume = alarmVolume, onDismiss = { showVolumeDialog = false }, onSave = { alarmVolume = it; showVolumeDialog = false })
    }

    if (showNameDialog) {
        AlarmNameEditDialog(initialName = alarmName, onDismiss = { showNameDialog = false }, onSave = { alarmName = it; showNameDialog = false })
    }
}

/**
 * 設定項目リスト
 */
@Composable
fun AlarmSettingListContent(
    modifier: Modifier = Modifier,
    currentTime: String,
    onTimeClick: () -> Unit,
    alarmName: String,
    onNameClick: () -> Unit,
    snoozeInterval: String,
    onSnoozeIntervalChange: (String) -> Unit,
    snoozeCount: String,
    onSnoozeCountChange: (String) -> Unit,
    snoozeIntervalOptions: List<String>,
    snoozeCountOptions: List<String>,
    alarmVolume: Float,
    onVolumeClick: () -> Unit,
    isFadeInEnabled: Boolean, // 💡 使用する
    onFadeInChange: (Boolean) -> Unit // 💡 使用する
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        fun header(text: String) = item {
            Text(text = text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp))
        }

        header("基本設定")
        item { AlarmTimeSettingItem(time = currentTime, onClick = onTimeClick) }
        item { AlarmSimpleSettingItem("繰り返し", "繰り返さない") }
        item { AlarmDropdownSettingItem("スヌーズの間隔", snoozeInterval, snoozeIntervalOptions, onSnoozeIntervalChange) }
        if (snoozeInterval != "なし") {
            item { AlarmDropdownSettingItem("スヌーズの回数", snoozeCount, snoozeCountOptions, onSnoozeCountChange) }
        }

        header("アラーム名")
        item { AlarmSimpleSettingItem("アラーム名", alarmName, onClick = onNameClick) }

        header("アラーム音の設定")
        item { AlarmSimpleSettingItem("アラーム音", "デフォルト") }
        item { AlarmVolumeSettingItem(volume = alarmVolume, onClick = onVolumeClick) }
        item {
            // 💡 修正：渡された状態とコールバックを SwitchItem に適用
            AlarmSwitchSettingItem(
                title = "フェードイン",
                subtitle = "音量を徐々に大きくする",
                checked = isFadeInEnabled,
                onCheckedChange = onFadeInChange
            )
        }
    }
}

/**
 * アラーム名編集用ダイアログ
 */
@Composable
fun AlarmNameEditDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(if (initialName == "指定なし") "" else initialName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("アラーム名を入力", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("例: 起床") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(text.ifBlank { "指定なし" }) }) { Text("OK") }
                }
            }
        }
    }
}

// --- 補助的なUIパーツ ---

@Composable
fun AlarmSwitchSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean, // 💡 外部から状態を受け取る
    onCheckedChange: (Boolean) -> Unit // 💡 外部へイベントを通知
) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmSimpleSettingItem(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmDropdownSettingItem(title: String, selectedValue: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).clickable { expanded = true }.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(selectedValue, color = MaterialTheme.colorScheme.primary)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onValueChange(option); expanded = false }) }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmTimeSettingItem(time: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 72.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("時刻", style = MaterialTheme.typography.bodyLarge)
            Text(time, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmVolumeSettingItem(volume: Float, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 56.dp).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("最大音量", style = MaterialTheme.typography.bodyLarge)
        Text("${(volume * 100).roundToInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
}

@Composable
fun AlarmVolumeDialog(initialVolume: Float, onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var currentVolume by remember { mutableFloatStateOf(initialVolume) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("最大音量を設定", style = MaterialTheme.typography.titleLarge)
                Text("${(currentVolume * 100).roundToInt()}%", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                Slider(value = currentVolume, onValueChange = { currentVolume = it })
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("キャンセル") }
                    Button(onClick = { onSave(currentVolume) }) { Text("完了") }
                }
            }
        }
    }
}