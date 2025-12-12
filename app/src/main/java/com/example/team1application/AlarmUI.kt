package com.example.team1application

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// AlarmSetting データクラス、AlarmDataStore、deleteAlarmAndSave、updateAlarmAndSave
// (更新関数は仮定) などのデータ操作関数は外部ファイルにある前提。

// --- Composable関数 ---

/**
 * アラームリスト、フィルタリング、新規作成ボタンを含むメイン画面。
 */
@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val initialAlarms = remember {
        AlarmDataStore.loadAlarms(context).toMutableStateList()
    }
    val allAlarms = initialAlarms

    // UIステート
    var showOnlyActive by remember { mutableStateOf(false) }
    var showSetupDialog by remember { mutableStateOf(false) }

    // 💡 修正点 1: 新規作成/編集のために使用する状態
    // 新規作成の場合: timeForDetailSetup (String?) のみが設定される
    // 編集の場合: editingAlarmId (Int?) が設定される
    var timeForDetailSetup by remember { mutableStateOf<String?>(null) } // 新規作成時の時刻
    var editingAlarmId by remember { mutableStateOf<Int?>(null) } // 編集中のアラームID

    // --- データ操作関数 (AlarmDataStoreに存在すると仮定) ---

    // 既存アラームを更新し、ファイルに保存する関数
    val onUpdateAlarm: (Int, String) -> Unit = { alarmId, newTime ->
        val index = allAlarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val oldAlarm = allAlarms[index]
            // ここでは時刻のみ更新するロジックを仮定
            allAlarms[index] = oldAlarm.copy(time = newTime)
            AlarmDataStore.saveAlarms(context, allAlarms) // ファイルに保存
        }
    }

    // アラームの状態を更新し、即座にファイルに保存する関数
    val onToggleActive: (Int, Boolean) -> Unit = { alarmId, newState ->
        val index = allAlarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val oldAlarm = allAlarms[index]
            allAlarms[index] = oldAlarm.copy(isActive = newState)
            AlarmDataStore.saveAlarms(context, allAlarms) // ファイルに保存
        }
    }

    // アラームを削除する関数
    val onDeleteAlarm: (Int) -> Unit = { alarmId ->
        deleteAlarmAndSave(context, allAlarms, alarmId) // 外部関数を呼び出し
    }

    // 新しいアラーム設定（時刻設定まで）が完了したときに呼び出される関数
    val onTimeSelected: (String) -> Unit = { selectedTime ->
        showSetupDialog = false
        timeForDetailSetup = selectedTime // 新規作成フロー開始
    }

    // AlarmSetUIでの「完了」時に呼ばれる関数 (新規作成の場合)
    val onNewAlarmSet: (String) -> Unit = { selectedTime ->
        addNewAlarmAndSave(context, allAlarms, selectedTime)
        timeForDetailSetup = null // 詳細設定画面を閉じる
    }

    // フィルタリングロジック
    val filteredAlarms = if (showOnlyActive) {
        allAlarms.filter { it.isActive }
    } else {
        allAlarms
    }

    // 💡 修正点 2: 編集対象のアラーム情報を取得
    val editingAlarm = allAlarms.find { it.id == editingAlarmId }

    // --- 画面表示の条件分岐 ---

    // 優先度1: 詳細設定画面（AlarmSetUI）を表示
    if (timeForDetailSetup != null || editingAlarm != null) {
        val isNew = editingAlarm == null
        val initialTime = timeForDetailSetup ?: editingAlarm?.time ?: ""
        val currentId = editingAlarmId // 編集の場合はIDを保持

        AlarmSetUI(
            initialTime = initialTime,
            // 閉じる処理 (新規/編集をリセット)
            onDismiss = {
                timeForDetailSetup = null
                editingAlarmId = null
            },
            // 保存処理 (新規/編集でロジックを切り替え)
            onSave = {
                if (isNew) {
                    onNewAlarmSet(initialTime) // 新規保存
                } else if (currentId != null) {
                    onUpdateAlarm(currentId, initialTime) // 既存更新
                    editingAlarmId = null
                }
            },
            // 削除処理 (編集時のみ有効)
            onDelete = {
                if (currentId != null) {
                    onDeleteAlarm(currentId)
                    editingAlarmId = null
                }
            },
            isNewAlarm = isNew
        )
        return
    }

    // 優先度2: アラーム時刻設定ダイアログの表示
    if (showSetupDialog) {
        AlarmSetupDialog(
            onDismiss = { showSetupDialog = false },
            onSave = onTimeSelected
        )
    }

    // 優先度3: アラームリスト画面のレイアウト
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... (フィルタリングスイッチの Row) ...
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "アクティブなアラームのみ表示",
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = showOnlyActive,
                onCheckedChange = { showOnlyActive = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. 設定済みアラームリストヘッダー
        Text(
            text = "現在のアラーム設定 (${filteredAlarms.size}件)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // リスト表示
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = filteredAlarms, key = { it.id }) { alarm ->
                AlarmSettingCard(
                    alarm = alarm,
                    onToggleActive = onToggleActive,
                    onDeleteAlarm = onDeleteAlarm,
                    // 💡 修正点 3: 編集開始コールバックを追加
                    onEditAlarm = { id -> editingAlarmId = id },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 設定を追加するボタン
        Button(
            onClick = {
                showSetupDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "➕ 新しいアラームを設定")
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
    onEditAlarm: (Int) -> Unit, // 💡 修正点 4: 編集開始コールバック
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 💡 修正点 5: カードのゴミ箱とスイッチを除く部分に clickable を適用
                // ただし、Row全体に clickable を適用し、内部の操作ボタンに干渉しないように制御する
                .clickable { onEditAlarm(alarm.id) }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側の情報 (時刻と曜日) - クリック可能な領域
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = alarm.days,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右側の操作ボタンとスイッチ (clickableの対象外)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 削除ボタン
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

                // アクティブ/非アクティブを切り替えるスイッチ
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
