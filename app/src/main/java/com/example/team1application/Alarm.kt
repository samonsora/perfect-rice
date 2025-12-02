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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment // Alignment のために必要


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
fun getDummyAlarmSettings(): List<AlarmSetting> {
    return listOf(
        AlarmSetting(1, "06:30", "月, 火, 水, 木, 金", true),
        AlarmSetting(2, "07:00", "土, 日", false),
        AlarmSetting(3, "08:00", "毎日", true)
    )
}

// --- Composable関数 ---

@Composable
fun AlarmScreen(modifier: Modifier = Modifier) {
    // 実際にはViewModelなどから取得しますが、ここではダミーデータを使用
    val allAlarms = getDummyAlarmSettings()

    // 💡 ステートホイスティングの例: アクティブなアラームのみを表示するかどうかの状態
    var showOnlyActive by remember { mutableStateOf(false) }

    // フィルタリングロジック (rememberを使用して、showOnlyActiveが変更されたときのみ再計算)
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

        // フィルタリングUIの追加（RirekiScreenの期間選択に相当）
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
                .weight(1f), // 残りのスペースを占有
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredAlarms, key = { it.id }) { alarm -> // key指定でパフォーマンス向上
                // 💡 注意: 実際にはonToggleActiveでallAlarmsリスト自体を更新する必要があります
                AlarmSettingCard(
                    alarm = alarm,
                    onToggleActive = { newState ->
                        println("アラーム ID ${alarm.id} の状態が $newState に切り替わりました")
                        // 実際のアプリでは、ここでViewModelを通じて永続的なデータを更新する
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 設定を追加するボタン
        Button(
            onClick = {
                println("アラーム設定ボタンがクリックされました")
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
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // 💡 修正点: スイッチの状態を親から受け取った状態 (alarm.isActive) にバインド
    // ここではonToggleActiveが呼ばれた後に親で状態が更新されることを期待します。
    // ダミーデータのため、ここでは元の実装を維持しますが、本番では不要な場合が多いです。
    var isChecked by remember { mutableStateOf(alarm.isActive) }

    Card(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // Alignment をインポート
        ) {
            Column {
                Text(
                    text = alarm.time,
                    style = MaterialTheme.typography.displaySmall // アラーム時間は大きく表示
                )
                Text(
                    text = alarm.days,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // アクティブ/非アクティブを切り替えるスイッチ
            Switch( // androidx.compose.material3.Switch を Switch に省略
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it // この行を削除し、onToggleActive(it)のみにする方が適切です
                    onToggleActive(it)
                }
            )
        }
    }
}