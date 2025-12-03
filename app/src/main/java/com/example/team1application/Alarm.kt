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
fun getDummyAlarmSettings(): SnapshotStateList<AlarmSetting> {
    // 💡 必須の修正点: toMutableStateList() を使用して、リスト全体を監視対象の状態にする
    return listOf(
        AlarmSetting(1, "06:30", "月, 火, 水, 木, 金", true),
        AlarmSetting(2, "07:00", "土, 日", false),
        AlarmSetting(3, "08:00", "毎日", true)
    ).toMutableStateList() // <-- これが非常に重要です
}

// --- Composable関数 ---

@Composable
fun AlarmScreen(
    // 💡 親(HomeScreen)からデータをもらう形に変更！
    alarms: List<AlarmSetting>,
    onToggleActive: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 以前の機能（フィルタリング）はそのまま残すよ！✨
    var showOnlyActive by remember { mutableStateOf(false) }

    val filteredAlarms = if (showOnlyActive) {
        alarms.filter { it.isActive }
    } else {
        alarms
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // スイッチ部分（そのまま）
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

        Text(
            text = "現在のアラーム設定 (${filteredAlarms.size}件)",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // リスト表示（そのまま）
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ボタン（そのまま）
        Button(
            onClick = { println("新しいアラーム設定") },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(text = "➕ 新しいアラームを設定")
        }
    }
}

// カード部分は変更なし！（そのまま使ってね）
@Composable
fun AlarmSettingCard(
    alarm: AlarmSetting,
    onToggleActive: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = alarm.time, style = MaterialTheme.typography.displaySmall)
                Text(text = alarm.days, style = MaterialTheme.typography.bodyMedium)
            }
            Switch(
                checked = alarm.isActive,
                onCheckedChange = { onToggleActive(alarm.id, it) }
            )
        }
    }
}