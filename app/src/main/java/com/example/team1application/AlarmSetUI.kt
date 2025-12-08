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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 詳細なアラーム設定画面の全体レイアウトを構築するComposable。
 *
 * @param onDismiss 「キャンセル」または「戻る」ボタンを押したときの処理
 * @param onSave 「完了」ボタンを押したときの処理 (この画面で設定を保存する)
 * @param onDelete 「削除」ボタンを押したときの処理
 * @param initialTime AlarmSetupDialogから渡される初期時刻 (例: "07:30")
 */
@Composable
fun AlarmSetUI(
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    initialTime: String
) {
    // 1. 画面で設定する時刻の状態を保持
    var alarmTime by remember { mutableStateOf(initialTime) }

    // 2. 時刻設定ダイアログの表示状態
    var showTimeDialog by remember { mutableStateOf(false) }

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
                    .background(MaterialTheme.colorScheme.surface) // Surfaceカラーを使用
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
                    .background(MaterialTheme.colorScheme.surface) // Surfaceカラーを使用
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 削除ボタン
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("削除", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.width(8.dp))
                // キャンセルボタン
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("キャンセル", color = onSurfaceColor)
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
        containerColor = MaterialTheme.colorScheme.background // 画面全体の背景色
    ) { paddingValues ->
        // 設定項目のリスト表示
        AlarmSettingListContent(
            modifier = Modifier.padding(paddingValues),
            currentTime = alarmTime, // 更新された時刻を渡す
            onTimeClick = {
                // 3. 時刻クリック時にダイアログ表示を要求
                showTimeDialog = true
            }
        )
    }

    // 4. showTimeDialog が true の場合、AlarmSetupDialog を表示
    if (showTimeDialog) {
        AlarmSetupDialog(
            onDismiss = { showTimeDialog = false },
            onSave = { newTime ->
                alarmTime = newTime // 新しい時刻で状態を更新
                showTimeDialog = false // ダイアログを閉じる
            }
        )
    }
}

/**
 * 設定項目のリストを構成するComposable (Scaffoldのコンテンツ部分)。
 */
@Composable
fun AlarmSettingListContent(
    modifier: Modifier = Modifier,
    currentTime: String, // 表示する時刻
    onTimeClick: () -> Unit // 時刻クリック時の処理
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // ヘッダー表示用のローカル関数
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
        item { AlarmTimeSettingItem(time = currentTime, onClick = onTimeClick) } // 時刻設定 (クリックで時刻ダイアログへ)
        item { AlarmSimpleSettingItem("繰り返し", "繰り返さない") }
        item { AlarmSimpleSettingItem("指定日の除外", "指定日を除外しない") }

        // --- アラーム名 ---
        header("アラーム名")
        item { AlarmSimpleSettingItem("アラーム名", "指定なし") }

        // --- アラーム音の設定 ---
        header("アラーム音の設定")
        item { AlarmSimpleSettingItem("アラーム音", "デフォルト") }
        item { AlarmVolumeSettingItem(volume = "50%") } // 音量
        item { AlarmSwitchSettingItem("フェードイン", "音量を徐々に大きくする") } // スイッチ付き
    }
}

/**
 * 汎用的な設定項目行（タイトルとサブタイトルのみ）。
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
    // 区切り線
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

/**
 * 時刻設定項目 (時刻を大きく表示し、クリック可能)。
 */
@Composable
fun AlarmTimeSettingItem(
    time: String,
    onClick: () -> Unit = {} // クリック処理
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick) // クリック可能
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
                fontSize = 32.sp, // 時刻を大きく表示
                fontWeight = FontWeight.Bold
            )
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

/**
 * 音量設定項目 (音量表示のみ、スライダーは省略)。
 */
@Composable
fun AlarmVolumeSettingItem(volume: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "音量",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        // 音量スライダーの代用テキスト
        Text(
            text = volume,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}

/**
 * スイッチ付きの設定項目。
 */
@Composable
fun AlarmSwitchSettingItem(
    title: String,
    subtitle: String,
) {
    var checked by remember { mutableStateOf(false) } // ダミーのステート
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
    Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
}