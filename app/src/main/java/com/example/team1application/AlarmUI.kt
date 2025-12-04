package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width // 💡 追加: Spacerでwidthを使うため
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons // 💡 追加: Icons.Filled.Deleteを使うため
import androidx.compose.material.icons.filled.Delete // 💡 追加: ゴミ箱アイコンを使うため
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon // 💡 追加: Iconコンポーネントを使うため
import androidx.compose.material3.IconButton // 💡 追加: IconButtonコンポーネントを使うため
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

// 💡 AlarmSetting データクラスは AlarmData.kt に移動しました。


// --- Composable関数 ---

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    // 1. AndroidのContextを取得 (ファイルI/Oに必要)
    val context = LocalContext.current

    // 2. 初期データをファイルから読み込む
    val initialAlarms = remember {
        // データを読み込み、MutableStateListに変換してステートとして保持する
        AlarmDataStore.loadAlarms(context).toMutableStateList()
    }
    val allAlarms = remember { initialAlarms }


    var showOnlyActive by remember { mutableStateOf(false) }
    // 💡 追加: アラーム設定画面（ダイアログ）の表示状態
    var showSetupDialog by remember { mutableStateOf(false) }

    // 3. アラームの状態を更新し、即座にファイルに保存する関数
    val onToggleActive: (Int, Boolean) -> Unit = { alarmId, newState ->
        val index = allAlarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            // ステートリスト内のオブジェクトを更新 (UIの再描画をトリガー)
            val oldAlarm = allAlarms[index]
            allAlarms[index] = oldAlarm.copy(isActive = newState)

            // 💡 変更をファイルに永続化 (保存) する
            AlarmDataStore.saveAlarms(context, allAlarms) // 保存処理を再度有効化
        }
    }

    // アラームを削除する関数
    val onDeleteAlarm: (Int) -> Unit = { alarmId ->
        // AlarmData.kt で定義された削除＆保存ロジックを呼び出す
        deleteAlarmAndSave(context, allAlarms, alarmId)
    }

    // 💡 新しいアラーム設定が完了したときに呼び出される関数
    val onNewAlarmSet: (String) -> Unit = { selectedTime ->
        // AlarmData.kt で定義された追加＆保存ロジックを呼び出す
        addNewAlarmAndSave(context, allAlarms, selectedTime)
        showSetupDialog = false // ダイアログを閉じる
        println("新しいアラーム設定が追加され、保存されました: $selectedTime")
    }

    // フィルタリングロジック
    val filteredAlarms = remember(showOnlyActive, allAlarms) {
        if (showOnlyActive) {
            allAlarms.filter { it.isActive }
        } else {
            allAlarms
        }
    }

    // 💡 アラーム設定ダイアログの表示
    if (showSetupDialog) {
        // AlarmSet.kt で定義する Composable を呼び出す
        AlarmSetupDialog(
            onDismiss = { showSetupDialog = false },
            onSave = onNewAlarmSet
        )
    }

    // アラーム設定画面のレイアウト
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... (フィルタリングUI) ...
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

        // 1. 設定済みアラームリスト
        Text(
            text = "現在のアラーム設定 (${filteredAlarms.size}件)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAlarms, key = { it.id }) { alarm ->
                AlarmSettingCard(
                    alarm = alarm,
                    onToggleActive = onToggleActive,
                    // 🚨 修正: onDeleteAlarm を渡す
                    onDeleteAlarm = onDeleteAlarm,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 設定を追加するボタン
        Button(
            onClick = {
                // 💡 変更: ダイアログ表示のステートを true にする
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
// --- アラーム設定カードコンポーネント (レイアウト修正) ---

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
            // 💡 修正: 左側の情報 (時刻と曜日) の表示を戻す
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
                // 💡 削除ボタン
                IconButton(onClick = { onDeleteAlarm(alarm.id) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "アラームを削除",
                        tint = MaterialTheme.colorScheme.error // 削除ボタンはエラー色に
                    )
                }
                Spacer(modifier = Modifier.width(8.dp)) // スペーサーで間隔調整

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