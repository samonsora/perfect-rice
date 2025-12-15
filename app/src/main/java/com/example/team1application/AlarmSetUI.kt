package com.example.team1application

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

/**
 * 詳細なアラーム設定画面の全体レイアウトを構築するComposable。
 *
 * @param onDismiss 「キャンセル」または「戻る」ボタンを押したときの処理
 * @param onSave 「完了」ボタンを押したときの処理 (この画面で設定を保存する)
 * @param onDelete 「削除」ボタンを押したときの処理
 * @param initialTime AlarmSetupDialogから渡される初期時刻 (例: "07:30")
 * @param isNewAlarm 新規作成モードかどうか
 */
@Composable
fun AlarmSetUI(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    initialTime: String,
    isNewAlarm: Boolean
) {
    // 1. 画面で設定する時刻の状態を保持
    var alarmTime by remember { mutableStateOf(initialTime) }

    // 2. 時刻設定ダイアログの表示状態
    var showTimeDialog by remember { mutableStateOf(false) }

    // 3. スヌーズの設定状態を保持
    val snoozeIntervalOptions = listOf("なし", "5分", "10分", "15分", "30分", "手動入力")
    val snoozeCountOptions = listOf("無制限", "1回", "2回", "3回", "4回", "5回")

    var snoozeInterval by remember { mutableStateOf(snoozeIntervalOptions[0]) } // 初期値: なし
    var snoozeCount by remember { mutableStateOf(snoozeCountOptions[0]) }

    // 4. 音量の設定状態を保持 (0.0f ~ 1.0f)
    var alarmVolume by remember { mutableFloatStateOf(0.5f) } // 初期値: 50%

    // 5. 音量設定ダイアログの表示状態 (新規追加)
    var showVolumeDialog by remember { mutableStateOf(false) } // 初期値: 非表示

    // カラーテーマの取得
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            // TopAppBar: 「< アラームの設定」
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 戻るボタン
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "アラームの設定",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        bottomBar = {
            // BottomBar: 削除、キャンセル、完了ボタン
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 削除ボタン (isNewAlarm が false、つまり既存の編集時のみ表示)
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

                // キャンセルボタン
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

                // 完了ボタン
                Button(
                    onClick = onSave,
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
        // 設定項目のリスト表示
        AlarmSettingListContent(
            modifier = Modifier.padding(paddingValues),
            currentTime = alarmTime,
            onTimeClick = { showTimeDialog = true },
            // スヌーズ関連
            snoozeInterval = snoozeInterval,
            onSnoozeIntervalChange = { newInterval ->
                snoozeInterval = newInterval
                if (newInterval == "なし") {
                    snoozeCount = snoozeCountOptions[0]
                }
            },
            snoozeCount = snoozeCount,
            onSnoozeCountChange = { newCount -> snoozeCount = newCount },
            snoozeIntervalOptions = snoozeIntervalOptions,
            snoozeCountOptions = snoozeCountOptions,
            // 音量関連
            alarmVolume = alarmVolume,
            onVolumeClick = { showVolumeDialog = true } // <--- 新規追加
        )
    }

    // 時刻設定ダイアログ
    if (showTimeDialog) {
        AlarmSetupDialog(
            onDismiss = { showTimeDialog = false },
            onSave = { newTime ->
                alarmTime = newTime
                showTimeDialog = false
            }
        )
    }

    // 5. showVolumeDialog が true の場合、AlarmVolumeDialog を表示 (新規追加)
    if (showVolumeDialog) {
        AlarmVolumeDialog(
            initialVolume = alarmVolume,
            onDismiss = { showVolumeDialog = false },
            onSave = { newVolume ->
                alarmVolume = newVolume // 新しい音量で状態を更新
                showVolumeDialog = false // ダイアログを閉じる
            }
        )
    }
}

/**
 * 設定項目のリストを構成するComposable。
 * 音量設定の引数を追加。
 */
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
    // 音量設定用
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

        // --- 基本設定 ---
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

        // --- アラーム名 ---
        header("アラーム名")
        item { AlarmSimpleSettingItem("アラーム名", "指定なし") }

        // --- アラーム音の設定 ---
        header("アラーム音の設定")
        item { AlarmSimpleSettingItem("アラーム音", "デフォルト") }
        // 音量設定（クリック可能な項目に変更）
        item {
            AlarmVolumeSettingItem(
                volume = alarmVolume,
                onClick = onVolumeClick // <--- クリック時にダイアログ表示を要求
            )
        }
        item { AlarmSwitchSettingItem("フェードイン", "音量を徐々に大きくする") }
    }
}

/**
 * 汎用的な設定項目行。
 */
@Composable
fun AlarmSimpleSettingItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * プルダウンメニュー付きの設定項目行。
 */
@Composable
fun AlarmDropdownSettingItem(
    title: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val menuWidthModifier = Modifier.width(200.dp)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = { expanded = true })
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )

            Column(
                modifier = Modifier.widthIn(min = 80.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = selectedValue,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = menuWidthModifier
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}


/**
 * 時刻設定項目。
 */
@Composable
fun AlarmTimeSettingItem(
    time: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "時刻",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = time,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * 音量設定項目 (クリック可能になり、現在の音量を表示)。
 * クリック時にダイアログを表示するトリガーとして機能します。
 *
 * @param volume 現在の音量 (0.0f ~ 1.0f)
 * @param onClick クリック時のコールバック
 */
@Composable
fun AlarmVolumeSettingItem(
    volume: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick) // <--- クリック可能に
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "最大音量",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        // 現在の音量を表示
        Text(
            text = "${(volume * 100).roundToInt()}%",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/**
 * スイッチ付きの設定項目。
 */
@Composable
fun AlarmSwitchSettingItem(
    title: String,
    subtitle: String,
) {
    var checked by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}


/**
 * 音量設定用のカスタムダイアログ。
 * 画面中央に表示され、スライダーで音量を設定します。
 */
@Composable
fun AlarmVolumeDialog(
    initialVolume: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    // ダイアログ内の音量の状態 (一時的な編集用)
    var currentVolume by remember { mutableFloatStateOf(initialVolume) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // タイトル
                Text(
                    text = "最大音量を設定",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 現在の音量値の表示 (スライダーの上)
                Text(
                    text = "${(currentVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 音量スライダー
                Slider(
                    value = currentVolume,
                    onValueChange = { currentVolume = it },
                    valueRange = 0f..1f, // 0%から100%
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(24.dp))

                // ボタン (キャンセルと完了)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(currentVolume) }) {
                        Text("完了")
                    }
                }
            }
        }
    }
}