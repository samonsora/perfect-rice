package com.example.team1application

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

// AlarmSetting データクラス、AlarmDataStore、deleteAlarmAndSave などのデータ操作関数は外部ファイルにある前提。

// --- Composable関数 ---

/**
 * アラームリスト、フィルタリング、新規作成ボタンを含むメイン画面。
 */
@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    // 1. AndroidのContextを取得 (ファイルI/Oに必要)
    val context = LocalContext.current

    // 2. 初期データをファイルから読み込み、可変なリストとして保持
    val initialAlarms = remember {
        AlarmDataStore.loadAlarms(context).toMutableStateList()
    }
    val allAlarms = initialAlarms

    // UIステート
    var showOnlyActive by remember { mutableStateOf(false) } // アクティブなアラームのみ表示するかどうか
    var showSetupDialog by remember { mutableStateOf(false) } // アラーム時刻設定ダイアログの表示状態
    var timeForDetailSetup by remember { mutableStateOf<String?>(null) } // 詳細設定画面 (AlarmSetUI) に渡す時刻。nullでない場合に画面を表示。


    // 3. アラームの状態を更新し、即座にファイルに保存する関数
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
    // 役割: 時刻設定ダイアログを閉じ、詳細設定画面への遷移をトリガーする
    val onTimeSelected: (String) -> Unit = { selectedTime ->
        showSetupDialog = false // 時刻設定ダイアログを閉じる
        timeForDetailSetup = selectedTime // 詳細設定画面の表示をトリガー
    }

    // AlarmSetUIでの「完了」時に呼ばれる関数
    // 役割: 詳細設定画面で最終的な保存処理を行い、画面を閉じる
    val onNewAlarmSet: (String) -> Unit = { selectedTime ->
        // AlarmData.kt で定義された追加＆保存ロジックを呼び出す
        addNewAlarmAndSave(context, allAlarms, selectedTime)
        timeForDetailSetup = null // 詳細設定画面を閉じる
    }

    // フィルタリングロジック
    val filteredAlarms = if (showOnlyActive) {
        allAlarms.filter { it.isActive }
    } else {
        allAlarms
    }

    // --- 画面表示の条件分岐 ---

    // 優先度1: 時刻設定後に詳細設定画面（AlarmSetUI）を表示
    val selectedTime = timeForDetailSetup
    if (selectedTime != null) {
        // AlarmSetUI を表示
        AlarmSetUI(
            initialTime = selectedTime,
            onDismiss = { timeForDetailSetup = null }, // キャンセルでリストに戻る
            onSave = {
                // onSaveは引数なしのため、内部で selectedTime を使用して保存処理を呼び出す
                onNewAlarmSet(selectedTime)
            },
            // 新規作成時なので削除はキャンセルの動きを代用
            onDelete = { timeForDetailSetup = null }
        )
        // AlarmSetUIが全画面表示を想定しているため、リスト表示を中断
        return
    }

    // 優先度2: アラーム時刻設定ダイアログの表示
    if (showSetupDialog) {
        AlarmSetupDialog(
            onDismiss = { showSetupDialog = false },
            onSave = onTimeSelected // 時刻が確定したら onTimeSelected を呼び出す
        )
    }

    // 優先度3: アラームリスト画面のレイアウト
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... (フィルタリングスイッチの Row)
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 設定を追加するボタン
        Button(
            onClick = {
                showSetupDialog = true // 時刻設定ダイアログ表示のステートを true にする
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "➕ 新しいアラームを設定")
        }
    }
}

// --- アラーム設定カードコンポーネント ---

/**
 * 個々のアラーム設定を表示するカードUIコンポーネント。
 */
@Composable
fun AlarmSettingCard(
    alarm: AlarmSetting,
    onToggleActive: (Int, Boolean) -> Unit,
    onDeleteAlarm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側の情報 (時刻と曜日)
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

            // 右側の操作ボタンとスイッチ
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 削除ボタン
                IconButton(onClick = { onDeleteAlarm(alarm.id) }) {
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