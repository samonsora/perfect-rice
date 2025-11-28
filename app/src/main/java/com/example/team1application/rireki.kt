package com.example.team1application

// 必要なインポートをすべて追加
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


/**
 * 1回分の睡眠記録を保持するデータクラス
 */
data class SleepRecord(
    val date: String,
    val sleepTime: String,
    val wakeUpTime: String,
    val bedtime: String,
    val snoozeCount: Int,
    val snoozeDuration: String
)

//テスト用ダミーデータ

fun getDummyRecords(): List<SleepRecord> {
    return listOf(
        SleepRecord("2025/11/25", "7h 30m", "07:00", "23:30", 2, "10分"),
        SleepRecord("2025/11/26", "8h 00m", "07:30", "23:30", 0, "0分"),
        SleepRecord("2025/11/27", "6h 45m", "06:30", "23:45", 3, "15分"),
        SleepRecord("2025/11/28", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/11/29", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/11/30", "5h 15m", "05:15", "00:00", 1, "5分")
    )
}

//画面定義(どの順番で表示していくかとか)
@Composable
fun RirekiScreen(modifier: Modifier = Modifier) {
    val records = getDummyRecords()

    // Column を使って、3つの主要なセクション（グラフ、検索、リスト）を縦に並べる
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        SleepChart(records = records)

        Spacer(modifier = Modifier.height(16.dp))

        RirekiSearchArea()

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 履歴リスト (LazyColumn)

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) //残りのエリア全て使用
        ) {
            items(records) { record ->
                RecordCard(
                    record = record,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

// 以下画面表示内容
@Composable
fun SleepChart(records: List<SleepRecord>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp) // グラフの固定の高さ
            .padding(top = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("グラフを表示する予定", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
@Composable
fun RirekiSearchArea() {
    // 修正点: 状態変数を定義し、入力内容を保持できるようにする
    var searchText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = searchText,
        onValueChange = { searchText = it }, // 入力内容を状態変数に反映
        label = { Text("日付で検索") },
        modifier = Modifier.fillMaxWidth()
    )
}
@Composable
fun RecordCard(record: SleepRecord, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = " ${record.date}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = "睡眠時間: ${record.sleepTime}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "就寝時間: ${record.bedtime}")
            Text(text = "起床時間: ${record.wakeUpTime}")
            Text(text = "スヌーズ回数: ${record.snoozeCount}回")
            Text(text = "スヌーズ合計時間: ${record.snoozeDuration}")
        }
    }
}