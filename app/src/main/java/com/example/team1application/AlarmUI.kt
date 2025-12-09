package com.example.team1application

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 1回分のアラーム設定を保持するデータクラス
 */
data class AlarmSetting(
    val id: Int,
    val time: String, // 例: "07:00"
    val days: String, // 例: "月, 火, 水, 木, 金" または "毎日"
    val isActive: Boolean
)

//テスト用ダミーデータ

// --- Composable関数 ---

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    // 💡 改善点: アラームリストを mutableStateListOf で保持し、変更を検知可能にする
    // 実際は親コンポーネント/ViewModelから渡されるべきステートです
    val allAlarms = remember { getDummyAlarmSettings() }

    // アクティブなアラームのみを表示するかどうかの状態
    var showOnlyActive by remember { mutableStateOf(false) }

    // アラームの状態を更新する関数
    val onToggleActive: (Int, Boolean) -> Unit = { alarmId, newState ->
        val index = allAlarms.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            // リスト内のオブジェクトを変更し、新しいインスタンスで置き換える
            val oldAlarm = allAlarms[index]
            allAlarms[index] = oldAlarm.copy(isActive = newState)
        }
    }

    // フィルタリングロジック
    val filteredAlarms = remember(showOnlyActive, allAlarms) {
        if (showOnlyActive) {
            allAlarms.filter { it.isActive }
        } else {
            allAlarms
        }
    }

    // アラーム設定画面のレイアウト
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... (フィルタリングUIは変更なし) ...
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

        // 1. 設定済みアラームリスト (LazyColumnを使用)
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
                    // 💡 改善点: IDと新しい状態を渡すコールバックを渡す
                    onToggleActive = onToggleActive,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 設定を追加するボタン
        Button(
            onClick = {
                // 実際には新規アラーム追加画面への遷移やリストへの追加処理
                println("新しいアラーム設定がクリックされました")
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

@Composable
fun AlarmSettingCard(
    alarm: AlarmSetting,
    // 💡 改善点: onToggleActiveの引数を (Int, Boolean) -> Unit に変更
    // 呼び出し側では alarm.id と新しい状態を渡す
    onToggleActive: (Int, Boolean) -> Unit,
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
            Column {
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

            // アクティブ/非アクティブを切り替えるスイッチ
            Switch(
                // 💡 改善点: スイッチの状態を親から受け取った状態 (alarm.isActive) にバインド
                checked = alarm.isActive,
                onCheckedChange = { newState ->
                    // 💡 改善点: 親にアラームIDと新しい状態を通知する
                    onToggleActive(alarm.id, newState)
                }
            )
        }
    }
}