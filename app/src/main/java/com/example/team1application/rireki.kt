package com.example.team1application

// 必要なインポートをすべて追加
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.Legend
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.rememberDateRangePickerState
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.components.AxisBase


// --- データ構造とヘルパー関数 ---


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
        SleepRecord("2025/12/2", "7h 30m", "07:00", "23:30", 2, "10分"),
        SleepRecord("2025/12/3", "8h 00m", "07:30", "23:30", 0, "0分"),
        SleepRecord("2025/12/4", "6h 45m", "06:30", "23:45", 3, "15分"),
        SleepRecord("2025/12/5", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/12/6", "7h 15m", "07:15", "00:00", 1, "5分"),
        SleepRecord("2025/12/7", "5h 15m", "05:15", "00:00", 1, "5分")
    )
}

/**
 * 睡眠時間文字列（例: "7h 30m"）を時間単位のFloatに変換する
 */
fun parseSleepDuration(sleepTime: String): Float {
    return try {
        val parts = sleepTime.split("h", "m").map { it.trim() }.filter { it.isNotEmpty() }
        val hours = parts.getOrNull(0)?.toFloat() ?: 0f
        val minutes = parts.getOrNull(1)?.toFloat() ?: 0f
        hours + minutes / 60f
    } catch (e: Exception) {
        0f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RirekiScreen(modifier: Modifier = Modifier) {
    val allRecords = getDummyRecords()

    // 状態iをRirekScreenで定義 (ステートホイスティング)
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    // フィルタリングロジック
    val filteredRecords = remember(startDate, endDate) {
        if (startDate == null || endDate == null) {
            allRecords // 期間が未選択なら全て表示
        } else {
            allRecords.filter { record ->
                try {
                    val recordDateMillis = dateFormatter.parse(record.date)?.time ?: 0L
                    val start = startDate!!
                    // 終了日の23:59:59までを範囲に含めるための調整
                    val end = endDate!! + TimeUnit.DAYS.toMillis(1) - 1

                    recordDateMillis in start..end
                } catch (e: Exception) {
                    false // 日付パースエラー時は除外
                }
            }
        }
    }

    // Column でセクションを縦に配置
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {

        // 1. グラフエリア (フィルタリングされたデータを渡す)
        SleepBarChart(
            records = filteredRecords,
            modifier = Modifier.height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 期間選択ボタン
        DateRangePickerButton(
            startDate = startDate,
            endDate = endDate,
            onDatesSelected = { start, end ->
                startDate = start
                endDate = end
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 表示件数
        Text(text = "表示件数: ${filteredRecords.size}件", style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(8.dp))

        // 4. 記録リスト
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredRecords) { record ->
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

// --- グラフコンポーネント ---

@Composable
fun SleepBarChart( // 関数名を内容に合わせて変更しました
    records: List<SleepRecord>,
    modifier: Modifier = Modifier
) {
    // 1. データの準備 (BarEntry を使用)
    val entries: List<BarEntry> = records.mapIndexed { index, record ->
        BarEntry(index.toFloat(), parseSleepDuration(record.sleepTime))
    }

    // 2. 解決済み: X軸ラベルの準備（Unresolved reference 'xAxisLabels' の定義）
    val xAxisLabels = records.map { it.date }

    // 3. AndroidView (BarChartの描画)
    AndroidView(
        modifier = modifier.fillMaxWidth().height(300.dp),
        factory = { context ->
            // BarChart を使用
            BarChart(context).apply {
                description.isEnabled = false
                isDragEnabled = true
                axisRight.isEnabled = false
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
            }
        },
        update = { chart ->
            // BarChart型にキャスト
            val barChart = chart

            if (entries.isNotEmpty()) {
                val barDataSet = BarDataSet(entries, "睡眠時間 (h)").apply {
                    color = Color.rgb(176, 224, 230)
                    valueTextSize = 12f
                    setDrawValues(true)
                }

                // 4. 修正済み: BarDataの設定を整理し、一度だけ実行する
                val barData = BarData(barDataSet).apply {
                    barWidth = 0.6f // 棒の幅を設定
                }
                barChart.data = barData // これで chart.data の設定は完了

                // X軸ラベル配列の安全な取得
                val labelsArray = if (xAxisLabels.isNotEmpty()) {
                    xAxisLabels.toTypedArray()
                } else {
                    arrayOf()
                }

                barChart.xAxis.apply {
                    // X軸ラベルに修正した配列を渡す
                    valueFormatter = IndexAxisValueFormatter(labelsArray)
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    labelRotationAngle = -45f
                    setDrawGridLines(false)
                }

                barChart.axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 10f

                    valueFormatter = object : ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            return String.format(Locale.US, "%.1f h", value)
                        }
                    }
                }
                barChart.animateY(800) // 棒グラフはY軸アニメーション
                barChart.invalidate()
            } else {
                barChart.clear()
            }
        }
    )
}

// --- 期間選択ボタン ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerButton(
    startDate: Long?,
    endDate: Long?,
    onDatesSelected: (Long?, Long?) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val formatter = remember { SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN) }

    Button(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        val startStr = startDate?.let { formatter.format(Date(it)) } ?: "開始日"
        val endStr = endDate?.let { formatter.format(Date(it)) } ?: "終了日"

        Text(text = "期間: $startStr 〜 $endStr")
    }

    if (showDatePicker) {
        val datePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDate,
            initialSelectedEndDateMillis = endDate
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        onDatesSelected(
                            datePickerState.selectedStartDateMillis,
                            datePickerState.selectedEndDateMillis
                        )
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedStartDateMillis != null &&
                            datePickerState.selectedEndDateMillis != null
                ) {
                    Text("決定")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("キャンセル")
                }
            }
        ) {
            DateRangePicker(
                state = datePickerState,
                headline = { Text("期間を選択してください") }
            )
        }
    }
}

// --- 記録カード ---

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