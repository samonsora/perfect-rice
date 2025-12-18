package com.example.team1application

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // 起動時にデータを読み込み、SnapshotStateListに変換して状態管理
    val allAlarms = remember {
        AlarmDataStore.loadAlarms(context).toMutableStateList()
    }

    // UIステート
    var showOnlyActive by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }
    var timeForNewAlarm by remember { mutableStateOf<String?>(null) } // 新規作成時の初期時刻
    var editingAlarmId by remember { mutableStateOf<Int?>(null) } // 編集中のアラームID

    // 編集対象のアラームを取得
    val editingAlarm = allAlarms.find { it.id == editingAlarmId }

    // --- 画面表示の条件分岐 ---

    // A. 詳細設定画面 (新規または編集)
    if (timeForNewAlarm != null || editingAlarm != null) {
        val isNew = editingAlarm == null
        // 表示する初期時刻の決定
        val initialTime = timeForNewAlarm ?: editingAlarm?.time ?: "07:00"

        AlarmSetUI(
            initialTime = initialTime,
            isNewAlarm = isNew,
            // 既存データがある場合はその値を渡し、なければデフォルト値を渡す
            existingAlarm = editingAlarm,
            onDismiss = {
                timeForNewAlarm = null
                editingAlarmId = null
            },
            onSave = { updatedSetting ->
                // ロジック側の alarmSave を呼び出し（新規・更新の両方に対応）
                alarmSave(context, allAlarms, updatedSetting)
                timeForNewAlarm = null
                editingAlarmId = null
            },
            onDelete = {
                editingAlarm?.let {
                    deleteAlarmAndSave(context, allAlarms, it.id)
                }
                editingAlarmId = null
            }
        )
        return // 他のUIを描画しない
    }

    // B. 時刻選択ダイアログ (新規作成の第一歩)
    if (showSetupDialog) {
        AlarmSetupDialog(
            onDismiss = { showSetupDialog = false },
            onSave = { selectedTime ->
                showSetupDialog = false
                timeForNewAlarm = selectedTime // これにより AlarmSetUI が表示される
            }
        )
    }

    // C. メインリスト画面
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("アクティブなアラームのみ表示")
            Switch(checked = showOnlyActive, onCheckedChange = { showOnlyActive = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        val filteredAlarms = if (showOnlyActive) allAlarms.filter { it.isActive } else allAlarms

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items = filteredAlarms, key = { it.id }) { alarm ->
                AlarmSettingCard(
                    alarm = alarm,
                    onToggleActive = { id, active ->
                        val index = allAlarms.indexOfFirst { it.id == id }
                        if (index != -1) {
                            allAlarms[index] = allAlarms[index].copy(isActive = active)
                            AlarmDataStore.saveAlarms(context, allAlarms)
                        }
                    },
                    onDeleteAlarm = { id -> deleteAlarmAndSave(context, allAlarms, id) },
                    onEditAlarm = { id -> editingAlarmId = id }
                )
            }
        }

        Button(
            onClick = { showSetupDialog = true },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("➕ 新しいアラームを設定")
        }
    }
}

// --- アラーム設定カードコンポーネントの修正 ---

/**
 * 個々のアラーム設定を表示するカードUIコンポーネント。
 */
@Composable
fun AlarmSettingCard(
    alarm: AlarmSetting,
    onToggleActive: (Int, Boolean) -> Unit,
    onDeleteAlarm: (Int) -> Unit,
    onEditAlarm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditAlarm(alarm.id) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側の情報 (名前・時刻・曜日)
            Column(modifier = Modifier.weight(1f)) {
                // 💡 名前が空でない場合のみ表示
                if (alarm.name.isNotBlank()) {
                    Text(
                        text = alarm.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displaySmall
                )

                // 💡 曜日が空でない場合のみ表示（空なら何も表示しない）
                if (alarm.days.isNotBlank()) {
                    Text(
                        text = alarm.days,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 右側の操作ボタンとスイッチ
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onDeleteAlarm(alarm.id) },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "アラームを削除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = alarm.isActive,
                    onCheckedChange = { newState ->
                        onToggleActive(alarm.id, newState)
                    }
                )
            }
        }
    }
}